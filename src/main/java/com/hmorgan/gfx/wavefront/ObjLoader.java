package com.hmorgan.gfx.wavefront;

import com.hackoeur.jglm.Vec3;
import com.hmorgan.gfx.Mesh;
import com.hmorgan.gfx.Vertex;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * @author Hunter N. Morgan
 */
public class ObjLoader {

    /**
     * Convenience method to find and return the Path of a filename from this project's
     * resources folder.
     *
     * @param filename filename to get a Path to
     * @return Path to filename
     * @throws IOException if file does not exist
     */
    public static Path getFilePathFromResources(String filename) throws IOException {
        URI uri;
        try {
            Optional<URL> url = Optional.ofNullable(ObjModel.class.getClassLoader().getResource(filename));
            if(url.isPresent()) {
                uri = url.get().toURI();
                return Paths.get(uri);
            }
            else
                throw new IOException(filename + " does not exist");
        } catch (URISyntaxException e) {
            throw new IOException(filename + " does not exist");
        }
    }

    /**
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public static ObjModel loadObjModel(Path filePath) throws IOException {
        return new ObjModel(loadObjMeshes(filePath));
    }

    /**
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public static Map<String, Mesh> loadObjMeshes(String fileName) throws IOException {
        return loadObjMeshes(getFilePathFromResources(fileName));
    }

    /**
     * Parses the given .OBJ file and attempts to extract the useful Mesh data
     * from it, populating this class's meshes map.
     *
     * @param filePath Path to .OBJ file
     * @throws IOException
     */
    public static Map<String, Mesh> loadObjMeshes(Path filePath) throws IOException {
        final Map<String, Mesh> meshMap = new HashMap<>();
        boolean builtFirstMesh = false;
        Mesh.Builder meshBuilder = new Mesh.Builder();
        List<Vec3> vertices = new ArrayList<>();
        List<Vec3> textureCoords = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        List<ObjIndex> indices = new ArrayList<>();
        int currVertexCount = 0;
        int currNormalCount = 0;
        int currTexCoordCount = 0;

        System.out.println("Parsing " + filePath.getFileName() + "...");
        // open file
        String line;
        BufferedReader bufferedReader = Files.newBufferedReader(filePath);
        while ((line = bufferedReader.readLine()) != null) {
            // start going through the file, reading line by line
            // if we read an object (o) then we need to make a new mesh
            // make a new list of verts, normals, texcoords, and indices
            // create a mesh.builder and set those lists
            // add the newly created mesh to the map

            if (line.charAt(0) != '#') {  // ignore comments
                final String[] tokens = line.split("\\s+");
                if (tokens.length > 0) {
                    final String firstToken = tokens[0];

                    switch (firstToken) {
                        case "o":
                            final String objectName = tokens[1];

                            // start of new object, create mesh and add it to map
                            if (builtFirstMesh) {
                                final Mesh mesh = buildMesh(meshBuilder,
                                        vertices,
                                        textureCoords,
                                        normals,
                                        indices,
                                        currVertexCount,
                                        currNormalCount,
                                        currTexCoordCount);
                                meshMap.put(mesh.getName(), mesh);
                                currVertexCount += vertices.size();
                                currNormalCount += normals.size();
                                currTexCoordCount += textureCoords.size();
                            } else {
                                builtFirstMesh = true;
                            }
                            // create a new mesh builder & reset working lists
                            meshBuilder = new Mesh.Builder();
                            meshBuilder.setName(objectName);
                            vertices = new ArrayList<>();
                            textureCoords = new ArrayList<>();
                            normals = new ArrayList<>();
                            indices = new ArrayList<>();

                            break;
                        case "v":
                            vertices.add(new Vec3(Float.parseFloat(tokens[1]),
                                    Float.parseFloat(tokens[2]),
                                    Float.parseFloat(tokens[3])));
                            break;
                        case "vn":
                            normals.add(new Vec3(Float.parseFloat(tokens[1]),
                                    Float.parseFloat(tokens[2]),
                                    Float.parseFloat(tokens[3])));
                            break;
                        case "vt":
                            // u, v, w (optional)
                            Vec3 texCoord = null;
                            if (tokens.length == 3) {
                                texCoord = new Vec3(Float.parseFloat(tokens[1]),
                                        Float.parseFloat(tokens[2]),
                                        0.0f);
                            } else {
                                new Vec3(Float.parseFloat(tokens[1]),
                                        Float.parseFloat(tokens[2]),
                                        Float.parseFloat(tokens[3]));
                            }
                            textureCoords.add(texCoord);
                            break;
                        case "f":
                            // split each token with '/'
                            // f vi/ti/ni vi/ti/ni vi/ti/ni
                            // or
                            // f vi vi vi
                            meshBuilder.setMeshType(Mesh.MeshType.POLYGON_MESH);

                            if (line.contains("/")) {
                                for (int i = 1; i < tokens.length; i++) {
                                    final String faceToken = tokens[i];
                                    final String[] faceTokens = faceToken.split("/");
                                    ObjIndex.Builder objIndexBuilder = new ObjIndex.Builder();
                                    objIndexBuilder.setVertexIndex(Integer.valueOf(faceTokens[0]) - 1);
                                    if (!faceTokens[1].isEmpty())
                                        objIndexBuilder.setTextureCoordIndex(Integer.valueOf(faceTokens[1]) - 1);
                                    if (!faceTokens[2].isEmpty())
                                        objIndexBuilder.setNormalIndex(Integer.valueOf(faceTokens[2]) - 1);
                                    indices.add(objIndexBuilder.build());
                                }
                            } else {
                                // uses spaces for each face vertex
                                for (int i = 1; i < tokens.length; i++) {
                                    ObjIndex.Builder objIndexBuilder = new ObjIndex.Builder();
                                    objIndexBuilder.setVertexIndex(Integer.valueOf(tokens[i]) - 1);
                                    indices.add(objIndexBuilder.build());
                                }
                            }

                            break;
                        case "l":
                            // dont split, keep original tokens

                            // l vi vi
                            // or
                            // l vi/ti vi/ti

                            meshBuilder.setMeshType(Mesh.MeshType.POLYLINE_MESH);

                            // just considering first case for now...
                            for (int i = 1; i < tokens.length; i++) {
                                ObjIndex.Builder objIndexBuilder = new ObjIndex.Builder();
                                objIndexBuilder.setVertexIndex(Integer.valueOf(tokens[i]) - 1);
                                indices.add(objIndexBuilder.build());
                            }
                            break;
                    }
                }
            } // end else (not a comment)
        } // end while read line

        // reached end of file
        final Mesh mesh = buildMesh(meshBuilder, vertices, textureCoords, normals, indices, currVertexCount, currNormalCount, currTexCoordCount);
        meshMap.put(mesh.getName(), mesh);
        System.out.println("Finished parsing " + filePath.getFileName());
        return meshMap;
    }

    /**
     * Builds a new Mesh object and stores it in meshes map.
     *
     * @param meshBuilder   mesh builder to store rest of arguments into
     * @param vertices      vertices list
     * @param textureCoords texture coordinates list
     * @param normals       normals list
     * @param indices       indices list
     * @return new Mesh object
     */
    private static Mesh buildMesh(Mesh.Builder meshBuilder,
                                  List<Vec3> vertices,
                                  List<Vec3> textureCoords,
                                  List<Vec3> normals,
                                  List<ObjIndex> indices,
                                  int currVertexCount,
                                  int currNormalCount,
                                  int currTexCoordCount) {

        // build vertex list and index buffer
        final List<Vertex> vertexList = new ArrayList<>();

        for(ObjIndex index : indices) {
            Vertex.Builder vertexBuilder = new Vertex.Builder(vertices.get(index.getVertexIndex()-currVertexCount));
            index.getNormalIndex()
                 .ifPresent(ni -> vertexBuilder.setNormal(normals.get(ni-currNormalCount)));
            index.getTextureCoordIndex()
                 .ifPresent(ti -> vertexBuilder.setTexCoord(textureCoords.get(ti-currTexCoordCount)));
            vertexList.add(vertexBuilder.build());
//            indicesBuf.put(index.getVertexIndex());
        }

        final IntBuffer indicesBuf = IntBuffer.allocate(vertexList.size());
        for(int i = 0; i < vertexList.size(); i++) {
            indicesBuf.put(i);
        }
        indicesBuf.flip();
        meshBuilder.setVertices(vertexList);
        meshBuilder.setIndices(indicesBuf);


//        // add all of the working lists into the meshBuilder
//        final FloatBuffer verticesBuf = FloatBuffer.allocate(vertices.size() * 3);
//        vertices.forEach(vec3 -> {
//            verticesBuf.put(vec3.getX());
//            verticesBuf.put(vec3.getY());
//            verticesBuf.put(vec3.getZ());
//        });
//        verticesBuf.flip();
//        meshBuilder.setVertices(verticesBuf);
//
//        if(!textureCoords.isEmpty()) {
//            final FloatBuffer texelBuf = FloatBuffer.allocate(textureCoords.size() * 3);
//            textureCoords.forEach(vec3 -> {
//                texelBuf.put(vec3.getX());
//                texelBuf.put(vec3.getY());
//                texelBuf.put(vec3.getZ());
//            });
//            texelBuf.flip();
//            meshBuilder.setTextureCoords(texelBuf);
//        }
//
//
//        // ok need to post-process the indices
//        // need to match every vertex with its corresponding normal!
//        if(!indices.isEmpty()) {
//            final ObjIndex first = indices.get(0);
//            int indicesBufSize = indices.size();
////            if(first.hasNormalIndex())
////                indicesBufSize += indices.size();
////            if(first.hasTextureCoordIndex())
////                indicesBufSize += indices.size();
//            final IntBuffer indicesBuf = IntBuffer.allocate(indicesBufSize);
//            for(ObjIndex idx : indices) {
//                indicesBuf.put(idx.getVertexIndex());
////                if(idx.hasTextureCoordIndex())
////                    indicesBuf.put(idx.getTextureCoordIndex().get());
////                if(idx.hasNormalIndex())
////                    indicesBuf.put(idx.getNormalIndex().get());
//            }
//            indicesBuf.flip();
//            meshBuilder.setIndices(indicesBuf);
//
//            // iterated through normal indices, get normal, and add it to buffer
//            // (yes this will make duplicate normals but performance hit is prolly negligable)
//            if(!normals.isEmpty()) {
//                final FloatBuffer normalsBuf = FloatBuffer.allocate(vertices.size() * 3);
//                for(ObjIndex idx : indices) {
//                    final Vec3 normalVec3 = normals.get(idx.getNormalIndex().get());
//                    normalsBuf.put(normalVec3.getX());
//                    normalsBuf.put(normalVec3.getY());
//                    normalsBuf.put(normalVec3.getZ());
//                }
////            normals.forEach(vec3 -> {
////                normalsBuf.put(vec3.getX());
////                normalsBuf.put(vec3.getY());
////                normalsBuf.put(vec3.getZ());
////            });
////            normalsBuf.flip();
//                meshBuilder.setNormals(normalsBuf);
//            }
//        }



        return meshBuilder.build();
    }
}
