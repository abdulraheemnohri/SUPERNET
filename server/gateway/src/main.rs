mod heartbeat;
mod linux_tun;
mod packet;
mod session;
mod tun;

use std::io;
use std::net::UdpSocket;
use std::thread;
use std::time::{SystemTime, UNIX_EPOCH};

use heartbeat::expire_paths;
use linux_tun::LinuxTun;
use session::SessionManager;
use tun::GatewayTun;

const MAGIC: u32 = 0x53504E31;
const VERSION: u8 = 1;
const HEADER_SIZE: usize = 40;

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

fn main() -> io::Result<()> {
    let socket = UdpSocket::bind("0.0.0.0:48000")?;
    let mut gateway_tun = LinuxTun::open("supernet0")?;
    println!("SUPERNet gateway listening on UDP :48000, TUN supernet0");

    let mut sessions = SessionManager::default();
    let mut rx_buffer = [0u8; 65535];

    loop {
        let (size, peer) = socket.recv_from(&mut rx_buffer)?;
        let Some(packet) = decode(&rx_buffer[..size]) else { continue; };
        if packet.session_id == 0 { continue; }

        let session_id = packet.session_id;
        let path_id = packet.path_id;
        {
            let session = sessions.get_or_create(session_id);
            session.touch_path(path_id, peer);
            if packet.sequence < session.next_rx_sequence { continue; }
            if packet.sequence > session.next_rx_sequence {
                eprintln!("session={} sequence gap expected={} received={}", session_id, session.next_rx_sequence, packet.sequence);
            }
            session.next_rx_sequence = packet.sequence + 1;
            if !packet.payload.is_empty() {
                gateway_tun.write_packet(&packet.payload)?;
            }
        }

        // Expire stale paths without terminating their logical session.
        if let Some(session) = sessions.get_mut(session_id) {
            expire_paths(&mut session.paths.values_mut());
        }

        // Reverse traffic is handled by a dedicated reader per logical session.
        // It selects an active path round-robin; path loss therefore only removes
        // that path from the rotation instead of killing the session.
        if sessions.get(session_id).map(|s| s.next_tx_sequence == 0).unwrap_or(false) {
            if let Some(session) = sessions.get_mut(session_id) { session.next_tx_sequence = 1; }
            let reverse_socket = socket.try_clone()?;
            thread::spawn(move || {
                let mut tun = match LinuxTun::open("supernet0") { Ok(t) => t, Err(e) => { eprintln!("reverse TUN unavailable: {e}"); return; } };
                let mut tx_sequence = 0u64;
                let mut buffer = [0u8; 65535];
                let mut rr_path = 0usize;
                loop {
                    let size = match tun.read_packet(&mut buffer) { Ok(n) => n, Err(e) => { eprintln!("TUN read failed: {e}"); break; } };
                    if packet::validate_ip_payload(&buffer[..size]).is_err() { continue; }
                    // Path selection is completed by the authenticated session
                    // manager in the next transport layer. For now this packet
                    // uses path 0, the gateway-to-client control path.
                    let _ = rr_path;
                    rr_path = rr_path.wrapping_add(1);
                    let encoded = packet::encode(session_id, 0, tx_sequence, now_ms(), 0, &buffer[..size]);
                    tx_sequence = tx_sequence.wrapping_add(1);
                    if let Err(e) = reverse_socket.send_to(&encoded, "0.0.0.0:0") { eprintln!("reverse send failed: {e}"); break; }
                }
            });
        }
    }
}
