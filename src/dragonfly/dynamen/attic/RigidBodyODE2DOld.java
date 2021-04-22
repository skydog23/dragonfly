package dragonfly.dynamen.attic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Timer;

import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;
import dragonfly.dynamen.RigidBodyInterface;

public class RigidBodyODE2DOld  implements RigidBodyInterface {

	
	double[] velocity = {1,1,1}, momentum = {1,1,1}, moments = {1,1,1}, 
	momentsWithMass = {1,1,1};
	transient double[] dims = new double[3];
	double mass = 1.0;
	transient double[] inertiaTensor, invInertiaTensor, motion = Rn.identityMatrix(4), motionDual = Rn.identityMatrix(4);
	protected double time = 0;

	Timer moveit;
	public ODE ode;
	Extrap extrap;
	double timeStep = .01;
	boolean runMotion = false;
	double[] solution = new double[]{1,0,0, 0,1,0, 0,0,1, 0,0,0};
	int metric;

	private static double[] swapZW = new double[]{1,0,0,0,  0,1,0,0,   0,0,0,1,  0,0,1,0};
	int count = 0;
	double delta = .01;

	public RigidBodyODE2DOld()	{
		this(Pn.EUCLIDEAN);
	}
	
	public RigidBodyODE2DOld(int sig)	{
		updateInertiaTensor();
		updateVelocity();
		dimensionsFromMoments() ;

		ode = getODE();
		extrap = new Extrap(solution.length);
		extrap.setAbsTol(10E-10);

		moveit = new Timer(20, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				doOneStep();
				broadcastChange();
			}
			
		});
		
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
		return dims;
	}

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
		moments[0] = metric == Pn.EUCLIDEAN ? 1 : m[0];
		moments[1] = metric == Pn.EUCLIDEAN ? 1 : m[1];
		moments[2] = m[2];
		dimensionsFromMoments();
		updateInertiaTensor();
		updateVelocity();	
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

	public double getTimeStep() {
		return timeStep;
	}

	public void setTimeStep(double deltaT) {
		this.timeStep = deltaT;
	}


	private void dimensionsFromMoments() {
		double M = (moments[0] + moments[1] + moments[2])/2;
		for (int i = 0; i<3;++i)	
			dims[i] = Math.sqrt(M-moments[i]);
	}

	protected void updateInertiaTensor() {
		inertiaTensor = Rn.diagonalMatrix(null, Rn.times(null, mass, moments));
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
		System.err.println("Inertia tensor = "+Rn.matrixToString(inertiaTensor));
	}
	

	protected void doOneStep() {
//		System.err.println("Sol = "+Rn.toString(solution));
		extrap.odex(ode, solution, time,time+timeStep);	
//		Quaternion qm = new Quaternion(solution[0],solution[1], solution[2], solution[3]);
//		Quaternion.quaternionToRotationMatrix(motion, qm);
		double[] tmp = Rn.identityMatrix(4);
		for (int i = 0; i<3; ++i) for (int j =0; j<3; ++j)
			tmp[i*4+j] = solution[i*3+j];
		Rn.conjugateByMatrix(motion, tmp, swapZW);
		tmp = new double[9]; System.arraycopy(solution, 0, tmp, 0, 9);
//		System.err.println("S = "+Rn.motionToString(tmp));
//		System.err.println("M = "+Rn.motionToString(motion));
		if (metric != Pn.EUCLIDEAN && count++ % 10 == 0)
			P3.orthonormalizeMatrix(motion, motion, 10E-8, metric);
		System.arraycopy(solution, 9, velocity, 0, 3);
		setVelocity(velocity);
		System.err.println("soln = "+Rn.toString(solution));
		motionDual = Rn.transpose(null, Rn.inverse(null, motion));
		if (metric == Pn.EUCLIDEAN) {motionDual[12] = motionDual[13] = motionDual[14] = 0.0; }
//		updateSceneGraphRepn();
	}

	static final int offset = 9;
	protected ODE getODE()	{
		if (ode == null) ode = new ODE() {
			// the rigid body motion is determined by two  equations
			// q' = q w			quaternion multiplication
			// w' = A w			Euler equations for angular velocityd*(-(c*x) + b*y + a*z) + c*(d*x + a*y - b*z) +
			double[] foo = new double[offset];
			public void eval(double t, double[] x, double[] y) {
//				y[0] = scale*(-x[1]*x[4] - x[2]*x[5] + x[3]*x[6]);
//				y[1] = scale*( x[0]*x[4] - x[3]*x[5] + x[2]*x[6]);
//				y[2] = scale*( x[3]*x[4] + x[0]*x[5] - x[1]*x[6]);
//				y[3] = scale*(-x[2]*x[4] + x[1]*x[5] + x[0]*x[6]);							
				double X=x[offset], Y= x[offset+1], Z = x[offset+2];
				double[] vel = {0,-Z,Y, Z,0,-X,  -metric*Y,metric * X,0};
				foo = new double[offset];
				System.arraycopy(x, 0, foo, 0, 9);
				double[] result = Rn.times(null, foo, vel);
				System.arraycopy(result, 0, y, 0, 9);
				y[offset+0] = ((moments[1] - metric * moments[2])/moments[0]) * Y*Z;
				y[offset+1] = ((metric * moments[2] - moments[0])/moments[1]) * Z*X;
				y[offset+2] = ((moments[0]-moments[1])/moments[2]) * X*Y;
//				System.err.println("velocity = "+y[9]+" "+y[10]+" "+y[11]);
			}
	
			public int getNumberOfEquations() {
				return solution.length;
			}
		};
		
		return ode;
	}
	protected void updateVelocity() {
			velocity = Rn.matrixTimesVector(velocity, invInertiaTensor, momentum);				
//			updateSceneGraphRepn();
			System.arraycopy(velocity, 0, solution, 9, 3);
			broadcastChange();
	}
	
	public void resetMotion() {
		time = 0;
		Rn.setIdentityMatrix(motion);
		Rn.setIdentityMatrix(motionDual);
		System.arraycopy(Rn.identityMatrix(3), 0, solution, 0, 9);
		updateVelocity();
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

	public void setBoxDims(double[] d, int metric) {
		// TODO Auto-generated method stub
		
	}

	public double getEnergy() {
		// TODO Auto-generated method stub
		return 0;
	}
	

}
