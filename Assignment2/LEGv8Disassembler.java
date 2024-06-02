import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class LEGv8Disassembler {
    public static void main(String[] args) {
        InsertInstructions(); // Insert instructions into the maps
        InsertConditions(); // Insert conditions into the map

        int instructionCount = 0;
        int[] instructions = null;
        try {
            RandomAccessFile file = new RandomAccessFile(args[0], "r");
            FileChannel channel = file.getChannel();
            ByteBuffer buffer = ByteBuffer.allocate((int) channel.size()).order(ByteOrder.BIG_ENDIAN);
            channel.read(buffer);
            buffer.flip(); // Flip from little to big endian

            instructionCount = buffer.remaining() / 4;
            instructions = new int[instructionCount];
            for (int i = 0; i < instructionCount; i++) {
                instructions[i] = buffer.getInt();
                System.out.println(Integer.toBinaryString(instructions[i]));
            }
            file.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        ArrayList<Instruction> instructionList = new ArrayList<>(); // List of instructions

        for (int i = 0; i < instructionCount; i++) {
            int instruction = instructions[i];
            InstructionType instructionType = getInstructionType(instruction);
            if (instructionType == null) {
                System.out.println("Unknown instruction: " + Integer.toBinaryString(instruction));
                System.out.println(Integer.toHexString(instruction));
            } else {
                switch (instructionType) {
                    case R:
                        instructionList.add(new RInstruction(instruction));
                        break;
                    case I:
                        instructionList.add(new IInstruction(instruction));
                        break;
                    case D:
                        instructionList.add(new DInstruction(instruction));
                        break;
                    case B:
                        instructionList.add(new BInstruction(instruction));
                        break;
                    case CB:
                        instructionList.add(new CBInstruction(instruction));
                        break;
                }
            }
        }

        ArrayList<Label> labelList = new ArrayList<>();
        int labelCount = 1;

        for (int i = 0; i < instructionList.size(); i++) {
            Instruction instruction = instructionList.get(i);
            if (instruction instanceof LabelInstruction) { // Check if instruction would have a label
                LabelInstruction labelInstruction = (LabelInstruction) instruction;
                int BR_address = labelInstruction.getBR_address();
                int labelIndex = getIndexInLabelList(labelList, i + BR_address);
                if (labelIndex != -1) { //If label already exists
                    labelInstruction.setLabelName(labelList.get(labelIndex).name); // Set label name to existing label
                } else {
                    labelList.add(new Label("label" + labelCount, i + BR_address)); // Create new label
                    labelInstruction.setLabelName("label" + labelCount);
                    labelCount++;
                }
                instructionList.set(i, labelInstruction); // Replace instruction with labelInstruction
            }
        }

        //Sort labelList so the labels are in ascending order relative to where they are put in the instructionList
        Collections.sort(labelList, new Comparator<Label>() {
            @Override
            public int compare(Label label1, Label label2) {
                return Integer.compare(label1.getIndex(), label2.getIndex());
            }
        });

        int offset = 0; //Keeps track of how many labels have been added so the indexes line up
        for (int i = 0; i < labelList.size(); i++) {
            instructionList.add(labelList.get(i).getIndex() + offset, labelList.get(i));
            offset++; 
        }

        for (Instruction instruction : instructionList) {
            System.out.println(instruction.toString());
        }
    }

    private static int getIndexInLabelList(ArrayList<Label> labelList, int index) {
        for (int i = 0; i < labelList.size(); i++) {
            if (labelList.get(i).getIndex() == index) {
                return i;
            }
        }
        return -1;
    }

    enum InstructionType {
        R, I, D, CB, B
    }

    private static InstructionType getInstructionType(int instruction) {
        int opcode = (instruction >>> 21); // R and D instructions opcode is in bits 21-31
        // Check if opcode is in R_instructionMap
        if (R_instructionMap.containsKey(opcode)) {
            return InstructionType.R;
        }
        // Check if opcode is in D_instructionMap
        if (D_instructionMap.containsKey(opcode)) {
            return InstructionType.D;
        }

        opcode = (opcode >>> 1); // I instructions opcode is in bits 22-31
        // Check if opcode is in I_instructionMap
        if (I_instructionMap.containsKey(opcode)) {
            return InstructionType.I;
        }

        opcode = (opcode >>> 2); // CB instructions opcode is in bits 24-31
        // Check if opcode is in CB_instructionMap
        if (CB_instructionMap.containsKey(opcode)) {
            return InstructionType.CB;
        }

        opcode = (opcode >>> 2); // B instructions opcode is in bits 26-31
        // Check if opcode is in B_instructionMap
        if (B_instructionMap.containsKey(opcode)) {
            return InstructionType.B;
        }

        // If not in any of the maps, return null
        return null;
    }

    // Maps for instructions and conditions
    static final Map<Integer, String> R_instructionMap = new HashMap<>();
    static final Map<Integer, String> I_instructionMap = new HashMap<>();
    static final Map<Integer, String> D_instructionMap = new HashMap<>();
    static final Map<Integer, String> B_instructionMap = new HashMap<>();
    static final Map<Integer, String> CB_instructionMap = new HashMap<>();
    static final Map<Integer, String> conditionMap = new HashMap<>();

    // Insert instructions into the maps
    public static void InsertInstructions() {
        R_instructionMap.put(0b10001011000, "ADD");
        I_instructionMap.put(0b1001000100, "ADDI");
        // I_instructionMap.put(0b1011000100, "ADDIS");
        // R_instructionMap.put(0b10101011000, "ADDS");
        R_instructionMap.put(0b10001010000, "AND");
        I_instructionMap.put(0b1001001000, "ANDI");
        // I_instructionMap.put(0b1111001000, "ANDIS");
        // R_instructionMap.put(0b1110101000, "ANDS");
        B_instructionMap.put(0b000101, "B");
        CB_instructionMap.put(0b01010100, "B.");
        B_instructionMap.put(0b100101, "BL");
        R_instructionMap.put(0b11010110000, "BR");
        CB_instructionMap.put(0b10110101, "CBNZ");
        CB_instructionMap.put(0b10110100, "CBZ");
        R_instructionMap.put(0b11111111110, "DUMP");
        R_instructionMap.put(0b11001010000, "EOR");
        I_instructionMap.put(0b1101001000, "EORI");
        // R_instructionMap.put(0b00011110011, "FADDD");
        // R_instructionMap.put(0b00011110001, "FADDS");
        // R_instructionMap.put(0b00011110011, "FCMPD");
        // R_instructionMap.put(0b00011110001, "FCMPS");
        // R_instructionMap.put(0b00011110011, "FDIVD");
        // R_instructionMap.put(0b00011110001, "FDIVS");
        // R_instructionMap.put(0b00011110011, "FMULD");
        // R_instructionMap.put(0b00011110001, "FMULS");
        // R_instructionMap.put(0b00011110011, "FSUBD");
        // R_instructionMap.put(0b00011110001, "FSUBS");
        R_instructionMap.put(0b11111111111, "HALT");
        D_instructionMap.put(0b11111000010, "LDUR");
        // D_instructionMap.put(0b00111000010, "LDURB");
        // R_instructionMap.put(0b11111100010, "LDURD");
        // D_instructionMap.put(0b01111000010, "LDURH");
        // R_instructionMap.put(0b10111100010, "LDURS");
        // D_instructionMap.put(0b10111000100, "LDURSW");
        R_instructionMap.put(0b11010011011, "LSL");
        R_instructionMap.put(0b11010011010, "LSR");
        R_instructionMap.put(0b10011011000, "MUL");
        R_instructionMap.put(0b10101010000, "ORR");
        I_instructionMap.put(0b1011001000, "ORRI");
        R_instructionMap.put(0b11111111100, "PRNL");
        R_instructionMap.put(0b11111111101, "PRNT");
        // R_instructionMap.put(0b10011010110, "SDIV");
        // R_instructionMap.put(0b10011011010, "SMULH");
        D_instructionMap.put(0b11111000000, "STUR");
        // D_instructionMap.put(0b00111000000, "STURB");
        // R_instructionMap.put(0b11111100000, "STURD");
        // D_instructionMap.put(0b01111000000, "STURH");
        // R_instructionMap.put(0b10111100000, "STURS");
        // D_instructionMap.put(0b10111000000, "STURW");
        R_instructionMap.put(0b11001011000, "SUB");
        I_instructionMap.put(0b1101000100, "SUBI");
        I_instructionMap.put(0b1111000100, "SUBIS");
        R_instructionMap.put(0b11101011000, "SUBS");
        // R_instructionMap.put(0b10011010110, "UDIV");
        // R_instructionMap.put(0b10011011110, "UMULH");
    }

    // Insert conditions into the map
    public static void InsertConditions() {
        conditionMap.put(0b00000, "EQ");
        conditionMap.put(0b00001, "NE");
        conditionMap.put(0b00010, "HS");
        conditionMap.put(0b00011, "LO");
        conditionMap.put(0b00100, "MI");
        conditionMap.put(0b00101, "PL");
        conditionMap.put(0b00110, "VS");
        conditionMap.put(0b00111, "VC");
        conditionMap.put(0b01000, "HI");
        conditionMap.put(0b01001, "LS");
        conditionMap.put(0b01010, "GE");
        conditionMap.put(0b01011, "LT");
        conditionMap.put(0b01100, "GT");
        conditionMap.put(0b01101, "LE");
    }
}

// Instruction classes
abstract class Instruction {
    public abstract String toString();
}

class RInstruction extends Instruction {
    int opcode;
    int Rm;
    int shamt;
    int Rn;
    int Rd;
    String name;

    RInstruction(int instruction) {
        opcode = (instruction >>> 21); // Extract bits 21-31
        Rm = (instruction >>> 16) & 0x1F; // Extract bits 16-20
        shamt = (instruction >>> 10) & 0x3F; // Extract bits 10-15
        Rn = (instruction >>> 5) & 0x1F; // Extract bits 5-9
        Rd = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.R_instructionMap.get(opcode);

    }

    @Override
    public String toString() {
        if (LEGv8Disassembler.R_instructionMap.get(opcode).equals("LSL")
                || LEGv8Disassembler.R_instructionMap.get(opcode).equals("LSR")) {
            return name + " X" + Rd + ", X" + Rn + ", #" + shamt;
        }
        if (LEGv8Disassembler.R_instructionMap.get(opcode).equals("BR")) {
            return name + " X" + Rn;
        }
        if (LEGv8Disassembler.R_instructionMap.get(opcode).equals("PRNT")) {
            return name + " X" + Rn;
        }
        if (LEGv8Disassembler.R_instructionMap.get(opcode).equals("PRNL")) {
            return name;
        }
        if (LEGv8Disassembler.R_instructionMap.get(opcode).equals("DUMP")) {
            return name;
        }
        if (LEGv8Disassembler.R_instructionMap.get(opcode).equals("HALT")) {
            return name;
        }
        return name + " X" + Rd + ", X" + Rn + ", X" + Rm;
    }
}

class IInstruction extends Instruction {
    int opcode;
    int ALU_immediate;
    int Rn;
    int Rd;
    String name;

    IInstruction(int instruction) {
        opcode = (instruction >>> 22); // Extract bits 21-31
        ALU_immediate = (instruction >>> 10) & 0xFFF; // Extract bits 10-21
        if ((ALU_immediate & 0x800) != 0) {
            ALU_immediate |= 0xFFFFF000;
        }
        Rn = (instruction >>> 5) & 0x1F; // Extract bits 5-9
        Rd = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.I_instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " X" + Rd + ", X" + Rn + ", #" + ALU_immediate;
    }
}

class DInstruction extends Instruction {
    int opcode;
    int DT_address;
    int op;
    int Rn;
    int Rt;
    String name;

    DInstruction(int instruction) {
        opcode = (instruction >>> 21); // Extract bits 21-31
        DT_address = (instruction >>> 12) & 0x1FF; // Extract bits 12-20
        op = (instruction >>> 10) & 0x3; // Extract bits 10-11
        Rn = (instruction >>> 5) & 0x1F; // Extract bits 5-9
        Rt = instruction & 0x1F; // Extract bits 0-4
        name = LEGv8Disassembler.D_instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " X" + Rt + ", [X" + Rn + ", #" + DT_address + "]";
    }
}

abstract class LabelInstruction extends Instruction {
    String labelName = "";
    int BR_address;

    public int getBR_address() {
        return BR_address;
    }

    public void setLabelName(String labelName) {
        this.labelName = labelName;
    }
}

class BInstruction extends LabelInstruction {
    int opcode;
    String name;

    BInstruction(int instruction) {
        opcode = (instruction >>> 26); // Extract bits 26-31
        BR_address = instruction & 0x3FFFFFF; // Extract bits 0-25

        // Check if the most significant bit is set
        if ((BR_address & 0x02000000) != 0) {
            // Perform sign extension
            BR_address |= 0xFC000000;
        }

        name = LEGv8Disassembler.B_instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        return name + " " + labelName;
    }
}

class CBInstruction extends LabelInstruction {
    int opcode;
    int Rt;
    String name;
    String condName;

    CBInstruction(int instruction) {
        opcode = (instruction >>> 24); // Extract bits 24-31

        BR_address = (instruction >>> 5) & 0x7FFFF; // Extract bits 5-23
        // Check if the most significant bit is set
        // Check if the most significant bit is set
        if ((BR_address & 0x00020000) != 0) {
            // Perform sign extension
            BR_address |= 0xFFFC0000;
        }

        Rt = instruction & 0x1F; // Extract bits 0-4
        condName = LEGv8Disassembler.conditionMap.get(Rt);
        name = LEGv8Disassembler.CB_instructionMap.get(opcode);
    }

    @Override
    public String toString() {
        if (LEGv8Disassembler.CB_instructionMap.get(opcode).equals("B.")) {
            return name + condName + " " + labelName;
        }
        return name + " X" + Rt + ", " + labelName;
    }
}

class Label extends Instruction {
    String name;
    int index;

    Label(String name, int index) {
        this.name = name;
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return name + ":";
    }
}
