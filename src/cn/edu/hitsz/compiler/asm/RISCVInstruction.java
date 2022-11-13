package cn.edu.hitsz.compiler.asm;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author Black
 */

public class RISCVInstruction {
    //============================== 不同种类 RISCV 的构造函数 ==============================
    public static RISCVInstruction createAdd(String result, String lhs, String rhs) {
        return new RISCVInstruction(RISCVInstructionKind.add, result, List.of(lhs, rhs));
    }

    public static RISCVInstruction createAddi(String result, String lhs, String rhs) {
        return new RISCVInstruction(RISCVInstructionKind.addi, result, List.of(lhs, rhs));
    }

    public static RISCVInstruction createSub(String result, String lhs, String rhs) {
        return new RISCVInstruction(RISCVInstructionKind.sub, result, List.of(lhs, rhs));
    }

    public static RISCVInstruction createMul(String result, String lhs, String rhs) {
        return new RISCVInstruction(RISCVInstructionKind.mul, result, List.of(lhs, rhs));
    }

    public static RISCVInstruction createLi(String result, String from) {
        return new RISCVInstruction(RISCVInstructionKind.li, result, List.of(from));
    }

    public static RISCVInstruction createMv(String result, String from) {
        return new RISCVInstruction(RISCVInstructionKind.mv, result, List.of(from));
    }

    //============================== 不同种类 RISCV 的参数 getter ==============================
    public RISCVInstructionKind getKind() {
        return this.kind;
    }

    public String getResult() {
        return this.result;
    }

//    public String getLHS() {
//        ensureKindMatch(Set.of(RISCVInstructionKind.add, RISCVInstructionKind.addi, RISCVInstructionKind.sub,
//                RISCVInstructionKind.mul));
//        return this.operands.get(0);
//    }
//
//    public String getRHS() {
//        ensureKindMatch(Set.of(RISCVInstructionKind.add, RISCVInstructionKind.addi, RISCVInstructionKind.sub,
//                RISCVInstructionKind.mul));
//        return this.operands.get(1);
//    }
//
//    public String getFrom() {
//        ensureKindMatch(Set.of(RISCVInstructionKind.mv, RISCVInstructionKind.li));
//        return this.operands.get(0);
//    }


    //============================== 基础设施 ==============================
    @Override
    public String toString() {
        final var kindString = kind.toString();
        final var resultString = result == null ? "" : result.toString();
        final var operandsString = operands.stream().map(Objects::toString).collect(Collectors.joining(", "));
        return "%s, %s, %s".formatted(kindString, resultString, operandsString);
    }

    public RISCVInstruction(RISCVInstructionKind kind, String result, List<String> operands) {
        this.kind = kind;
        this.result = result;
        this.operands = operands;
    }

    private final RISCVInstructionKind kind;
    private final String result;
    private final List<String> operands;

    private void ensureKindMatch(Set<RISCVInstructionKind> targetKinds) {
        final var kind = getKind();
        if (!targetKinds.contains(kind)) {
            final var acceptKindsString = targetKinds.stream()
                    .map(RISCVInstructionKind::toString)
                    .collect(Collectors.joining(","));
            throw new RuntimeException(
                    "Illegal operand access, except %s, but given %s".formatted(acceptKindsString, kind)
            );
        }
    }
}
