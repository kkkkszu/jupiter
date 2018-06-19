package vsim.riscv.instructions.rtype;

import vsim.Globals;
import vsim.riscv.instructions.MachineCode;
import vsim.riscv.instructions.Instruction;
import vsim.riscv.instructions.InstructionField;


public final class Fles extends Instruction {

  public Fles() {
    super(
      Instruction.Format.R,
      "fle.s",
      "fle.s rd, frs1, frs2",
      "set rd = 1 if frs1 <= frs2 else 0"
    );
  }

  @Override
  public int getOpCode() {
    return 0b1010011;
  }

  @Override
  public int getFunct5() {
    return 0b10100;
  }

  @Override
  public void execute(MachineCode code) {
    float rs1 = Globals.fregfile.getRegister(code.get(InstructionField.RS1));
    float rs2 = Globals.fregfile.getRegister(code.get(InstructionField.RS2));
    int result = (rs1 <= rs2) ? 1 : 0;
    Globals.regfile.setRegister(
      code.get(InstructionField.RD),
      result
    );
    Globals.regfile.incProgramCounter();
  }

}