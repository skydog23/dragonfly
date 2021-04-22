package dragonfly.dynamen.attic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Timer;

import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.SceneGraphComponent;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;

abstract public class AbstractRigidBodyODE2D {

	double[] velocity = {1,1,1}, momentum = {1,1,1}, moments = {1,1,1}, 
	momentsWithMass = {1,1,1};
	transient double[] dims;
	double mass = 1.0;
	transient double[] inertiaTensor, invInertiaTensor, motion = Rn.identityMatrix(4);
	protected double time = 0;

	Timer moveit;
	ActionListener doOneStep;
	ODE ode;
	Extrap extrap;
	double deltaT = .01;
	boolean runMotion = false;
	double[] solution = getInitialSolution();

	SceneGraphComponent object, parent, scale;

	public AbstractRigidBodyODE2D()	{
		
		updateInertiaTensor();
		updateVelocity();
		dims = dimensionsFromMoments(moments);
		createSceneGraphRepn();				
		update();

		ode = getODE();
		extrap = new Extrap(solution.length);
		extrap.setAbsTol(10E-10);
		doOneStep = getOneStepper();
		moveit = new Timer(20, doOneStep);
		
	}

	int signature = Pn.ELLIPTIC;
	public int getSignature() {
		return signature;
	}

	public void setSignature(int signature) {
		this.signature = signature;
	}
	
	private double[] dimensionsFromMoments(double[] moments2) {
		double[] dims = new double[3];
		double M = (moments[0] + moments[1] + moments[2])/2;
		for (int i = 0; i<3;++i)	
			dims[i] = Math.sqrt(M-moments[i]);
		return dims;
	}

	protected ActionListener getOneStepper()	{
		return new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				doOneStep();
				broadcastChange();
			}
			
		};
	}

	abstract double[] getInitialSolution();
	abstract void doOneStep();
	
	abstract protected ODE getODE();
	
	abstract protected void resetMotion();
	
	protected void updateInertiaTensor() {
		inertiaTensor = Rn.diagonalMatrix(null, Rn.times(null, mass, moments));
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
		System.err.println("Inertia tensor = "+Rn.matrixToString(inertiaTensor));
	}
	
	protected void updateVelocity() {
		velocity = Rn.matrixTimesVector(velocity, invInertiaTensor, momentum);				
//		System.err.println("momentum = "+Rn.toString(momentum));
//		System.err.println("velocity = "+Rn.toString(velocity));
	}

	public SceneGraphComponent getSceneGraphRepresentation()	{
		return parent;
	}
	
	public void update()	{
		updateSceneGraphRepn();
	}

	abstract protected void updateSceneGraphRepn();

	abstract protected void createSceneGraphRepn();
	
	public double getMass() {
		return mass;
	}

	public void setMass(double mass) {
		this.mass = mass;
		Rn.times(momentsWithMass, mass, moments);
		updateInertiaTensor();
		updateVelocity();
	}

	public double[] getMoments() {
		return moments;
	}

	public void setMoments(double[] m) {
//		this.moments = moments;
		moments[0] = signature == Pn.EUCLIDEAN ? 1 : m[0];
		moments[1] = signature == Pn.EUCLIDEAN ? 1 : m[1];
		moments[2] = m[2];
		updateDimensions();
		updateInertiaTensor();
		updateVelocity();	
	}

	private void updateDimensions() {
		dims = dimensionsFromMoments(moments);
	}

	public double[] getMomentum() {
		return momentum.clone();
	}

	public void setMomentum(double[] mom) {
		System.arraycopy(mom, 0, momentum, 0, 3);
		updateVelocity();
	}

	public double[] getVelocity() {
		return velocity;
	}
	
	public void setVelocity(double[] v) {
		velocity = v;
		momentum = Rn.matrixTimesVector(momentum, inertiaTensor, velocity);
//		System.err.println("momentum = "+Rn.toString(momentum));
//		System.err.println("velocity = "+Rn.toString(velocity));
	}

	public boolean isRunMotion() {
		return runMotion;
	}

	public void setRunMotion(boolean b) {
		runMotion = b;
		if (runMotion) moveit.start();
		else moveit.stop();
	}

	public double getDeltaT() {
		return deltaT;
	}

	public void setDeltaT(double deltaT) {
		this.deltaT = deltaT;
	}

	public double[] getMotion() {
		return motion;
	}

	public SceneGraphComponent getObject() {
		return object;
	}

	public void setObject(SceneGraphComponent sgc) {
		if (object != null) parent.removeChild(object);
		object = sgc;
		parent.addChild(object);
	}

	Vector<ActionListener> listeners = new Vector<ActionListener>();
	

	public void addListener(ActionListener l)	{
		if (listeners.contains(l)) return;
		listeners.add(l);
		//JOGLConfiguration.theLog.log(Level.INFO,"SelectionManager: Adding geometry listener"+l+"to this:"+this);
	}
	
	public void removeSelectionListener(ActionListener l)	{
		listeners.remove(l);
	}

	public void broadcastChange()	{
		if (listeners == null) return;
		//SyJOGLConfiguration.theLog.log(Level.INFO,"SelectionManager: broadcasting"+listeners.size()+" listeners");
		if (!listeners.isEmpty())	{
			ActionEvent ae = new ActionEvent(this, 0, "");
			//JOGLConfiguration.theLog.log(Level.INFO,"SelectionManager: broadcasting"+listeners.size()+" listeners");
			for (int i = 0; i<listeners.size(); ++i)	{
				ActionListener l = listeners.get(i);
				l.actionPerformed(ae);
			}
		}
	}


}
