package dragonfly.dynamen;

import static de.jreality.shader.CommonAttributes.ATTENUATE_POINT_SIZE;
import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.EDGE_DRAW;
import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.LINE_WIDTH;
import static de.jreality.shader.CommonAttributes.METRIC;
import static de.jreality.shader.CommonAttributes.TEXT_OFFSET;
import static de.jreality.shader.CommonAttributes.POINT_SHADER;
import static de.jreality.shader.CommonAttributes.POINT_SIZE;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TEXT_SCALE;
import static de.jreality.shader.CommonAttributes.SPHERES_DRAW;
import static de.jreality.shader.CommonAttributes.TEXT_SHADER;
import static de.jreality.shader.CommonAttributes.TUBES_DRAW;
import static de.jreality.shader.CommonAttributes.TUBE_RADIUS;
import static de.jreality.shader.CommonAttributes.VERTEX_DRAW;

import java.awt.Color;
import java.awt.Dimension;

import javax.swing.border.BevelBorder;

import charlesgunn.jreality.geometry.projective.PointCollector;
import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.jreality.texture.SimpleTextureFactory.TextureType;
import de.jreality.geometry.BezierPatchMesh;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.PointSetFactory;
import de.jreality.geometry.QuadMeshFactory;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.scene.data.StorageModel;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.Texture2D;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;
//import de.jtem.java2d.CoordinateGrid;
//import de.jtem.java2d.Viewer2D;
//import de.jtem.java2d.ViewportChangeEvent;
//import de.jtem.java2d.ViewportChangeListener;
import discreteGroup.EquidistantTextureFactory;

abstract public class ClotheIt {

	SceneGraphComponent 
	unMoved,
	  moveToCenter,
		mover,
			parent,
				velocityBody, 
					velocityLine,
				momentumBody,
					momentumPoint,
				velocityCurveSGC, 
				velocityCurveWorldSGC,
				positionCurveSGC,
			object, 
	  hyperbolicBoundary, 
		equiDistTex;
	EquidistantTextureFactory etf;
	IndexedLineSet amb = null;
	PointSet avb = null, ampb = null;
	PointCollector velocityCurve, velocityCurveWorld, pointCurve;
	
//	PointRangeFactory momentumFactory;
	Geometry momentumGeometry;
	PointSetFactory velocityFactory, momentumBodyFactory;

	RigidBodyInterface dynamics;
	boolean doLabels = false;
	boolean improperMomentum = false, showHerpolhode = false;
	int metric = Pn.EUCLIDEAN;
	int snakeLength = 10000;
	double[] currentPosition;
//	Viewer2D viewer2d;
	protected Color momVecArrowColor = new Color(50,255,200), 
			momVecStickColor = new Color(50,255,50),
//			velColor = new Color(0,100,255),
			momColor = new Color(255,50,0),
			polhodeColor = new Color(100,255,255),
			herpolhodeColor = Color.green;
	private Color momentumColor = new Color(250, 0, 250);
	private Color velColor = herpolhodeColor;
	private Color velWColor = new Color(255, 150,150);
	private Color positionColor = polhodeColor; //new Color(80, 150, 150);
	boolean moveToCenterB = true;
	
	public ClotheIt(RigidBodyInterface d)	{
		dynamics = d;
		
		createSceneGraphRepn();
//		viewer2d = getViewer2D();
	}
	public SceneGraphComponent getBodyRepn() {
		return object;
	}

	public void setBodyRepn(SceneGraphComponent sgc)	{
		SceneGraphUtility.removeChildren(object);
		object.addChild(sgc);
	}
	
	abstract double[] convertPoint(double[] p);
	abstract SceneGraphComponent getMomentumGeometry();
	abstract double[] correctionMatrix();
	
	transient double[] momentum, velocity, motion;
	static double[] np = {0,0,1,1};
	public void updateSceneGraphRepn() {
		velocity = dynamics.getVelocity();
		momentum = dynamics.getMomentum();
//		System.err.println("clotheit mom = "+Rn.toString(momentum));
		motion = dynamics.getMotionMatrix();
		currentPosition = new double[]{motion[3], motion[7],motion[15]};
		pointCurve.addPoint(convertPoint(currentPosition));
		equiDistTex.setVisible(!improperMomentum);
	}
	protected void handleVelocityInBody(double[] v) {
		velocityCurve.addPoint(v);
		velocityCurveWorld.addPoint(Rn.matrixTimesVector(null, motion, v));
		if (velocity[2] != 0)	{
			velocityBody.setVisible(true);
			Pn.dehomogenize(v, v);
			velocityFactory.setVertexCoordinates(v);
			velocityFactory.update();
			MatrixBuilder.euclidean().times(motion).assignTo(velocityBody);
			if (doLabels && avb == null) velocityFactory.getPointSet().setVertexAttributes(Attribute.LABELS, StorageModel.STRING_ARRAY.createReadOnly(
					new String[]{"velocity state"}));
		} 
		else velocityBody.setVisible(false);
	}

	public SceneGraphComponent fullSceneGraph() {
		return unMoved;

	}
	
	protected void createSceneGraphRepn() {
		unMoved = SceneGraphUtility.createFullSceneGraphComponent("unmoved");
		moveToCenter = SceneGraphUtility.createFullSceneGraphComponent("moveToCenter");
		parent = SceneGraphUtility.createFullSceneGraphComponent("body");
		velocityBody = SceneGraphUtility.createFullSceneGraphComponent("velBody");
		momentumBody = SceneGraphUtility.createFullSceneGraphComponent("momBody");
		momentumPoint = SceneGraphUtility.createFullSceneGraphComponent("momBodyPoint");
		momentumBody.addChild(momentumPoint);
		Appearance ap = unMoved.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white);
		ap.setAttribute(CommonAttributes.LINE_WIDTH,2.5);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+DIFFUSE_COLOR, velBodyColor);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_OFFSET, new double[]{.1,0,0.0});
//		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+SCALE, .006);
		velocityBody.getAppearance().setAttribute(METRIC, Pn.EUCLIDEAN);
		velocityBody.getAppearance().setAttribute(VERTEX_DRAW, true);
		velocityBody.getAppearance().setAttribute(EDGE_DRAW, true);
		velocityBody.getAppearance().setAttribute(POINT_SHADER+"."+SPHERES_DRAW, true);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
//		velocityBody.getAppearance().setAttribute(POINT_SHADER+"."+POINT_RADIUS, .01);
		velocityBody.getAppearance().setAttribute(POINT_SHADER+"."+POINT_SIZE, 5.0);
		velocityBody.getAppearance().setAttribute(POINT_SHADER+"."+ATTENUATE_POINT_SIZE, false);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBE_RADIUS, .02);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, velBodyColor);
		velocityBody.getAppearance().setAttribute(POINT_SHADER+"."+DIFFUSE_COLOR, velBodyColor);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, velBodyColor);
		velocityFactory = new PointSetFactory();
		velocityFactory.setVertexCount(1);

		avb = velocityFactory.getPointSet();			
		velocityBody.setGeometry(avb);
		avb.setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+DIFFUSE_COLOR, momentumColor);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_OFFSET, new double[]{2,0.2,2.2});
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_SCALE, .008);
		momentumBody.getAppearance().setAttribute(EDGE_DRAW, true);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+LINE_WIDTH, 2);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, momentumColor);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, momentumColor);
		momentumPoint.getAppearance().setAttribute(VERTEX_DRAW, true);
		momentumPoint.getAppearance().setAttribute(POINT_SHADER+"."+SPHERES_DRAW, true);
		momentumPoint.getAppearance().setAttribute(POINT_SHADER+"."+ATTENUATE_POINT_SIZE, false);
		momentumPoint.getAppearance().setAttribute(POINT_SHADER+"."+POINT_SIZE, 5.0);
		momentumPoint.getAppearance().setAttribute(POINT_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, momentumColor);
		momentumPoint.getAppearance().setAttribute(POINT_SHADER+"."+DIFFUSE_COLOR, momentumColor);

		getMomentumGeometry();
		
		velocityCurve = new PointCollector(snakeLength, 4);
		velocityCurveWorld = new PointCollector(snakeLength, 4);
		pointCurve = new PointCollector(snakeLength, 4);
		mover = SceneGraphUtility.createFullSceneGraphComponent("mover");
		mover.getAppearance().setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		mover.getAppearance().setAttribute(METRIC, Pn.EUCLIDEAN);
		
		velocityCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("velocityCurve");
		velocityCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		velocityCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, velColor);
		velocityCurveSGC.setGeometry(velocityCurve.getCurve());
		velocityCurveWorldSGC = SceneGraphUtility.createFullSceneGraphComponent("velocityCurveWorld");
		velocityCurveWorldSGC.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		velocityCurveWorldSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, velWColor);
		velocityCurveWorldSGC.setGeometry(velocityCurveWorld.getCurve());
		velocityCurveWorldSGC.setVisible(showHerpolhode);
		positionCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("positionCurve");
		positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, positionColor);
		positionCurveSGC.setGeometry(pointCurve.getCurve());
		hyperbolicBoundary = SceneGraphUtility.createFullSceneGraphComponent("boundary");
		hyperbolicBoundary.getAppearance().setAttribute("metric", Pn.EUCLIDEAN);
		hyperbolicBoundary.getAppearance().setAttribute("lineShader."+TUBES_DRAW,false);
		hyperbolicBoundary.getAppearance().setAttribute("lineShader.diffuseColor",Color.WHITE);
		hyperbolicBoundary.getAppearance().setAttribute("lineShader.lineWidth",2);
		hyperbolicBoundary.setGeometry(IndexedLineSetUtility.circle(100)); 
		hyperbolicBoundary.setVisible(metric == Pn.HYPERBOLIC);
		etf = new EquidistantTextureFactory();
		etf.update();
		hyperbolicBoundary.addChild(equiDistTex = etf.getSceneGraphComponent());
		equiDistTex.getAppearance().setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		MatrixBuilder.euclidean().translate(0,0,-.005).assignTo(equiDistTex);
		
		parent.addChildren(velocityBody, 
				momentumBody, 
				velocityCurveSGC, 
				velocityCurveWorldSGC, 
				positionCurveSGC);
		SceneGraphComponent rawBody = getDefaultBodyRepn();
		object = new SceneGraphComponent("the body repn wrapper");
		object.addChild(rawBody);
		mover.addChildren(object, parent);
		updateSceneGraphRepn();
		unMoved = SceneGraphUtility.createFullSceneGraphComponent("unMoved");
		moveToCenter = SceneGraphUtility.createFullSceneGraphComponent("moveToCenter");
		moveToCenter.addChildren(mover);
		unMoved.addChildren(moveToCenter, hyperbolicBoundary);
	}
	private static double[][][] defaultPoints = {{{0,0,0,1}, {1,0,0,1}},  {{0,1,0,1},  {1,1,0,1}}};
	//protected IndexedFaceSetFactory bodyFactory = Primitives.texturedQuadrilateralFactory(defaultPoints);
	QuadMeshFactory bodyFactory = null;
	Matrix defaultBodyRepnMatrix = new Matrix();
	double[][] origverts  = null;
	private Color velBodyColor = new Color(0, 255,0);
	protected SceneGraphComponent getDefaultBodyRepn() {
		Appearance ap;
		SceneGraphComponent rawBody = SceneGraphUtility.createFullSceneGraphComponent("body repn");
		// render inertia ellipsoid
		BezierPatchMesh bodyrep = new BezierPatchMesh(1, 1, defaultPoints);
		bodyrep.refine(); bodyrep.refine();
		bodyFactory = BezierPatchMesh.representBezierPatchMeshAsQuadMesh(bodyrep, Pn.EUCLIDEAN);
		origverts = bodyFactory.getIndexedFaceSet().getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		rawBody.setGeometry(bodyFactory.getGeometry()); //regularPolygon(30));
		rawBody.getAppearance().setAttribute(VERTEX_DRAW, false);
		rawBody.getAppearance().setAttribute(EDGE_DRAW, false);
		rawBody.getAppearance().setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white );
		MatrixBuilder.euclidean().scale(.15).translate(-1,-1,0).scale(2).assignTo(defaultBodyRepnMatrix);
		defaultBodyRepnMatrix.assignTo(rawBody);
		ap = rawBody.getAppearance();
		Texture2D tex2d = (Texture2D) AttributeEntityUtility
		   .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", ap, true);		
		SimpleTextureFactory stf = new SimpleTextureFactory();
		stf.setType(TextureType.CHECKERBOARD);
		stf.setSize(8);
		stf.setColor(0, Color.blue); //Color.red);
		stf.setColor(1, Color.yellow);
		stf.update();
		tex2d.setImage(stf.getImageData());
		Matrix foo = new Matrix();
		MatrixBuilder.euclidean().scale(4,4,1).assignTo(foo);
		tex2d.setTextureMatrix(foo);
		ap.setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		return rawBody;
	}

	public int getMetric() {
		return metric;
	}
	
	public void setMetric(int metric) {
		this.metric = metric;
		hyperbolicBoundary.setVisible(metric == Pn.HYPERBOLIC);
	}

	public void reset()	{
		new Matrix().assignTo(moveToCenter);
		velocityCurve.reset();
		velocityCurveWorld.reset();
		pointCurve.reset();
		new Matrix().assignTo(velocityCurveSGC);
		if (moveToCenterB && metric ==Pn.HYPERBOLIC) { //!= Pn.EUCLIDEAN) { //
			// apply transformation to moveToCenter node so that the momentum appears in a 
			// generic position
			double d = Pn.innerProduct(momentum, momentum, metric);
			if (Math.abs(d) < 10E-3) return;		// almost ideal, too risky
			double[] point = null;		// this point will be sent to the origin
			if (d < 0) {		// improper line, move to infinity
				point = Pn.polarize(null,momentum, metric);
				improperMomentum = true;
			} else {			// otherwise take the point on the mom line closest to origin
				point = new double[]{0, -momentum[2], momentum[1]};
				improperMomentum = false;
			}
			double[] point4 = new double[]{point[0], point[1], 0, point[2]};
			if (point4[3] < 0) Rn.times(point4, -1, point4);
			System.err.println("point = "+Rn.toString(point4));
			//if (metric == Pn.HYPERBOLIC) 
				MatrixBuilder.init(null, metric).translate(point4, P3.originP3).assignTo(moveToCenter);
			//else  MatrixBuilder.init(null, metric).rotateZ(Math.PI).translate(point4, P3.originP3).assignTo(moveToCenter);
		} 
		updateSceneGraphRepn();
	}

	protected double[] getDual(double[] mat)	{
		double[] motionDual = Rn.transpose(null, Rn.inverse(null, mat ));
		if (metric == Pn.EUCLIDEAN) {motionDual[12] = motionDual[13] = motionDual[14] = 0.0; }
		return motionDual;
	}


}
