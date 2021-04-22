package dragonfly.dynamen;

import static de.jreality.shader.CommonAttributes.BACK_FACE_CULLING_ENABLED;
import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.EDGE_DRAW;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;
import static de.jreality.shader.CommonAttributes.SPECULAR_COEFFICIENT;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY;
import static de.jreality.shader.CommonAttributes.TRANSPARENCY_ENABLED;
import static de.jreality.shader.CommonAttributes.VERTEX_DRAW;

import java.awt.Color;

import charlesgunn.jreality.geometry.projective.CircleFactory;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.PointSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.SphereUtility;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class CompactClotheIt extends ClotheIt {

	CircleFactory momentumImage;
	SceneGraphComponent pointMirror, 
		hypersphere, 
		bodyHyp, 
		bodyEll, bodyEll2,
		bodyBoth,
		correctionMatrix;
		public CompactClotheIt(RigidBodyInterface d) {
			super(d);
		}

		@Override
		double[] convertPoint(double[] p) {
			double[] p4 = new double[]{p[0], p[1], 0, p[2]};
			if (metric == Pn.EUCLIDEAN) {
				return p4 ;
			}
			// when the following gets its z and w coords flipped,
			// voila! one has points on the unit sphere
			if (metric == Pn.ELLIPTIC) {
				Rn.normalize(p4, p4);
				p4[2] = 1.0;
				return p4;
			}
				
			Compactor.compactify(p4, dynamics.getMetric());
			return p4;
			}

		@Override
		SceneGraphComponent getMomentumGeometry() {
			if (momentumImage == null)	{
				momentumImage = new CircleFactory();
				SceneGraphComponent flipper = new SceneGraphComponent();
				momentumBody.addChild(flipper);
				flipper.addChild(momentumImage.getSphereSGC());
				MatrixBuilder.euclidean().scale(-1,-1,-1).assignTo(flipper);
				
				momentumBodyFactory = new PointSetFactory();
				momentumBodyFactory.setVertexCount(1);

				ampb = momentumBodyFactory.getPointSet();			
				momentumPoint.setGeometry(ampb);
				ampb.setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
			}
			return momentumBody;
		}

		@Override
		public void updateSceneGraphRepn() {
			super.updateSceneGraphRepn();
//				double[][] points =velocityCurve.getCurve().getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
//				Rn.matrixTimesVector(points, motion, points);
//				velocityCurveSGC.setGeometry(IndexedLineSetUtility.createCurveFromPoints(points, false));
//			velocityCurveSGC.getTransformation().setMatrix(motion);
			momentumBody.setVisible(true);
			double[] momentumSpace = {momentum[0],  momentum[1],0,momentum[2]};
			Rn.matrixTimesVector(momentumSpace, getDual(dynamics.getMotionMatrix()), momentumSpace);
			double[] mpb = Pn.dehomogenize(null,Pn.polarize(null, momentumSpace,metric));
			Compactor.compactify(mpb, metric);
			momentumBodyFactory.setVertexCoordinates(mpb);
			momentumBodyFactory.update();
//			System.err.println("momentum point = "+Rn.toString(mpb));
			if (metric == Pn.HYPERBOLIC)	{
				double[] circleCoords = {momentumSpace[0],  momentumSpace[1], -2*momentumSpace[3],0};
//				System.err.println("Circle coords = "+Rn.toString(circleCoords));
				momentumImage.setCircleCoordinates(circleCoords);
				updateBodyRepn();
				double[] v = foobar(velocity);	
				handleVelocityInBody(v);
				//new Matrix(Rn.conjugateByMatrix(null,new Matrix(motion).getArray(), correctionMatrices[1])).assignTo(bodyHyp);
			} else if (metric == Pn.ELLIPTIC)	{
				double[] v  = convertPoint(velocity);
				super.handleVelocityInBody(v);
				velocityCurveSGC.getTransformation().setMatrix(motion);
				double[] dims = dynamics.getDims();
				dims = Rn.normalize(null,dims);
				MatrixBuilder.euclidean().scale(dims).scale(.5).assignTo(bodyEll2);
//				IndexedFaceSet box = Primitives.box(dims[0], dims[1], dims[2], false);
//				bodyEll2.setGeometry(box);
				new Matrix().assignTo(bodyBoth);
				bodyEll.getTransformation().setMatrix(Rn.conjugateByMatrix(null, motion,correctionMatrices[2]));
				double[] pt1 = {momentumSpace[1], -momentumSpace[0], 0, 0};
				double[] pt2 =null;
				if (momentumSpace[1] != 0) pt2 = new double[]{0, momentumSpace[3], 1, -momentumSpace[1]};
				else pt2 = new double[]{momentumSpace[3], 0, 1, -momentumSpace[0]};
				double[] plane = P3.planeFromPoints(null, pt1, pt2, P3.originP3);
				momentumImage.setPlaneEquation(new double[]{momentumSpace[0],  momentumSpace[1],momentumSpace[3], momentumSpace[2]});
//				System.err.println("plane equation = "+Rn.toString(momentumImage.getPlaneEquation()));
			}
			momentumImage.update();
		}
		SceneGraphComponent velTmp = new SceneGraphComponent();
		@Override
		protected void handleVelocityInBody(double[] v) {
//			super.handleVelocityInBody(v);
			velocityCurve.addPoint(v);
			Matrix mm = new Matrix(motion);
			mm.assignTo(velTmp);
			velTmp.setGeometry(velocityCurve.getCurve());
			Compactor cmp = new Compactor(velTmp, metric);
			SceneGraphComponent foo = cmp.visit();
			velocityCurveSGC.setGeometry(SceneGraphUtility.getFirstGeometry(foo));
			double[] worldV = mm.multiplyVector(v);
			Compactor.compactify(worldV, metric);
			velocityFactory.setVertexCoordinates(worldV);
			velocityFactory.update();
			velocityCurveWorld.addPoint(worldV);
		}

		private double[] foobar(double[] p) {
			return  new double[]{p[0], p[1], 0, p[2]};
		}
		private void updateBodyRepn() {
			if (motion == null) return;
			double[] dims = dynamics.getDims();
			Matrix tmp = new Matrix();
			// update points
			MatrixBuilder.euclidean().times(motion).scale(dims[0]/dims[2], dims[1]/dims[2],1.0).
				times(defaultBodyRepnMatrix.getArray()).assignTo(tmp);
			double[][] verts = Rn.matrixTimesVector(null, tmp.getArray(), origverts);
			for (int i= 0 ;i < verts.length; ++i) {
				Compactor.compactify(verts[i], metric);
			}
//			System.err.println("verts = "+Rn.toString(verts));
			bodyFactory.setVertexAttribute(Attribute.COORDINATES, verts);
			bodyFactory.update();
			MatrixBuilder.euclidean().scale(1.01).assignTo(bodyHyp);
		}

		@Override
		public SceneGraphComponent fullSceneGraph() {
			super.fullSceneGraph();
			pointMirror = new SceneGraphComponent("pointMirror");
			pointMirror.addChild(mover);
			pointMirror.setVisible(false);
			MatrixBuilder.euclidean().scale(-1, -1, -1).assignTo(pointMirror);
			hypersphere = Primitives.sphere(.995, 0,0,0);
			hypersphere.setGeometry(SphereUtility.tessellatedIcosahedronSphere(5));
			hypersphere.getAppearance().setAttribute(BACK_FACE_CULLING_ENABLED,  true);
			hypersphere.getAppearance().setAttribute(TRANSPARENCY_ENABLED, true);
			hypersphere.getAppearance().setAttribute(TRANSPARENCY, .5);
			hypersphere.getAppearance().setAttribute(SPECULAR_COEFFICIENT, 0.0);
			hypersphere.getAppearance().setAttribute(EDGE_DRAW, false);
			hypersphere.getAppearance().setAttribute(VERTEX_DRAW, false);
			hypersphere.getAppearance().setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.WHITE);
			moveToCenter.addChildren( pointMirror);
			unMoved.addChild(hypersphere);
			equiDistTex.setVisible(false);
			return unMoved;
		}

		@Override
		public void setMetric(int metric) {
			super.setMetric(metric);
			bodyEll.setVisible(metric == Pn.ELLIPTIC);
			bodyHyp.setVisible(metric == Pn.HYPERBOLIC);
			pointMirror.setVisible(metric == Pn.ELLIPTIC);
			System.err.println("In compact set metric");
			if (metric == Pn.ELLIPTIC)	{
				parent.getTransformation().setMatrix(correctionMatrices[2]);
				// have to "re-correct" the above by repeating it (it's an involution)
				momentumBody.getTransformation().setMatrix(correctionMatrices[2]);	
			} else {
				parent.getTransformation().setMatrix(correctionMatrices[1]);
				momentumBody.getTransformation().setMatrix(correctionMatrices[1]);				
			}
			velocityCurveSGC.setGeometry(velocityCurve.getCurve());
		}

		public static double[][] correctionMatrices = {
			{1,0,0,0,  0,1,0,0,  0,0,1,-1,  0,0,1,0}, 
			Rn.identityMatrix(4), 
			{1,0,0,0,   0,1,0,0,   0,0,0,1,  0,0,1,0}
		};
		@Override
		double[] correctionMatrix() {
			return correctionMatrices[metric+1];
		}

		SceneGraphComponent bodyRep = null;
		@Override
		protected SceneGraphComponent getDefaultBodyRepn() {
			if (bodyBoth == null)	{
				bodyRep = super.getDefaultBodyRepn();
				bodyHyp = bodyRep; //compacted(bodyRep, Pn.HYPERBOLIC);
				updateBodyRepn();
				bodyEll2 = new SceneGraphComponent("elliptic body 2");
				bodyEll2.setGeometry(ClotheIt3D.getColoredBox().getGeometry());
//				SceneGraphComponent corr = new SceneGraphComponent("correction node");
//				corr.addChild(bodyEll2);
//				corr.setTransformation(new Transformation(correctionMatrices[2]));
				bodyEll = SceneGraphUtility.createFullSceneGraphComponent("elliptic body");
				bodyEll.addChild(bodyEll2);
				bodyBoth = SceneGraphUtility.createFullSceneGraphComponent("both");
				bodyBoth.addChildren(bodyHyp, bodyEll);			
				bodyBoth.getAppearance().setAttribute(EDGE_DRAW, false);
				bodyBoth.getAppearance().setAttribute(VERTEX_DRAW, false);
				bodyBoth.getAppearance().setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white);
				bodyEll.setVisible(metric == Pn.ELLIPTIC);
			}
			return bodyBoth;
		}

		public static SceneGraphComponent compacted(SceneGraphComponent sgc, int sig)	{
//			FullCopyVisitor fcv = new FullCopyVisitor();
//			fcv.visit(sgc);
			Compactor cmp = new Compactor(sgc, sig);
			return cmp.visit();
			
		}


}
