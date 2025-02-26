package org.kendar.utils.parser;

import java.util.ArrayList;
import java.util.List;

public class Token {
    public TokenType type;
    public String value;
    public List<Token> children = new ArrayList<>();

    public Token(String value, TokenType type) {
        this.value = value;
        this.type = type;
    }

    public Token() {

    }

    public Token(TokenType type) {

        this.type = type;
    }

    @Override
    public String toString() {
        var ch = children.isEmpty() ? "" : ", children=" + children;
        var va = value == null || value.isEmpty() ? "" : ", value='" + value + '\'';

        return "Token{" +
                "type=" + type + va + ch +
                '}';
    }
}
