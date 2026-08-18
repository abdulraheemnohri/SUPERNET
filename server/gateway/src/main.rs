mod linux_tun;
mod tun;

use std::collections::BTreeMap;
use std::io;
use std::net::UdpSocket;
use std::time::{SystemTime, UNIX_EPOCH};

use linux_tun::LinuxTun;
use tun::GatewayTun;

const MAGIC: u32 = 0x53504E31;
const VERSION: u8 = 1;
const HEADER_SIZE: usize = 40;

#[derive(Debug)]
struct BondingPacket {
    session_id: u64,
    path_id: u32,
    sequence: u64,
    timestamp_ms: u64,
    flags: u8,
    payload: Vec<u8>,
}

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

fn now_ms() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_millis() as u64
}

fn main() -> io::Result<()> {
    let socket = UdpSocket::bind("0.0.0.0:48000")?;
    socket.set_nonblocking(false)?;
    let mut gateway_tun = LinuxTun::open("supernet0")?;
    println!("SUPERNet gateway listening on UDP :48000, TUN supernet0");

    let mut buffer = [0u8; 65535];
    let mut sessions: BTreeMap<u64, u64> = BTreeMap::new();

    loop {
        let (size, peer) = socket.recv_from(&mut buffer)?;
        let Some(packet) = decode(&buffer[..size]) else { continue };
        let expected = sessions.entry(packet.session_id).or_insert(packet.sequence);

        if packet.sequence < *expected { continue; }
        if packet.sequence > *expected {
            eprintln!("session={} sequence gap expected={} received={}", packet.session_id, *expected, packet.sequence);
        }
        *expected = packet.sequence + 1;

        if !packet.payload.is_empty() {
            gateway_tun.write_packet(&packet.payload)?;
        }

        println!(
            "rx session={} path={} seq={} bytes={} peer={} age_ms={}",
            packet.session_id,
            packet.path_id,
            packet.sequence,
            packet.payload.len(),
            peer,
            now_ms().saturating_sub(packet.timestamp_ms)
        );
    }
}
