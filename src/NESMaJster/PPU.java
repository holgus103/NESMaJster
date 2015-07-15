/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.awt.image.BufferedImage;
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
    short VRAMAddr,tempVRAMAddr;
    byte xScroll,writeToggle;

    Sprite[] primaryOAM;
    int nSprites;
    Sprite[] secondaryOAM;
    int height;
    int i;

    //rejestry do tla
    short bgBmpHigh,bgBmpLow;
    byte bgPaletteHigh,bgPaletteLow;
    boolean bgNextPaletteHigh,bgNextPaletteLow;

    //rejestry do sprite'ow
    byte[] fgBmpHigh,fgBmpLow,fgXPos,fgAttr,timesUsed;


    PPU(){
        VRAM = new MemoryMapper();
        primaryOAM =new Sprite[PRIMARY_OAM_SIZE];
        secondaryOAM=new Sprite[SECONDARY_OAM_SIZE];
        fgBmpHigh=new byte[SECONDARY_OAM_SIZE];
        fgBmpLow=new byte[SECONDARY_OAM_SIZE];
        fgXPos=new byte[SECONDARY_OAM_SIZE];
        fgAttr=new byte[SECONDARY_OAM_SIZE];
        timesUsed=new byte[SECONDARY_OAM_SIZE];
        PPUCTRL=PPUMASK=PPUSTATUS=0;
        PPUMASK=0x1E;
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
            if(index2 >= 0x3F20)
                index2=0x3F00+(index2&0x1F);
            if(index2 >= 0x3F00)
                index2-=0x0F00;
            else if(index2 >= 0x3000)
                index2-=0x1000;
            return index2;
        }
    }
    private class Sprite{
        public byte y;
        public byte tileIndex;
        public byte attributes;
        public byte x;
        public static final byte WIDTH=8;
        public boolean isInLine(int nr) {
            int properY=y<0?y+256:y;
            return properY<nr && properY+height>=nr;
        }
        public boolean isInColumn(int nr) {
            int properX=x<0?x+256:x;
            return properX<=nr &&properX+WIDTH<nr;
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
    public byte[][] render() throws InterruptedException {
        byte[][] img;
        img=new byte[FRAME_HEIGHT][];
        //linia -1 do zrobienia (nic ciekawego sie w niej nie dzieje...
        VRAMAddr= (byte) (xScroll/8 + ((PPUCTRL&0x3)<<10)-1);
        tempVRAMAddr= VRAMAddr;
        loadBgRegisters();
        loadBgPaletteReg();
        bgBmpLow>>=8;
        bgBmpHigh>>=8;
        loadBgRegisters();
        --VRAMAddr;
        //updateBgRegisters();
        for(i=0;i<FRAME_HEIGHT;++i) {
            //rownolegle trzeba wyznaczyc sprite'y do nastepnej linii - spritesInLine
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    //nie mozna uzyc i - trzeba cos wymyslic...
                    spritesInLine((byte) (i+1));
                }
            });
            t.start();
            img[i] = drawLine();
            t.join();
            //automagicznie sie podniesie y bo x==32
            //++VRAMAddr;
            //trzeba zaktualizowac przesuniecie wewnatrz plytki
            VRAMAddr+=0x1000;
            if((VRAMAddr&0x8000) == 0)
                VRAMAddr-=0x0020;
            //VRAMAddr jest 15-bitowy!
            VRAMAddr&=0x7FFF;
            //ladujemy sprite'y dla nastepnej linii do rejestrow
            loadSprites();
            //ladujemy 2 plytki do nastepnej linii!
            loadBgRegisters();
            loadBgPaletteReg();
            bgBmpHigh>>=8;
            bgBmpLow>>=8;
            loadBgRegisters();
            --VRAMAddr;
            //??? rozkmin to Kuba
            //Two bytes are fetched, but the purpose for this is unknown. These fetches are 2 PPU cycles each.
        }
        if((PPUMASK & 0x01) == 1)
            //przekonwertowac na skale szarosci
                ;
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
        int offset=(1<<(xScroll%8));
        if((PPUMASK &0x08) == 0)
            return 0;
        byte res = (byte) (((bgPaletteHigh&offset)<<3)+((bgPaletteLow&offset)<<2)
                +((bgBmpHigh&offset)<<1)+((bgBmpLow&offset)));
        return res;
    }
    private void updateBgRegisters() {
        bgPaletteHigh>>=1;
        bgPaletteLow>>=1;
        bgBmpHigh>>=1;
        bgBmpLow>>=1;
        if(bgNextPaletteHigh)
            bgPaletteHigh|=0x80;
        else
            bgPaletteHigh&=0x70;
        if(bgNextPaletteLow)
            bgPaletteLow|=0x80;
        else
            bgPaletteLow&=0x70;
    }
    private void loadBgRegisters() {
        ++VRAMAddr;
        //przesuniecie y wewnatrz plytki
        int yOffset=(VRAMAddr&0x7000)>>12;
        int n=(VRAMAddr&0x0C00)>>10;
        short addr= (short) (0x2000+n*0x400+(VRAMAddr&0x3FF)+yOffset);
        int patternTable=0;
        if((PPUCTRL & 0x10) == 1)
            patternTable=0x1000;
        bgBmpHigh&=0x00FF;
        bgBmpHigh|=VRAM.readByte((short) (patternTable+VRAM.readByte(addr)*16+yOffset))<<8;
        bgBmpLow&=0x00FF;
        bgBmpLow|=VRAM.readByte((short) (patternTable+VRAM.readByte((short) (addr+8))*16+yOffset))<<8;
    }
    private void loadBgPaletteReg() {
        //y jest przesuniete!!!
        int y=VRAMAddr&0x3E0;
        int x=VRAMAddr&0x1F;
        int n=(VRAMAddr&0xC00)>>10;
        byte bgNextPalette=VRAM.readByte((short) (y/4+x/4+0x23C0+0x400*n));
        int xMask=(x&0x10) > 0?1:1<<3;
        int yMask=(y&0x10) > 0?1:1<<5;
        bgNextPaletteHigh=((bgNextPalette&(2*xMask*yMask)) == 1);
        bgNextPaletteLow=((bgNextPalette&(xMask*yMask)) == 1);
    }
    private byte getPixel() {
        byte fgColor;
        byte bgColor=getBgPixel();
        //nie rysujemy tla
        if((PPUMASK & 0x08) == 0)
            bgColor=0;
        if((PPUMASK &0x10) != 0) {
            //rysujemy sprite'y (jak jest 0 to nie rysujemy)
            for(int i=0;i<8;++i) {
                if (fgXPos[i] == 0)
                    timesUsed[i]++;
                else
                    fgXPos[i]--;
            }
            for (int i = 0; i < 8; ++i) {
                if (timesUsed[i] > 0 && timesUsed[i] <= 8) {
                    fgColor = (byte) (((fgAttr[i] & 0x3) << 2) + ((fgBmpHigh[i] & 1) << 1) + (fgBmpLow[i] & 1));
                    fgBmpHigh[i] >>= 1;
                    fgBmpLow[i] >>= 1;
                    if (fgColor != 0) {
                        if (i == 0 && (bgColor & 0x03) != 0)
                            //sprite zero hit
                            PPUSTATUS |= 0x40;
                        if ((fgAttr[i] & 0x10) == 0)
                            return VRAM.readByte((short) (0x3F10 + fgColor));
                        else
                            return VRAM.readByte((short) (0x3F00 + bgColor));
                    }
                }
            }
        }
        return VRAM.readByte((short) (0x3F00+bgColor));
    }
    private byte[] drawLine() {
        byte[] line=new byte[FRAME_WIDTH];
        for(int i=0;i<SECONDARY_OAM_SIZE;++i)
            timesUsed[i]=0;
        for(int i=0;i<FRAME_WIDTH;++i) {
            if(i < 8 && ((PPUMASK &0x4) == 0)) {
                if((PPUMASK & 0x04) == 0)
                    line[i]=getBgPixel();
                if((PPUMASK & 0x02) == 0)
                    //nie rysuj tla...
                ;
            }
            else
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
            if(secondaryOAM[i].tileIndex == (byte)0xFF) {
                //powinny byc odczyty, ale moze je sobie darujemy? I tak trzeba podmienic na 0...
                fgBmpHigh[i]=0;
                fgBmpLow[i]=0;
            }
            else {
                int patternTable=0;
                if((PPUCTRL & 0x08) == 1)
                    patternTable=0x1000;
                fgBmpHigh[i] = VRAM.readByte((short) (secondaryOAM[i].tileIndex*16 + this.i-secondaryOAM[i].y+patternTable));
                fgBmpLow[i] = VRAM.readByte((short) (secondaryOAM[i].tileIndex*16 + this.i-secondaryOAM[i].y + 8 + patternTable));
            }
            fgAttr[i] = secondaryOAM[i].attributes;
            fgXPos[i]=secondaryOAM[i].x;
        }
    }
    public void spritesInLine(byte nr) {
        int nSprInLn=0;
        int realNr=nr<0?nr+256:nr;
        if((PPUCTRL & 0x20) != 0)
            height = 16;
        else
            height=8;
        for(int i=0;i<SECONDARY_OAM_SIZE;++i)
            secondaryOAM[i]= new Sprite();
        for(int i=0;i<nSprites;++i)
            if(primaryOAM[i].isInLine(realNr)) {
                secondaryOAM[nSprInLn++]= primaryOAM[i];
                if(nSprInLn >= 8)
                    break;
            }
        //podobno nie tak to dziala... trzeba bedzie poprawic
        //tak mialo dzialac, ale sa bugi...
        if(nSprInLn >= 8)
            PPUSTATUS|=0x20;
    }
    public void ZeroVRAM() {
        VRAM.setByte((short)0x3F21,(byte)0);
        for(int i=0;i<0x10000;++i)
            VRAM.setByte((short)i,(byte)0);
    }
    public void putSprite(int pos, byte x, byte y, byte attr,byte tileNr) {
        primaryOAM[pos]=new Sprite(y,tileNr,attr,x);
        nSprites++;
    }
    BufferedImage draw() throws InterruptedException {
        byte[][] picture=render();
        BufferedImage bi=new BufferedImage(FRAME_WIDTH,FRAME_HEIGHT,BufferedImage.TYPE_INT_RGB);
        for(int i=0;i<FRAME_WIDTH;++i)
            for(int j=0;j<FRAME_HEIGHT;++j) {
                if (picture[j][i] == 0)
                    bi.setRGB(i, j, 0);
                else {
                    bi.setRGB(i, j, 0x00FFFFFF);
                }
            }
        return bi;
    }
}
