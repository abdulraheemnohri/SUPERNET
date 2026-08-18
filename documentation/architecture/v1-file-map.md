# SUPERNet V1 File Map

## Android

The Android V1 implementation is organized around the VPN service, network/link management, bonding scheduler, packet codec, session/control handling, recovery, and UI. The VPN service is the traffic interception boundary.

## Gateway

`server/gateway` contains the Linux gateway binary, session and handshake logic, packet/reassembly code, TUN integration, forwarding, heartbeat, scheduler and routing setup.

## Protocol

`protocol/specification` is the protocol contract shared by client and gateway. It defines the V1 wire frame, ACK/retransmission semantics and security boundary.

## CI

`.github/workflows/v1-ci.yml` builds/tests the Rust gateway and assembles the Android debug APK on every push to `main` and every pull request targeting `main`.

## V1 completion rule

A file existing is not considered feature completion. V1 is complete only when CI is green and a real Android device can establish an authenticated encrypted session, carry bidirectional IP traffic through at least two physical paths, survive one path failure, recover packets, and terminate cleanly.
