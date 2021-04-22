/*
 * Created on Dec 1, 2010
 *
 */
package charlesgunn.jreality.worlds.iscador;

import static de.jreality.geometry.GeometryUtility.BOUNDING_BOX;
import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.EDGE_DRAW;
import static de.jreality.shader.CommonAttributes.LIGHTING_ENABLED;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.TUBES_DRAW;
import static de.jreality.shader.CommonAttributes.VERTEX_DRAW;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;

import charlesgunn.anim.core.KeyFrameAnimatedDelegate;
import charlesgunn.anim.core.KeyFrameAnimatedDouble;
import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.anim.util.AnimationUtility.InterpolationTypes;
import charlesgunn.jreality.GeometryCollector;
import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import charlesgunn.jreality.geometry.projective.LinePencilFactory;
import charlesgunn.jreality.geometry.projective.LineUtility;
import charlesgunn.jreality.geometry.projective.PointCollector;
import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import charlesgunn.math.Biquaternion;
import charlesgunn.math.clifford.TwoSpace;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.BallAndStickFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.IndexedLineSetFactory;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.actions.view.SetViewerSize;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class Iscador2DCentrifuge extends Assignment {

	int numLines = 10;
	private SceneGraphComponent world,
		eucSGC,
			diskSGC,
			diskBoundarySGC, // drawn as vertices
			diskLabelSGC,
		peucSGC,
			annulusSGC,  // the "inside" of the polar region
			annulus2SGC,  // the "inside" of the polar region
		rotationCenterSGC,
		diskBoundary2SGC,  // drawn as red line
		starSGC,
		tangentsSGC,
		arrowsSGC,
			physArrowsSGC,
			ethArrowsSGC,
		circleAnimationSGC;
	int numPoints = 500;
	GeometryCollector lineCircle = new GeometryCollector(numPoints), 
			tangentsgc = new GeometryCollector(numPoints);
	double[] lastPolar = null;
	// draw an ellipse and its polar
	double a = 1.0, b = 1.0, c = 0, 
			d = 0.0, s = .015, arrowScale = .2, rotAngle = 0;
	int fullCircles = 3;
	int num = 80, m = 128, sampleRate = 5;
	Color mygray = new Color(m,m,m),
			myblack = new Color(m/3, m/3, m/3),
			diskColor = new Color(255,220, 180),
			pointColor = new Color(125, 0, 125),
			polarDiskColor = new Color(140, 220, 255);
	Color physicalSpacePC = new Color(255, 128, 0),
			physicalSpaceLC = physicalSpacePC,
			ethericSpacePC = new Color(0, 128, 0),
			ethericSpaceLC = new Color(0, 128, 0),
			physicalSpacePCO = myblack,
			physicalSpaceLCO = myblack,
			ethericSpacePCO = mygray,
			ethericSpaceLCO = mygray;
	IndexedLineSetFactory diskILSF = new IndexedLineSetFactory(), 
			flowedDiskILSF = new IndexedLineSetFactory(),
			physArrowILSF = new IndexedLineSetFactory(),
			ethArrowILSF = new IndexedLineSetFactory();
	BallAndStickFactory physArrowBSF, ethArrowBSF;
	double[][] diskVerts, flowedDiskVerts, flowedDiskDirs;

	@Override
	public SceneGraphComponent getContent() {
		if  (world != null) return world;
		lineCircle.setName("original line circle");
		tangentsgc.setName("flowed line circle");
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		eucSGC = SceneGraphUtility.createFullSceneGraphComponent("euc");
		peucSGC = SceneGraphUtility.createFullSceneGraphComponent("polar euc");
		diskSGC = SceneGraphUtility.createFullSceneGraphComponent("disk");
		diskBoundarySGC = SceneGraphUtility.createFullSceneGraphComponent("disk boundary");
		diskBoundary2SGC = SceneGraphUtility.createFullSceneGraphComponent("disk boundary unmoved");
		arrowsSGC = SceneGraphUtility.createFullSceneGraphComponent("arrows");
		ethArrowsSGC = SceneGraphUtility.createFullSceneGraphComponent("eth arrows");
		physArrowsSGC = SceneGraphUtility.createFullSceneGraphComponent("phys arrows");
		diskLabelSGC = SceneGraphUtility.createFullSceneGraphComponent("hiscia logo");
		annulusSGC = SceneGraphUtility.createFullSceneGraphComponent("polar disk interior");
		annulus2SGC = SceneGraphUtility.createFullSceneGraphComponent("polar disk interior bkgd");
		rotationCenterSGC = SceneGraphUtility.createFullSceneGraphComponent("rotation center");
		starSGC = SceneGraphUtility.createFullSceneGraphComponent("star center");
		tangentsSGC = SceneGraphUtility.createFullSceneGraphComponent("tangents");
		circleAnimationSGC = SceneGraphUtility.createFullSceneGraphComponent("circle animation");
		SceneGraphComponent hisciaSGC = Iscador3DEarth.getLabelForDisk();
		MatrixBuilder.euclidean().translate(0,0,-.01).assignTo(hisciaSGC);
		diskLabelSGC.addChild(hisciaSGC);
		Appearance ap = annulusSGC.getAppearance();
		ap.setAttribute(EDGE_DRAW, false);
		ap.setAttribute(VERTEX_DRAW, false);
		ap.setAttribute(CommonAttributes.TRANSPARENCY, .5);
		ap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
		ap.setAttribute(BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute("polygonShader.diffuseColor", polarDiskColor);
		ap = annulus2SGC.getAppearance();
		ap.setAttribute(EDGE_DRAW, false);
		ap.setAttribute(VERTEX_DRAW, false);
		ap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, false);
		ap.setAttribute(BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute("polygonShader.diffuseColor", polarDiskColor);
		tangentsgc.setAppearance(new Appearance());
		ap = arrowsSGC.getAppearance();
		ap.setAttribute("pointShader.diffuseColor", physicalSpacePC);
		ap.setAttribute("lineShader.diffuseColor", physicalSpaceLC);
		MatrixBuilder.euclidean().translate(0, 0, .02).assignTo(arrowsSGC);
		ap = tangentsgc.getAppearance();
//		ap.setAttribute("pointShader.diffuseColor", ethericSpacePC);
//		ap.setAttribute("lineShader.diffuseColor", ethericSpaceLC);
		ap.setAttribute(BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute(VERTEX_DRAW, false);
		ap = diskBoundarySGC.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute("pointShader.diffuseColor", physicalSpacePCO);
		ap.setAttribute("pointShader.pointRadius", .015);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.LINE_WIDTH,1.7);
		ap = diskBoundary2SGC.getAppearance();
//		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, true);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .01);
		ap.setAttribute("pointShader.diffuseColor", Color.RED);
		ap.setAttribute("lineShader.diffuseColor", Color.red);
//		ap.setAttribute(CommonAttributes.LIGHTING_ENABLED, true);
		lineCircle.setAppearance(new Appearance());
		ap = lineCircle.getAppearance();
		ap.setAttribute("pointShader.diffuseColor", ethericSpacePCO);
		ap.setAttribute("lineShader.diffuseColor", ethericSpaceLCO);
		ap.setAttribute(BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute(VERTEX_DRAW, false);
		ap = diskSGC.getAppearance();
		ap.setAttribute(EDGE_DRAW, false);
		ap.setAttribute(VERTEX_DRAW, false);
		ap.setAttribute("polygonShader.diffuseColor",diskColor);
		ap = diskLabelSGC.getAppearance();
		ap.setAttribute("polygonShader.diffuseColor",diskColor);
		MatrixBuilder.euclidean().translate(0,0,-.0).rotateZ(-Math.PI/2).scale(.65).assignTo(diskLabelSGC);
		MatrixBuilder.euclidean().translate(0,0,-.01).assignTo(diskSGC);
		ap = rotationCenterSGC.getAppearance();
		ap.setAttribute(VERTEX_DRAW, true);
//		ap.setAttribute(CommonAttributes.SPHERES_DRAW,true);
		ap.setAttribute(CommonAttributes.POINT_SIZE, 3.0);
		ap.setAttribute(CommonAttributes.POINT_RADIUS, .020);
		ap.setAttribute("pointShader.diffuseColor",pointColor);
		ap = tangentsSGC.getAppearance();
		ap.setAttribute(VERTEX_DRAW, false);
//		ap.setAttribute(CommonAttributes.SPHERES_DRAW,true);
		ap.setAttribute(CommonAttributes.TUBES_DRAW, false);
		ap.setAttribute(CommonAttributes.LINE_WIDTH, 1.7);
		ap.setAttribute("lineShader.diffuseColor",pointColor);
		
		starSGC.setGeometry(GeometryUtilityOverflow.starPoint(16, .015, .05));
		ap = starSGC.getAppearance();
		ap.setAttribute("lineShader.diffuseColor", pointColor);
		ap.setAttribute("lineShader.drawTubes", false);
		ap.setAttribute("lineShader.lineWidth", 1.0);

		ap = world.getAppearance();
		ap.setAttribute(TUBES_DRAW, false);
		diskBoundarySGC.setGeometry(diskILSF.getGeometry());
		diskBoundary2SGC.setGeometry(diskILSF.getGeometry());
				
		tangentsSGC.addChild(tangentsgc);
		world.addChildren(eucSGC, peucSGC, rotationCenterSGC, diskBoundary2SGC, starSGC, tangentsSGC, arrowsSGC);
		arrowsSGC.setVisible(false);
		arrowsSGC.addChildren(ethArrowsSGC, physArrowsSGC);
		eucSGC.addChildren(diskSGC, diskLabelSGC, diskBoundarySGC);
		peucSGC.addChildren(lineCircle, annulusSGC, annulus2SGC); //flowedgc,
		
		ap = world.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, new Color(255,255,255,0));
//		ap.setAttribute("lineShader.lineWidth", 0.7);
		ap.setAttribute(LIGHTING_ENABLED, false);

		// draw conic section C: a^2(x-c)^2 + b^2y^2 -z^2=0
		initializeGeometry();
		fixedGeometry();
		flow();

		return world;
	}
	int animationPhase = 2; // 0: Vary s with d = 0,  1: vary d with s = whatever,  2: little circle animation
							// 4: rev up the rotation
	@Override
	public void startAnimation() {
		super.startAnimation();
	}
	
	@Override
	public void endAnimation() {super.endAnimation();};
	double begin = .02, end = .98, minS = 0, maxS = .1339, minD = 0.6, maxD1 = 1.0, maxD2 = 1.999;
	double dmid = .33;
	@Override
	public void setValueAtTime(double time) {
		super.setValueAtTime(time);
		switch(animationPhase) {
		case 0:				// vary s
			s = AnimationUtility.linearInterpolation(time, begin, end, minS, maxS);
			flow();
			break;
		case 1:				// vary arrow scale
			arrowScale = AnimationUtility.linearInterpolation(time, begin, end, minS, maxS);
			flow();
			break;
		case 2:				// vary d
			double tmp = (time < dmid) ? time/dmid : (time-dmid)/(1-dmid);
			if ( time < dmid) d = AnimationUtility.linearInterpolation(tmp, begin, end, minD, maxD1);
			else d = AnimationUtility.linearInterpolation(tmp, begin, end, maxD1, maxD2);
			flow();
			break;
		case 3:
			double r = AnimationUtility.linearInterpolation(time, begin, end, .02,0);
			System.err.println("annulus with r = "+(50*r));
			annulus2SGC.setGeometry(Primitives.regularAnnulus(num, r, .02));
			break;
		default:
			break;
		}
	}
	@Override
	public void display() {
		super.display();
//		jrviewer.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(204,204,204));
	    setBackground();
		if (animationPhase == 2) {
//			diskSGC.setVisible(false);
			diskLabelSGC.setVisible(false);
//			diskBoundary2SGC.setVisible(false);
			annulusSGC.setVisible(false);
//			annulus2SGC.setVisible(false);
//			starSGC.setVisible( false);
		}
	    animationPlugin.setAnimateSceneGraph(false);
	    
		animationPlugin.getAnimationPanel().setResourceDir("src/charlesgunn/jreality/worlds/iscador/");
//		try {
//			animationPlugin.getAnimationPanel().read(new Input(this.getClass().getResource("iscadorCentrifuge-anim.xml")));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		animationPlugin.setAnimateCamera(true);
		if (animationPhase >= 5) {
			KeyFrameAnimatedDelegate<Double> dd = new KeyFrameAnimatedDelegate<Double> () {

				public void propagateCurrentValue(Double t) {
					d = t;
					dSlider.setValue(t);
					flow();
					jrviewer.getViewer().renderAsync();
				}

				public Double gatherCurrentValue(Double t) {
					return d;
				}
				
			};
			KeyFrameAnimatedDouble animAlpha = new KeyFrameAnimatedDouble(dd );
			animAlpha.setName("animD");
			animationPlugin.getAnimated().add(animAlpha);
			KeyFrameAnimatedDelegate<Double> sd = new KeyFrameAnimatedDelegate<Double> () {

				public void propagateCurrentValue(Double t) {
					s = t;
					sSlider.setValue(t);
					flow();
					jrviewer.getViewer().renderAsync();
					System.err.println("IN propagate");
				}

				public Double gatherCurrentValue(Double t) {
					return s;
				}
				
			};
			animAlpha = new KeyFrameAnimatedDouble(sd );
			animAlpha.setName("animS");
			animationPlugin.getAnimated().add(animAlpha);

			KeyFrameAnimatedDelegate<Double> asd = new KeyFrameAnimatedDelegate<Double> () {

				public void propagateCurrentValue(Double t) {
					arrowScale = t;
					asSlider.setValue(t);
					flow();
					jrviewer.getViewer().renderAsync();
					System.err.println("IN propagate");
				}

				public Double gatherCurrentValue(Double t) {
					return arrowScale;
				}
				
			};
			animAlpha = new KeyFrameAnimatedDouble(asd );
			animAlpha.setName("animArrowS");
			animationPlugin.getAnimated().add(animAlpha);

			KeyFrameAnimatedDelegate<Double> angled = new KeyFrameAnimatedDelegate<Double> () {

				public void propagateCurrentValue(Double t) {
					rotAngle = t;
					raSlider.setValue(t);
					flow();
					jrviewer.getViewer().renderAsync();
					System.err.println("IN propagate");
				}

				public Double gatherCurrentValue(Double t) {
					return rotAngle;
				}
				
			};
			animAlpha = new KeyFrameAnimatedDouble(angled );
			animAlpha.setName("animRotAngle");
			animationPlugin.getAnimated().add(animAlpha);
		}
		Camera cam = CameraUtility.getCamera(jrviewer.getViewer());
		cam.setFieldOfView(1.25 * cam.getFieldOfView());
		cam.setPerspective(false);
	}
	private void initializeGeometry() {
		lineCircle.reset();
		diskVerts = new double[num][];
		flowedDiskVerts = new double[num][];
		flowedDiskDirs = new double[num][];
		for (int i = 0; i<num; ++i)	{
			double angle = Math.PI * 2  *(i)/(num);
			// following point lies on the conic section above
			double[] pt = {Math.cos(angle), Math.sin(angle), 0, 1};
			diskVerts[i] = pt;
//			// polar line at this point is given by gradient of C evaluated at this point
			double[] polarline = {pt[0], pt[1], -1};
			double[] polarline6 = { polarline[2], 0, -polarline[1], 0, -polarline[0], 0};
			PointRangeFactory prf = new PointRangeFactory();
			prf.setPluckerLine(polarline6);
			prf.setFiniteSphere(false);
			prf.update();
			lineCircle.addGeometry(prf.getLine());
		}
		diskSGC.setGeometry(IndexedFaceSetUtility.constructPolygon(diskVerts));
		IndexedLineSetUtility.createCurveFactoryFromPoints(diskILSF, diskVerts, true);
	}
	private void fixedGeometry()	{
		annulusSGC.setGeometry(Primitives.regularAnnulus(num, 0, .02));
		annulus2SGC.setGeometry(annulusSGC.getGeometry());
		MatrixBuilder.euclidean().translate(0,0,-.01). scale(50).assignTo(annulusSGC);
		MatrixBuilder.euclidean().translate(0,0,-.02). scale(50).assignTo(annulus2SGC);
		physArrowBSF = new BallAndStickFactory(physArrowILSF.getIndexedLineSet());
		BallAndStickFactory tmp = physArrowBSF;
		tmp.setStickRadius(.012);
		tmp.setStickColor(Color.RED);
		tmp.setArrowColor(Color.RED);
		tmp.setShowArrows(true);
		tmp.setShowBalls(false);
		tmp.setArrowScale(.03);
		tmp.setArrowSlope(2);
		tmp.setArrowPosition(.9);
		tmp = ethArrowBSF = new BallAndStickFactory(ethArrowILSF.getIndexedLineSet());
		tmp.setStickRadius(.012);
		tmp.setStickColor(Color.BLUE);
		tmp.setArrowColor(Color.BLUE);
		tmp.setShowArrows(true);
		tmp.setShowBalls(false);
		tmp.setArrowScale(.03);
		tmp.setArrowSlope(2);
		tmp.setArrowPosition(.9);
		physArrowsSGC.addChild(physArrowBSF.getSceneGraphComponent());
		ethArrowsSGC.addChild(ethArrowBSF.getSceneGraphComponent());
	}
	
	double[] lineCenter = {0,0,1},
			w2n, n2w;
	private void flow() {
		if (d >= 2) d = 1.9999; 
		double distance = (d <= 1.0) ? d : 1.0/(2.0-d);
		System.err.println("distance = "+distance);
		
		// scaling has to be damped down for larger values of distance
		double scale = (d > 1) ? (1+s/distance) : (1 + s);

		tangentsgc.reset();
		double[] pointCenter = {0,distance,0,1};
		rotationCenterSGC.setGeometry(Primitives.point(pointCenter));
		MatrixBuilder.euclidean().translate(0, distance, 0).assignTo(starSGC);
		w2n = new double[]{1,0,0,   0,1,0,    0,distance,1};
//		w2n = new double[]{1,0,0,   0,1,-d,    0,0,1};
		n2w = Rn.inverse(null, w2n);
		int lim = diskVerts.length;
		for (int i = 0; i<lim; ++i)	{
			flowedDiskDirs[i] = Rn.subtract(null, diskVerts[i], pointCenter);
			flowedDiskVerts[i] = Rn.add(flowedDiskVerts[i], diskVerts[i], Rn.times(null, s, flowedDiskDirs[i] ));
			// following point lies on the conic section above
//			double[] pt = diskVerts[i];
//			// polar line at this point is given by gradient of C evaluated at this point
//			double[] tangentLine = {pt[0], pt[1],  -1};
			// use the polar euclidean metric on lines (like euclidean points)
//			double[] foo = getForceForLine(tangentLine);
//			PointRangeFactory prf = new PointRangeFactory();
//			double[] polarline6 = { foo[2], 0, -foo[1], 0, -foo[0], 0};
//			prf.setPluckerLine(polarline6);
//			prf.setFiniteSphere(false);
//			prf.update();
//			flowedgc.addGeometry(prf.getLine());
		}
		// set up flow arrows  first physical
		for (int k = 0; k<2; ++k)	{
			int numArrows = lim/sampleRate;
			double[][] coords = new double[2*numArrows][];
			int[][] indices = new int[numArrows][2];
			double tmp2 = (k==0) ? arrowScale : -arrowScale;
			tmp2 = (d>1) ? tmp2/distance : tmp2;
			for (int i = 0; i<numArrows; ++i)	{
				coords[2*i] = diskVerts[sampleRate*i];
				coords[2*i+1] = Rn.add(null, diskVerts[sampleRate*i], Rn.times(null, tmp2, flowedDiskDirs[sampleRate*i] ));
				indices[i][0] = 2*i;
				indices[i][1] = 2*i+1;
			}
			IndexedLineSetFactory tmp = (k==0) ? physArrowILSF : ethArrowILSF;
			BallAndStickFactory tmpbasf = (k==0) ? physArrowBSF : ethArrowBSF;
			tmp.setVertexCount(coords.length);
			tmp.setVertexCoordinates(coords);
			tmp.setEdgeCount(numArrows);
			tmp.setEdgeIndices(indices);
			tmp.update();
			tmpbasf.update();			
		}
//		IndexedLineSetUtility.createCurveFactoryFromPoints(flowedDiskILSF, flowedDiskVerts, true);
		// try to implement the transformation using matrices
		double[] center = new double[]{0, distance,0,1};
		double[] n2w = P3.makeTranslationMatrix(null, center, Pn.EUCLIDEAN);
//		scale = 1+s;
		double[] pointScale = Rn.conjugateByMatrix(null, P3.makeScaleMatrix(null, scale), n2w);
		new Matrix(pointScale).assignTo(eucSGC);
		double[] lineScale = Rn.conjugateByMatrix(null, P3.makeScaleMatrix(null, 1/(scale)), n2w);
		new Matrix(lineScale).assignTo(peucSGC);
		
		if (distance > 1.0005) {
			double b = 1/distance,
					a = Math.sqrt(1.0 - b*b);
			double[] tan1 = {a,b,0,1}, tan2 = {-a,b,0,1};
			tangentsgc.addGeometry(PointRangeFactory.line(center, tan1));
			tangentsgc.addGeometry(PointRangeFactory.line(center, tan2));			
		}
		else if (distance > .9995) {
			double[] tan1 = {1,1,0,1}, tan2 = {-1,1,0,1};
			tangentsgc.addGeometry(PointRangeFactory.line(center, tan1));
		}
		setRotAngle();
	}

	private void setRotAngle() {
		// rotate the whole thing
		double tmp = (rotAngle)*rotAngle * fullCircles * 2 *Math.PI;
		MatrixBuilder.euclidean().rotateZ(tmp).assignTo(world);
		// HACK!
		if (animationPhase == 4) arrowScale = rotAngle*rotAngle * .2;
	}
	
	private void setBackground()	{
		Color bc =  (eucSGC.isVisible() && peucSGC.isVisible()) ? Color.black : new Color(204,204,204);
		jrviewer.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, bc);
	}
	final TextSlider sSlider = new TextSlider.Double("s",SwingConstants.HORIZONTAL, 0, 1, s);
	final TextSlider dSlider = new TextSlider.Double("d",SwingConstants.HORIZONTAL, 0, 2, d);
	final TextSlider asSlider = new TextSlider.Double("arrow s",SwingConstants.HORIZONTAL, 0, 1, arrowScale);
	final TextSlider raSlider = new TextSlider.Double("angle",SwingConstants.HORIZONTAL, 0, 1, rotAngle);
	final TextSlider fcSlider = new TextSlider.Integer("full circles",SwingConstants.HORIZONTAL, 0, 5, fullCircles);
	@Override
	public Component getInspector() {
		getContent();
		Box inspectionPanel =  inspector;
		final TextSlider nSlider = new TextSlider.Integer("num",SwingConstants.HORIZONTAL, 1, 200, num);
		nSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				num = nSlider.getValue().intValue();
				initializeGeometry();
				flow();
			}
		});
		inspectionPanel.add(nSlider);
		final TextSlider sampleSlider = new TextSlider.Integer("arrow sample rate",SwingConstants.HORIZONTAL, 1, 20, sampleRate);
		sampleSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				sampleRate = sampleSlider.getValue().intValue();
				initializeGeometry();
				flow();
			}
		});
		inspectionPanel.add(sampleSlider);
		dSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				d = dSlider.getValue().doubleValue();
				flow();
			}
		});
		inspectionPanel.add(dSlider);
//		final TextSlider sSlider = new TextSlider.Double("s",SwingConstants.HORIZONTAL, 0, 1, s);
		sSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				s = sSlider.getValue().doubleValue();
				flow();
			}
		});
		inspectionPanel.add(sSlider);
		asSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				arrowScale = asSlider.getValue().doubleValue();
				flow();
			}
		});
		inspectionPanel.add(asSlider);
		raSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				rotAngle = raSlider.getValue().doubleValue();
				flow();
			}
		});
		inspectionPanel.add(raSlider);
		fcSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				fullCircles = fcSlider.getValue().intValue();
				flow();
			}
		});
		inspectionPanel.add(fcSlider);
		Box horbox = Box.createHorizontalBox();
		inspectionPanel.add(horbox);
		final JCheckBox showPolar = new JCheckBox("show polar");
		horbox.add(showPolar);
		showPolar.setSelected(peucSGC.isVisible());
		showPolar.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Boolean b = showPolar.isSelected();
				peucSGC.setVisible(b);
				ethArrowsSGC.setVisible(b);
				setBackground();
			}
		});
		final JCheckBox showEuc = new JCheckBox("show euc");
		horbox.add(showEuc);
		showEuc.setSelected(eucSGC.isVisible());
		showEuc.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Boolean b = showEuc.isSelected();
				eucSGC.setVisible(b);
				physArrowsSGC.setVisible(b);
				setBackground();
			}
		});
		final JCheckBox showLines = new JCheckBox("show lines");
		horbox.add(showLines);
		showLines.setSelected(lineCircle.isVisible());
		showLines.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Boolean b = showLines.isSelected();
				lineCircle.setVisible(b);
			}
		});

		final JCheckBox showTans = new JCheckBox("show tangents");
		horbox.add(showTans);
		showTans.setSelected(tangentsSGC.isVisible());
		showTans.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				Boolean b = showTans.isSelected();
				tangentsSGC.setVisible(b);
			}
		});

		return inspectionPanel;
	}

	public static void main(String[] args) {
		new Iscador2DCentrifuge().display();
	}
}
