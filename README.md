# SUPERNet

**One Connection. Multiple Networks. Continuous Internet.**

SUPERNet is a non-AI Android network-bonding platform focused on connection continuity, multi-link resilience, and practical traffic aggregation.

## V1

The first milestone is a real Android-to-Linux bonded tunnel:

- Android `VpnService`
- Wi-Fi and cellular network discovery
- deterministic link scoring
- persistent logical session
- multipath packet scheduling
- gateway-side reassembly
- failover and recovery
- encrypted transport using established primitives
- diagnostics and automated tests

SUPERNet does **not** manufacture bandwidth. Aggregation depends on independent network links and a compatible gateway.

## Repository

- `android/` — Android client
- `server/` — Linux gateway
- `protocol/` — versioned bonding protocol
- `documentation/` — architecture, security, and testing

## Project status

V1 foundation is under active development on `agent/v1-foundation`.
