package dragonfly.dynamen;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Vector;

import charlesgunn.math.BiquaternionUtility;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jtem.projgeom.PlueckerLineGeometry;

public class RigidBody3D implements RigidBodyInterface {

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
	// work with a rectangular box with the following dimensions 
	transient double[]  boxDims = new double[4];
	double mass = 1;
	transient double[] inertiaTensor = Rn.diagonalMatrix(null, moments), 
		invInertiaTensor = Rn.inverse(null, inertiaTensor), 
		motion = Rn.identityMatrix(4), 
		motionDual = Rn.identityMatrix(4);
	protected double time = 0;
	protected RBMOde ode;
	int metric;
	boolean runMotion = false;

	int count = 0;

	public RigidBody3D()	{
		this(Pn.EUCLIDEAN);
	}
	
	public RigidBody3D(int sig)	{
		updateInertiaTensor();
		updateVelocity();

		ode = new RBMOdeBiquaternion3D(this); 
		
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

	public void setMotionMatrix(double[] motion) {
		this.motion = motion;
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
		for (int i = 0; i<6; ++i) moments[i] = m[i];
		System.err.println("moments = "+Rn.toString(moments));
		updateInertiaTensor();
	}

	public double[] getMomentum() {
		return momentum;
	}
	
	public double getEnergy()	{
		velocity = BiquaternionUtility.diagonalMatrixTimesVector(
				velocity, invInertiaTensor, PlueckerLineGeometry.dualizeLine(null, momentum));
		return Rn.innerProduct(velocity, PlueckerLineGeometry.dualizeLine(null, momentum));
	}

	public void setMomentum(double[] mom) {
		System.arraycopy(mom, 0, momentum, 0, 6);
		System.err.println("setting momentum = "+Rn.toString(momentum));
		updateVelocity();
	}

	public double[] getVelocity() {
		return velocity;
	}
	
	public void setVelocity(double[] v) {
		System.arraycopy(v, 0, velocity, 0, 6);
//		momentum = Rn.matrixTimesVector(momentum, inertiaTensor, velocity);
		updateMomentum();
//		System.err.println("set velocity\n\tvelocity = "+Rn.toString(velocity));
//		System.err.println("\tmomentum = "+Rn.toString(momentum));
//		System.err.println("velocity = "+Rn.toString(velocity));
	}

	private void updateMomentum() {
		momentum = BiquaternionUtility.diagonalMatrixTimesVector(
				momentum, inertiaTensor, velocity);
		PlueckerLineGeometry.dualizeLine(momentum, momentum);
	}

	// when the momentum is set, this is called
	protected void updateVelocity() {
//		velocity = Rn.matrixTimesVector(velocity, invInertiaTensor, PlueckerLineGeometry.dualizePlueckerLine(null, momentum));	
		velocity = BiquaternionUtility.diagonalMatrixTimesVector(
				velocity, invInertiaTensor, PlueckerLineGeometry.dualizeLine(null, momentum));
		if (ode != null) ode.setVelocity(velocity);
		broadcastChange();
	}
	
	public void update()	{
		doOneStep();
	}

	public double[] getDims() {
		return boxDims;
	}

	public void setDims(double[] d) {
		boxDims = d.clone();
	}
	
	protected void updateInertiaTensor() {
		inertiaTensor = Rn.diagonalMatrix(null, Rn.times(null, mass, moments));
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
		// here we should have the option to update the momentum
//		System.err.println("before update\n\tvelocity = "+Rn.toString(velocity));
//		System.err.println("\tmomentum = "+Rn.toString(momentum));
		updateVelocity();
		System.err.println("after update\n\tvelocity = "+Rn.toString(velocity));
		System.err.println("\tmomentum = "+Rn.toString(momentum));
	}
	

	protected void doOneStep() {
		ode.update();
		if (metric != Pn.EUCLIDEAN && count++ % 10 == 0)
			P3.orthonormalizeMatrix(motion, motion, 10E-8, metric);
		setVelocity(velocity);
		motionDual = Rn.transpose(null, Rn.inverse(null, motion));
		if (metric == Pn.EUCLIDEAN) {motionDual[12] = motionDual[13] = motionDual[14] = 0.0; }
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

	public void setBoxDims(double[] d, int metric) {
		boxDims = d.clone();
		setMoments(RigidBody3D.wxyzMomentsFromXZYWDims(d, metric));
	}

	// this is a bit tricky, wrt coordinate system.
		// in this class coordinates are xyzw but all classes which deal
		// with 6D moments are based on wxyz coordinates.  So
		public static  double[] wxyzMomentsFromXZYWDims(double[] dim, int metric)	{
			double[] moms = new double[6];
			double[] sqs = new double[4];
			double[] wxyzDims = {dim[3], dim[0], dim[1], dim[2]};
			for (int i =0; i<4; ++i) sqs[i] = wxyzDims[i]*wxyzDims[i];
			moms[0] = sqs[3] + sqs[2];
			moms[1] = sqs[3] + sqs[1];
			moms[2] = sqs[2] + sqs[1];
			moms[3] = metric*sqs[3] + sqs[0];
			moms[4] = metric*sqs[2] + sqs[0];
			moms[5] = metric*sqs[1] + sqs[0];
			// normalize in some weird way
	//		double c = dim[0]*dim[1]*dim[2]/(Math.pow(dim[3],3));
	//		Rn.times(moms, c, moms);
			// TODO instrument following normalization
			//Rn.setToLength(moms, moms, 6);
			return moms;
		}
	

}
