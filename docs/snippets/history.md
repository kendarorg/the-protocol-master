
* Create a state machine able to interpret a generic wire protocol, handling
  special situations like incomplete messages, sanding and receiving data according
  to byte ordering and formatting
* Translate the queries in a standard, serializable format, logging all queries
  and results in a consistent way
* Forward wire protocol to drivers. For SQL means passing the queries to JDBC drivers,
  for NO-SQL forwarding to the specific ones like MongoDB or AMQP
* Run queries against a pre-recorded sequence of commands to simulate a real data
  storage, without the need of a real server (in the making)
* Adding custom Java plugins

For this to become real an event based state machine has been developed, with
several database wire protocol implementations: