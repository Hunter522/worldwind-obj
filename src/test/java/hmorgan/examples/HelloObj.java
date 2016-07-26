package hmorgan.examples;

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
 * Simple "Hello World" example demonstrating how to create and add an ObjModel to
 * NASA WorldWind.
 *
 * @author Hunter N. Morgan
 */
public class HelloObj {

    private static WorldWindowGLCanvas ww;

    public static void main(String[] args) {
        JFrame frame = new JFrame("HelloObj");
        frame.setLayout(new BorderLayout());
        frame.setSize(new Dimension(1024, 768));

        // use NASA WW's Configuration class to set the camera's orientation & position
        Configuration.setValue(AVKey.INITIAL_LATITUDE, 35.0);
        Configuration.setValue(AVKey.INITIAL_LONGITUDE, -120.0);
        Configuration.setValue(AVKey.INITIAL_ALTITUDE, 15100);
        Configuration.setValue(AVKey.INITIAL_PITCH, 45);
        Configuration.setValue(AVKey.INITIAL_HEADING, 45);

        ww = new WorldWindowGLCanvas();
        ww.setModel(new BasicModel());

        // create a RenderableLayer, which we will add our ObjModel renderable to
        RenderableLayer layer = new RenderableLayer();

        // create an ObjModel from a obj file
        ObjModel objModel = null;
        try {
            objModel = new ObjModel("v22/v22.obj");
//            objModel.setTextureDisabled(true);
//            objModel.setMaterial(Material.RED);
//            objModel.setOpacity(0.2f);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // set the ObjModel's position and orientation
        // this can be changed at any time (e.g. setting position over time)
        objModel.setPosition(Position.fromDegrees(35.0, -120.0, 3000));
        objModel.setScale(200);
        objModel.setPitch(10.0);
        objModel.setRoll(1.0);
        objModel.setYaw(90.0);

        // add the ObjModel to the layer
        layer.addRenderable(objModel);

        // and add the layer to the WW layers
        ww.getModel().getLayers().add(layer);

        frame.add(ww, BorderLayout.CENTER);
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
