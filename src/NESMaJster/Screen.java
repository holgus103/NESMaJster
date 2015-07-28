package NESMaJster;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Created by Lukasz on 2015-07-22.
 */
public class Screen extends Canvas {
    private BufferedImage img;

    public Screen() {
        this.setSize(256,240);
    }

    @Override
    public void paint(Graphics g) {
        super.paint(g);
        if(img != null)
            g.drawImage(img,0,0,null);
    }

    public void setImg(BufferedImage img) {
        this.img = img;
    }
}
