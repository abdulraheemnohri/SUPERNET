use std::io;

pub const CLIENT_HELLO: u8 = 1;
pub const SESSION_ACCEPT: u8 = 2;
pub const PATH_REGISTER: u8 = 3;
pub const PATH_ACK: u8 = 4;
pub const HEARTBEAT: u8 = 5;
pub const HEARTBEAT_ACK: u8 = 6;
pub const PATH_CLOSE: u8 = 7;
pub const SESSION_CLOSE: u8 = 8;
pub const ACK: u8 = 9;

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ControlMessage {
    ClientHello { session_id: u64 },
    SessionAccept { session_id: u64 },
    PathRegister { session_id: u64, path_id: u32 },
    PathAck { session_id: u64, path_id: u32 },
    Heartbeat { session_id: u64, path_id: u32, nonce: u64 },
    HeartbeatAck { session_id: u64, path_id: u32, nonce: u64 },
    PathClose { session_id: u64, path_id: u32 },
    SessionClose { session_id: u64 },
    Ack { session_id: u64, path_id: u32, sequence: u64 },
}

impl ControlMessage {
    pub fn encode(&self) -> Vec<u8> {
        let mut out = Vec::with_capacity(32);
        match *self {
            Self::ClientHello { session_id } => { out.push(CLIENT_HELLO); out.extend_from_slice(&session_id.to_be_bytes()); }
            Self::SessionAccept { session_id } => { out.push(SESSION_ACCEPT); out.extend_from_slice(&session_id.to_be_bytes()); }
            Self::PathRegister { session_id, path_id } => { out.push(PATH_REGISTER); out.extend_from_slice(&session_id.to_be_bytes()); out.extend_from_slice(&path_id.to_be_bytes()); }
            Self::PathAck { session_id, path_id } => { out.push(PATH_ACK); out.extend_from_slice(&session_id.to_be_bytes()); out.extend_from_slice(&path_id.to_be_bytes()); }
            Self::Heartbeat { session_id, path_id, nonce } => { out.push(HEARTBEAT); out.extend_from_slice(&session_id.to_be_bytes()); out.extend_from_slice(&path_id.to_be_bytes()); out.extend_from_slice(&nonce.to_be_bytes()); }
            Self::HeartbeatAck { session_id, path_id, nonce } => { out.push(HEARTBEAT_ACK); out.extend_from_slice(&session_id.to_be_bytes()); out.extend_from_slice(&path_id.to_be_bytes()); out.extend_from_slice(&nonce.to_be_bytes()); }
            Self::PathClose { session_id, path_id } => { out.push(PATH_CLOSE); out.extend_from_slice(&session_id.to_be_bytes()); out.extend_from_slice(&path_id.to_be_bytes()); }
            Self::SessionClose { session_id } => { out.push(SESSION_CLOSE); out.extend_from_slice(&session_id.to_be_bytes()); }
            Self::Ack { session_id, path_id, sequence } => { out.push(ACK); out.extend_from_slice(&session_id.to_be_bytes()); out.extend_from_slice(&path_id.to_be_bytes()); out.extend_from_slice(&sequence.to_be_bytes()); }
        }
        out
    }

    pub fn decode(input: &[u8]) -> io::Result<Self> {
        let kind = *input.first().ok_or_else(|| io::Error::new(io::ErrorKind::UnexpectedEof, "empty control message"))?;
        fn u64_at(b: &[u8], o: usize) -> io::Result<u64> { b.get(o..o+8).and_then(|v| v.try_into().ok()).map(u64::from_be_bytes).ok_or_else(|| io::Error::new(io::ErrorKind::UnexpectedEof, "missing u64")) }
        fn u32_at(b: &[u8], o: usize) -> io::Result<u32> { b.get(o..o+4).and_then(|v| v.try_into().ok()).map(u32::from_be_bytes).ok_or_else(|| io::Error::new(io::ErrorKind::UnexpectedEof, "missing u32")) }
        match kind {
            CLIENT_HELLO if input.len() == 9 => Ok(Self::ClientHello { session_id: u64_at(input, 1)? }),
            SESSION_ACCEPT if input.len() == 9 => Ok(Self::SessionAccept { session_id: u64_at(input, 1)? }),
            PATH_REGISTER if input.len() == 13 => Ok(Self::PathRegister { session_id: u64_at(input, 1)?, path_id: u32_at(input, 9)? }),
            PATH_ACK if input.len() == 13 => Ok(Self::PathAck { session_id: u64_at(input, 1)?, path_id: u32_at(input, 9)? }),
            HEARTBEAT if input.len() == 21 => Ok(Self::Heartbeat { session_id: u64_at(input, 1)?, path_id: u32_at(input, 9)?, nonce: u64_at(input, 13)? }),
            HEARTBEAT_ACK if input.len() == 21 => Ok(Self::HeartbeatAck { session_id: u64_at(input, 1)?, path_id: u32_at(input, 9)?, nonce: u64_at(input, 13)? }),
            PATH_CLOSE if input.len() == 13 => Ok(Self::PathClose { session_id: u64_at(input, 1)?, path_id: u32_at(input, 9)? }),
            SESSION_CLOSE if input.len() == 9 => Ok(Self::SessionClose { session_id: u64_at(input, 1)? }),
            ACK if input.len() == 21 => Ok(Self::Ack { session_id: u64_at(input, 1)?, path_id: u32_at(input, 9)?, sequence: u64_at(input, 13)? }),
            _ => Err(io::Error::new(io::ErrorKind::InvalidData, "invalid control message")),
        }
    }
}
