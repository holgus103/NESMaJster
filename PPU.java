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
public class PPU {
    static final int MAX_VRAM_SIZE = 0x4000;
    byte VRAM[];
    PPU(){
        VRAM = new byte[MAX_VRAM_SIZE];
    }
    
}
