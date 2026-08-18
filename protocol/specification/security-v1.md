# SUPERNet V1 Security Boundary

SUPERNet must not invent a cryptographic protocol. The V1 implementation uses an established authenticated encrypted transport for production traffic.

## Requirements

- Server identity must be authenticated.
- Session establishment must provide fresh key material.
- Data confidentiality and integrity are mandatory.
- Replay protection must be provided by the selected secure transport/session.
- Credentials and private keys must never be logged.
- Gateway configuration must not accept unauthenticated Internet clients in production mode.
- Certificate/public-key validation failures terminate the session rather than silently downgrading security.

## Android

Long-lived client credentials belong in Android Keystore-backed storage where applicable. The VPN service must not inspect application passwords, OTPs or unrelated credentials.

## Gateway

Private server keys must be loaded from protected configuration or a secret-management mechanism and must never be committed to source control.

## Development mode

A local/insecure mode may exist solely for isolated development and integration tests. It must be explicit, clearly labeled and disabled by default in production builds.
