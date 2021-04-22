/*
 * Created on Aug 23, 2010
 *
 */
package dragonfly.turingpattern;

import java.awt.Color;

import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.math.Rn;
import de.jreality.plugin.JRViewer;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

public class TestVectorField {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		BarycentricVF244 vf = new BarycentricVF244();
		vf.setScale(2);
		vf.setRotation(Math.PI/4);
		vf.setB1(.5);
		vf.setB2(.2);
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		world.getAppearance().setAttribute(CommonAttributes.TUBES_DRAW, false);
		world.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);
		world.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.black);
		
		int width = 20, height = width;
		double scale = .5;
		double[] coords = new double[3], vfd = new double[3];
		double[][] points = {coords, new double[3]};
		for (int i = 0; i<height; ++i)	{
			coords[1] = i/(height-1.0);
			for (int j = 0; j<width; ++j)	{
				coords[0] = j/(width-1.0);
				vf.valueAt(vfd, coords);
				System.err.println("VF = "+Rn.toString(vfd));
				Rn.add(points[1], Rn.times(null, scale, vfd), coords);
				IndexedLineSet vector = IndexedLineSetUtility.createCurveFromPoints(points, false);
				SceneGraphComponent child = new SceneGraphComponent();
				child.setGeometry(vector);
				world.addChild(child);
			}
		}
		JRViewer.display(world);
	}

}
