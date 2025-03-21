## TPMql

For the TPM APIs a new (yay!)

### Grammar

* value: String,Integer,Float,Array or Object
* variable: The path of an item on Json,
* function: A function, can return a `value`
* binaryOperator: One of `+ - * / == <= >= > < !=`

* functionCall: `function(expression*)`
* binaryFunction: `expression binaryOperator expression` returns `boolean`
* expression: `[value|variable|function|binaryFunction|functionCall]`
* group_ops: `MIN|MAX|AVG|COUNT`
* order_expression: `ASC|DESC(fieldname)`: valid only for strings and numbers
*

### Functions

In the select the expression are evaluated in this order

* WHERE
* GROUPBY
* WHAT
* ORDERBY

* `AND(expression_boolean*)` returns `boolean`
* `OR(expression_boolean*)` returns `boolean`
* `ISNULL(expression)` returns `boolean`
* `ISNOTNULL(expression)` returns `boolean`
* `NOT(expression_boolean` returns `boolean`
* `TRUE` returns `boolean_true` Constant
* `FALSE` returns `boolean_true` Constant
* `NULL` returns `null` Constant
* `CONTAINS(expression_string,expression_string_to_search_for)` returns `boolean`
* `CONCAT(expression_string*)` returns `string`
* `FILTER(expression_[object|array],expression_boolean)` returns `array`
    * For arrays the object passed to the second parameter is the item of the array, and it will be named `it`
    * For arrays the object passed to the second parameter is an object with `key` field with the name
      of the property and `value` containing the value of the property
* `COUNT(expression_[object|array|string])` returns `integer`
    * For arrays counts the item of the array
    * For objects count the properties
    * For strings count the length
    * If the object is null returns 0
* `SELECT(WHAT(),WHERE(),GROUPBY(),ORDERBY())` returns a list, all parameters optional
* `WHAT(fieldname|fieldname=expression|group_ops(fieldname)*)` select fieldname, or assign to fieldname the value of the
  expression
* `WHERE(boolean_expression)` execute a query
* `GROUPBY(fieldname*)` all fields of select must be in format `variable=expression` where `expression` can be the
  variable itself e.g. `tags.path=tags.path`
* `ORDERBY(order_expression*)`
* `SUBSTR(expression,integer)`: substring, if done with objects and array, first serializes to json string
* `MSTODATE(expression_string)`: convert to readable date in format YYYY/MM/DD HH:MM:SS.SSSS
* `WRAP(expression,integer,string)`: wrap the string in blocks of integer length using string as separator

### Examples

Given the object

`
{"biglist":[
    {
        "name":"TestObject"
        "list": [
            {
                "id":1,
                "name":"First",
                "enabled":true
            },
            {
                "id":2,
                "name":"Second"
            }
        ],
        "data": {
            "description":"Description",
            "cost":2.22
            "enabled":true
        }
    },
    ...
}}
`

* On the first object directly as input:
    * Checking if the object has `data.cost=2.22` directly on the first object
        * `data.cost==2.22`
        * `COUNT(FILTER(data,AND(key=='cost',value==2.22)))>0`
* On the list
    * Checking if an object exists with `data.value=2.22` directly on the list of object
        * `COUNT(FILTER(biglist,COUNT(FILTER(it,it.data.value==2.22))>0))>0`

