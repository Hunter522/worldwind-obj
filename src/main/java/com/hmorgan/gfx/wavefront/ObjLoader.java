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
import java.nio.file.*;
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

    private static final Map<String, FileSystem> fileSystemCache = new HashMap<>(); // FileSystem cache

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
     * Convenience method to convert a resource into a path. This allows a {@link Path} to be created for a resource
     * that resides in a JAR file (e.g. if the project is packaged in a JAR).
     *
     * Thanks to user VGR on StackOverflow. https://stackoverflow.com/questions/15713119/java-nio-file-path-for-a-classpath-resource
     *
     * @param resource the resource to convert to a {@link Path}
     * @return the {@link Path} representing the resource
     * @throws IOException
     * @throws URISyntaxException
     */
    public static Path resourceToPath(URL resource) throws IOException, URISyntaxException {
        Objects.requireNonNull(resource, "Resource URL cannot be null");
        final URI uri = resource.toURI();

        final String scheme = uri.getScheme();
        if(scheme.equals("file")) {
            return Paths.get(uri);
        }

        if(!scheme.equals("jar")) {
            throw new IllegalArgumentException("Cannot convert to Path: " + uri);
        }

        final String s = uri.toString();
        final int separator = s.indexOf("!/");
        final String entryName = s.substring(separator + 2);
        final URI fileURI = URI.create(s.substring(0, separator));

        // create a FileSystem to allow loading resource from JAR
        // apparently one cannot create another FileSystem object on a certain path if there is already one open
        // so a cache is used here
        FileSystem fs = fileSystemCache.get(fileURI.toString());
        if(fs == null) {
            fs = FileSystems.newFileSystem(fileURI, Collections.<String, Object>emptyMap());
            fileSystemCache.put(fileURI.toString(), fs);
        }
        return fs.getPath(entryName);
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
        return new ObjModel(loadObjMeshes(filePath));
    }

    /**
     *
     * @param fileName
     * @return
     * @throws IOException
     */
    public Map<String, Mesh> loadObjMeshes(String fileName) throws IOException {
        try {
            return loadObjMeshes(resourceToPath(ObjLoader.class.getClassLoader().getResource(fileName)));
        } catch(URISyntaxException e) {
            throw new IOException(e);
        }
    }

    /**
     * Parses the given .OBJ file and attempts to extract the useful Mesh data
     * from it, populating this class's meshes map.
     *
     * @param filePath Path to .OBJ file
     * @throws IOException
     */
    public Map<String, Mesh> loadObjMeshes(Path filePath) throws IOException {
        meshes = new HashMap<>();
        state = ParserState.INIT;

        vertices = new ArrayList<>();
        textureCoords = new ArrayList<>();
        normals = new ArrayList<>();
        indices = new ArrayList<>();
        materials = new HashMap<>();

        boolean builtFirstMesh = false;
        Mesh.Builder meshBuilder = null;
        final String fileName = filePath.getFileName().toString();
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
                                indices = new ArrayList<>();
                                currObjName = fileName + ". " + tokens[1];
                                break;
                            case "mtllib":
                                // load MTL files
                                // mtllib filename1 filename2 . . .
                                final String restLine = line.replace("mtllib ", "");
                                final String[] mtlTokens = restLine.split("\\.mtl");

                                for (String mtlToken : mtlTokens) {
                                    final String mtlFileName = mtlToken + ".mtl";
                                    // filename is likely relative
                                    final Path mtlFilePath = filePath.resolveSibling(mtlFileName);
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
                                currObjName = fileName + ". " + tokens[1];

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
                            // filepath is likely relative
                            diffuseTextureMapFilepath = mtlFilePath.resolveSibling(textureFilename);
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
