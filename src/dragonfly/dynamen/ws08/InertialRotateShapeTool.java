package dragonfly.dynamen.ws08;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import charlesgunn.jreality.tools.ContinuedMotion;
import charlesgunn.jreality.tools.MotionManager;
import de.jreality.math.FactoredMatrix;
import de.jreality.math.Matrix;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.tool.ToolContext;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;


/**
 * This tools behaves just like a regular rotate tool until the user releases the mouse button.
 * Then, instead of continuing the motion as usual, this tool simulates free rigid body rotation
 * using the final angular velocity of the rotation and the instance of {@link ODE} provided by the
 * {@link RigidBodySimulator} given to the constructor.
 * 
 * Important is that the superclass keeps track of the current angular velocity in
 * the field <i>finalAxis</i> and its magnitude in <i>finalAngle</i>.
 *
 */
public class InertialRotateShapeTool extends RotateTool  {
	
	protected final RigidBodySimulatorGunn rbode;
	boolean deactivated = false;
	ContinuedMotion continuedMotion;
	MotionManager mm;
	private double rotAngle;
	private double[] axis;
	Matrix lastPosition;
	
	public InertialRotateShapeTool(RigidBodySimulatorGunn rb) {
		rbode = rb;
	}
	 
	@Override
	public void activate(ToolContext tc) {
		super.activate(tc);
		deactivated = false;
		if (mm != null) mm.removeMotion(continuedMotion);
	}

	@Override
	public void deactivate(ToolContext tc) {
		if (deactivated || !tracking) return;
		deactivated = true;
		lastPosition = new Matrix(selectedComponent.getTransformation().getMatrix());
        FactoredMatrix e = new FactoredMatrix(evolution, Pn.EUCLIDEAN);
        rotAngle = e.getRotationAngle();
        axis = e.getRotationAxis();
         if (rotAngle > Math.PI){
        		rotAngle = 2 * Math.PI - rotAngle;
        		Rn.times(axis, -1, axis);
        }
        if (rotAngle < 0) {
        		rotAngle *= -1;
        		Rn.times(axis, -1, axis);
        }
        System.err.println("dt = "+dt);
        System.err.println("rotation angle = "+rotAngle);
        //if (rotAngle > Math.PI) rotAngle -= 2*Math.PI;
        resetODE();
		if (mm == null) mm = MotionManager.motionManagerForViewer(tc.getViewer());
		if (continuedMotion != null) {
			if (mm != null) mm.removeMotion(continuedMotion);
		}
		continuedMotion = new ContinuedMotion(20, selectedComponent, new ActionListener()	{
			public void actionPerformed(ActionEvent e) {
				rbode.update();
				}
		} );
		if (mm != null) mm.addMotion(continuedMotion);
		return;
	}

	public void removeMotion()	{
		if (mm != null && continuedMotion != null) mm.removeMotion(continuedMotion);
	}
	
	public void resetODE()	{
		if (axis == null) return;
		rbode.setCurrentPosition(lastPosition.getArray());
		rbode.setAngularVelocity(axis);
		rbode.ode.setTimeStep(2*rotAngle);
		rbode.reset();
	}
	
}
