package dragonfly.dynamen;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.Vector;

import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;

public class RigidBody2D implements RigidBodyInterface {

	{
		Locale.setDefault(Locale.US);
	}
double[] velocity = {1,1,1}, momentum = {1,1,1}, moments = {1,1,1};
	transient double[]  boxDims = new double[3];
	transient double[] inertiaTensor, 
		invInertiaTensor, 
		motion = Rn.identityMatrix(4), 
		motionDual = Rn.identityMatrix(4);
	protected double time = 0;
	double mass = 1.0;

	protected RBMOde ode;
	int metric;
	boolean runMotion = false;

	int count = 0;
	double delta = .01;
	double cc = .25;

	public RigidBody2D()	{
		this(Pn.EUCLIDEAN);
	}
	
	public RigidBody2D(int sig)	{
		updateInertiaTensor();
		updateVelocity();
		dimensionsFromMoments() ;

		ode = new RBMOdeBiquaternion2D(this); 

		metric = sig;
	}
	public int getMetric() {
		return metric;
	}

	public void setMetric(int metric) {
		this.metric = metric;
	}
	
	public double[] getMotionMatrix() {
		return motion;
	}

	public double[] getMotionDual() {
		return motionDual;
	}

	public void setCurrentPosition(double[] motion) {
		this.motion = motion;
	}

	
	public double[] getDims() {
		return boxDims;
	}


	public double[] getMoments() {
		return moments;
	}

	public void setMoments(double[] m) {
//		this.moments = moments;
		moments = m.clone();
		System.err.println("Moments of inertia = "+Rn.toString(moments));
//		moments[0] = metric == Pn.EUCLIDEAN ? 1 : m[0];
//		moments[1] = metric == Pn.EUCLIDEAN ? 1 : m[1];
//		moments[2] = m[2];
		//dimensionsFromMoments();
		updateInertiaTensor();
		updateVelocity();	
	}

	public void setMass(double d) {
		mass = d;
		updateInertiaTensor();
	}
	
	public double getMass() {
		// TODO Auto-generated method stub
		return mass;
	}

	public double[] getMomentum() {
		return momentum;
	}


	public void setMomentum(double[] mom) {
		System.arraycopy(mom, 0, momentum, 0, 3);
		if (momentum[2] < 0 && metric == Pn.ELLIPTIC) Rn.times(momentum, -1, momentum);
		updateVelocity();
	}

	public double[] getVelocity() {
		return velocity;
	}
	
	public void setVelocity(double[] v) {
		velocity = v;
		momentum = Rn.matrixTimesVector(momentum, inertiaTensor, velocity);
		System.err.println("momentum = "+Rn.toString(momentum));
		System.err.println("velocity = "+Rn.toString(velocity));
	}

//	public boolean isRunMotion() {
//		return runMotion;
//	}
//
//	public void setRunMotion(boolean b) {
//		runMotion = b;
//		if (runMotion) moveit.start();
//		else moveit.stop();
//	}

	private void dimensionsFromMoments() {
		double M = (moments[0] + moments[1] + moments[2])/2;
		for (int i = 0; i<3;++i)	
			boxDims[i] = Math.sqrt(M-moments[i]);
	}

	protected void updateInertiaTensor() {
		inertiaTensor = Rn.diagonalMatrix(null, Rn.times(null, mass, moments));
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
//		System.err.println("Inertia tensor = "+Rn.matrixToString(inertiaTensor));
	}
	

	public void doOneStep() {
		ode.update();
		if (metric != Pn.EUCLIDEAN && count++ % 10 == 0)
			P3.orthonormalizeMatrix(motion, motion, 10E-8, metric);
		setVelocity(velocity);
		motionDual = Rn.transpose(null, Rn.inverse(null, motion));
		if (metric == Pn.EUCLIDEAN) {motionDual[12] = motionDual[13] = motionDual[14] = 0.0; }
	}

	protected void updateVelocity() {
		velocity = Rn.matrixTimesVector(velocity, invInertiaTensor, momentum);				
		broadcastChange();
	}
	
	public void resetMotion() {
		time = 0;
		Rn.setIdentityMatrix(motion);
		Rn.setIdentityMatrix(motionDual);
		updateVelocity();
		ode.reset();
	}
	Vector<ActionListener> listeners = new Vector<ActionListener>();
	

	public void addListener(ActionListener l)	{
		if (listeners.contains(l)) return;
		listeners.add(l);
		//JOGLConfiguration.theLog.log(Level.INFO,"SelectionManager: Adding geometry listener"+l+"to this:"+this);
	}
	
	public void removeListener(ActionListener l)	{
		listeners.remove(l);
	}

	public void broadcastChange()	{
		if (listeners == null || listeners.isEmpty()) return;
			ActionEvent ae = new ActionEvent(this, 0, "");
			//JOGLConfiguration.theLog.log(Level.INFO,"SelectionManager: broadcasting"+listeners.size()+" listeners");
			for (int i = 0; i<listeners.size(); ++i)	{
				ActionListener l = listeners.get(i);
				l.actionPerformed(ae);
			}
	}
	
	public void update()	{
		doOneStep();
	}

	public double[] getInertiaTensor() {
		// TODO Auto-generated method stub
		return null;
	}

	public double[] getInvInertiaTensor() {
		// TODO Auto-generated method stub
		return null;
	}

	public void setDims(double[] d) {
		// TODO Auto-generated method stub
		
	}

	public void setMotionMatrix(double[] d) {
		// TODO Auto-generated method stub
		
	}
	
	public double getEnergy()	{
		return Rn.innerProduct(momentum, velocity);
	}

	public void setBoxDims(double[] d, int metric) {
		boxDims = d.clone();
		setMoments(RigidBody2D.momentsFromDimn(d, metric));
	}

	public static double[] momentsFromDimn(double[] dim, int metric)	{
		double[] moms = new double[3];
		double M = Rn.innerProduct(dim, dim);
		//for (int i = 0; i<3; ++i) 
		moms[0] = metric * dim[1]*dim[1] + dim[2]*dim[2];
		moms[1] = metric * dim[0]*dim[0] + dim[2]*dim[2];
		moms[2] = dim[0]*dim[0] + dim[1]*dim[1];
//		Rn.setToLength(moms, moms, 3);
		return moms;
	}



}
