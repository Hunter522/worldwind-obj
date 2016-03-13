package com.hmorgan.gfx;

import com.hackoeur.jglm.Vec3;

import java.util.Optional;

/**
 * @author Hunter N. Morgan
 */
public class Vertex {
    private Vec3 position;
    private Vec3 normal;
    private Vec3 texCoord;

    public static final class Builder {
        private Vec3 position;
        private Vec3 normal;
        private Vec3 texCoord;

        public Builder(Vec3 pos) {
            this.position = pos;
        }

        public Builder setNormal(Vec3 val) {
            normal = val;
            return this;
        }

        public Builder setTexCoord(Vec3 val) {
            texCoord = val;
            return this;
        }

        public Vertex build() {
            return new Vertex(this);
        }
    }

    private Vertex(Builder builder) {
        position = builder.position;
        normal = builder.normal;
        texCoord = builder.texCoord;
    }


    public Vec3 getPosition() {
        return position;
    }

    public Optional<Vec3> getNormal() {
        return Optional.ofNullable(normal);
    }

    public Optional<Vec3> getTexCoord() {
        return Optional.ofNullable(texCoord);
    }


}
