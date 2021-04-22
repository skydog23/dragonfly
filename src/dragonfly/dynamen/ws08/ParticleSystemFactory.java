package dragonfly.dynamen.ws08;

import java.util.List;
import java.util.Vector;

import javax.swing.event.ChangeListener;

import de.jreality.scene.PointSet;
import de.jreality.ui.viewerapp.SelectionListener;

/**
 * This abstract class provides a list of {@link Particle}'s and a
 * listener mechanism for change notification.
 * 
 * Note: I have removed the {@link PointSet} instance which was originally here since it
 * actually is part of the <i>View</i> module, not the <i>Model</i>, IMHO.
 * @author Charles Gunn
 *
 */
abstract public class ParticleSystemFactory {

	protected List<Particle> particles;
	List<ChangeListener> listeners = new Vector<ChangeListener>();
	protected int eventType;
	
	abstract public void initializeParticles();
	
	abstract public void update();
	
	public void addListener(ChangeListener l)	{
		if (listeners.contains(l)) return;
		listeners.add(l);
	}
	
	public void removeSelectionListener(SelectionListener l)	{
		listeners.remove(l);
	}

	public void broadcastChange(ParticleSystemEvent e)	{
		if (listeners == null) return;
		if (!listeners.isEmpty())	{
//			ChangeEvent e = new ParticleSystemEvent(this, eventType); 
			for (int i = 0; i<listeners.size(); ++i)	{
				ChangeListener l =  listeners.get(i);
				l.stateChanged(e);
			}
		}
	}

	public List<Particle> getParticles() {
		return particles;
	}

}
