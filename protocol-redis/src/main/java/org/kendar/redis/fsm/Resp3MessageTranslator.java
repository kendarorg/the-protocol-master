package org.kendar.redis.fsm;

import org.kendar.buffers.BBuffer;
import org.kendar.exceptions.AskMoreDataException;
import org.kendar.protocol.events.BytesEvent;
import org.kendar.protocol.messages.NetworkReturnMessage;
import org.kendar.protocol.messages.ProtoStep;
import org.kendar.protocol.states.InterruptProtoState;
import org.kendar.protocol.states.ProtoState;
import org.kendar.redis.fsm.events.Resp3Message;
import org.kendar.redis.parser.Resp3Input;
import org.kendar.redis.parser.Resp3ParseException;
import org.kendar.redis.parser.Resp3Parser;

import java.util.Iterator;
import java.util.List;

public class Resp3MessageTranslator  extends ProtoState implements NetworkReturnMessage, InterruptProtoState {
    private Resp3Parser parser = new Resp3Parser();
    public Resp3MessageTranslator() {
        super();
    }


    public Resp3MessageTranslator(Class<?>... events) {
        super(events);
    }
    @Override
    public void write(BBuffer resultBuffer) {

    }

    public boolean canRun(BytesEvent event) {
        var rb = event.getBuffer();
        var oldPos = rb.getPosition();
        rb.setPosition(0);
        var allBytes = rb.getAll();
        if(allBytes.length==0){
            return false;
        }
        var str = new String(allBytes);
        var input = Resp3Input.of(str);
        try{
            Object result = parser.parse(input);
            rb.setPosition(0);
        }catch(Resp3ParseException ex){
            if(ex.isMissingData()){
                rb.setPosition(0);
                throw new AskMoreDataException();
            }
        }
        return true;
    }

    public Iterator<ProtoStep> execute(BytesEvent event) {
        var rb = event.getBuffer();
        var oldPos = rb.getPosition();
        rb.setPosition(oldPos);
        var allBytes = rb.getRemaining();
        var str = new String(allBytes);
        var input = Resp3Input.of(str);
        List<Object> result = null;
        try{
            result = (List<Object>)parser.parse(input);
            rb.setPosition(oldPos+input.getIndex());
        }catch(Resp3ParseException ex){
            if(ex.isMissingData()){
                rb.setPosition(0);
                throw new AskMoreDataException();
            }
        }

        event.getContext().send(new Resp3Message(event.getContext(), event.getPrevState(), result));
        return iteratorOfEmpty();
    }
}
