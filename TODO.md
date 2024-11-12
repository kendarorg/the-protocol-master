## TODO

### Docs

* Replacequery example
* Change all the parameter names
* Explain filter

### Code

V Mysql DataTypesPsTest
* verifify the serializator
* Recording and replay plugin should use the right storage
  V Use anonymous "Object" instead of JsonNode for saving
  V Check if should use filters even for Protocol
* Add an api to list the plugins
* Add an api for start/stop record/replay/mock
* For mock set the nth call
* Remove any reference to spring boot
  V respectCallDuration on http
* User replayId (with timestamp)
* Store Jsons as json in Request/Response/Multipart
* Store other stuffs as external objects in Request/Response/Multipart
* Publish on maven central
* Change Supplier<Boolean> stopWhenFalseAction to a callback to register for
  V KENDARMYSQLPREPARESTATEMENT

STATE CHARTS:

* https://www.ascii-code.com/
* https://github.com/klangfarbe/UML-Statechart-Framework-for-Java
* https://github.com/klangfarbe/UML-Statechart-Framework-for-Java
* https://www.graphviz.org/
* https://en.wikipedia.org/wiki/DOT_(graph_description_language)