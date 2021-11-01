package dragonfly.dynamen;

import static de.jreality.shader.CommonAttributes.AMBIENT_COEFFICIENT;
import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.EDGE_DRAW;
import static de.jreality.shader.CommonAttributes.LIGHTING_ENABLED;
import static de.jreality.shader.CommonAttributes.LINE_LIGHTING_ENABLED;
import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.METRIC;
import static de.jreality.shader.CommonAttributes.TEXT_OFFSET;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TEXT_SCALE;
import static de.jreality.shader.CommonAttributes.TEXT_SHADER;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY_ENABLED;
import static de.jreality.shader.CommonAttributes.TUBES_DRAW;
import static de.jreality.shader.CommonAttributes.TUBE_RADIUS;
import static de.jreality.shader.CommonAttributes.VERTEX_DRAW;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import charlesgunn.jreality.GeometryCollector;
import charlesgunn.jreality.geometry.projective.PointCollector;
import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.math.Biquaternion;
import charlesgunn.math.Biquaternion.Metric;
import charlesgunn.math.p5.PlueckerLineGeometry;
import charlesgunn.math.BiquaternionUtility;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.FrameFieldType;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.PolygonalTubeFactory;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.SphereUtility;
import de.jreality.geometry.TubeFactory;
import de.jreality.geometry.TubeUtility.FrameInfo;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

/**
 * TODO: 
 * 	show axis pairs with icons representing pitch
 * in elliptic case, calculate 4 body repn's: in each another coordinate is the "homogeneous" coordinate, and show
 * them all? or just one at a time?
 * 
 */
public class ClotheIt3D {
	SceneGraphComponent 
		moverSGC,
			parent,
				velocityBody,
					vBodyPolar,
				momentumBody,
					mBodyPolar,
			object, 
			body, 
			hyperbolicBoundary, 
			velocityCurveSGC, 
			positionCurveSGC,
			positionCurveListSGC[] = new SceneGraphComponent[4];
	IndexedLineSet amb = null;
	PointSet avb = null;
	PointCollector pointCurve[] = new PointCollector[4];
	List<double[]> handfulOfPoints = new ArrayList<double[]>();
//	CurveCollector velMesh;
//	PointRangeFactory momentumFactory;
	PointRangeFactory velFactory, momFactory, velPolarFactory, momPolarFactory;	
	RigidBody3D dynamics;
	boolean doLabels = false;
	int metric = Pn.EUCLIDEAN;
	int positionHistory = 100000, velocityHistory = 10000;
	GeometryCollector geomCollector = new GeometryCollector(velocityHistory);
	int velocityBucketSize = 25, collectionCount = 0;
	int pathBucketSize = 20;
	int stepcount = 0;
	int samplesPerLine = 12;
	int whichCenterOfMass = 3;
	double axisLineWidth = 2.0;
	double globalBoxScale = .1;
	boolean finiteSphere = false;
	boolean showPosition = true, lineShading = false;
	public static enum VelocityDisplay  {LINES, BAND, NONE};
	VelocityDisplay velDisplay = VelocityDisplay.NONE;
	double sphereRadius = 1.0;
	double stripWidth = .5;
	double sphereTransp = .3, greySphere = .75;
	transient double[] momentum, velocity, motion;
	transient double[][] velseg = new double[2][4];
	IndexedLineSet velILS;
	Biquaternion lastVelocity;
	static double[] np = {0,0,1,1};
	TestRigidBody3D t3d;
	
	public ClotheIt3D(RigidBody3D d, TestRigidBody3D t3d)	{
		dynamics = d;		
		this.t3d = t3d;
		createSceneGraphRepn();
	}
	
	public ClotheIt3D(RigidBody3D d)	{
		this(d, null);
	}
	
	public SceneGraphComponent getBodyRepn() {
		return object;
	}

	public void setBodyRepn(SceneGraphComponent sgc)	{
		SceneGraphUtility.removeChildren(object);
		object.addChild(sgc);
	}
		

	public SceneGraphComponent fullSceneGraph() {
		return moverSGC;

	}
	
	boolean velDirOnly = false;
	private TextSlider pcountGUI, vcountGUI;
	protected void updateSceneGraphRepn() {
		velocity = dynamics.getVelocity();
		Biquaternion bivel = new Biquaternion(velocity, Metric.metricForCurvature(metric));
		Biquaternion bivelaxis = Biquaternion.axisForBivector(null, bivel);
		double[] velaxis = Biquaternion.lineComplexFromBivector(null, bivelaxis);//axis);
		if (velDirOnly) {
			//velaxis[0] = velaxis[5]; velaxis[1] = velaxis[4]; velaxis[2] = velaxis[3];
			velaxis[3] = velaxis[4] = velaxis[5] = 0;
		}
		double[] vv  = PlueckerLineGeometry.permuteCoordinates(null, velaxis, new int[]{3,0,1,2});
		Geometry velGeometry = null;
		velFactory.setPluckerLine(vv);
		velFactory.update();
		if (metric != Pn.EUCLIDEAN) {
			Biquaternion velpolar = Biquaternion.polarize(null, bivelaxis);
			velaxis = Biquaternion.lineComplexFromBivector(null, velpolar);
			double[] pvv  =  PlueckerLineGeometry.permuteCoordinates(null, velaxis, new int[]{3,0,1,2});
			velPolarFactory.setPluckerLine(pvv);
			velPolarFactory.update();			
		}
		// following code handles collecting velocity states
		if (velDisplay != VelocityDisplay.NONE && collectionCount % velocityBucketSize == 0 )	{
			if (velDisplay == VelocityDisplay.LINES)	{
				PointRangeFactory localVelFactory = new PointRangeFactory();
				localVelFactory.setFiniteSphere(finiteSphere);
				localVelFactory.setSphereRadius(sphereRadius);
				localVelFactory.setNumberOfSamples(samplesPerLine);	
				localVelFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);			
				localVelFactory.setPluckerLine(vv);
				localVelFactory.update();
				velGeometry = localVelFactory.getLine();
			} else {
				if (lastVelocity != null)	{
					Biquaternion commonNormal = Biquaternion.commutator(null, bivelaxis, lastVelocity);
					double[] cnpl = Biquaternion.lineComplexFromBivector(null, commonNormal);
					double[] velpl = Biquaternion.lineComplexFromBivector(null, bivelaxis);
					if (PlueckerLineGeometry.isValidLine(cnpl)) {
						Rn.setToLength(cnpl, cnpl, 1.0);
						double[] cc = PlueckerLineGeometry.intersectionPoint(null, cnpl, velpl);
//						closestPoint = new double[]{cc[1], cc[2], cc[3], cc[0]};
//						pointCurve.addPoint(closestPoint); //currentPosition); //
//						System.err.println("Closest point = "+Rn.toString(closestPoint));	
						BiquaternionUtility.lineSegmentCenteredOnPoint(velseg, bivelaxis, cc, stripWidth);
						convertWXYZ2XYZW(velseg[0]); convertWXYZ2XYZW(velseg[1]);
						velILS = IndexedLineSetUtility.createCurveFromPoints(null, velseg, false);
						velGeometry = velILS;
					}
				}	else velGeometry = null;
			}
			if (velGeometry != null)	
				geomCollector.addGeometry(velGeometry);
		}
		collectionCount++;

		// handle the momentum
		momentum = dynamics.getMomentum();
		Biquaternion bimom = new Biquaternion(momentum, Metric.metricForCurvature(metric));
		Biquaternion bimomaxis = Biquaternion.axisForBivector(null, bimom);
		double[] momaxis = Biquaternion.lineComplexFromBivector(null, bimomaxis);
		double[] mm  = PlueckerLineGeometry.permuteCoordinates(null, momaxis, new int[]{3,0,1,2});
//		System.err.println("clotheit mom = "+Rn.toString(mm));
		momFactory.setPluckerLine(mm);
		momFactory.update();
		if (metric != Pn.EUCLIDEAN) {
			Biquaternion bimompolaraxis = Biquaternion.polarize(null, bimomaxis);
			momaxis = Biquaternion.lineComplexFromBivector(null, bimompolaraxis);
			mm  = PlueckerLineGeometry.permuteCoordinates(null, momaxis, new int[]{3,0,1,2});
			momPolarFactory.setPluckerLine(mm);
			momPolarFactory.update();
		}
//		System.err.println("clotheit vel = "+Rn.toString(vv));

		motion = dynamics.getMotionMatrix();
//		System.err.println("Motion = "+Rn.matrixToString(motion));
		updateBoxRepn();
		
		//MatrixBuilder.euclidean().times(motion).scale(.3).assignTo(object);
//		System.err.println("object mat = "+Rn.matrixToString(object.getTransformation().getMatrix()));
		for (int i =0; i<4; ++i) {
			double[] currentPosition = new double[]{motion[0+i], 
					motion[4 + i], motion[8+i], motion[12 + i]};
			pointCurve[i].addPoint(currentPosition);
		}
//		System.err.println("current pos = "+Rn.toString(currentPosition));
//		double[] cp = Rn.setToValue(null, currentPosition[3], currentPosition[0], currentPosition[1], currentPosition[0]);
//		cp = BiquaternionUtility.projectPointOntoLine(null, 
//				new double[]{1,0,0,0}, 
//				bivelaxis);
//		double[] closestPoint = Rn.setToValue(null, cp[1], cp[2], cp[3], cp[0]);
//		closestPoint = Pn.dehomogenize(null,closestPoint); //Rn.matrixTimesVector(null, motion, closestPoint));

//		velMesh.addCurve(velFactory.getVertices());
		Matrix m = new Matrix(motion);
		m.assignTo(velocityBody);
		m.assignTo(momentumBody);
		m.assignTo(velocityCurveSGC);			

		lastVelocity = bivelaxis;
		
		int count = pointCurve[whichCenterOfMass].getCount();
		if ((count % 100) == 0) pcountGUI.setValue(count);
		count = geomCollector.getCount();
		if ((count % 100) == 0) vcountGUI.setValue(count);
		
	}

	private void updateBoxRepn() {
		double[] d = dynamics.getDims();
//		System.err.println("Dims = "+Rn.toString(d));
		double[] scale = {d[0], d[1], d[2], d[3]};
		MatrixBuilder.euclidean().scale(scale).scale(.5).assignTo(box);
		
		double E = dynamics.getEnergy();
		double scaler = Math.sqrt(E);
//		System.err.println("scale = "+scaler);
		double[] moments = dynamics.getMoments();
		double m1 = moments[0], m2 = moments[1], m3 = moments[2];	
		double p1 = d[0]*d[1]*d[2], p2 = Math.sqrt((m1*m2*m3)); // Math.sqrt(1.0/(m1*m2*m3));
		scaler = Math.pow(p1/p2, 1.0/3.0)/d[3];
		double[] ellipsoidScale = {Math.sqrt(m1),Math.sqrt(m2),Math.sqrt(m3)};
		//scale = Math.sqrt(d);  //scale(ellipsoidScale).
		MatrixBuilder.euclidean().scale(scaler).assignTo(ellipsoid);
//		box.setVisible(!showEllipsoid);
		ellipsoid.setVisible(showEllipsoid);
		MatrixBuilder.euclidean().times(motion).scale(globalBoxScale).assignTo(object);
	}

	private void convertWXYZ2XYZW(double[] pt)	{
		double tmp = pt[0];
		for (int i = 0; i<3;++i)		pt[i] = pt[i+1];
		pt[3] = tmp;
	}
	protected void createSceneGraphRepn() {
		velocityBody = SceneGraphUtility.createFullSceneGraphComponent("velBody");
		momentumBody = SceneGraphUtility.createFullSceneGraphComponent("momBody");
		vBodyPolar = SceneGraphUtility.createFullSceneGraphComponent("velBody");
		mBodyPolar = SceneGraphUtility.createFullSceneGraphComponent("momBody");
		velocityBody.addChild(vBodyPolar);
		momentumBody.addChild(mBodyPolar);
		parent = SceneGraphUtility.createFullSceneGraphComponent("body");
		Appearance ap = parent.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+DIFFUSE_COLOR, new Color(250, 0, 250));
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_OFFSET, new double[]{2,0.2,2.2});
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_SCALE, .008);
		velocityBody.getAppearance().setAttribute(EDGE_DRAW, true);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		velocityBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBE_RADIUS, .02);
		velocityBody.getAppearance().setAttribute(METRIC, Pn.EUCLIDEAN);
		velocityBody.getAppearance().setAttribute(
				LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, 
				new Color(0, 255,0));
		velocityBody.getAppearance().setAttribute(
				LINE_SHADER+"."+DIFFUSE_COLOR, 
				new Color(0, 255,0));
		velFactory = new PointRangeFactory();
		velFactory.setFiniteSphere(finiteSphere);
		velFactory.setSphereRadius(sphereRadius);
		velFactory.setNumberOfSamples(samplesPerLine);	
//		velocityFactory.setPluckerLine(P5.pluckerLineWXYZToXYZW(null, velocity));
		if (!finiteSphere) velFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		velocityBody.setGeometry(velFactory.getLine());
		velPolarFactory = new PointRangeFactory();
		velPolarFactory.setFiniteSphere(finiteSphere);
		velPolarFactory.setNumberOfSamples(samplesPerLine);	
		velPolarFactory.setSphereRadius(sphereRadius);
//		velocityFactory.setPluckerLine(P5.pluckerLineWXYZToXYZW(null, velocity));
		if (!finiteSphere) velPolarFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		vBodyPolar.setGeometry(velPolarFactory.getLine());
		vBodyPolar.getAppearance().setAttribute(
				LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, 
				new Color(0, 250, 0));
		vBodyPolar.getAppearance().setAttribute(
				LINE_SHADER+"."+DIFFUSE_COLOR, 
				new Color(0, 250, 0));

		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+DIFFUSE_COLOR, new Color(250, 0, 250));
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_OFFSET, new double[]{2,0.2,2.2});
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TEXT_SHADER+"."+TEXT_SCALE, .008);
		momentumBody.getAppearance().setAttribute(EDGE_DRAW, true);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		momentumBody.getAppearance().setAttribute(LINE_SHADER+"."+TUBE_RADIUS, .02);
		momentumBody.getAppearance().setAttribute(
				LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, 
				new Color(250, 0, 250));
		momentumBody.getAppearance().setAttribute(
				LINE_SHADER+"."+DIFFUSE_COLOR, 
				new Color(250, 0, 250));
		momFactory = new PointRangeFactory();
		momFactory.setFiniteSphere(finiteSphere);
		momFactory.setNumberOfSamples(samplesPerLine);			
		momFactory.setSphereRadius(sphereRadius);
//		momentumFactory.setPluckerLine(P5.pluckerLineWXYZToXYZW(null, momentum));
		if (!finiteSphere) momFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		momentumBody.setGeometry(momFactory.getLine());
		momPolarFactory = new PointRangeFactory();
		momPolarFactory.setFiniteSphere(finiteSphere);
		momPolarFactory.setNumberOfSamples(samplesPerLine);	
		momPolarFactory.setSphereRadius(sphereRadius);
//		velocityFactory.setPluckerLine(P5.pluckerLineWXYZToXYZW(null, velocity));
		if (!finiteSphere) momPolarFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		mBodyPolar.getAppearance().setAttribute(
				LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, 
				new Color(250, 0, 250));
		mBodyPolar.getAppearance().setAttribute(
				LINE_SHADER+"."+DIFFUSE_COLOR, 
				new Color(250, 0, 250));
		mBodyPolar.setGeometry(momPolarFactory.getLine());

		for (int i =0; i<4; ++i) 
			pointCurve[i] = new PointCollector(positionHistory, 4);
//		velMesh = new CurveCollector(snakeLength, finiteSphere ? 2 : lineSamples, 4);
		moverSGC = SceneGraphUtility.createFullSceneGraphComponent("mover");
		moverSGC.getAppearance().setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
//		mover.getAppearance().setAttribute(METRIC, Pn.EUCLIDEAN);
		
		positionCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("positionCurve");
		positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		posColor = new Color(100, 255, 255);
		positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, posColor);
		for (int i=0; i<4; ++i) {
			positionCurveListSGC[i] = SceneGraphUtility.createFullSceneGraphComponent("positionCurve"+i);
				positionCurveListSGC[i].setGeometry(pointCurve[i].getCurve());
			positionCurveSGC.addChild(positionCurveListSGC[i]);
		}
		updatePositionVisibility();
		
		velocityCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("velocityCurve");
		velocityCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);
		velocityCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, 
				new Color(255, 255, 0));
//		velocityCurveSGC.setGeometry(velMesh.getMesh());
		velocityCurveSGC.getAppearance().setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		velocityCurveSGC.addChild(geomCollector);
		
		hyperbolicBoundary = SceneGraphUtility.createFullSceneGraphComponent("boundary");
		
		Appearance appearance = hyperbolicBoundary.getAppearance();
		appearance.setAttribute(METRIC, Pn.EUCLIDEAN);
		appearance.setAttribute(LIGHTING_ENABLED, false);
		appearance.setAttribute(LINE_SHADER+"."+TUBES_DRAW,false);
		appearance.setAttribute(EDGE_DRAW,false);
		appearance.setAttribute(TRANSPARENCY_ENABLED, true);
		//appearance.setAttribute(TRANSPARENCY, 0.0);
		appearance.setAttribute("lineShader.diffuseColor",Color.WHITE);
		appearance.setAttribute("lineShader.lineWidth",2);
		updateSphereTexture();
//		hyperbolicBoundary.addChild(Primitives.wireframeSphere(80,40)); 
		IndexedFaceSet sphericalPatch = SphereUtility.sphericalPatch(0.0, 0.0, 360.0, 180.0, 120, 80, 1.0);
		hyperbolicBoundary.setGeometry(sphericalPatch);
		hyperbolicBoundary.setVisible(metric == Pn.HYPERBOLIC);
		parent.addChildren(velocityBody, momentumBody);
		SceneGraphComponent rawBody = getDefaultBodyRepn();
		object = new SceneGraphComponent("the body repn wrapper");
		object.addChild(rawBody);
		moverSGC.addChildren(object, parent, positionCurveSGC, velocityCurveSGC, hyperbolicBoundary);
		//updateSceneGraphRepn();
	}
	
	private void updateSphereTexture() {
		sphereTexFac = new SimpleTextureFactory();
		sphereTexFac.setType(SimpleTextureFactory.TextureType.LINE);
		sphereTexFac.setColor(2, new Color(255, 155, 0)); //Color.cyan);
		sphereTexFac.setColor(3, new Color(0f,0f,0f,((float) sphereTransp)));
		sphereTexFac.update();
		Appearance ap = hyperbolicBoundary.getAppearance();
		Texture2D grid = TextureUtility.createTexture(ap, "polygonShader", 0, sphereTexFac.getImageData());
		//gridTexture2d[i].setImage(stf.getImageData());
		Matrix tm = new Matrix();
		MatrixBuilder.euclidean().scale(80,60,1).assignTo(tm);
		grid.setTextureMatrix(tm);
		grid.setApplyMode(Texture2D.GL_MODULATE);
		grid.setBlendColor(new Color(1f, 1f, 1f, 1f));	
		int greyi = ((int) (greySphere*255));
		ap.setAttribute("polygonShader.diffuseColor", new Color(greyi, greyi, greyi));//new Color(130,130,130,255)); //
	}

	private void updateAxisLineWidth()	{
		parent.getAppearance().setAttribute("lineShader.lineWidth",axisLineWidth);
//		vBodyPolar.getAppearance().setAttribute("lineShader.lineWidth",2);
//		momentumBody.getAppearance().setAttribute("lineShader.lineWidth",2);
//		mBodyPolar.getAppearance().setAttribute("lineShader.lineWidth",2);

	}
	private void updatePositionVisibility() {
		for (int i=0; i<4; ++i) {
			positionCurveListSGC[i].setVisible(whichCenterOfMass == i);
		}
	}

	// 
	static private double[][] clrs = {
		{0d, 1d, 0d},
		{0d, 0d, 1d},
		{1d, 0d, 0d},
		{1d, 0d, 1d}
		};
	private SimpleTextureFactory sphereTexFac;
	private Color posColor;
	static double[][] sixFromThree(int i, int j, int k)	{
		return new double[][]{clrs[i], clrs[j], clrs[k], clrs[i], clrs[j], clrs[k]};
	}
	boolean showEllipsoid = false;
	private SceneGraphComponent ellipsoid, box;
	protected SceneGraphComponent getDefaultBodyRepn() {
		SceneGraphComponent rawBody = SceneGraphUtility.createFullSceneGraphComponent("body repn");
		// render inertia ellipsoid
		ellipsoid = SceneGraphUtility.createFullSceneGraphComponent("ellipsoid");
		IndexedFaceSet sphericalPatch = SphereUtility.sphericalPatch(0.0, 0.0, 360.0, 180.0, 120, 80, 1.0);
		ellipsoid.setGeometry(sphericalPatch); //Primitives.wireframeSphere());
		Appearance ap = ellipsoid.getAppearance();
		ap.setAttribute(CommonAttributes.FACE_DRAW, true);
		rawBody.addChild(ellipsoid);
		
		IndexedFaceSetFactory ifsf = getColoredBox();
		box = SceneGraphUtility.createFullSceneGraphComponent("body repn");
		box.setGeometry(ifsf.getIndexedFaceSet()); //regularPolygon(30));
		box.getAppearance().setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white );
		box.getAppearance().setAttribute(CommonAttributes.AMBIENT_COEFFICIENT, .2 );
		box.getAppearance().setAttribute(CommonAttributes.AMBIENT_COLOR, Color.yellow);
		rawBody.addChild(box);
		rawBody.getAppearance().setAttribute(VERTEX_DRAW, false);
		rawBody.getAppearance().setAttribute(EDGE_DRAW, false);
		return rawBody;
	}

	public static IndexedFaceSetFactory getColoredBox() {
		IndexedFaceSetFactory ifsf = Primitives.boxFactory(2,2,2, false, Pn.EUCLIDEAN);
		ifsf.setFaceColors(sixFromThree(1,2,3));
		ifsf.update();
		return ifsf;
	}

	public int getMetric() {
		return metric;
	}
	
	public void setMetric(int metric) {
		this.metric = metric;
		reset();
		hyperbolicBoundary.setVisible(metric == Pn.HYPERBOLIC);
		moverSGC.getAppearance().setAttribute(METRIC, metric);
		mBodyPolar.setVisible(metric != Pn.EUCLIDEAN);
		vBodyPolar.setVisible(metric != Pn.EUCLIDEAN);
	}

	public void reset()	{
		lastVelocity = null;
		for (int i = 0; i<4;++i) pointCurve[i].reset();
		geomCollector.reset();
		handfulOfPoints.clear();
		updateSceneGraphRepn();
	}

	
	protected double[] getDual(double[] mat)	{
		double[] motionDual = Rn.transpose(null, Rn.inverse(null, mat ));
		if (metric == Pn.EUCLIDEAN) {motionDual[12] = motionDual[13] = motionDual[14] = 0.0; }
		return motionDual;
	}

	public Component getInspector()	{	
		final Box container = Box.createVerticalBox();

		container.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "Clothing")));
		Box hbox = Box.createHorizontalBox();
		container.add(hbox);
		hbox.add(Box.createHorizontalGlue());
		hbox.add(Box.createHorizontalStrut(5));
		final JCheckBox posCB = new JCheckBox("show C of M path");
		posCB.setSelected(showPosition);
		hbox.add(posCB);
		posCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showPosition = (posCB.isSelected());
			}
			
		});
		hbox.add(Box.createHorizontalStrut(10));
		hbox.add(new JLabel("Velocity history: "));
		hbox.add(Box.createHorizontalStrut(5));
		final JComboBox veldsplCB = new JComboBox(VelocityDisplay.values());
		veldsplCB.setSelectedItem(velDisplay);
	    hbox.setMaximumSize(new Dimension(1000, 30));   
	    veldsplCB.setMaximumSize(new Dimension(1000, 30));   
		hbox.add(veldsplCB);
		veldsplCB.addActionListener(new ActionListener()	{

			public void actionPerformed(ActionEvent e) {
				velDisplay = (VelocityDisplay) (veldsplCB.getSelectedItem());
				System.err.println("vel dsply = "+velDisplay);
				velocityCurveSGC.setVisible(velDisplay != VelocityDisplay.NONE);
				geomCollector.reset();
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		hbox.add(Box.createHorizontalGlue());
		hbox = Box.createHorizontalBox();
		final JCheckBox ellCB = new JCheckBox("show ellipsoid");
		ellCB.setSelected(showEllipsoid);
		hbox.add(ellCB);
		ellCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				showEllipsoid = (ellCB.isSelected());
				updateBoxRepn();
			}
			
		});
		final JButton llB = new JButton("line shading");
		hbox.add(llB);
		llB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				lineShading = !lineShading;
				setShadeLines(lineShading);
				updateSceneGraphRepn();
			}
			
		});
		hbox.add(Box.createHorizontalStrut(10));

		final JButton colorb = new JButton("pos color ");
		colorb.setBackground(posColor);
		colorb.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				Color color = JColorChooser.showDialog(((Component)t3d.viewer.getViewingComponent()), "Select color ",  null);
				if (color != null) {
					posColor = color;
					positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, posColor);
					colorb.setBackground(posColor);
				}
			}
		});
		hbox.add(colorb);

		container.add(hbox);
		final TextSlider countSlider = new TextSlider.IntegerLog("path bucket size",  SwingConstants.HORIZONTAL, 1, 1000, pathBucketSize);
		countSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				pathBucketSize = countSlider.getValue().intValue();
				stepcount = 0;
			}
		});
		container.add(countSlider);
		final TextSlider ssSlider = new TextSlider.IntegerLog("velocity sampling rate",  SwingConstants.HORIZONTAL, 1, 100, velocityBucketSize);
		ssSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				velocityBucketSize = ssSlider.getValue().intValue();
			}
		});
		container.add(ssSlider);
		final TextSlider widthSlider = new TextSlider.Double("strip width",  SwingConstants.HORIZONTAL, 0.0, 1.0, stripWidth);
		widthSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				stripWidth = widthSlider.getValue().intValue();
			}
		});
		container.add(widthSlider);
		final TextSlider whichSlider = new TextSlider.Integer("which center",  SwingConstants.HORIZONTAL, 0, 3, whichCenterOfMass);
		whichSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				whichCenterOfMass = whichSlider.getValue().intValue();
				updatePositionVisibility();
			}
		});
		container.add(whichSlider);
		final TextSlider alwSlider = new TextSlider.Double("axis lw",  SwingConstants.HORIZONTAL, 0, 10, axisLineWidth);
		alwSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				axisLineWidth = alwSlider.getValue().intValue();
				updateAxisLineWidth();
			}
		});
		container.add(alwSlider);
		final TextSlider transpSlider = new TextSlider.Double("sphere alpha",  SwingConstants.HORIZONTAL,0,1, sphereTransp);
		transpSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				sphereTransp = transpSlider.getValue().doubleValue();
				updateSphereTexture();
			}
		});
		container.add(transpSlider);
		final TextSlider greySlider = new TextSlider.Double("sphere bright",  SwingConstants.HORIZONTAL,0,1, greySphere);
		greySlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				greySphere = greySlider.getValue().doubleValue();
				updateSphereTexture();
			}
		});
		container.add(greySlider);
		final TextSlider sizeSlider = new TextSlider.DoubleLog("body scale",  SwingConstants.HORIZONTAL,0.01,2, globalBoxScale);
		sizeSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				globalBoxScale = sizeSlider.getValue().doubleValue();
				updateBoxRepn();
			}
		});
		container.add(sizeSlider);


		pcountGUI = new TextSlider.Integer("point count",  SwingConstants.HORIZONTAL, 0, 10000000, 0);
		pcountGUI.getSlider().setVisible(false);
		vcountGUI = new TextSlider.Integer("velocity count",  SwingConstants.HORIZONTAL, 0, 10000000, 0);
		vcountGUI.getSlider().setVisible(false);
		container.add(pcountGUI);
		container.add(vcountGUI);
		container.add(Box.createVerticalGlue());
		return container;
	
	}

	public int getWhichCenterOfMass() {
		return whichCenterOfMass;
	}

	public void setWhichCenterOfMass(int whichCenterOfMass) {
		this.whichCenterOfMass = whichCenterOfMass;
		updatePositionVisibility();
	}
	
	protected void setShadeLines(boolean b)	{
		IndexedLineSet currpos = pointCurve[whichCenterOfMass].getCurve();
		Appearance ap = positionCurveListSGC[whichCenterOfMass].getAppearance();
		if (!b) {
			currpos.setVertexAttributes(Attribute.NORMALS, null);
			ap.setAttribute(LINE_SHADER+"."+LINE_LIGHTING_ENABLED, false);
			ap.setAttribute(METRIC, Appearance.INHERITED);
			positionCurveListSGC[whichCenterOfMass].setGeometry(pointCurve[whichCenterOfMass].getCurve());
			return;
		} 
		IndexedLineSet newone = IndexedLineSetUtility.createCurveFromPoints(IndexedLineSetUtility.extractCurve(null, currpos, 0), false);
		
		newone.setVertexAttributes(Attribute.NORMALS, StorageModel.DOUBLE_ARRAY_ARRAY.createReadOnly(calculateCurveNormals(currpos)));
		ap.setAttribute(LINE_SHADER+"."+LINE_LIGHTING_ENABLED, true);
		positionCurveListSGC[whichCenterOfMass].setGeometry(newone);
		ap.setAttribute(METRIC, Pn.EUCLIDEAN);
		ap.setAttribute(AMBIENT_COEFFICIENT, .12);
		return;

	}
	  private static double[][] calculateCurveNormals(IndexedLineSet ifs)	{
		  TubeFactory tf = new PolygonalTubeFactory(ifs, 0);
		  tf.setFrameFieldType(FrameFieldType.FRENET);
		  tf.setClosed(true);
		  tf.update();
		  FrameInfo[] frames = tf.getFrameField();
		  int n = frames.length-1;
		  double[][] nn = new double[n][3];
		  for (int i = 0; i<n; ++i)	{
			  double[] frame = Rn.transpose(null, frames[i].frame);
			  System.arraycopy(frame, 0, nn[i], 0, 3);
		  }
		  Rn.times(nn, -1, nn);
		  System.err.println("coords length = "+ifs.getNumPoints());
		  System.err.println("normal length = "+n);
		  return nn;
	  }

}
