package dragonfly.dynamen.attic;

import java.awt.Color;

import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Quaternion;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.SceneGraphUtility;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;

public class RigidBodySO3ODE {
	double[] inertiaTensor, invInertiaTensor;
	double m1, m2, m3;
	ODE ode;
	double[] angularVelocity = {0,0,1}, angularMomentum = new double[3];
	private SceneGraphComponent ellipsoid, body, angularVelocityBody, angularMomentumBody, avCurveSGC;
	IndexedLineSet avb = null, amb = null;
	public RigidBodySO3ODE(double[] moments)	{
		super();
		m1 = moments[0];
		m2 = moments[1];
		m3 = moments[2];
		inertiaTensor = Rn.diagonalMatrix(null, moments);
		invInertiaTensor = Rn.inverse(null, inertiaTensor);
		ode = new ODE() {
			// the rigid body motion is determined by two  equations
			// q' = q w			quaternion multiplication
			// w' = A w			Euler equations for angular velocityd*(-(c*x) + b*y + a*z) + c*(d*x + a*y - b*z) +
		double scale = .5;
		public void eval(double t, double[] x, double[] y) {
			Quaternion vel = new Quaternion(0,x[4], x[5], x[6]);
			Quaternion pos = new Quaternion(x[0], x[1], x[2], x[3]);
			Quaternion nPos = Quaternion.times(null, pos, vel);
			Quaternion.times(nPos, .5, nPos);
			y[0] = nPos.re;
			y[1] = nPos.x;
			y[2] = nPos.y;
			y[3] = nPos.z;
				
			y[4] = ((m2-m3)/m1) * x[5]*x[6];
			y[5] = ((m3-m1)/m2) * x[6]*x[4];
			y[6] = ((m1-m2)/m3) * x[4]*x[5];
		}

		public int getNumberOfEquations() {
			System.err.println("Returning 7");
			return 7;
		}
		
	};
		update();
		
	}
	
	public SceneGraphComponent getSceneGraphRepresentation()	{
		return body;
	}
	
	public void update()	{
		if (body == null)	{
			body = SceneGraphUtility.createFullSceneGraphComponent("body");
			ellipsoid = SceneGraphUtility.createFullSceneGraphComponent("ellipsoid");
			// render inertia ellipsoid
			double scale = Math.pow(Math.sqrt(m1*m2*m3), 1/3.0);
			MatrixBuilder.euclidean().scale(scale).scale(Math.sqrt(1/m1),Math.sqrt(1/m2),Math.sqrt(1/m3)).assignTo(ellipsoid);
			ellipsoid.addChild(Primitives.wireframeSphere());
			//ellipsoid.setGeometry(new Sphere());
			body.addChild(ellipsoid);
			angularVelocityBody = SceneGraphUtility.createFullSceneGraphComponent("angVelBody");
			angularVelocityBody.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", new Color(200, 150,0));
			double[][] points = {angularVelocity, Rn.times(null, -1, angularVelocity)};
			avb = IndexedLineSetUtility.createCurveFromPoints(avb, points, false);
			angularVelocityBody.setGeometry(avb);
			body.addChild(angularVelocityBody);

			angularMomentumBody = SceneGraphUtility.createFullSceneGraphComponent("angMomBody");
			angularMomentumBody.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", new Color(255, 50,0));
			points = new double[][]{angularMomentum, Rn.times(null, -1, angularMomentum)};
			amb = IndexedLineSetUtility.createCurveFromPoints(points, false);
			angularMomentumBody.setGeometry(amb);
			body.addChild(angularMomentumBody);				
		} 
		// update the positions of the axes
		double[][] points = {angularVelocity, Rn.times(null, -1, angularVelocity)};
		avb = IndexedLineSetUtility.createCurveFromPoints(avb, points, false);
		points = new double[][]{angularMomentum, Rn.times(null, -1, angularMomentum)};
		amb = IndexedLineSetUtility.createCurveFromPoints(amb, points, false);

		
	}
	public ODE getODE()	{
		return ode;
	}

	public double[] getAngularMomentum() {
		return angularMomentum;
	}

	public void setAngularMomentum(double[] angularMomentum) {
		this.angularMomentum = angularMomentum;
		angularVelocity = Rn.matrixTimesVector(angularVelocity, invInertiaTensor, angularMomentum);
	}

	public double[] getAngularVelocity() {
		return angularVelocity;
	}

	public void setAngularVelocity(double[] angularVelocity) {
		this.angularVelocity = angularVelocity;
		angularMomentum = Rn.matrixTimesVector(angularMomentum, inertiaTensor, angularVelocity);
	}

}
