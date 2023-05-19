/*
 * Created on 12 May 2023
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;

import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.jreality.viewer.Assignment;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.geometry.ParametricSurfaceFactory.Immersion;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.tools.DragEventTool;
import de.jreality.tools.DraggingTool;
import de.jreality.tools.PointDragEvent;
import de.jreality.tools.PointDragListener;
import de.jreality.tools.RotateTool;
import de.jreality.util.CopyVisitor;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.util.WingedEdge;
import de.jtem.discretegroup.util.WingedEdgeUtility;

public class ParaboloidPolarity extends Assignment {

	SceneGraphComponent 
		world = SceneGraphUtility.createFullSceneGraphComponent("world"),
			formSGC = SceneGraphUtility.createFullSceneGraphComponent("form"),
			forceSGC = SceneGraphUtility.createFullSceneGraphComponent("force"),
			proj2D = SceneGraphUtility.createFullSceneGraphComponent("2D proj"),
				csSGC = SceneGraphUtility.createFullSceneGraphComponent("coord sys"),
				shadFormSGC = SceneGraphUtility.createFullSceneGraphComponent("shadow form"),
				shadForceSGC = SceneGraphUtility.createFullSceneGraphComponent("shadow force"),
			paraSGC = SceneGraphUtility.createFullSceneGraphComponent("parab"),
				ptOnParaSGC = SceneGraphUtility.createFullSceneGraphComponent("point on parab");
	IndexedFaceSetFactory formFactory = new IndexedFaceSetFactory();
	IndexedFaceSetFactory forceFactory = new IndexedFaceSetFactory();
	
	double sq3 = Math.sqrt(3.0)/2.0;
	double k = .1, xk = .5;
	static double k2 = 1.0;
	boolean tformChanged = false;
	double[][] points = {{1+xk,0,k,1},{-.5+xk, sq3, 2*k, 1}, {-.5+xk, -sq3, 3*k, 1}, {xk,0,-k,1}};
	int[][] fInd = {{0,1,2},{1,0,3}, {2,1,3},{0,2,3}};
	int[][] vInd = {{1,3,0},{2,1,0},{3,2,0},{1,2,3}};
	
	Color[] vCol = {Color.orange, Color.green, Color.yellow, Color.gray};
	Color[] fCol = {Color.red, Color.blue, Color.cyan, Color.magenta};
	static Paraboloid parab = new Paraboloid();
	
	@Override
	public SceneGraphComponent getContent() {
		formFactory.setVertexCount(points.length);
		formFactory.setVertexCoordinates(points);
		formFactory.setVertexColors(vCol);
		formFactory.setFaceCount(fInd.length);
		formFactory.setFaceIndices(fInd);
		formFactory.setFaceColors(fCol);
		formFactory.setGenerateEdgesFromFaces(true);
		formFactory.setGenerateFaceNormals(true);
		formFactory.update();
		formSGC.setGeometry(formFactory.getGeometry());
//		formSGC.addTool(new DraggingTool());
//		formSGC.addTool(new RotateTool());
		
		DragEventTool t = new DragEventTool();
		t.addPointDragListener(new PointDragListener() {

			public void pointDragStart(PointDragEvent e) {
				System.err.println("drag start of vertex no "+e.getIndex());				
			}

			public void pointDragged(PointDragEvent e) {
				System.err.println("dragging e-coord "+e.getPosition()[2]);				
				PointSet pointSet = e.getPointSet();
				double[][] ppp =new double[pointSet.getNumPoints()][];
		        double[] ptnew =e.getPosition(),
		        		pval = new double[3];
		        parab.evaluate(ptnew[0], ptnew[1], pval, 0);
		        ppp[e.getIndex()]= new double[]{ptnew[0], ptnew[1], pval[2], 1};
		        pointSet.setVertexAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY.array(4).createReadOnly(ppp));	
		        update();
			}

			public void pointDragEnd(PointDragEvent e) {
			}
			
		});
		formSGC.addTool(t);
		
		WingedEdge we = WingedEdgeUtility.convertConvexPolyhedronToWingedEdge(formFactory.getIndexedFaceSet());
//		we.setMetric(Pn.ELLIPTIC);
//		WingedEdge polar = we.polarize(1.0);
//		forceSGC.setGeometry(polar);
		// apply polarity to the points
		forceFactory.setVertexCount(fInd.length);
		forceFactory.setVertexColors(fCol);
		forceFactory.setFaceCount(points.length);
		forceFactory.setFaceIndices(vInd);
		forceFactory.setFaceColors(vCol);
		forceFactory.setGenerateEdgesFromFaces(true);
		forceFactory.setGenerateFaceNormals(true);
		update();
		forceSGC.setGeometry(forceFactory.getIndexedFaceSet());
		forceFactory.update();
		
//		formSGC.getTransformation().addTransformationListener(new TransformationListener() {
//			
//			@Override
//			public void transformationMatrixChanged(TransformationEvent ev) {
//				update();
//			}
//		});
		
//		CoordinateSystemFactory csf = new CoordinateSystemFactory(2, .1);
//		csSGC.addChild(csf.getCoordinateSystem());
//		csSGC.setVisible(false);
		
		initProj2D();
		updateProj2D();
		
		paraSGC.setGeometry(paraboloid());
		Appearance ap  = paraSGC.getAppearance();
		ap.setAttribute(CommonAttributes.SMOOTH_SHADING, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		paraSGC.addChild(ptOnParaSGC);
		ap  = ptOnParaSGC.getAppearance();
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute(CommonAttributes.POINT_RADIUS, .02);
		ap.setAttribute("pointShader.diffuseColor", Color.RED);
		DragEventTool p = new DragEventTool();
		p.addPointDragListener(new PointDragListener() {

			public void pointDragStart(PointDragEvent e) {
				System.err.println("drag start of vertex no "+e.getIndex());				
			}

			public void pointDragged(PointDragEvent e) {
				PointSet pointSet = e.getPointSet();
				double[][] ppp =new double[pointSet.getNumPoints()][];
		        pointSet.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(ppp);
		        ppp[e.getIndex()]= getClosestPoint(e.getPosition()); //new double[]{ptold[0], ptold[1], ptnew[2], ptold[3]};
		        pointSet.setVertexAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY.array(4).createReadOnly(ppp));	
		        update();
			}

			public void pointDragEnd(PointDragEvent e) {
			}
			
		});
		ptOnParaSGC.addTool(p);

		double[] pt = {0,0,0,1};
		parab.evaluate(0,-.5, pt, 0);
		ptOnParaSGC.setGeometry(Primitives.point(pt));
		world.addChildren(formSGC, forceSGC, paraSGC, proj2D);
		ap = world.getAppearance();
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute(CommonAttributes.SMOOTH_SHADING, false);
		ap.setAttribute("lineShader."+CommonAttributes.TUBE_RADIUS,.005);
		ap.setAttribute("polygonShader.diffuseColor", Color.white);
		ap.setAttribute("lineShader.diffuseColor", Color.white);
		ap.setAttribute("pointShader.diffuseColor", Color.white);
		return world;
	}
	
	static double tol = 10E-4;
	protected double[] getClosestPoint(double[] position) {
		if (position.length == 3) position = Pn.homogenize(null, position);
		else position = Pn.dehomogenize(null, position);
		double[] ppl = Pn.normalizePlane(null, polarize(position),0),
				normal = {ppl[0], ppl[1], ppl[2], 0},
				P2 = new double[4];
		double k1 = Rn.innerProduct(position, ppl),
				k3 = Rn.innerProduct(normal, ppl),
				t = -k1/k3;
		// calculate the closest point on the polar plane to the position 
		P2 = Rn.add(null, position, Rn.times(null, t, normal));
		System.err.println("eval map = "+Rn.innerProduct(P2, ppl));				
		System.err.println("position = "+Rn.toString(position));				
		System.err.println("P2 = "+Rn.toString(P2));				
		// we want to march back and forth on the segment P- until 
		// we are on near the paraboloid
		int count = 0;
		double[] mp = null,
				left = position,
				right = P2;
		double sL = Math.signum(evaluate(left)),
				sR = Math.signum(evaluate(right));
		do  // binary search! always keep left and right on opposite sides of the surface
		{   mp = Rn.times(null, .5, Rn.add(null, left, right));
			t = evaluate(mp);
			double sM = Math.signum(t);
			if (sL*sM < 0) right = mp;
			else left = mp;                             
			System.err.println("evaluate "+t);				
		}
		while (Math.abs(t) > tol && count++ < 24);
		System.err.println("mp = "+Rn.toString(mp));				
		return mp;
	}

	private void initProj2D() {
		csSGC.setGeometry(Primitives.texturedQuadrilateral());
		MatrixBuilder.euclidean().scale(16,16,1).translate(-.5,-.5,-.01).assignTo(csSGC);
		Appearance ap = csSGC.getAppearance();
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		SimpleTextureFactory stf = new SimpleTextureFactory();
		stf.setType(SimpleTextureFactory.TextureType.GRAPH_PAPER); // LINE); //
		stf.setColor(1, new Color(100,100,100));
		stf.setColor(0, new Color(250,250,250));
		stf.setColor(2, new Color(140,140,140));
		stf.setColor(3, new Color(250,250,250));
		stf.setSize(512);
		stf.setAppearance(ap);
		stf.update();
		
		Texture2D tex2d = TextureUtility.createTexture(ap, "polygonShader", stf.getImageData());
		Matrix foo = new Matrix();
		MatrixBuilder.euclidean().scale(40,40, 1).assignTo(foo);
		tex2d.setTextureMatrix(foo);

		proj2D.addChildren(shadForceSGC, shadFormSGC, csSGC);
		MatrixBuilder.euclidean().translate(0,0,-3).assignTo(proj2D);
		ap = proj2D.getAppearance();
		ap.setAttribute("lineShader.tubeRadius", .01);
		ap = shadFormSGC.getAppearance();
//		shadFormSGC.setAppearance(ap);
		ap.setAttribute(CommonAttributes.FACE_DRAW, false);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute("lineShader.diffuseColor", Color.black);
		ap = shadForceSGC.getAppearance();
//		shadFormSGC.setAppearance(ap);
		ap.setAttribute(CommonAttributes.FACE_DRAW, false);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute("lineShader.diffuseColor", Color.blue);
		ap.setAttribute("lineShader.tubeRadius", .01);

	}
	final double[] projZ = {1,0,0,0,   0,1,0,0,   0,0,0,0,   0,0,0,1};
	private void updateProj2D() {
		

		CopyVisitor cv = new CopyVisitor();
		cv.visit(formFactory.getIndexedFaceSet());
		IndexedFaceSet copy = (IndexedFaceSet) cv.getCopy();
		double[][] pts = copy.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		double[] moomoo = formSGC.getTransformation().getMatrix();
		double[][] tpts = Rn.matrixTimesVector(null, moomoo, pts);
		Rn.matrixTimesVector(tpts, projZ, tpts);
		copy.setVertexAttributes(Attribute.COORDINATES, StorageModel.STRING_ARRAY_ARRAY.toDoubleArrayArray(tpts));
		shadFormSGC.setGeometry(copy);
		
		cv.visit(forceFactory.getIndexedFaceSet());
		copy = (IndexedFaceSet) cv.getCopy();
		pts = copy.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		Rn.matrixTimesVector(pts, projZ, pts);
		copy.setVertexAttributes(Attribute.COORDINATES, StorageModel.STRING_ARRAY_ARRAY.toDoubleArrayArray(pts));
		shadForceSGC.setGeometry(copy);
	}

	
	public  static class Paraboloid implements Immersion {
		public void evaluate(double u, double v, double[] xyz, int index) {
			// the paraboloid  z = k2*(x^2+y^2)
			xyz[index]= u;
			xyz[index+1]= v;
			xyz[index+2]= k2*(u*u + v*v);
		}
		// how many dimensions in the image space?
		public int getDimensionOfAmbientSpace() { return 3;	}
		// Does evaluate() always put the same value into xyz for a given pair (u,v)?
		// If the immersion has parameters that affect the result of evaluate() then isImmutable()
		// should return false.
		public boolean isImmutable() { return true; }
	};
	

	public Geometry paraboloid() {

			//initialize the parametric surface factory
			final ParametricSurfaceFactory psf = new ParametricSurfaceFactory(parab);
			//uv-extension of the domain
			psf.setUMin(-1);psf.setUMax(1);psf.setVMin(-1);psf.setVMax(1);
			//subdivisions of th domain
			psf.setULineCount(20);psf.setVLineCount(20);
			//generate edges and normals
			psf.setGenerateEdgesFromFaces(true);
			psf.setGenerateVertexNormals(true);
			//generate the IndexFaceSet
			psf.update();
			return psf.getGeometry();

	}
	private void update() {
		double[][] pts = formFactory.getIndexedFaceSet().getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		double[] moomoo = formSGC.getTransformation().getMatrix();
		double[][] tpts = Rn.matrixTimesVector(null, moomoo, pts);
		double[][] pplns = new double[tpts.length][],
				ppts = new double[fInd.length][];
		for (int i = 0; i<pts.length; ++i)	{
			pplns[i] = polarize(tpts[i]);
		}
		for (int i = 0; i<fInd.length; ++i) {
			int[] f = fInd[i];
			ppts[i] = P3.pointFromPlanes(null, pplns[f[0]], pplns[f[1]], pplns[f[2]]);
		}
		Pn.dehomogenize(ppts, ppts);
		forceFactory.setVertexCoordinates(ppts);
		forceFactory.update();
		updateProj2D();
	}
	// fill one slot of the matrix to create the polar plane of a point
	// in explicit form this is x^2 + y*2 = k2*z   (k2!= 0)
	private double[] polarize(double[] pt) {
		return new double[] {(1/k2)*pt[0], (1/k2)*pt[1],  -.5*pt[3], -.5*pt[2]};
	}

	// the value of the QF at a point is given by <P, polar(P)> 
	private double evaluate(double[] pt) {
		return Rn.innerProduct(pt, polarize(pt));
	}

	public static void main(String[] args) {
		new ParaboloidPolarity().display();
	}

}
