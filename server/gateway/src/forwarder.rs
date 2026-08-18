use std::io;
use std::net::{SocketAddr, UdpSocket};

/// V1 forwarding boundary. The gateway receives packets from the bonded tunnel
/// and sends them toward the configured upstream. Routing/NAT policy remains
/// outside this small transport abstraction.
pub struct UdpForwarder {
    socket: UdpSocket,
    upstream: SocketAddr,
}

impl UdpForwarder {
    pub fn bind(bind: SocketAddr, upstream: SocketAddr) -> io::Result<Self> {
        Ok(Self { socket: UdpSocket::bind(bind)?, upstream })
    }

    pub fn send(&self, packet: &[u8]) -> io::Result<usize> {
        self.socket.send_to(packet, self.upstream)
    }

    pub fn socket(&self) -> &UdpSocket { &self.socket }
}
