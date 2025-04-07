# The Rest-Plugins plugin

This plugin allows to intercept every call made to every protocol and to modify
it within your preferred environment

## Your server

* Create a REST api able to answer to POST request. e.g. 'http://host:91/callinterceptor'

### Request
* The request will be in the following format

```
{
    "inputType":"",
    "outputType":"",
    "phase":"",
    "input":"",
    "output:""
}
```

Where

* inputType: The simple class name of the input object (e.g. Request for http or Query for postgres), Object for any (included null)
* outputType: The simple class name of the output object (e.g. Response for http or Query for postgres), Object for any (included null)

### Response

* And the response

```
{
    "blocking":true,
    "message":"",
    "error":"",
    "withError":false
}
```