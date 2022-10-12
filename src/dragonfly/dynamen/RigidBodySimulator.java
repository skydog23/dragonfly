package dragonfly.dynamen;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;
import dragonfly.dynamen.ws08.RBMOde;
import dragonfly.dynamen.ws08.RBMOdeMatrix;
import dragonfly.dynamen.ws08.RigidBodyInterface;

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
public  class RigidBodySimulator implements RigidBodyInterface {
	
	protected SceneGraphComponent ellipsoid, body, angularVelocityBody, angularMomentumBody;
	protected double[] moments = {1,1,1};
	protected double[] inertiaTensor, invInertiaTensor;
	protected double[] angularVelocity = {0,0,1}, 
		angularMomentum = new double[3];
	RBMOde ode;
	protected double[] currentPosition = Rn.identityMatrix(4);
	protected IndexedLineSet avb = null, amb = null;
	protected boolean running = true;
	public RigidBodySimulator()	{
		// create basic scene graph
		body = SceneGraphUtility.createFullSceneGraphComponent("RBSimulator body");
		ellipsoid = SceneGraphUtility.createFullSceneGraphComponent("ellipsoid");
		ellipsoid.addChild(Primitives.wireframeSphere());
		body.addChild(ellipsoid);
		angularVelocityBody = SceneGraphUtility.createFullSceneGraphComponent("angVelBody");
		angularVelocityBody.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", new Color(200, 150,0));
		body.addChild(angularVelocityBody);

		angularMomentumBody = SceneGraphUtility.createFullSceneGraphComponent("angMomBody");
		angularMomentumBody.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", new Color(255, 50,0));
		body.addChild(angularMomentumBody);				
		body.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);

		ode = new RBMOdeMatrix(this);
		init();
		setMoments(moments);
		setAngularVelocity(angularVelocity);
	}
	
	public SceneGraphComponent getSceneGraphRepresentation()	{
		return body;
	}
	
	public SceneGraphComponent getBodySceneGraphRepresentation()	{
		return body;
	}
	
	public void reset()	{
		
	}
	JPanel insp = new JPanel();
	Box vbox = Box.createVerticalBox();
	private JCheckBox runB;
	public Component getInspector() {
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
		vbox.add(ode.getInspector());
		return insp;
	}
	
	public void init() {}
	//  keep the scene graph up-to-date.
	public void update()	{
		if (!running) return;
		// which we apply to the differential equation contained in the rigid body simulator
		_update();
		
	}

	protected void _update() {
		ode.update();
		setAngularVelocity(angularVelocity);
		body.getTransformation().setMatrix(currentPosition);
		// create transform for inertia ellipsoid each time
		// use the angular velocity to determine the energy, hence the size of the inertia ellipsoid
		
		double m1 = moments[0], m2 = moments[1], m3 = moments[2];	
		double[] ellipsoidScale = {Math.sqrt(1/m1),Math.sqrt(1/m2),Math.sqrt(1/m3)};
		double d = Rn.innerProduct(angularMomentum, angularVelocity);
		double scale = Math.sqrt(d);
		MatrixBuilder.euclidean().scale(scale).scale(ellipsoidScale).assignTo(ellipsoid);

		// update the positions of the axes
		double[][] points = {angularVelocity, Rn.times(null, -1, angularVelocity)};
		avb = IndexedLineSetUtility.createCurveFromPoints(avb, points, false);
		if (angularVelocityBody.getGeometry() == null) angularVelocityBody.setGeometry(avb);
		double[] scaledAM = Rn.times(null, 1/scale, angularMomentum);
		points = new double[][]{scaledAM, Rn.times(null, -1, scaledAM)};
		amb = IndexedLineSetUtility.createCurveFromPoints(amb, points, false);
		if (angularMomentumBody.getGeometry() == null) angularMomentumBody.setGeometry(amb);
	}

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
