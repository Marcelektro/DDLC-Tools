package com.github.marcelektro.ddlctools;

import com.github.marcelektro.ddlctools.util.SwingUtil;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class NatsukiChrDecoder {

    public static void main(String[] args) throws Exception {

        var natsuki = ImageIO.read(new File("natsuki.chr")); // jpg

        natsuki = makeArgb(natsuki);

        natsuki = applyColorInvert(natsuki);
        natsuki = applyRectToPolar(natsuki);

        SwingUtil.displayImage(natsuki, "Natsuki final result");

        ImageIO.write(natsuki, "png", new File("natsuki.chr.decoded.png"));

    }


    public static BufferedImage applyColorInvert(BufferedImage inputImage) {
        var width = inputImage.getWidth();
        var height = inputImage.getHeight();
        var outputImage = new BufferedImage(width, height, inputImage.getType());

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                var pixel = inputImage.getRGB(x, y);
                outputImage.setRGB(x, y, pixel ^ 0x00FFFFFF);
            }
        }

        return outputImage;
    }

    public static BufferedImage applyRectToPolar(BufferedImage inputImage) {
        var width = inputImage.getWidth();
        var height = inputImage.getHeight();
        var outputImage = new BufferedImage(width, height, inputImage.getType());

        var centerX = width / 2;
        var centerY = height / 2;

        var scale = 110d;

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {

                var adjustedDx = (double) x - centerX;
                adjustedDx *= centerY / scale;

                var adjustedDy = (double) centerY - y;
                adjustedDy *= centerX / scale;


                var radius = Math.sqrt(adjustedDx * adjustedDx + adjustedDy * adjustedDy);

                var polarX = getPolarX(adjustedDx, adjustedDy, centerX);

                if (polarX == outputImage.getWidth()) {
                    polarX--;
                }

                var polarY = (int) Math.ceil(radius);

                if (polarX >= 0 && polarX < width &&
                    polarY >= 0 && polarY < height) {

                    outputImage.setRGB(x, y, inputImage.getRGB(polarX, polarY));
                }

            }
        }

        return outputImage;
    }


    private static int getPolarX(double dx, double dy, int centerX) {
        double angle;

        if (dx == 0.0) {
            angle = Math.PI / 2;
            if (dy < 0.0) {
                angle += Math.PI;
            }
        } else {
            angle = Math.atan(dy / dx);
        }

        if (dx < 0.0) {
            angle += Math.PI;
        }

        if (dx > 0.0 && dy < 0.0) {
            angle += Math.PI * 2;
        }

        angle -= Math.PI / 2;
        if (angle < 0.0) {
            angle += Math.PI * 2;
        }

        angle = angle * centerX / Math.PI;

        return (int) Math.ceil(angle);
    }


    public static BufferedImage makeArgb(BufferedImage inputImage) {

        var outputImage = new BufferedImage(
                inputImage.getWidth(), inputImage.getHeight(),
                BufferedImage.TYPE_INT_ARGB
        );

        outputImage.getGraphics().drawImage(inputImage, 0, 0, null);

        return outputImage;
    }


}
