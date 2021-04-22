/*
 * Created on Mar 23, 2004
 *
 */
package dragonfly.dynamen.attic;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;

import charlesgunn.jreality.newtools.Rotator;
import charlesgunn.jreality.tools.ContinuedMotion;
import charlesgunn.jreality.tools.RotateShapeTool;
import charlesgunn.jreality.tools.ToolManager;
import de.jreality.jogl.plugin.HelpOverlay;
import de.jreality.math.FactoredMatrix;
import de.jreality.math.Quaternion;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Transformation;
import de.jreality.scene.tool.ToolContext;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;

/**
 * @author Charles Gunn
 *
 */
public class InertialRotateShapeTool extends RotateShapeTool  {
	Rotator 	theRotator;
	double	theAngle;

	public InertialRotateShapeTool() {
		super();
		theRotator = new Rotator();
	}
	 

	private double _time = 0.0;
	public void deactivate(ToolContext tc) {

		boolean rigidbody = false;
		Object obj = null;
		if (theEditedNode.getAppearance() != null)	{
			obj = theEditedNode.getAppearance().getAttribute("rigidBodyODE");
			if (obj != null && obj instanceof RigidBodySO3ODE)	rigidbody = true;
		}

		if (rigidbody)	{
			subclassHandlesDeactivate = true;
			super.deactivate(tc);
			if (!isTracking) return;
			final Extrap extrap = new Extrap(7);
			extrap.setAbsTol(10E-10);
			final RigidBodySO3ODE rbode = (RigidBodySO3ODE) obj;
			final ODE ode = rbode.getODE();
			//Rn.times(axis, angle, axis);
			final double deltaT = finalAngle;
			final double[] matrix = new double[16];
			final double[] angularVelocity = new double[3];
			FactoredMatrix fm = new FactoredMatrix(theEditedTransform);
			Quaternion qq = fm.getRotationQuaternion();
			final double[] solution = {qq.re, qq.x, qq.y, qq.z,finalAxis[0], finalAxis[1], finalAxis[2]};
			//System.err.println("In rigid body motion rotate");
			if (theEditedNode != rbode.getSceneGraphRepresentation() &&
					!theEditedNode.isDirectAncestor(rbode.getSceneGraphRepresentation()))
				theEditedNode.addChild(rbode.getSceneGraphRepresentation());
			continuedMotion = new ContinuedMotion(20, (SceneGraphComponent) theEditedNode, new ActionListener()	{
				final Transformation tt = theEditedTransform;
				public void actionPerformed(ActionEvent e) {
					extrap.odex(ode, solution, 0, deltaT);	
					Quaternion motion = new Quaternion(solution[0],solution[1], solution[2], solution[3]);
					Quaternion.quaternionToRotationMatrix(matrix, motion);
					tt.setMatrix(matrix);
					System.arraycopy(solution, 4, angularVelocity, 0, 3);
					rbode.setAngularVelocity(angularVelocity);
					rbode.update();

					viewer.render();
				}
//				public void stop()	{
//					if (theEditedNode.isDirectAncestor(rbode.getSceneGraphRepresentation()))
//						theEditedNode.removeChild(rbode.getSceneGraphRepresentation());
//					super.stop();
//				}
			} );
			if (mm!= null) mm.addMotion(continuedMotion);			
		} else {
			subclassHandlesDeactivate = false;
			super.deactivate(tc);
		}
		return;
	}
	
	public ImageIcon getIcon(int size) {
		return ToolManager.createImageIcon(size ==  ToolManager.LARGE? 
				"inertialRotateTool-32.png" : "inertialRotateTool-24.png", this.getClass());
	}

	@Override
	public void registerHelp(HelpOverlay overlay) {
		overlay.registerInfoString("Inertial Rotate tool", 
		"Same as rotate tool (see below) but integrates Euler's eqns when mouse is released");
		overlay.registerInfoString("", 
		"This only works if selected SGC has Appearance attribute 'rigidBodyODE' ");
		overlay.registerInfoString("", 
		"set to an instance of charlesgunn.dynamen.RigidBodyODE ");
		super.registerHelp(overlay);
	}

	
}
