use std::time::{Duration, Instant};

pub const PATH_TIMEOUT: Duration = Duration::from_secs(15);

pub fn expire_paths(paths: &mut impl Iterator<Item = &'_ mut crate::session::PathState>) {
    let now = Instant::now();
    for path in paths {
        if now.duration_since(path.last_seen) > PATH_TIMEOUT {
            path.active = false;
        }
    }
}
