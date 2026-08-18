# SUPERNet V1 Wire Format

All multi-byte integers are unsigned big-endian.

## Data frame

| Offset | Size | Field |
|---:|---:|---|
| 0 | 4 | Magic `SUPN` |
| 4 | 1 | Version `1` |
| 5 | 1 | Flags |
| 6 | 2 | Header length |
| 8 | 8 | Session ID |
| 16 | 4 | Path ID |
| 20 | 8 | Sequence |
| 28 | 8 | Timestamp (ms) |
| 36 | 4 | Payload length |
| 40 | N | IP payload |

Receivers must validate magic, version, header length, payload length and maximum frame size before allocation or forwarding.

## Control frames

Control frames are deliberately separate from data frames. Their message definitions are specified in `session.md`.

## Reliability

Sequence numbers are scoped to a logical session. Path IDs identify physical transport paths. A receiver may reorder packets across paths and must discard duplicates that have already been delivered.

## Security

The wire format is not itself an encryption primitive. Production deployments must run the control and data channels inside an authenticated encrypted transport/session. Do not add custom cryptography to this framing layer.
