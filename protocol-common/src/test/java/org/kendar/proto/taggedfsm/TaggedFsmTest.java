package org.kendar.proto.taggedfsm;

import org.junit.jupiter.api.Test;
import org.kendar.protocol.context.Tag;
import org.kendar.protocol.states.ProtoState;
import org.kendar.protocol.states.special.ProtoStateSequence;
import org.kendar.protocol.states.special.ProtoStateWhile;
import org.kendar.protocol.states.special.Tagged;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TaggedFsmTest {
    private TaggedProtocol getProtocol() {
        return new TaggedProtocol() {
            @Override
            public ProtoState doTestInitialize() {
                return new ProtoStateSequence(
                        new TaggedState("A", TaggedEvent.class),
                        new TaggedState("B", TaggedEvent.class),
                        new Tagged(
                                Tag.ofKeys("LEV1"),
                                new ProtoStateWhile(
                                        new TaggedState("A1", TaggedEvent.class),
                                        new TaggedState("B1", TaggedEvent.class),
                                        new Tagged(
                                                Tag.ofKeys("LEV2"),
                                                new ProtoStateWhile(
                                                        new TaggedState("A2", TaggedEvent.class),
                                                        new TaggedState("B2", TaggedEvent.class),
                                                        new TaggedState("C2", TaggedEvent.class)
                                                )
                                        ),
                                        new TaggedState("C1", TaggedEvent.class)
                                )
                        ),
                        new TaggedState("C", TaggedEvent.class));
            }
        };
    }

    @Test
    void levelZeroTest() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C")));
        //Sequence, cannot run
        assertThrows(RuntimeException.class, () -> context.sendSync(new TaggedEvent(context, null, "A")));
    }

    @Test
    void levelOneTest() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.1")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "C")));
    }

    @Test
    void levelOneTestWithNoChildLevel() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.1")));
        //Cannot run befor the right time
        assertThrows(RuntimeException.class, () -> context.sendSync(new TaggedEvent(context, null, "A2", "LEV2", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.1")));
    }

    @Test
    void levelOneTestPrematureEnd() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.1")));
        //Parent not completed
        assertThrows(RuntimeException.class, () -> context.sendSync(new TaggedEvent(context, null, "C")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.1")));
    }


    @Test
    void levelTwoTest() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.2")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.2")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.2")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "C")));
    }

    @Test
    void levelTwoTestPremature() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.2")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.2")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.1")));

        //Not allowed (parent not completed)
        assertThrows(RuntimeException.class, () -> context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.2")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "B2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.2")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "C")));
    }

    @Test
    void levelTwoTestNotMatching() {
        var protocol = getProtocol();
        protocol.initializeProtocol();
        var context = (TaggedContext) protocol.createContext(protocol, protocol.getCounter("CONTEXT_ID"));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "A1", "LEV1", "1.2")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "B1", "LEV1", "1.2")));

        //Start on wrong tag
        assertThrows(RuntimeException.class, () -> context.sendSync(new TaggedEvent(context, null, "A2", "LEV2", "2.1")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "A2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.1")));

        //Existing tag but wrong level
        assertThrows(RuntimeException.class, () -> context.sendSync(new TaggedEvent(context, null, "B2", "LEV2", "2.1")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "B2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C2", "LEV1", "1.2", "LEV2", "2.1")));
        assertTrue(context.sendSync(new TaggedEvent(context, null, "C1", "LEV1", "1.2")));

        assertTrue(context.sendSync(new TaggedEvent(context, null, "C")));
    }
}
