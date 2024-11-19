package org.kendar.server.events;


import org.kendar.server.exchange.ExchangeImpl;

public class Event {

    public final ExchangeImpl exchange;

    protected Event(ExchangeImpl t) {
        this.exchange = t;
    }
}
