package dragonfly.dynamen.ws08;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
public class SimpleParticleSystemFactory extends ParticleSystemFactory {

	protected double[] gravityV = {-1,0,0};	// the gravity vector (constant)
	protected int numParticles = 1000;		// number of particles to begin with
	protected double[] bodyToSpace, 			// transformation from body to space coordinate system
		spaceToBody;				// the inverse
	protected double velocitySpread = .15, 	// how much the initial velocity vectors diverge from (0,0,1)
		positionSpread = .2, 		// radius of disk in (x,y) plane for initial positions
		speedSpread = 1.0;			// speed (length of initial velocity) vary from 1 to 1+speedSpread
	protected double timeStep = .03, 			// time interval for DE solver
		gravity = .35;				// a scalar constant (equivalent to setting all masses to this amount)
	
	public SimpleParticleSystemFactory()	{
		setBodyToSpace(Rn.identityMatrix(4));
	}
	
	/**
	 * This is called to start the whole simulation over again.
	 */
	@Override
	public void initializeParticles() {
		if (particles == null) particles = new ArrayList<Particle>();
		particles.clear();
		for (int i = 0; i<numParticles; ++i)	{
			Particle p = getNewParticle(new Particle());
			particles.add(p);
		}
	}

	/**
	 * The new particles are always oriented so that the average particle
	 * is positioned at (0,0,0) with velocity (0,0,1)
	 * @param p
	 * @return
	 */
	public Particle getNewParticle(Particle p) {
		double r1 = Math.random(), r2 = Math.random();
		double x = positionSpread * r1 * Math.cos(Math.PI*2*r2);
		double y = positionSpread * r1 * Math.sin(Math.PI*2*r2);
		double r3 = Math.random(), r4 = Math.random();
		double xx = velocitySpread * r3 * Math.cos(Math.PI*2*r4);
		double yy = velocitySpread * r3 * Math.sin(Math.PI*2*r4);
		
		p.setPosition(new double[]{x,y,0});
		p.setVelocity(Rn.setToLength(null, new double[]{xx,yy,1.0}, 1.0 + speedSpread*Math.random()));
		p.transformBy(bodyToSpace);
		return p;
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
		for (Particle p : particles)	{
			double[] pos = p.getPosition();
			double[] vel = p.getVelocity();
			Rn.add(pos, Rn.times(null, timeStep, vel), pos);
			Rn.add(vel, Rn.times(null, timeStep, scaledGravityV), vel);
			p.setPosition(pos);
			p.setVelocity(vel);
			if (pos[0] < -2) {
				getNewParticle(p);
				eventType = ParticleSystemEvent.PARTICLE_BOUNCE;
				broadcastChange(new ParticleSystemEvent(p, eventType));
			}
		}
	}

	/*
	 * various get and set methods follow here
	 * The set methods generally generate a <i>broadcastChange()<i> call.
	 */
	public double[] getBodyToSpace() {
		return bodyToSpace;
	}

	public void setBodyToSpace(double[] m) {
		this.bodyToSpace = m;
		spaceToBody = Rn.inverse(null, bodyToSpace);
		eventType = 2;
		broadcastChange(new ParticleSystemEvent(this, eventType));
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
	public double getPositionSpread() {
		return positionSpread;
	}
	public void setPositionSpread(double positionSpread) {
		this.positionSpread = positionSpread;
		eventType = ParticleSystemEvent.PARAMETER_CHANGE;
		broadcastChange(new ParticleSystemEvent(this, eventType));
	}
	public double getSpeedSpread() {
		return speedSpread;
	}
	public void setSpeedSpread(double speedSpread) {
		this.speedSpread = speedSpread;
		eventType = ParticleSystemEvent.PARAMETER_CHANGE;
		broadcastChange(new ParticleSystemEvent(this, eventType));
	}
	public double getVelocitySpread() {
		return velocitySpread;
	}
	public void setVelocitySpread(double velocitySpread) {
		this.velocitySpread = velocitySpread;
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
				"position spread", SwingConstants.HORIZONTAL, 0.0, 1,
				positionSpread);
		posSpreadSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setPositionSpread(posSpreadSlider.getValue());
			}
		});
		container.add(posSpreadSlider);
		final TextSlider.Double velSpreadSlider = new TextSlider.Double(
				"velocity spread", SwingConstants.HORIZONTAL, 0.0, 2,
				velocitySpread);
		velSpreadSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setVelocitySpread(velSpreadSlider.getValue());
			}
		});
		container.add(velSpreadSlider);
		final TextSlider.Double velslider = new TextSlider.Double(
				"speed spread", SwingConstants.HORIZONTAL, 0.0, 1, speedSpread);
		velslider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setSpeedSpread(velSpreadSlider.getValue());
			}
		});
		container.add(velslider);
		final TextSlider.Double gravslider = new TextSlider.Double("gravity",
				SwingConstants.HORIZONTAL, 0.0, 2, gravity);
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
