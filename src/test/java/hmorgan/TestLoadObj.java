package hmorgan;

import com.hmorgan.gfx.wavefront.ObjModel;

import java.io.IOException;

/**
 * @author Hunter N. Morgan
 */
public class TestLoadObj {

    public static void main(String[] args) {
        // test loading an obj file

        ObjModel monkeyModel = null;
        try {
            monkeyModel = new ObjModel("monkey.obj");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        ObjModel testPathModel = null;
        try {
            testPathModel = new ObjModel("testpath.obj");
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        System.out.println("Done");
    }
}
