/*
 * Created on Dec 17, 2007
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;

import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.math.p5.PlueckerLineGeometry;
import de.jreality.geometry.BallAndStickFactory;
import de.jreality.geometry.GeometryMergeFactory;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.ThickenedSurfaceFactory;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.CameraUtility;
import de.jreality.util.SceneGraphUtility;

public class InterlockedTetrahedra  extends Assignment {

	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		Appearance ap = world.getAppearance();
		ap.setAttribute("polygonShader."+CommonAttributes.AMBIENT_COEFFICIENT	, .1);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute("lineShader.diffuseColor", new Color(102,102,102));
		ap.setAttribute("lineShader.tubeRadius", .01);
		ap.setAttribute("pointShader.diffuseColor", new Color(102,102,102));
		ap.setAttribute("lineShader.tubeRadius", .01);
		ap.setAttribute("pointShader."+CommonAttributes.POINT_RADIUS, .01);
		if (kemper && oneTri)	{
			world.setGeometry(oneTriangle().getGeometry());
			MatrixBuilder.euclidean().rotateX(Math.PI/2).rotateZ(-Math.PI/4).assignTo(world);
			return world;
		}
		world.addChild(getInterlockedTetrahedra());
		if (allTen)	{
			SceneGraphComponent other5 = SceneGraphUtility.createFullSceneGraphComponent();
			other5.addChild(world.getChildComponent(0));
			MatrixBuilder.euclidean().scale(-1).assignTo(other5);
			world.addChild(other5);
		}
		return world;
	}

	boolean kemper = true, oneTri = false, allTen = false;
	double phi = (Math.sqrt(5.0)-1.0)/2.0;
	double phi2 = phi*phi;
	double[] axis = {phi, 0.0, 1.0};
	double radius = .10;
	double[] rot = P3.makeRotationMatrix(null, axis, Math.PI * (2.0/5.0));
	Color[] colors = {Color.RED, Color.GREEN, Color.YELLOW, new Color(255, 120,20),Color.BLUE};
	private int numCylinderSides = 4;
	private SceneGraphComponent getInterlockedTetrahedra() {
		System.err.println("x = "+(1/phi)*Math.sqrt(phi*phi*phi));
		double[] acc;
		SceneGraphComponent interlockedTetras = SceneGraphUtility.createFullSceneGraphComponent();
		acc = Rn.identityMatrix(4);
		double[] line1 = PlueckerLineGeometry.lineFromPoints(null, new double[]{1,1,1,1}, new double[]{-1,-1,1,1});
		double[] line2 = PlueckerLineGeometry.lineFromPoints(null, new double[]{-1,1,1,1}, new double[]{phi+1,0,phi,1});
		double thickness = .02;
		double r = .5 * PlueckerLineGeometry.distanceBetweenLines(line1, line2);
		r = (1-thickness/2.0)*r;
		SceneGraphComponent tetra = getTetrahedron();
//		Geometry cyl = Primitives.cylinder(numCylinderSides, 1, 1, -.5, .5, Math.PI*2, 2);
//		SceneGraphComponent sgc = Primitives.closedCylinder(numCylinderSides, 1, -.5, .5, Math.PI*2);
//		GeometryMergeFactory gmf = new GeometryMergeFactory();
//		IndexedFaceSet ifs = gmf.mergeIndexedFaceSets(sgc);
//		ThickenedSurfaceFactory tsf = new ThickenedSurfaceFactory(ifs);		// constructor requires a surface
//		tsf.setThickness(thickness);				// distance between top and bottom
//		tsf.setMakeHoles(true);				// boolean
//		tsf.setHoleFactor(.4);				// closer to 0, the bigger the hole
//		tsf.setStepsPerEdge(4);				// each original edge is replaced by 6 segments
//		tsf.setCurvedEdges(true);
//		// profile curve describes the cross-section of the hole
//		tsf.setLinearHole(false);		
//		tsf.setProfileCurve(new double[][]{{0,0}, {.5,1},{1.5, 1},{1,0}});
//		tsf.update();
//		cyl = tsf.getThickenedSurface();
		for (int i = 0; i<5; ++i)	{
			SceneGraphComponent cubekit = SceneGraphUtility.createFullSceneGraphComponent();
			cubekit.getTransformation().setMatrix(acc);
			if (kemper)	{
				cubekit.addChild(tetra); // setGeometry(cyl); //
			} else {
				BallAndStickFactory basf;
				basf = new BallAndStickFactory(Primitives.tetrahedron());
				basf.setBallColor(colors[i]);
				basf.setBallRadius(r);//(1+thickness/2.0)*r);
				basf.setStickColor(colors[i]);
				basf.setStickRadius(r);
				basf.setMetric(Pn.EUCLIDEAN);
				//			basf.setStickGeometry(cyl);
				basf.update();
				cubekit.addChild(basf.getSceneGraphComponent());
			}
			interlockedTetras.addChild(cubekit);
			Rn.times(acc,acc,rot);
		}
		return interlockedTetras;
	}

	private SceneGraphComponent getTetrahedron() {
		SceneGraphComponent collector = SceneGraphUtility.createFullSceneGraphComponent("collector");
		if (kemper) {
			IndexedFaceSetFactory ifsf = oneTriangle();
			Matrix[] mlist = new Matrix[6];
			for (int i = 0; i<6; ++i) mlist[i] = new Matrix();
			MatrixBuilder.euclidean().assignTo(mlist[0]);
			MatrixBuilder.euclidean().rotate(2*Math.PI/3.0, 1,1,1).assignTo(mlist[1]);;
			MatrixBuilder.euclidean().rotate(4*Math.PI/3.0, 1,1,1).assignTo(mlist[2]);;
			MatrixBuilder.euclidean().rotate(2*Math.PI/3.0, -1,-1,1).assignTo(mlist[3]);;
			MatrixBuilder.euclidean().rotate(4*Math.PI/3.0, -1,-1,1).assignTo(mlist[4]);;
			MatrixBuilder.euclidean().rotate(Math.PI, 1,0,0).assignTo(mlist[5]);;
			for (int i = 0; i<6; ++i)	{
				SceneGraphComponent child = new SceneGraphComponent("child");
				collector.addChild(child);
				child.setGeometry(ifsf.getGeometry());
				mlist[i].assignTo(child);
			}
			return collector;
		} 
			return collector;

	}

	private IndexedFaceSetFactory oneTriangle() {
		double k1 = 0.566915, k2 = .447, k3 = .2361, k4 = 0.105573;
		double[][] verts = {{0,0,0},{-1,-1,1},{-k1,-k1,1},{-k2,-k2,1},{-k3,-k3,1},{-k4,-k4,1},
				{k4,k4,1},{k3,k3,1},{k2,k2,1},{k1,k1,1},{1,1,1}};
		int[][] indices5 = {{0,1,2},{0,2,5},{0,5,6},{0,6,9},{0,9,10}};
		int[][] indices10 = {{0,1,2},{0,2,3},{0,3,4}, {0,4,5},{0,5,6},{0,6,7}, {0,7,8},{0,8,9},{0,9,10}};
		int[][] indices = allTen ? indices10 : indices5;
		Color[] fc5 = {Color.cyan, Color.red, Color.yellow,  Color.red,Color.cyan};
		Color[] fc10 = {Color.cyan, Color.red,Color.green, Color.blue, Color.yellow, Color.blue, Color.green, Color.red,Color.cyan};
		Color[] fc = allTen ? fc10 : fc5;
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		ifsf.setVertexCount(verts.length);
		ifsf.setVertexCoordinates(verts);
		ifsf.setFaceCount(indices.length);
		ifsf.setFaceIndices(indices);
		ifsf.setFaceColors(fc);
		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.setGenerateFaceNormals(true);
		ifsf.update();
		return ifsf;
	}

	
	@Override
	public void display() {
		// TODO Auto-generated method stub
		super.display();
		SceneGraphComponent headlight = new SceneGraphComponent();
		headlight.setLight(new DirectionalLight());
		CameraUtility.getCameraNode(jrviewer.getViewer()).addChild(headlight);
	}

	public static void main(String[] args) {
		new InterlockedTetrahedra().display();
	}
}
