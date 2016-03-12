package com.hmorgan;

import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import com.hmorgan.worldwind.Cube;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;

import javax.swing.*;
import java.awt.*;

/**
 * @author Hunter N. Morgan
 */
public class Main {

    private static WorldWindowGLCanvas ww;

    public static void main(String[] args) {
        JFrame frame = new JFrame("worldwind-obj");
        frame.setLayout(new BorderLayout());
        frame.setSize(new Dimension(1024, 768));

        Configuration.setValue(AVKey.INITIAL_LATITUDE, 35.0);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, -120.0);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 15500);
        Configuration.setValue(AVKey.INITIAL_PITCH, 45);
        Configuration.setValue(AVKey.INITIAL_HEADING, 45);


        ww = new WorldWindowGLCanvas();
        ww.setModel(new BasicModel());

        RenderableLayer layer = new RenderableLayer();
        Cube cube = new Cube(Position.fromDegrees(35.0, -120.0, 3000), 1000);
        layer.addRenderable(cube);

        ww.getModel().getLayers().add(layer);

        frame.add(ww, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
