package dragonfly.dynamen;

import charlesgunn.math.Biquaternion;
import charlesgunn.math.BiquaternionUtility;
import charlesgunn.math.Biquaternion.Metric;
import de.jreality.math.Quaternion;
import de.jreality.math.Rn;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;
import de.jtem.projgeom.PlueckerLineGeometry;

public class AbstractRBMOdeBiquaternion extends RBMOde {

	// 14 dimensional solution space:  rotor (8D) + velocity (6D)
	final static double[] biquaternionSolution = {1,0,0,0, 0,0,0,0,   0,0,0,0,0,0};
	// This is 0 in the euclidean case.  Regarding dual numbers: \epsilon^2 = metric
	Metric metric = Metric.EUCLIDEAN;
	Biquaternion velocityState = new Biquaternion(metric), 
			momentumState = new Biquaternion(metric);
	// Sometimes we need the velocity and the momentum as double arrays
	double[] velocityStateD = {0,0,1,0,0,0}, 
			momentumStateD = {1,0,0,0,0,0};
	// Finally, the inertia tensor that takes up back and forth between kinematic entities
	//  (velocity, acceleration, twists, etc.) to dynamics ones (momenta, torque, wrenches)
	double[] inertiaTensor = Rn.identityMatrix(6), 
			invInertiaTensor = Rn.identityMatrix(6);
	
	public AbstractRBMOdeBiquaternion(RigidBodyInterface rbs) {
		super(rbs);
		// The ODE class encapsulates a set of first order ODE's  x' = f(x)
		ode = new ODE() {
			// the rigid body motion is determined by the so-called Euler equations
			// on the pair (g, V) where 
			//	g is rotor (rather a path in the rotor group) and is hence 8 dimensional, and 
			// 	V is a bivector (hence 6 dimensional), 
			//  the instantaneous velocity state in body coordinates,
			//  associated to g  according to the equation 
			//     g' = gV	  (essentially V is the velocity "pulled back" to the identity
			//					via g^{-1}g' =: V)
			// The velocity state V has an associated momentum bivector defined by
			//		M = AV   where A is the inertia tensor of the body 
			//  A is a constant symmetric 6x6 matrix which can be diagonalized.
			//  M and V live in different Lie algebras, one that has natural point-based
			//  Pluecker coordinates (the momenta) and one that is naturally plane-based
			//  (the velocity).  This makes for some bookkeeping in the code, 
			//  moving back and forth between these two representations for bivectors.
			//	M and V satisfy the additional Euler equation
			//		M' = [M, V]
			//	where [ , ] is the Lie bracket of the two bivectors
			// the input pair (g, V) are passed in via the array x[14]
			// the output pair (g', V') are returned via the array y[14]
			// This information is then fed to the ode solver
			public void eval(double t, double[] x, double[] y) {
				// this code works for euclidean, spherical, or hyperbolic space
				metric = Metric.metricForCurvature(rigidBodySimulator.getMetric());
				// extract the position: the first 8 elements of x[]
				Biquaternion rotor = new Biquaternion(x, metric);
				// evaluate the first equation g' = gV
				Biquaternion rotorDot = Biquaternion.times(null, rotor, velocityState);
				// write it into the first 8 slots of y
				Biquaternion.asDouble(y, rotorDot);
				// extract the velocity state as a bivector
				Quaternion vqr = new Quaternion(0, x[8], x[9], x[10]);
				Quaternion vqd = new Quaternion(0, x[13], x[12], x[11]);
				velocityState = new Biquaternion(vqr, vqd, metric);
				// extract the bivector (aka line complex) from the biquaternion (its imaginary part)
				// in order to multiply by 6x6 matrix (not available on Biquaternion class)
				Biquaternion.lineComplexFromBivector(velocityStateD, velocityState);
				// calculate the momentum according to M = AV
				// That is, it is the polar of the velocity with respect to inertia tensor A
				BiquaternionUtility.diagonalMatrixTimesVector(momentumStateD, inertiaTensor, velocityStateD);
				// have to convert this dual bivector into a standard bivector 
				// that is, point-based Pluecker coords have to be converted to the plane-based ones.
				PlueckerLineGeometry.dualizeLine(momentumStateD, momentumStateD);
				// convert back to Biquaternion
				momentumState = new Biquaternion(momentumStateD, metric);
				// evaluate the equation M' = [M, V]
				// [M,V] = MV - VM  (using dual quaternion multiplication)
				Biquaternion dd =  Biquaternion.lieBracket(null, momentumState, velocityState);
				// once more we have to flip from the Lie algebra to the co- Lie algebra
				dd.dualize();
				// once more convert to double[] in order to apply 6x6 matrix
				double[] ddd = Biquaternion.lineComplexFromBivector(null, dd);
				// apply inverse of inertia tensor:  V' = A^{-1}M'
				BiquaternionUtility.diagonalMatrixTimesVector(ddd, invInertiaTensor, ddd);
				// copy out the resulting value for V' into y
				System.arraycopy(ddd, 0, y, velocityOffset, 6);
				Biquaternion pro = Biquaternion.times(null, momentumState, velocityState);
				System.err.println("product = "+pro.toString());
			}
	
			public int getNumberOfEquations() {
				return 14;
			}
			
		};
		extrap = new Extrap(ode.getNumberOfEquations());
		velocityOffset = 8;
		reset();
	}
	public static final double[] zplane = {0,0,0,1};
	boolean firsttime = true;
	/**
	 * The update() method gets called to integrate the equations of motion delta t.
	 */
	@Override
	public void update() {
		// the odex() call will invoke the above eval() method and then use some ODE solver
		// to take a step along the solution curve.
		extrap.odex(ode, solution, 0, timeStep);
		// Take the result and update the various fields that control the display
		Biquaternion motion = new Biquaternion(solution, metric);
		Biquaternion.asDouble(solution, motion);
		Biquaternion.matrixFromBiquaternion(rigidBodySimulator.getMotionMatrix(), motion);	
		System.arraycopy(solution, velocityOffset, velocityStateD, 0, 6);
	}
	
	public void init() {
		System.arraycopy(velocityStateD, 0, solution, velocityOffset, 6);
		velocityState = new Biquaternion(velocityStateD,Metric.metricForCurvature(rigidBodySimulator.getMetric()));
		BiquaternionUtility.diagonalMatrixTimesVector(momentumStateD, inertiaTensor, velocityStateD);
		PlueckerLineGeometry.dualizeLine(momentumStateD, momentumStateD);
	}
	
	@Override
	public void reset() {
		solution = biquaternionSolution.clone();	
//		velocityState = new Biquaternion(new double[]{0,0,1,0,0,0},Metric.metricForCurvature(rigidBodySimulator.getMetric()));
		firsttime = true;
		init();
	}
	@Override
	public void setVelocity(double[] velocity) {
		System.arraycopy(velocity, 0, solution, velocityOffset, 6);
	}

}
