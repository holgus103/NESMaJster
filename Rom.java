/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package NESMaJster;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
/**
 *
 * @author Administrator
 */
public class Rom {
    static final byte HEADER_SIZE = 16;
    static final byte HEADER[] = {0x4e,0x45,0x53,0x1a};
    byte header[];
    String path;
    Rom(String path){
        //czytamy nagłówek romu i sprawdzamy czy jest spoko
        header = new byte[HEADER_SIZE];
        try{
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(path));
        in.read(header);
        }
        catch(IOException e){
            System.out.printf("Nie ma pliku, kartoflu");
        }
        for(byte i=0;i<4;i++){
            if(header[i]!=HEADER[i]){
                System.out.printf("Smuta, to jednak nie rom z nesa");
                //to do
            }
        }
    }
}
