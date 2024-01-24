# MySQL Protocol

You can directly use the "proxy" as a normal mysql backend

## Prepared statement

By default the JDBC driver emulates the prepared statements client side.
If you want to use prepared statement, on the proxy connection string should set

<pre>
    ?generateSimpleParameterMetadata=true&useServerPrepStmts=true
</pre>

And on the fake db connection string

<pre>
    ?useServerPrepStmts=true
</pre>

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



