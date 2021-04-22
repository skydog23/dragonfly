package dragonfly.jreality.examples;

import java.awt.Color;

import charlesgunn.jreality.viewer.Assignment;
import de.jreality.geometry.SphereUtility;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.SceneGraphUtility;

public class ColoredSphere extends Assignment {

	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent();
		world.setGeometry(SphereUtility.tessellatedIcosahedronSphere(5));
		world.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		return world;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new ColoredSphere().display();
	}

}
