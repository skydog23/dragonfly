package dragonfly.animation.facets;

import java.awt.Component;

import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

public class HelixSS implements SubSequence {
	Appearance frontAp, backAp;
	int numStrips = 40, numRows = 2;
	DoubleSidedFacet[] facets;
	boolean cullBack = true;
	DancingSquares ds;
	public HelixSS(DancingSquares ds)	{
		this.ds = ds;
	}
	
	public Component getInspector() {
		return null;
	}

	public SceneGraphComponent getScene() {
		facets = new DoubleSidedFacet[numStrips * numRows];
		SceneGraphComponent theFacets = SceneGraphUtility.createFullSceneGraphComponent("facets");
		frontAp.setAttribute(CommonAttributes.BACK_FACE_CULLING_ENABLED, cullBack);
		backAp.setAttribute(CommonAttributes.BACK_FACE_CULLING_ENABLED, cullBack);
		if (cullBack)
			backAp.setAttribute(CommonAttributes.FLIP_NORMALS_ENABLED, true);
		for (int i = 0; i<numRows; ++i)	{
			for (int j = 0; j<numStrips; ++j)	{
				facets[i*numStrips + j] = new HelixFacet(i, j, numRows, numStrips, frontAp, backAp);
				theFacets.addChild(facets[i*numStrips+j].getSceneGraphComponent());
			}
		}
		theFacets.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED,false);
		// calculate the scaling required by the field of view, so that
		// the figure exactly fits the viewing frustum at z=ds.initialFocus
		double aspectRatio = (Double) frontAp.getAttribute("aspectRatio", Double.class);
		double factor = ds.initialFocus * Math.tan((Math.PI/180.0)*ds.initialFOV/2.0);
		MatrixBuilder.euclidean().translate(0,0,-ds.initialFocus).
			scale(aspectRatio*(2.0/numRows)*factor, (2.0/numStrips)*factor, factor). //, factor, factor).
			translate(-numRows/2.0, -numStrips/2.0,0).		// center at (0,0)
			assignTo(theFacets);
		return theFacets;
	}

	public void setAppearance0(Appearance ap) {
		frontAp = ap;
	}

	public void setAppearance1(Appearance ap) {
		backAp = ap;
	}

	public void setValueAtTime(double t) {
		for (DoubleSidedFacet ff : facets) ff.setValueAtTime(t);
	}

	public void setupAnimation(AnimationPanel ap) {
	}

	public int getNumStrips() {
		return numStrips;
	}

	public void setNumStrips(int numStrips) {
		this.numStrips = numStrips;
	}

}
