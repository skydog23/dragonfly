/*
 * Created on Jan 29, 2004
 *
 */
package dragonfly.dynamen;

import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.METRIC;
import static de.jreality.shader.CommonAttributes.TUBES_DRAW;

import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;

import charlesgunn.anim.core.Animated;
import charlesgunn.anim.core.AnimatedAdaptor;
import charlesgunn.anim.core.KeyFrameAnimatedBoolean;
import charlesgunn.anim.core.KeyFrameAnimatedDelegate;
import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.anim.util.AnimationUtility.InterpolationTypes;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import charlesgunn.util.TextSlider;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;


public class TestRigidBody2D extends Assignment {
	transient private SceneGraphComponent 
		world, 
			flatMover, 
			compactMover;
	transient IndexedLineSet avb = null, amb = null;
	transient double momentOfForce = 2;
	transient double[] initialMomentum = {0,1,momentOfForce};		// a line close to the origin
	transient double[] boxDims = {1,1,2};
	transient double mass = 1.0;
	transient int metric = Pn.EUCLIDEAN;
	transient boolean compactMode = false;
	transient RigidBody2D rbode = new RigidBody2D(metric);
	transient ClotheIt flatRepn, compactRepn;
	transient int count = 0;
	transient Viewer viewer;
	transient double initialDeltaT;
	transient TextSlider[] dimSliders;
	transient JButton runB;
	transient Timer moveit;
	@Override
	public SceneGraphComponent getContent()	{
		
		
		rbode.setMomentum(initialMomentum);
		double[] inertiaMoments = RigidBody2D.momentsFromDimn(boxDims, metric);
		rbode.setMoments(inertiaMoments);
		initialDeltaT = rbode.ode.getTimeStep();
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		world.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, true);
		world.getAppearance().setAttribute(METRIC,metric);
		MatrixBuilder.euclidean().translate(0,0,-2).assignTo(world);

		flatRepn = new FlatClotheIt(rbode);
		world.addChild(flatMover = flatRepn.fullSceneGraph());
		flatMover.setVisible(!compactMode);
		compactRepn = new CompactClotheIt(rbode);
		world.addChild(compactMover = compactRepn.fullSceneGraph());
		flatRepn.fullSceneGraph().getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
		compactRepn.fullSceneGraph().getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
		compactMover.setVisible(compactMode);
		
		moveit = new Timer(20, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				rbode.doOneStep();
				rbode.broadcastChange();
			}
			
		});
		

		rbode.addListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
					updateRepn();
					viewer.renderAsync();
			}			
		});

		return world;
	}

	@Override
	public void startAnimation() {
		super.startAnimation();
//		reset();
	}

	@Override
	public void endAnimation() {
		// TODO Auto-generated method stub
		super.endAnimation();
	}

	private void updateRepn() {
		flatRepn.updateSceneGraphRepn();
		if (metric != Pn.HYPERBOLIC || compactMode) compactRepn.updateSceneGraphRepn();
	}

	private String doubleToString(double dd)	{
		return Double.toString(  (   (Math.floor(1000*dd))  * 0.001d)    )   ;
	}

	private void reset()	{
		setRunMotion(false);
		rbode.resetMotion();
		double[] foo = {0,1,momentOfForce};
		Rn.normalize(foo, foo);
		rbode.setMomentum(foo);
		rbode.setMetric(metric);
		rbode.setBoxDims(boxDims, metric); 
		updateMoments();
		flatRepn.reset();
		compactRepn.reset();
		count = 0;
		viewer.renderAsync();
	}

	private void updateMoments() {
		double[] moments = rbode.getMoments();
		imTF.setText(Rn.toString(moments));
	}
	transient final Color URBackground = new Color(.8f, .85f, .68f); //new Color(215, 215, 190);
	transient final Color ULBackground  = new Color(1f, .98f, .8f); //new Color(255, 255, 200);  // bg[1];
	transient final Color LLBackground  = new Color(.35f, .5f, .6f); //new Color(20,20,60);
	transient final Color LRBackground  = new Color(0.2f, .5f, .7f); //new Color(25, 25, 100);  //bg[2];
	transient private TextSlider massSlider;
	transient private JTextField imTF;
	transient double speedFactor = 1.0;
	@Override
	public void display() {
		super.display();
		viewer = jrviewer.getViewer();
		updateMoments();

//		Color[] backgroundArray = new Color[4];
//		backgroundArray[0] = URBackground;
//		backgroundArray[1] = ULBackground;// bg[1];
//		backgroundArray[2] = LLBackground;
//		backgroundArray[3] = LRBackground;  //bg[2];
		Color[] backgroundArray = new Color[4];
		backgroundArray[3] = new Color(0,0,40);
		backgroundArray[2] = new Color(0,20,60);
		backgroundArray[1] = new Color(190,100,140);
		backgroundArray[0] = new Color(160,50,140);

		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColors", backgroundArray);
	    AnimationPlugin aplugin = animationPlugin;
	    aplugin.setAnimateSceneGraph(true);
	    aplugin.setAnimateCamera(false);
//	    aplugin.resetSceneGraph();
	    aplugin.setDefaultInterp(InterpolationTypes.CUBIC_HERMITE);
	    AnimationPanel ap = aplugin.getAnimationPanel();
//	    speedFactor = 25/ap.getRecordPrefs().getFps();
	    KeyFrameAnimatedDelegate<Boolean> dd = new KeyFrameAnimatedDelegate<Boolean>() {

			public Boolean gatherCurrentValue(Boolean t) {
				// TODO Auto-generated method stub
				return isRunMotion();
			}

			public void propagateCurrentValue(Boolean t) {
				setRunMotion(t);
			}
		};
	    KeyFrameAnimatedBoolean runningKF = new KeyFrameAnimatedBoolean(dd);
	    runningKF.setName("running");
	    runningKF.setCurrentValue(isRunMotion());
	    aplugin.getAnimated().add(runningKF);
	    
	    // create an animated variable just to be able to call update
//	    Animated updater = new AnimatedAdaptor() {
//			@Override
//			public void startAnimation() {
//				setAnimating(true);
//			}
//			@Override
//			public void endAnimation() {
//				setAnimating(false);
//			}
//			// we compute several iterates for each recorded frame
//			@Override
//			public void setValueAtTime(double t) {
//				if (!runMotion) return;
////				for (int i = 0; i<speedFactor; ++i) {
//					rbode.update();
//					updateRepn();
//					viewer.renderAsync();
////				}
//				System.err.println("calling update at "+t+" seconds");
//			}
//		};
//		updater.setName("rbodeUpdater");
//		aplugin.getAnimated().add(updater);
	}

	@Override
	public void setValueAtTime(double t) {
//		if (!runMotion) return;
//		for (int i = 0; i<speedFactor; ++i) {
			rbode.update();
			updateRepn();
			viewer.renderAsync();
//		}
		System.err.println("calling update at "+t+" seconds");
	}

	// when we are animating then we disable to automatic run motion timer
	transient boolean runMotion = false,
		animating = false;
	protected void setAnimating(boolean b) {
		animating = b;
		if (animating)	{
			moveit.stop();
		} else {
			if (runMotion) moveit.start();
		}
	}

	public boolean isRunMotion() {
		return runMotion;
	}

	public void setRunMotion(boolean b) {
		if (runMotion == b) return;
		runMotion = b;
		System.err.println("Setting run motion "+b);
		if (runMotion) {
			runB.setText("Pause");
			if (!animating) moveit.start();
		} else {
			runB.setText("Run");
			if (!animating) moveit.stop();
		}				

	}

	@Override
	public Component getInspector() {
//		Component container = super.getInspector();
		Box container = inspector;
		Box hbox = Box.createHorizontalBox();
		JComboBox jcb = new JComboBox(new String[]{"Euclidean", "Hyperbolic", "Elliptic"});
	    hbox.setBorder(new EmptyBorder(5,10,5,10));
	    int height = (int)(jcb.getPreferredSize().getHeight());
	    hbox.setMaximumSize(new Dimension(1000, height));   
	    jcb.setMaximumSize(new Dimension(1000, height));   
	    jcb.setAlignmentX(Component.LEFT_ALIGNMENT);
		jcb.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				switch(((JComboBox)e.getSource()).getSelectedIndex()) {
				case 0:
					metric = Pn.EUCLIDEAN;
					break;
				case 1:
					metric = Pn.HYPERBOLIC;
					break;
				case 2:
					metric = Pn.ELLIPTIC;
					break;
				}
				rbode.setMass(metric == Pn.EUCLIDEAN ? 1.0: 0.25);
				massSlider.setValue(rbode.getMass());
				compactRepn.setMetric(metric);
				flatRepn.setMetric(metric);
				reset();
			}
			
		});
		hbox.add(jcb);
		hbox.add(Box.createHorizontalStrut(5));
		final JCheckBox compactModeB = new JCheckBox("Compact view");
		hbox.add(compactModeB);
		compactModeB.setSelected(compactMode);
		compactModeB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				compactMode = compactModeB.isSelected();
				compactMover.setVisible(compactMode);
				flatMover.setVisible(!compactMode);
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		JButton resetB = new JButton("Reset");
		hbox.add(resetB);
		resetB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				runB.setLabel("Run");
				reset();
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		runB = new JButton("Run");
		hbox.add(runB);
		runB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if (runB.getLabel() == "Run") {
					runB.setLabel("Pause");
					setRunMotion(true);
				} else {
					runB.setLabel("Run");
					setRunMotion(false);
				}
				
			}
			
		});
		hbox.add(Box.createHorizontalGlue());
		container.add(hbox);
		hbox = Box.createHorizontalBox();
		container.add(hbox);
		final JButton colorb = new JButton("pos color ");
		//colorb.setBackground(posColor);
		colorb.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				Color color = JColorChooser.showDialog(((Component)viewer.getViewingComponent()), "Select color ",  null);
				if (color != null) {
					compactRepn.positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, color);
					flatRepn.positionCurveSGC.getAppearance().setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, color);
					colorb.setForeground(color);
				}
			}
		});
		hbox.add(colorb);
		final TextSlider aSlider = new TextSlider.DoubleLog("moment of force",  SwingConstants.HORIZONTAL, .01, 1000,momentOfForce);
		aSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				momentOfForce = aSlider.getValue().doubleValue();
				double[] foo = {0,1,momentOfForce};
				Rn.normalize(foo, foo);
				reset();
				rbode.setMomentum(foo);
				flatRepn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		container.add(aSlider);
		final TextSlider bSlider = new TextSlider.Double("time step",  SwingConstants.HORIZONTAL, 0.0, 4, 1);
		bSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				rbode.ode.setTimeStep(initialDeltaT * bSlider.getValue().doubleValue());
				viewer.renderAsync();
			}
		});
		container.add(bSlider);
		massSlider = new TextSlider.Double("mass",  SwingConstants.HORIZONTAL, 0.0, 4, mass);
		massSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				mass = (massSlider.getValue().doubleValue());
				rbode.setMass(mass);
				reset();
				flatRepn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		container.add(massSlider);
		dimSliders = new TextSlider[3];
		String[] labels = {"xDim","yDim","zDim"};
		for (int i = 0; i<3; ++i)	{
			dimSliders[i] = new TextSlider.Double(labels[i],  SwingConstants.HORIZONTAL, 0.0, 10, boxDims[i]);
			final int j = i;
			dimSliders[i].addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					boxDims[j] = (((TextSlider) e.getSource()).getValue().doubleValue());
					validateBoxDims();
					rbode.setBoxDims(boxDims, metric);
					flatRepn.updateSceneGraphRepn();
					updateMoments();
					reset();
					viewer.renderAsync();
				}
			});
			container.add(dimSliders[i]);
		}
		imTF = new JTextField("It works fine at the start");
		container.add(imTF);
		container.setName("Parameters");
//		container.add(flatRepn.getViewer2d());
		container.add(rbode.ode.getInspector());
		container.add(Box.createVerticalGlue());
		return container;
	}


	protected void validateBoxDims() {
		if (metric != Pn.HYPERBOLIC) return;
		if (boxDims[0] > boxDims[2]) boxDims[2] = boxDims[0]+.01;
		if (boxDims[1] > boxDims[2]) boxDims[2] = boxDims[1]+.01;
		dimSliders[2].setValue(boxDims[2]);
	}

	public static void main(String[] args) {
		new TestRigidBody2D().display();
	}
}
