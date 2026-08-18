use std::collections::HashMap;
use std::net::SocketAddr;
use std::time::Instant;

#[derive(Debug, Clone)]
pub struct PathState {
    pub path_id: u32,
    pub endpoint: SocketAddr,
    pub last_seen: Instant,
    pub active: bool,
}

#[derive(Debug)]
pub struct SessionState {
    pub session_id: u64,
    pub paths: HashMap<u32, PathState>,
    pub next_rx_sequence: u64,
    pub next_tx_sequence: u64,
}

impl SessionState {
    pub fn new(session_id: u64) -> Self {
        Self {
            session_id,
            paths: HashMap::new(),
            next_rx_sequence: 0,
            next_tx_sequence: 0,
        }
    }

    pub fn attach_path(&mut self, path_id: u32, endpoint: SocketAddr) {
        self.paths.insert(path_id, PathState {
            path_id,
            endpoint,
            last_seen: Instant::now(),
            active: true,
        });
    }

    pub fn touch_path(&mut self, path_id: u32, endpoint: SocketAddr) {
        if let Some(path) = self.paths.get_mut(&path_id) {
            path.endpoint = endpoint;
            path.last_seen = Instant::now();
            path.active = true;
        } else {
            self.attach_path(path_id, endpoint);
        }
    }

    pub fn active_paths(&self) -> impl Iterator<Item = &PathState> {
        self.paths.values().filter(|p| p.active)
    }
}

#[derive(Default)]
pub struct SessionManager {
    sessions: HashMap<u64, SessionState>,
}

impl SessionManager {
    pub fn get_or_create(&mut self, session_id: u64) -> &mut SessionState {
        self.sessions.entry(session_id).or_insert_with(|| SessionState::new(session_id))
    }

    pub fn get(&self, session_id: u64) -> Option<&SessionState> {
        self.sessions.get(&session_id)
    }
}
