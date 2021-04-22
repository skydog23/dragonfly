package dragonfly.dynamen;

public interface RigidBodyInterface {

	public double[] getVelocity();
	public double[] getMotionMatrix();
	public double[] getMoments();
	public double[] getMomentum();
	public double[] getDims();
	public double[] getInertiaTensor();
	public double[] getInvInertiaTensor();
	public int getMetric();
	public double getMass();
	public double getEnergy();
	
	public void setVelocity(double[] d);
	public void setMotionMatrix(double[] d);
	public void setBoxDims(double[] d, int metric);
	public void setMoments(double[] d);
	public void setMomentum(double[] d);
	public void setDims(double[] d);
	public void setMass(double d);
}
