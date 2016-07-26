package com.hmorgan.gfx.wavefront;

import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.util.Logging;

import java.awt.*;
import java.nio.file.Path;

/**
 * A {@link Material} that has some extra properties that a Wavefront MTL file has.
 *
 * @author Hunter N. Morgan
 */
public class WavefrontMaterial extends Material {

    private Path diffuseTextureMapPath;

    public static final WavefrontMaterial WHITE;
    public static final WavefrontMaterial LIGHT_GRAY;
    public static final WavefrontMaterial GRAY;
    public static final WavefrontMaterial DARK_GRAY;
    public static final WavefrontMaterial BLACK;
    public static final WavefrontMaterial RED;
    public static final WavefrontMaterial PINK;
    public static final WavefrontMaterial ORANGE;
    public static final WavefrontMaterial YELLOW;
    public static final WavefrontMaterial GREEN;
    public static final WavefrontMaterial MAGENTA;
    public static final WavefrontMaterial CYAN;
    public static final WavefrontMaterial BLUE;

    static {
        WHITE = new WavefrontMaterial(Color.WHITE);
        LIGHT_GRAY = new WavefrontMaterial(Color.LIGHT_GRAY);
        GRAY = new WavefrontMaterial(Color.GRAY);
        DARK_GRAY = new WavefrontMaterial(Color.DARK_GRAY);
        BLACK = new WavefrontMaterial(Color.BLACK);
        RED = new WavefrontMaterial(Color.RED);
        PINK = new WavefrontMaterial(Color.PINK);
        ORANGE = new WavefrontMaterial(Color.ORANGE);
        YELLOW = new WavefrontMaterial(Color.YELLOW);
        GREEN = new WavefrontMaterial(Color.GREEN);
        MAGENTA = new WavefrontMaterial(Color.MAGENTA);
        CYAN = new WavefrontMaterial(Color.CYAN);
        BLUE = new WavefrontMaterial(Color.BLUE);
    }

    public WavefrontMaterial(Color specular,
                             Color diffuse,
                             Color ambient,
                             Color emission,
                             float shininess,
                             Path diffuseTextureMapPath) {
        super(specular, diffuse, makeDarker2(ambient), emission, shininess);
        this.diffuseTextureMapPath = diffuseTextureMapPath;
    }

    protected static Color makeDarker2(Color var1) {
        if(var1 == null) {
            String var7 = Logging.getMessage("nullValue.ColorIsNull");
            Logging.logger().severe(var7);
            throw new IllegalArgumentException(var7);
        } else {
            float var2 = 0.3F;
            int var3 = var1.getRed();
            int var4 = var1.getGreen();
            int var5 = var1.getBlue();
            int var6 = var1.getAlpha();
            return new Color(Math.max(0, (int)((float)var3 * var2)), Math.max(0, (int)((float)var4 * var2)), Math.max(0, (int)((float)var5 * var2)), var6);
        }
    }

    public WavefrontMaterial(Color diffuse, float shininess) {
        super(diffuse, shininess);
    }

    public WavefrontMaterial(Color diffuse) {
        super(diffuse);
    }

    public Path getDiffuseTextureMapPath() {
        return diffuseTextureMapPath;
    }
}
