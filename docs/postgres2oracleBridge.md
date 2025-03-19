## Postgres 2 Oracle Bridge

This is a POC derived from an
issue [Add custom response files for db specific queries](https://github.com/kendarorg/the-protocol-master/issues/14)

The queries where used to expose on Grafana an Oracle DB.

Grafana only integrate Postgres driver.

You should add four rewrite files (name them as you please) containing each one object

<pre>
  {
    "toFind": "SELECT current_setting('server_version_num')::int/100 as version",
    "toReplace": "select 15 from dual",
    "regex": false
  }
</pre>

<pre>
  {
    "toFind": "select quote_ident(table_name) as \"table\" from information_schema.tables\n    where quote_ident(table_schema) not in ('information_schema',\n                             'pg_catalog',\n                             '_timescaledb_cache',\n                             '_timescaledb_catalog',\n                             '_timescaledb_internal',\n                             '_timescaledb_config',\n                             'timescaledb_information',\n                             'timescaledb_experimental')\n      and table_type = 'BASE TABLE' and \n          quote_ident(table_schema) IN (\n          SELECT\n            CASE WHEN trim(s[i]) = '\"$user\"' THEN user ELSE trim(s[i]) END\n          FROM\n            generate_series(\n              array_lower(string_to_array(current_setting('search_path'),','),1),\n              array_upper(string_to_array(current_setting('search_path'),','),1)\n            ) as i,\n            string_to_array(current_setting('search_path'),',') s\n          )",
    "toReplace": "SELECT table_name as \"table\" FROM dba_tables",
    "regex": false
  }
</pre>

<pre>
  {
    "toFind": "select quote_ident\\(column_name\\) as \"column\", data_type as \"type\"\n    from information_schema.columns\n    where quote_ident\\(table_name\\) = '([a-zA-Z_\\-$]+)'",
    "toReplace": "SELECT column_name as \"column\", data_type as \"type\" FROM DBA_TAB_COLUMNS WHERE TABLE_NAME = '$1'",
    "regex": true
  }
</pre>

<pre>
  {
    "toFind": "(.+) LIMIT ([0-9]+)",
    "toReplace": "select * from  ( $1 ) where ROWNUM <= $2",
    "regex": true
  }
</pre>

