# DNS Protocol

You can directly use the protocol on UDP or TCP

## Configuration

* protocol: dns (this is mandatory)
* port: the port on which the DNS server will listen (default 53)
* childDns: list of sub DNS, default 8.8.8.8 for docker can be 127.0.0.11. You can even use the name of a server, but it
  should be resolvable by the system dns
* registered: list of resolved names it's a list of
    * ip: the ip that should be resolved to
    * name: the dns linked to the ip. If it starts with `@` it will be a regexp
* useCache: if should cache requests
* blocked: list of blocked domains. If it starts with `@` it will be a regexp

## Installation

Just copy into TPM plugins directory