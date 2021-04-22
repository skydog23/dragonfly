/*
 * Created on Oct 26, 2008
 *
 */
package dragonfly.dynamen.ws08;

import javax.swing.event.ChangeEvent;

public class ParticleSystemEvent extends ChangeEvent {

	public int eventType;
	public static final int PARTICLE_BOUNCE = 1;
	public static final int PARAMETER_CHANGE = 2;
	public static final int FRAME_CHANGE = 3;
	public ParticleSystemEvent(Object source, int type) {
		super(source);
		eventType = type;
	}

}
