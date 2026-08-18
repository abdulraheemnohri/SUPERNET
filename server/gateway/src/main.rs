mod ack;
mod heartbeat;
mod handshake;
mod linux_tun;
mod packet;
mod reassembly;
mod session;
mod tun;

use std::io;
use std::net::UdpSocket;
use std::time::{SystemTime, UNIX_EPOCH};

use ack::cumulative_ack;
use handshake::ControlMessage;
use heartbeat::expire_paths;
use linux_tun::LinuxTun;
use reassembly::ReassemblyBuffer;
use session::SessionManager;
use tun::GatewayTun;

const MAGIC: u32 = 0x53504E31;
const VERSION: u8 = 1;
const HEADER_SIZE: usize = 40;
const FLAG_CONTROL: u8 = 0x01;

#[derive(Debug)]
struct BondingPacket { session_id: u64, path_id: u32, sequence: u64, timestamp_ms: u64, flags: u8, payload: Vec<u8> }

fn decode(bytes: &[u8]) -> Option<BondingPacket> {
    if bytes.len() < HEADER_SIZE { return None; }
    if u32::from_be_bytes(bytes[0..4].try_into().ok()?) != MAGIC || bytes[4] != VERSION { return None; }
    let flags = bytes[5];
    let session_id = u64::from_be_bytes(bytes[8..16].try_into().ok()?);
    let path_id = u32::from_be_bytes(bytes[16..20].try_into().ok()?);
    let sequence = u64::from_be_bytes(bytes[20..28].try_into().ok()?);
    let timestamp_ms = u64::from_be_bytes(bytes[28..36].try_into().ok()?);
    let length = u32::from_be_bytes(bytes[36..40].try_into().ok()?) as usize;
    if length != bytes.len() - HEADER_SIZE { return None; }
    Some(BondingPacket { session_id, path_id, sequence, timestamp_ms, flags, payload: bytes[HEADER_SIZE..].to_vec() })
}

fn now_ms() -> u64 { SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_millis() as u64 }

fn send_control(socket: &UdpSocket, peer: std::net::SocketAddr, message: ControlMessage) -> io::Result<()> {
    let session_id = match message {
        ControlMessage::ClientHello { session_id } | ControlMessage::SessionAccept { session_id } | ControlMessage::SessionClose { session_id } | ControlMessage::PathRegister { session_id, .. } | ControlMessage::PathAck { session_id, .. } | ControlMessage::Heartbeat { session_id, .. } | ControlMessage::HeartbeatAck { session_id, .. } | ControlMessage::PathClose { session_id, .. } | ControlMessage::Ack { session_id, .. } => session_id,
    };
    let encoded = packet::encode(session_id, 0, 0, now_ms(), FLAG_CONTROL, &message.encode());
    socket.send_to(&encoded, peer)?;
    Ok(())
}

fn main() -> io::Result<()> {
    let socket = UdpSocket::bind("0.0.0.0:48000")?;
    let mut gateway_tun = LinuxTun::open("supernet0")?;
    println!("SUPERNet gateway listening on UDP :48000, TUN supernet0");

    let mut sessions = SessionManager::default();
    let mut reassembly: std::collections::HashMap<u64, ReassemblyBuffer> = std::collections::HashMap::new();
    let mut rx_buffer = [0u8; 65535];

    loop {
        let (size, peer) = socket.recv_from(&mut rx_buffer)?;
        let Some(packet) = decode(&rx_buffer[..size]) else { continue; };
        if packet.session_id == 0 { continue; }

        if packet.flags & FLAG_CONTROL != 0 {
            let Ok(control) = ControlMessage::decode(&packet.payload) else { continue; };
            match control {
                ControlMessage::ClientHello { session_id } => {
                    sessions.get_or_create(session_id);
                    reassembly.entry(session_id).or_insert_with(|| ReassemblyBuffer::new(0, 256));
                    send_control(&socket, peer, ControlMessage::SessionAccept { session_id })?;
                }
                ControlMessage::PathRegister { session_id, path_id } => {
                    let session = sessions.get_or_create(session_id);
                    session.touch_path(path_id, peer);
                    send_control(&socket, peer, ControlMessage::PathAck { session_id, path_id })?;
                }
                ControlMessage::Heartbeat { session_id, path_id, nonce } => {
                    let session = sessions.get_or_create(session_id);
                    session.touch_path(path_id, peer);
                    send_control(&socket, peer, ControlMessage::HeartbeatAck { session_id, path_id, nonce })?;
                }
                ControlMessage::Ack { session_id, path_id, sequence } => {
                    if let Some(session) = sessions.get_mut(session_id) {
                        session.touch_path(path_id, peer);
                        session.last_ack = session.last_ack.max(sequence);
                    }
                }
                ControlMessage::PathClose { session_id, path_id } => {
                    if let Some(session) = sessions.get_mut(session_id) { if let Some(path) = session.paths.get_mut(&path_id) { path.active = false; } }
                }
                ControlMessage::SessionClose { session_id } => {
                    if let Some(session) = sessions.get_mut(session_id) { for path in session.paths.values_mut() { path.active = false; } }
                    reassembly.remove(&session_id);
                }
                ControlMessage::SessionAccept { .. } | ControlMessage::PathAck { .. } | ControlMessage::HeartbeatAck { .. } => {}
            }
            if let Some(session) = sessions.get_mut(packet.session_id) { expire_paths(&mut session.paths.values_mut()); }
            continue;
        }

        let session_id = packet.session_id;
        let path_id = packet.path_id;
        let sequence = packet.sequence;
        let mut ready = Vec::new();
        {
            let session = sessions.get_or_create(session_id);
            session.touch_path(path_id, peer);
            let buffer = reassembly.entry(session_id).or_insert_with(|| ReassemblyBuffer::new(session.next_rx_sequence, 256));
            ready = buffer.push(sequence, packet.payload);
            session.next_rx_sequence = buffer.next_sequence();
        }
        for payload in ready { if !payload.is_empty() { gateway_tun.write_packet(&payload)?; } }
        if let Some(session) = sessions.get_mut(session_id) {
            expire_paths(&mut session.paths.values_mut());
            let ack = cumulative_ack(session_id, path_id, session.next_rx_sequence.saturating_sub(1));
            send_control(&socket, peer, ack)?;
        }
    }
}
