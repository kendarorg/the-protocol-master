## The FSM

To handle all the protocols i had to implement an easy way to handle a state
machine

## The "simple" FSM

This is a simple FSM without network involved

### An example protocol

This means that

* if ToIntOne canRun BytesEvent
    * Loop between the ToInt-s.
    * Back to the SwitchCase if after the ToIntThree the bytes cannot be run by ToIntOne
* If SequenceOne canRun BytesEvent
    * Try running SequenceTwo (optional), if it cannot be run go to SequenceThree
    * Run SequenceTree
    * After this the machine ends
* If at any moment Interrupt canRun BytesEvent, execute it and go back to normal

<pre>
@Override
    protected void initializeProtocol() {
        addInterruptState(new Interrupt(BytesEvent.class));
        initialize(new ProtoStateSwitchCase(
                        new ProtoStateWhile(
                                new ToIntOne(BytesEvent.class),
                                new ToIntTwo(BytesEvent.class),
                                new ToIntThree(BytesEvent.class)
                        ),
                        new ProtoStateSequence(
                                new SequenceOne(BytesEvent.class),
                                new SequenceTwo(BytesEvent.class).asOptional(),
                                new SequenceThree(BytesEvent.class)
                        )));
    }
</pre>

An example run can be the following:

* ToIntOne
* ToIntTwo
* Interrupt
* ToIntThree
* ToIntOne
* ToIntTwo
* Interrupt
* ToIntThree
* SequenceOne
* SequenceThree
* END

Notice that if the Sequence are run, the only possible path is the following, since
the root is a Switch, not a While

* SequenceOne
* (SequenceTwo)
* SequenceThree
* END

### Basic classes

First should implement the "ProtoContext". This will be the current status of
the FSM. The example will simply write all the results on the "result" array.

<pre>
    public class SillyContext extends ProtoContext {
        public List<ReturnMessage> getResult() {
            return result;
        }
    
        private List<ReturnMessage> result = new ArrayList<>();
        public SillyContext(ProtoDescriptor descriptor, Channel client) {
            super(descriptor);
        }
    
        public SillyContext(SillyProtocol descriptor) {
            super(descriptor);
        }
    
        protected void write(ReturnMessage returnMessage) {
            result.add(returnMessage);
        }
    }
</pre>

Then should add the "protocol" class

<pre>
    public class SillyProtocol extends ProtoDescriptor {

    @Override
    protected void initializeProtocol() {}

    @Override
    protected ProtoContext createContext(ProtoDescriptor protoDescriptor) {
        return new SillyContext(this);
    }
}
</pre>

### The states for the protocol

The states are triggered by events. The events are implementation of the
BaseEvent class. The event -ALWAYS- contain the context. An example event
is the following

<pre>
    public class SampleEvent extends BaseEvent {
        public SampleEvent(ProtoContext context, Class<?> prevState) {
            super(context, prevState);
        }
    }
</pre>

The States should implement the ProtoState. The classes passed to the second
constructors are the events that the state can handle. The states when created
check that the implementation contains the methods corresponding to the events
declared. THERE ARE NO INTERFACES FOR THE EVENTS!

<pre>
    public class SampleState extends ProtoState {
        public SampleState(){}

        public SampleState(Class<?> ... eventTypes) {
            super(eventTypes);
        }
    }
</pre>

For example declaring the following state on the state machine initialization

<pre>
    new SampleState(SampleEvent.class);
</pre>

Means that the SampleState implements the following methods.

* canRun: return true if the event is matching the state. It's useful when
  the events contain byte buffers and should check the content to verify if
  it is correct
* execute: run the action associated with the event. It returns an iterator of
  return messages (or ProtoStep) that will be sent to the ProtoContext::write
  method.

<pre>
        public boolean canRun(SampleEvent event){}
        public Iterator<ProtoStep> execute(SampleEvent event) {}
</pre>

### The ProtoSteps

The ProtoStep is essentially a supplier of ReturnMessage. This is to allow the
interruption of the execution at any point. In standard situations you simply
create a simple ReturnMessage/

<pre>
    public interface ProtoStep {
        ReturnMessage run();
    }
</pre>

There are utilities method to generate the proto steps iterator

* iteratorOfEmpty(): no data to return
* Iterator<ProtoStep> iteratorOfList(ReturnMessage... msg): to pass the list of
  return messages
* Iterator<ProtoStep> iteratorOfList(ProtoStep... steps): to pass the list of
  actions to

### Events

The ProtoContext has the special ProtoContext::send(BaseEvent) method.
If you start the context inside a Thread like the following, this is the only
good way to send data to it

<pre>
    var context = (SillyContext)protocol.createContext(protocol);
    new Thread(()->context.start()).start();
</pre>

In the SillyTest for example a ByteEvent is sent (even though in the test the
context is run step by step to check the outcome)

<pre>
    context.send(new BytesEvent(context, null, BBuffer.of(new byte[]{'1'})));
</pre>

### Collecting the data

A simple way to collect the production of the FSM is to override the ProtoContext::write
method, before we used an array of ReturnMessage but a concurrent queue emptied
by another thread can be used.

To retrieve the current state the ProtoContext::getCurrentState can be used