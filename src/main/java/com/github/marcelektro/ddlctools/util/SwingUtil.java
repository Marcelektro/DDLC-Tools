package com.github.marcelektro.ddlctools.util;

import javax.swing.*;
import java.awt.*;

public class SwingUtil {

    public static void displayImage(Image image, String title) {
        var frame = new JFrame("DDLC Tools - " + title);

        var prePanel = new JPanel();

        var button = new JButton("Reveal image");
        button.addActionListener(e -> {
            frame.remove(prePanel);
            frame.add(new JLabel(new ImageIcon(image)));
            frame.pack();
        });
        prePanel.add(button);
        prePanel.add(new JLabel(": " + title));

        frame.add(prePanel);

        // set the size of the frame to the size of the image
        frame.setPreferredSize(new Dimension(image.getWidth(null), image.getHeight(null)));

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // this will make all windows disappear when you close one of them
        frame.setVisible(true);
    }

}
