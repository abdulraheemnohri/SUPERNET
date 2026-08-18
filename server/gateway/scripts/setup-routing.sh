#!/usr/bin/env bash
set -euo pipefail

# Configure forwarding/NAT for the SUPERNet gateway.
# Run as root and explicitly provide the Internet-facing interface.
UPSTREAM_IFACE="${1:?usage: $0 <upstream-interface> [supernet-cidr]}"
SUPERNET_CIDR="${2:-10.77.0.0/24}"

sysctl -w net.ipv4.ip_forward=1 >/dev/null

if command -v nft >/dev/null 2>&1; then
  nft add table inet supernet 2>/dev/null || true
  nft 'add chain inet supernet postrouting { type nat hook postrouting priority srcnat; policy accept; }' 2>/dev/null || true
  nft add rule inet supernet postrouting oifname "$UPSTREAM_IFACE" ip saddr "$SUPERNET_CIDR" masquerade 2>/dev/null || true
else
  iptables -t nat -C POSTROUTING -s "$SUPERNET_CIDR" -o "$UPSTREAM_IFACE" -j MASQUERADE 2>/dev/null || \
    iptables -t nat -A POSTROUTING -s "$SUPERNET_CIDR" -o "$UPSTREAM_IFACE" -j MASQUERADE
fi

echo "SUPERNet IPv4 forwarding/NAT enabled on $UPSTREAM_IFACE for $SUPERNET_CIDR"
