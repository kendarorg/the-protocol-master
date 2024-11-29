## The properties.json

All configurations can be stored in a json file.

The first section contains the main settings:

* pluginsDir: The directory containing custom plugins loaded (not activated) at startup
* logLevel: The main log level
* dataDir: Where the recording/replaying data is founded
* apiPort: Where the main apis are listening

```
{
  "pluginsDir": "plugins",
  "logLevel": "INFO",
  "dataDir": "data",
  "apiPort": 8095,
  ...
```

Here can be set all the protocol used multiple times. The name of the section will
be appended to the registration/loaded for replay, but keeping all the calls in
order (to understand interactions)

For the specific settings and supported plugin search on the protocols

```
{
    ...
    "protocols": {
        "postgres-01": {
          "protocol": "postgres",
          "port": 5432,
          "login": "remotelogin",
          "password": "remotepassword",
          "timeout": 30,
          "connectionString": "jdbc:mysql://localhost:3306"
        },
        "mqtt-01": {
          "protocol": "mqtt",
          "port": 1885,
          "login": "remotelogin",
          "password": "remotepassword",
          "timeout": 30,
          "connectionString": "tcp://localhost:1885"
        },
        "http-01": {
          "protocol": "http",
          "http": 8085,
          "https": 8485,
          "proxy": 9999,
          ...
        }
    }
}

```

Each protocol can have several plugins, the section id defines the plugin to load.
The plugins can be activated or deactivated via [apis](apis.md).

```
    "http-01": {
      "protocol": "http",
      ...
      "plugins": {
        "error-plugin": {
          "errorCode": 0,
          "errorMessage": "ERROR",
          "percentage": 0
        },
        "replay-plugin": {
          "replay": false,
          "replayId": "replay{timestamp}",
          "respectCallDuration": true,
          "blockExternal": true,
          "matchSites": []
        },
        "record-plugin": {
          "record": false,
          "recordSites": []
        }
```
