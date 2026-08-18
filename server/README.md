# SUPERNet Gateway

V1 gateway responsibilities:

1. Accept authenticated client sessions.
2. Attach independent client network paths to one logical session.
3. Validate packet metadata and sequence numbers.
4. Reorder and reassemble traffic.
5. Route reconstructed traffic to the Internet.
6. Return response traffic through available paths.
7. Keep the logical session alive when an individual path fails.

The production gateway will be implemented with Rust or Go and an established encrypted transport such as QUIC. No custom cryptography is planned.
