# protocol-http analysis

## What it is

HTTP/HTTPS transparent proxy with MITM SSL termination. Unlike the JDBC protocols,
it does NOT use the FSM-based wire-protocol engine — it wraps `com.sun.net.httpserver`
with a custom server implementation and a separate TCP CONNECT proxy for browser traffic.

Three listener ports run simultaneously:

| Port (default) | Role |
|---|---|
| `http=4080` | Plain HTTP — receives forwarded traffic |
| `https=4443` | HTTPS — terminates TLS, same handler |
| `proxy=9999` | HTTP CONNECT proxy — browser points here; redirects to the two ports above |

---

## Architecture

```
Browser / client
    │
    ▼  (HTTP CONNECT or direct HTTP)
ProxyServer (port 9999)
    │  CONNECT → redirect to 4443
    │  HTTP    → redirect to 4080
    ▼
KendarHttpServer / KendarHttpsServer  (com.sun.net.httpserver wrappers)
    │
    ▼
MasterHandler.handle()
    │
    ├─ CONNECT phase   → plugins (e.g. auth, rate-limit)
    ├─ PRE_CALL phase  → plugins (mock, rewrite, replay → short-circuit if matched)
    ├─ ExternalRequester.callSite()   ← forward to real upstream (Apache HttpClient)
    ├─ POST_CALL phase → plugins (record, report)
    └─ FINALIZE phase  → always runs (cleanup, metrics)
```

---

## Key classes

| Class | Role |
|---|---|
| `HttpProtocol` | Entry point; owns lifecycle (start/terminate); builds the three servers; wires `CertificatesManager`, `ProxyServer`, `MasterHandler` |
| `MasterHandler` | `HttpHandler` impl; orchestrates plugin phases; sends response |
| `ProxyServer` | Plain TCP server on proxy port; one thread per connection via cached thread pool |
| `ProxyServerHandler` | Per-connection: parses first line; CONNECT → tunnel to 4443; plain HTTP → tunnel to 4080 |
| `KendarHttpServer` / `KendarHttpsServer` | Thin wrappers over `com.sun.net.httpserver`; delegate to custom `ServerImpl` |
| `CertificatesManager` | BouncyCastle-based MITM CA; generates per-host SNI certs on demand; updates `HttpsServer` SSLContext live |
| `ExternalRequesterImpl` | Forwards request to real upstream via Apache HttpClient; DNS-resolves host first |
| `BaseRequesterImpl` | Builds Apache `HttpRequestBase` for all HTTP methods; handles form, multipart, binary, JSON-Smile bodies |
| `PluginClassesHandlerImpl` | Dispatches to active plugins by `ProtocolPhase`; returns `true` to short-circuit |
| `SiteMatcherUtils` | Host+path glob filtering used by record/replay/mock plugins to scope which requests they act on |
| `DnsMultiResolverImpl` | Resolves hostnames; used by `ExternalRequester` to validate before forwarding |

---

## Plugin phases

`ProtocolPhase` values used by HTTP plugins, in order:

1. `CONNECT` — pre-request (auth, ACL)
2. `PRE_CALL` — before upstream call (mock returns here, rewrite mutates request here)
3. `POST_CALL` — after upstream call (record saves here, report emits metrics here)
4. `FINALIZE` — always called in `finally` block regardless of exceptions

Any phase handler returning `true` stops further processing for that phase.
`PRE_CALL` returning `true` skips the upstream call entirely (mock/replay path).

---

## SSL MITM flow

1. Browser sends `CONNECT host:443` to proxy port 9999.
2. `ProxyServerHandler` intercepts, calls `dnsResolver(host)` which:
   - Calls `CertificatesManager.setupSll(httpsServer, [host], ...)` → generates SNI cert if new host.
   - Returns `127.0.0.1` (redirect to local 4443).
3. Browser tunnels TLS to 4443; `KendarHttpsServer` terminates with the just-generated cert.
4. Decrypted request hits `MasterHandler` as plain HTTP.

New hosts trigger a live `SSLContext` rebuild on `HttpsServer` — all current connections keep their
old context; new handshakes get the updated one with the new SAN.

`CertificatesManager` uses a hardcoded root CA from `resource://certificates/ca.der` + `ca.key`
(overridable in settings). Certs valid ±360 days, RSA 2048, signed `SHA256WithRSA`.

`localhost` and `127.0.0.1` are always skipped in SAN registration.
Subdomains are wildcard-collapsed: `foo.bar.example.com` → `*.bar.example.com`.

---

## Mock matching algorithm (`HttpMockPlugin`)

Scoring system — highest score wins:

| Match | Points |
|---|---|
| Exact host | required (hard filter) |
| Exact path | +10000 |
| Path segment matches template `${var}` | +1000 |
| Path segment matches regex `@{pattern}` | +1000 |
| Query param key present | +1 |
| Query param exact value match | +3 |
| Query param `${var}` template | +1 |
| Query param `@{pattern}` regex | +2 |

After match, captured `${var}` values from path/query/headers are substituted into the
mock response body and headers via string replacement.

---

## Recursion guard

`BaseRequesterImpl` adds header `X-BLOCK-RECURSIVE: <full-url>` to every outgoing request.
`MasterHandler` checks for this header on incoming requests and returns 404 if the
host+path matches — prevents infinite proxy loops.

---

## Body handling (`BaseRequesterImpl`)

Priority order for outgoing request body:

1. Post parameters (form-urlencoded)
2. Multipart (`isMultipart` check)
3. Binary body (`BinaryNode`)
4. JSON-Smile (converts Jackson tree → Smile binary)
5. String entity (default)

All body types support `Content-Encoding: gzip` via `GzipCompressingEntity` wrapping.
Brotli (`br`) is detected but NOT compressed — only detected to avoid double-encoding.

---

## Settings (`HttpProtocolSettings`)

```
http    = 4080    # plain HTTP listener
https   = 4443    # HTTPS listener
proxy   = 9999    # HTTP CONNECT proxy
ssl.der = resource://certificates/ca.der
ssl.key = resource://certificates/ca.key
ssl.cname = C=US,O=Local Development,CN=local.org
ssl.hosts = []    # pre-registered SNI hosts
```

---

## Plugins

| Plugin | Phases | Notes |
|---|---|---|
| `HttpMockPlugin` | PRE_CALL | Scoring-based match; template substitution in response |
| `HttpReplayPlugin` | PRE_CALL | Replays recorded traffic |
| `HttpRecordPlugin` | POST_CALL | Saves request/response pairs |
| `HttpRewritePlugin` | PRE_CALL | Mutates request before forwarding |
| `HttpLatencyPlugin` | PRE_CALL | Injects sleep |
| `HttpErrorPlugin` | PRE_CALL | Returns configured error response |
| `HttpRateLimitPlugin` | CONNECT | Enforces rate limits |
| `HttpReportPlugin` | POST_CALL | Emits metrics/report data |
| `HttpRestPluginsPlugin` | CONNECT | Exposes plugin management REST API |
| `SSLDummyPlugin` | — | Placeholder for SSL host management |

---

## Notable quirks

- `HttpProtocol.isWrapper()` returns `true` and `getPort()` returns `0` — it bypasses the FSM
  engine entirely; the protocol framework treats it as a self-managed wrapper.
- `createContext(...)` returns `null` — no FSM `ProtoContext` is ever created.
- CORS headers (`Access-Control-Allow-*`) are force-injected on every response in `sendResponse`,
  unconditionally. This makes the proxy transparent to browser CORS checks.
- Hardcoded ignored hosts in `ProxyServer`: `static.chartbeat.com`, `detectportal.firefox.com`,
  `firefox.settings.services.mozilla.com`, `incoming.telemetry.mozilla.org`, `push.services.mozilla.com`.
  These silently drop connections without forwarding — intended to suppress browser telemetry noise.
- `ProxyServerHandler` only redirects port 443 HTTPS to 4443; non-443 HTTPS ports (8443, etc.)
  are forwarded directly to the real host, bypassing MITM.
- Apache HttpClient retry handler retries exactly once (`executionCount != 1`).
- `EventsQueue` used for dynamic SSL host add/remove — `SSLAddHostEvent` / `SSLRemoveHostEvent`
  allow other parts of the system to expand the MITM cert coverage at runtime.
