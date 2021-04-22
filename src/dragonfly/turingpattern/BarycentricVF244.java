/*
 * Created on Aug 23, 2010
 *
 */
package dragonfly.turingpattern;

import java.util.Arrays;

import de.jreality.math.Rn;

public class BarycentricVF244 extends AbstractVectorField {

	double weights[] = {.5, .2, .3}; //.25, .25, .25, .25};
	
	public double[] valueAt(double[] val, double[] coords) {
		if (val == null) val = new double[2];
		else Arrays.fill(val, 0);
		double x = coords[0], y = coords[1];
		boolean UL = (y>x);
		if (UL) {
			x = coords[1];
			y = coords[0];
		}
		double[] v1 = {1-x, -y},
			v2 = {1-x, 1-y},
			v3 = {-x, -y};
		double b1 = weights[0] * (x-y)/2,
			b2 = weights[1] * y/2,
			b3 = weights[2] * (1-x)/2;
		Rn.add(val, 
				Rn.add(null, Rn.times(null, b1, v1), 
					Rn.times(null, b2, v2)),
				Rn.times(null, b3, v3));
//		double[] v1 = {1-x, 1-y},
//			v2 = {-x, 1-y},
//			v3 = {-x, -y},
//			v4 = {1-x, -y};
//		double b1 = weights[0]*(1-x)/2,
//			b2 = weights[1] * (1-y)/2,
//			b3 = weights[2] * (x/2),
//			b4 = weights[3] * (y/2);
//		Rn.add(val, 
//				Rn.add(null, Rn.times(null, b1+b2, v3), 
//							Rn.times(null, b2+b3, v4)),
//				Rn.add(null, Rn.times(null, b3+b4, v1), 
//							Rn.times(null, b4+b1, v2)));
		if (UL) {
			double tmp = val[0];
			val[0] = - val[1];
			val[1] = - tmp;
		}
		return super.valueAt(val,coords);
	}

	public void setB1(double b1)	{
		weights[0] = b1;
//		weights[3] = .5 - b1;
	}
	
	public void setB2(double b2)	{
		weights[1] = b2;
//		weights[2] = .5 - b2;
	}
	
	public void setB3(double b3)	{
		weights[2] = b3;
//		weights[1] = .5 - b3;
	}
	
	public void setB4(double b4)	{
		weights[3] = b4;
//		weights[0] = .5 - b4;
	}
}
