package dragonfly.dynamen.ws08;

import static de.jreality.shader.CommonAttributes.POINT_SHADER;
import static de.jreality.shader.CommonAttributes.SPHERES_DRAW;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.prefs.InvalidPreferencesFormatException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;

import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.math.MatrixBuilder;
import de.jreality.plugin.experimental.ViewerKeyListener;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.DefaultPointShader;
import de.jreality.shader.ShaderUtility;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.util.SceneGraphUtility;
import de.jreality.vr.ViewerVR;

public class Sample04SpringSystem extends LoadableScene {
	
	protected SceneGraphComponent world;
	private SpringedILS springedILS;
	boolean active = false;
	protected Timer timer; 		// used to drive the evolution of the particle system.
	protected Viewer viewer;
	
	@Override
	public SceneGraphComponent makeWorld()	{
		timer = new Timer(20, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				springedILS.update();
			}
			
		});
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(world.getAppearance(), true);
		DefaultPointShader dps = (DefaultPointShader) dgs.createPointShader("default");
		dps.setDiffuseColor(Color.white);
		dps.setPointRadius(.003);
		dgs.setShowLines(true);
		DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
		dls.setTubeRadius(.002);
		dgs.setShowPoints(true);
		springedILS = new SpringedILS();
		springedILS.initializeParticles();
		world.setGeometry(springedILS.getIls());
		MatrixBuilder.euclidean().translate(01,-1,-2).rotateX(-Math.PI/2).assignTo(world);
		return world;
	}

	@Override
	public Component getInspector(Viewer v) {
		return getInspector();
	}

	@Override
	public boolean hasInspector() {
		return true;
	}

	private Component getReadme() {
		JPanel panel = new JPanel();
		panel.setName("ReadMe");
		JTextArea textarea = new JTextArea(10,20);
		textarea.setEditable(false);
		textarea.append("This is a simple demo of\n"+
				"a spring system.\n"+
				"Here's how:\n"+
				"    left mouse drag:    grab an object\n"+
				"    'r':    reset grid\n\n"+
				"Use mouse click wheel to zoom in and out.\n"+
				"Arrow keys allow walking on terrain\n"+
				"Shift-cntl-f  toggles fullscreen mode.\n"+
				"Click on the tab 'Scene Graph' to explore structure\n"+
				"\nAuthor: Charles Gunn\n"+
				"    gunn at math.tu-berlin.de\n");
		panel.add(textarea);
		return panel;
	}

	/**
	 * Create an inspector for toggling run/pause, for resetting, for toggling cheap sphere mode,
	 * Add in inspector from the ParticleSystemFactory and add an inspector for setting the colors
	 * used to color the particles.
	 */
	protected Component getInspector() {
		Box container = Box.createVerticalBox();
		Box hbox = Box.createHorizontalBox();
		// swing gui has unpredictable sizing properties! I'm punting here ...
		hbox.setPreferredSize(new Dimension(300, 40));
		hbox.setMaximumSize(new Dimension(300, 40));
		final JButton runCB = new JButton();
		runCB.setText(active ? "pause" : "run");
		hbox.add(runCB);
		runCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				active = !active;
				if (active)
					timer.start();
				else
					timer.stop();
				runCB.setText(active ? "pause" : "run");
			}

		});
		hbox.add(Box.createHorizontalStrut(5));
		final JButton resetCB = new JButton();
		resetCB.setText("reset");
		hbox.add(resetCB);
		resetCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				springedILS.initializeParticles();
				springedILS.update();
				active = false;
				timer.stop();
				runCB.setText(active ? "pause" : "run");
			}

		});
		hbox.add(Box.createHorizontalStrut(5));
		final JButton oneCB = new JButton();
		oneCB.setText("step");
		hbox.add(oneCB);
		oneCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				springedILS.update();
			}

		});
		hbox.add(Box.createHorizontalStrut(5));
		final JCheckBox sphereCB = new JCheckBox();
		sphereCB.setText("cheap spheres");
		hbox.add(sphereCB);
		sphereCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				boolean cheapSpheres = sphereCB.isSelected();
				world.getAppearance().setAttribute(
						POINT_SHADER + "." + SPHERES_DRAW, !cheapSpheres);
			}

		});
		container.add(hbox);
		
		container.add(springedILS.getInspector());

		container.setName("Parameters");
		Dimension d = container.getPreferredSize();
		container.setPreferredSize(new Dimension(d.width / 2, d.height));
		return container;
	}

	protected void doIt()	{
		makeWorld();

		if (active) timer.start();
		// make a ViewerVR and replace the navigation tool by one that uses jBullet for picking
		ViewerVR vr = ViewerVR.createDefaultViewerVR(null);
		
		// set content, undoing the rotation that ViewerVR applies to content
		vr.setDoAlign(false);
//		MatrixBuilder.euclidean().rotateX(Math.PI/2).assignTo(world);
		vr.setContent(world);
		// initialize graphics
		ViewerApp va = vr.initialize();
		try {
			vr.importPreferences(Sample04SpringSystem.class.getResourceAsStream("JRBasicDemo.xml"));
		} catch (InvalidPreferencesFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// moving this here seems to avoid deadlock that sometimes appears
		va.setAttachNavigator(true);
		va.setExternalNavigator(false);
		Component insp = getInspector();
		va.addAccessory(insp);
	    Component readme = getReadme();
	    va.addAccessory(readme);
	    va.setFirstAccessory(insp);		// make it the selected tab at startup
	    ViewerKeyListener vkl = new ViewerKeyListener(va.getCurrentViewer(), null, null);
		((Component) va.getCurrentViewer().getViewingComponent()).addKeyListener(vkl);
		va.update();
		va.display();
		viewer = va.getCurrentViewer();
	}
	
	public static void main(String[] args) {
		Sample04SpringSystem demo = new Sample04SpringSystem();
		demo.doIt();
	}
	

}