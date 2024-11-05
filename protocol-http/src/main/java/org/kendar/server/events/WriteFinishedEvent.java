package org.kendar.server.events;


import org.kendar.server.exchange.ExchangeImpl;

public class WriteFinishedEvent extends Event {
    public WriteFinishedEvent(ExchangeImpl t) {
        super(t);

        assert !t.writefinished;

        t.writefinished = true;
    }
}
