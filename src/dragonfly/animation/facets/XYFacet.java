package dragonfly.animation.facets;

import java.awt.Color;

import de.jreality.math.P3;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;

public class XYFacet extends DoubleSidedFacet {

	XYFacet(int ii, int jj, int nn, int mm, Appearance fa, Appearance ba) {
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
		if (  (i+j)%2 == 0)	{
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
		Color foo = new Color( (int) (255*(i/(n-1.0))), (int)(255*(j/(m-1.0))), 0);
		translate.getAppearance().setAttribute("polygonShader.diffuseColor", foo);
	}
	double gain = .05, speed = 2.0;
	@Override
	public void setValueAtTime(double t) {
		int mod = (i+j)%2;
		double delay;
		if (mod == 0) delay = (i+j)*gain;
		else delay = (n-i-1+m-j-1)*gain;
		t = t - delay;
		t = t * speed;
		if (t < 0 ) t = 0;
		else if (t > 1) t = 1;
		if (mod == 0) P3.makeRotationMatrixX(tmpM, t*Math.PI);
		else P3.makeRotationMatrixY(tmpM, t*Math.PI);
		Rn.conjugateByMatrix(tmpN, tmpM, center);
		rotate.getTransformation().setMatrix(tmpN);
	}

	
}
