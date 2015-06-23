/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;
/**
 *
 * @author Administrator
 */
class CPU {
        static final short MAX_RAM_SIZE = 2048;
        enum CMD{
            STOP ((byte)0x00),
            ADD  ((byte)0x01),
            LDA  ((byte)0x02),
            ;
            
            byte No;         
            CMD(byte input){
                this.No = input;
            }
        }
    	byte A;
	byte X;
	byte Y;
	byte S;
	byte PC;
	byte P;
	//memory
	byte RAM[];
	//
	void stackPush(byte value){//push decrements, pop increments from 00 to FF with offset 0100
        }
	byte stackPop(){
            return 0;
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
            RAM = new byte[MAX_RAM_SIZE];
        }
	void run(){
            do{} while (execute());
        }

}
