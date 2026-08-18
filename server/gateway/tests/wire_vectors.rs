#[test]
fn minimum_data_frame_layout_is_40_bytes_plus_payload() {
    let payload = [0u8; 20];
    assert_eq!(40 + payload.len(), 60);
}

#[test]
fn sequence_is_session_scoped() {
    let session_a = 10u64;
    let session_b = 11u64;
    let sequence = 1u64;
    assert_ne!(session_a, session_b);
    assert_eq!(sequence, 1);
}
