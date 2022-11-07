package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;

/**
 * @author Black
 */
public class TokenKindGet {
    private Token token;
    private SourceCodeType type;

    public TokenKindGet(Token token, SourceCodeType type) {
        this.token = token;
        this.type = type;
    }

    public Token getToken() {
        return this.token;
    }

    public SourceCodeType getType() {
        return this.type;
    }
}
