# SUPERNet V1 Status

## Implemented foundations

- Android `VpnService` TUN foundation
- Android network/path discovery and deterministic link scoring
- Android packet scheduler and bonding packet model
- Android control-message codec and control channel
- Android session/path heartbeat controller
- Android TUN packet-pump/data-plane coordinator
- Gateway session/path lifecycle
- Gateway packet codec and scheduling foundation
- Gateway packet reassembly foundation
- Gateway forwarding abstraction
- Linux forwarding/NAT setup script
- V1 wire-format specification

## Production gates

These must pass before calling V1 production-ready:

1. Android client and gateway compile cleanly together.
2. An authenticated encrypted session is established.
3. Android sends TUN traffic over at least two independently bound networks.
4. Gateway reassembles and forwards traffic through Linux routing/NAT.
5. Return traffic is delivered back to the correct Android session and injected into TUN.
6. One path can disappear without terminating the logical session.
7. Duplicate, reordered and delayed packets are handled deterministically.
8. MTU/fragmentation behavior is tested.
9. Firewall/kill-switch behavior is tested.
10. Real-device tests cover Wi-Fi/cellular handover, sleep/wake and airplane mode.
11. Load tests cover concurrent sessions and bounded memory.
12. No credential/traffic inspection is performed outside the tunnel endpoint.

The project should not claim zero buffering or unlimited bandwidth. Performance depends on the aggregate physical links, their latency/loss characteristics, gateway capacity and Internet path.
