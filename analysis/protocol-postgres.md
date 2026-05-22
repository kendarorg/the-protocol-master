# protocol-postgres analysis

## What it is

PostgreSQL wire protocol proxy (port 5432, big-endian). Sits between PG clients and a real database,
enabling record/replay/mock/rewrite/forward plugins.

---

## Architecture

```
Client TCP
    │
    ▼
PostgresPacketTranslator  (interrupt state: BytesEvent → PostgresPacket)
    │
    ▼
FSM (PostgresProtocol.initializeProtocol)
  ┌─ SSLRequest (optional)
  ├─ SSLHandshake (optional)
  ├─ StartupMessage          → AuthOk + ParameterStatus + BackendKeyData + ReadyForQuery
  ├─ PasswordMessage (opt)
  └─ while loop / switch:
       ├─ Query (Q)                      ← simple query path
       ├─ Parse + Bind + Describe + Execute  ← extended query path
       ├─ Bind + Execute
       ├─ Sync (S)
       ├─ Close (C)
       └─ Terminate (X)
```

---

## Key packages

| Package | Role |
|---|---|
| `fsm/` | One class per wire message type; each reads `BBuffer`, mutates context, queues responses |
| `executor/` | `PostgresExecutor` routes SQL to real DB or fake handlers; handles BEGIN/COMMIT/ROLLBACK/isolation |
| `executor/converters/` | `PostgresDataConverter` — binary↔Java via `org.postgresql.ByteConverter`; `PostgresCallConverter` — `$1`→`?` JDBC rewrite |
| `messages/` | Wire response builders (RowDescription, DataRow, CommandComplete, ErrorResponse, …) |
| `plugins/` | Forward, Mock, Record, Replay, Rewrite, Latency, NetError, Report, RestPlugins |
| `dtos/` | `Parse` (statement name + query + OIDs), `Binding` (portal + param values), `Field` |
| `constants/` | `TypesOidsConstants` — OID numbers; runtime mapping loaded from `/postgresdtt.json` |

---

## Extended query sync pattern

`Parse` / `Bind` / `Execute` don't send responses directly — they call `postgresContext.addSync(iterator)`.
`Sync` flushes everything at once via `clearSync()` + appends `ReadyForQuery`.
This matches the PG spec's pipeline flushing model.

---

## Type resolution

1. Client sends OID per parameter in `Parse` message.
2. OID `0` (unknown) → `inferMetadataIfPossible()` calls `PreparedStatement.getParameterMetaData()` on the real connection.
3. Binary-format params decoded by `PostgresDataConverter.bytesToJava()` using `org.postgresql.ByteConverter`.
4. Text-format temporal types parsed by regex (strips timezone offset before passing to `java.sql.*`).

---

## What's missing (from TODO.md)

- **No COPY protocol** (CopyData / CopyDone / CopyFail / CopyIn / CopyOut)
- **No Flush message** — clients expecting partial flushes will hang
- **No FunctionCall / FunctionCallResponse**
- **No PortalSuspended** — cursor-based streaming broken
- **No Savepoints**
- **BEGIN with isolation specifier** (`BEGIN ISOLATION LEVEL ...`) not parsed
- **Binary result rows** — fields always sent as text
- **Stored procedure OUT params** not saved after execute
- **Async cancel** — `CancelRequest` sets flag but `executeQuery` doesn't poll it

---

## Notable quirks

- `StartupMessage` advertises `server_version=15` and `server_type=JANUS` regardless of actual backend.
- `FIXED_SECRET = 5678` hardcoded — `CancelRequest` validation uses pid+secret; fine as long as cancel is intra-process only.
- `SetHandler` is the only active fake query handler; several are commented out (pg_type, pg_namespace, current_schema). These would be needed for JDBC drivers that introspect catalog tables on connect.
- `handleWithinTransaction` wraps multi-statement simple queries in an implicit transaction but uses `maxRecords=1` and `describable=false` for each sub-statement — could lose rows for multi-row INSERTs in a batch.
