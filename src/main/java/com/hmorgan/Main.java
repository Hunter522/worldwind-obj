package com.hmorgan;

import com.hmorgan.gfx.wavefront.ObjModel;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import com.hmorgan.worldwind.Cube;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;

import javax.swing.*;
import java.awt.*;
import java.io.IOException;

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
        Position position = Position.fromDegrees(35.0, -120.0, 3000);
//        Cube cube = new Cube(position, 1000);
//        layer.addRenderable(cube);

        try {
            ObjModel suzanne = new ObjModel("monkey.obj");
            suzanne.setPosition(position);
            suzanne.setSize(1000);
            layer.addRenderable(suzanne);
        } catch(IOException e) {
            e.printStackTrace();
        }

        ww.getModel().getLayers().add(layer);
//        ww.getModel().getLayers().getLayerByName("Atmosphere").setEnabled(false);

        frame.add(ww, BorderLayout.CENTER);

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
