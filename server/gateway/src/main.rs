mod linux_tun;
mod packet;
mod tun;

use std::collections::BTreeMap;
use std::io;
use std::net::{SocketAddr, UdpSocket};
use std::thread;
use std::time::{SystemTime, UNIX_EPOCH};

use linux_tun::LinuxTun;
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

    // The current V1 prototype keeps the most recently observed client endpoint
    // per logical session. A production version will authenticate and maintain
    // explicit path registrations rather than trusting source addresses.
    let mut sessions: BTreeMap<u64, SocketAddr> = BTreeMap::new();
    let mut rx_buffer = [0u8; 65535];

    loop {
        let (size, peer) = socket.recv_from(&mut rx_buffer)?;
        let Some(packet) = decode(&rx_buffer[..size]) else { continue };
        let session = packet.session_id;
        sessions.insert(session, peer);

        if packet.sequence > 0 && packet.payload.is_empty() { continue; }
        if !packet.payload.is_empty() {
            gateway_tun.write_packet(&packet.payload)?;
        }

        // Spawn the reverse reader once per session. It reads IP packets from
        // the gateway TUN and returns them to the observed client endpoint.
        if sessions.len() == 1 && session != 0 {
            let reverse_socket = socket.try_clone()?;
            let reverse_peer = peer;
            thread::spawn(move || {
                let mut tun = match LinuxTun::open("supernet0") { Ok(t) => t, Err(e) => { eprintln!("reverse TUN unavailable: {e}"); return; } };
                let mut sequence = 0u64;
                let mut buffer = [0u8; 65535];
                loop {
                    let size = match tun.read_packet(&mut buffer) { Ok(n) => n, Err(e) => { eprintln!("TUN read failed: {e}"); break; } };
                    if packet::validate_ip_payload(&buffer[..size]).is_err() { continue; }
                    let encoded = packet::encode(session, 0, sequence, now_ms(), 0, &buffer[..size]);
                    sequence = sequence.wrapping_add(1);
                    if let Err(e) = reverse_socket.send_to(&encoded, reverse_peer) { eprintln!("reverse send failed: {e}"); break; }
                }
            });
        }
    }
}
