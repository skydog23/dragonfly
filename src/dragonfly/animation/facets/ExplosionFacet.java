package dragonfly.animation.facets;

import charlesgunn.anim.util.AnimationUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;

public class ExplosionFacet extends SingleSidedFacet {

	double[] axis = {1,0,0};
	double angle = 0.0, speed = 0.0, startTime;
	static double gravity = 9;
	double geometryParameter = 0.0;
	double waveDelay = .5;
	
	ExplosionFacet(int ii, int jj, int nn, int mm, Appearance fa) {
		super(ii, jj, nn, mm, fa);
		for (int i = 0; i<3; ++i) axis[i] = Math.random() - .5;
		angle = 1 + Math.random();
		speed = 1 + Math.random();
		startTime = Math.random();
		geometryParameter = getGeometryParameter();
	}

	protected double getGeometryParameter()	{
		return Math.sqrt(((i-n/2)*(i-n/2)+(j-m/2)*(j-m/2))); // 1.0-j/(n-1.0);//(i+j)/(m+n-2.0);
	}
	@Override
	public void assignColor() {
	}

	@Override
	public void assignTextureCoordinates() {
		double umin = i/((double)n);
		double vmin = j/((double)m);
		double umax = (i+1)/((double)n);
		double vmax = (j+1)/((double)m);
		double[] ll = {umin, vmin}, lr = {umax, vmin}, ul = {umin, vmax}, ur = {umax, vmax};
		double[][] tc = {ll, lr, ur, ul};
		frontRectangle.setVertexAttributes(Attribute.TEXTURE_COORDINATES, 
				StorageModel.DOUBLE_ARRAY.array(2).createReadOnly(tc));
	}
	static double[][] axes = {{-1,1,0},{1,1,0},{-1,-1,0},{1,-1,0}};
	@Override
	public void setValueAtTime(double t) {
		int mod = 2*(i%2)+j%2;
		double ival = Math.abs(i-n/2);
		double jval = Math.abs(j-m/2);
		double delay = .1*(ival+jval);
		delay =  .05*geometryParameter; //.05 *Math.sqrt(((i-n/2)*(i-n/2)+(j-m/2)*(j-m/2)));
		t = (t - delay);
		if (t < 0) t = 0;
//		if ((1) < t) t = 1;
		t = t * 1.5;
        // waveParameter varies between 0 and 1; input values less than 0 and greater than 1-waveDelay are clipped
        double waveParameter = t; //AnimationUtility.linearInterpolation(t-waveDelay*geometryParameter/3.0, 0, 1-waveDelay, 0, 1);
		MatrixBuilder.euclidean().translate(0,-gravity*waveParameter*waveParameter,3*gravity*waveParameter).rotate(Math.PI*waveParameter*t, axes[mod]).assignTo(tmpM);
//		P3.makeRotationMatrix(tmpM, axes[mod], Math.PI*(t));
		Rn.conjugateByMatrix(tmpN, tmpM, center);
		rotate.getTransformation().setMatrix(tmpN);
	}


//	double tfactor = 15, rfactor = 8.0;
//	@Override
//	public void setValueAtTime(double t) {
//		t = t - startTime;
//		if (t<0) t = 0;
//		t = t * 2;
////		if (t>1) t = 1.0;
//		MatrixBuilder.euclidean().translate(0, -t*gravity*tfactor, t*speed*tfactor).
//			rotate(Math.PI*angle*t*rfactor, axis).assignTo(tmpM);
//		Rn.conjugateByMatrix(tmpN, tmpM, center);
//		rotate.getTransformation().setMatrix(tmpN);
//	}

}
