package dragonfly.dynamen.ws08;

import java.util.ArrayList;
import java.util.List;

public class SpringParticle extends Particle {

	List<Spring> springs = new ArrayList<Spring>();
	int index;
	
	public SpringParticle(int i)	{
		index = i;
		velocity = new double[3];
	}
	
	public void addSpring(Spring s)	{
		if (s.p1 != this && s.p2 != this)
			throw new IllegalArgumentException("This spring doesn't belong here");
		springs.add(s);
	}
	
	public void removeSpring(Spring s)	{
		springs.remove(s);
	}
	
	public List<Spring> getSprings() {
		return springs;
	}
	
	public void setSprings(List<Spring> l)	{
		springs = l;
	}
}
