use std::io;

/// Minimal gateway TUN abstraction. The concrete Linux fd integration is kept
/// behind this interface so packet routing can be tested independently.
pub trait GatewayTun {
    fn read_packet(&mut self, buffer: &mut [u8]) -> io::Result<usize>;
    fn write_packet(&mut self, packet: &[u8]) -> io::Result<()>;
}

pub struct UnsupportedTun;

impl GatewayTun for UnsupportedTun {
    fn read_packet(&mut self, _buffer: &mut [u8]) -> io::Result<usize> {
        Err(io::Error::new(io::ErrorKind::Unsupported, "Linux TUN backend not configured"))
    }

    fn write_packet(&mut self, _packet: &[u8]) -> io::Result<()> {
        Err(io::Error::new(io::ErrorKind::Unsupported, "Linux TUN backend not configured"))
    }
}
