## APIs

Apis are exposed (only when required) on the localhost port specified by

* Global setting: apisPort
* Command line: -apis

### GET /api/global/shutdown

Shutdown the whole system gracefully

### GET /api/protocols

List all active protocols with the instanceId of the protocol

```
[
    {"id":"http-01","protocol":"http"},....
```

### GET /api/protocols/{protocolInstanceId}/plugins

List all the available plugins (with status) by protocol

```
[
    {"id":"record-plugin","active":false},....
```

### GET /api/protocols/{protocolInstanceId}/plugins/{pluginId}/{action}

Change/retrieve the status for the given plugin. Action can be

* start (returns '{"OK"}')
* stop (returns '{"OK"}')
* status (returns '{"active":false}')

