package dragonfly.dynamen.ws08;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import charlesgunn.jreality.geometry.Snake;
import charlesgunn.jreality.geometry.projective.LineUtility;
import charlesgunn.math.p5.PlueckerLineGeometry;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.BallAndStickFactory;
import de.jreality.geometry.IndexedLineSetFactory;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;

/**
 * A class which manages rigid body motion of an object free to rotate in euclidean space around its
 * center of gravity.
 * 
 * The class allows the user to set:
 * 	the array of moments of inertia of the body, see {@link #setMoments(double[])}.  
 * From this the inertia tensor and its inverse are calculated.  
 * Additionally, the user should provide 
 * 	an angular momentum {@link #setAngularMomentum(double[])} or 
 * 	angular velocity {@link #setAngularVelocity(double[])} as initial condition.
 * 
 * To drive the simulation, the user should obtain the instance of {@link ODE} using {@link #getODE()}, and also
 * call {@link #update()} to keep the scene graph representation up-to-date.  
 * 
 * In the scene graph, 
 * <ul>
 * <li> the angular velocity is represented as a brown axis,</li>
 * <li> the angular momentum is represented as a red axis, and </li>
 * <li> the inertia ellipsoid is represented as a wireframe ellipsoid.</li>
 * </ul>
 * 
 * The current version has optional support for an ODE based on 3x3 matrices
 * instead of  quaternions.  This is set using the method {@link #setUseQuaternions(boolean)},
 * but at the current time this option doesn't work yet.
 * 
 * @author Charles Gunn
 *
 */
public class RigidBodySimulatorGunn implements RigidBodyInterface {
	
	protected SceneGraphComponent 
		world, 
			body, 
				boxSGC,
					momentumVectorsSGC,
					momentumLinesSGC,
				ellipsoid, 
				angularVelocityBody, 
				angularMomentumBody,
				angVelCurveSGC,
			invPlaneSGC, 
				invPlaneGeomSGC;
	protected SceneGraphComponent[] momVecLines = new SceneGraphComponent[8];	
	protected double[] moments = {1,1,1};
	protected double[] inertiaTensor, invInertiaTensor;
	protected double[] angularVelocity = {0,0,1}, 
		angularMomentum = new double[3];
	RBMOde ode;
	protected double[] currentPosition = Rn.identityMatrix(4);
	protected IndexedLineSet avb = null, amb = null;
	protected IndexedLineSetFactory momentumVectorsFactory = null;
	protected BallAndStickFactory basf;
	protected Color momVecArrowColor = new Color(50,255,200), 
			momVecStickColor = new Color(50,255,50),
			velColor = new Color(0,100,255),
			momColor = new Color(255,50,0),
			polhodeColor = new Color(100,255,255),
			herpolhodeColor = Color.green;
	protected double[][] boxVertices, momentumVertices = new double[16][3];
	protected double vectorScale = .75;
	protected boolean running = true,
		showParticleM = true;
	protected Snake invPlaneCurve, angVelCurve;
	int limit = 5000, avlimit = 5000;
	double[][] velocityPoints = new double[limit][3], avpoints = new double[avlimit][3];
	int count = 0;
	final static double[] zaxis = {0,0,1};
	protected double a = .5, b = 1, c = 1.5;	// dimensions of box
	protected boolean cutVB;
	
	public RigidBodySimulatorGunn()	{
		
		body = SceneGraphUtility.createFullSceneGraphComponent("RBSimulator body");
		ellipsoid = SceneGraphUtility.createFullSceneGraphComponent("ellipsoid");
		ellipsoid.addChild(Primitives.wireframeSphere());
		body.addChild(ellipsoid);
		angularVelocityBody = SceneGraphUtility.createFullSceneGraphComponent("angVelBody");
		angularVelocityBody.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", velColor);
		angularVelocityBody.getAppearance().setAttribute("pointShader.polygonShader.diffuseColor", velColor);
		angularVelocityBody.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, true);
		body.addChild(angularVelocityBody);

		angularMomentumBody = SceneGraphUtility.createFullSceneGraphComponent("angMomBody");
		angularMomentumBody.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", momColor);
		angularMomentumBody.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .019);
		body.addChild(angularMomentumBody);				
		body.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);

		// create basic scene graph
		world = SceneGraphUtility.createFullSceneGraphComponent("RBSimulator world");
		//world.setPickable(false);
		angVelCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("angular velocity curve");
		invPlaneSGC = SceneGraphUtility.createFullSceneGraphComponent("invariable plane");
		invPlaneGeomSGC = SceneGraphUtility.createFullSceneGraphComponent("invariable plane geom");
		invPlaneGeomSGC.setGeometry(Primitives.regularPolygon(100));
		Appearance ap = invPlaneSGC.getAppearance();
		ap.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, herpolhodeColor);
		ap.setAttribute(CommonAttributes.TUBES_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap = angVelCurveSGC.getAppearance();
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, polhodeColor);
		ap.setAttribute(CommonAttributes.TUBES_DRAW, false);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .002);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap = invPlaneGeomSGC.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.FACE_DRAW, true);
		ap.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.TRANSPARENCY, .5);
		ap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
		angVelCurve = new Snake(avpoints);
		angVelCurveSGC.setGeometry(angVelCurve);
		body.addChild(angVelCurveSGC);
		invPlaneCurve = new Snake(velocityPoints);
		invPlaneSGC.setGeometry(invPlaneCurve);
		invPlaneSGC.addChild(invPlaneGeomSGC);
		world.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .02);
		world.getAppearance().setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.POINT_RADIUS, .02);
		world.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, true);
		world.addChild(body);
		world.addChild(invPlaneSGC);
		
		boxSGC = SceneGraphUtility.createFullSceneGraphComponent("box");
		boxSGC.setGeometry(Primitives.box(a, b, c, true));
		boxSGC.getAppearance().setAttribute(CommonAttributes.FACE_DRAW, false);
		boxSGC.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, true);
		boxSGC.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.yellow);
		boxSGC.getAppearance().setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.yellow);
		momentumVectorsSGC = SceneGraphUtility.createFullSceneGraphComponent("momentumVectors");
		momentumLinesSGC = SceneGraphUtility.createFullSceneGraphComponent("momentumLines");
		boxSGC.addChildren(momentumVectorsSGC, momentumLinesSGC);
		momentumVectorsSGC.setVisible(showParticleM);
		ap = momentumVectorsSGC.getAppearance();
		ap = momentumLinesSGC.getAppearance();
		ap.setAttribute("lineShader."+CommonAttributes.TUBE_RADIUS, .005);
		ap.setAttribute("lineShader."+CommonAttributes.TUBES_DRAW, false);
		boxVertices = ((IndexedFaceSet)boxSGC.getGeometry()).getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		System.arraycopy(boxVertices, 0, momentumVertices, 0, 8);
		momentumVertices = Pn.homogenize(null, momentumVertices);
		momentumVectorsFactory = new IndexedLineSetFactory();
		momentumVectorsFactory.setVertexCount(16);
		momentumVectorsFactory.setVertexCoordinates(momentumVertices);
		momentumVectorsFactory.setEdgeCount(8);
		int[][] edges = {{0,8},{1,9},{2,10},{3,11},{4,12},{5,13},{6,14},{7,15}};
		Color c1 = new Color(200, 255, 255), c2 = new Color(255,200,255), c3 = Color.white, c4 = new Color(255,255,200);
		Color[] colors = {c1, c2, c3, c4, c4, c3, c2, c1};
		momentumVectorsFactory.setEdgeIndices(edges);
		momentumVectorsFactory.update();
		basf= new BallAndStickFactory(momentumVectorsFactory.getIndexedLineSet());
		basf.setShowArrows(true);
		basf.setShowBalls(false);
		basf.setStickColor(momVecStickColor);
		basf.setArrowColor(momVecArrowColor);
		basf.setStickRadius(.02);
		basf.setArrowScale(.03);
		basf.setArrowSlope(2.0);
		basf.setArrowPosition(1.0);
		basf.update();
		momentumVectorsSGC.addChild(basf.getSceneGraphComponent());
		
		for (int i =0; i<8; ++i)	{
			double[] lineCoords = PlueckerLineGeometry.lineFromPoints(null, momentumVertices[i], momentumVertices[i+8]);
			momVecLines[i] = 
					LineUtility.sceneGraphForLine(null, lineCoords, null, 100.0);		
			momentumLinesSGC.addChild(momVecLines[i]);
			momVecLines[i].setAppearance(new Appearance());
			momVecLines[i].getAppearance().setAttribute("lineShader.diffuseColor", colors[i]);
		}
		ode = new RBMOdeMatrix(this);
		setMoments(getMomentsFromBox(a,b,c));
		getBodySceneGraphRepresentation().addChild(boxSGC);

		init();
		setMoments(moments);
		setAngularVelocity(angularVelocity);
		

	}
	
	/**
	 * Calculate the moments of inertia for a solid box with dimensions (a,b,c)
	 * See for example {@see <a href="http://www.physics.ubc.ca/~birger/p206l19/index.html>this derivation</a>}.
	 * We return a set of moments which are proportional to the actual values, 
	 * which is equivalent to choosing a mass density for the box.
	 */
	protected double[] getMomentsFromBox(double a, double b, double c) {
		// normalize by forcing the box to have volume 1
		double f = 1.0/Math.sqrt(a*b*c);
		a *= f; b *= f; c *= f;
		return new double[]{b*b+c*c, a*a+c*c, a*a+b*b};
	}

	public SceneGraphComponent getBodySceneGraphRepresentation()	{
		return body;
	}
	
	public SceneGraphComponent getSceneGraphRepresentation()	{
		return world;
	}
	
	private int newlimit = limit;
	
	public void reset()	{
		System.err.println("In reset");
//		System.err.println("MW = "+Rn.toString(Rn.matrixTimesVector(null, getCurrentPosition(), getMomentum())));
		double[] m = getCurrentPosition();
		double[] mom = Rn.matrixTimesVector(null, m, getMomentum());
		double[] vel = Rn.matrixTimesVector(null, m, getVelocity());
		double f = Rn.innerProduct(mom, vel)/Rn.innerProduct(mom, mom);
		
		MatrixBuilder.euclidean().
			translate(Rn.times(null, f, mom)).
			rotateFromTo(zaxis, mom).
			assignTo(invPlaneGeomSGC);
		
		if (newlimit != limit)	{
			System.err.println("Reallocating snake "+newlimit);
			limit= newlimit;
			velocityPoints = new double[limit][3];
			invPlaneCurve = new Snake(velocityPoints);
			invPlaneSGC.setGeometry(invPlaneCurve);
		}
		count = 0;
		System.err.println("Leaving reset");
		_update();
		
	}
	
	//  keep the scene graph up-to-date.
	public void update()	{
		if (!running) return;
		_update();
		
	}

	protected void _update() {
		if (limit==0) return;
		ode.update();
		setAngularVelocity(angularVelocity);
		body.getTransformation().setMatrix(currentPosition);
		// create transform for inertia ellipsoid each time
		// use the angular velocity to determine the energy, hence the size of the inertia ellipsoid
		
		double m1 = moments[0], m2 = moments[1], m3 = moments[2];	
		double d = Rn.innerProduct(angularMomentum, angularVelocity);
		double scale = Math.sqrt(d);
		double[] ellipsoidScale = {Math.sqrt(1/m1),Math.sqrt(1/m2),Math.sqrt(1/m3)};
		MatrixBuilder.euclidean().scale(scale).scale(ellipsoidScale).assignTo(ellipsoid);

		// update the positions of the axes
		double[][] points = {angularVelocity, Rn.times(null, -1, angularVelocity)};
//		if (angularVelocityBody.getGeometry() == null) 
		if (cutVB) {
			avb = IndexedLineSetUtility.createCurveFromPoints(avb, points, false);
			angularVelocityBody.setGeometry(avb);			
		} else {
			points = Pn.homogenize(null, points);
			double[] lc = PlueckerLineGeometry.lineFromPoints(null,points[0], points[1]);
			LineUtility.sceneGraphForLine(angularVelocityBody, lc, null, 25.0);				
		}
		double[] scaledAM = Rn.times(null, 1/scale, angularMomentum);
		points = new double[][]{scaledAM, Rn.times(null, -1, scaledAM)};
		amb = IndexedLineSetUtility.createCurveFromPoints(amb, points, false);
		//if (angularMomentumBody.getGeometry() == null) 
			angularMomentumBody.setGeometry(amb);
		double[] m = getCurrentPosition();
		double[] vel = Rn.matrixTimesVector(null, m, getVelocity());
		velocityPoints[count%limit] = vel;
		int[] snakeinfo = invPlaneCurve.getInfo();
		snakeinfo[0] = (count < limit) ? 0 : (count+1)%limit;
		snakeinfo[1] = (count < limit) ? count : limit;
		invPlaneCurve.update();
		avpoints[count%avlimit] = getVelocity().clone();
		snakeinfo = angVelCurve.getInfo();
		snakeinfo[0] = (count < avlimit) ? 0 : (count+1)%avlimit;
		snakeinfo[1] = (count < avlimit) ? count : avlimit;
		angVelCurve.update();
		count++;
		
		if (showParticleM) 	{
			for (int i = 0; i<8; ++i)	{
				double[] pos = boxVertices[i];
				double[] dir = Rn.times(null, vectorScale, Rn.crossProduct(null, angularVelocity, pos));
				Rn.add(momentumVertices[i+8], momentumVertices[i], dir);
				momentumVectorsFactory.setVertexCoordinates(momentumVertices);
				momentumVectorsFactory.update();
				basf.update();
				double[] lineCoords = PlueckerLineGeometry.lineFromPoints(null, momentumVertices[i], momentumVertices[i+8]);
				LineUtility.sceneGraphForLine(momVecLines[i], lineCoords, null, 1000.0);	
			}
			}
	}
	
	JPanel insp = new JPanel();
	Box vbox = Box.createVerticalBox();
	private JCheckBox runB, showVs, showMs, showV, rotateOnly,  showPol, showIP, showEll, showM, cutV;
	private JButton  axisR;
	double[] angVelOld = null;
	public Component getInspector() {
		final TextSlider.IntegerLog pathLengthSlider = new TextSlider.IntegerLog(
				"path length", SwingConstants.HORIZONTAL, 1, 100000,
				limit);
		pathLengthSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double tmp = pathLengthSlider.getValue();
				newlimit = (int) tmp;
			}
		});		
		vbox.add(pathLengthSlider);
		insp.add(vbox);
		Box hbox = Box.createHorizontalBox();
		vbox.add(hbox);
		runB = new JCheckBox("Run");
		hbox.add(runB);
		runB.setSelected(isRunning());
		runB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				setRunning(foo);
			}
			
		});
		showVs = new JCheckBox("Show Vs");
		hbox.add(showVs);
		showVs.setSelected(momentumVectorsSGC.isVisible());
		showVs.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				momentumVectorsSGC.setVisible(foo);
				}
			
		});
		showMs = new JCheckBox("Show Ms");
		hbox.add(showMs);
		showMs.setSelected(momentumLinesSGC.isVisible());
		showMs.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				momentumLinesSGC.setVisible(foo);
				}
			
		});
		showV = new JCheckBox("Show V");
		hbox.add(showV);
		showV.setSelected(angularVelocityBody.isVisible());
		showV.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				angularVelocityBody.setVisible(foo);
				}
			
		});
		showM = new JCheckBox("Show M");
		hbox.add(showM);
		showM.setSelected(angularMomentumBody.isVisible());
		showM.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				angularMomentumBody.setVisible(foo);
				}
			
		});
		cutV = new JCheckBox("Cut V");
		hbox.add(cutV);
		cutV.setSelected(cutVB);
		cutV.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				cutVB = foo;
				update();
				}
			
		});
		axisR = new JButton("Axis Rot");
		hbox.add(axisR);
		axisR.addActionListener(new ActionListener() {
			int which = 0;
			double[][] axes = {{1/moments[0],0,0},{0,1/moments[1],0},{0,0,1/moments[2]}};
			public void actionPerformed(ActionEvent e) {
				
				
					angVelOld = angularVelocity.clone();
					setAngularVelocity(axes[(which++)%3]);
//				} else if (angVelOld != null){
//					setAngularVelocity(angVelOld);
				}
			
		});
		final SceneGraphComponent[] list = {momentumVectorsSGC,
				momentumLinesSGC,
			ellipsoid, 
			//angularVelocityBody, 
			angularMomentumBody,
			angVelCurveSGC,
			invPlaneSGC};
		
		hbox = Box.createHorizontalBox();
		vbox.add(hbox);
		rotateOnly = new JCheckBox("Only rotate");
		hbox.add(rotateOnly);
		rotateOnly.setSelected(false);
		rotateOnly.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				for(int i = 0; i<list.length; ++i) {
					list[i].setVisible(!foo);
					}
				}
			
		});
		showPol = new JCheckBox("Show Pol");
		hbox.add(showPol);
		showPol.setSelected(angVelCurveSGC.isVisible());
		showPol.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				angVelCurveSGC.setVisible(foo);
				}
			
		});
		showIP = new JCheckBox("Show IP");
		hbox.add(showIP);
		showIP.setSelected(invPlaneSGC.isVisible());
		showIP.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				invPlaneSGC.setVisible(foo);
				}
			
		});
		showEll = new JCheckBox("Show Ell");
		hbox.add(showEll);
		showEll.setSelected(ellipsoid.isVisible());
		showEll.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean foo =  ((JCheckBox) e.getSource()).isSelected();
				ellipsoid.setVisible(foo);
				}
			
		});

		vbox.add(ode.getInspector());
		return insp;
	}
	
	public void init() {}

	public boolean isRunning() {
		return running;
	}

	public void setRunning(boolean running) {
		this.running = running; 
		runB.setSelected(running);
	}

	public double[] getMomentum() {
		return angularMomentum;
	}

	public void setAngularMomentum(double[] angularMomentum) {
		this.angularMomentum = angularMomentum.clone();
		angularVelocity = Rn.matrixTimesVector(angularVelocity, invInertiaTensor, angularMomentum);
		System.err.println("Setting momentum = "+Rn.toString(Rn.matrixTimesVector(null, getCurrentPosition(), getMomentum())));
	}

	public double[] getVelocity() {
		return angularVelocity;
	}

	public void setAngularVelocity(double[] angularVelocity) {
		this.angularVelocity = angularVelocity.clone();
		angularMomentum = Rn.matrixTimesVector(angularMomentum, inertiaTensor, angularVelocity);
//		System.err.println("MW = "+Rn.toString(Rn.matrixTimesVector(null, getCurrentPosition(), getMomentum())));
		//		ode.setAngularVelocity(angularVelocity);
	}

	public double[] getMoments()	{
		return moments;
	}
	
	public void setMoments(double[] moments)	{
		System.arraycopy(moments, 0, this.moments, 0, 3);
		inertiaTensor = Rn.diagonalMatrix(null,moments);
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
		update();
	}

	public double[] getCurrentPosition() {
		return currentPosition;
	}

	public void setCurrentPosition(double[] matrix) {
		System.arraycopy(matrix, 0, currentPosition, 0, 16);
		
	}

	public double[] getDims() {
		return new double[]{1,1,1};
	}

	public int getMetric() {
		return Pn.EUCLIDEAN;
	}
	
}
