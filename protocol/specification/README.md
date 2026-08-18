# SUPERNet V1 Protocol

SUPERNet V1 uses a persistent logical session between the Android client and a gateway.

## Packet model

Each bonded data unit is associated with:

- session ID
- path ID
- sequence number
- payload length
- timestamp
- flags
- integrity metadata

## Path model

A session may attach multiple physical paths. A path can be ACTIVE, DEGRADED, UNSTABLE, FAILED, or RECOVERING.

## Design requirements

1. A physical path failure must not destroy the logical session.
2. Sequence numbers allow the gateway to detect reordering and gaps.
3. Scheduler decisions are deterministic and metric-driven.
4. Cryptography uses established primitives; SUPERNet does not invent cryptography.
5. Protocol changes must add test vectors before implementation rollout.
