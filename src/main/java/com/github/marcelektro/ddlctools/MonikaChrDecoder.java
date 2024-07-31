package com.github.marcelektro.ddlctools;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class MonikaChrDecoder {

    public static void main(String[] args) throws Exception {

        // load the img
        var monika = ImageIO.read(new File("monika.chr"));


        var RGB_BLACK = new Color(0, 0, 0).getRGB();
        var RGB_WHITE = new Color(255, 255, 255).getRGB();

        // top left pixel: 330, 330
        // bottom right pixel: 469, 469
        // square wh: 140 (because inclusive)

        var wh = 469 - 330 + 1;

        // buffer
        int pos = 0;
        byte buff = 0;

        // result str builder
        var sb = new StringBuilder();


        for (int i = 0; i < wh * wh; i++) {
            var x = i % wh;
            var y = i / wh;

            var pixel = monika.getRGB(330 + x, 330 + y);

            if (pixel == RGB_WHITE) {
                buff |= (byte) (1 << (7-pos));
            }
            pos++;

            if (pos == 8) {

                if (buff != 0)
                    sb.append((char) buff);

                pos = 0;
                buff = 0;
            }

        }


        var b64d = Base64.getDecoder();

        var res = new String(b64d.decode(sb.toString()), StandardCharsets.UTF_8);

        System.out.println("---- RESULT ----");
        System.out.println(res);
        System.out.println("---- /RESULT ----");

        // write res to out file
        Files.writeString(new File("monika.chr.decoded.txt").toPath(), res);

    }

}
