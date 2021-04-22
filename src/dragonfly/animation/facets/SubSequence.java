package dragonfly.animation.facets;

import java.awt.Component;

import charlesgunn.anim.gui.AnimationPanel;
import de.jreality.scene.Appearance;

public interface SubSequence {

	public void setValueAtTime(double t);	// t's between 0 and1
	public de.jreality.scene.SceneGraphComponent getScene();
	public void setupAnimation(AnimationPanel ap);
	public Component getInspector();
	public void setAppearance0(Appearance ap);
	public void setAppearance1(Appearance ap);
}
