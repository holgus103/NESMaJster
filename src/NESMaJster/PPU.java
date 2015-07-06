/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Administrator
 */
public class PPU {
    MemoryMapper VRAM;
    private static final int FRAME_HEIGHT=240;
    private static final int FRAME_WIDTH=256;
    byte[] canvas;
    //$2000
    byte PPUCTRL,PPUMASK;
    //$2002
    byte PPUSTATUS,OAMADDR,OAMDATA,PPUSCROLL,PPUADDR,PPUDATA;
    //$4014
    byte OAMDMA;
    byte VRAMAddr,tempVRAMAddr,xScroll,writeToggle;
    Sprite[] sprites;
    int nSprites;
    PPU(){
        VRAM = new MemoryMapper();
        sprites=new Sprite[64];
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
    class Sprite{
        byte y;
        byte tileIndex;
        byte attributes;
        byte x;
        static final byte WIDTH=8;
        static final byte HEIGHT=8;
        public boolean isInLine(int nr) {
            return y<nr && y+HEIGHT>=nr;
        }
        public boolean isInColumn(int nr) {
            return x<=nr &&x+WIDTH<nr;
        }
    }
    private byte[][] render() {
        byte[][] img;
        byte[][] sprites;
        img = drawBackground();
        //w osobnym watku
        sprites=drawForeground();
        throw new NotImplementedException();
    }
    private byte[] drawLine(byte nr) {
        List<Sprite> spr=spritesInLine(nr);
        byte attrTblPart=0,thirdBit=0,forthBit=0;
        short nameTblOff= (short) (0x2000+(nr/32)*8);
        short attrTblOff= (short) (0x23C0+nr%8+(nr/8)*32);
        byte[] bg=new byte[FRAME_WIDTH];
        for(int x=0;x<FRAME_WIDTH;++x) {
            //zaladuj dane o nowym bloku pikseli
            //attr - 32x32
            //third/forthBit - 8x1
            if((x&31) == 0) {
                attrTblPart = VRAM.readByte((short) (attrTblOff +
                        (x >> 5)));
                if(nr%32 > 16)
                    attrTblPart>>=4;
            }
            if((x&7) == 0) {
                short tmp= (short) (VRAM.readByte((short) (nameTblOff+(x>>2)))+
                                        VRAM.readByte((short) (nameTblOff+(x>>2)+1))<<8);
                thirdBit=VRAM.readByte((short) ((x<<1)+nameTblOff));
                forthBit=VRAM.readByte((short) ((x<<1)+nameTblOff+8));
            }
            bg[x] = (byte) ((attrTblPart & 3) + (thirdBit & 1) << 1 + (forthBit & 1));
            //przesun na nastepny piksel
            thirdBit>>=1;
            forthBit>>=1;
            if((x&15) == 0)
                attrTblPart>>=2;
            //byte fgColor=x-cur.x+cur.tileIndex;
        }
        return bg;
    }
    public List<Sprite> spritesInLine(byte nr) {
        List<Sprite> l= new ArrayList<>();
        for(int i=0;i<nSprites;++i)
            if(sprites[i].isInLine(i)) {
                l.add(sprites[i]);
                if(l.size() > 8)
                    break;
            }
        return l;
    }
    public Sprite spriteAt(Sprite[] s,int x) {
        for(int i=0;i<s.length;++i)
            if(s[i].isInColumn(x))
                return s[i];
        return null;
    }
    private void drawSprite(int tmp) {
        if((PPUMASK & 0x08) == 0)
            return;
    }
    private byte[][] drawBackground() {
        if((PPUMASK &0x04) == 0)
            return null;
        byte[][] bg=new byte[FRAME_HEIGHT][];
        for(int i=0;i<FRAME_HEIGHT;++i)
            bg[i]=drawLine((byte)i);
    }
    private byte[][] drawForeground(){
        return null;
    }
}
