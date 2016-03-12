package com.hmorgan.gfx.wavefront;

import com.hackoeur.jglm.Vec3;
import com.hmorgan.gfx.Mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

/**
 * Represents a Wavefront .OBJ 3d model. This class directly references a .OBJ
 * file. Upon construction, the .OBJ file is parsed and the meshes/objects are
 * extracted.
 * <p>
 * This parsing only supports some features of the Wavefront .OBJ specification.
 * Those features are:
 * <ul>
 *     <li>'o' - objects</li>
 *     <li>'v' - vertices</li>
 *     <li>'vn' - vertex normals</li>
 *     <li>'vt' - texture coordinates</li>
 *     <li>'f' - faces</li>
 *     <li>'l' - lines</li>
 * </ul>
 * Each object in the .OBJ file are represented in this class as a {@link Mesh} object,
 * and are stored in a map keyed by their name.
 *
 * @author Hunter N. Morgan
 */
public class ObjModel {

    private Map<String, Mesh> meshes;       //  collection of Meshes

    /**
     * Constructs a new ObjModel.
     *
     * @param fileName String filename of .OBJ file
     * @throws IOException
     */
    public ObjModel(String fileName) throws IOException {
        meshes = new HashMap<>();

        final Path filePath = getFilePathFromResources(fileName);
        parseObj(filePath);
    }

    /**
     * Constructs a new ObjModel.
     *
     * @param filePath Path to .OBJ file
     * @throws IOException
     */
    public ObjModel(Path filePath) throws IOException {
        meshes = new HashMap<>();
        parseObj(filePath);
    }

    /**
     * Convinience method to find and return the Path of a filename from this project's
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
     * Parses the given .OBJ file and attempts to extract the useful Mesh data
     * from it, populating this class's meshes map.
     *
     * @param filePath Path to .OBJ file
     * @throws IOException
     */
    private void parseObj(Path filePath) throws IOException {
        boolean builtFirstMesh = false;
        Mesh.Builder meshBuilder = new Mesh.Builder();
        List<Vec3> vertices = new ArrayList<>();
        List<Vec3> textureCoords = new ArrayList<>();
        List<Vec3> normals = new ArrayList<>();
        List<ObjIndex> indices = new ArrayList<>();

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
                final String[] tokens = line.split(" ");
                if (tokens.length > 0) {
                    final String firstToken = tokens[0];

                    switch (firstToken) {
                        case "o":
                            final String objectName = tokens[1];

                            // start of new object, create mesh and add it to map
                            if (builtFirstMesh) {
                                buildMesh(meshBuilder, vertices, textureCoords, normals, indices);
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
        buildMesh(meshBuilder, vertices, textureCoords, normals, indices);
        System.out.println("Finished parsing " + filePath.getFileName());
    } // end parseObj

    /**
     * Builds a new Mesh object and stores it in meshes map.
     *
     * @param meshBuilder   mesh builder to store rest of arguments into
     * @param vertices      vertices list
     * @param textureCoords texture coordinates list
     * @param normals       normals list
     * @param indices       indices list
     */
    private void buildMesh(Mesh.Builder meshBuilder,
                           List<Vec3> vertices,
                           List<Vec3> textureCoords,
                           List<Vec3> normals,
                           List<ObjIndex> indices) {
        // add all of the working lists into the meshBuilder
        meshBuilder.setVertices(vertices);
        if(!textureCoords.isEmpty())
            meshBuilder.setTextureCoords(textureCoords);
        if(!normals.isEmpty())
            meshBuilder.setNormals(normals);

        List<Integer> meshIndices = new ArrayList<>();
        for(ObjIndex idx : indices) {
            meshIndices.add(idx.getVertexIndex());
            if(idx.hasTextureCoordIndex())
                meshIndices.add(idx.getTextureCoordIndex().get());
            if(idx.hasNormalIndex())
                meshIndices.add(idx.getNormalIndex().get());
        }
        meshBuilder.setIndices(meshIndices);
        Mesh mesh = meshBuilder.build();
        meshes.put(mesh.getName(), mesh);
    }

    public Map<String, Mesh> getMeshes() {
        return meshes;
    }
}
