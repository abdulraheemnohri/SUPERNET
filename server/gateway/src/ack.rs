use crate::handshake::ControlMessage;

/// Builds a cumulative ACK for the logical receive stream.
pub fn cumulative_ack(session_id: u64, path_id: u32, highest_contiguous: u64) -> ControlMessage {
    ControlMessage::Ack {
        session_id,
        path_id,
        sequence: highest_contiguous,
    }
}
