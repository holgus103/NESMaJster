/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;


import com.sun.jndi.cosnaming.IiopUrl;
import com.sun.org.apache.bcel.internal.generic.NOP;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOException;

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
            BRK,ORA,STP,SLO,NOP,ASL,PHP,ANC,BPL,CLC,JSR,AND,RLA,BIT,ROL,PLP,BMI,
            SEC,RTI,EOR,SRE,LSR,PHA,ALR,JMP,BVC,CLI,RTS,ADC,RRA,ROR,PLA,ARR,BVS,
            SEI,STA,SAX,STY,STX,DEY,TXA,XAA,BCC,AHX,TYA,TXS,TAS,SHY,SHX,LDY,LDA,
            LDX,LAX,TAY,TAX,BCS,CLV,TSX,LAS,CPY,CMP,DCP,DEC,INY,DEX,AXS,BNE,CLD,
            CPX,SBC,ISC,INC,INX,BEQ,SED;
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
        private String[] ops;
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
                return CMD.valueOf(ops[opcode]);
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
            final String OPCODES_PATH = "C:\\opcodes.txt"; 
		memMap=new MemoryMapper();
                ops = new String[256];
                try{
                FileReader file = new FileReader(OPCODES_PATH);
                BufferedReader textReader = new BufferedReader(file);
                for(int i=0;i<256;i++){
                    ops[i]= textReader.readLine();
                }
                }
                catch(IOException e){
                    
                }
                
	}
	void run(){
		do{/*przerwanie!*/} while (executeInstruction());
	}
}
