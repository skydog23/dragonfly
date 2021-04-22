package dragonfly.animation.facets;

import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.SceneGraphUtility;

abstract public class DoubleSidedFacet {
	static double epsilon = 10E-3;
	static double[] shiftF = P3.makeTranslationMatrix(null, new double[]{0,0,epsilon}, Pn.EUCLIDEAN);
	static double[] shiftB = P3.makeTranslationMatrix(null, new double[]{0,0,-epsilon}, Pn.EUCLIDEAN);
	static double[][] points = {{0,0,0},{1,0,0},{1,1,0},{0,1,0}};
	static double[] center = P3.makeTranslationMatrix(null, new double[]{.5, .5,0}, Pn.EUCLIDEAN);
	SceneGraphComponent translate = SceneGraphUtility.createFullSceneGraphComponent("translate Facet");
	SceneGraphComponent rotate = SceneGraphUtility.createFullSceneGraphComponent("rotate");
	SceneGraphComponent front = SceneGraphUtility.createFullSceneGraphComponent("front"),
		back = SceneGraphUtility.createFullSceneGraphComponent("back");
	int i, j, n, m;
	IndexedFaceSet frontRectangle, backRectangle;
	double[] tmpM = new double[16], tmpN = new double[16];
	Appearance frontAp, backAp;
	static int[] rv = {0,1,2,3};//3,2,1};
	DoubleSidedFacet (int ii, int jj, int nn, int mm, Appearance fa, Appearance ba) {
		i = ii;
		j = jj;
		n = nn;
		m = mm;
		frontAp = fa;
		backAp = ba;
		frontRectangle = IndexedFaceSetUtility.constructPolygon(points);
		backRectangle = IndexedFaceSetUtility.constructPolygon(
				new double[][]{points[rv[0]],points[rv[1]],points[rv[2]],points[rv[3]]});
		GeometryUtilityOverflow.flipNormals(backRectangle);
		front.setGeometry(frontRectangle);
		back.setGeometry(backRectangle);
		front.setAppearance(frontAp);
		back.setAppearance(backAp);
		front.getTransformation().setMatrix(shiftF);
		back.getTransformation().setMatrix(shiftB);
		rotate.addChildren(front, back);
		MatrixBuilder.euclidean().
			translate(ii, jj, 0).assignTo(translate);
//			assignColor();
		assignTextureCoordinates();
		translate.addChild(rotate);
	}

	abstract public void assignTextureCoordinates();
	abstract public void assignColor();
	
	SceneGraphComponent getSceneGraphComponent()	{
		return translate;
	}
	
	abstract public void setValueAtTime(double t);

}
