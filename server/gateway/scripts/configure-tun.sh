#!/usr/bin/env bash
set -euo pipefail

TUN_IFACE="${1:-supernet0}"
TUN_ADDR="${2:-10.77.0.1/24}"
UPSTREAM_IFACE="${3:?usage: $0 [tun-interface] [tun-address] <upstream-interface>}"

ip link set "$TUN_IFACE" up
ip addr replace "$TUN_ADDR" dev "$TUN_IFACE"
sysctl -w net.ipv4.ip_forward=1 >/dev/null

if command -v nft >/dev/null 2>&1; then
  nft add table inet supernet 2>/dev/null || true
  nft 'add chain inet supernet forward { type filter hook forward priority filter; policy accept; }' 2>/dev/null || true
  nft 'add chain inet supernet postrouting { type nat hook postrouting priority srcnat; policy accept; }' 2>/dev/null || true
  nft "add rule inet supernet forward iifname \"$TUN_IFACE\" oifname \"$UPSTREAM_IFACE\" accept" 2>/dev/null || true
  nft "add rule inet supernet forward iifname \"$UPSTREAM_IFACE\" oifname \"$TUN_IFACE\" ct state established,related accept" 2>/dev/null || true
  nft "add rule inet supernet postrouting oifname \"$UPSTREAM_IFACE\" ip saddr 10.77.0.0/24 masquerade" 2>/dev/null || true
else
  iptables -C FORWARD -i "$TUN_IFACE" -o "$UPSTREAM_IFACE" -j ACCEPT 2>/dev/null || iptables -A FORWARD -i "$TUN_IFACE" -o "$UPSTREAM_IFACE" -j ACCEPT
  iptables -C FORWARD -i "$UPSTREAM_IFACE" -o "$TUN_IFACE" -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT 2>/dev/null || iptables -A FORWARD -i "$UPSTREAM_IFACE" -o "$TUN_IFACE" -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT
  iptables -t nat -C POSTROUTING -s 10.77.0.0/24 -o "$UPSTREAM_IFACE" -j MASQUERADE 2>/dev/null || iptables -t nat -A POSTROUTING -s 10.77.0.0/24 -o "$UPSTREAM_IFACE" -j MASQUERADE
fi

echo "SUPERNet TUN configured: $TUN_IFACE $TUN_ADDR -> $UPSTREAM_IFACE"
