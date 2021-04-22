package dragonfly.animation.facets;

import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.SceneGraphUtility;

abstract public class SingleSidedFacet {
	static double epsilon = 10E-3;
	static double[][] points = {{0,0,0},{1,0,0},{1,1,0},{0,1,0}};
	static double[] center = P3.makeTranslationMatrix(null, new double[]{.5, .5,0}, Pn.EUCLIDEAN);
	SceneGraphComponent translate = SceneGraphUtility.createFullSceneGraphComponent("translate Facet");
	SceneGraphComponent rotate = SceneGraphUtility.createFullSceneGraphComponent("rotate");
	SceneGraphComponent front = SceneGraphUtility.createFullSceneGraphComponent("front");
	int i, j, n, m;
	IndexedFaceSet frontRectangle;
	double[] tmpM = new double[16], tmpN = new double[16];
	Appearance frontAp;
	static int[] rv = {0,1,2,3};//3,2,1};
	SingleSidedFacet (int ii, int jj, int nn, int mm, Appearance fa) {
		i = ii;
		j = jj;
		n = nn;
		m = mm;
		frontAp = fa;
		frontRectangle = IndexedFaceSetUtility.constructPolygon(points);
		front.setGeometry(frontRectangle);
		front.setAppearance(frontAp);
		rotate.addChildren(front);
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
