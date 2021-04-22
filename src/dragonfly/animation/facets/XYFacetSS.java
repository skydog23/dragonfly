package dragonfly.animation.facets;

import java.awt.Component;

import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

public class XYFacetSS implements SubSequence {
	Appearance frontAp, backAp;
	int n = 5, m = 4;
	DoubleSidedFacet[] facets;
	boolean cullBack = true;
	DancingSquares ds;
	public XYFacetSS(DancingSquares ds)	{
		this.ds = ds;
	}
	public Component getInspector() {
		return null;
	}

	public SceneGraphComponent getScene() {
		facets = new DoubleSidedFacet[n*m];
		SceneGraphComponent theFacets = SceneGraphUtility.createFullSceneGraphComponent("facets");
		frontAp.setAttribute(CommonAttributes.BACK_FACE_CULLING_ENABLED, cullBack);
		backAp.setAttribute(CommonAttributes.BACK_FACE_CULLING_ENABLED, cullBack);
		if (cullBack)
			backAp.setAttribute(CommonAttributes.FLIP_NORMALS_ENABLED, true);
		for (int i = 0; i<m; ++i)	{
			for (int j = 0; j<n; ++j)	{
				facets[i*n+j] = new XYFacet(j, i, n, m, frontAp, backAp);
				theFacets.addChild(facets[i*n+j].getSceneGraphComponent());
			}
		}
		theFacets.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED,false);
		// calculate the scaling required by the field of view, so that
		// the figure exactly fits the viewing frustum at z=ds.initialFocus
		double factor = ds.initialFocus * Math.tan((Math.PI/180.0)*ds.initialFOV/2.0);
		MatrixBuilder.euclidean().translate(0,0,-ds.initialFocus).
			scale((2.0/m)*factor). //, factor, factor).
			translate(-n/2.0, -m/2.0,0).		// center at (0,0)
			assignTo(theFacets);
		return theFacets;
	}

	public void setValueAtTime(double t) {	
		for (DoubleSidedFacet ff : facets) ff.setValueAtTime(t);
	}

	public void setupAnimation(AnimationPanel ap) {
	}

	public void setN(int n)	{
		this.n = n;
	}
	
	public void setM(int m)	{
		this.m = m;
	}
	
	public void setAppearance0(Appearance ap0)	{
		frontAp = ap0;
	}
	public void setAppearance1(Appearance ap0)	{
		backAp = ap0;
	}
}
