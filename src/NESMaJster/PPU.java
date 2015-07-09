/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.List;

/**
 *
 * @author Administrator
 */
public class PPU {
    MemoryMapper VRAM;
    private static final int FRAME_HEIGHT=240;
    private static final int FRAME_WIDTH=256;
    private static final int PRIMARY_OAM_SIZE=64;
    private static final int SECONDARY_OAM_SIZE=8;
    byte[] canvas;
    //$2000
    byte PPUCTRL,PPUMASK;
    //$2002
    byte PPUSTATUS,OAMADDR,OAMDATA,PPUSCROLL,PPUADDR,PPUDATA;
    //$4014
    byte OAMDMA;
    byte VRAMAddr,tempVRAMAddr,xScroll,writeToggle;

    Sprite[] primaryOAM;
    int nSprites;
    Sprite[] secondaryOAM;

    //rejestry do tla
    short bgBmpHigh,bgBmpLow;
    byte bgPaletteHigh,bgPaletteLow;
    boolean bgNextPaletteHigh,bgNextPaletteLow;

    //rejestry do sprite'ow
    byte[] fgBmpHigh,fgBmpLow,fgXPos,fgAttr;


    PPU(){
        VRAM = new MemoryMapper();
        primaryOAM =new Sprite[PRIMARY_OAM_SIZE];
        secondaryOAM=new Sprite[SECONDARY_OAM_SIZE];
        fgBmpHigh=new byte[SECONDARY_OAM_SIZE];
        fgBmpLow=new byte[SECONDARY_OAM_SIZE];
        fgXPos=new byte[SECONDARY_OAM_SIZE];
        fgAttr=new byte[SECONDARY_OAM_SIZE];
    }
    class MemoryMapper {
        byte[] VRAM;
        static final int MAX_VRAM_SIZE=0x3F20-0x0F00;
        MemoryMapper() {
            VRAM=new byte[MAX_VRAM_SIZE];
        }
        byte readByte(short index) {
            int index2=indexFromShort(index);
            return VRAM[index2];
        }
        void setByte(short index,byte value) {
            VRAM[indexFromShort(index)]= value;
        }
        int indexFromShort(short index) {
            int index2=index < 0?index+65536:index;
            index2%=0x4000;
            if(index2 > 0x3000)
                index2-=0x0F00;
            return index2;
        }
    }
    private class Sprite{
        public byte y;
        public byte tileIndex;
        public byte attributes;
        public byte x;
        static final byte WIDTH=8;
        static final byte HEIGHT=8;
        public boolean isInLine(int nr) {
            return y<nr && y+HEIGHT>=nr;
        }
        public boolean isInColumn(int nr) {
            return x<=nr &&x+WIDTH<nr;
        }
        public Sprite(byte y, byte tileIndex, byte attributes, byte x) {
            this.y = y;
            this.tileIndex = tileIndex;
            this.attributes = attributes;
            this.x = x;
        }
        public Sprite() {
            this.y= (byte) 0xFF;
            this.tileIndex= (byte) 0xFF;
            this.attributes=(byte)0xFF;
            this.x= (byte) 0xFF;
        }
    }
    private byte[][] render() throws InterruptedException {
        byte[][] img;
        img=new byte[FRAME_HEIGHT][];
        //linia -1 do zrobienia (nic ciekawego sie w niej nie dzieje...
        loadBgRegisters();
        loadBgPaletteReg();
        updateBgRegisters();
        for(int i=0;i<FRAME_HEIGHT;++i) {
            //rownolegle trzeba wyznaczyc sprite'y do nastepnej linii - spritesInLine
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    //nie mozna uzyc i - trzeba cos wymyslic...
                    spritesInLine(i);
                }
            });
            t.start();
            img[i] = drawLine();
            t.join();
            //automagicznie sie podniesie y bo x==32
            ++VRAMAddr;
            //trzeba zaktualizowac przesuniecie wewnatrz plytki
            VRAMAddr+=0x2000;
            //ladujemy sprite'y dla nastepnej linii do rejestrow
            loadSprites();
            //ladujemy 2 plytki do nastepnej linii!
            loadBgRegisters();
            loadBgPaletteReg();
            updateBgRegisters();
            //??? rozkmin to Kuba
            //Two bytes are fetched, but the purpose for this is unknown. These fetches are 2 PPU cycles each.
        }
        //teraz czekamy jedna linie dla picu
        //ustawic tu VBLANK i czekac (CPU moze edytowac pamiec)
        PPUSTATUS|=0x80;
        if((PPUCTRL & 0x80) != 0)
            //wyslac NMI (cokolwiek to znaczy)
            ;
        return img;
    }
    private byte getBgPixel() {
        //offset = 1<<(fineX % 8)
        int offset=1<<(xScroll%8));
        if((PPUMASK &0x08) == 0)
            return 0;
        byte res = (byte) ((bgPaletteHigh&offset)<<3+(bgPaletteLow&offset)<<2
                +(bgBmpHigh&offset)<<1+(bgBmpLow&offset));
        return res;
    }
    private void updateBgRegisters() {
        bgPaletteHigh>>=1;
        bgPaletteLow>>=1;
        bgBmpHigh>>=1;
        bgBmpLow>>=1;
        if(bgNextPaletteHigh)
            bgBmpHigh|=0x80;
        else
            bgBmpHigh&=0x70;
        if(bgNextPaletteLow)
            bgBmpLow|=0x80;
        else
            bgBmpLow&=0x70;
    }
    private void loadBgRegisters() {
        bgBmpHigh&=0x00FF;
        bgBmpLow&=0x00FF;
        ++VRAMAddr;
        //przesuniecie y wewnatrz plytki
        int yOffset=VRAMAddr&0xE000;
        int n=VRAMAddr&0x1800;
        short addr= (short) (n*0x400+(VRAMAddr&0x7FF)+yOffset);
        bgBmpHigh&=0x00FF;
        bgBmpHigh|=VRAM.readByte((short) (0x0000+VRAM.readByte(addr)+yOffset))<<8;
        bgBmpLow&=0x00FF;
        bgBmpLow|=VRAM.readByte((short) (0x0000+VRAM.readByte((short) (addr+8))+yOffset))<<8;
    }
    private void loadBgPaletteReg() {
        //y jest przesuniete!!!
        int y=VRAMAddr&0x3E0;
        int x=VRAMAddr&0x1F;
        byte bgNextPalette=VRAM.readByte((short) (y/4+x/4+0x300+0x400*n));
        int xMask=(x&0x10) > 0?1:1<<3;
        int yMask=(y&0x10) > 0?1:1<<5;
        bgNextPaletteHigh=((bgNextPalette&(2*xMask*yMask)) == 1);
        bgNextPaletteLow=((bgNextPalette&(xMask*yMask)) == 1);
    }
    private byte getPixel() {
        byte fgColor;
        byte bgColor=getBgPixel();
        if((PPUMASK &0x10) != 0)
            //rysujemy sprite'y (jak jest 0 to nie rysujemy)
            for(int i=0;i<8;++i)
                if(fgXPos[i] == 0) {
                    fgColor = (byte) ((fgAttr[i]&0x3)<<2+(fgBmpHigh[i] & 1) << 1 + (fgBmpLow[i] & 1));
                    fgBmpHigh[i]>>=1;
                    fgBmpLow[i]>>=1;
                    if(fgColor != 0) {
                        if(i == 0 && (bgColor &0x03)!=0)
                            //sprite zero hit
                            PPUSTATUS|=0x40;
                        if((fgAttr[i]&0x10) ==0)
                            return (byte) fgColor;
                        else
                            return bgColor;
                    }
                }
        return bgColor;
    }
    private byte[] drawLine() {
        byte[] line=new byte[FRAME_WIDTH];
        for(int i=0;i<FRAME_WIDTH;++i) {
            line[i] = getPixel();
            if(i%8 == 0 && i != 0)
                loadBgRegisters();
            if(i%16 == 0 && i != 0)
                loadBgPaletteReg();
            updateBgRegisters();
        }
        return line;
    }
    private void loadSprites() {
        for(int i=0;i<8;++i) {
            if(secondaryOAM[i].tileIndex == 0xFF) {
                //powinny byc odczyty, ale moze je sobie darujemy? I tak trzeba podmienic na 0...
                fgBmpHigh[i]=0;
                fgBmpLow[i]=0;
            }
            else {
                fgBmpHigh[i] = VRAM.readByte((short) (secondaryOAM[i].tileIndex + (VRAMAddr & (0xE000) >> 13)));
                fgBmpLow[i] = VRAM.readByte((short) (secondaryOAM[i].tileIndex + (VRAMAddr & (0xE000) >> 13) + 8));
            }
            fgAttr[i] = secondaryOAM[i].attributes;
            fgXPos[i]=secondaryOAM[i].x;
        }
    }
    public void spritesInLine(byte nr) {
        int nSprInLn=0;
        for(int i=0;i<SECONDARY_OAM_SIZE;++i)
            secondaryOAM[i].tileIndex= (byte) 0xFF;
        for(int i=0;i<nSprites;++i)
            if(primaryOAM[i].isInLine(i)) {
                secondaryOAM[nSprInLn++]= primaryOAM[i];
                if(nSprInLn >= 8)
                    break;
            }
        //podobno nie tak to dziala... trzeba bedzie poprawic
        //tak mialo dzialac, ale sa bugi...
        if(nSprInLn >= 8)
            PPUSTATUS|=0x20;
    }
}
