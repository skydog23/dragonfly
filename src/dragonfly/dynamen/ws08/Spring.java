package dragonfly.dynamen.ws08;

public class Spring {

	Particle p1, p2;
	public double length, stiffness, damping;
	
	public Spring(Particle p1, Particle p2, double l, double s, double d){
		this.p1 = p1;
		this.p2 = p2;
		length = l;
		stiffness = s;
		damping = d;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Spring)	{
			Spring spr = (Spring) obj;
			return (p1 == spr.p1 && p2 == spr.p2) || (p1 == spr.p2 && p2 == spr.p1);
		}
		return super.equals(obj);
	}
}
