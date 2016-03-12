package com.hmorgan.gfx.wavefront;

import java.util.Optional;

/**
 * Represents an element index in a Wavefront .OBJ file. An .OBJ element can be:
 * <ul>
 *     <li>'f' - a face</li>
 *     Faces have a vertex index, a normal index (optional), and a texture coord index (optional)
 *     <li>'l' - a line</li>
 *     Lines have a vertex index, and an optional texture coord index
 * </ul>
 *
 * <p>
 * This class is <i>immutable</i> and uses a builder class. This makes this class
 * inheritly thread-safe.
 *
 * @author Hunter N. Morgan
 */
public class ObjIndex {
    private Integer vertexIndex;
    private Integer normalIndex;
    private Integer textureCoordIndex;

    private boolean hasNormalIndex;
    private boolean hasTextureCoordIndex;


    public static class Builder {
        private ObjIndex objIndex;

        public Builder() {
            objIndex = new ObjIndex();
        }

        public Builder setVertexIndex(int i) {
            objIndex.vertexIndex = i;
            return this;
        }

        public Builder setNormalIndex(int i) {
            objIndex.normalIndex = i;
            objIndex.hasNormalIndex = true;
            return this;
        }

        public Builder setTextureCoordIndex(int i) {
            objIndex.textureCoordIndex = i;
            objIndex.hasTextureCoordIndex = true;
            return this;
        }

        public ObjIndex build() {
            return this.objIndex;
        }
    }

    private ObjIndex() {
        vertexIndex = null;
        normalIndex = null;
        textureCoordIndex = null;
        hasNormalIndex = false;
        hasTextureCoordIndex = false;
    }

    public Integer getVertexIndex() {
        return vertexIndex;
    }

    public Optional<Integer> getNormalIndex() {
        return Optional.ofNullable(normalIndex);
    }

    public Optional<Integer> getTextureCoordIndex() {
        return Optional.ofNullable(textureCoordIndex);
    }

    public boolean hasNormalIndex() {
        return hasNormalIndex;
    }

    public boolean hasTextureCoordIndex() {
        return hasTextureCoordIndex;
    }
}
