package com.github.marcelektro.ddlctools;

import javax.sound.sampled.AudioSystem;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;

import com.github.marcelektro.ddlctools.util.QrCodeUtil;
import com.github.marcelektro.ddlctools.util.SwingUtil;
import com.google.zxing.DecodeHintType;
import org.jtransforms.fft.DoubleFFT_1D;

public class SayoriChrDecoder {

    // CONSTANTS

    private static final int WIDTH = 800;
    private static final int HEIGHT = 800;

    private static final int FFT_SIZE = 1024;
    private static final double THRESHOLD = 0.5; // threshold to filter out low volume artifacts
    private static double SPECTRUM_MAX_VALUE = 3.6199142; // for this specific use case, this is the value. It is updated on its own though.



    public static void main(String[] args) throws Exception {

        byte[] fileContents = Files.readAllBytes(new File("sayori.chr.wav").toPath()); // for now doesn't support OGG

        double[] audioData = convertToPCM(fileContents);

        double[][] spectrogramData = generateSpectrogramData(audioData);

        var generatedSpectrogramImage = generateSpectrogramImage(spectrogramData);


        // Stretch the image to the desired size

        var spectrogramImage = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        spectrogramImage.getGraphics().drawImage(generatedSpectrogramImage, 0, 0, WIDTH, HEIGHT, null);

//        SwingUtil.displayImage(spectrogramImage, "Spectrogram + stretched");
        //



        // Apply a non-linear transformation to the spectrogram image,
        // so the top part remains unchanged, but the bottom part is stretched.

        var spectrogramImageTransformed = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {

                var dy = y / (double) HEIGHT;

                var fa = Math.pow(dy, 2) + 1;

                int newY = (int) (fa * y * 1.1);

                if (newY >= HEIGHT)
                    newY = HEIGHT - 1;


                if (newY > 1)
                    spectrogramImageTransformed.setRGB(x, newY-1, spectrogramImage.getRGB(x, y));


                spectrogramImageTransformed.setRGB(x, newY, spectrogramImage.getRGB(x, y));

                if (newY+1 < HEIGHT)
                    spectrogramImageTransformed.setRGB(x, newY+1, spectrogramImage.getRGB(x, y));

            }
        }

//        SwingUtil.displayImage(spectrogramImageTransformed, "Spectrogram + stretched vertically");
        //




        // Create a blurred version of the image

        // to apply blur, we need to add edges to the image for the whole image to be blurred (simplest way)
        var offset = 40;

        var imgWithBorders = new BufferedImage(
                spectrogramImageTransformed.getWidth() + offset*2,
                spectrogramImageTransformed.getHeight() + offset*2,
                BufferedImage.TYPE_INT_ARGB
        );

        // initially fill with black
        {
            var g = imgWithBorders.createGraphics();
            g.setColor(Color.BLACK);
            g.fillRect(0, 0, imgWithBorders.getWidth(), imgWithBorders.getHeight());
            g.dispose();
        }

        // draw our image in the center
        imgWithBorders.getGraphics().drawImage(spectrogramImageTransformed, offset, offset, null);

//        SwingUtil.displayImage(imgWithBorders, "Transformed + Offset");


        // apply blur to the whole
        // -\/- taken from https://stackoverflow.com/q/29295929 -\/-

        int radius = 7;
        int size = radius * 2 + 1;
        float weight = 1.0f / (size * size);
        float[] data = new float[size * size];

        Arrays.fill(data, weight);

        var kernel = new Kernel(size, size, data);
        var op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);

        var spectrogramImageBlurred = op.filter(imgWithBorders, null);

//        SwingUtil.displayImage(spectrogramImageBlurred, "Spectrogram blurred");
        //



        // Create an image with the downscaled blurred image in the center

        var spectrogramBlurredSmol = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_ARGB);

        {
            var g = spectrogramBlurredSmol.createGraphics();

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, spectrogramBlurredSmol.getWidth(), spectrogramBlurredSmol.getHeight());

            g.dispose();
        }

        var downscaleFactor = 3;

        var downscaled = resizeImage(spectrogramImageBlurred,
                WIDTH / downscaleFactor,
                HEIGHT / downscaleFactor + 25 // +25 to slightly stretch it vertically
        );

        spectrogramBlurredSmol.getGraphics().drawImage(
                downscaled,
                WIDTH/2 - downscaled.getWidth()/2,
                HEIGHT/2 - downscaled.getHeight()/2,
                null
        );

//        SwingUtil.displayImage(spectrogramBlurredSmol, "Spectrogram blurred smol");
        //



        // Invert colors of the downscaled blurred image

        var spectrogramBlurredSmolInverted = applyInvertColors(spectrogramBlurredSmol);

        SwingUtil.displayImage(spectrogramBlurredSmolInverted, "Spectrogram Blurred Smol Inverted");
        //




        try {
            var hints = new HashMap<DecodeHintType, Object>();
            hints.put(DecodeHintType.TRY_HARDER, true);

            var result = QrCodeUtil.parseQRCode(spectrogramBlurredSmolInverted, hints);

            System.out.println("QR Code contents: " + result);

            Files.writeString(new File("sayori.chr.decoded.txt").toPath(), result);

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private static BufferedImage resizeImage(BufferedImage originalImage, int targetWidth, int targetHeight) {
        var res = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

        var scaled = originalImage.getScaledInstance(targetWidth, targetHeight, Image.SCALE_DEFAULT);

        res.getGraphics().drawImage(scaled, 0, 0, null);

        return res;
    }


    private static double[] convertToPCM(byte[] audioBytes) throws Exception {

        var ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(audioBytes));

        byte[] buffer = new byte[ais.available()];
        ais.read(buffer);
        ais.close();

        double[] audioData = new double[buffer.length / 2];
        for (int i = 0, j = 0; i < buffer.length; i += 2, j++) {
            audioData[j] = ((buffer[i + 1] << 8) | (buffer[i] & 0xff)) / 32768.0;
        }
        return audioData;
    }

    private static double[][] generateSpectrogramData(double[] audioData) {
        int numFrames = audioData.length / FFT_SIZE;
        double[][] spectrogramData = new double[numFrames][FFT_SIZE / 2];

        var fft = new DoubleFFT_1D(FFT_SIZE);
        double[] fftBuffer = new double[FFT_SIZE * 2];

        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;


        for (int i = 0; i < numFrames; i++) {
            System.arraycopy(audioData, i * FFT_SIZE, fftBuffer, 0, FFT_SIZE);

            fft.realForwardFull(fftBuffer);

            for (int j = 0; j < FFT_SIZE / 2; j++) {
                double re = fftBuffer[2 * j];
                double im = fftBuffer[2 * j + 1];

                var v = Math.log(Math.sqrt(re * re + im * im) + 1);

                if (v < min) min = v;
                if (v > max) max = v;

                spectrogramData[i][j] = v;
            }

        }

        System.out.println("min: " + min);
        System.out.println("max: " + max);

        SPECTRUM_MAX_VALUE = max;

        return spectrogramData;
    }


    private static BufferedImage generateSpectrogramImage(double[][] spectrogramData) {

        var spectrogramImage = new BufferedImage(spectrogramData.length, spectrogramData[0].length, BufferedImage.TYPE_INT_ARGB);


        for (int x = 0; x < spectrogramData.length; x++) {
            for (int y = 0; y < spectrogramData[0].length; y++) {
                double value = spectrogramData[x][y] / SPECTRUM_MAX_VALUE;

                if (value <= THRESHOLD) {
                    spectrogramImage.setRGB(x, spectrogramImage.getHeight() - 1 - y, Color.BLACK.getRGB());
                    continue;
                }

                int colorValue = (int) (Math.min(value, 1.0) * 255);
                //int color = new Color(colorValue, colorValue / 2, 0, 255).getRGB();
                int color = new Color(colorValue, colorValue, colorValue, 255).getRGB();
                spectrogramImage.setRGB(x, spectrogramImage.getHeight() - 1 - y, color);
            }
        }

        return spectrogramImage;
    }


    public static BufferedImage applyInvertColors(BufferedImage image) {

        var res = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);

        for (int x = 0; x < image.getWidth(); x++) {
            for (int y = 0; y < image.getHeight(); y++) {

                var color = new Color(image.getRGB(x, y));

                var newColor = new Color(255 - color.getRed(), 255 - color.getGreen(), 255 - color.getBlue(), color.getAlpha());
                res.setRGB(x, y, newColor.getRGB());
            }
        }

        return res;
    }




}
