## APIs

Apis are exposed (only when required) on the localhost port specified by

* Global setting: apisPort
* Command line: -apis

### GET /api/global/shutdown

Shutdown the whole system gracefully

### GET /api/storage/download

Download a zip file with all the recordings

### POST /api/storage/upload

Upload a zip file with all the recordings

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

## Protocol http

### GET /api/protocols/{protocolInstanceId}/plugins/ssl-plugin/der

Download the der certificate file

### GET /api/protocols/{protocolInstanceId}/plugins/ssl-plugin/key

Download the key certificate file

