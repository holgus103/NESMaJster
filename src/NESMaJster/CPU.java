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

	//zagniezdzone klasy
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

		public Instruction(CMD opcode, AddressingMode addrMode, byte[] params) {
			this.opcode = opcode;
			this.addrMode = addrMode;
			this.params = params;
		}
		public CMD command() {
			return opcode;
		}
	}
	private enum CMD {
		BRK((byte) 0x00), PHP((byte) 0x08), BPL((byte) 0x10), CLC((byte) 0x18),
		JSR((byte) 0x20), PLP((byte) 0x28), BMI((byte) 0x30), SEC((byte) 0x38),
		RTI((byte) 0x40), PHA((byte) 0x48), BVC((byte) 0x50), CLI((byte) 0x58),
		ALR((byte) 0x4B), RTS((byte) 0x60), PLA((byte) 0x68), BVS((byte) 0x70),
		SEI((byte) 0x78), ARR((byte) 0x6B), DEY((byte) 0x88), BCC((byte) 0x90),
		TYA((byte) 0x98), SHY((byte) 0x9C), TXA((byte) 0x8A), TXS((byte) 0x9A),
		SHX((byte) 0x9E), XAA((byte) 0x8B), TAS((byte) 0x9B), TAY((byte) 0xAC),
		BCS((byte) 0xB0), CLV((byte) 0xB8), TAX((byte) 0xAA), TSX((byte) 0xBA),
		LAS((byte) 0xBB), INY((byte) 0xC8), BNE((byte) 0xD0), CLD((byte) 0xD8),
		DEX((byte) 0xCA), AXS((byte) 0xCB), INX((byte) 0xE8), BEQ((byte) 0xF0),
		SED((byte) 0xF8), ORA((byte) 0x01), AND((byte) 0x21), EOR((byte) 0x41),
		ADC((byte) 0x61), STA((byte) 0x81), LDA((byte) 0xA1), CMP((byte) 0xC1),
		SBC((byte) 0xE1), NOP((byte) 0x04), BIT((byte) 0x24), JMP((byte) 0x4C),
		CPX((byte) 0xE0), CPY((byte) 0xC0), STY((byte) 0x84), LDY((byte) 0xA0),
                STP((byte) 0x02), ASL((byte) 0x06)
                ;
		CMD(byte b) {}
	}
	private class MemoryMapper {
		static final short MAX_RAM_SIZE=2048;
		private byte[] RAM;
		public MemoryMapper() {
			RAM=new byte[MAX_RAM_SIZE];
		}
		public byte getByte(short pos) {
			return RAM[pos];
		}
		public void setByte(short pos,byte val) {
			RAM[pos]=val;
		}
		public short getShort(short pos) {
			byte loWord=getByte(pos);
			byte hiWord=getByte((short) (pos+1));
			return (short) ((hiWord<<8)+loWord);
		}
		public void setShort(short pos,short val) {
			byte loWord= (byte) (val&0xFF);
			byte hiWord= (byte) (val>>8);
			RAM[pos]=loWord;
			RAM[pos+1]=hiWord;
		}
	}

	//pola
    byte A;
	byte X;
	byte Y;
	byte SP;
	//!PC jest rejestrem 16-bitowym
	short PC;
	byte P;
	private Instruction currentInstruction;
	private MemoryMapper memMap;
	//

	//metody
	void readNextInstruction() {
		byte opcode=memMap.getByte(PC);
		AddressingMode addrMode=addressingMode(opcode);
		CMD command=command(opcode);

	}
	boolean executeInstruction() {
		switch(currentInstruction.command()) {
			case PHP:
				//wrzuc na stos status procesora
				stackPush(P);
				break;
			case CLC:
				//wyzeruj bit przeniesienia
				P&= 0xFE;
				break;
			case PLP:
				P=stackPop();
				break;
			case SEC:
				P|=0x01;
				break;
			case PHA:
				stackPush(A);
				break;
			case PLA:
				A=stackPop();
				break;
			case CLI:
				P&=0xFB;
				break;
			case SEI:
				P|=0x04;
				break;
		}
		return true;
	}
	private AddressingMode addressingMode(byte opcode) {
		AddressingMode addrMode=AddressingMode.None;
		switch((opcode & 0x1F)) {
			case 0x00:
				if(opcode > 0x80)
					addrMode= AddressingMode.Immediate;
				if(opcode == 0x20)
					addrMode= AddressingMode.Absolute;
				break;
			case 0x02:
				if(opcode > 0x80)
					addrMode= AddressingMode.Immediate;
			case 0x01:
			case 0x03:
				addrMode= AddressingMode.IndirectX;
				break;
			case 0x04:
			case 0x05:
			case 0x06:
			case 0x07:
				addrMode= AddressingMode.ZeroPageIndexed;
				break;
			case 0x09:
			case 0x0B:
				addrMode= AddressingMode.Immediate;
				break;
			case 0x0C:
			case 0x0D:
			case 0x0E:
			case 0x0F:
				addrMode= AddressingMode.Absolute;
				break;
			case 0x10:
				break;
			case 0x11:
			case 0x13:
				addrMode= AddressingMode.IndirectY;
				break;
			case 0x14:
			case 0x15:
				addrMode= AddressingMode.ZeroPageIndexedX;
				break;
			case 0x16:
			case 0x17:
				//0xB6, 0xB7, 0x96, 0x97 sa po Y, reszta po X
				if(opcode >= 0x96 && opcode <=0xB7)
					addrMode= AddressingMode.ZeroPageIndexedY;
				else
					addrMode= AddressingMode.ZeroPageIndexedX;
				break;
			case 0x19:
			case 0x1B:
				addrMode= AddressingMode.AbsoluteIndexedY;
				break;
			case 0x1C:
			case 0x1D:
				addrMode= AddressingMode.AbsoluteIndexedX;
				break;
			case 0x1E:
			case 0x1F:
				//0x9E, 0x9F, 0xBE, 0xBF sa po Y, reszta po X
				if(opcode >= 0x9E && opcode <= 0xBF)
					addrMode= AddressingMode.AbsoluteIndexedY;
				else
					addrMode= AddressingMode.AbsoluteIndexedX;
				break;
			//w pozostalych przypadkach brak adresowania
			default:
				addrMode= AddressingMode.None;
		}
		return addrMode;
	}
	private CMD command(byte opcode) {
		CMD command;
		byte tmp = (byte) (opcode & 0x1F);
		switch (opcode & 0x03) {//0000 0011
			case 0://xxxx xx00
				if((opcode & 0x07) == 4) {//xxxx x100
					switch(opcode & 0xE0) {//1110 0000
						case 0x00://000x x100
							return CMD.NOP;
						case 0x20://001x x100 - 2,3 | 4,C
							if((opcode & 0x10) == 0)
								return CMD.BIT;
							else
								return CMD.NOP;
						case 0x40://4,5|4,C
						case 0x60://6,7|4,C
							if((opcode & 0x1F) == 0x0C)
								return CMD.JMP;
							else
								return CMD.NOP;
						case 0x80://8,9|4,C
							if((opcode & 0x1F) == 0x1C)
								return CMD.SHY;
							else
								return CMD.STY;
						case 0xA0://A,B|4,C
							return CMD.LDY;
						case 0xC0://C,D| 4,C
							if((opcode & 0x10) == 0)
								return CMD.CPY;
							else
								return CMD.NOP;
						case 0xE0://E,F| 4,C
							if((opcode & 0x10) == 0)
								return CMD.CPX;
							else
								return CMD.NOP;
					}
				}
				else {
					switch((int)opcode) {//xxxx x000
						case 0x80:
							return CMD.NOP;
						case 0xA0:
							return CMD.LDY;
						case 0xC0:
							return CMD.CPY;
						case 0xE0:
							return CMD.CPX;
						default:
							return CMD.values()[opcode];
					}
				}
				break;
			case 1://xxxx xx01
				switch (opcode & 0xE0) {//1110 0000 
					case 0x00://000x xx01 - 0,1|1,5,9,D
						return CMD.ORA;
					case 0x20://2,3
						return CMD.AND;
					case 0x40://4,5
						return CMD.EOR;
					case 0x60://6,7
						return CMD.ADC;
					case 0x80://8,9
						return CMD.STA;
					case 0xA0://A,b
						return CMD.LDA;
					case 0xC0://C,D
						return CMD.CMP;
					case 0xE0://E,F
						return CMD.SBC;
				}
				//niechlubny wyjatek
				if (opcode == 0x89)
					return CMD.NOP;
				break;
			case 2://xxxx xx10
                            if((opcode & 0x8F) ==2 || (opcode & 0x12) == 0x12 )
                                command = CMD.STP;
                            else{
                                
                            switch(opcode & 0xE0){
                                case 0x00:
                                    if((opcode & 0xFF) == 0x1A)
                                        command =CMD.NOP;
                                    else
                                        command = CMD.ASL;
                                            
                                case 0x20:
                                case 0x40:
                                case 0x60:
                                case 0x80:
                                case 0xA0:
                                case 0xC0:
                                case 0xE0:
                                      
                            }
                            }
			case 3://xxxx xx11
				switch (opcode & 0xE0) {//1110 0000
					case 0x00://000x xx11
						switch (opcode & 0x1F) {//0001 1111 
							case 0x00:
								command = CMD.BRK;
								break;
							case 0x04:
							case 0x0C:
							case 0x14:
							case 0x1C:
							case 0x1A:
								command = CMD.NOP;
								break;
						}
						break;
					case 0x20://001x xx11
						command = CMD.AND;
						break;
					case 0x40:
						command = CMD.EOR;
						break;
					case 0x60:
						command = CMD.ADC;
						break;
					case 0x80:
						command = CMD.STA;
						break;
					case 0xA0:
						command = CMD.LDA;
						break;
					case 0xC0:
						command = CMD.CMP;
						break;
					case 0xE0:
						command = CMD.SBC;
						break;
				}
				break;
                        default:
			return CMD.values()[opcode];
		}
                return command;
	}

	void stackPush(byte value) {//push decrements, pop increments from 00 to FF with offset 0100
		memMap.setByte((short) (0x0100 + SP), value);
		--SP;
	}

	byte stackPop() {
		++SP;
		return memMap.getByte((short) (0x0100 + SP));
	}

	void add(byte value) {

	}
	void load(){
            
	}
	CPU(){
		memMap=new MemoryMapper();
	}
	void run(){
		do{/*przerwanie!*/} while (executeInstruction());
	}
}
