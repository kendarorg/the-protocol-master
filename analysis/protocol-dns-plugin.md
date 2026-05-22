# protocol-dns-plugin analysis

## What it is

DNS server/proxy plugin (port 53, UDP + TCP). Like HTTP, it is a wrapper protocol —
no FSM engine, no `ProtoContext`. Intercepts DNS queries, applies static overrides and
blocklists, then forwards to upstream resolvers. Packaged as a pf4j plugin
(`DnsPlugin extends Plugin`), making it dynamically loadable.

---

## Architecture

```
Client DNS query
    │
    ├─ UDP (DatagramSocket, port 53)   ─┐
    └─ TCP (ServerSocket, port 53)     ─┤
                                        ▼
                              DnsProtocol.buildResponse()
                                        │
                              PRE_CALL plugins  ←─ can inject IPs, short-circuit
                                        │
                              ┌─────────┴──────────┐
                              │  blocked list check │  → NXDOMAIN if matched
                              └─────────┬──────────┘
                                        │
                              ┌─────────┴──────────┐
                              │  DnsProtocol        │
                              │  .doResolve()       │
                              │   1. registered map │  (static overrides, first)
                              │   2. cache lookup   │  (if useCache=true)
                              │   3. resolveRemote()│  (childDns servers, parallel)
                              └─────────┬──────────┘
                                        │
                              POST_CALL plugins
                                        │
                              PRE_SOCKET_WRITE plugins  ←─ on raw wire bytes
                                        │
                              send A record response (or NXDOMAIN)
```

---

## Key classes

| Class | Role |
|---|---|
| `DnsProtocol` | Entry point; owns two listeners (UDP + TCP); resolution logic; plugin dispatch |
| `DnsRunnable` | `Callable` that queries one upstream DNS server via dnsjava `Lookup` |
| `DnsPlugin` | pf4j `Plugin` lifecycle; exposes `getTpmPluginName()` / `getTpmPluginVersion()` |
| `DnsMapping` | Name→IP pair; name can be exact or `@regex` |
| `DnsProtocolSettings` | port, childDns, blocked, registered, useCache |
| `DnsApis` | REST API for runtime CRUD on registered/blocked lists |

---

## Resolution order in `doResolve()`

1. **Registered map** — static `DnsMapping` entries; name is exact or `@{regex}`.
   First match wins, immediately returned (no cache write).
2. **Cache** — in-memory `ConcurrentHashMap<String, List<String>>`. No TTL.
   Only queried when `useCache=true`.
3. **Remote** — `resolveRemote()`: queries all `childDns` servers in parallel via
   `DnsRunnable`. First server that returns a non-empty result cancels the others.
   2-second wall-clock timeout (polling loop, not `Future.get(timeout)`).

---

## Blocking logic

Checked before `doResolve()`. Two formats:
- Plain string — exact case-insensitive match on requested domain.
- `@{pattern}` — compiled as `Pattern` (cached in `patterns` map), matched against domain.

Blocked domains → `doResolve` is skipped, `ips` stays empty → `NXDOMAIN` response.

---

## Cyclic loop guard

Two mechanisms:

1. **All-caps domain**: `resolveRemote()` checks `domain.equals(domain.toUpperCase())`.
   `DnsRunnable` uppercases the domain before passing it to dnsjava (`Lookup`), so if the
   query somehow bounces back to this server, the uppercase form is detected and returns empty.

2. **Repeated TLD guard** in `buildResponse()`: if the domain has 3+ segments and its last
   two segments appear again as an internal occurrence (e.g. `x.foo.com.foo.com`), resolution
   is skipped. Guards against misconfigured forwarding loops.

3. **Special IPv6 loopback**: `1.0.0.0...ip6.arpa` hardcoded → `127.0.0.1`.

---

## Remote resolution (`DnsRunnable`)

- Uses dnsjava `SimpleResolver` + `ExtendedResolver`.
- Per-server timeout: 1 second, 2 retries.
- Queries `Type.A` only — no AAAA, MX, CNAME, etc.
- **Bug**: iterates `records[]` but reads `records[0]` for the IP on every iteration —
  only the first record's IP is ever added (though deduplication via `HashSet` would hide it).

---

## TCP framing

DNS-over-TCP uses a 2-byte big-endian length prefix (RFC 1035 §4.2.2):

```
[2 bytes: length][N bytes: DNS message]
```

`resolveAll(OutputStream, ...)` writes this format via `DataOutputStream`.
`runTcp()` reads with `DataInputStream.readUnsignedShort()` + `readFully()`.

---

## Plugin phases used

| Phase | Used by |
|---|---|
| `PRE_CALL` | receives `(requestedDomain, List<String> ips)` — can populate ips and return `true` to skip resolution |
| `POST_CALL` | receives `(requestedDomain, List<String> ips)` — read-only reporting |
| `PRE_SOCKET_WRITE` | receives `(byte[] responseWireBytes, null)` — can mutate raw DNS response |

---

## Plugins

| Plugin | Notes |
|---|---|
| `DnsLatencyPlugin` | Injects sleep; extends `BasicLatencyPlugin` |
| `DnsNetErrorPlugin` | Injects network errors; extends `BasicNetworkErrorPlugin` |
| `DnsReportPlugin` | Emits metrics; extends base report plugin |
| `DnsRestPluginsPlugin` | REST management API for plugins |

No Mock, Record, or Replay plugins — DNS responses are fully reconstructed from static
config and upstream forwarding; recording/replaying DNS is not supported.

---

## REST API (`DnsApis`)

All endpoints scoped to `{#protocolInstanceId}`:

| Method | Path | Action |
|---|---|---|
| `GET` | `/dns/registered` | List all static mappings |
| `POST` | `/dns/registered` | Add or update mappings (upsert by name) |
| `DELETE` | `/dns/registered/{dnsName}` | Remove by name or IP |
| `GET` | `/dns/blocked` | List all blocked domains |
| `POST` | `/dns/blocked` | Add blocked domains (no-op if already present) |
| `DELETE` | `/dns/blocked/{dnsName}` | Remove blocked domain |

Every mutating call invokes `protocol.clearCache()` — wipes both `cached` and `patterns`.

---

## Settings (`DnsProtocolSettings`)

```
port     = 53
childDns = []         # upstream DNS servers (IP or hostname; hostname resolved at startup)
blocked  = []         # exact names or @regex patterns → NXDOMAIN
registered = []       # static overrides: [{name, ip}]; name can be @regex
useCache = false      # in-memory cache with no TTL
```

---

## Notable quirks

- Plugin initialization loop runs **twice** in the constructor — identical code block
  is duplicated (lines 56–68 and 79–89 in `DnsProtocol`). Second pass is redundant.
- `resolveRemote()` passes `requestedDomain.toUpperCase()` to `DnsRunnable.Lookup()`,
  but dnsjava's `Lookup` is case-insensitive; the uppercase trick is only meaningful
  as the cyclic-call sentinel described above.
- `localhost` and `*.in-addr.arpa` / `*.ip6.arpa` always resolve to `127.0.0.1` in
  `resolveRemote()`, regardless of registered/blocked config — these are hardcoded early
  returns.
- `childDns` entries that are hostnames (not IPs) are resolved to IPs at startup but
  stored as the resolved IP string in `dnsServers`; `DnsRunnable` uses `settings.getChildDns()`
  directly, not `dnsServers` — so startup resolution is unused.
- Cache is never persisted; cleared on any REST mutation; no max size.
- Thread pool: fixed 20 threads shared between UDP and TCP handlers.
