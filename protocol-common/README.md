## DataTypesBuilder

Used to retrieve the list of types from the specific DB and mix them with the ones used internally
Up to the protocol to use it or not

## Interesting stuffs

The EventsQueue use a Thread instead of a SingleThreadExecutor because the internals of the
SingleThreadExecutor is based on the LinkedBlockingQueue that slow down the events parsing





