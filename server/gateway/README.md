# SUPERNet Gateway

The gateway terminates SUPERNet client sessions and reconstructs packets received over independent network paths.

V1 responsibilities:

1. Authenticate a client session.
2. Track attached path IDs.
3. Validate and decode `BondingPacket` envelopes.
4. Reorder packets by sequence number.
5. Detect gaps for future retransmission/recovery.
6. Forward reconstructed IP packets toward the Internet.
7. Return Internet packets through the persistent logical session.

The gateway must treat path loss as a path event, not a session event.

The concrete UDP/QUIC transport implementation comes after the protocol and scheduling layers are tested.
