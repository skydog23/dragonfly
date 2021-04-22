package dragonfly.dynamen.ws08;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.util.TextSlider;
import de.jreality.math.Rn;

/**
 * A simple subclass of {@link ParticleSystemFactory} that has a constant gravity acceleration field.  
 * The particles are initialized in a "body" coordinate system but transformed 
 * into the "space" coordinate system before being used.
 * 
 * When they <i>fall through</i> the floor (in this case the x == -2 plane), 
 * they are re-initialized and ''shot out'' again.
 * 
 * TODO: redo the world coordinate system so that the floor is either a y- or z- constant plane!
 * 
 * There is currently very limited support for differential equation solving.  Instead a naive approach
 * equivalent to a first order Euler solver is used (See {@link #update()}.
 * @author Charles Gunn
 *
 */
public abstract class SpringParticleSystemFactory extends ParticleSystemFactory {

	protected double[] gravityV = {0,0,-.1};	// the gravity vector (constant)
	protected int numParticles = 25;		// number of particles to begin with
	protected double timeStep = .002, 			// time interval for DE solver
		gravity = .1,				// a scalar constant (equivalent to setting all masses to this amount)
		damping = 0.3,
		stiffness = 2;
	double[][] positions, velocities, initialPositions;
	
	public SpringParticleSystemFactory()	{
	}
	
	
	/**
	 * Do one time step 
	 * TODO: synchonize better with wall clock
	 */
	@Override
	public void update()	{
		// update the particle position by the velocity, and the velocity by the acceleration (gravity field)
		// scale the gravity vector by the gravity constant
		double[] scaledGravityV = Rn.times(null, gravity, gravityV);
		int count = 0;
		boolean debug = false;
		double[] impulse = new double[3];
		for (Particle p : particles)	{
			if (! (p instanceof SpringParticle))
				throw new IllegalStateException("Wrong kind of particle");
			if (debug) System.err.println("particle "+count++);
			SpringParticle sp = (SpringParticle) p;
			if (sp.getMass() == 0.0) continue;
			List<Spring> springs = sp.getSprings();
			if (debug) System.err.println("handling "+springs.size()+" springs.");
			double[] force = Rn.times(null, sp.getMass()*gravity, gravityV);
			for (Spring spring : springs)	{
				Particle me = sp;
				Particle you = (spring.p2 == me) ? spring.p1 : spring.p2;
				double[] dp = Rn.subtract(null, you.position, me.position);
				if (debug) System.err.println("dp = "+Rn.toString(dp));
				double length = Rn.euclideanNorm(dp);
				if (length < 10E-3) length = 10E-3;
				double springFactor = stiffness*(length - spring.length)/length;
				Rn.add(force, 
						force, 
						Rn.times(null, springFactor, dp));
				double[] dv = Rn.subtract(null, you.velocity, me.velocity);
				double[] dampingV = Rn.times(null,
					damping*Rn.innerProduct(dv, dp)/length,
					dp);
				Rn.add(force, force, dampingV);
			}
			double[] pos = p.getPosition();
			double[] vel = p.getVelocity();
			Rn.add(impulse, impulse, vel);
			Rn.add(vel, Rn.times(null, timeStep, force), vel);
			Rn.add(pos, Rn.times(null, timeStep, vel), pos);
			if (debug) {
				System.err.println("force = "+Rn.toString(force));
				System.err.println("vel = "+Rn.toString(vel));
				System.err.println("position = "+Rn.toString(pos));
				
			}
//			p.setPosition(pos);
			positions[sp.index] = pos.clone();
			velocities[sp.index] = vel.clone();
//			p.setVelocity(vel);
		}
//		System.err.println("Impulse = "+Rn.euclideanNorm(impulse));
		for (Particle p : particles)	{
			SpringParticle sp = (SpringParticle) p;
			p.setPosition(positions[sp.index]);
			p.setVelocity(velocities[sp.index]);
		}

	}

	public double[] getGravityV() {
		return gravityV;
	}
	public void setGravityV(double[] gravityV) {
		this.gravityV = gravityV;
		eventType = 3;
		broadcastChange(new ParticleSystemEvent(this, eventType));
	}
	public int getNumParticles() {
		return numParticles;
	}
	public void setNumParticles(int numParticles) {
		this.numParticles = numParticles;
		eventType = ParticleSystemEvent.PARAMETER_CHANGE;
		broadcastChange(new ParticleSystemEvent(this, eventType));
	}
	public double getTimeStep() {
		return timeStep;
	}
	public void setTimeStep(double timeStep) {
		this.timeStep = timeStep;
		eventType = ParticleSystemEvent.PARAMETER_CHANGE;
		broadcastChange(new ParticleSystemEvent(this, eventType));
	}
	public double getGravity() {
		return gravity;
	}
	public void setGravity(double gravity) {
		this.gravity = gravity;
		eventType = ParticleSystemEvent.PARAMETER_CHANGE;
		broadcastChange(new ParticleSystemEvent(this, eventType));
	}
	
	/**
	 * Create an inspection panel for this instance.
	 * @return
	 */
	public Component getInspector() {
		Box container = Box.createVerticalBox();

		container.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Particle system parameters"));
		final TextSlider.IntegerLog particleCountSlider = new TextSlider.IntegerLog(
				"particle count", SwingConstants.HORIZONTAL, 1, 100000,
				numParticles);
		particleCountSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				double tmp = particleCountSlider.getValue();
				numParticles = (int) tmp;
				initializeParticles();
			}
		});
		container.add(particleCountSlider);
		final TextSlider.Double timeStepSlider = new TextSlider.Double(
				"time step", SwingConstants.HORIZONTAL, 0.0, .1, timeStep);
		timeStepSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setTimeStep(timeStepSlider.getValue());
			}
		});
		container.add(timeStepSlider);
		final TextSlider.Double posSpreadSlider = new TextSlider.Double(
				"damping", SwingConstants.HORIZONTAL, 0.0, 1,
				damping);
		posSpreadSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				damping = posSpreadSlider.getValue();
				//initializeParticles();
			}
		});
		container.add(posSpreadSlider);
		final TextSlider.DoubleLog velSpreadSlider = new TextSlider.DoubleLog(
				"stiffness", SwingConstants.HORIZONTAL, 0.01, 200,
				stiffness);
		velSpreadSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				stiffness = velSpreadSlider.getValue();
				//initializeParticles();
			}
		});
		container.add(velSpreadSlider);
		final TextSlider.DoubleLog gravslider = new TextSlider.DoubleLog("mass",
				SwingConstants.HORIZONTAL, 0.001, 10, gravity);
		gravslider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setGravity(gravslider.getValue());

			}
		});
		container.add(gravslider);
		
		container.setName("Parameters");
		return container;
	}
}
