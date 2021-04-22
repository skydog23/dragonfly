package dragonfly.animation.facets;

import java.awt.Color;

import de.jreality.math.P3;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;

public class DiagonalFacet extends DoubleSidedFacet {

	DiagonalFacet(int ii, int jj, int nn, int mm, Appearance fa, Appearance ba) {
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
		double delay = .1*(i+j);
		if (delay > t ) t = delay;
		if ((delay+1) < t) t = delay + 1;
		P3.makeRotationMatrix(tmpM, axes[mod], Math.PI*(t-delay));
		Rn.conjugateByMatrix(tmpN, tmpM, center);
		rotate.getTransformation().setMatrix(tmpN);
	}

	
}
