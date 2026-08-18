# SUPERNet V1 Session Handshake

## Goals

The gateway must explicitly establish a logical session before accepting data traffic and must treat each physical network as an independently registered path.

## Message types

- `CLIENT_HELLO` — starts a session.
- `SESSION_ACCEPT` — gateway accepts the session and returns negotiated parameters.
- `PATH_REGISTER` — attaches a physical path to an existing session.
- `PATH_ACK` — acknowledges path registration.
- `HEARTBEAT` — proves path liveness.
- `HEARTBEAT_ACK` — confirms liveness.
- `DATA` — carries an IP packet.
- `DATA_ACK` — acknowledges received data ranges.
- `PATH_CLOSE` — intentionally removes a path.
- `SESSION_CLOSE` — terminates the logical session.

## State machine

```text
NEW -> NEGOTIATING -> ESTABLISHED
                       |
                       +--> PATH_ACTIVE
                       +--> PATH_DEGRADED
                       +--> PATH_FAILED
                       +--> CLOSED
```

A path failure must not close the logical session while another authenticated path remains active.

## Path identity

`path_id` is unique within a session. A reconnecting physical path must not silently impersonate an existing path; the implementation should authenticate the registration and reject conflicting ownership.

## Security

The initial implementation must use an established authenticated cryptographic transport/session mechanism. This document deliberately does not define a custom cryptographic primitive or password-based authentication scheme.

## Versioning

Unknown message types and unsupported protocol versions must be rejected cleanly. New optional fields must not change the interpretation of existing mandatory fields.
