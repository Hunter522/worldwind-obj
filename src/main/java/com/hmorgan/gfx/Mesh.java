package com.hmorgan.gfx;

import com.hackoeur.jglm.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a generic 3D mesh. A mesh can be a polygonal, polyline,
 * or a point-based mesh.
 * <p>
 * This class is <i>immutable</i> and uses a builder class. This makes this class
 * inheritly thread-safe.
 *
 * @author Hunter N. Morgan
 */
public class Mesh {

    private String name;
    protected List<Vec3> vertices;
    protected List<Vec3> normals;
    protected List<Vec3> textureCoords;
    protected List<Integer> indices;  // v1/v2/v3 or v1/n1/v2/n2 or /v1/t1/n1/v2/t2/n2

    // vbo
    // ibo
    // vao ?

    public enum MeshType {
        POINTS_MESH,            // mesh contains just points
        POLYLINE_MESH,          // mesh is a polygon (triangle mesh)
        POLYGON_MESH            // mesh is a polyline (line segments)
    }

    protected MeshType meshType;

    protected boolean hasNormalIndex;
    protected boolean hasTextureCoordIndex;

    /**
     * Builder class for Mesh
     */
    public static class Builder {
        private Mesh mesh;

        public Builder() {
            mesh = new Mesh();
        }

        public Builder setName(String name) {
            mesh.name = name;
            return this;
        }

        public Builder setVertices(List<Vec3> verts) {
            mesh.vertices = verts;
            return this;
        }

        public Builder setNormals(List<Vec3> normals) {
            mesh.normals = normals;
            mesh.hasNormalIndex = true;
            return this;
        }

        public Builder setTextureCoords(List<Vec3> texCoords) {
            mesh.textureCoords = texCoords;
            mesh.hasTextureCoordIndex = true;
            return this;
        }

        public Builder setIndices(List<Integer> indices) {
            mesh.indices = indices;
            return this;
        }

        public Builder setMeshType(MeshType meshType) {
            mesh.meshType = meshType;
            return this;
        }

        public Mesh build() {
            return mesh;
        }
    }

    private Mesh() {
        vertices = new ArrayList<>();
        normals = new ArrayList<>();
        textureCoords = new ArrayList<>();
        meshType = MeshType.POLYGON_MESH;   // most common
        hasNormalIndex = false;
        hasTextureCoordIndex = false;
    }


    public String getName() {
        return name;
    }

    public List<Vec3> getVertices() {
        return vertices;
    }

    public List<Vec3> getNormals() {
        return normals;
    }

    public List<Vec3> getTextureCoords() {
        return textureCoords;
    }

    public List<Integer> getIndices() {
        return indices;
    }

    public MeshType getMeshType() {
        return meshType;
    }

    public boolean hasNormalIndex() {
        return hasNormalIndex;
    }

    public boolean hasTextureCoordIndex() {
        return hasTextureCoordIndex;
    }
}
