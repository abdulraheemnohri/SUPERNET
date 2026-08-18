use std::fs::OpenOptions;
use std::io::{self, Read, Write};
use std::os::fd::AsRawFd;
use std::os::unix::io::RawFd;

const TUNSETIFF: libc::c_ulong = 0x4004_54ca;
const IFF_TUN: libc::c_short = 0x0001;
const IFF_NO_PI: libc::c_short = 0x1000;

#[repr(C)]
struct IfReq {
    name: [libc::c_char; libc::IFNAMSIZ],
    flags: libc::c_short,
}

pub struct LinuxTun {
    file: std::fs::File,
}

impl LinuxTun {
    pub fn open(name: &str) -> io::Result<Self> {
        if name.len() >= libc::IFNAMSIZ { return Err(io::Error::new(io::ErrorKind::InvalidInput, "TUN name too long")); }
        let mut req = IfReq { name: [0; libc::IFNAMSIZ], flags: IFF_TUN | IFF_NO_PI };
        for (dst, src) in req.name.iter_mut().zip(name.bytes()) { *dst = src as libc::c_char; }
        let file = OpenOptions::new().read(true).write(true).open("/dev/net/tun")?;
        let rc = unsafe { libc::ioctl(file.as_raw_fd(), TUNSETIFF, &req) };
        if rc < 0 { return Err(io::Error::last_os_error()); }
        Ok(Self { file })
    }

    pub fn fd(&self) -> RawFd { self.file.as_raw_fd() }
}

impl crate::tun::GatewayTun for LinuxTun {
    fn read_packet(&mut self, buffer: &mut [u8]) -> io::Result<usize> { self.file.read(buffer) }
    fn write_packet(&mut self, packet: &[u8]) -> io::Result<()> { self.file.write_all(packet) }
}
