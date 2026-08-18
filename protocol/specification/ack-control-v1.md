# SUPERNet V1 ACK Control Frame

ACK is a control message carried on an authenticated control channel.

Layout, big-endian:

| Field | Size |
|---|---:|
| Type `ACK` | 1 |
| Session ID | 8 |
| Path ID | 4 |
| Highest contiguous sequence | 8 |

A receiver sends the highest sequence that has been delivered contiguously to the logical receive stream. The sender removes all retained frames up to that sequence from its retransmission queue.

ACKs are advisory to reliability state and must never cause a frame to be delivered twice.
