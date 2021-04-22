package dragonfly.animation.facets;

import java.awt.Color;

import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;

public class ExplosionFacet2 extends DoubleSidedFacet {

	ExplosionFacet2(int ii, int jj, int nn, int mm, Appearance fa, Appearance ba) {
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
		if ((i+j)%2 == 1)	{
			tc2 = new double[][]{tc[rv[0]], tc[rv[3]], tc[rv[2]], tc[rv[1]]}; //ll, ul, ur, lr};
		} else tc2 = new double[][]{tc[rv[2]], tc[rv[1]], tc[rv[0]], tc[rv[3]]};
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

	static double[][] axes = {{-1,1,0},{1,1,0},{-1,-1,0},{1,-1,0}};
	@Override
	public void setValueAtTime(double t) {
		int mod = 2*(i%2)+j%2;
		double ival = Math.abs(i-n/2);
		double jval = Math.abs(j-m/2);
		double delay = .1*(ival+jval);
		t = t - delay;
		if (t < 0) t = 0;
		if ((1) < t) t = 1;
		MatrixBuilder.euclidean().translate(0,0,9*t*t).rotate(Math.PI*t, axes[mod]).assignTo(tmpM);
//		P3.makeRotationMatrix(tmpM, axes[mod], Math.PI*(t));
		Rn.conjugateByMatrix(tmpN, tmpM, center);
		rotate.getTransformation().setMatrix(tmpN);
	}

	
}
