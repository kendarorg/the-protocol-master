# Postgres Protocol

You can directly use the "proxy" as a normal postgres backend
This protocol can be used as a "drop-in" replacement since the
"special queries" like commit, save points etc. are translate to JDBC and
not left as they are. So you can use this protocol to proxy and log a
connection even with Oracle or SQLServer

## Configuration

* protocol: redis (this is mandatory)
* port: the port on which the proxy will listen
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server (e.g. jdbc:postgresql://localhost:5432/db?ssl=false ) can
  be even another db!!
* timeoutSeconds: the timeout to drop the connections
* forceSchema: the force is called in case the jdbc driver does not allow setting the schema from connection string

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

These variable are assigned taking the value from the real request and are modified
inside the replaced response. For example the mocked response
can be set to the following

```
  "output": {
    "selectResult": {
      "records": [
        [
          "${denomination}",
          "${age}"
        ]
      ],
```

This is useful to generate "dynamic" responses

The files are located into the "path" `[dataDir]/[protocol instance id]/[mock-plugin]

### rewrite-plugin

To change some call, for example to rewrite all call to `SELECT * FROM XX` to `SELECT * FROM YY`
This can be used to avoid configuring the proxy on the application

The recording will contain the target call!

* active: If it is active

The format, is the following. When settings a regexp the replacements (like $1 etc.)
can be used.

```
[
    {"toFind":"SELECT * FROM ATABLE ORDER BY ID DESC",
    "toReplace":"SELECT * FROM ATABLE WHERE ID>100 ORDER BY ID DESC",
    "isRegex":false}
]
```

An example of complex regexp

```
    {"toFind":"SELECT * FROM ([a-zA-Z]+) WHERE ID=([0-9]+) ORDER BY ID DESC",
    "toReplace":"SELECT * FROM NEW_TABLE_$1 WHERE NEW_ID=$2 ORDER BY ID DESC",
    "regex": true
```

The files are located into the "path" `[dataDir]/[protocol instance id]/[rewrite-plugin]

### Connect to different servers

Leveraging on rewrite and mock plugin it is possible to connect to any kind of db
just replacing the queries (or adding them as mocks if tables are not available).

For example calling the following

```
java -cp "ojdbc11.jar;protocol-runner.jar" \
  org.kendar.Main \
  -cfg oracleSettings.json
```

and specifying the connection string for Oracle(TM) in the settings

```
  "connectionString":"jdbc:oracle:thin:@192.168.1.96:1521/FREEPDB",
```

And adding a couple of rewrites, can use an Oracle as a Postgres DB!!

If you are interested check the tutorial [Postgres to Oracle Bridge](../docs/postgres2oracleBridge.md)

## Missing features

* Real authentication (always allowed)
* `Savepoints` (NOT TRANSLATED TO JDBC)
* Replication protocol
* Messages
    * CancelRequest
    * CopyData (send/receive blobs)
    * Describe
    * Flush
    * FunctionCall

## Documentation used

* https://www.postgresql.org/docs/current/protocol-message-formats.html
* https://gavinray97.github.io/blog/postgres-wire-protocol-jdk-21
* https://www.instaclustr.com/blog/postgresql-data-types-mappings-to-sql-jdbc-and-java-data-types/
* https://beta.pgcon.org/2014/schedule/attachments/330_postgres-for-the-wire.pdf

## Interesting information

Several messages exist with the same code, but they have different directions

The indication on 0/1 or >1 on bind is fake. all work correctly

Always on bind the indication of the return columns is fake

### Generated keys and returning data

Something not specified on documentation is that when an Execute request is made
with an insert, the generated keys are returned according to the MaxRecords
field in the Execute message

When it's set to 0 (zero) then the data inserted CAN be returned without generating
errors. And will be returned all the inserted row. Given some unknown setting with the
execution with JPA it MUST be returned

When is set to 1 (one) then the data inserted MUST NOT be returned

### Sp and function calls (jdbc)

Calling sp or functions add around the call the following and set up the out param rebuilding everything
MANUALLY in Parser::modifyJdbcCall. The out parameter is diabolically reintegrated!

```
      prefix = "select * from ";
      suffix = " as result";
```

### Numeric format

`varlen` is not present in any of the 7.4 on-the-wire formats. According
to `numeric_recv`,

* External format is a sequence of int16's:
* `ndigits`, `weight`, `sign`, `dscale`, `NumericDigits`.

Some other relevant comments are

* The value represented by a NumericVar is determined by the sign, weight,
* `ndigits`, and `digits[]` array.
* Note: the first digit of a `NumericVar`'s value is assumed to be multiplied
* by `NBASE` ** weight. Another way to say it is that there are weight+1
* digits before the decimal point. It is possible to have weight < 0.


* `dscale`, or display scale, is the nominal precision expressed as number
* of digits after the decimal point (it must always be >= 0 at present).
* `dscale` may be more than the number of physically stored fractional
  digits,
* implying that we have suppressed storage of significant trailing zeroes.
* It should never be less than the number of stored digits, since that
  would
* imply hiding digits that are present. NOTE that `dscale` is always
  expressed
* in *decimal* digits, and so it may correspond to a fractional number of

```
  pq_sendint(&buf, x.ndigits, sizeof(int16));
  pq_sendint(&buf, x.weight, sizeof(int16));
  pq_sendint(&buf, x.sign, sizeof(int16));
  pq_sendint(&buf, x.dscale, sizeof(int16));
  for (i = 0; i < x.ndigits; i++)
  pq_sendint(&buf, x.digits[i], sizeof(NumericDigit));
```

Note that the "digits" are base-10000 digits.



