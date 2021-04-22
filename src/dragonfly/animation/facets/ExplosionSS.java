package dragonfly.animation.facets;

import java.awt.Component;

import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.util.SceneGraphUtility;

public class ExplosionSS implements SubSequence {
	Appearance frontAp, backAp;
	int n = 20, m = 16;
	SingleSidedFacet[] facets;
	boolean cullBack = true;
	DancingSquares ds;
	SceneGraphComponent theFacets = SceneGraphUtility.createFullSceneGraphComponent();
	int type = 0;
	public int getType() {
		return type;
	}

	public void setType(int type) {
		this.type = type;
	}

	public ExplosionSS(DancingSquares ds)	{
		this.ds = ds;
	}

	public Component getInspector() {
		// TODO Auto-generated method stub
		return null;
	}

	public SceneGraphComponent getScene() {
		SceneGraphComponent parent = SceneGraphUtility.createFullSceneGraphComponent("parent");
		parent.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
		SceneGraphComponent backScene = SceneGraphUtility.createFullSceneGraphComponent("backScene");
		backScene.setAppearance(backAp);
		double[][] texc = {{1,1} ,{0,1},{0,0},{1,0}};
		double[][] verts = {   {1,1,0},{-1,1,0}, {-1,-1,0},{1,-1,0}};			// front
		IndexedFaceSet square = IndexedFaceSetUtility.constructPolygon(verts);
		square.setVertexAttributes(Attribute.TEXTURE_COORDINATES,StorageModel.DOUBLE_ARRAY.array(2).createReadOnly(texc));
		double aspectRatio = (Double) backAp.getAttribute("aspectRatio",Double.class);
		MatrixBuilder.euclidean().translate(0,0,-.02).scale(aspectRatio, 1, aspectRatio).assignTo(backScene);
		backScene.setGeometry(square);
		parent.addChild(backScene);
		facets = new SingleSidedFacet[n*m];
		for (int i = 0; i<m; ++i)	{
			for (int j = 0; j<n; ++j)	{
				if (type == 0) facets[i*n+j] = new ExplosionFacet(j, i, n, m, frontAp) ;
				else facets[i*n+j] = new ExplosionFacet3(j, i, n, m, frontAp);
				theFacets.addChild(facets[i*n+j].getSceneGraphComponent());
			}
		}
		theFacets.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED,false);
		// calculate the scaling required by the field of view, so that
		// the figure exactly fits the viewing frustum at z=ds.initialFocus
		double factor = ds.initialFocus * Math.tan((Math.PI/180.0)*ds.initialFOV/2.0);
		parent.addChild(theFacets);
		MatrixBuilder.euclidean().
			scale((2.0/m)). //, factor, factor).
			translate(-n/2.0, -m/2.0,0).		// center at (0,0)
			assignTo(theFacets);
		MatrixBuilder.euclidean().translate(0,0,-ds.initialFocus).scale(factor).assignTo(parent);
		return parent;
	}

	public void setValueAtTime(double t) {
		for (SingleSidedFacet ff : facets) ff.setValueAtTime(t);
	}

	public void setupAnimation(AnimationPanel ap) {
		// TODO Auto-generated method stub

	}
	
	public void setAppearance0(Appearance ap0)	{
		frontAp = ap0;
	}
	public void setAppearance1(Appearance ap0)	{
		backAp = ap0;
	}

}
