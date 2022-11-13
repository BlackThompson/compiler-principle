package cn.edu.hitsz.compiler.asm;

import cn.edu.hitsz.compiler.ir.*;
import cn.edu.hitsz.compiler.utils.FileUtils;

import java.util.ArrayList;
import java.util.List;


/**
 * TODO: 实验四: 实现汇编生成
 * <br>
 * 在编译器的整体框架中, 代码生成可以称作后端, 而前面的所有工作都可称为前端.
 * <br>
 * 在前端完成的所有工作中, 都是与目标平台无关的, 而后端的工作为将前端生成的目标平台无关信息
 * 根据目标平台生成汇编代码. 前后端的分离有利于实现编译器面向不同平台生成汇编代码. 由于前后
 * 端分离的原因, 有可能前端生成的中间代码并不符合目标平台的汇编代码特点. 具体到本项目你可以
 * 尝试加入一个方法将中间代码调整为更接近 risc-v 汇编的形式, 这样会有利于汇编代码的生成.
 * <br>
 * 为保证实现上的自由, 框架中并未对后端提供基建, 在具体实现时可自行设计相关数据结构.
 *
 * @author Black
 * @see AssemblyGenerator#run() 代码生成与寄存器分配
 */
public class AssemblyGenerator {

    private int regNum = 7;

    private List<Instruction> instructionList;
    private ArrayList<Instruction> newInstructionList = new ArrayList<>();

    private BinMap<Register, IRVariable> binMap = new BinMap<>();
    private Register[] regs = new Register[regNum];
    private List<RISCVInstruction> riscvInstructions = new ArrayList<>();

    /**
     * 加载前端提供的中间代码
     * <br>
     * 视具体实现而定, 在加载中或加载后会生成一些在代码生成中会用到的信息. 如变量的引用
     * 信息. 这些信息可以通过简单的映射维护, 或者自行增加记录信息的数据结构.
     *
     * @param originInstructions 前端提供的中间代码
     */
    public void loadIR(List<Instruction> originInstructions) {
        // TODO: 读入前端提供的中间代码并生成所需要的信息
        this.instructionList = originInstructions;
    }

    private void preprocess() {
        for (int i = 0; i < regNum; i++) {
            regs[i] = new Register(i);
        }
        for (int i = 0; i < instructionList.size(); i++) {
            Instruction instruction = instructionList.get(i);
            InstructionKind instructionKind = instruction.getKind();

            if (instructionKind.isReturn()) {
                // if RET, exit
                newInstructionList.add(instruction);
                break;
            } else if (instructionKind.isUnary()) {
                // if MOV, don't move
                newInstructionList.add(instruction);
            } else {
                IRVariable result = instruction.getResult();
                IRValue operation1 = instruction.getLHS();
                IRValue operation2 = instruction.getRHS();
                if (operation1 instanceof IRImmediate) {
                    if (operation2 instanceof IRImmediate) {
                        // If both operands are immediate numbers, calculate the sum of the two immediate numbers and replace them with the MOV
                        // Determine the type of operation
                        Instruction newInstruction;
                        switch (instructionKind) {
                            case ADD -> newInstruction = Instruction.createMov(result,
                                    IRImmediate.of(((IRImmediate) operation1).getValue() + ((IRImmediate) operation2).getValue()));
                            case SUB -> newInstruction = Instruction.createMov(result,
                                    IRImmediate.of(((IRImmediate) operation1).getValue() - ((IRImmediate) operation2).getValue()));
                            case MUL -> newInstruction = Instruction.createMov(result,
                                    IRImmediate.of(((IRImmediate) operation1).getValue() * ((IRImmediate) operation2).getValue()));
                            default -> throw new RuntimeException();
                        }
                        newInstructionList.add(newInstruction);
                    } else {
                        // Determine whether it is multiplication or left immediate subtraction
                        if (instructionKind.equals(InstructionKind.MUL) || instructionKind.equals(InstructionKind.SUB)) {
                            IRVariable irVariable = IRVariable.temp();
                            newInstructionList.add(Instruction.createMov(irVariable,
                                    IRImmediate.of(((IRImmediate) operation1).getValue())));
                            switch (instructionKind) {
                                case SUB -> newInstructionList.add(Instruction.createSub(result, irVariable, operation2));
                                case MUL -> newInstructionList.add(Instruction.createMul(result, irVariable, operation2));
                                default -> throw new RuntimeException();
                            }
                        } else {
                            Instruction newInstruction = Instruction.createAdd(result, operation2, operation1);
                            newInstructionList.add(newInstruction);
                        }
                    }
                } else {
                    if (operation2 instanceof IRImmediate) {
                        // Determine if it is a multiplication of immediate numbers
                        if (instructionKind.equals(InstructionKind.MUL)) {
                            IRVariable irVariable = IRVariable.temp();
                            newInstructionList.add(Instruction.createMov(irVariable,
                                    IRImmediate.of(((IRImmediate) operation1).getValue())));
                            newInstructionList.add(Instruction.createMul(result, irVariable, operation2));
                        } else {
                            // Do not change
                            newInstructionList.add(instruction);
                        }
                    } else {
                        // Do not change
                        newInstructionList.add(instruction);
                    }
                }
            }
        }
        // System.out.println(newInstructionList);
    }

    private Register getReg(int index) {
        // The index passed in is the current instruction sequence number
        // If there is a free register, use the free register directly
        for (int i = 0; i < regNum; i++) {
            if (!binMap.containsKey(regs[i])) {
                return regs[i];
            }
        }
        // If there are no free registers, the newInstructionList is traversed, looking for registers that will not be used later
        boolean[] willBeUsed = new boolean[regNum];
        for (int i = 0; i < regNum; i++) {
            if (willBeUsed[i]) {
                continue;
            }
            // Gets the variable name in the current register
            String IRName = binMap.getByKey(regs[i]).getName();
            for (int j = index; j < newInstructionList.size(); j++) {
                if (willBeUsed[i]) {
                    break;
                }
                List<IRValue> operands = newInstructionList.get(j).getOperands();
                for (int k = 0; k < operands.size(); k++) {
                    if (operands.get(k).isIRVariable()) {
                        IRVariable irVariable = (IRVariable) operands.get(k);
                        if (irVariable.getName().equals(IRName)) {
                            willBeUsed[i] = true;
                            break;
                        }
                    }
                }
            }
        }
        for (int i = 0; i < regNum; i++) {
            if (!willBeUsed[i]) {
                return regs[i];
            }
        }
        throw new RuntimeException("No Reg for variable");
    }

    /**
     * 执行代码生成.
     * <br>
     * 根据理论课的做法, 在代码生成时同时完成寄存器分配的工作. 若你觉得这样的做法不好,
     * 也可以将寄存器分配和代码生成分开进行.
     * <br>
     * 提示: 寄存器分配中需要的信息较多, 关于全局的与代码生成过程无关的信息建议在代码生
     * 成前完成建立, 与代码生成的过程相关的信息可自行设计数据结构进行记录并动态维护.
     */
    public void run() {
        // TODO: 执行寄存器分配与代码生成
        // Pretreatment
        preprocess();
        for (int i = 0; i < newInstructionList.size(); i++) {
            Instruction curr = newInstructionList.get(i);
            switch (curr.getKind()) {
                case ADD -> {
                    IRVariable result = curr.getResult();
                    IRValue operation1 = curr.getLHS();
                    IRValue operation2 = curr.getRHS();
                    Register regForResult, regForoperation1, regForoperation2;
                    // If result already exists in a register, the register is used directly
                    if (binMap.containsValue(result)) {
                        regForResult = binMap.getByValue(result);
                    } else {
                        // Otherwise call getReg() to get the register
                        regForResult = getReg(i);
                        // Complete the correlation mapping
                        binMap.replace(regForResult, result);
                    }
                    // The operation operation1 is the same as the result
                    if (binMap.containsValue((IRVariable) operation1)) {
                        regForoperation1 = binMap.getByValue((IRVariable) operation1);
                    } else {
                        regForoperation1 = getReg(i);
                        binMap.replace(regForoperation1, (IRVariable) operation1);
                    }
                    // operation2 needs to determine if it is an immediate number
                    if (operation2.isIRVariable()) {
                        // If operation2 is not an immediate number, the operation is the same as regForResult
                        if (binMap.containsValue((IRVariable) operation2)) {
                            regForoperation2 = binMap.getByValue((IRVariable) operation2);
                        } else {
                            regForoperation2 = getReg(i);
                            binMap.replace(regForoperation2, (IRVariable) operation2);
                        }
                        // Generate the corresponding assembly instructions
                        riscvInstructions.add(RISCVInstruction.createAdd(regForResult.getName(), regForoperation1.getName(), regForoperation2.getName()));
                    } else {
                        // If operation2 is the immediate number
                        String valueOfoperation2 = operation2.toString();
                        riscvInstructions.add(RISCVInstruction.createAddi(regForResult.getName(), regForoperation1.getName(), valueOfoperation2));
                    }
                }
                case SUB -> {
                    IRVariable result = curr.getResult();
                    IRValue operation1 = curr.getLHS();
                    IRValue operation2 = curr.getRHS();
                    Register regForResult, regForoperation1, regForoperation2;
                    // If result already exists in a register, the register is used directly
                    if (binMap.containsValue(result)) {
                        regForResult = binMap.getByValue(result);
                    } else {
                        // Otherwise call getReg() to get the register
                        regForResult = getReg(i);
                        // Complete the correlation mapping
                        binMap.replace(regForResult, result);
                    }
                    // The operation operation1 is the same as the result
                    if (binMap.containsValue((IRVariable) operation1)) {
                        regForoperation1 = binMap.getByValue((IRVariable) operation1);
                    } else {
                        regForoperation1 = getReg(i);
                        binMap.replace(regForoperation1, (IRVariable) operation1);
                    }
                    // operation2 needs to determine if it is an immediate number
                    if (operation2.isIRVariable()) {
                        if (binMap.containsValue((IRVariable) operation2)) {
                            regForoperation2 = binMap.getByValue((IRVariable) operation2);
                        } else {
                            regForoperation2 = getReg(i);
                            binMap.replace(regForoperation2, (IRVariable) operation2);
                        }
                        // Generate the corresponding assembly instructions
                        riscvInstructions.add(RISCVInstruction.createSub(regForResult.getName(), regForoperation1.getName(), regForoperation2.getName()));
                    } else {
                        // If operation2 is the immediate number
                        String valueOfoperation2 = operation2.toString();
                        int value = Integer.parseInt(valueOfoperation2);
                        value = -value;
                        valueOfoperation2 = String.valueOf(value);
                        riscvInstructions.add(RISCVInstruction.createAddi(regForResult.getName(), regForoperation1.getName(), valueOfoperation2));
                    }
                }
                case MUL -> {
                    IRVariable result = curr.getResult();
                    IRValue operation1 = curr.getLHS();
                    IRValue operation2 = curr.getRHS();
                    Register regForResult, regForoperation1, regForoperation2;
                    // If result already exists in a register, the register is used directly
                    if (binMap.containsValue(result)) {
                        regForResult = binMap.getByValue(result);
                    } else {
                        // Otherwise call getReg() to get the register
                        regForResult = getReg(i);
                        // Complete the correlation mapping
                        binMap.replace(regForResult, result);
                    }
                    // The operation operation1 is the same as the result
                    if (binMap.containsValue((IRVariable) operation1)) {
                        regForoperation1 = binMap.getByValue((IRVariable) operation1);
                    } else {
                        regForoperation1 = getReg(i);
                        binMap.replace(regForoperation1, (IRVariable) operation1);
                    }
                    // operation2 needs to determine if it is an immediate number
                    if (operation2.isIRVariable()) {
                        // If operation2 is not an immediate number, the operation is the same as regForResult
                        if (binMap.containsValue((IRVariable) operation2)) {
                            regForoperation2 = binMap.getByValue((IRVariable) operation2);
                        } else {
                            regForoperation2 = getReg(i);
                            binMap.replace(regForoperation2, (IRVariable) operation2);
                        }
                        // Generate the corresponding assembly instructions
                        riscvInstructions.add(RISCVInstruction.createMul(regForResult.getName(), regForoperation1.getName(), regForoperation2.getName()));
                    } else {
                        // If operation2 is the immediate number
                        String valueOfoperation2 = operation2.toString();
                        riscvInstructions.add(RISCVInstruction.createMul(regForResult.getName(), regForoperation1.getName(), valueOfoperation2));
                    }
                }
                case MOV -> {
                    IRVariable result = curr.getResult();
                    IRValue from = curr.getFrom();
                    Register regForResult, regForFrom;
                    if (binMap.containsValue(result)) {
                        regForResult = binMap.getByValue(result);
                    } else {
                        regForResult = getReg(i);
                        binMap.replace(regForResult, result);
                    }
                    //  whether it is an immediate number
                    if (from.isIRVariable()) {
                        if (binMap.containsValue((IRVariable) from)) {
                            regForFrom = binMap.getByValue((IRVariable) from);
                        } else {
                            regForFrom = getReg(i);
                            binMap.replace(regForFrom, (IRVariable) from);
                        }
                        // Generate assembly instruction
                        riscvInstructions.add(RISCVInstruction.createMv(regForResult.getName(), regForFrom.getName()));
                    } else {
                        String valueOfFrom = from.toString();
                        riscvInstructions.add(RISCVInstruction.createLi(regForResult.getName(), valueOfFrom));
                    }
                }
                case RET -> {
                    IRValue ret = curr.getReturnValue();
                    if (ret.isImmediate()) {
                        riscvInstructions.add(RISCVInstruction.createLi("a0", ret.toString()));
                    } else {
                        Register regForRet;
                        if (binMap.containsValue((IRVariable) ret)) {
                            regForRet = binMap.getByValue((IRVariable) ret);
                        } else {
                            regForRet = getReg(i);
                            binMap.replace(regForRet, (IRVariable) ret);
                        }
                        riscvInstructions.add(RISCVInstruction.createMv("a0", regForRet.getName()));
                    }
                }
                default -> throw new RuntimeException();
            }


        }
    }

    /**
     * 输出汇编代码到文件
     *
     * @param path 输出文件路径
     */
    public void dump(String path) {

        // TODO: 输出汇编代码到文件
        FileUtils.writeLines(path, riscvInstructions.stream().map(RISCVInstruction::toString).toList());
    }
}

