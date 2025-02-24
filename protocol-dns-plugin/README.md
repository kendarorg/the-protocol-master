
# DNS Protocol

You can directly use the protocol on UDP or TCP

## Configuration

* protocol: dns (this is mandatory)
* port: the port on which the DNS server will listen (default 53)
* childDns: list of sub DNS, default 8.8.8.8 for docker can be 127.0.0.11
* registered: list of resolved names
* useCache: if should cache requests
* blocked: list of blocked domains

## Installation

Just copy into TPM plugins directory