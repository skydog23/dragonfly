package dragonfly.dynamen.ws08;

import de.jreality.math.Rn;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;

public class RBMOdeMatrix extends RBMOde {

	final static double[] matrixSolution = {1,0,0, 0,1,0, 0,0,1, 0,0,0};
	public RBMOdeMatrix(RigidBodyInterface rbs)	{
		super(rbs);
		if (ode == null) ode = new ODE() {
			// first 9 entries represent 3x3 euclidean rotation matrix A,
			// last 3 represent the angular velocity w
			// Let S be the skew 3x3 matrix representing the linear map L(v) = w X v
			// Then the differential equations to solve are:
			// A' = A.S 
			// w' = a X w
			double[] matrixAlone = new double[9];
			public void eval(double t, double[] x, double[] y) {
				double[] moments = rigidBodySimulator.getMoments();
				double X=x[9], Y= x[10], Z = x[11];
				// create skew operator represent angular velocity as linear map
				double[] vel = {0,-Z,Y, Z,0,-X,  -Y,X,0};
				System.arraycopy(x, 0, matrixAlone, 0, 9);
				double[] result = Rn.times(null, matrixAlone, vel);
				System.arraycopy(result, 0, y, 0, 9);
				y[9] = ((moments[1] - moments[2])/moments[0]) * Y*Z;
				y[10] = ((moments[2] - moments[0])/moments[1]) * Z*X;
				y[11] = ((moments[0] - moments[1])/moments[2]) * X*Y;
			}

			public int getNumberOfEquations() {
				return 12;
			}
		};
		extrap = new Extrap(ode.getNumberOfEquations());
		solution = matrixSolution;
		velocityOffset = 9;
	}

	@Override
	public void update() {
		System.arraycopy(rigidBodySimulator.getVelocity(), 0, solution, velocityOffset, 3);
		double[] m = rigidBodySimulator.getCurrentPosition();
		for (int i = 0; i<3; ++i)
			for (int j= 0; j<3; ++j)	
				solution[3*i+j]=m[4*i+j];
		extrap.odex(ode, solution, 0,timeStep);	
		for (int i = 0; i<3; ++i) 
			for (int j =0; j<3; ++j)
				m[i*4+j] = solution[i*3+j];
		System.arraycopy(solution, velocityOffset, rigidBodySimulator.getVelocity(), 0, 3);
//		System.err.println("cp = "+Rn.toString(solution));
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		
	}

	

}
