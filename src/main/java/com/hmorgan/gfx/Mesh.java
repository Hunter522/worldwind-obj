package com.hmorgan.gfx;

import com.hackoeur.jglm.Vec3;
import com.hmorgan.gfx.wavefront.WavefrontMaterial;
import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.LazilyLoadedTexture;
import gov.nasa.worldwind.render.WWTexture;

import javax.imageio.ImageIO;
import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Path;
import java.util.*;

// TODO: Implement textures/materials
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

    protected  List<Vertex> vertices;
//    protected FloatBuffer vertices;     // v0x/v0y/v0z/v1x/v1y/v1z...
//    protected FloatBuffer normals;      // n0x/n0y/n0z/n1x/n1y/n1z...
//    protected FloatBuffer textureCoords;// t0x/v0y/t1x/t1y...
    protected IntBuffer indices;        // v1/v2/v3 or v1/n1/v2/n2 or /v1/t1/n1/v2/t2/n2
    protected FloatBuffer vboBuf;       // vvvvnnnn (if no normals, then just vvvv)
    private WavefrontMaterial material;
    private WWTexture texture;

    private int[] vboIds;               // vertex buffer object ids
    private int[] eboIds;               // element buffer object ids
    private boolean generatedGlBuffers;
    private boolean hasValidDiffuseTextureMap; // true if material diffuse texture map exists

    public enum MeshType {
        POINTS_MESH,            // mesh contains just points
        POLYLINE_MESH,          // mesh is a polygon (triangle mesh)
        POLYGON_MESH            // mesh is a polyline (line segments)
    }

    protected MeshType meshType;

    // VBO Cache. Whenever a VBO is to be loaded, it first checks here to see if
    // the mesh already has a VBO. If so, then it will use the existing VBO. The key
    // is the mesh's name, so take care into keeping unique names for each mesh!
    private static Map<String, int[]> vboCache = new HashMap<>();
    private static Map<String, int[]> eboCache = new HashMap<>();
    private static Map<String, WWTexture> textureCache = new HashMap<>();


    public static final class Builder {
        private String name;
        private List<Vertex> vertices;
        private IntBuffer indices;
        private MeshType meshType;
        private WavefrontMaterial material;

        public Builder() {
            vertices = new ArrayList<>();
            meshType = MeshType.POLYGON_MESH;   // most common
            material = WavefrontMaterial.GRAY;
        }

        public Builder setName(String val) {
            name = val;
            return this;
        }

        public Builder setVertices(List<Vertex> val) {
            vertices = val;
            return this;
        }

        public Builder setIndices(IntBuffer val) {
            indices = val;
            return this;
        }

        public Builder setMeshType(MeshType val) {
            meshType = val;
            return this;
        }

        public Builder setMaterial(WavefrontMaterial val) {
            material = val;
            return this;
        }

        public Mesh build() {
            return new Mesh(this);
        }
    }

    private Mesh(Builder builder) {
        name = builder.name;
        vertices = builder.vertices;
        indices = builder.indices;
        meshType = builder.meshType;
        material = builder.material;
        vboIds = new int[1];
        eboIds = new int[1];
        generatedGlBuffers = false;

        if(material != null) {
            if(material.getDiffuseTextureMapPath() != null) {
                if(textureCache.containsKey(material.getDiffuseTextureMapPath().toString())) {
                    // texture in cache
                    texture = textureCache.get(material.getDiffuseTextureMapPath().toString());
                    hasValidDiffuseTextureMap = true;
                } else {
                    // texture not in cache, load it (note this does load it into GPU
                    // memory..since it's lazily loaded)
                    try {
                        texture = new LazilyLoadedTexture(ImageIO.read(material.getDiffuseTextureMapPath().toFile()), true);
                        textureCache.put(material.getDiffuseTextureMapPath().toString(), texture);
                        hasValidDiffuseTextureMap = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

//        // check to see if the material texture is valid:
//        if(material != null) {
//            if(material.getDiffuseTextureMapPath() != null) {
//                if(material.getDiffuseTextureMapPath().toFile().exists()) {
//                    hasValidDiffuseTextureMap = true;
//                } else {
//                    hasValidDiffuseTextureMap = false;
//                    System.err.println("WARNING: Mesh " + name + " diffuse texture map " + material.getDiffuseTextureMapPath() + " not a valid path");
//                }
//            }
//        }
//
//
//        if(hasValidDiffuseTextureMap) {
//            BufferedImage diffuseTextureMapBI = ImageIO.read(diffuseTextureMapPath.toFile();
//        }
//
//        // gen textures
//        if(hasValidDiffuseTextureMap) {
//            final Path diffuseTextureMapPath = material.getDiffuseTextureMapPath();
//            try {
//                if(textureCache.containsKey(diffuseTextureMapPath.toString()))
//                    texture = textureCache.get(diffuseTextureMapPath.toString());
//                else {
//                    texture = new LazilyLoadedTexture(diffuseTextureMapBI, true);
//                    textureCache.put(diffuseTextureMapPath.toString(), texture);
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//        }
    }

    public void genGlBuffers(DrawContext dc) {
        if(vboCache.containsKey(this.name)){
            this.generatedGlBuffers = true;
        }

        final GL2 gl = dc.getGL().getGL2();



        // create VBO, layout is:
        //  - vertices, normals, and texels: vvvnnntttvvvnnnttt...
        //  - vertices, normals:             vvvnnnvvvnnn...
        //  - vertices:                      vvvvvv...
        final int stride = (hasValidDiffuseTextureMap) ? 8 : 6;
        vboBuf = FloatBuffer.allocate(vertices.size() * stride);
        for(Vertex v : vertices) {
            vboBuf.put(v.getPosition().getX());
            vboBuf.put(v.getPosition().getY());
            vboBuf.put(v.getPosition().getZ());

            v.getNormal().ifPresent(n -> {
                vboBuf.put(n.getX());
                vboBuf.put(n.getY());
                vboBuf.put(n.getZ());
            });

            // if diffuse texture map file exists, then include the tex coords
            if(hasValidDiffuseTextureMap) {
                final BufferedImage textureBI = (BufferedImage) texture.getImageSource();
                if(v.getTexCoord().isPresent()) {
                    final Vec3 t = v.getTexCoord().get();
                    vboBuf.put(t.getX());

                    // for some reason the Y/V coordinate is flipped
                    vboBuf.put(textureBI.getHeight() - t.getY());
                }
            }
        }

        vboBuf.flip();

        // gen vbo
        if(!vboCache.containsKey(name)) {
            try {
                gl.glGenBuffers(1, vboIds, 0);                      // gen 1 buffer for VBO
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, vboIds[0]);     // bind buffer ID as VBO
                gl.glBufferData(GL.GL_ARRAY_BUFFER, vboBuf.limit() * Buffers.SIZEOF_FLOAT, vboBuf.rewind(), GL.GL_STATIC_DRAW);   // copy data to buffer
                generatedGlBuffers = true;
                vboCache.put(name, vboIds);
            } finally {
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);             // unbind buffer
            }
        }

        // gen ebo
        if(!eboCache.containsKey(name)) {
            getIndices().ifPresent(indices -> {
                try {
                    gl.glGenBuffers(1, eboIds, 0);                              // gen 1 buffer for EBO
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, eboIds[0]);     // bind buffer ID as EBO
                    gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, indices.limit() * Buffers.SIZEOF_INT, indices.rewind(), GL.GL_STATIC_DRAW);   // copy data to buffer
                    eboCache.put(name, eboIds);
                } finally {
                    gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);             // unbind buffer
                }
            });
        }
    }

    public String getName() {
        return name;
    }

    public List<Vertex> getVertices() {
        return vertices;
    }

    public Optional<IntBuffer> getIndices() {
        return Optional.ofNullable(indices);
    }

    public FloatBuffer getVboBuf() {
        return vboBuf;
    }

    public Optional<WavefrontMaterial> getMaterial() {
        return Optional.ofNullable(material);
    }

    public Optional<WWTexture> getTexture() {
        return Optional.ofNullable(texture);
    }

    public int[] getVboIds() {
        return vboCache.get(name);
    }

    public int[] getEboIds() {
        return eboCache.get(name);
    }

    public boolean isGeneratedGlBuffers() {
        return generatedGlBuffers;
    }

    public MeshType getMeshType() {
        return meshType;
    }

    public boolean isHasValidDiffuseTextureMap() {
        return hasValidDiffuseTextureMap;
    }

    @Override
    public boolean equals(Object o) {
        if(this == o) return true;
        if(o == null || getClass() != o.getClass()) return false;

        Mesh mesh = (Mesh) o;

        if(generatedGlBuffers != mesh.generatedGlBuffers) return false;
        if(!name.equals(mesh.name)) return false;
        if(!vertices.equals(mesh.vertices)) return false;
        if(indices != null ? !indices.equals(mesh.indices) : mesh.indices != null) return false;
        if(!vboBuf.equals(mesh.vboBuf)) return false;
        if(!Arrays.equals(vboIds, mesh.vboIds)) return false;
        if(!Arrays.equals(eboIds, mesh.eboIds)) return false;
        return meshType == mesh.meshType;

    }

    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + vertices.hashCode();
        result = 31 * result + (indices != null ? indices.hashCode() : 0);
        result = 31 * result + vboBuf.hashCode();
        result = 31 * result + Arrays.hashCode(vboIds);
        result = 31 * result + Arrays.hashCode(eboIds);
        result = 31 * result + (generatedGlBuffers ? 1 : 0);
        result = 31 * result + meshType.hashCode();
        return result;
    }
}
