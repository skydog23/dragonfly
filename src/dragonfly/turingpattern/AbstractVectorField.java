/*
 * Created on Aug 23, 2010
 *
 */
package dragonfly.turingpattern;

public abstract class AbstractVectorField implements VectorField {

	double scale = 1.0;
	double rotate = 0.0;
	private transient double cos = 1.0, sin = 0.0;
	
	public void setRotation(double r) {
		this.rotate = r;
		cos = Math.cos(r);
		sin = Math.sin(r);
	}

	public void setScale(double d) {
		this.scale = d;
	}

	public double[] valueAt(double[] val, double[] coords) {
		double tx = scale * (cos * val[0] + sin * val[1]),
			ty = scale * (-sin * val[0] + cos * val[1]);
		val[0] = tx;
		val[1] = ty;
		return val;
	}

}
