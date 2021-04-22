/*
 * Created on Aug 23, 2010
 *
 */
package dragonfly.turingpattern;

public interface VectorField {

	public double[] valueAt(double[] val, double[] coords);
	public void setScale(double d);
	public void setRotation(double r);
}
