# MS SQL Server Protocol

You can directly use the "proxy" as a normal SQL Server backend, speaking
the native TDS (Tabular Data Stream, MS-TDS) protocol on port 1433.
The "special queries" like the implicit transaction batches sent by the
drivers are translated to JDBC calls, so it can be used to proxy, record
and replay any SQL Server connection (or even forward to another jdbc db).

## Configuration

* protocol: mssql (this is mandatory)
* port: the port on which the proxy will listen (default 1433)
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server
  (e.g. jdbc:sqlserver://localhost:1433;encrypt=false;databaseName=master )
* timeoutSeconds: the timeout to drop the connections
* forceSchema: the force is called in case the jdbc driver does not allow setting the schema from connection string
* useTls: if true the proxy answers ENCRYPT_ON in the PRELOGIN negotiation
  and upgrades the connection to TLS (the handshake records are wrapped in
  TDS PRELOGIN packets as the protocol mandates). When false the proxy
  answers ENCRYPT_NOT_SUP, so the clients must connect with `encrypt=false`

Uses the following phases

* CONNECT (Before connecting to the real server)
* PRE_CALL (Before calling the real server)
* POST_CALL
* PRE_SOCKET_WRITE (Before sending data to the client)

## Supported features

* PRELOGIN/LOGIN7 SQL authentication (the credentials provided by the client
  are accepted, the proxy connects with the configured ones, like mysql/postgres)
* TLS encryption (mssql-jdbc default `encrypt=true;trustServerCertificate=true`)
  and plaintext (`encrypt=false`)
* SQL Batch queries
* RPC prepared statements: sp_executesql, sp_prepare, sp_execute,
  sp_prepexec, sp_unprepare
* Transactions (both the Transaction Manager requests and the driver
  "IF @@TRANCOUNT > 0 ..." batches)
* Attention (query cancel)
* Core data types: int family, bit, float/real, decimal/numeric,
  [n]varchar/[n]char, varbinary, date/time/datetime/datetime2,
  uniqueidentifier, money

## Missing features

* ENCRYPT_OFF semantics (login-only encryption): the negotiation always
  answers full ENCRYPT_ON or ENCRYPT_NOT_SUP
* MARS (Multiple Active Result Sets)
* Bulk load (packet type 0x07)
* NTLM/Kerberos/FedAuth/Always-Encrypted authentication
* Table-valued parameters
* Output parameters (beyond the prepared statement handles)
* NBCROW (null bitmap compressed rows, plain ROW tokens are emitted)
* Server side cursors (sp_cursoropen answers with an error so the drivers
  fall back to regular execution)
* XML/sql_variant types

Reference: [MS-TDS specification](https://learn.microsoft.com/en-us/openspecs/windows_protocols/ms-tds/)

## Plugins

### record-plugin

The data will be stored in the global dataDir.

* active: If it is active
* ignoreTrivialCalls: store in full only calls that cannot be generated automatically (the ones with real data)

### replay-plugin

The data will be loaded from the global dataDir. This is used to replay a whole flow
without the need to mock a single request

* active: If it is active
* respectCallDuration: respect the duration of the round trip
* blockExternal: Block calls to real service when not matching (default true)

### mock-plugin

To mock single requests

* active: If it is active

The mock files are exactly like the recorded files with an addition of a few fields

* nthRequest: run only from the nTh request (default 0)
* count: run for "count" times (set to 99999 if you want them all)

The mocks can be parametrized using ${variableName} format inside

* Query: e.g. `SELECT ADDRESS,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination}' AND AGE=${age}`
* Parameter: setting a value to ${myParameterVariable}

The files are located into the "path" `[dataDir]/[protocol instance id]/[mock-plugin]

### rewrite-plugin

To change some call, for example to rewrite all call to `SELECT * FROM XX` to `SELECT * FROM YY`
This can be used to avoid configuring the proxy on the application

The recording will contain the target call!

* active: If it is active

```
[
    {"toFind":"SELECT * FROM ATABLE ORDER BY ID DESC",
    "toReplace":"SELECT * FROM ATABLE WHERE ID>100 ORDER BY ID DESC",
    "isRegex":false}
]
```

The files are located into the "path" `[dataDir]/[protocol instance id]/[rewrite-plugin]

### rest-plugins-plugin

This plugin is used to intercept protocol calls and forward the request to a REST API
that will need to respond with the correct response data. Contains a list of "interceptors"
definitions. For details on the implementation [here](../docs/rest-plugins-plugin.md)

* name: The name of the interceptor
* destinationAddress: The api to call (POST)
* inputType: The expected input type (simple class name), Object for any
* inMatcher: The matcher for the in content, `@` for Java regexp, `!` for [tpmql](../docs/tpmql.md), generic string from contains
* outputType: The expected output type (simple class name), Object for any
* outMatcher: The matcher for the out content, `@` for Java regexp, `!` for [tpmql](../docs/tpmql.md), generic string from contains
* blockOnException: If there is an exception return the error and stop the filtering

### latency-plugin

Introduce latency on the responses

* active: If it is active
* minMs: Minimum latency added (default 0)
* maxMs: Max latency added (default 0)

### network-error-plugin

Introduce network errors (garbage bytes) on the responses

* active: If it is active
* percentAction: The percentage of calls to broke

## Example

Create a local mssql "forward proxy" to a real one and record everything

```
java -jar protocol-runner.jar -protocol mssql -port 1433 \
    -connection "jdbc:sqlserver://REALSERVER:1433;encrypt=false" \
    -login sa -password mypassword \
    -record
```

then connect with any client, e.g.

```
sqlcmd -S localhost,1433 -U sa -P mypassword -C
```

or an mssql-jdbc based application with

```
jdbc:sqlserver://localhost:1433;encrypt=false;trustServerCertificate=true
```
