/*
 * Created on Jan 29, 2004
 *
 */
package dragonfly.dynamen;

import static de.jreality.shader.CommonAttributes.DIFFUSE_COLOR;
import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import charlesgunn.anim.core.Animated;
import charlesgunn.anim.core.AnimatedAdaptor;
import charlesgunn.anim.core.KeyFrameAnimatedBoolean;
import charlesgunn.anim.core.KeyFrameAnimatedDelegate;
import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.anim.util.AnimationUtility.InterpolationTypes;
import charlesgunn.jreality.newtools.FlyTool2;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import charlesgunn.math.p5.PlueckerLineGeometry;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.util.CameraUtility;
import de.jreality.util.SceneGraphUtility;


public class TestRigidBody3D extends Assignment {
	{
		Locale.setDefault(Locale.US);
	}
	transient private SceneGraphComponent world;
	transient double momentOfForce = .4, //Math.atan2(2,1)/Math.PI,
		polarPart = 0,
		momentumAngle = 0; 
	transient double[] boxDims = {1,1,1,1};
	transient int metric = Pn.EUCLIDEAN;
	transient boolean canonicalMomentum = true;
	transient RigidBody3D rbode;
	transient ClotheIt3D repn;
	transient protected Viewer viewer;
	transient double initialDeltaT;
	transient JButton runB;
	transient Timer moveit;
	transient int speedFactor;
	@Override
	public SceneGraphComponent getContent()	{
		if (rbode != null) return world;
		rbode = new RigidBody3D(metric);
		initialDeltaT = rbode.ode.getTimeStep();
		world = SceneGraphUtility.createFullSceneGraphComponent("world");

		repn = new ClotheIt3D(rbode, this);
		world.addChild(repn.fullSceneGraph());
		repn.fullSceneGraph().getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
		
		moveit = new Timer(5, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				rbode.doOneStep();
				rbode.broadcastChange();
			}
			
		});

		rbode.addListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				repn.updateSceneGraphRepn();
				viewer.renderAsync();
			}			
		});

		return world;
	}

	transient double[][] falloffs = {{1,.25,0},{.5, .5,0},{.5, .5, 0}};
	transient double[][] cameraClips = {{.01,10},{.01, 1000},{.01,-.05}};
//	double[][] falloffs = {{1,0,0},{1,0,0},{1,0, 0}};
	transient double distance = .4;
	transient double[] unitD = {Math.tanh(distance), distance, Math.tan(distance)};
	private void resetBody()	{
		setRunMotion(false);
		repn.setMetric(metric);
		rbode.setMetric(metric);
		System.err.println("setting metric to "+metric);
		validateDimensions();
		rbode.setBoxDims(boxDims, metric);
//		rbode.setMoments(momentsFromDimn(boxDims, metric));
		rbode.setDims(boxDims);
//		if (viewer == null) return;
		updateMomentumLine();
		rbode.resetMotion();
		updateMoments();
		repn.reset();
		CameraUtility.getCamera(viewer).setNear(cameraClips[metric+1][0]);
		CameraUtility.getCamera(viewer).setFar(cameraClips[metric+1][1]);
		MatrixBuilder.init(null, metric).translate(0,0,2*unitD[metric+1]).assignTo(CameraUtility.getCameraNode(viewer));
		viewer.getSceneRoot().getAppearance().setAttribute("metric", metric);
		viewer.getSceneRoot().getAppearance().setAttribute("fogEnabled", metric != Pn.ELLIPTIC);

		viewer.renderAsync();
	}
	
	private void validateDimensions() {
		if (metric!= Pn.HYPERBOLIC) return;
		// for the hyperbolic metric, the w coordinate must be bigger than the others
		for (int i= 0; i<3; ++i)	{
			if (boxDims[i] > boxDims[3]) boxDims[3] = boxDims[i];
		}
		dimSliders[3].setValue(boxDims[3]);
	}

	private void resetMomentum() {
		resetBody();		// shouldn't have to do this but will anyway
		Rn.setIdentityMatrix(currentMomentumAxisMatrix);
		trackballPanel.getSceneGraphComponent().getTransformation().setMatrix(currentMomentumAxisMatrix);
		rbode.setMomentum(momentumLines);
		updateMomentumLine();
	}
	
	transient private JTextField imTF;
	private void updateMoments() {
		double[] moments = rbode.getMoments();
		imTF.setText(Rn.toString(moments));
	}

	@Override
	public Component getInspector() {
		getContent();
		Box biggie = inspector;
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
				resetBody();
			}
			
		});
		hbox.add(jcb);
		hbox.add(Box.createHorizontalStrut(5));
		JButton resetB = new JButton("Reset body");
		hbox.add(resetB);
		resetB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				runB.setLabel("Run");
				resetBody();
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		runB = new JButton("Run");
		hbox.add(runB);
		runB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if (runB.getText() == "Run") {
					setRunMotion(true);
				} else {
					setRunMotion(false);
				}				
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		final JCheckBox canmomCB = new JCheckBox("Can. mom.");
		hbox.add(canmomCB);
		canmomCB.setSelected(canonicalMomentum);
		canmomCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				canonicalMomentum = canmomCB.isSelected();
				updateCanonicalMomentum();
			}
			
		});
		hbox.add(Box.createHorizontalGlue());
		biggie.add(hbox);
		Box container = Box.createVerticalBox();
		final TextSlider bSlider = new TextSlider.DoubleLog("time step",  SwingConstants.HORIZONTAL, 0.001, 2.0, 1);
		bSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				rbode.ode.setTimeStep(initialDeltaT * bSlider.getValue().doubleValue());
				viewer.renderAsync();
			}
		});
		biggie.add(bSlider);
		biggie.add(Box.createVerticalStrut(5));
		JTabbedPane tabs = new JTabbedPane();
//		tabs.addTab("General", container);
		biggie.add(tabs);

	
		tabs.addTab("Body", container);
		Box vbox = Box.createVerticalBox();
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "Body properties")));
		container.add(vbox);

		final TextSlider cSlider = new TextSlider.DoubleLog("mass",  SwingConstants.HORIZONTAL, 0.001, 4, rbode.getMass());
		cSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				rbode.setMass(cSlider.getValue().doubleValue());
				repn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		vbox.add(cSlider);
		dimSliders = new TextSlider[4];
		String[] labels = {"xDim","yDim","zDim", "wdim"};
		for (int i = 0; i<4; ++i)	{
			dimSliders[i] = new TextSlider.Double(labels[i],  SwingConstants.HORIZONTAL, 0.01, 10, boxDims[i]);
			final int j = i;
			dimSliders[i].addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					boxDims[j] = (((TextSlider) e.getSource()).getValue().doubleValue());
					validateDimensions();
					rbode.setBoxDims(boxDims, metric);
					//rbode.setDims(boxDims);
					updateMoments();
					repn.updateSceneGraphRepn();
					viewer.renderAsync();
				}
			});
			vbox.add(dimSliders[i]);
		}
		imTF = new JTextField("It works fine at the start");
		biggie.add(imTF);
		updateMoments();
		//container.add(Box.createVerticalGlue());
		


		container = Box.createVerticalBox();
		tabs.addTab("Impulse", container);
		vbox = Box.createVerticalBox();
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "Impulse")));
		container.add(vbox);
		hbox = Box.createHorizontalBox();
		hbox.add(Box.createHorizontalGlue());
		hbox.add(Box.createHorizontalStrut(5));
		resetB = new JButton("Reset momentum");
		hbox.add(resetB);
		resetB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				resetMomentum();
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		hbox.add(Box.createHorizontalGlue());
		vbox.add(Box.createVerticalStrut(5));
		vbox.add(hbox);
		vbox.add(Box.createVerticalStrut(5));
		SceneGraphComponent sgc = SceneGraphUtility.createFullSceneGraphComponent();
		sgc.getAppearance().setAttribute(LINE_SHADER+"."+POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white);
		IndexedLineSet ils = IndexedLineSetUtility.createCurveFromPoints(pts, false);
		sgc.setGeometry(ils);
		trackballPanel.getSceneGraphComponent().addChild(sgc);
		trackballPanel.getSceneGraphComponent().getTransformation().addTransformationListener(
				new TransformationListener()	{
					public void transformationMatrixChanged(TransformationEvent ev) {
						ev.getMatrix(currentMomentumAxisMatrix);
						resetBody();
					}
		});
		vbox.add(trackballPanel.getComponent());
//		final TextSlider dSlider = new TextSlider.Double("z/y momentum slope",  SwingConstants.HORIZONTAL, 0, Math.PI, momentumAngle);
//		dSlider.addActionListener(new ActionListener()	{
//			public void actionPerformed(ActionEvent e)	{
//				momentumAngle = dSlider.getValue().doubleValue();
//				reset();
//				repn.updateSceneGraphRepn();
//				viewer.renderAsync();
//			}
//		});
//		container.add(dSlider);
		
		final TextSlider aSlider = new TextSlider.Double("dist to O",  SwingConstants.HORIZONTAL, 0, 1,momentOfForce);
		aSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				momentOfForce = aSlider.getValue().doubleValue();
				resetBody();
				repn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		vbox.add(aSlider);
		final TextSlider eSlider = new TextSlider.Double("polar part",  SwingConstants.HORIZONTAL, 0, 1,polarPart);
		eSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				polarPart = eSlider.getValue().doubleValue();
				resetBody();
//				updateMomentumLine();
				repn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		vbox.add(eSlider);
		container.add(Box.createVerticalGlue());

		tabs.add("Repn", repn.getInspector());
		biggie.setName("Rigid Body");
		return biggie;
	}

	protected void updateCanonicalMomentum() {
	}

	transient double[][] pts = {{0,1,0,1},{1,1,0,1}},		// the line y==1 in the z==0 plane
		flippedPts = {{0,0,1,1},{-1,0,1,1}};
	transient double[] currentMomentumAxisMatrix = Rn.identityMatrix(4);
	transient private TrackballPanel trackballPanel = new TrackballPanel();
	transient private double[] momentumLines = new double[6];
	transient private TextSlider<Double>[] dimSliders;
	transient Matrix yline2mom = new Matrix();
	// TODO document the following
	private void updateMomentumLine() {
		yline2mom = new Matrix(currentMomentumAxisMatrix.clone());
		double factor =  Math.tan(momentOfForce*Math.PI);
		if (factor != 0) MatrixBuilder.euclidean(yline2mom).scale(factor).assignTo(yline2mom); 
		double[][] tformed = Rn.matrixTimesVector(null, yline2mom.getArray(), pts);
		// calculate a homogeneous factor for the points which determine the momentum line
		//tformed[0][3] = tformed[1][3] = (factor == 0) ? 10E16 : 1.0/factor;
		double[] plucker = PlueckerLineGeometry.lineFromPoints(null, tformed[1], tformed[0]);
		momentumLines = PlueckerLineGeometry.permuteCoordinates(null, plucker, new int[]{1,2,3,0});
		if (metric != Pn.EUCLIDEAN)	{
			double[] polar = PlueckerLineGeometry.polarize(null, plucker, metric);
			double[] polarLine = PlueckerLineGeometry.permuteCoordinates(null, polar, new int[]{1,2,3,0});
//			double c = Math.cos(Math.PI*.5*polarPart);
//			double s = Math.sin(Math.PI*.5*polarPart);
			Rn.linearCombination(momentumLines, 1-polarPart, momentumLines, polarPart, polarLine);			
		}
		rbode.setMomentum(Rn.setToLength(momentumLines, momentumLines, 1.0));
//		if (!canonicalMomentum || metric != Pn.HYPERBOLIC) return;
//		if (factor == 1.0) return;
//		if (factor > 1)  {// move to infinity
//			factor = 1.0/factor;
//		}
//		// create a hyperbolic translation moving {0,factor,0,1} to {0,0,0,1}	
//		MatrixBuilder.hyperbolic().translateFromTo(new double[]{0,factor,0,1}, P3.originP3).assignTo(world);
	}

	@Override
	public void display() {
		super.display();
		this.viewer = jrviewer.getViewer();
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", new Color(150, 150, 150));
		viewer.getSceneRoot().getAppearance().setAttribute("fogEnabled", false);
		FlyTool2 flytool = new FlyTool2();
		flytool.setGain(.05);
		flytool.setRotateGain(.05);
		CameraUtility.getCameraNode(viewer).addTool(flytool);
		resetBody();
		updateMomentumLine();
//		trackballPanel.removeStandardTools();
	    AnimationPlugin aplugin = animationPlugin;
	    aplugin.setAnimateSceneGraph(true);
	    aplugin.setAnimateCamera(false);
	    aplugin.resetSceneGraph();
	    aplugin.setDefaultInterp(InterpolationTypes.CUBIC_HERMITE);
	    AnimationPanel ap = aplugin.getAnimationPanel();
	    speedFactor = 50/ap.getRecordPrefs().getFps();
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
	    Animated updater = new AnimatedAdaptor() {
			@Override
			public void startAnimation() {
				setAnimating(true);
			}
			@Override
			public void endAnimation() {
				setAnimating(false);
			}
			// we compute several iterates for each recorded frame
			@Override
			public void setValueAtTime(double t) {
				if (!runMotion) return;
				for (int i = 0; i<speedFactor; ++i) {
					rbode.update();
					repn.updateSceneGraphRepn();
					viewer.renderAsync();
				}
				System.err.println("calling update at "+t+" seconds");
			}
		};
		updater.setName("rbodeUpdater");
		aplugin.getAnimated().add(updater);
	}

	// when we are animating then we disable to automatic run motion timer
	protected void setAnimating(boolean b) {
		animating = b;
		if (animating)	{
			moveit.stop();
		} else {
			if (runMotion) moveit.start();
		}
	}

	transient boolean runMotion = false,
		animating = false;
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

	public static void main(String[] args) {
		new TestRigidBody3D().display();
	}

}
