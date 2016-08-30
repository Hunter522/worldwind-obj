package com.hmorgan;

import com.hmorgan.gfx.wavefront.ObjModel;
import gov.nasa.worldwind.BasicModel;
import gov.nasa.worldwind.Configuration;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.Material;

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
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 15100);
        Configuration.setValue(AVKey.INITIAL_PITCH, 45);
        Configuration.setValue(AVKey.INITIAL_HEADING, 45);


//        final GLCapabilities glCaps = new GLCapabilities(GLProfile.getDefault());
//        glCaps.setSampleBuffers(true);
//        glCaps.setNumSamples(2);

        ww = new WorldWindowGLCanvas();
        ww.setModel(new BasicModel());

        RenderableLayer layer = new RenderableLayer();
        Position position = Position.fromDegrees(35.0, -120.0, 3000);
//        Cube cube = new Cube(position, 1000);
//        layer.addRenderable(cube);


        try {
            Position pos = Position.fromDegrees(35.0, -120.0, 3000);
            ObjModel objModel = new ObjModel("crate.obj");
            objModel.setPosition(pos);

            final float scale = 500;
            objModel.setScale(scale);
//            objModel.setMaterial(Material.RED);
            objModel.setPitch(45.0);
            objModel.setRoll(90.0);
            objModel.setYaw(90.0);
//            objModel.setOpacity(0.5f);
            layer.addRenderable(objModel);

//            for(int i = 0; i < 100; i++) {
//                objModel = new ObjModel(objModel);  // copy instead of reloading from file
//                objModel.setPosition(pos.add(Position.fromDegrees(0.0, i / 100.0)));
//                objModel.setScale(scale);
//                layer.addRenderable(objModel);
//            }


//            new Thread(() -> {
//                Position pos1 = Position.fromDegrees(35.0, -120.0, 3000);
//                try {
//                    Thread.sleep(3000);
//                } catch(InterruptedException e) {
//                    e.printStackTrace();
//                }
//                while(true) {
//                    pos1 = pos1.add(Position.fromDegrees(0.0, 0.0001));
//                    objModel.setPosition(pos1);
//                    try {
//                        Thread.sleep(500);
//                    } catch(InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                    ww.redraw();
//                }
//            }).start();

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
