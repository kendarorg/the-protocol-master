# MySQL Protocol

You can directly use the "proxy" as a normal mysql backend

## Configuration

* protocol: redis (this is mandatory)
* port: the port on which the proxy will listen
* login: the -real- login to use to connect to the real server
* password: the -real- password to use to connect to the real server
* connectionString: the connection string for the real server (e.g. jdbc:mysql://localhost:5432/db?ssl=false )
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

### mock-plugin

To mock single requests

* active: If it is active
* dataDir: The directory where the mock will be stored

The mock files are exactly like the recorded files with an addition of a few fields

* nthRequest: run only from the nTh request (default 0)
* count: run for "count" times (set to 99999 if you want them all)

The mocks can be parametrized using ${variableName} format inside

* Query: e.g. SELECT ADDRESS,AGE FROM COMPANY_R WHERE DENOMINATION='${denomination}' AND AGE=${age}
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

### rewrite-plugin

To change some call, for example to rewrite all call to localhost/microservice.1/* to remoteserice.com/*
This can be used to avoid configuring the proxy on the application

The recording will contain the target address!

* active: If it is active
* rewritesFile: the json file containing the rewrites

The format, is the following. When settings a regexp the replacements (like $1 etc)
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

## Missing features

* Real authentication (always allowed)
* Multi-Resultset
* Multi-Statement
* Replication Protocol
* Commands
    * COM_QUIT
    * COM_STATISTICS
    * COM_DEBUG
    * COM_CHANGE_USER
    * COM_RESET_CONNECTION
    * COM_SET_OPTION
    * COM_STMT_FETCH
    * COM_STMT_CLOSE
    * COM_STMT_RESET
    * COM_STMT_SEND_LONG_DATA (send/receive blobs)
* Deprecated commands
    * COM_FIELDS_LIST (from 5.7.11)
    * COM_REFRESH (from 5.7.11)
    * COM_PROCESS_INFO  (from 5.7.11)
    * COM_PROCESS_KILL  (from 5.7.11)

## Documentation used

* https://dev.mysql.com/doc/dev/mysql-server/latest/PAGE_PROTOCOL.html
* https://mariadb.com/kb/en/clientserver-protocol/
* https://github.com/colinnewell/pcap2mysql-log
* https://github.com/zhuchuangang/mysql-protocol
* https://github.com/mneri/mysql-proto
* https://www.turing.com/blog/understanding-mysql-client-server-protocol-using-python-and-wireshark-part-1/
* https://www.turing.com/blog/understanding-mysql-client-server-protocol-using-python-and-wireshark-part-2/
* https://mysqlcode.com/mysql-blob-datatype-and-jdbc/
* https://github.com/fnmsd/MySQL_Fake_Server/tree/master/mysqlproto
* https://github.com/kelsin/mysql-mimic
* https://learning.oreilly.com/library/view/understanding-mysql-internals/0596009577/ch04.html
* https://clickhouse.com/codebrowser/ClickHouse/contrib/mariadb-connector-c/include/mariadb_com.h.html#319

## Interesting informations

MYSQL_TYPE_MEDIUMINT seems not existing (from docs)

Several options (like the options to avoid sending metadata for the queries) are
not considered by the 8.x connector

When using aliases the column name returned by the column descriptor MUST be
the alias AND NOT THE COLUMN NAME

The packet number is used to define the steps of the "subprotocols" (e.g. query
and its response)

### Data types mess

This happens only on binary protocol and -real- prepared statements (that are
used extensively by ODBC and .NET drivers)

During the conversions, from/to wire protocol, the data types returned by JDBC
drivers ARE NOT MATCHING WITH MySQL types. The smallest possible type is used
e.g.

* A Double field is set for the prepared statement, but it could be fitting in a Float
* The driver uses a Float!
* Everything is messed up :P

Double and floats follows the IEEE 754 floating-point value in Little-endian
format on 8 or 4 bytes.

When using the binary protocol (prepared statements)...everything is transmitted
as string.. as with text protocol (cached statement) there is now no point in
using that approach. Only the parameters of the PS are sent in "compact mysql format"

### Generated keys and returning data

When doing an INSERT if the generated key is a LONG/INT then should be returned
-ONLY- the new value without a real recordset

### Prepared statement

By default the JDBC driver emulates the prepared statements client side.
If you want to use prepared statement, on the proxy connection string should set

```
  ?generateSimpleParameterMetadata=true&useServerPrepStmts=true
```

And on the fake db connection string

```
  ?useServerPrepStmts=true
```


