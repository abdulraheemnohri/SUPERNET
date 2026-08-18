use std::io;
use std::net::UdpSocket;
use std::sync::{Arc, Mutex};
use std::thread;
use std::time::{Duration, SystemTime, UNIX_EPOCH};

use crate::scheduler::send_ip_packet;
use crate::session::SessionManager;
use crate::tun::GatewayTun;

fn now_ms() -> u64 {
    SystemTime::now().duration_since(UNIX_EPOCH).unwrap_or_default().as_millis() as u64
}

/// Starts the V1 reverse dataplane. Packets emitted by the Linux TUN are
/// scheduled over any active client path, preserving the logical session.
pub fn start(
    mut tun: impl GatewayTun + Send + 'static,
    socket: UdpSocket,
    sessions: Arc<Mutex<SessionManager>>,
) -> io::Result<thread::JoinHandle<()>> {
    socket.set_read_timeout(Some(Duration::from_millis(250)))?;
    let handle = thread::Builder::new()
        .name("supernet-gateway-reverse".to_string())
        .spawn(move || {
            let mut buffer = [0u8; 65535];
            loop {
                match tun.read_packet(&mut buffer) {
                    Ok(size) if size > 0 => {
                        let payload = &buffer[..size];
                        let Ok(mut manager) = sessions.lock() else { break; };
                        // V1 supports the first active logical session; the
                        // session manager remains structured for multi-session V2.
                        if let Some(session_id) = manager.first_active_session_id() {
                            if let Some(session) = manager.get_mut(session_id) {
                                if let Err(error) = send_ip_packet(&socket, session, payload, now_ms()) {
                                    eprintln!("SUPERNet reverse send failed: {error}");
                                }
                            }
                        }
                    }
                    Ok(_) => {}
                    Err(error) if error.kind() == io::ErrorKind::WouldBlock || error.kind() == io::ErrorKind::TimedOut => {}
                    Err(error) => {
                        eprintln!("SUPERNet TUN read stopped: {error}");
                        break;
                    }
                }
            }
        })?;
    Ok(handle)
}
