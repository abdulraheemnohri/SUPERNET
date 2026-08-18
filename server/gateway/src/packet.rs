use std::io;

pub const MAGIC: u32 = 0x53504E31;
pub const VERSION: u8 = 1;
pub const HEADER_SIZE: usize = 40;

pub fn encode(session_id: u64, path_id: u32, sequence: u64, timestamp_ms: u64, flags: u8, payload: &[u8]) -> Vec<u8> {
    let mut out = Vec::with_capacity(HEADER_SIZE + payload.len());
    out.extend_from_slice(&MAGIC.to_be_bytes());
    out.push(VERSION);
    out.push(flags);
    out.extend_from_slice(&0u16.to_be_bytes());
    out.extend_from_slice(&session_id.to_be_bytes());
    out.extend_from_slice(&path_id.to_be_bytes());
    out.extend_from_slice(&sequence.to_be_bytes());
    out.extend_from_slice(&timestamp_ms.to_be_bytes());
    out.extend_from_slice(&(payload.len() as u32).to_be_bytes());
    out.extend_from_slice(payload);
    out
}

pub fn validate_ip_payload(payload: &[u8]) -> io::Result<()> {
    if payload.is_empty() { return Err(io::Error::new(io::ErrorKind::InvalidData, "empty IP packet")); }
    let version = payload[0] >> 4;
    if version != 4 && version != 6 { return Err(io::Error::new(io::ErrorKind::InvalidData, "unsupported IP version")); }
    Ok(())
}
