/*
 * Created on Jan 29, 2004
 *
 */
package dragonfly.dynamen.attic;

import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JMenuBar;

import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;


public class TestRigidBodyEucPlane extends LoadableScene {
	private SceneGraphComponent body, world;
	IndexedLineSet avb = null, amb = null;
	double[] initialMomentum = {1,1,1};		// a line close to the origin
	double[] initialMoments = {1,1, 3};
	RigidBodyODEE2 rbode = new RigidBodyODEE2();
	@Override
	public SceneGraphComponent makeWorld()	{
		
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		world.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, true);
		body = SceneGraphUtility.createFullSceneGraphComponent("body");
		world.addChild(body);
		rbode.setMoments(initialMoments);
		rbode.setMomentum(initialMomentum);
		rbode.setDeltaT(1*rbode.getDeltaT());
		rbode.update();
		body.addChild(rbode.getSceneGraphRepresentation());
		
		return world;
	}

	@Override
	public boolean isEncompass() {
		return true;
	}

	@Override
	public void customize(JMenuBar menuBar, final Viewer viewer) {
		((Component) viewer.getViewingComponent()).addKeyListener( new KeyAdapter()	{
			@Override
			public void keyPressed(KeyEvent e)	{ 
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_H:
					break;
	
				case KeyEvent.VK_1:
					rbode.setRunMotion(!rbode.isRunMotion());
					break;
					
				case KeyEvent.VK_2:
					double[] m = rbode.getMoments();
					double m3 = m[2];
					if (e.isShiftDown()) m3 = m3/1.1;
					else m3 = m3 * 1.1;
					m[2] = m3;
					rbode.setMoments(m);
					rbode.update();
					viewer.renderAsync();
					break;
					
				case KeyEvent.VK_3:
					double mass = rbode.getMass();
					if (e.isShiftDown()) mass = mass/1.1;
					else mass = mass * 1.1;
					rbode.setMass(mass);
					rbode.update();
					viewer.renderAsync();
					break;
					
				case KeyEvent.VK_4:
					if (e.isShiftDown()) rbode.setDeltaT(rbode.getDeltaT()/1.1);
					else rbode.setDeltaT(rbode.getDeltaT()*1.1);
					break;
					
			}

			}
		});

	}
}
