package com.hmorgan.gfx.wavefront;

import com.hackoeur.jglm.Vec3;
import com.hmorgan.gfx.Mesh;
import com.hmorgan.gfx.Vertex;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.WWTexture;

import java.awt.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.IntBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.List;

/**
 * @author Hunter N. Morgan
 */
public class ObjLoader {

    private Map<String, Mesh> meshes;
    private ParserState state;
    private List<Vec3> vertices;
    private List<Vec3> textureCoords;
    private List<Vec3> normals;
    private List<ObjIndex> indices;
    private Map<String, WavefrontMaterial> materials;
    private List<WWTexture> textures;

    private enum ParserState {
        START,
        INIT,
        NEW_OBJECT,
        PROCESS_VNT,
        NEW_GROUP,
        PROCESS_VERTS,
        READ_EOF
    }

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

    public ObjLoader() {

    }

    /**f
     *
     * @param filePath
     * @return
     * @throws IOException
     */
    public ObjModel loadObjModel(Path filePath) throws IOException {
        return new ObjModel(loadObjMeshesV2(filePath));
    }

    /**
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public Map<String, Mesh> loadObjMeshes(String fileName) throws IOException {
        return loadObjMeshesV2(getFilePathFromResources(fileName));
    }
//
//    /**
//     * Parses the given .OBJ file and attempts to extract the useful Mesh data
//     * from it, populating this class's meshes map.
//     *
//     * @param filePath Path to .OBJ file
//     * @throws IOException
//     */
//    public static List<MeshTreeNode> loadObjMeshes(Path filePath) throws IOException {
////        final Map<String, Mesh> meshMap = new HashMap<>();
//        final List<MeshTreeNode> meshes = new ArrayList<>();
//        boolean builtFirstMesh = false;
//        Mesh.Builder meshBuilder = new Mesh.Builder();
//        List<Vec3> vertices = new ArrayList<>();
//        List<Vec3> textureCoords = new ArrayList<>();
//        List<Vec3> normals = new ArrayList<>();
//        List<ObjIndex> indices = new ArrayList<>();
//        Map<String, WavefrontMaterial> materials = new HashMap<>();
//        List<WWTexture> textures = new ArrayList<>();
//        int currVertexCount = 0;
//        int currNormalCount = 0;
//        int currTexCoordCount = 0;
//
//        System.out.println("Parsing " + filePath.getFileName() + "...");
//        // open file
//        String line;
//        BufferedReader bufferedReader = Files.newBufferedReader(filePath);
//        while ((line = bufferedReader.readLine()) != null) {
//            // start going through the file, reading line by line
//            // if we read an object (o) then we need to make a new mesh
//            // make a new list of verts, normals, texcoords, and indices
//            // create a mesh.builder and set those lists
//            // add the newly created mesh to the map
//
//            if (line.charAt(0) != '#') {  // ignore comments
//                final String[] tokens = line.split("\\s+");
//                if (tokens.length > 0) {
//                    final String firstToken = tokens[0];
//
//                    switch (firstToken) {
//                        case "o":
//                            final String objectName = tokens[1];
//
//                            // start of new object, create mesh and add it to map
//                            if (builtFirstMesh) {
//                                final Mesh mesh = buildMesh(meshBuilder,
//                                        vertices,
//                                        textureCoords,
//                                        normals,
//                                        indices,
//                                        currVertexCount,
//                                        currNormalCount,
//                                        currTexCoordCount);
//                                final MeshTreeNode meshTreeNode = new MeshTreeNode.Builder()
//                                 .
//                                meshMap.put(mesh.getName(), mesh);
//                                currVertexCount += vertices.size();
//                                currNormalCount += normals.size();
//                                currTexCoordCount += textureCoords.size();
//                            } else {
//                                builtFirstMesh = true;
//                            }
//                            // create a new mesh builder & reset working lists
//                            meshBuilder = new Mesh.Builder();
//                            meshBuilder.setName(objectName);
//                            vertices = new ArrayList<>();
//                            textureCoords = new ArrayList<>();
//                            normals = new ArrayList<>();
//                            indices = new ArrayList<>();
//
//                            break;
//                        case "v":
//                            vertices.add(new Vec3(Float.parseFloat(tokens[1]),
//                                    Float.parseFloat(tokens[2]),
//                                    Float.parseFloat(tokens[3])));
//                            break;
//                        case "vn":
//                            normals.add(new Vec3(Float.parseFloat(tokens[1]),
//                                    Float.parseFloat(tokens[2]),
//                                    Float.parseFloat(tokens[3])));
//                            break;
//                        case "vt":
//                            // u, v, w (optional)
//                            Vec3 texCoord = null;
//                            if (tokens.length == 3) {
//                                texCoord = new Vec3(Float.parseFloat(tokens[1]),
//                                        Float.parseFloat(tokens[2]),
//                                        0.0f);
//                            } else {
//                                new Vec3(Float.parseFloat(tokens[1]),
//                                        Float.parseFloat(tokens[2]),
//                                        Float.parseFloat(tokens[3]));
//                            }
//                            textureCoords.add(texCoord);
//                            break;
//                        case "f":
//                            // split each token with '/'
//                            // f vi/ti/ni vi/ti/ni vi/ti/ni
//                            // or
//                            // f vi vi vi
//                            meshBuilder.setMeshType(Mesh.MeshType.POLYGON_MESH);
//
//                            if (line.contains("/")) {
//                                for (int i = 1; i < tokens.length; i++) {
//                                    final String faceToken = tokens[i];
//                                    final String[] faceTokens = faceToken.split("/");
//                                    ObjIndex.Builder objIndexBuilder = new ObjIndex.Builder();
//                                    objIndexBuilder.setVertexIndex(Integer.valueOf(faceTokens[0]) - 1);
//                                    if (!faceTokens[1].isEmpty())
//                                        objIndexBuilder.setTextureCoordIndex(Integer.valueOf(faceTokens[1]) - 1);
//                                    if (!faceTokens[2].isEmpty())
//                                        objIndexBuilder.setNormalIndex(Integer.valueOf(faceTokens[2]) - 1);
//                                    indices.add(objIndexBuilder.build());
//                                }
//                            } else {
//                                // uses spaces for each face vertex
//                                for (int i = 1; i < tokens.length; i++) {
//                                    ObjIndex.Builder objIndexBuilder = new ObjIndex.Builder();
//                                    objIndexBuilder.setVertexIndex(Integer.valueOf(tokens[i]) - 1);
//                                    indices.add(objIndexBuilder.build());
//                                }
//                            }
//
//                            break;
//                        case "l":
//                            // dont split, keep original tokens
//
//                            // l vi vi
//                            // or
//                            // l vi/ti vi/ti
//
//                            meshBuilder.setMeshType(Mesh.MeshType.POLYLINE_MESH);
//
//                            // just considering first case for now...
//                            for (int i = 1; i < tokens.length; i++) {
//                                ObjIndex.Builder objIndexBuilder = new ObjIndex.Builder();
//                                objIndexBuilder.setVertexIndex(Integer.valueOf(tokens[i]) - 1);
//                                indices.add(objIndexBuilder.build());
//                            }
//                            break;
//                        case "mtllib":
//                            // load MTL files
//                            // mtllib filename1 filename2 . . .
//                            final String restLine = line.replace("mtllib ", "");
//                            final String[] mtlTokens = restLine.split("\\.mtl");
//
//                            for(int i = 0; i < mtlTokens.length; i++) {    // for each MTL file
//                                final String mtlFileName = mtlTokens[i] + ".mtl";
//                                // filename is likely relative
//                                final Path mtlFilePath = Paths.get(filePath.getParent().toString(), mtlFileName);
//                                Map<String, WavefrontMaterial> parsedMaterials = parseMtlFile(mtlFilePath);
//                                materials.putAll(parsedMaterials);
//                            }
//
//                            break;
//                        case "usemtl":
//                            final WavefrontMaterial material = materials.get(tokens[1]);
//                            if(material != null) {
//                                meshBuilder.setMaterial(material);
//                            } else {
//                                throw new IOException("material " + tokens[1] + " not found in any of the MTL files");
//                            }
//                            break;
//                    }
//                }
//            } // end else (not a comment)
//        } // end while read line
//
//        // reached end of file
//        final Mesh mesh = buildMesh(meshBuilder, vertices, textureCoords, normals, indices, currVertexCount, currNormalCount, currTexCoordCount);
//        meshMap.put(mesh.getName(), mesh);
//        bufferedReader.close();
//        System.out.println("Finished parsing " + filePath.getFileName());
//        return meshMap;
//    }


    /**
     * Parses the given .OBJ file and attempts to extract the useful Mesh data
     * from it, populating this class's meshes map.
     *
     * @param filePath Path to .OBJ file
     * @throws IOException
     */
    public Map<String, Mesh> loadObjMeshesV2(Path filePath) throws IOException {
        meshes = new HashMap<>();
        state = ParserState.INIT;

        vertices = new ArrayList<>();
        textureCoords = new ArrayList<>();
        normals = new ArrayList<>();
        indices = new ArrayList<>();
        materials = new HashMap<>();

        boolean builtFirstMesh = false;
        Mesh.Builder meshBuilder = null;
        String currObjName = "";

        textures = new ArrayList<>();

        try(BufferedReader bufferedReader = Files.newBufferedReader(filePath)) {
            String line;
            while((line = bufferedReader.readLine()) != null) {
                final String[] tokens = line.split("\\s+");
                if(tokens.length <= 0)
                    continue;
                final String firstToken = tokens[0];

                if(firstToken.trim().length() <= 0)
                    continue;
                if(firstToken.trim().charAt(0) == '#') // ignore comments
                    continue;

                switch(state) {
                    case INIT:
                        switch(firstToken) {
                            case "o":
                                state = ParserState.PROCESS_VNT;
//                                vertices = new ArrayList<>();
//                                textureCoords = new ArrayList<>();
//                                normals = new ArrayList<>();
                                indices = new ArrayList<>();
                                currObjName = tokens[1];
                                break;
                            case "mtllib":
                                // load MTL files
                                // mtllib filename1 filename2 . . .
                                final String restLine = line.replace("mtllib ", "");
                                final String[] mtlTokens = restLine.split("\\.mtl");

                                for (String mtlToken : mtlTokens) {
                                    final String mtlFileName = mtlToken + ".mtl";
                                    // filename is likely relative
                                    final Path mtlFilePath = Paths.get(filePath.getParent().toString(), mtlFileName);
                                    Map<String, WavefrontMaterial> parsedMaterials = parseMtlFile(mtlFilePath);
                                    materials.putAll(parsedMaterials);
                                }
                                break;
                            default:
                                throw new IOException("Illegal token " + firstToken);
                        }
                        break;
                    case PROCESS_VNT:
                        switch(firstToken) {
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
                                state = ParserState.PROCESS_VERTS;
                                meshBuilder = new Mesh.Builder();
                                meshBuilder.setName(currObjName);
                                meshBuilder.setMeshType(Mesh.MeshType.POLYGON_MESH);
                                processVertLine(line, tokens);
                                break;
                            case "g":
                                state = ParserState.PROCESS_VERTS;
                                meshBuilder = new Mesh.Builder();
                                meshBuilder.setName(currObjName + "." + tokens[1]);
                                break;
                            case "usemtl":
                                state = ParserState.PROCESS_VERTS;
                                meshBuilder = new Mesh.Builder();
                                meshBuilder.setName(currObjName);
                                final WavefrontMaterial material = materials.get(tokens[1]);
                                if(material != null) {
                                    meshBuilder.setMaterial(material);
                                } else {
                                    throw new IOException("material " + tokens[1] + " not found in any of the MTL files");
                                }
                                break;
                            case "s":
                                // ignore smoothing group
                                break;
                            default:
                                throw new IOException("Illegal token " + firstToken);
                        }
                        break;
                    case PROCESS_VERTS:
                        switch(firstToken) {
                            case "o": {
                                state = ParserState.PROCESS_VNT;
//                                vertices = new ArrayList<>();
//                                textureCoords = new ArrayList<>();
//                                normals = new ArrayList<>();

                                // current mesh builder needs to be built and put in list
                                final Mesh mesh = buildMeshV2(meshBuilder, vertices, textureCoords, normals, indices);
//                                final MeshTreeNode meshTreeNode = new MeshTreeNode(mesh, null, null);
                                meshes.put(mesh.getName(), mesh);


                                indices = new ArrayList<>();
                                currObjName = tokens[1];

                                meshBuilder = new Mesh.Builder();
                                meshBuilder.setName(currObjName + "." + tokens[1]);
                                break;
                            }
                            case "g": {
                                // current mesh builder needs to be built and put in list
                                final Mesh mesh = buildMeshV2(meshBuilder, vertices, textureCoords, normals, indices);
//                                final MeshTreeNode meshTreeNode = new MeshTreeNode(mesh, null, null);
                                meshes.put(mesh.getName(), mesh);
                                meshBuilder = new Mesh.Builder();
                                meshBuilder.setName(currObjName + "." + tokens[1]);
                                break;
                            }

                            case "f":
                                meshBuilder.setMeshType(Mesh.MeshType.POLYGON_MESH);
                                processVertLine(line, tokens);
                                break;

                            case "l":
                                break;

                            case "usemtl": {
                                // current mesh builder needs to be built and put in list
                                final Mesh mesh = buildMeshV2(meshBuilder, vertices, textureCoords, normals, indices);
//                                final MeshTreeNode meshTreeNode = new MeshTreeNode(mesh, null, null);
                                meshes.put(mesh.getName(), mesh);
                                meshBuilder = new Mesh.Builder();
                                meshBuilder.setName(currObjName + "." + String.valueOf(meshes.size()));
                                final WavefrontMaterial material = materials.get(tokens[1]);
                                if(material != null) {
                                    meshBuilder.setMaterial(material);
                                } else {
                                    throw new IOException("material " + tokens[1] + " not found in any of the MTL files");
                                }
                                break;
                            }

                            case "s":
                                // ignore smoothing groups
                                break;

                            default:
                                throw new IOException("Illegal token " + firstToken);
                        }
                        break;
                    case READ_EOF:
                        break;
                }
            }

            state = ParserState.READ_EOF;

            final Mesh mesh = buildMeshV2(meshBuilder, vertices, textureCoords, normals, indices);
            meshes.put(mesh.getName(), mesh);
        }

        return meshes;
    }

    private void processVertLine(String line, String[] tokens) {
        // split each token with '/'
        // f vi/ti/ni vi/ti/ni vi/ti/ni
        // or
        // f vi vi vi
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

        // TODO: Can optimize using element index buffer if we can remove all v/n/t duplicates

        // build vertex list and index buffer
        final List<Vertex> vertexList = new ArrayList<>();

        for(ObjIndex index : indices) {
            Vertex.Builder vertexBuilder = new Vertex.Builder(vertices.get(index.getVertexIndex()-currVertexCount));
            index.getNormalIndex()
                 .ifPresent(ni -> vertexBuilder.setNormal(normals.get(ni-currNormalCount)));
            index.getTextureCoordIndex()
                 .ifPresent(ti -> vertexBuilder.setTexCoord(textureCoords.get(ti-currTexCoordCount)));
            vertexList.add(vertexBuilder.build());
        }

        final IntBuffer indicesBuf = IntBuffer.allocate(vertexList.size());
        for(int i = 0; i < vertexList.size(); i++) {
            indicesBuf.put(i);
        }
        indicesBuf.flip();
        meshBuilder.setVertices(vertexList);
        meshBuilder.setIndices(indicesBuf);

        return meshBuilder.build();
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
    private static Mesh buildMeshV2(Mesh.Builder meshBuilder,
                                    List<Vec3> vertices,
                                    List<Vec3> textureCoords,
                                    List<Vec3> normals,
                                    List<ObjIndex> indices) {

        // TODO: Can optimize using element index buffer if we can remove all v/n/t duplicates

        // build vertex list and index buffer
        final List<Vertex> vertexList = new ArrayList<>();

        for(ObjIndex index : indices) {
            Vertex.Builder vertexBuilder = new Vertex.Builder(vertices.get(index.getVertexIndex()));
            index.getNormalIndex()
                 .ifPresent(ni -> vertexBuilder.setNormal(normals.get(ni)));
            index.getTextureCoordIndex()
                 .ifPresent(ti -> vertexBuilder.setTexCoord(textureCoords.get(ti)));
            vertexList.add(vertexBuilder.build());
        }

        final IntBuffer indicesBuf = IntBuffer.allocate(vertexList.size());
        for(int i = 0; i < vertexList.size(); i++) {
            indicesBuf.put(i);
        }
        indicesBuf.flip();
        meshBuilder.setVertices(vertexList);
        meshBuilder.setIndices(indicesBuf);

        return meshBuilder.build();
    }

    /**
     * Parses a MTL file
     *
     * @param mtlFilePath the {@link Path} to the MTL file
     * @return the {@link Material}s parsed from the MTL file
     */
    private static Map<String, WavefrontMaterial> parseMtlFile(Path mtlFilePath) throws IOException {
        final Map<String, WavefrontMaterial> materials = new HashMap<>();

        String name = null;
        Color ambient = null;
        Color diffuse = null;
        Color specular = null;
        Float shininess = null;
        float alpha = 1.0f;
        Path diffuseTextureMapFilepath = null;
        boolean createdFirstMtl = false;
        String line;
        BufferedReader bufferedReader = Files.newBufferedReader(mtlFilePath);

        while((line = bufferedReader.readLine()) != null) {
            if(!line.isEmpty() && line.charAt(0) != '#') {  // ignore comments
                final String[] tokens = line.split("\\s+");
                if (tokens.length > 0) {
                    final String firstToken = tokens[0];
                    switch (firstToken) {
                        case "newmtl":
                            // start of a new material
                            if(createdFirstMtl) {
                                diffuse = new Color(diffuse.getRed() / 255f,
                                                    diffuse.getGreen() / 255f,
                                                    diffuse.getBlue() / 255f,
                                                    alpha);
                                materials.put(name, new WavefrontMaterial(specular,
                                                                          diffuse,
                                                                          ambient,
                                                                          new Color(0, 0, 0),
                                                                          shininess,
                                                                          diffuseTextureMapFilepath));

                                ambient = null;
                                diffuse = null;
                                specular = null;
                                shininess = null;
                                alpha = 1.0f;
                                diffuseTextureMapFilepath = null;
                            } else {
                                createdFirstMtl = true;
                            }
                            name = tokens[1];
                            break;
                        case "Ka":
                            // ambient color
                            ambient = new Color(Float.parseFloat(tokens[1]),
                                                Float.parseFloat(tokens[2]),
                                                Float.parseFloat(tokens[3]));
                            break;
                        case "Kd":
                            // diffuse color
                            diffuse = new Color(Float.parseFloat(tokens[1]),
                                                Float.parseFloat(tokens[2]),
                                                Float.parseFloat(tokens[3]));
                            break;
                        case "Ks":
                            // specular color
                            specular = new Color(Float.parseFloat(tokens[1]),
                                                 Float.parseFloat(tokens[2]),
                                                 Float.parseFloat(tokens[3]));
                            break;
                        case "Ns":
                            // specular exponent / shininess
                            shininess = Float.parseFloat(tokens[1]);
                            break;
                        case "d":
                            // transparency
                            alpha = Float.parseFloat(tokens[1]);
                            break;
                        case "Tr":
                            // transparency (1-d)
                            alpha = 1.0f - Float.parseFloat(tokens[1]);
                            break;
                        case "illum":
                            // 0 This is a constant color illumination model. The color is the specified Kd for the material. The formula is:
                            //   color = Kd
                            // 1 This is a diffuse illumination model using Lambertian shading. The color includes an ambient and diffuse shading terms for each light source. The formula is
                            //   color = KaIa + Kd { SUM j=1..ls, (N * Lj)Ij }
                            // 2 This is a diffuse and specular illumination model using Lambertian shading and Blinn's interpretation of Phong's specular illumination model (BLIN77).
                            //   The color includes an ambient constant term, and a diffuse and specular shading term for each light source. The formula is:
                            //   color = KaIa + Kd { SUM j=1..ls, (N*Lj)Ij } + Ks { SUM j=1..ls, ((H*Hj)^Ns)Ij }
                            //
                            // ...yeah ignore for now
                            break;
                        case "map_Ka":  // ambient texture map
                            // ignore for now
                            break;
                        case "map_Kd":  // diffuse texture map
                            String textureFilename = tokens[1];
                            if(Paths.get(textureFilename).isAbsolute())
                                diffuseTextureMapFilepath = Paths.get(textureFilename);
                            else
                                diffuseTextureMapFilepath = Paths.get(mtlFilePath.getParent().toString(), textureFilename);
                            int abc = 123;
                            break;
                    }
                }
            } // end ignore comments
        } // end while read line;

        diffuse = new Color(diffuse.getRed() / 255f,
                            diffuse.getGreen() / 255f,
                            diffuse.getBlue() / 255f,
                            alpha);
        materials.put(name, new WavefrontMaterial(specular,
                                                  diffuse,
                                                  ambient,
                                                  new Color(0, 0, 0),
                                                  shininess,
                                                  diffuseTextureMapFilepath));

        bufferedReader.close();
        return materials;
    }
}
