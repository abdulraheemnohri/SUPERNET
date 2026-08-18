# SUPERNet V1 Packet Format

SUPERNet V1 uses a fixed 40-byte big-endian envelope followed by an IPv4 or IPv6 packet payload.

| Offset | Size | Field |
|---:|---:|---|
| 0 | 4 | Magic `0x53504E31` (`SPN1`) |
| 4 | 1 | Version `1` |
| 5 | 1 | Flags |
| 6 | 2 | Reserved, zero |
| 8 | 8 | Session ID |
| 16 | 4 | Path ID |
| 20 | 8 | Sequence number |
| 28 | 8 | Sender timestamp in Unix milliseconds |
| 36 | 4 | Payload length |
| 40 | N | IPv4/IPv6 payload |

All integer fields are network byte order (big endian).

## V1 invariants

- Session ID MUST be non-zero.
- Path ID identifies a physical transport path within a session.
- Sequence numbers are monotonically increasing within each packet direction.
- Payload length MUST equal the datagram remainder.
- Payload MUST contain an IPv4 or IPv6 packet.
- Implementations MUST reject unknown protocol versions.
- Reserved bits MUST be zero when transmitted and ignored when received.

## Security

This envelope is framing, not authentication. Production deployments MUST authenticate the peer and protect packet confidentiality/integrity with an established secure transport such as QUIC/TLS. Do not treat an unauthenticated UDP source address as proof of session ownership.
