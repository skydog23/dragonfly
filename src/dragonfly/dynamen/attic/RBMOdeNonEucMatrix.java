package dragonfly.dynamen.attic;

import de.jreality.math.Rn;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;
import dragonfly.dynamen.RBMOde;
import dragonfly.dynamen.RigidBodyInterface;

public class RBMOdeNonEucMatrix extends RBMOde {

	final static double[] matrixSolution = {1,0,0, 0,1,0, 0,0,1, 0,0,0};
	public RBMOdeNonEucMatrix(RigidBodyInterface rbs)	{
		super(rbs);
		ode = new ODE() {
			// first 9 entries represent 3x3 euclidean rotation matrix A,
			// last 3 represent the angular velocity w
			// Let S be the skew 3x3 matrix representing the linear map L(v) = w X v
			// Then the differential equations to solve are:
			// A' = A.S 
			// w' = a X w
			double[] matrixAlone = new double[9];
			public void eval(double t, double[] x, double[] y) {
				double[] moments = rigidBodySimulator.getMoments();
				int metric = rigidBodySimulator.getMetric();
				double X=x[velocityOffset], Y= x[velocityOffset+1], Z = x[velocityOffset+2];
				double[] vel = {0,-Z,Y, Z,0,-X,  -metric*Y,metric * X,0};
				System.arraycopy(x, 0, matrixAlone, 0, velocityOffset);
				double[] result = Rn.times(null, matrixAlone, vel);
				System.arraycopy(result, 0, y, 0, velocityOffset);
				y[velocityOffset] = ((moments[1] - metric * moments[2])/moments[0]) * Y*Z;
				y[velocityOffset+1] = ((metric * moments[2] - moments[0])/moments[1]) * Z*X;
				y[velocityOffset+2] = ((moments[0]-moments[1])/moments[2]) * X*Y;
			}

			public int getNumberOfEquations() {
				return 12;
			}
		};
		extrap = new Extrap(ode.getNumberOfEquations());
//		extrap.setAbsTol(10E-10);
		solution = matrixSolution.clone();
		velocityOffset = 9;
	}
	private static double[] swapZW = new double[]{1,0,0,0,  0,1,0,0,   0,0,0,1,  0,0,1,0};

	@Override
	public void update() {
		System.arraycopy(rigidBodySimulator.getVelocity(), 0, solution, velocityOffset, 3);
		double[] m = rigidBodySimulator.getMotionMatrix();
//		for (int i = 0; i<3; ++i)
//			for (int j= 0; j<3; ++j)	
//				solution[3*i+j]=m[4*i+j];
		extrap.odex(ode, solution, 0,timeStep);	
//		System.err.println("soln = "+Rn.toString(solution));
		double[] tmp = Rn.identityMatrix(4);
		for (int i = 0; i<3; ++i) for (int j =0; j<3; ++j)
			tmp[i*4+j] = solution[i*3+j];
		Rn.conjugateByMatrix(m, tmp, swapZW);
		System.arraycopy(solution, velocityOffset, rigidBodySimulator.getVelocity(), 0, 3);
//		System.err.println("matrix = "+Rn.matrixToString(m));
	}

	@Override
	public void reset() {
		solution = matrixSolution.clone();
	}

	@Override
	public void setVelocity(double[] velocity) {
		// TODO Auto-generated method stub
		
	}

	

}
