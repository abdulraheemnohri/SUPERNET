# SUPERNet V1 Reliability

V1 uses cumulative acknowledgements at the logical-session level.

## ACK state

A receiver tracks the highest contiguous sequence delivered to TUN and reports that value as `ack_sequence`.

Packets above the contiguous point remain buffered until the gap is filled or the bounded reassembly policy expires them.

## Retransmission

The sender retains unacknowledged frames in a bounded per-session retransmission queue. A frame may be retransmitted when:

- its retransmission timer expires;
- an explicit gap/negative acknowledgement identifies it as missing; or
- the selected path fails before acknowledgement.

Retransmission must respect a maximum retry count and congestion/backoff limits.

## Duplicate handling

A sequence already delivered is ignored. Retransmitted packets are therefore safe to deliver over another healthy path.

## Path failure

Path failure removes only the physical path. Unacknowledged frames remain associated with the logical session and may be scheduled on another available path.

## Bounds

Implementations must cap queued bytes, queued packet count and retry count. A broken peer must never cause unbounded memory growth.
