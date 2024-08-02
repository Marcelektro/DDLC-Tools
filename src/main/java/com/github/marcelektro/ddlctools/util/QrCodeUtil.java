package com.github.marcelektro.ddlctools.util;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import java.awt.image.BufferedImage;
import java.util.Map;

public class QrCodeUtil {

    public static String parseQRCode(BufferedImage img, Map<DecodeHintType, ?> hintMap) throws Exception {

        var binaryBitmap = new BinaryBitmap(new HybridBinarizer(new BufferedImageLuminanceSource(img)));

        var result = new MultiFormatReader().decode(binaryBitmap, hintMap);

        return result.getText();
    }



    /*public static void main(String[] args) throws Exception {

        var qrCode1 = ImageIO.read(new FileInputStream("qr-code2.png"));
        var qrCode2 = ImageIO.read(new FileInputStream("asdasdasd.png"));


        var hints = new HashMap<DecodeHintType, Object>();
        hints.put(DecodeHintType.TRY_HARDER, Boolean.TRUE);
        hints.put(DecodeHintType.ALSO_INVERTED, Boolean.TRUE);
        hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");

        System.out.println(parseQRCode(qrCode1, hints));
        System.out.println(parseQRCode(qrCode2, hints));

    }*/

}
