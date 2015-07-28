/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;

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
    Screen screen;


    PPU(Screen screen){
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
        this.screen=screen;
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
        PPUSTATUS&=0x7F;
        byte[][] img;
        img=new byte[FRAME_HEIGHT][];
        //linia -1 do zrobienia (nic ciekawego sie w niej nie dzieje...
        //VRAMAddr= (short) (xScroll/8 + ((((PPUCTRL&0x3)-1)&0x3)<<10)+0x73FE);
        //System.out.printf("%x\n",VRAMAddr);
        int realXScroll=xScroll < 0? xScroll+256:xScroll;
        tempVRAMAddr= (short) (realXScroll/8+((PPUCTRL&0x3)<<10));
        VRAMAddr=(short) (tempVRAMAddr+0x73E0);
        for(int i=0;i<FRAME_WIDTH/8-2;++i)
            VRAMAddr=nextVRAMAddr();
        System.out.printf("%x\n",VRAMAddr);
        loadBgRegisters();
        loadBgPaletteReg();
        bgBmpLow>>=8;
        bgBmpHigh>>=8;
        loadBgRegisters();
        System.out.printf("%x\n",VRAMAddr);
        //VRAMAddr-=0x400;
        //--VRAMAddr;
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
            //ladujemy sprite'y dla nastepnej linii do rejestrow
            loadSprites();
            //ladujemy 2 plytki do nastepnej linii!
            loadBgRegisters();
            loadBgPaletteReg();
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
        int offset=xScroll%8;
        if((PPUMASK &0x08) == 0)
            return 0;
        byte res = (byte) ((((bgPaletteHigh>>offset)&1)<<3)+(((bgPaletteLow>>offset)&1)<<2)
                +(((bgBmpHigh>>offset)&1)<<1)+(((bgBmpLow>>offset)&1)));
        return res;
    }
    private short nextVRAMAddr() {
        short nextVRAMAddr =this.VRAMAddr;
        if((nextVRAMAddr &0x1F)==(tempVRAMAddr+31)%32 && (xScroll %8 == 0)) {
            //podnosimy y
            nextVRAMAddr += 0x1000;
            if ((nextVRAMAddr & 0x8000) != 0) {
                //po y==-1(31) mamy y=0
                if((nextVRAMAddr&0x3E0) == 0x3E0)
                    nextVRAMAddr&=0xFC1F;
                else
                    nextVRAMAddr+=0x0020;
            }
            if(xScroll != 0 )
                nextVRAMAddr -=0x0400;
            nextVRAMAddr &=0x7FE0;
            nextVRAMAddr |=tempVRAMAddr&0x1F;
            return nextVRAMAddr;
        }
        if((nextVRAMAddr &0x1F) == 31) {
            //przechodzimy do nastepnego ekranu
            nextVRAMAddr +=0x0400;
            nextVRAMAddr &=0x7FE0;
            return nextVRAMAddr;
        }
        ++nextVRAMAddr;
        nextVRAMAddr &=0x7FFF;
        return nextVRAMAddr;
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
        VRAMAddr=nextVRAMAddr();
        //przesuniecie y wewnatrz plytki
        short nextVRAMAddr=nextVRAMAddr();
        int yOffset=(nextVRAMAddr&0x7000)>>12;
        short addr= (short) (0x2000+(nextVRAMAddr&0xFFF)+yOffset);
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
        short nextVRAMAddr=nextVRAMAddr();
        int y=nextVRAMAddr&0x3E0;
        int x=nextVRAMAddr&0x1F;
        int n=(nextVRAMAddr&0xC00)>>10;
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
        byte[] r={(byte)0x75, (byte)0x27, (byte)0x0, (byte)0x47, (byte)0x8F, (byte)0xAB, (byte)0xA7,
                (byte)0x7F, (byte)0x43, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x1B, (byte)0x0,
                (byte)0x0, (byte)0x0, (byte)0xBC, (byte)0x0, (byte)0x23, (byte)0x83, (byte)0xBF,
                (byte)0xE7, (byte)0xDB, (byte)0xCB, (byte)0x8B, (byte)0x0, (byte)0x0, (byte)0x0,
                (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0xFF, (byte)0x3F, (byte)0x5F,
                (byte)0xA7, (byte)0xF7, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xF3, (byte)0x83,
                (byte)0x4F, (byte)0x58, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0xFF,
                (byte)0xAB, (byte)0xC7, (byte)0xD7, (byte)0xFF, (byte)0xFF, (byte)0xFF, (byte)0xFF,
                (byte)0xFF, (byte)0xE3, (byte)0xAB, (byte)0xB3, (byte)0x9F, (byte)0x0, (byte)0x0,
                (byte)0x0};
        byte[] g={(byte)0x75, (byte)0x1B, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0x0,
                (byte)0x0B, (byte)0x2F, (byte)0x47, (byte)0x51, (byte)0x3F, (byte)0x3F, (byte)0x0,
                (byte)0x0, (byte)0x0, (byte)0xBC, (byte)0x73, (byte)0x3B, (byte)0x0, (byte)0x0,
                (byte)0x0, (byte)0x2B, (byte)0x4F, (byte)0x73, (byte)0x97, (byte)0xAB, (byte)0x93,
                (byte)0x83, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0xFF, (byte)0xBF, (byte)0x97,
                (byte)0x8B, (byte)0x7B, (byte)0x77, (byte)0x77, (byte)0x9B, (byte)0xBF, (byte)0xD3,
                (byte)0xDF, (byte)0xF8, (byte)0xEB, (byte)0x0, (byte)0x0, (byte)0x0, (byte)0xFF,
                (byte)0xE7, (byte)0xD7, (byte)0xCB, (byte)0xC7, (byte)0xC7, (byte)0xBF, (byte)0xDB,
                (byte)0xE7, (byte)0xFF, (byte)0xF3, (byte)0xFF, (byte)0xFF, (byte)0x0, (byte)0x0,
                (byte)0x0};
        byte[] b = {(byte) 0x75, (byte) 0x8F, (byte) 0xAB, (byte) 0x9F, (byte) 0x77, (byte) 0x13,
                (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0x17, (byte) 0x5F,
                (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0xBC, (byte) 0xEF, (byte) 0xEF,
                (byte) 0xF3, (byte) 0xBF, (byte) 0x5B, (byte) 0x0, (byte) 0x0F, (byte) 0x0,
                (byte) 0x0, (byte) 0x0, (byte) 0x3B,
                (byte) 0x8B, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
                (byte) 0xFD, (byte) 0xFF, (byte) 0xB7, (byte) 0x63, (byte) 0x3B, (byte) 0x3F, (byte) 0x13,
                (byte) 0x4B, (byte) 0x98, (byte) 0xDB, (byte) 0x0, (byte) 0x0, (byte) 0x0, (byte) 0xFF,
                (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xDB, (byte) 0xB3, (byte) 0xAB,
                (byte) 0xA3, (byte) 0xA3, (byte) 0xBF, (byte) 0xCF, (byte) 0xF3, (byte) 0x0, (byte) 0x0,
                (byte) 0x0};
        IndexColorModel icm=new IndexColorModel(6,64,r,g,b);
        BufferedImage bi=new BufferedImage(FRAME_WIDTH,FRAME_HEIGHT,
                BufferedImage.TYPE_BYTE_INDEXED,icm);
        for(int i=0;i<FRAME_WIDTH;++i)
            for(int j=0;j<FRAME_HEIGHT;++j) {
                bi.setRGB(i,j,icm.getRGB(picture[j][i]));
                //if (picture[j][i] == 0)
                //    bi.setRGB(i, j, 0);
               // else {
                 //   bi.setRGB(i, j, 0x00FFFFFF);
                //}
            }
        screen.setImg(bi);
        screen.repaint();
        return bi;
    }
}
