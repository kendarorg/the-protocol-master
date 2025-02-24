# Http(s) Protocol

You can access the protocol as a proxy (as configured) or use the "rewrites" to
define global rewriting of requests.

When accessing via proxy the site certificates are generated in runtime, instead
when accessing using a fake DNS server or the hosts file to redirect the requests
on localhost the sites to build must be added on the "ssl/hosts"

## Configuration

### General

* protocol: http (this is mandatory)
* http: http port for interception of calls (mostly with rewrite-plugin)
* https: https port for interception of calls (mostly with rewrite-plugin)
* proxy: proxy port (used to intercept -everything-)

### SSL

* der: root certificate path
* key: root certificate key path
* cname: root certificate cname
* hosts: list of pre generated dns to generate SSL site certificates

## Plugins

### record-plugin

The data will be stored in the global dataDir

* active: If it is active
* recordSites: list of matching ```hosts/path``` to record. When empty everything is recorded. When prepending with @
  uses regexp instead can use a simple string with * as wildcard
  else exact match
* `removeEtags`: default to true, cleans all ETag-related fields to avoid caching

### replay-plugin

The data will be loaded from the global dataDir. This is used to replay a whole flow
without the need to mock a single request

* active: If it is active
* matchSites: list of matching ```hosts/path``` to replay. When empty everything is replayed. When prepending with @
  uses regexp instead can use a simple string with * as wildcard
  else exact match
* respectCallDuration: respect the duration of the round trip
* blockExternal: default to true, block any calls to not recorded website

### error-plugin

Generate random errors

* active: If it is active
* errorSites: list of matching ```hosts/path``` to generate errors on. When empty everything can generate errors. When
  prepending with @
  uses regexp instead can use a simple string with * as wildcard
  else exact match
* showError: The error code to expose
* errorMessage: the error message to write to output
* errorPercent: the percent of calls to generate errors

### latency-plugin

Introduce random latency

* active: If it is active
* latencySites: list of matching ```hosts/path``` to apply latency on. When empty everything
  has latency. When prepending with @
  uses regexp instead can use a simple string with * as wildcard
  else exact match
* minMs: Minimum latency added (default 0)
* maxMs: Max latency added (default 0)

### rate-limit-plugin

Add the handling of throttling and rate limits

* active: If it is active
* limitSites: list of matching ```hosts/path``` to apply throttling on. When empty everything
  has throttling. When prepending with @
  uses regexp instead can use a simple string with * as wildcard
  else exact match
* headerLimit: The header for the limit count (default RateLimit-Limit)
* rateLimit: The limit count (default 120)
* costPerRequest: How much each request will consume from rateLimit (default 2)
* warningThresholdPercent: When reaching %of rate limit start warning
* headerRemaining: How many hits remaining (default RateLimit-Remaining)
* headerReset: Header for seconds/time till the reset of counters (default RateLimit-Reset)
* resetFormat: Format of the headerReset value (default secondsLeft, can be utcEpochSeconds)
* headerRetryAfter: Header for seconds after which should retry (default Retry-After)
* resetTimeWindowSeconds: When counters are reset
* useCustomResponse: If should use a custom response (see an [example](src/test/resources/ratelimitresponse.json))

The file is located into the "path" `[dataDir]/[protocol instance id]/[rate-limit-plugin]/response.json

### mock-plugin

To mock single requests

* active: If it is active

The mock files are exactly like the recorded files with an addition of a few fields

* nthRequest: run only from the nTh request (default 0)
* count: run for "count" times (set to 99999 if you want them all)

The mocks can be parametrized using ${variableName} format inside

* Query: setting a value to ${myQueryVariable}
* Header: setting a value to ${myHeaderVariable}
* Path: e.g. `/jsonized/${myPathVariable}/wetheaver`

These variable are assigned taking the value from the real request and are modified
inside the headers and the content (when textual). For example the mocked response
can be set to the following, same goes for the header variable.

```
  "output": {
    "responseText": "${myPathVariable} ${myQueryVariable} ${myHeaderVariable}",
```

This is useful to generate "dynamic" responses

The files are located into the "path" `[dataDir]/[protocol instance id]/[mock-plugin]

### rewrite-plugin

To change some call, for example to rewrite all call to `localhost/microservice.1/*` to `remoteservice.com/*`
This can be used to avoid configuring the proxy on the application

The recording will contain the target address!

* active: If it is active

The format, is the following. When settings a regexp the replacements (like $1 etc.)
can be used. Please remind that what follows the founded request is added at the end!

```
[
    {"toFind":"http://localhost/microservice.1",
    "toReplace":"https://remoteservice.com",
    "isRegex":false}
]
```

An example of complex regexp

```
    "toFind": "http://localhost/multireg/([a-zA-Z0-9]+)/test/([0-9]+)",
    "toReplace": "https://www.$1.com/test/$2",
    "regex": true
```

The file is located into the "path" `[dataDir]/[protocol instance id]/[rewrite-plugin]/rewrite.json

## Documentation used

* https://stackoverflow.com/questions/9357585/creating-a-java-proxy-server-that-accepts-https
* https://github.com/DanielChanJA/HttpProxy
* https://github.com/stefano-lupo/Java-Proxy-Server
* https://medium.com/@lbroudoux/generate-self-signed-certificates-in-pure-java-83d3ad94b75

## Interesting information

Unlike the other protocol this is not a "wire" protocol. It's instead based
on a custom reimplementation of standard Java HttpServer and HttpsServer to
allow changing the SSL certificates in runtime.

A custom http/s proxy had been used to mask external servers and allow
intercepting external resources like a `MITM` (Man In The Middle) attack.
