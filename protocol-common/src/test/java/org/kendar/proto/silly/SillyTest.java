package org.kendar.proto.silly;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.kendar.buffers.BBuffer;
import org.kendar.proto.fsm.*;
import org.kendar.protocol.descriptor.ProtoDescriptor;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateSwitchCase;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.utils.Sleeper;

import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class SillyTest {
    public static String result;

    @BeforeEach
    public void beforeEach(TestInfo testInfo) {
        SillyTest.result = "";
        LoopOne.run = true;
        LoopTwo.run = true;
        ChoiceOne.run = true;
        ChoiceTwo.run = true;
    }


    @Test
    void testExplode2() throws ExecutionException, InterruptedException {

        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return new ProtoStateWhile(
                        new ProtoStateWhile(
                                new LoopOne(BytesEvent.class),
                                new LoopTwo(BytesEvent.class)
                        ),
                        new ProtoStateSwitchCase(
                                new ChoiceOne(BytesEvent.class),
                                new ChoiceTwo(BytesEvent.class)
                        )
                );
            }
        };
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        LoopOne.run = false;
        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        ChoiceOne.run = false;
        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertArrayEquals(
                new String[]{"LoopOne", "LoopTwo", "LoopOne", "LoopTwo", "ChoiceOne", "ChoiceOne", "ChoiceTwo"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());
        ChoiceTwo.run = false;
        assertThrows(RuntimeException.class, () ->
                        context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'Z'})))
                , "Wrong message!");
        assertArrayEquals(
                new String[]{"LoopOne", "LoopTwo", "LoopOne", "LoopTwo", "ChoiceOne", "ChoiceOne", "ChoiceTwo"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());
    }

    @Test
    void testInterrupt() {
        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return new ProtoStateSwitchCase(
                        new ProtoStateWhile(
                                new ToIntOne(BytesEvent.class),
                                new ToIntTwo(BytesEvent.class),
                                new ToIntThree(BytesEvent.class)
                        ));
            }
        };
        protocol.addInterruptStateTest(new Interrupt(BytesEvent.class));
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));

        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'1'}))));


        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'2'}))));


        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'I'}))));

        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'3'}))));

        assertArrayEquals(
                new String[]{"ToIntOne", "ToIntTwo", "Interrupt", "ToIntThree"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());
    }

    @Test
    void testExplode() {
        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return
                        new ProtoStateSequence(
                                new ToIntOne(BytesEvent.class),
                                new ToIntTwo(BytesEvent.class),
                                new ToIntThree(BytesEvent.class)
                        );
            }
        };
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));

        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'1'}))));


        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'2'}))));


        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'3'}))));


        assertThrows(RuntimeException.class, () -> context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'3'}))), "Wrong message!");

        var state = context.getCurrentState();
        assertEquals("ToIntThree", state.getSimpleName());


        assertArrayEquals(
                new String[]{"ToIntOne", "ToIntTwo", "ToIntThree"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());
    }

    @Test
    void testThread() throws ExecutionException, InterruptedException {
        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return new ProtoStateSwitchCase(
                        new ProtoStateWhile(
                                new ToIntOne(BytesEvent.class),
                                new ToIntTwo(BytesEvent.class)
                        ),
                        new ProtoStateSwitchCase(
                                new ToIntThree(BytesEvent.class),
                                new ToIntFour(BytesEvent.class)
                        )
                );
            }
        };
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        Sleeper.sleep(500);
        assertTrue(context.send(new BytesEvent(context, null, BBuffer.of(new byte[]{'1'}))).get());
        Sleeper.sleep(500);
        var state = context.getCurrentState();
        assertEquals("ToIntOne", state.getSimpleName());

        assertTrue(context.send(new BytesEvent(context, null, BBuffer.of(new byte[]{'2'}))).get());
        Sleeper.sleep(100);
        state = context.getCurrentState();
        assertEquals("ToIntTwo", state.getSimpleName());
    }


    /*********************************************/
    /*********************************************/
    /*********************************************/

    @Test
    void testLoop() {
        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return new ProtoStateSwitchCase(
                        new ProtoStateWhile(
                                new LoopOne(BytesEvent.class),
                                new LoopTwo(BytesEvent.class)
                        ),
                        new ProtoStateSwitchCase(
                                new ChoiceOne(BytesEvent.class),
                                new ChoiceTwo(BytesEvent.class)
                        )
                );
            }
        };
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer()))); //l1

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer()))); //l2

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer()))); //l1

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer()))); //l2

        LoopOne.run = false;
        assertThrows(RuntimeException.class, () -> context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertArrayEquals(
                new String[]{"LoopOne", "LoopTwo", "LoopOne", "LoopTwo"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());

    }

    @Test
    void testLoop2() {
        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return new ProtoStateWhile(
                        new ProtoStateWhile(
                                new LoopOne(BytesEvent.class),
                                new LoopTwo(BytesEvent.class)
                        ),
                        new ProtoStateSwitchCase(
                                new ChoiceOne(BytesEvent.class),
                                new ChoiceTwo(BytesEvent.class)
                        )
                );
            }
        };
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        LoopOne.run = false;
        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        ChoiceOne.run = false;
        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));


        LoopOne.run = true;
        assertTrue(context.sendSync(new BytesEvent(context, null, new BBuffer())));

        assertArrayEquals(
                new String[]{"LoopOne", "LoopTwo", "LoopOne", "LoopTwo",
                        "ChoiceOne", "ChoiceOne", "ChoiceTwo",
                        "LoopOne"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());

    }

    @Test
    void testInterruptItem() {
        var protocol = new SillyProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return
                        new ProtoStateWhile(
                                new ToIntOne(BytesEvent.class),
                                new ToIntTwo(BytesEvent.class),
                                new ToIntThree(BytesEvent.class)
                        );
            }
        };
        protocol.addInterruptStateTest(new Interrupt(BytesEvent.class));
        protocol.initializeProtocol();
        var context = (SillyContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));

        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'1'}))));


        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'2'}))));


        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'I'}))));

        assertTrue(context.sendSync(new BytesEvent(context, null, BBuffer.of(new byte[]{'3'}))));

        assertArrayEquals(
                new String[]{"ToIntOne", "ToIntTwo", "Interrupt", "ToIntThree"}
                , context.getResult().stream().map(r -> r.getClass().getSimpleName()).toArray());
    }

}
