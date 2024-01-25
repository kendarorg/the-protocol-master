# Postgres Protocol

You can directly use the "proxy" as a normal postgres backend
This protocol can be used as a "drop-in" replacement since the
"special queries" like commit, savepoints etc are translate to JDBC and
not left as they are. So you can use this protocol to proxy and log a
connection even with Oracle or SQLServer

## Missing features

* Real authentication (always allowed)
* Savepoints (NOT TRANSLATED TO JDBC)
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

## Interesting informations

Several messages exists with the same code, but they have different directions

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

Calling sp or functions add around the call the following and setup the out param rebuilding everything -BY HAND-
in Parser::modifyJdbcCall. The out parameter is diabolically reintegrated!

<pre>
      prefix = "select * from ";
      suffix = " as result";
</pre>

### Numeric format

varlen is not present in any of the 7.4 on-the-wire formats. According
to numeric_recv,

* External format is a sequence of int16's:
* ndigits, weight, sign, dscale, NumericDigits.

Some other relevant comments are

* The value represented by a NumericVar is determined by the sign, weight,
* ndigits, and digits[] array.
* Note: the first digit of a NumericVar's value is assumed to be multiplied
* by NBASE ** weight. Another way to say it is that there are weight+1
* digits before the decimal point. It is possible to have weight < 0.


* dscale, or display scale, is the nominal precision expressed as number
* of digits after the decimal point (it must always be >= 0 at present).
* dscale may be more than the number of physically stored fractional
  digits,
* implying that we have suppressed storage of significant trailing zeroes.
* It should never be less than the number of stored digits, since that
  would
* imply hiding digits that are present. NOTE that dscale is always
  expressed
* in *decimal* digits, and so it may correspond to a fractional number of

<pre>
  pq_sendint(&buf, x.ndigits, sizeof(int16));
  pq_sendint(&buf, x.weight, sizeof(int16));
  pq_sendint(&buf, x.sign, sizeof(int16));
  pq_sendint(&buf, x.dscale, sizeof(int16));
  for (i = 0; i < x.ndigits; i++)
  pq_sendint(&buf, x.digits[i], sizeof(NumericDigit));
</pre>

Note that the "digits" are base-10000 digits.



