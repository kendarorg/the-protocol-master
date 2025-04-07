# The Rest-Plugins plugin

This plugin allows to intercept every call made to every protocol and to modify
it within your preferred environment

You write your server code, setup it on TPM and that's it

## Your server

* Create a REST api able to answer to POST request. e.g. 'http://host:91/callinterceptor'
* Deserialize the JSON of the request and parse input and proposed output (when present)
* Do your operations and eventually modify the output
* Respond with the output, error and blocking flags as needed

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

* inputType: The simple class name of the input object, Object for any (included null). For DB calls can use `JdbcCall`, for Http/s `Request`
* outputType: The simple class name of the output object, Object for any (included null). For DB calls can use `SelectResult`, for Http/s `Response`
* phase: the protocol phase

### Response

And the response

* blocking: If the protocol should respond directly with the response given
* message: The JSON serialized object of the response. For DB calls can use `SelectResult`, for Http/s `Response`
* withError: True when there is an error
* error: The error message

```
{
    "blocking":true,
    "message":"",
    "error":"",
    "withError":false
}
```

### An example with Http/s

* The inputType would be Request, the output Response
* The phase will be `POST_CALL`, that means the original server had been called already
* Then we can deserialize the Request json (simplified)
* If it's from google (in pseudocode `input.host contains google`)
* Then change int the `output.responseText` all the "google.png" into "bing.png"
* Re-serialize the output to JSON and send it into `message` field

```
{
  "input": {
    "host": "www.google.com",
    ...
  },
  "outuput:{
   "responseText": "<html><body><img src='google.png'/></body></html>
  },
  "inputType": "Request",
  "outputType": "Response",
  "phase":"POST_CALL"
  ...
}
```

## TPM Side

Just set in the settings file the relative interceptor where

* name: Mnemonic for the interceptor
* phase: The phase (`POST_CALL` in this case)
* destinationAddress: The API to call
* inputType: `Request`
* outputType: `Response`
* inMatcher: The matcher for the input content, can be a 
  * regexp, prepend `@`
  * [tpmQl](tpmql.md), prepend `!`
  * contains, just the string that should be founded

If you need it exists even the outputMatcher that follows the same rules

```
{
    "protocols":{
        "http-01":{
            "plugins":{
                "rest-plugins-plugin": {
                  "interceptors": [
                    {
                      "name": "MyInterceptor",
                      "phase": "POST_CALL",
                      "destinationAddress": "http://host:91/callinterceptor",
                      "inputType": "Request",
                      "outputType": "Response",
                      "inMatcher": "!CONTAINS(host,'google')"
                    }
                  ]
                },
        
        }
    }
}

```