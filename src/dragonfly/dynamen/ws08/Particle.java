package dragonfly.dynamen.ws08;

import de.jreality.math.Rn;

/**
 * A simple class to encapsulate the mathematical model for a simple newtonian particle.
 * 
 * As currently used, neither <i>mass</i> nor <i>size</i> are used.
 * Perhaps <i>size</i> belongs in another class since the mathematical particle has
 * no size.
 * 
 * @author Charles Gunn
 *
 */
public class Particle {

	double[] position = {0,0,0}, 
		velocity = {1,0,0},
		force = {0,0,0};
	double mass = 1.0, 
		size = 1.0;
	
	public double getMass() {
		return mass;
	}
	public void setMass(double mass) {
		this.mass = mass;
	}
	public double[] getPosition() {
		return position;
	}
	public void setPosition(double[] position) {
		this.position = position;
	}
	public double getSize() {
		return size;
	}
	public void setSize(double size) {
		this.size = size;
	}
	public double[] getVelocity() {
		return velocity;
	}
	public void setVelocity(double[] velocity) {
		this.velocity = velocity;
	}
	public void transformBy(double[] m)	{
		Rn.matrixTimesVector(position, m, position);
		double[] homogeneousVector = {velocity[0], velocity[1], velocity[2], 0};
		Rn.matrixTimesVector(homogeneousVector, m, homogeneousVector);
		for (int i = 0; i<3; ++i) velocity[i] = homogeneousVector[i];
	}
}
