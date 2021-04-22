package dragonfly.dynamen;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.util.TextSlider;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;

public abstract class RBMOde {
	protected RigidBodyInterface rigidBodySimulator;
	protected ODE ode;
	protected Extrap extrap;
	protected double timeStep = .01;
	protected double[] solution;
	protected int velocityOffset;
	public double cc = 1;
	
	public RBMOde(RigidBodyInterface rbs)	{
		rigidBodySimulator = rbs;
	}
	
	public abstract void update();
	public abstract void reset();
	
	public ODE getODE()	{
		if (extrap == null) extrap = new Extrap(ode.getNumberOfEquations());
		return ode;
	}
	
	public double getTimeStep() {
		return timeStep;
	}

	public void setTimeStep(double timeStep) {
		this.timeStep = timeStep;
	}
	
	public Component getInspector() {
		Box container = Box.createVerticalBox();
		final TextSlider cSlider = new TextSlider.Double("constant",  SwingConstants.HORIZONTAL, 0.0,2, cc);
		cSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				cc = (cSlider.getValue().doubleValue());
			}
		});
		container.add(cSlider);
		return container;
	}

	abstract public void setVelocity(double[] velocity);


}
