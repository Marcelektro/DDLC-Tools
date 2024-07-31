package com.github.marcelektro.ddlctools;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Base64;

public class YuriChrDecoder {

    public static void main(String[] args) throws Exception {

        var contentsStr = Files.readString(new File("yuri.chr").toPath());


        var b64d = Base64.getDecoder();

        var res = new String(b64d.decode(contentsStr), StandardCharsets.UTF_8);

        System.out.println("---- RESULT ----");
        System.out.println(res);
        System.out.println("---- /RESULT ----");

        Files.writeString(new File("yuri.chr.decoded.txt").toPath(), res);

    }

}
