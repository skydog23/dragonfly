package dragonfly.dynamen.attic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import javax.swing.Timer;

import charlesgunn.math.p5.PlueckerLineGeometry;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import dragonfly.dynamen.RBMOde;
import dragonfly.dynamen.RBMOdeBiquaternion3D;
import dragonfly.dynamen.RigidBodyInterface;

public class RigidBody3DOld implements RigidBodyInterface {

	// we deal always with point-based plucker line coordinates.
	//  First, we sometimes think of plucker coordinates as a pair of vectors:
	// {p12, p13, p14, p23, p42, p34} <--> {v1; v2} where v1 = {p12, p13, p14}, v2 = {p34, p42, p23}
	// this means that for a velocity state (say, a rotation around a line l)
	// that the first vector is the angular velocity and the second is a translation
	// for a momentum state, the first vector is the angular momentum and the second
	// is the linear momentum
	// Also, for these coordinates to make sense, have to set the first coordinate as the
	// "homogeneous coordinate" -- instead of the assumption (e.g., in P3) that the last coordinate
	// is the "homogeneous" one (i.e., the "w" coordinate).
	// compatibility check with pre-existing 2D mechanics:
	// so assume that we start with an inf. rotation around the line through (1, x,y,0) parallel to
	// the z-axis.  That is, the line  v = (1, x,y,0) ^ (1,x,y,1) (in homog coord) = 
	// {0,0,1,0,-x,y} = {(0,0,1);(y,-x,0)}
	// In this interpretation, (in the euclidean case), the inertia tensor will have the form
	// A = m diagonal(m1, m2, m3, 1, 1, 1) where {mj} are the moments of inertia and m the mass.  
	// Of course the 
	// resulting momentum state m = Av will be given in plane coordinates, and has then to be
	// converted to point coordinates (by simply reversing the order of the coordinates!). So it
	// will look like {y, -x, 0, m3, 0, 0} = {(y,-x,0);(0,0,m3)}.  This is a horizontal line whose 
	// distance from the origin ("moment wrt origin") is proportional to m3, the angular velocity,
	// as it should be.
	// in the noneuclidean case A = m diagonal(m1,m2,m3,m4,m5,m6) = {m6 y, -m5 x, 0, m3, 0, 0}.
	double x = 0.0, y = 1.0;
	double[] velocity = {0,0,1,0,-x,y}, momentum = {y,-x,0,1,0,0}, moments = {1,1,1,1,1,1};
	transient double[] dims = new double[3];
	double mass = 1.0;
	transient double[] inertiaTensor = Rn.diagonalMatrix(null, moments), 
		invInertiaTensor = Rn.inverse(null, inertiaTensor), 
		motion = Rn.identityMatrix(4), 
		motionDual = Rn.identityMatrix(4);
	protected double time = 0;
	static final double[] rev = new double[36];
	static {
		rev[5] = rev[10] = rev[15] = rev[20] = rev[25] = rev[30] = 1;
	}

	Timer moveit;
	protected RBMOde ode;
	int metric;
	boolean runMotion = false;

	int count = 0;
	double delta = .01;
	double cc = .25;

	public RigidBody3DOld()	{
		this(Pn.EUCLIDEAN);
	}
	
	public RigidBody3DOld(int sig)	{
		updateInertiaTensor();
		updateVelocity();
		dimensionsFromMoments() ;

		ode = new RBMOdeBiquaternion3D(this); 

		moveit = new Timer(20, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				doOneStep();
				broadcastChange();
			}
			
		});
		
		metric = sig;
	}

	public double[] getInertiaTensor() {
		return inertiaTensor;
	}

	public double[] getInvInertiaTensor() {
		return invInertiaTensor;
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
		updateInertiaTensor();
		updateVelocity();
	}

	public double[] getMoments() {
		return moments;
	}

	public void setMoments(double[] m) {
//		this.moments = moments;
		for (int i = 0; i<6; ++i) moments[i] = m[i];
		if (metric == Pn.EUCLIDEAN) {
			for (int i = 0; i<3; ++i) moments[i+3] = 1;
		}
		System.err.println("moments = "+Rn.toString(moments));
		dimensionsFromMoments();
		updateInertiaTensor();
//		updateVelocity();	
	}

	public double[] getMomentum() {
		return momentum;
	}


	public void setMomentum(double[] mom) {
		System.arraycopy(mom, 0, momentum, 0, 6);
//		System.err.println("setting momentum = "+Rn.toString(momentum));
		updateVelocity();
	}

	public double[] getVelocity() {
		return velocity;
	}
	
	public void setVelocity(double[] v) {
		velocity = v;
		momentum = Rn.matrixTimesVector(momentum, inertiaTensor, velocity);
		PlueckerLineGeometry.dualizeLine(momentum, momentum);
//		System.err.println("set velocity: momentum = "+Rn.toString(momentum));
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

	private void dimensionsFromMoments() {
		double[] mom3 = {moments[4], moments[5], moments[2]};
		double M = (mom3[0] + mom3[1] + mom3[2])/2;
		for (int i = 0; i<3;++i)	
			dims[i] = Math.sqrt(M-mom3[i]);
//		System.err.println("Dims = "+Rn.toString(dims));
	}

	protected void updateInertiaTensor() {
		inertiaTensor = Rn.diagonalMatrix(null, Rn.times(null, mass, moments));
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
//		inertiaTensor = Rn.times(null, rev, inertiaTensor);
//		invInertiaTensor = Rn.times(null, invInertiaTensor, rev);
		// a little cheat: assume diagonal form
//		for (int i = 0; i<6; ++i) {
//			if (inertiaTensor[i*6+i] != 0) invInertiaTensor[i*6+i] = 1.0/inertiaTensor[i*6+i];
//			else throw new IllegalStateException("degenerate inertia tensor");
//		}
//		System.err.println("Inertia tensor = "+Rn.matrixToString(inertiaTensor));
//		System.err.println("invInertia tensor = "+Rn.matrixToString(invInertiaTensor));
	}
	

	protected void doOneStep() {
		ode.update();
		if (metric != Pn.EUCLIDEAN && count++ % 10 == 0)
			P3.orthonormalizeMatrix(motion, motion, 10E-8, metric);
		setVelocity(velocity);
		motionDual = Rn.transpose(null, Rn.inverse(null, motion));
		if (metric == Pn.EUCLIDEAN) {motionDual[12] = motionDual[13] = motionDual[14] = 0.0; }
	}

	protected void updateVelocity() {
		velocity = Rn.matrixTimesVector(velocity, invInertiaTensor, PlueckerLineGeometry.dualizeLine(null, momentum));	
//		PlueckerLineGeometry.dualizePlueckerLine(velocity, velocity);
//		System.err.println("momentum = "+Rn.toString(momentum));
//		System.err.println("velocity = "+Rn.toString(velocity));
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
