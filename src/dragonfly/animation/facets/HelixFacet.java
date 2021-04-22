package dragonfly.animation.facets;

import de.jreality.math.P3;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;

public class HelixFacet extends DoubleSidedFacet {

	HelixFacet(int ii, int jj, int nn, int mm, Appearance fa, Appearance ba) {
		super(ii, jj, nn, mm, fa, ba);
	}


	@Override
	public void assignTextureCoordinates() {
		double umin = i/((double)n);
		double vmin = j/((double)m);
		double umax = (i+1)/((double)n);
		double vmax = (j+1)/((double)m);
		double[] ll = {umin, vmin}, lr = {umax, vmin}, ul = {umin, vmax}, ur = {umax, vmax};
		double[][] tc = {ll, lr, ur, ul}, tc2;
		if (false &&  (i+j)%2 == 0)	{
			tc2 = new double[][]{ul, ur, lr, ll};
		} else 
			tc2 = new double[][]{lr,ll,ul,ur};
		frontRectangle.setVertexAttributes(Attribute.TEXTURE_COORDINATES, 
				StorageModel.DOUBLE_ARRAY.array(2).createReadOnly(tc));
		backRectangle.setVertexAttributes(Attribute.TEXTURE_COORDINATES, 
				StorageModel.DOUBLE_ARRAY.array(2).createReadOnly(tc2));
	}

	@Override
	public void assignColor() {
	}
	double gain = .025, speed = 2.0;
	@Override
	public void setValueAtTime(double t) {
		double reversed = i > 1 ? 1 : -1;
		boolean opposite = (i == 1 || i == 2);
		int index = opposite ? (m-j-1) : j;
		double delay = gain*(index);
		t = t - delay;
		t = t * speed;
		if (t < 0 ) t = 0;
		else if (t > Math.PI) t = Math.PI;
		P3.makeRotationMatrixY(tmpM, reversed*t);
		Rn.conjugateByMatrix(tmpN, tmpM, center);
		rotate.getTransformation().setMatrix(tmpN);
	}

	
}
