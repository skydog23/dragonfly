package dragonfly.dynamen.ws08;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JPanel;

import charlesgunn.anim.core.Animated;
import charlesgunn.anim.core.AnimatedAdaptor;
import charlesgunn.anim.core.KeyFrameAnimatedBoolean;
import charlesgunn.anim.core.KeyFrameAnimatedDelegate;
import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.jreality.geometry.Snake;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.CameraUtility;
import de.jreality.util.SceneGraphUtility;

/**
 * This class implements a simple demonstration of simulation of the free motion of a rigid body 
 * fixed at its center of gravity (no external forces).
 * 
 * It does this for a simple rectangular box.  It attaches a modified rotation tool {@link InertialRotateShapeTool}
 * to the component containing the box.  This tool rotates normally until the user releases the mouse. At this point
 * it determines the angular velocity of the rotation and uses that as the initial condition for integrating the
 * equations of motion for the box.
 * 
 * @author Charles Gunn
 *
 */
public class MyEulerTop  extends Assignment {

	protected RigidBodySimulatorGunn rbode = new RigidBodySimulatorGunn();;
	protected InertialRotateShapeTool irst;
	protected boolean toolActive = true;
	SceneGraphComponent invPlaneSGC, invPlaneGeomSGC;
	Snake invPlaneCurve;
	int limit = 10000;
	double[][] points = new double[limit][3];
	int count = 0;
	final static double[] zaxis = {0,0,1};
	
	@Override
	public SceneGraphComponent getContent()	{
		Locale.setDefault(Locale.US);
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		
		// create an object for simulating rigid body motion in the rotation group SO3.
		irst = new InertialRotateShapeTool(rbode);
		rbode.getBodySceneGraphRepresentation().addTool(irst);
		// the tool provides a graphical representation of the motion:
		// angular momentum, angular velocity, and inertial ellipsoid
		world.addChild(rbode.getSceneGraphRepresentation());
		MatrixBuilder.euclidean().rotateX(Math.PI/2).assignTo(world);
		return world;
	}

	@Override
	public void startAnimation() {
		// TODO Auto-generated method stub
		super.startAnimation();
		rbode.setRunning(false);
	}

	@Override
	public void endAnimation() {
		// TODO Auto-generated method stub
		super.endAnimation();
		rbode.setRunning(true);
	}

	@Override
	public void setValueAtTime(double d) {
		// TODO Auto-generated method stub
//		rbode.update();
	}

	boolean pause = false;
	Box hbox = Box.createHorizontalBox();
	@Override
	public Component getInspector() {
		Box vbox = Box.createVerticalBox();
		inspector.add(vbox);
		vbox.add(hbox);
		JCheckBox activeB = new JCheckBox("Activate tool");
		hbox.add(activeB);
		activeB.setSelected(toolActive);
		activeB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				toolActive = ((JCheckBox) e.getSource()).isSelected();
				if (toolActive) rbode.getBodySceneGraphRepresentation().addTool(irst);
				else rbode.getBodySceneGraphRepresentation().removeTool(irst);
			}
			
		});
		JButton restartB = new JButton("Reset ODE");
		hbox.add(restartB);
		restartB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				irst.resetODE();
			}
			
		});

		vbox.add(rbode.getInspector());
		return inspector;
	}

	@Override
	public void display() {// set the background colors
		super.display();
		MatrixBuilder.euclidean().translate(0,.5,0).assignTo(CameraUtility.getCameraNode(jrviewer.getViewer()));

		Color[] backgroundArray = new Color[4];
		backgroundArray[3] = new Color(0,0,40);
		backgroundArray[2] = new Color(0,20,60);
		backgroundArray[1] = new Color(190,100,140);
		backgroundArray[0] = new Color(160,50,140);
	    jrviewer.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLORS, backgroundArray);

	    AnimationPlugin aplugin = animationPlugin;
	    aplugin.setAnimateSceneGraph(false);
	    aplugin.resetSceneGraph();
	    KeyFrameAnimatedDelegate<Boolean> dd = new KeyFrameAnimatedDelegate<Boolean>() {

			public Boolean gatherCurrentValue(Boolean t) {
				// TODO Auto-generated method stub
				return rbode.isRunning();
			}

			public void propagateCurrentValue(Boolean t) {
				rbode.setRunning(t);
			}
		};
	    KeyFrameAnimatedBoolean runningKF = new KeyFrameAnimatedBoolean(dd);
	    runningKF.setName("running");
	    runningKF.setCurrentValue(rbode.isRunning());
	    aplugin.getAnimated().add(runningKF);
	    
	    // create an animated variable just to be able to call update
	    Animated updater = new AnimatedAdaptor() {
			@Override
			public void startAnimation() {
				// here we should disable the motion attached to the inertial rotate tool
				irst.removeMotion();
			}
			@Override
			public void setValueAtTime(double t) {
				rbode.update();
			}
		};
		updater.setName("rbodeUpdater");
		aplugin.getAnimated().add(updater);
		
//		KeyFrameAnimatedTransformation worldKF = new KeyFrameAnimatedTransformation(world.getTransformation());
//		aplugin.getAnimated().add(worldKF);
	}
		
	public static void main(String[] args) {
		new MyEulerTop().display();
	}

}
