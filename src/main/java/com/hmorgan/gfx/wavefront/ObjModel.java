package com.hmorgan.gfx.wavefront;

import com.hmorgan.gfx.Mesh;
import com.jogamp.common.nio.Buffers;
import gov.nasa.worldwind.geom.*;
import gov.nasa.worldwind.pick.PickSupport;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.OrderedRenderable;
import gov.nasa.worldwind.util.OGLStackHandler;
import gov.nasa.worldwind.util.OGLUtil;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;
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
public class ObjModel implements OrderedRenderable {

    private Map<String, Mesh> meshes;       // collection of Meshes
    private float opacity;

    private Position position;              // geographic position of the cube
    private double roll;                    // roll (degrees)
    private double pitch;                   // pitch (degrees)
    private double yaw;                     // yaw (degrees)
    private double scale;                   // scale (1.0 is normal)

    // Determined each frame
    protected long frameTimestamp = -1L;    // frame timestamp, increments during each render cycle
    protected Vec4 placePoint;              // cartesian position of the cube, computed from #position
    protected double eyeDistance;           // distance from the eye point to the cube
    protected Extent extent;                // extent of this model which is used to compute frustum intersection

    private static final OGLStackHandler oglStackHandler = new OGLStackHandler(); // used in beginDrawing/endDrawing
    protected PickSupport pickSupport = new PickSupport();


    private ObjModel() {
        opacity = 1.0f;
        position = Position.ZERO;
    }

    /**
     * Constructs a new ObjModel.
     *
     * @param fileName String filename of .OBJ file
     * @throws IOException
     */
    public ObjModel(String fileName) throws IOException {
        this();
        this.meshes = ObjLoader.loadObjMeshes(fileName);
    }

    /**
     * Constructs a new ObjModel.
     *
     * @param filePath Path to .OBJ file
     * @throws IOException
     */
    public ObjModel(Path filePath) throws IOException {
        this();
        this.meshes = ObjLoader.loadObjMeshes(filePath);
    }

    /**
     * Constructs a new ObjModel.
     *
     * @param meshMap Map of all meshes for this ObjModel
     */
    public ObjModel(Map<String, Mesh> meshMap) {
        this.meshes = meshMap;
    }

    @Override
    public void render(DrawContext dc) {
        // 1) Set up drawing state
        // 2) Apply transform to position cube
        // 3) Draw the cube
        // 4) Restore drawing state to default

        // Rendering is controlled by NASA WorldWind's SceneController
        // The render cycle looks like this:
        // Render is called three times:
        // 1) During picking. The cube is drawn in a single color.
        // 2) As a normal renderable. The cube is added to the ordered renderable queue.
        // 3) As an OrderedRenderable. The cube is drawn.

        if (this.extent != null)
        {
            if (!this.intersectsFrustum(dc))
                return;

            // If the shape is less that a pixel in scale, don't render it.
            if (dc.isSmall(this.extent, 1))
                return;
        }

        if(dc.isOrderedRenderingMode()) {
            drawObjModel(dc);
        } else {
            makeOrderedRenderable(dc);
        }
    }

    @Override
    public void pick(DrawContext dc, Point point) {
        render(dc);
    }

    @Override
    public double getDistanceFromEye() {
        return this.eyeDistance;
    }

    /**
     * Determines whether the cube intersects the view frustum.
     *
     * @param dc the current draw context.
     *
     * @return true if this cube intersects the frustum, otherwise false.
     */
    protected boolean intersectsFrustum(DrawContext dc) {
        if (this.extent == null)
            return true; // don't know the visibility, shape hasn't been computed yet

        if (dc.isPickingMode())
            return dc.getPickFrustums().intersectsAny(this.extent);

        return dc.getView().getFrustumInModelCoordinates().intersects(this.extent);
    }

    /**
     * Setup drawing state in preparation for drawing. State changed by this method must be
     * restored in endDrawing.
     *
     * @param dc Active draw context.
     */
    public void beginDrawing(DrawContext dc) {
        final GL2 gl = dc.getGL().getGL2();
        final int attrMask = GL2.GL_CURRENT_BIT
                | GL2.GL_DEPTH_BUFFER_BIT
                | GL2.GL_LINE_BIT | GL2.GL_HINT_BIT // for outlines
                | GL2.GL_COLOR_BUFFER_BIT // for blending
                | GL2.GL_TRANSFORM_BIT // for texture
                | GL2.GL_POLYGON_BIT; // for culling


        // use stack handler to allow us to push attribs
        oglStackHandler.clear();
        oglStackHandler.pushAttrib(gl, attrMask);
        oglStackHandler.pushModelview(gl);
        oglStackHandler.pushClientAttrib(gl, GL2.GL_CLIENT_VERTEX_ARRAY_BIT);
        gl.glEnableClientState(GL2.GL_VERTEX_ARRAY); // all drawing uses vertex arrays

        // enable lighting if not in picking mode
        if(!dc.isPickingMode()) {

            gl.glEnable(GL.GL_LINE_SMOOTH);
            gl.glEnable(GL.GL_BLEND);
            OGLUtil.applyBlending(gl, false);

            dc.beginStandardLighting();
            gl.glEnable(GL2.GL_LIGHTING);
            gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);

            // Were applying a scale transform on the modelview matrix, so the normal vectors must be re-normalized
            // before lighting is computed.
            gl.glEnable(GL2.GL_NORMALIZE);

            gl.glEnable(GL2.GL_POLYGON_SMOOTH);
            gl.glHint(GL2.GL_POLYGON_SMOOTH_HINT, GL2.GL_NICEST);

            gl.glEnable(GL2.GL_LINE_SMOOTH);
            gl.glHint(GL.GL_LINE_SMOOTH_HINT, GL.GL_NICEST);
        }

        gl.glDisable(GL.GL_CULL_FACE);

        // Multiply the modelview matrix by a surface orientation matrix to set up a local coordinate system with the
        // origin at the cube's center position, the Y axis pointing North, the X axis pointing East, and the Z axis
        // normal to the globe.
        gl.glMatrixMode(GL2.GL_MODELVIEW);

        Matrix matrix = dc.getGlobe().computeSurfaceOrientationAtPosition(this.position);
        matrix = dc.getView().getModelviewMatrix().multiply(matrix);

        final Matrix attitudeMatrix =
                Matrix.fromRotationZ(Angle.fromDegrees(-yaw))
                .multiply(Matrix.fromRotationX(Angle.fromDegrees(pitch)))
                .multiply(Matrix.fromRotationY(Angle.fromDegrees(roll)));
        matrix = matrix.multiply(attitudeMatrix);


        double[] matrixArray = new double[16];
        matrix.toArray(matrixArray, 0, false);
        gl.glLoadMatrixd(matrixArray, 0);
    }

    /**
     * Restore drawing state changed in beginDrawing to the default.
     *
     * @param dc Active draw context.
     */
    public void endDrawing(DrawContext dc) {
        final GL2 gl = dc.getGL().getGL2();

        if(!dc.isPickingMode()) {

            gl.glDisable(GL.GL_LINE_SMOOTH);
            gl.glDisable(GL.GL_BLEND);

            dc.endStandardLighting();
            gl.glDisable(GL2.GL_LIGHTING);
            gl.glDisableClientState(GL2.GL_NORMAL_ARRAY);

            // Were applying a scale transform on the modelview matrix, so the normal vectors must be re-normalized
            // before lighting is computed.
            gl.glDisable(GL2.GL_NORMALIZE);
        }

        gl.glDisableClientState(GL2.GL_VERTEX_ARRAY);
        oglStackHandler.pop(gl);
    }

    /**
     * Compute per-frame attributes, and add the ordered renderable to the ordered renderable list.
     *
     * @param dc Current draw context.
     */
    protected void makeOrderedRenderable(DrawContext dc) {
        meshes.values()
                .stream()
                .filter(mesh -> !mesh.isGeneratedGlBuffers())
                .forEach(mesh -> mesh.genGlBuffers(dc));

        // This method is called twice each frame: once during picking and once during rendering. We only need to
        // compute the placePoint and eye distance once per frame, so check the frame timestamp to see if this is a
        // new frame.
        if (dc.getFrameTimeStamp() != this.frameTimestamp)
        {
            // Convert the cube's geographic position to a position in Cartesian coordinates.
            this.placePoint = dc.getGlobe().computePointFromPosition(this.position);

            // Compute the distance from the eye to the cube's position.
            this.eyeDistance = dc.getView().getEyePoint().distanceTo3(this.placePoint);

            // Compute a sphere that encloses the cube. We'll use this sphere for intersection calculations to determine
            // if the cube is actually visible.
            this.extent = new Sphere(this.placePoint, Math.sqrt(3.0) * scale / 2.0);

            this.frameTimestamp = dc.getFrameTimeStamp();
        }

        // Add the cube to the ordered renderable list. The SceneController sorts the ordered renderables by eye
        // distance, and then renders them back to front. render will be called again in ordered rendering mode, and at
        // that point we will actually draw the cube.
        dc.addOrderedRenderable(this);
    }

    /**
     * Draws this Obj model.
     *
     * @param dc Current draw context.
     */
    private void drawObjModel(DrawContext dc) {
        final GL2 gl = dc.getGL().getGL2();
        beginDrawing(dc);
        try {

            if (dc.isPickingMode()) {
                Color pickColor = dc.getUniquePickColor();
                pickSupport.addPickableObject(pickColor.getRGB(), this, this.position);
                gl.glColor3ub((byte) pickColor.getRed(), (byte) pickColor.getGreen(), (byte) pickColor.getBlue());
            }


            gl.glScaled(scale, scale, scale);
            // for each mesh, draw it
            meshes.values().forEach(mesh -> {
                final int vboBufNumVerts = (mesh.getVboBuf().limit() / 2) / 3;
                gl.glBindBuffer(GL.GL_ARRAY_BUFFER, mesh.getVboIds()[0]);
                gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, mesh.getEboIds()[0]);

                // VBO layout: vvvnnntttvvvnnnttt or just vvvnnnvvvnnn (interleaved)
                gl.glVertexPointer(3, GL.GL_FLOAT, Buffers.SIZEOF_FLOAT*6, 0);

                if(!dc.isPickingMode())
                    gl.glNormalPointer(GL.GL_FLOAT, Buffers.SIZEOF_FLOAT*6, Buffers.SIZEOF_FLOAT*3);

                mesh.getMaterial().apply(gl, GL2.GL_FRONT_AND_BACK, opacity);
                if(opacity < 1.0f) {
//                    gl.glDepthMask(false);

                    // cheap trick to achieve transparency
                    // need to render all back faces first then all front faces using culling
                    gl.glEnable(GL.GL_CULL_FACE);
                    gl.glCullFace(GL.GL_FRONT);
                    gl.glDrawArrays(GL.GL_TRIANGLES, 0, vboBufNumVerts);
                    gl.glCullFace(GL.GL_BACK);
                    gl.glDrawArrays(GL.GL_TRIANGLES, 0, vboBufNumVerts);
                    gl.glDisable(GL.GL_CULL_FACE);
                } else {
                    gl.glDrawArrays(GL.GL_TRIANGLES, 0, vboBufNumVerts);
                }
//                gl.glDrawElements(GL.GL_TRIANGLES, mesh.getIndices().get().limit(), GL.GL_UNSIGNED_INT, 0);
            });
        } finally {
            gl.glBindBuffer(GL.GL_ARRAY_BUFFER, 0);
            gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, 0);
            endDrawing(dc);
        }
    }

    ////////////////////////
    // GETTERS AND SETTERS
    ////////////////////////

    public Position getPosition() {
        return position;
    }

    public void setPosition(Position position) {
        this.position = position;
    }

    public Map<String, Mesh> getMeshes() {
        return meshes;
    }

    public float getOpacity() {
        return opacity;
    }

    public void setOpacity(float opacity) {
        this.opacity = opacity;
    }

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public void setAttitude(double roll, double pitch, double yaw) {
        setRoll(roll);
        setPitch(pitch);
        setYaw(yaw);
    }

    public double getRoll() {
        return roll;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }
}
