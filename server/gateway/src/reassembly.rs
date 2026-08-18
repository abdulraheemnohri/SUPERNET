use std::collections::BTreeMap;

#[derive(Debug)]
pub struct ReassemblyBuffer {
    next_sequence: u64,
    pending: BTreeMap<u64, Vec<u8>>,
    max_pending: usize,
}

impl ReassemblyBuffer {
    pub fn new(next_sequence: u64, max_pending: usize) -> Self {
        Self { next_sequence, pending: BTreeMap::new(), max_pending: max_pending.max(1) }
    }

    pub fn push(&mut self, sequence: u64, payload: Vec<u8>) -> Vec<Vec<u8>> {
        if sequence < self.next_sequence { return Vec::new(); }
        if sequence == self.next_sequence {
            let mut ready = vec![payload];
            self.next_sequence = self.next_sequence.wrapping_add(1);
            while let Some(packet) = self.pending.remove(&self.next_sequence) {
                ready.push(packet);
                self.next_sequence = self.next_sequence.wrapping_add(1);
            }
            return ready;
        }
        if self.pending.len() < self.max_pending {
            self.pending.entry(sequence).or_insert(payload);
        }
        Vec::new()
    }

    pub fn next_sequence(&self) -> u64 { self.next_sequence }
    pub fn pending_len(&self) -> usize { self.pending.len() }
}

#[cfg(test)]
mod tests {
    use super::ReassemblyBuffer;

    #[test]
    fn reorders_small_gap() {
        let mut r = ReassemblyBuffer::new(10, 8);
        assert!(r.push(11, b"b".to_vec()).is_empty());
        let ready = r.push(10, b"a".to_vec());
        assert_eq!(ready, vec![b"a".to_vec(), b"b".to_vec()]);
    }

    #[test]
    fn ignores_duplicate() {
        let mut r = ReassemblyBuffer::new(1, 8);
        assert_eq!(r.push(1, b"x".to_vec()), vec![b"x".to_vec()]);
        assert!(r.push(1, b"duplicate".to_vec()).is_empty());
    }
}
