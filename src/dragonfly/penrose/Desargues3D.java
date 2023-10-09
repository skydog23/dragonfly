/*
 * Created on 25 Sep 2023
 *
 */
package dragonfly.penrose;

import java.awt.Color;

import charlesgunn.jreality.geometry.projective.LineUtility;
import charlesgunn.jreality.newtools.FlyTool;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.math.p5.PlueckerLineGeometry;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.PointSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.tools.DragEventTool;
import de.jreality.tools.PointDragEvent;
import de.jreality.tools.PointDragListener;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class Desargues3D extends Assignment {

	final Color c1 = new Color(255, 0, 255),
			c2 = new Color(0,255,200),
			cz = new Color(200,200,0),
			vc1 = new Color(255,0,0),
			vc2 = new Color(0,255,0),
			vc3 = new Color(100,100,255);

	SceneGraphComponent
		world,
			linesSGC,
			pointsSGC,
			trianglesSGC;
	double[][] planes = {{0,0,0,1}, {.5,.5,2,1}};
	double[] Z = {0,0,2,1};
//	double[][] initTri = {
//			{1,0,0,1},
//			{-.5, .866, 0 ,1},
//			{-.5, -.866, 0, 1}
//	};
	double[] initPoints = {
			0.00000,	0.00000,	2.00000,	1.00000,		
	0.815402,	0.794253,	0.922630,	1.00000,		
	-0.632649,	1.10904,	0.456673,	1.00000,		
	-0.513560,	-0.613359,	0.317136,	1.00000,		
	3.02020,	2.94187,	-1.99052,	1.00000	,	
	-1.11051,	1.94675,	-0.709059,	1.00000,		
	-0.653519,	-0.780517,	-0.141491,	1.00000,		
	-2.71170,	1.56101,	-0.212328,	1.00000,		
	-0.395563,	-2.31996,	0.178880,	1.00000,		
	-1.06529,	-1.19774,	0.0657591,	1.00000	};	

	double[][] points = new double[10][],
			lines = new double[10][];
	
	boolean doColors = true;
	@Override
	public SceneGraphComponent getContent() {
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		linesSGC = SceneGraphUtility.createFullSceneGraphComponent("lines");
		pointsSGC = SceneGraphUtility.createFullSceneGraphComponent("points");
		trianglesSGC = SceneGraphUtility.createFullSceneGraphComponent("triangles");
		world.addChildren(linesSGC,pointsSGC,trianglesSGC);
		MatrixBuilder.euclidean().rotateX(-Math.PI/2).assignTo(world);
		Appearance ap = pointsSGC.getAppearance();
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute("pointShader.pointRadius", .03);
		ap.setAttribute("pointShader.diffuseColor", Color.white);
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		ap = linesSGC.getAppearance();
		ap.setAttribute("lineShader.tubeRadius", .01);
		ap.setAttribute("lineShader.diffuseColor", Color.cyan);
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute(CommonAttributes.PICKABLE, false);
		ap = trianglesSGC.getAppearance();
		ap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
		ap.setAttribute("polygonShader."+CommonAttributes.TRANSPARENCY,.35 );
	    ap = world.getAppearance();
	    ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute(CommonAttributes.AMBIENT_COEFFICIENT, .1);

		init();
		update();
		
		DragEventTool t = new DragEventTool();
		t.addPointDragListener(new PointDragListener() {

			public void pointDragStart(PointDragEvent e) {
				System.out.println("drag start of vertex no "+e.getIndex());				
			}

			public void pointDragged(PointDragEvent e) {
				PointSet pointSet = e.getPointSet();
				double[][] pts=new double[pointSet.getNumPoints()][];
		        pointSet.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(pts);
		        double[] pos3 = e.getPosition().clone();
		        pos3[3] = 1.0;
		        points[e.getIndex()]=pos3;
//		        System.err.println(e.getIndex()+" npt = "+Rn.toString(pos3));
//		        pointSet.setVertexAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY_ARRAY.createReadOnly(pts));			
		        update();
			}

			public void pointDragEnd(PointDragEvent e) {
				System.err.println("points = \n"+Rn.toString(points));
			}
			
		});
		
		pointsSGC.addTool(t);

		return world;
	}

	private void update()	{
		planes[0] = PlueckerLineGeometry.planeFromPoints(null, points[1],points[2],points[3]);
		for (int i = 0; i<3; ++i)	{
			lines[i] = PlueckerLineGeometry.lineFromPoints(null, points[0], points[i+1]);
			points[i+4] = Pn.normalize(null, 
					PlueckerLineGeometry.lineIntersectPlane(null, lines[i], planes[1]),
					Pn.EUCLIDEAN);
		}
		for (int i = 0; i<3; ++i)	{		
			lines[i+3] = PlueckerLineGeometry.lineFromPoints(null, points[(i%3)+1], points[((i+1)%3)+1]);
		}
		for (int i = 0; i<3; ++i)	{
			lines[i+6] = PlueckerLineGeometry.lineFromPoints(null, points[(i%3)+4], points[((i+1)%3)+4]);
		}
		planes[1] = PlueckerLineGeometry.planeFromPoints(null, points[4],points[5],points[6]);
		lines[9] = PlueckerLineGeometry.lineFromPlanes(null, planes[0], planes[1]);
		for (int i = 0; i<3; ++i)	{
			points[i+7] = Pn.normalize(null, PlueckerLineGeometry.lineIntersectPlane(null, lines[i+3], planes[1]),
								Pn.EUCLIDEAN);
		}
		psf.setVertexCoordinates(points);
		psf.update();
		
		//linesSGC.removeAllChildren();
//		System.err.println("points = "+Rn.toString(points));
//		System.err.println("lines = "+Rn.toString(lines));
		LineUtility.sceneGraphForCurveOfLines(linesSGC, lines, null,50, true);
		updateTriangles();
	}
	
	private  int[][] tris = {{4,5,6},{1,2,3}};
	private Color[] lcols = {vc1, vc2, vc3, c1,c1,c1, c2,c2,c2,cz},
			pcols = {cz, vc1,vc2,vc3, vc1,vc2,vc3, vc3,vc1,vc2};
	private Color[] fcols = {c2, c1};
	IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
	PointSetFactory psf = new PointSetFactory();
	private void init() {
		ifsf.setVertexCount(points.length);
		ifsf.setFaceCount(2);
		ifsf.setFaceIndices(tris);
		ifsf.setFaceColors(fcols);
		ifsf.setGenerateFaceNormals(true);
		trianglesSGC.setGeometry(ifsf.getGeometry());

		psf.setVertexCount(10);
		psf.setVertexColors(pcols);
		pointsSGC.setGeometry(psf.getPointSet());
		
		points = new double[10][4];
		for (int i = 0; i<10; ++i)	{
			for (int j = 0; j<4; ++j)	{
				points[i][j] = initPoints[4*i+j];
			}
		}
		
		if (doColors) {
			for (int i= 0; i<10; ++i)	{
				SceneGraphComponent sgc = new SceneGraphComponent();
				sgc.setAppearance(new Appearance());
				linesSGC.addChild(sgc);
				sgc.getAppearance().setAttribute("lineShader.diffuseColor", lcols[i]);
			}
		}

	}
	private void updateTriangles() {
		ifsf.setVertexCoordinates(points);
		ifsf.update();
	}
	
	
	@Override
	public void display() {
		super.display();
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", Color.white);
		SceneGraphComponent cameraNode = CameraUtility.getCameraNode(viewer);
	    FlyTool flytool = new FlyTool();
	    flytool.setGain(.15);
		cameraNode.addTool(flytool);
	}

	public static void main(String[] args) {
		new Desargues3D().display();
	}

}
