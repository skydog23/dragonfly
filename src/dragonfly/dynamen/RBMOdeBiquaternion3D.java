package dragonfly.dynamen;


public class RBMOdeBiquaternion3D extends AbstractRBMOdeBiquaternion {

	public RBMOdeBiquaternion3D(RigidBodyInterface rbs) {
		super(rbs);
	}
	@Override
	public void update() {
		if (firsttime) {
			inertiaTensor = rigidBodySimulator.getInertiaTensor(); //Rn.diagonalMatrix(null, new double[]{1,1,m3, 1, m1, m2});
			invInertiaTensor = rigidBodySimulator.getInvInertiaTensor(); //Rn.diagonalMatrix(null, new double[]{1,1,1/m3, 1, 1/m1, 1/m2});
			velocityStateD = rigidBodySimulator.getVelocity();
			super.init();
			firsttime = false;
		}
		super.update();
	}

}
