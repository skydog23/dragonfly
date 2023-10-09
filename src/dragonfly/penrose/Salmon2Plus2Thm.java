/*
 * Created on 22 Sep 2023
 *
 */
package dragonfly.penrose;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import charlesgunn.jreality.geometry.projective.LineUtility;
import charlesgunn.jreality.newtools.FlyTool;
import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.jreality.texture.SimpleTextureFactory.TextureType;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.math.Utility;
import charlesgunn.math.clifford.ConicSection;
import charlesgunn.math.p5.PlueckerLineGeometry;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.PointSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P2;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.plugin.experimental.ViewerKeyListener;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.tools.DragEventTool;
import de.jreality.tools.PointDragEvent;
import de.jreality.tools.PointDragListener;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class Salmon2Plus2Thm extends Assignment {

	private static final double conicTubeRad = .008,
			vPointsRad = .023,
			tetraVRad = vPointsRad,
			nullContactLineRad = .008,
			tetraTubeRad = .007;

	private static final Color sC = new Color(255,0,0),
			tC = new Color(0,255,0),
			uC = new Color(0,200, 200),
			tetraColor = new Color(120,120,120), //255,220,130),
			tetraEdgeColor = new Color(0,0,150), //255,220,130),
			conicColor = new Color(255,200,0), //new Color(50,50, 255),
			planeColor = new Color(240,240,240);

	SceneGraphComponent world,
		sgc3d,
			tetraSGC,
			tetraLinesSGC,
		sgc2d,
			v4PointLinesSGC,
			pointPlaneSGC,
			vPointsSGC,
			conicSGC;
	
	SceneGraphComponent contentNode = null;
	Matrix oldContentM = new Matrix();
	IndexedFaceSetFactory tetraFac = new IndexedFaceSetFactory(),
			planeFac = null;
	PointSetFactory vPointsFac = new PointSetFactory();
	double ppScale = 6;
	boolean doTransp = true,
			pointPlaneOnly = false;
	@Override
	public SceneGraphComponent getContent() {
		
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		sgc2d = SceneGraphUtility.createFullSceneGraphComponent("2d");
		sgc3d = SceneGraphUtility.createFullSceneGraphComponent("3d");
		tetraSGC = SceneGraphUtility.createFullSceneGraphComponent("tetra");
		tetraLinesSGC = SceneGraphUtility.createFullSceneGraphComponent("tetra lines");
		pointPlaneSGC = SceneGraphUtility.createFullSceneGraphComponent("pointPlane");
		vPointsSGC = SceneGraphUtility.createFullSceneGraphComponent("v points");
		v4PointLinesSGC = SceneGraphUtility.createFullSceneGraphComponent("ver 4-point lines");
		conicSGC = SceneGraphUtility.createFullSceneGraphComponent("conic");
		world.addChildren(sgc2d, sgc3d);
		sgc3d.addChildren(tetraSGC, tetraLinesSGC, pointPlaneSGC); //v4PointLinesSGC, pointPlaneSGC);
		sgc2d.addChildren(vPointsSGC, conicSGC, v4PointLinesSGC); //v4PointLinesSGC, pointPlaneSGC);
		initTetra();
		tetraSGC.setGeometry(tetraFac.getGeometry());

		Appearance ap = tetraSGC.getAppearance();
		ap.setAttribute(CommonAttributes.FACE_DRAW, false);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute("lineShader."+CommonAttributes.TUBES_DRAW, true);
		ap.setAttribute("lineShader."+CommonAttributes.TUBE_RADIUS, tetraTubeRad);
		ap.setAttribute("lineShader."+CommonAttributes.DIFFUSE_COLOR,tetraEdgeColor);
		ap.setAttribute("pointShader."+CommonAttributes.SPHERES_DRAW, true);
		ap.setAttribute("pointShader.pointRadius", tetraVRad);
		ap = tetraLinesSGC.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute("lineShader."+CommonAttributes.TUBE_RADIUS, nullContactLineRad);
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		
		initPointPlane();
		pointPlaneSGC.setGeometry(planeFac.getGeometry());
		MatrixBuilder.euclidean().scale(ppScale).translate(-.5,-.5,0).assignTo(pointPlaneSGC);
		ap = pointPlaneSGC.getAppearance();
		ap.setAttribute(CommonAttributes.FACE_DRAW, true);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute("polygonShader.diffuseColor", planeColor);
		ap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
		ap.setAttribute("polygonShader."+CommonAttributes.TRANSPARENCY, doTransp ? .35 : 0.0);
		ap.setAttribute(CommonAttributes.PICKABLE, false);
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);

		if (!doTransp) {
			int br = 240;
			Color gridC = new Color(br, br, br);
			SimpleTextureFactory stf = new SimpleTextureFactory();
			stf.setType(TextureType.GRAPH_PAPER);
			stf.setColor(2, gridC);
			stf.setColor(3, Color.blue);
			stf.setColor(0, new Color(br, br, br, 0));
			stf.setColor(1, gridC);
			stf.setSize(256);
			stf.update();
			Texture2D tex2d = TextureUtility.createTexture(ap, "polygonShader", stf.getImageData());
			Matrix foo = new Matrix();
			MatrixBuilder.euclidean().scale(2.5*ppScale, 2.5*ppScale,1).assignTo(foo);
			tex2d.setTextureMatrix(foo);
//			MatrixBuilder.euclidean().translate(-25,-25, -.01).scale(50).assignTo(pointPlaneSGC);
			ap.setAttribute("polygonShader.diffuseColor", Color.white);
			ap.setAttribute("ambientCoefficient", .05);			
		}

		
		initVPoints();
		vPointsSGC.setGeometry(vPointsFac.getGeometry());
		ap = vPointsSGC.getAppearance();
		ap.setAttribute(CommonAttributes.FACE_DRAW, false);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
//		ap.setAttribute("pointShader."+CommonAttributes.DIFFUSE_COLOR,tetraEdgeColor);
//		ap.setAttribute("pointShader.polygonShader."+CommonAttributes.DIFFUSE_COLOR,tetraEdgeColor);
		ap.setAttribute("pointShader."+CommonAttributes.SPHERES_DRAW, true);
		ap.setAttribute("pointShader.pointRadius", vPointsRad);
		v4PointLinesSGC.setAppearance(tetraLinesSGC.getAppearance());
		
		initConic();
		ap = conicSGC.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute("lineShader.diffuseColor", conicColor);
		ap.setAttribute("lineShader."+CommonAttributes.TUBES_DRAW, true);
		ap.setAttribute("lineShader."+CommonAttributes.TUBE_RADIUS, conicTubeRad);

		world.getAppearance().setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);

		DragEventTool t = new DragEventTool();
		t.addPointDragListener(new PointDragListener() {

			public void pointDragStart(PointDragEvent e) {
				System.out.println("drag start of vertex no "+e.getIndex());				
			}

			public void pointDragged(PointDragEvent e) {
				PointSet pointSet = e.getPointSet();
				double[][] points=new double[pointSet.getNumPoints()][];
		        pointSet.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(points);
		        points[e.getIndex()]=e.getPosition();  
		        pointSet.setVertexAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(points));			
		        updateGeometry();
			}

			public void pointDragEnd(PointDragEvent e) {

			}
			
		});
		
		tetraSGC.addTool(t);
		return world;
	}

	@Override
	public void display() {
		super.display();
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", new Color(255,255,255,255));
		SceneGraphComponent cameraNode = CameraUtility.getCameraNode(viewer);
	    FlyTool flytool = new FlyTool();
	    flytool.setGain(.15);
		cameraNode.addTool(flytool);
		
		SceneGraphPath sgp = SceneGraphUtility.getPathsToNamedNodes(viewer.getSceneRoot(), "content").get(0);
		contentNode = sgp.getLastComponent();
		
		((Component) viewer.getViewingComponent()).addKeyListener(new KeyAdapter()	{
			
		    @Override
			public void keyPressed(KeyEvent e)	{ 
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_H:
					break;

				case KeyEvent.VK_1:
					pointPlaneOnly = !pointPlaneOnly;
					if (pointPlaneOnly)	{
						sgc3d.setVisible(false);
						oldContentM.assignFrom(contentNode.getTransformation());
						new Matrix().assignTo(contentNode);
					} else {
						oldContentM.assignTo(contentNode.getTransformation());
						sgc3d.setVisible(true);
					}
					break;
				case KeyEvent.VK_2:
					v4LinesSGC[2].setVisible(!v4LinesSGC[2].isVisible());
					v4LinesSGC[3].setVisible(!v4LinesSGC[3].isVisible());
					break;
					}
			}
		});
	}
	public static void main(String[] args) {
		new Salmon2Plus2Thm().display();
	}
	
	double asymm = .6;
	private double[][] tv4 =  
			{{1,1,1+asymm,1},   // red
			{1,-1,-1+asymm,1},
			{-1,1,-1-asymm,1},
			{-1,-1,1-asymm,1}};  // green

	private double curvert[][] = tv4;
	
	private int[][] tfi = {
		{0,1,2},
		{2,1,3},
		{1,0,3},
		{0,2,3}};

	private int[][] tei = {
			{0,1},{0,2},{0,3},{1,2},{1,3},{2,3}
	};
	
	double[][] vpts = new double[6][];
	double[] vplane = {0,0,1,0};    // the point plane
	Color dtec = tetraEdgeColor;
	Color[] tvc = {sC, tC, tC, sC};
	Color[] tec = {dtec, dtec, sC, tC, dtec, dtec};
	Color[] vc = {tC, sC, uC, uC, sC, tC};
	String[] vlbls = {"0","1","2","3","4","5"};
	double[][] tetraLines = new double[6][],
			v4PointLines = new double[6][];
	Color[] v4c = null;
	SceneGraphComponent[] linesSGC = new SceneGraphComponent[6],
			v4LinesSGC = new SceneGraphComponent[6];
	
	private void initGeometry() {
		initTetra();
		initPointPlane();
		initVPoints();
	}
	private void initTetra() {
		tetraFac.setVertexCount(4);
		tetraFac.setEdgeCount(6);
		tetraFac.setFaceCount(4);
		tetraFac.setGenerateFaceNormals(true);
	
		tetraFac.setVertexCoordinates(curvert);
		tetraFac.setFaceIndices(tfi);
		tetraFac.setEdgeIndices(tei);
		tetraFac.setVertexColors(tvc);
		tetraFac.update();
		
		for (int i = 0; i<6; ++i)	{
			tetraLines[i] = PlueckerLineGeometry.lineFromPoints(null, tv4[tei[i][0]], tv4[tei[i][1]]);
			linesSGC[i] = LineUtility.sceneGraphForLine(linesSGC[i], tetraLines[i], null, 5.0, true);
			linesSGC[i].setAppearance(new Appearance());
			linesSGC[i].getAppearance().setAttribute("lineShader.diffuseColor", tec[i]);
			tetraLinesSGC.addChild(linesSGC[i]);
			linesSGC[i].setVisible(i==2 || i == 3);
		}
	}
	
	private void initPointPlane() {
		planeFac = Primitives.texturedQuadrilateralFactory();
		planeFac.update();
	}

	private void initVPoints()	{
		vPointsFac.setVertexCount(6);
		vPointsFac.setVertexColors(tec);
//		vPointsFac.setVertexLabels(vlbls);
		for (int i = 0; i<6; ++i)	{
			v4LinesSGC[i] = new SceneGraphComponent("v4"+i);
			v4LinesSGC[i].setAppearance(new Appearance());
			v4LinesSGC[i].getAppearance().setAttribute("lineShader.diffuseColor", vc[i]);
			v4PointLinesSGC.addChild(v4LinesSGC[i]);
			if (i == 3 || i == 2) v4LinesSGC[i].setVisible(false);
		}
		updateVPoints();
	}
	private void updateTetra() {
        curvert = tetraFac.getIndexedFaceSet().getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		for (int i = 0; i<6; ++i)	{
			tetraLines[i] = PlueckerLineGeometry.lineFromPoints(null, curvert[tei[i][0]], curvert[tei[i][1]]);
			LineUtility.sceneGraphForLine(linesSGC[i], tetraLines[i], null, 5.0, true);
		}
	}
	
	private void updateVPoints() {
		if (tetraLines[0] == null) return;
		for (int i = 0; i<6; ++i)	{
			vpts[i] = PlueckerLineGeometry.lineIntersectPlane(null, tetraLines[i], vplane);
			Pn.dehomogenize(vpts[i], vpts[i]);
		}
//		System.err.println("vpoints = "+Rn.toString(vpts));
		vPointsFac.setVertexCoordinates(vpts);
		vPointsFac.update();
		double[][] v4 = {vpts[0], vpts[1], vpts[4], vpts[5]};
		for (int i = 0; i<6; ++i)	{
			v4PointLines[i] = PlueckerLineGeometry.lineFromPoints(null, v4[tei[i][0]], v4[tei[i][1]]);
			LineUtility.sceneGraphForLine(v4LinesSGC[i], v4PointLines[i], null, 5.0, true);
		}
		
	}
	
	double[][] v3pts = new double[6][];
	
	private void initConic() {
		updateConic();
	}
	
	ConicSection conic = new ConicSection();
	private void updateConic() {
		// convert to 2D
		v3pts = Utility.demote(null, vpts);
		double[] A = v3pts[0],
				B = v3pts[4],
				C = v3pts[5],
				D = v3pts[1],
				P = v3pts[2],
				Q = v3pts[3],
				r = P2.lineFromPoints(null, P, Q),
				bd = P2.lineFromPoints(null, B, D),
				ac = P2.lineFromPoints(null, A, C),
				X = P2.pointFromLines(null, r, ac),
				t = P2.lineFromPoints(null, B, X);
		double[][] persp5 = {A, bd, P, t, C};
		conic.setFivePerspectivities(persp5);
		IndexedLineSet ils = conic.getCurve(200);
		conicSGC.setGeometry(ils);
	}
	private void updateGeometry() {
		updateTetra();
		updateVPoints();
		updateConic();
	}
}
