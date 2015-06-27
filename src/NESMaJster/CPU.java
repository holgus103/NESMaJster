/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;


import com.sun.jndi.cosnaming.IiopUrl;
import com.sun.org.apache.bcel.internal.generic.NOP;

/**
 *
 * @author Administrator
 */
class CPU {
	
	private enum AddressingMode {
		None,
		Immediate,
		ZeroPageIndexed,
		ZeroPageIndexedX,
		ZeroPageIndexedY,
		Absolute,
		AbsoluteIndexedX,
		AbsoluteIndexedY,
		IndirectX,
		IndirectY;
	}
	private class Instruction {
		private CMD opcode;
		private AddressingMode addrMode;
		private byte[] params;

		public Instruction(byte opcode, AddressingMode addrMode, byte[] params) {
			this.opcode = opcode;
			this.addrMode = addrMode;
			this.params = params;
		}
	}
//	private enum CMD{
//		STOP ((byte)0x00),
//		ADD  ((byte)0x01),
//		LDA  ((byte)0x02),
//		;
//
//		byte No;
//		CMD(byte input){
//			this.No = input;
//		}
//	}
	private enum CMD {
		BRK ((byte)0x00),
		PHP ((byte)0x08),
		BPL ((byte)0x10),
		CLC ((byte)0x18),
		JSR ((byte)0x20),
		PLP ((byte)0x28),
		BMI ((byte)0x30),
		SEC ((byte)0x38),
		RTI ((byte)0x40),
		PHA ((byte)0x48),
		BVC ((byte)0x50),
		CLI ((byte)0x58),
		ALR ((byte)0x4B),
		RTS ((byte)0x60),
		PLA ((byte)0x68),
		BVS ((byte)0x70),
		SEI ((byte)0x78),
		ARR ((byte)0x6B),
		DEY ((byte)0x88),
		BCC ((byte)0x90),
		TYA ((byte)0x98),
		SHY ((byte)0x9C),
		TXA ((byte)0x8A),
		TXS ((byte)0x9A),
		SHX ((byte)0x9E),
		XAA ((byte)0x8B),
		TAS ((byte)0x9B),
		TAY ((byte)0xAC),
		BCS ((byte)0xB0),
		CLV ((byte)0xB8),
		TAX ((byte)0xAA),
		TSX ((byte)0xBA),
		LAS ((byte)0xBB),
		INY ((byte)0xC8),
		BNE ((byte)0xD0),
		CLD ((byte)0xD8),
		DEX ((byte)0xCA),
		AXS ((byte)0xCB),
		INX ((byte)0xE8),
		BEQ ((byte)0xF0),
		SED ((byte)0xF8),
		ORA,
		AND,
		EOR,
		ADC,
		STA,
		LDA,
		CMP,
		SBC,
		NOP,
	}
    byte A;
	byte X;
	byte Y;
	byte SP;
	//!PC jest rejestrem 16-bitowym
	short PC;
	byte P;
	//memory
	byte RAM[];
	private Instruction currentInstruction;
	private MemoryMapper memMap;
	//

	void readNextInstruction() {
		byte opcode=memMap.readByte(PC);
		AddressingMode addrMode;
		switch((opcode & 0x1F)) {
			case 0x00:
				if(opcode > 0x80)
					addrMode=AddressingMode.Immediate;
				if(opcode == 0x20)
					addrMode=AddressingMode.Absolute;
				break;
			case 0x02:
				if(opcode > 0x80)
					addrMode=AddressingMode.Immediate;
			case 0x01:
			case 0x03:
				addrMode=AddressingMode.IndirectX;
				break;
			case 0x04:
			case 0x05:
			case 0x06:
			case 0x07:
				addrMode=AddressingMode.ZeroPageIndexed;
				break;
			case 0x09:
			case 0x0B:
				addrMode=AddressingMode.Immediate;
				break;
			case 0x0C:
			case 0x0D:
			case 0x0E:
			case 0x0F:
				addrMode=AddressingMode.Absolute;
				break;
			case 0x10:
				break;
			case 0x11:
			case 0x13:
				addrMode=AddressingMode.IndirectY;
				break;
			case 0x14:
			case 0x15:
				addrMode=AddressingMode.ZeroPageIndexedX;
				break;
			case 0x16:
			case 0x17:
				//0xB6, 0xB7, 0x96, 0x97 sa po Y, reszta po X
				if(opcode >= 0x96 && opcode <=0xB7)
					addrMode=AddressingMode.ZeroPageIndexedY;
				else
					addrMode=AddressingMode.ZeroPageIndexedX;
				break;
			case 0x19:
			case 0x1B:
				addrMode=AddressingMode.AbsoluteIndexedY;
				break;
			case 0x1C:
			case 0x1D:
				addrMode=AddressingMode.AbsoluteIndexedX;
				break;
			case 0x1E:
			case 0x1F:
				//0x9E, 0x9F, 0xBE, 0xBF sa po Y, reszta po X
				if(opcode >= 0x9E && opcode <= 0xBF)
					addrMode=AddressingMode.AbsoluteIndexedY;
				else
					addrMode=AddressingMode.AbsoluteIndexedX;
				break;
			//w pozostalych przypadkach brak adresowania
			default:
				addrMode=AddressingMode.None;
		}
		//a teraz sczytanie rozkazu:
		CMD command;
		byte tmp= (byte) (opcode&0x1F);
		if((tmp&0x03) == 1) {
			switch (opcode & 0xE0) {
				case 0x00:
					command = CMD.ORA;
					break;
				case 0x20:
					command = CMD.AND;
					break;
				case 0x40:
					command = CMD.EOR;
					break;
				case 0x60:
					command = CMD.ADC;
					break;
				case 0x80:
					command=CMD.STA;
					break;
				case 0xA0:
					command=CMD.LDA;
					break;
				case 0xC0:
					command=CMD.CMP;
					break;
				case 0xE0:
					command=CMD.SBC;
					break;
			}
			//niechlubny wyjatek
			if(opcode == 0x89)
				command=CMD.NOP;
		}
		else switch(opcode &0xE0) {
			case 0x00:
				switch(opcode & 0x1F) {
					case 0x00:
						command=CMD.BRK;
						break;
					case 0x04:
					case 0x0C:
					case 0x14:
					case 0x1C:
					case 0x1A:
						command=CMD.NOP;
						break;
				}
				break;
			case 0x20:
				command = CMD.AND;
				break;
			case 0x40:
				command = CMD.EOR;
				break;
			case 0x60:
				command = CMD.ADC;
				break;
			case 0x80:
				command=CMD.STA;
				break;
			case 0xA0:
				command=CMD.LDA;
				break;
			case 0xC0:
				command=CMD.CMP;
				break;
			case 0xE0:
				command=CMD.SBC;
				break;
		}
	}
	void stackPush(byte value){//push decrements, pop increments from 00 to FF with offset 0100
		RAM[0x0100+SP--] = value;
        }
	byte stackPop(){
		return RAM[0x0100+(++SP)];
        }
	void add(byte value){
            
	}
	boolean execute(){
        CMD temp = CMD.values()[RAM[PC]];
		switch (temp){
		case ADD:
			PC++;
			A = (byte)(A + RAM[PC]);
			break;
		case LDA:
			PC++;
			A = RAM[PC];
			break;
		case STOP:
			return false;
		}
		PC++;
		return true;
        }
	void load(){
            
        }
	CPU(){
            RAM = new byte[NESMaJster.Const.MAX_RAM_SIZE];
        }
	void run(){
            do{} while (execute());
        }

	private class MemoryMapper {
	}
}
