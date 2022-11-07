package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SourceCodeType;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.symtab.SymbolTableEntry;

import java.util.Stack;

// TODO: 实验三: 实现语义分析
public class SemanticAnalyzer implements ActionObserver {

    private Stack<TokenKindGet> attributeStack = new Stack<>();
    private SymbolTable symbolTable;

    @Override
    public void whenAccept(Status currentStatus) {
        // TODO: 该过程在遇到 Accept 时要采取的代码动作
        // 到达接收状态则清空状态栈
        for (int i = 0; i < attributeStack.size(); i++) {
            attributeStack.pop();
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO: 该过程在遇到 reduce production 时要采取的代码动作
        int term = production.body().size();
        int index = production.index();
        if (index == 5) {

            TokenKindGet attribute = attributeStack.pop();
            TokenKindGet newAttribute = new TokenKindGet(null, attribute.getType());
            attributeStack.push(newAttribute);
        } else if (index == 4) {
            TokenKindGet tokenAttribute = attributeStack.pop();
            TokenKindGet typeAttribute = attributeStack.pop();
            // 修改符号表
            SymbolTableEntry symbolTableEntry = symbolTable.get(tokenAttribute.getToken().getText());
            symbolTableEntry.setType(typeAttribute.getType());
            attributeStack.push(null);
        } else {
            for (int i = 0; i < term; i++) {
                attributeStack.pop();
            }
            attributeStack.push(null);
        }
    }

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO: 该过程在遇到 shift 时要采取的代码动作
        String tokenKindId = currentToken.getKindId();
        if (tokenKindId.equals("int")) {
            TokenKindGet attribute = new TokenKindGet(currentToken, SourceCodeType.Int);
            attributeStack.push(attribute);
        } else {
            TokenKindGet attribute = new TokenKindGet(currentToken, null);
            attributeStack.push(attribute);
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO: 设计你可能需要的符号表存储结构
        // 如果需要使用符号表的话, 可以将它或者它的一部分信息存起来, 比如使用一个成员变量存储
        symbolTable = table;
    }
}

