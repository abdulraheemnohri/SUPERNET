use std::net::{SocketAddr, UdpSocket};
use std::io;

use crate::packet;
use crate::session::SessionState;

pub fn send_ip_packet(
    socket: &UdpSocket,
    session: &mut SessionState,
    payload: &[u8],
    timestamp_ms: u64,
) -> io::Result<bool> {
    packet::validate_ip_payload(payload)?;
    let Some((path_id, endpoint)) = session.select_next_path() else { return Ok(false); };
    let sequence = session.next_tx_sequence;
    session.next_tx_sequence = session.next_tx_sequence.wrapping_add(1);
    let encoded = packet::encode(session.session_id, path_id, sequence, timestamp_ms, 0, payload);
    socket.send_to(&encoded, endpoint)?;
    Ok(true)
}

#[allow(dead_code)]
pub fn selected_endpoint(session: &mut SessionState) -> Option<(u32, SocketAddr)> {
    session.select_next_path()
}
