package dragonfly.dynamen;

import java.awt.geom.GeneralPath;

import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.PointSetFactory;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P2;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.Rectangle3D;
//import de.jtem.java2d.SceneComponent;

public class FlatClotheIt extends ClotheIt {
	boolean testMomCS = true, doMomCS = false, rotating = false;
	double[] initialP,  toMomCS;
	PointRangeFactory momentumFactory;
	GeneralPath path = new GeneralPath();
//	SceneComponent sc = new SceneComponent();
	public FlatClotheIt(RigidBodyInterface d) {
		super(d);
//		sc.setFilled(Boolean.FALSE);
//		sc.setShape(path);
//		viewer2d.getRoot().addChild(sc); 
//		viewer2d.getRoot().setOutlinePaint( java.awt.Color.RED);
//		viewer2d.setVisible(true);
	}
	double[] momP = {0,0};
	@Override
	public void updateSceneGraphRepn() {
		super.updateSceneGraphRepn();
		double[] v  = convertPoint(velocity);
		handleVelocityInBody(v);
		velocityCurveSGC.getTransformation().setMatrix(motion);
		if (doMomCS)	{
				momP = convertPointMomentumCS(currentPosition, momP);
				path.lineTo((float) momP[0], (float) momP[1]);	
//				System.err.println("drawing in viewer2d");
//				viewer2d.repaint();
		}
//		System.err.println("dims  = "+Rn.toString(dynamics.getDims()));
		MatrixBuilder.euclidean().times(motion).scale(convertPoint(dynamics.getDims())).assignTo(object); //1).assignTo(object); //
		if (Rn.innerProduct(momentum, momentum, 2) > 10E-8)  {
			momentumBody.setVisible(true);
			momentum = dynamics.getMomentum();
			double[] momentumSpace = convertPoint(momentum);
//			System.err.println("clotheit mom = "+Rn.toString(momentum));
			Rn.matrixTimesVector(momentumSpace, getDual(dynamics.getMotionMatrix()), momentumSpace);
//			System.err.println("clotheit mom space= "+Rn.toString(momentumSpace));
			double[] pt1 = convertPoint(new double[]{momentumSpace[1], -momentumSpace[0], 0});
			double[] pt2;
			if (momentumSpace[1] != 0) 
				pt2 = convertPoint(new double[]{0, momentumSpace[3], -momentumSpace[1]});
			else pt2 = convertPoint(new double[]{momentumSpace[3], 0,  -momentumSpace[0]});
			momentumFactory.setElement0(pt1);
			momentumFactory.setElement1(pt2);
			momentumFactory.update();
			System.err.println("momentum pt1 = "+Rn.toString(pt1));
			System.err.println("momentum pt2 = "+Rn.toString(pt2));
			double[] mpb = Pn.dehomogenize(null, Pn.polarize(null, momentumSpace,metric));
			momentumBodyFactory.setVertexCoordinates(mpb);
			momentumBodyFactory.update();
//			System.err.println("momentum point = "+Rn.toString(mpb));
		} 		else 
			momentumBody.setVisible(false);

	}

	@Override
	public void reset() {
		super.reset();
		if (  !testMomCS || metric == Pn.EUCLIDEAN)  { doMomCS = false; return; }
		doMomCS = true;
		motion = dynamics.getMotionMatrix();
		initialP = new double[]{motion[3], motion[7],motion[15]};
		double[] polarM = Pn.normalize(null, Pn.polarize(null, momentum, metric), metric);
		System.err.println("reset: momentum = "+Rn.toString(momentum));
		rotating = true;
		// determine if the momentum line is a proper hyperbolic line: then we're not rotating
		double k1 =  Math.abs(momentum[2])/Math.sqrt(Rn.innerProduct(momentum, momentum, 2));
		double k2 = Pn.normSquared(momentum, metric);
		if (metric == Pn.HYPERBOLIC && k2 > 0) {
				rotating = false;
		}
		double[] e1, e2, e3;
		if (!rotating) 	{  // polarM is improper point
			e1 = P2.pointFromLines(null, Pn.polarize(null, initialP, metric), momentum); // where polar of CofM meets momentum line
			e2 = polarM; // improper point
			e3 = Pn.polarize(null, P2.lineFromPoints(null, e1, e2), metric);
		} else {  // polarM is proper, momentum is improper 
			e1 = new double[]{momentum[2], 0, -momentum[0]}; // point where momentum line intersects x-axis
			e2 = Pn.polarize(null, P2.lineFromPoints(null, polarM, e1), metric); // polar of the first two
			e3 = polarM;			// proper point
		}
		// calculate isometry taking momentum line and current position to fundamental triangle
		Pn.normalize(e1, e1, metric);
		Pn.normalize(e2, e2, metric);
		Pn.normalize(e3, e3, metric);
		toMomCS = Rn.inverse(null, Rn.transpose(null, new double[]{e1[0],e1[1],e1[2],  e2[0], e2[1], e2[2],   e3[0], e3[1], e3[2]}));
//		System.err.println("xaxis: ="+Rn.toString(xaxis));
//		System.err.println("yaxis: ="+Rn.toString(yaxis));
		double[] toMomCS2 = P2.makeDirectIsometryFromFrames(null, polarM, e1, ee0, ee1, metric);
		System.err.println("to mom cs: ="+Rn.matrixToString(toMomCS));
		System.err.println("to mom cs2: ="+Rn.matrixToString(toMomCS2));
		path.reset();
		momP = convertPointMomentumCS(currentPosition, new double[]{0,0});
		path.moveTo((float) momP[0], (float) momP[1]);
//		momentumBody.setVisible(!doFlatHyp);
//		velocityBody.setVisible(!doFlatHyp);
//		velocityCurveSGC.setVisible(!doFlatHyp);
		
	}
	private final double[] ee0 = {0,0,1}, ee1 = {0,1,0};
	@Override
	double[] convertPoint(double[] p) {
		return  new double[]{p[0], p[1], 0, p[2]};
	}
	static double[] orig = {0,0,1}, yaxe={1,0,0}, xaxe = {0,1,0};
	double[] tp = new double[3];
	double[] convertPointMomentumCS(double[] p, double[] lastP)	{
		if (!doMomCS) return p;
		Rn.matrixTimesVector(tp, toMomCS, p);
		Pn.normalize(tp, tp, metric);
		double theta, phi;
		if (rotating)	{
			theta = Math.atan2(tp[1], tp[0]);
			while (lastP[0] - theta > 1) theta += Math.PI*2;
			while (lastP[0] - theta < -1) theta -= Math.PI*2;
			phi = Pn.distanceBetween(tp, orig,metric);
		} else {
			phi =  Pn.distanceBetween(tp, xaxe, metric);		
			tp[1] = 0;
			theta = Pn.distanceBetween(tp, yaxe, metric);
		}
//		System.err.println("tp = "+Rn.toString(tp));
//		System.err.println("x,y = "+theta+" "+phi);
		return new double[]{theta, phi};			
	}
	
	@Override
	SceneGraphComponent getMomentumGeometry() {
		if (momentumFactory == null)	{
			momentumFactory = new PointRangeFactory();
			momentumFactory.setFiniteSphere(false);
			momentumFactory.setNumberOfSamples(50);			
			momentumFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
			momentumBody.setGeometry(momentumFactory.getLine());

			momentumBodyFactory = new PointSetFactory();
			momentumBodyFactory.setVertexCount(1);

			ampb = momentumBodyFactory.getPointSet();			
			momentumPoint.setGeometry(ampb);
			ampb.setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
			
		}
		return momentumBody;
	}
	public static double[] correction = Rn.identityMatrix(4);
	@Override
	double[] correctionMatrix() {
		return correction;
	}

}
