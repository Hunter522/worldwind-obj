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
        testModel = new ObjModel("monkey.obj");
        final Map<String, Mesh> meshes = testModel.getMeshes();

        assertEquals(1, meshes.size(), 0);
        assertTrue(meshes.containsKey("Suzanne"));

        final Mesh polygonMesh = meshes.get("Suzanne");
        assertEquals(507, polygonMesh.getVertices().size(), 0);
        assertEquals(942, polygonMesh.getNormals().size(), 0);
        assertEquals(0, polygonMesh.getTextureCoords().size(), 0);
        assertEquals(5808, polygonMesh.getIndices().size(), 0);
        assertEquals(Mesh.MeshType.POLYGON_MESH, polygonMesh.getMeshType());
    }

    @Test
    public void testLoadPolylineMesh() throws Exception {
        testModel = new ObjModel("testpath.obj");
        Map<String, Mesh> meshes = testModel.getMeshes();

        assertEquals(2, meshes.size(), 0);
        final Mesh polylineMesh1 = meshes.get("NurbsPath.001");
        assertEquals(48, polylineMesh1.getVertices().size(), 0);
        assertEquals(0, polylineMesh1.getNormals().size(), 0);
        assertEquals(0, polylineMesh1.getTextureCoords().size(), 0);
        assertEquals(94, polylineMesh1.getIndices().size(), 0);
        assertEquals(Mesh.MeshType.POLYLINE_MESH, polylineMesh1.getMeshType());

        final Mesh polylineMesh2 = meshes.get("NurbsPath");
        assertEquals(120, polylineMesh2.getVertices().size(), 0);
        assertEquals(0, polylineMesh2.getNormals().size(), 0);
        assertEquals(0, polylineMesh2.getTextureCoords().size(), 0);
        assertEquals(238, polylineMesh2.getIndices().size(), 0);
        assertEquals(Mesh.MeshType.POLYLINE_MESH, polylineMesh2.getMeshType());
    }
}