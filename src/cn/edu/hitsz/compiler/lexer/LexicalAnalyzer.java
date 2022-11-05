package cn.edu.hitsz.compiler.lexer;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;

/**
 * TODO: 实验一: 实现词法分析
 * <br>
 * 你可能需要参考的框架代码如下:
 *
 * @see Token 词法单元的实现
 * @see TokenKind 词法单元类型的实现
 */
public class LexicalAnalyzer {

    private final SymbolTable symbolTable;
    private final ArrayList<Token> tokens = new ArrayList<>();
    private StringBuffer read_code;

    public LexicalAnalyzer(SymbolTable symbolTable) {
        this.symbolTable = symbolTable;
    }

    /**
     * 从给予的路径中读取并加载文件内容
     *
     * @param path 路径
     */
    public void loadFile(String path) {
        // TODO: 词法分析前的缓冲区实现
        // 可自由实现各类缓冲区
        // 或直接采用完整读入方法
        read_code = new StringBuffer(FileUtils.readFile(path));
    }

    /**
     * 执行词法分析, 准备好用于返回的 token 列表 <br>
     * 需要维护实验一所需的符号表条目, 而得在语法分析中才能确定的符号表条目的成员可以先设置为 null
     */
    public void run() {
        // TODO: 自动机实现的词法分析过程
        Token currentToken = null;

        int i = 0;

        while (i < read_code.length()) {
            char single = read_code.charAt(i);

            /**
             * skip the useless symbol
             */
            if (single == ' ' || single == '\n') {
                i++;
                continue;
            }

            /**
             * match symbol
             */
            currentToken = MatchSymbol(single, currentToken);
            if (currentToken != null) {
                i++;
                tokens.add(currentToken);
                continue;
            }

            /**
             * connect to make word
             */
            StringBuffer word = new StringBuffer();

            while ((single >= 'a' && single <= 'z') || (single >= 'A' && single <= 'Z')
                    || (single >= '0' && single <= '9')) {
                word.append(single);
                i++;
                single = read_code.charAt(i);
            }

            if (word.length() > 0) {
                currentToken = MatchWord(word, currentToken);
            }
            tokens.add(currentToken);
        }
        tokens.add(Token.simple("$"));
    }


    public Token MatchSymbol(char symbol, Token currentToken) {

        switch (symbol) {
            case '+':
                currentToken = Token.simple("+");
                break;
            case '-':
                currentToken = Token.simple("-");
                break;
            case '*':
                currentToken = Token.simple("*");
                break;
            case '/':
                currentToken = Token.simple("/");
                break;
            case '(':
                currentToken = Token.simple("(");
                break;
            case ')':
                currentToken = Token.simple(")");
                break;
            case ',':
                currentToken = Token.simple(",");
                break;
            case '=':
                currentToken = Token.simple("=");
                break;
            case ';':
                currentToken = Token.simple("Semicolon");
                break;
            default:
                currentToken = null;
                break;
        }

        return currentToken;
    }

    public Token MatchWord(StringBuffer word, Token currentToken) {

        char first = word.charAt(0);
        String str_word = word.toString();

        /**
         * judge whether it is a word or number
         */
        if ((first >= 'a' && first <= 'z') || (first >= 'A' && first <= 'Z')) {
            if (str_word.equals("int")) {
                currentToken = Token.simple("int");
            } else if (str_word.equals("return")) {
                currentToken = Token.simple("return");
            } else {
                currentToken = Token.normal("id", str_word);
                if (!symbolTable.has(str_word)) {
                    symbolTable.add(str_word);
                }
            }
        } else {
            currentToken = Token.normal("IntConst", str_word);
        }
        return currentToken;
    }

    /**
     * 获得词法分析的结果, 保证在调用了 run 方法之后调用
     *
     * @return Token 列表
     */
    public Iterable<Token> getTokens() {
        // TODO: 从词法分析过程中获取 Token 列表
        // 词法分析过程可以使用 Stream 或 Iterator 实现按需分析
        // 亦可以直接分析完整个文件
        // 总之实现过程能转化为一列表即可
        return tokens;
    }

    public void dumpTokens(String path) {
        FileUtils.writeLines(
                path,
                StreamSupport.stream(getTokens().spliterator(), false).map(Token::toString).toList()
        );
    }


}
