package dragonfly.dynamen.ws08;

import static de.jreality.shader.CommonAttributes.POINT_SHADER;
import static de.jreality.shader.CommonAttributes.SPHERES_DRAW;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;
import java.util.prefs.InvalidPreferencesFormatException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.Timer;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import charlesgunn.jreality.tools.MotionManager;
import charlesgunn.jreality.tools.RotateShapeTool;
import de.jreality.geometry.PointSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.plugin.experimental.ViewerKeyListener;
import de.jreality.scene.Cylinder;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.SelectionManager;
import de.jreality.ui.viewerapp.SelectionManagerImpl;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.util.SceneGraphUtility;
import de.jreality.util.Secure;
import de.jreality.vr.ViewerVR;

/**
 * A wrapper class for accessing an instance of {@link SimpleParticleSystemFactory}.  This wrapper class
 * is basically responsible for building a scene graph representation for the particle system and for
 * controlling the dynamics (run/pause and reset). This includes managing an instance of {@link PointSet}
 * which represents the particle system.  It also contains a SceneGraphComponent node containing a special
 * tool for rotating the particle system with respect to the world coordinate system, and notifying
 * the particle system of these changes.
 * 
 * @author Charles Gunn
 *
 */
public class Sample01ParticleSystem {

	protected SceneGraphComponent 	// the indentation here shows the tree structure of the scene graph
		world, 	// contains everything
		 	cylinderSGC, 	// contains the particles and the particle "shooter"
		 		cylinderToolSGC,	// a node just for rotating the "shooter"
		 			cylinderProper,	// a node containing the scaled and translated cylinder
		 		particlesSGC, 	// the point set containing the particles
		 	sinkSGC;	// the surface where the particles disappear and start over again.
	protected SimpleParticleSystemFactory psf;
	protected transient Color[] baseColors = {Color.yellow, Color.green};
	protected transient PointSetFactory particlePointSet = new PointSetFactory();
	protected boolean active = false;	// sets whether particles are drawn with real spheres or not
	protected boolean cheapSpheres = false;
	protected Timer timer; 		// used to drive the evolution of the particle system.
	protected Viewer viewer;
	
	/**
	 * create and return a scene graph containing a graphical representation of the particle system
	 */
	protected SceneGraphComponent makeWorld()	{
		timer = new Timer(20, new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				psf.update();
				updatePointSet();
			}
			
		});
		if (active) timer.start();

		// Allocate and initialize the particle system
		psf = new SimpleParticleSystemFactory();
		psf.initializeParticles();
		psf.addListener(getChangeListener());
		// initialize the point set representation
		updatePointSet();
		
		// Allocate the scene graph components
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		cylinderSGC = SceneGraphUtility.createFullSceneGraphComponent("cylinder");
		cylinderToolSGC = SceneGraphUtility.createFullSceneGraphComponent("cylinder tool");
		particlesSGC = SceneGraphUtility.createFullSceneGraphComponent("particles");
		cylinderProper = SceneGraphUtility.createFullSceneGraphComponent("cylinder proper");
		sinkSGC = SceneGraphUtility.createFullSceneGraphComponent("sink");
		// construct scene graph
		world.addChildren(cylinderSGC, sinkSGC);
		cylinderSGC.addChildren(cylinderToolSGC, particlesSGC);
		cylinderToolSGC.addChild(cylinderProper);
		// set geometries
		particlesSGC.setGeometry(particlePointSet.getPointSet());
		cylinderProper.setGeometry(new Cylinder());
		// we use a non-standard rotate tool in order to easily rotate around an axis perpendicular to the screen
		RotateShapeTool rt = new RotateShapeTool();
		cylinderToolSGC.addTool(rt);
		
		// we need to update the particle system with the new body-to-space 
		// coordinate transformation, which is given by the position of the yellow cylinder.
		// We use a TransformationListener to be notified of all changes
		cylinderToolSGC.getTransformation().addTransformationListener(new TransformationListener() {
			public void transformationMatrixChanged(TransformationEvent ev) {
				psf.setBodyToSpace(cylinderToolSGC.getTransformation().getMatrix());
			}
		});
		// Set appearances and transformations
		cylinderProper.getAppearance().setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.yellow);
		MatrixBuilder.euclidean().scale(psf.getPositionSpread(),psf.getPositionSpread(), 2)
			.translate(0,0,-1).assignTo(cylinderProper);

		// following settings are only relevant when we're drawing cheap spheres
		particlesSGC.getAppearance().setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		particlesSGC.getAppearance().setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.POINT_SIZE, 200.0);
		// we don't want to waste time picking the particles
		particlesSGC.setPickable( false);
		// set up the sink geometry: a big disk
		sinkSGC.getAppearance().setAttribute(CommonAttributes.DIFFUSE_COLOR, new Color(0,200,255));
		sinkSGC.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, false);
		sinkSGC.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);		
		sinkSGC.setGeometry(Primitives.regularPolygon(100));
		MatrixBuilder.euclidean().translate(-2, 0, 0).rotateY(Math.PI/2).scale(20,20,1).assignTo(sinkSGC);
		// world transformation depends on whether we're using ViewerVR or not.
		if (!useViewerVR) MatrixBuilder.euclidean()
			.translate(0,0,3)
			.rotateZ(Math.PI/2)
			.rotateX(-Math.PI/2)
			.assignTo(world);
		else {
			// the ViewerVR coordinate system is somewhat different than the ViewerApp one
			MatrixBuilder.euclidean()
				.translate(0,0,2)
				.rotateX(0) //Math.PI)
				.rotateY(-Math.PI/2)
				.assignTo(world);	
			sinkSGC.setVisible(false);
		}
		return world;
	}

	protected ChangeListener getChangeListener() {
		return new ChangeListener() {
			// we need to be updated when the state changes since the shape of the cylindrical "shooter"
			// depends on a property of the particle system
			public void stateChanged(ChangeEvent e) {
				MatrixBuilder.euclidean().scale(psf.getPositionSpread(), psf.getPositionSpread(), 2).translate(0,0,-1).assignTo(cylinderProper);	
			}
			
		};
	}

	protected double[][] positions;	// cache these in case size doesn't change
	protected double[][] velocities;
	public void updatePointSet() {
		List<Particle> particles = psf.getParticles();
		int n = particles.size();
		if (n == 0) return;
		if (positions == null || positions.length != n)
			positions = new double[n][];
		if (velocities == null || velocities.length != n)
			velocities = new double[n][];
		int count = 0;
		double[][] baseColorsD = new double[2][3];
		for (int i = 0; i<2; ++i)	{
			float[] rgb = baseColors[i].getRGBColorComponents(null);
			for (int j = 0; j<3; ++j)	baseColorsD[i][j] = rgb[j];
		}
		double[][] colors = new double[n][3];
		// get positions and velocities from particle list and generate color as linear span.
		for (Particle p : particles)	{
			double frac = count/(n-1.0);
			positions[count] = p.getPosition();
			velocities[count] = p.getVelocity();
			// currently not doing anything with velocity but could use to calculate color!
			Rn.linearCombination(colors[count], 1-frac, baseColorsD[0], frac, baseColorsD[1]);
			count++;
		}
		// set the PointSetFactory and update it. (Its PointSet instance remains the same throughout.)
		particlePointSet.setVertexCount(n);
		particlePointSet.setVertexCoordinates(positions);
		particlePointSet.setVertexColors(colors);
		particlePointSet.update();
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
				psf.initializeParticles();
				updatePointSet();
				viewer.renderAsync();
			}

		});
		hbox.add(Box.createHorizontalStrut(5));
		final JCheckBox sphereCB = new JCheckBox();
		sphereCB.setText("cheap spheres");
		hbox.add(sphereCB);
		sphereCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				cheapSpheres = sphereCB.isSelected();
				particlesSGC.getAppearance().setAttribute(
						POINT_SHADER + "." + SPHERES_DRAW, !cheapSpheres);
			}

		});
		container.add(hbox);
		container.add(psf.getInspector());
		// add color inspector
		hbox = Box.createHorizontalBox();
		hbox.setPreferredSize(new Dimension(300, 80));
		hbox.setMaximumSize(new Dimension(300, 80));
		hbox.add(Box.createHorizontalGlue());
		hbox.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Particle colors"));
		for (int i = 0; i<2; ++i)	{
			final JButton colorsb = new JButton("color "+i);
			colorsb.setBackground(baseColors[i]);
			final int j = i;
			colorsb.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					Color color = JColorChooser.showDialog(null, "Select color ",  null);
					if (color != null) {
						colorsb.setBackground(color);
						baseColors[j] = color;
						updatePointSet();
					}
				}
			});
			hbox.add(colorsb);
			if (i == 0) hbox.add(Box.createHorizontalStrut(5));
		}
		hbox.add(Box.createHorizontalGlue());
		container.add(hbox);
//		container.add(Box.createVerticalGlue());
		container.setName("Parameters");
		Dimension d = container.getPreferredSize();
		container.setPreferredSize(new Dimension(d.width / 2, d.height));
		return container;
	}

	/**
	 * Create a simple documentation panel for the application.
	 */
	protected Component getReadme()	{
		JPanel panel = new JPanel();
		panel.setName("ReadMe");
		JTextArea textarea = new JTextArea(10,20);
		textarea.setEditable(false);
		textarea.append("This is a simple demo of particle systems.\n"+
				"That is, a set of particles each \n"+
				"with an position and velocity\n"+
				"subject to a uniform gravitational field.\n\n"+
				"Click on the tab labeled 'Parameters'\n"+
				"to see the control panel for the application.\n"+
				"Toggling the 'run/pause' button will start/stop \n" +
				"the movement of the particles.\n\n"+
				"The yellow cylinder is a particle source.\n"+
				"The initial orientation and position of the \n"+
				"particle stream is determined by the\n"+
				"yellow cylinder, which can be rotated\n"+
				"separately from the rest of the scene.\n"+
				"Hint: middle mouse rotates around an axis\n"+
				"perpendicular to the screen.\n\n"+
				"Currently, when the particles reach\n"+
				"a given elevation, they are recycled\n"+
				"(ejected from the cylinder again).\n\n"+
				"The check box labeled 'cheap spheres' toggles\n"+
				"a much faster, but lower quality sphere\n"+
				"rendering method\n\n"+
				"Keyboard controls:\n"+
//				"    '1':    toggle terrain texture\n"+
				"    'h':    toggle display of help overlay for viewer\n"+
				"    'i':    toggle display of performance overlay\n\n"+
				"Use mouse click wheel to zoom in and out.\n"+
				"Shift-cntl-f  toggles fullscreen mode.\n"+
				"Click on the tab 'Scene Graph' to explore structure\n"+
				"\nAuthor: Charles Gunn\n"+
				"    gunn at math.tu-berlin.de\n");
		panel.add(textarea);
		return panel;
	}
	protected static boolean useViewerVR = false;
	public static void main(String[] args) {
		final Sample01ParticleSystem ps = new Sample01ParticleSystem();
		ps.doIt();
	}

	protected void doIt() {
		// to run the application inside of ViewerVR, set the VM argument 
		// -DuseViewerVR=true  
		// In eclipse: Run->Run..., click on "Arguments", enter this in "VM Arguments" text field
		String foo = Secure.getProperty("useViewerVR");
		if (foo != null && foo.equals("true")) useViewerVR = true;
		ViewerApp va = null;
		// This doesn't work yet
		if (useViewerVR)	{
			ViewerVR vr=ViewerVR.createDefaultViewerVR(null);
			vr.setDoAlign(false);
			vr.setContent(makeWorld());
			va=vr.initialize();
			try {
				vr.importPreferences(Sample01ParticleSystem.class
						.getResourceAsStream("Sample01VR.xml"));
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidPreferencesFormatException e) {
				e.printStackTrace();
			}
			vr.setAvatarPosition(0, 0, 6);
		} else {
			va = new ViewerApp(makeWorld());
		}
		va.setAttachNavigator(true);
		va.setExternalNavigator(false);
		va.setAttachBeanShell(false);
		va.setExternalBeanShell(false);
	    Component insp = getInspector();
	    va.addAccessory(insp);
	    Component readme = getReadme();
	    va.addAccessory(readme);
	    va.setFirstAccessory(readme);		// make it the selected tab at startup
	    Viewer v = viewer = va.getCurrentViewer();
	    // set the background colors
		Color[] backgroundArray = new Color[4];
		backgroundArray[0] = new Color(0,0,40);
		backgroundArray[1] = new Color(0,20,60);
		backgroundArray[2] = new Color(50,50,70);
		backgroundArray[3] = new Color(40,20,70);
	    v.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLORS, backgroundArray);
	    // the following method turns off inertia in the rotate tool, so it doesn't continue to rotate 
	    // after releasing the mouse
	    MotionManager.motionManagerForViewer(v).setAllowMotion(false);
	    // add a full-featured key listener to the viewing component which allows lots of
	    // keyboard shortcuts for common scene graph editing commands (type 'h' for help)
	    ViewerKeyListener vkl = new ViewerKeyListener(v, null, null);
		((Component) v.getViewingComponent()).addKeyListener(vkl);
		// It's important for some of the key listener actions that there is a valid
		// selection in the selection manager
		SelectionManager sm = SelectionManagerImpl.selectionManagerForViewer(v);
		List<SceneGraphPath> paths = SceneGraphUtility.getPathsBetween(v.getSceneRoot(), world);
		sm.setSelectionPath(paths.get(0));
		// finally, draw the picture
		va.update();
		JFrame frame = va.display();
		frame.setTitle("Particle system");
	}

}
