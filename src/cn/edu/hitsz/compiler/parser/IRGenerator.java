package cn.edu.hitsz.compiler.parser;

import cn.edu.hitsz.compiler.NotImplementedException;
import cn.edu.hitsz.compiler.ir.IRImmediate;
import cn.edu.hitsz.compiler.ir.IRValue;
import cn.edu.hitsz.compiler.ir.IRVariable;
import cn.edu.hitsz.compiler.ir.Instruction;
import cn.edu.hitsz.compiler.lexer.Token;
import cn.edu.hitsz.compiler.parser.table.Production;
import cn.edu.hitsz.compiler.parser.table.Status;
import cn.edu.hitsz.compiler.symtab.SymbolTable;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

// TODO: 实验三: 实现 IR 生成

/**
 *
 */
public class IRGenerator implements ActionObserver {

    SymbolTable symbolTable;
    Stack<IRValue> irValueStack = new Stack<>();
    ArrayList<Instruction> instructions = new ArrayList<>();

    @Override
    public void whenShift(Status currentStatus, Token currentToken) {
        // TODO
        String tokenKindId = currentToken.getKindId();
        switch (tokenKindId) {
            case "IntConst" -> {
                IRValue irValue = IRImmediate.of(Integer.parseInt(currentToken.getText()));
                irValueStack.push(irValue);
            }
            case "id" -> {
                IRValue irValue = IRVariable.named(currentToken.getText());
                irValueStack.push(irValue);
            }
            default -> {
                irValueStack.push(null);
            }
        }
    }

    @Override
    public void whenReduce(Status currentStatus, Production production) {
        // TODO
        int index = production.index();
        int term = production.body().size();
        switch (index) {
            case 6 -> {
                // S -> id = E
                // gencode(MOV, id, E.addr)
                IRValue irValueOfE = irValueStack.pop();
                // pop =
                irValueStack.pop();
                IRVariable irVariableOfId = (IRVariable) irValueStack.pop();
                instructions.add(Instruction.createMov(irVariableOfId, irValueOfE));
                irValueStack.push(null);
            }
            case 7 -> {
                // S -> return E
                // gencode(RET, E)
                IRValue irValueOfE = irValueStack.pop();
                // pop return
                irValueStack.pop();
                instructions.add(Instruction.createRet(irValueOfE));
                irValueStack.push(null);
            }
            case 8 -> {
                // E -> E + A
                // gencode(E.addr = E.addr + A.addr)
                IRValue irValueOfA = irValueStack.pop();
                // pop +
                irValueStack.pop();
                IRValue irValueOfE = irValueStack.pop();
                IRVariable irVariable = IRVariable.temp();
                instructions.add(Instruction.createAdd(irVariable, irValueOfE, irValueOfA));
                irValueStack.push(irVariable);
            }
            case 9 -> {
                // E -> E - A
                // gencode(E.addr = E.addr - A.addr)
                IRValue irValueOfA = irValueStack.pop();
                // pop -
                irValueStack.pop();
                IRValue irValueOfE = irValueStack.pop();
                // generate temp
                IRVariable irVariable = IRVariable.temp();
                instructions.add(Instruction.createSub(irVariable, irValueOfE, irValueOfA));
                irValueStack.push(irVariable);
            }
            case 11 -> {
                // A -> A * B
                // gencode(A.addr = A.addr * B.addr
                IRValue irValueOfB = irValueStack.pop();
                // pop *
                irValueStack.pop();
                IRValue irValueOfA = irValueStack.pop();
                // generate temp
                IRVariable irVariable = IRVariable.temp();
                instructions.add(Instruction.createMul(irVariable, irValueOfA, irValueOfB));
                irValueStack.push(irVariable);
            }
            case 13 -> {
                // B -> ( E )
                // pop (
                irValueStack.pop();
                IRValue irValueOfE = irValueStack.pop();
                // pop )
                irValueStack.pop();
                irValueStack.push(irValueOfE);
            }
            default -> {
                if (term != 1) {
                    for (int i = 0; i < term; i++) {
                        irValueStack.pop();
                    }
                    irValueStack.push(null);
                }
            }
        }
    }


    @Override
    public void whenAccept(Status currentStatus) {
        // TODO
        for (int i = 0; i < irValueStack.size(); i++) {
            irValueStack.pop();
        }
    }

    @Override
    public void setSymbolTable(SymbolTable table) {
        // TODO
        this.symbolTable = table;
    }

    public List<Instruction> getIR() {
        // TODO
        return instructions;
    }

    public void dumpIR(String path) {
        FileUtils.writeLines(path, getIR().stream().map(Instruction::toString).toList());
    }
}

