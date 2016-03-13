package hmorgan.gfx.wavefront;

import com.hmorgan.gfx.Mesh;
import com.hmorgan.gfx.wavefront.ObjModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Hunter N. Morgan
 */
public class ObjModelTest {

    private ObjModel testModel;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testLoadPolygonMesh() throws Exception {
        testModel = new ObjModel("cube.obj");
        final Map<String, Mesh> meshes = testModel.getMeshes();

        assertEquals(1, meshes.size(), 0);
        assertTrue(meshes.containsKey("Cube_Cube.001"));

        final Mesh polygonMesh = meshes.get("Cube_Cube.001");
//        assertEquals(507*3, polygonMesh.getVertices().limit(), 0);
//        assertEquals(942*3, polygonMesh.getNormals().get().limit(), 0);
//        assertFalse(polygonMesh.getTextureCoords().isPresent());
//        assertEquals(5808, polygonMesh.getIndices().get().limit(), 0);
        assertEquals(Mesh.MeshType.POLYGON_MESH, polygonMesh.getMeshType());
    }

    @Test
    public void testLoadPolylineMesh() throws Exception {
        testModel = new ObjModel("testpath.obj");
        Map<String, Mesh> meshes = testModel.getMeshes();

        assertEquals(2, meshes.size(), 0);
        final Mesh polylineMesh1 = meshes.get("NurbsPath.001");
//        assertEquals(48*3, polylineMesh1.getVertices().limit(), 0);
//        assertFalse(polylineMesh1.getNormals().isPresent());
//        assertFalse(polylineMesh1.getTextureCoords().isPresent());
//        assertEquals(94, polylineMesh1.getIndices().get().limit(), 0);
        assertEquals(Mesh.MeshType.POLYLINE_MESH, polylineMesh1.getMeshType());

        final Mesh polylineMesh2 = meshes.get("NurbsPath");
//        assertEquals(120*3, polylineMesh2.getVertices().limit(), 0);
//        assertFalse(polylineMesh2.getNormals().isPresent());
//        assertFalse(polylineMesh2.getTextureCoords().isPresent());
//        assertEquals(238, polylineMesh2.getIndices().get().limit(), 0);
        assertEquals(Mesh.MeshType.POLYLINE_MESH, polylineMesh2.getMeshType());
    }
}