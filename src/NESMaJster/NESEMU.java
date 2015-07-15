/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.Random;

/**
 *
 * @author Administrator
 */
public class NESEMU {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) throws InterruptedException {
        JFrame frame=new JFrame("Demo");
        PPU ppu=new PPU();
        CPU cpu=new CPU();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        BufferedImage bi=new BufferedImage(256,240, BufferedImage.TYPE_INT_RGB);
        Random r = new Random();
        for(int i=0;i<256;++i)
            for(int j=0;j<240;++j)
                bi.setRGB(i,j, r.nextInt());
        ppu.ZeroVRAM();
        //podkreslenie gornej lini (tlo)
        for(int j=0;j<2;++j)
            ppu.VRAM.setByte((short)(j*8),(byte)0xFF);
        //literka T
        for(int j=0;j<2;++j) {
            for (int i = 0; i < 3; ++i)
                ppu.VRAM.setByte((short) (16+i+j*8), (byte) 0xFF);
            for (int i = 3; i < 8; ++i)
                ppu.VRAM.setByte((short) (16+i+j*8), (byte) 0x18);
        }
        //literka E
        for(int j=0;j<2;++j) {
            ppu.VRAM.setByte((short)(32+8*j), (byte) 0xFF);
            ppu.VRAM.setByte((short)(39+8*j),(byte)0xFF);
            for(int i=1;i<3;++i)
                ppu.VRAM.setByte((short)(32+i+j*8), (byte) 0x03);
            for(int i=3;i<5;++i)
                ppu.VRAM.setByte((short)(32+i+j*8), (byte) 0x1F);
            for(int i=5;i<7;++i)
                ppu.VRAM.setByte((short)(32+i+j*8), (byte) 0x03);
        }
        //literka S
        for(int j=0;j<2;++j) {
            for(int i=0;i<2;++i)
                ppu.VRAM.setByte((short)(48+i+j*8), (byte) 0xFF);
            for(int i=2;i<3;++i)
                ppu.VRAM.setByte((short)(48+i+j*8), (byte) 0x03);
            for(int i=3;i<5;++i)
                ppu.VRAM.setByte((short)(48+i+j*8), (byte) 0xFF);
            for(int i=5;i<6;++i)
                ppu.VRAM.setByte((short)(48+i+j*8), (byte) 0xC0);
            for(int i=6;i<8;++i)
                ppu.VRAM.setByte((short)(48+i+j*8), (byte) 0xFF);
        }
        ppu.putSprite(0, (byte) 30, (byte) 126, (byte) 0,(byte)1);
        ppu.putSprite(1,(byte)40,(byte)126,(byte)0,(byte)2);
        ppu.putSprite(2,(byte)50,(byte)126,(byte)0,(byte)3);
        ppu.putSprite(3,(byte)60,(byte)126,(byte)0,(byte)1);
        ppu.VRAM.setByte((short) 0x3F03, (byte) 1);
        ppu.VRAM.setByte((short)0x3F13,(byte)1);
        System.out.printf("%x",ppu.VRAM.readByte((short)6));
        frame.getContentPane().add(new JLabel(new ImageIcon(ppu.draw())));
        frame.pack();
        frame.setVisible(true);
// TODO code application logic here
    }
    
}
