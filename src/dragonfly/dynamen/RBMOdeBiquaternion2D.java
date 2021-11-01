package dragonfly.dynamen;

import charlesgunn.math.p5.PlueckerLineGeometry;
import de.jreality.math.Rn;

public class RBMOdeBiquaternion2D extends AbstractRBMOdeBiquaternion {

	public RBMOdeBiquaternion2D(RigidBodyInterface rbs) {
		super(rbs);
	}
	@Override
	public void update() {
		if (firsttime) {
			double[] moments = rigidBodySimulator.getMoments();
			double m1 = moments[0], m2 = moments[1], m3 = moments[2];	
			inertiaTensor = Rn.diagonalMatrix(null, new double[]{1,1,m3, 1, m1, m2});
			invInertiaTensor = Rn.diagonalMatrix(null, new double[]{1,1,1/m3, 1, 1/m1, 1/m2});
			double[] av = rigidBodySimulator.getVelocity();
			double[] p0 = {av[2], av[0], av[1], 0};
			double[] p1 = {av[2], av[0], av[1], 1};
			PlueckerLineGeometry.lineFromPoints(velocityStateD, p0, p1);
			super.init();
			firsttime = false;
		}
		super.update();
		double[] av = rigidBodySimulator.getVelocity();
		av[0] = -velocityStateD[4]; av[1] = velocityStateD[5]; av[2] = velocityStateD[2];
		
	}
}
