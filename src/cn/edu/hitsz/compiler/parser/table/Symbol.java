package cn.edu.hitsz.compiler.parser.table;

import cn.edu.hitsz.compiler.lexer.Token;

/**
 * @author Black
 */
public class Symbol {
    private Token token;
    private NonTerminal nonTerminal;

    private Symbol(Token token, NonTerminal nonTerminal) {
        this.token = token;
        this.nonTerminal = nonTerminal;
    }

    public Symbol(Token token) {
        this(token, null);
    }

    public Symbol(NonTerminal nonTerminal) {
        this(null, nonTerminal);
    }

    public boolean isToken() {
        return this.token != null;
    }

    public boolean isNonterminal() {
        return this.nonTerminal != null;
    }
}
