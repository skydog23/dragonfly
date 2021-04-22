package dragonfly.tencell;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.util.TextSlider;

import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.scene.PointLight;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.util.WingedEdgeUtility;

public class SphericalLights {
	double[] atten = {.5, 1.0, 0};
	double intensity = .3;
	double[][] positions = {{1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1},{-1, 1,.5,0},{0,.5,1,0}, {1,1,1,1}}; //, //};
	PointLight[] lights = new PointLight[positions.length];
	
	public SceneGraphComponent makeLights() {
		SceneGraphComponent lightNode = new SceneGraphComponent();
		lightNode.setName("lights");
		double s = 1.4;
//		double[][] positions ={
//				{0,0,0,1},{1,-1,1,s},{-1,1,1,s}, {1,1,-1,s}// {-1,-1,-1,s},{{1,0,0,0}, {0,1,0,0}, {0,0,1,0}, {0,0,0,1}}; //
//		};
		double[] zaxis = {0,0,1,0};
		boolean  direct = false;
		double[] bloo = direct ? zaxis : P3.originP3;
		for (int i = 0; i<positions.length; ++i)	{
//			double[] axis = {-1.2,-1.6,-2,1};
//			if (i > 0) axis[i-1] = 1; 
//			else axis = new double[]{1.8,1.3,1,1};
//			positions[i]= randomino();
			SceneGraphComponent l0 = SceneGraphUtility.createFullSceneGraphComponent("light0");
			PointLight dl = new PointLight();
			lights[i] = dl;
			dl.setFalloff(atten);
//			DirectionalLight dl = new DirectionalLight();
			int c[] = {255, 255, 255};
			if (i < 3) c[i] = 150;
			dl.setColor(new Color(c[0], c[1], c[2]));
			dl.setIntensity(intensity);
			l0.getTransformation().setMatrix( P3.makeTranslationMatrix(null, bloo, positions[i], Pn.ELLIPTIC));		
			l0.setLight(dl);
			lightNode.addChild(l0);
//			l0 = SceneGraphUtility.createFullSceneGraphComponent("light0");
//			double[] flipped = Rn.times(null, -1, positions[i]);
//			flipped[3] *= -1.0;
//			l0.getTransformation().setMatrix( P3.makeTranslationMatrix(null, bloo, flipped, Pn.ELLIPTIC));
//			l0.setLight(dl);
//			lightNode.addChild(l0);
//			l0 = SceneGraphUtility.createFullSceneGraphComponent("mlight0");
//			l0.getTransformation().setMatrix( P3.makeTranslationMatrix(null, mzaxis, positions[i], Pn.ELLIPTIC));		
//			lightNode.addChild(l0);
		}
		return lightNode;
	}

	private void update()	{
		for (int i = 0; i<lights.length; ++i)	{
			lights[i].setIntensity(intensity);
			lights[i].setFalloff(atten);
		}
	}
	public Component getInspector() {
			Box container = Box.createVerticalBox();
			final TextSlider atten0Slider = new TextSlider.Double("atten0",  SwingConstants.HORIZONTAL, 0, 1.0, atten[0]);
		    atten0Slider.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					atten[0] = ((TextSlider) arg0.getSource()).getValue().doubleValue();
					update();	
				}
		    });
		    container.add(atten0Slider);
			final TextSlider atten1Slider = new TextSlider.Double("atten1",  SwingConstants.HORIZONTAL, 0.0, 2.0, atten[1]);
		    atten1Slider.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					atten[1] = ((TextSlider) arg0.getSource()).getValue().doubleValue();
					update();	
				}
		    });
		    container.add(atten1Slider);
			final TextSlider intensitySlider = new TextSlider.Double("intensity",  SwingConstants.HORIZONTAL, 0.0, 1.0, intensity);
		    intensitySlider.addActionListener(new ActionListener() {

				public void actionPerformed(ActionEvent arg0) {
					intensity = ((TextSlider) arg0.getSource()).getValue().doubleValue();
					update();
				}
		    });
		    container.add(intensitySlider);
			return container;
	}
}
