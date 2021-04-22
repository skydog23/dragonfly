package dragonfly.dynamen.ws08;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.util.TextSlider;

import com.bulletphysics.dynamics.RigidBody;


public class PhysicalParametersInspector {

	RigidBody[] bodies = null;
	float activationTime = 0.8f;
	float sleepingThreshold1 = 1.6f;
	float sleepingThreshold2 = 2.5f;
	float linearDamping = 0.05f;
	float angularDamping = 0.85f;
	float friction = 0.25f;
	
	public PhysicalParametersInspector(RigidBody[] bs) {
		bodies = bs;
		activationTime = bodies[0].getDeactivationTime();
		friction = bodies[0].getFriction();
	}
	
	public void setPhysicalParameters() {
		for (int k = 0; k<bodies.length; ++k)	{
			bodies[k].setDamping(linearDamping, angularDamping);
			bodies[k].setDeactivationTime(activationTime);
			bodies[k].setSleepingThresholds(sleepingThreshold1, sleepingThreshold2);
			bodies[k].setFriction(friction);
		}
		
	}
	
	public Component getInspector()	{
		Box container = Box.createVerticalBox();
		container.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Physical parameters"));
		Box hbox = Box.createHorizontalBox();
		// swing gui has unpredictable sizing properties! I'm punting here ...
		hbox.setPreferredSize(new Dimension(300, 40));
		hbox.setMaximumSize(new Dimension(300, 40));
		final TextSlider.Double linDampSlider = new TextSlider.Double(
				"linear damping", SwingConstants.HORIZONTAL, 0.0, 2, linearDamping);
		linDampSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				linearDamping = (float) (linDampSlider.getValue().doubleValue());
				setPhysicalParameters();
			}
		});
		container.add(linDampSlider);
		final TextSlider.Double angDampSlider = new TextSlider.Double(
				"angular damping", SwingConstants.HORIZONTAL, 0.0, 2,
				angularDamping);
		angDampSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				linearDamping = (float) (angDampSlider.getValue().doubleValue());
				setPhysicalParameters();
			}
		});
		container.add(angDampSlider);
		final TextSlider.Double deactSlider = new TextSlider.Double(
				"deactivation time", SwingConstants.HORIZONTAL, 0.0, 2,
				angularDamping);
		deactSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				activationTime = (float) (deactSlider.getValue().doubleValue());
				setPhysicalParameters();
			}
		});
		container.add(deactSlider);
		final TextSlider.Double st1Slider = new TextSlider.Double(
				"sleep threshold 1", SwingConstants.HORIZONTAL, 0.0, 2,
				sleepingThreshold1);
		st1Slider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sleepingThreshold1 = (float) (st1Slider.getValue().doubleValue());
				setPhysicalParameters();
			}
		});
		container.add(st1Slider);
		final TextSlider.Double st2Slider = new TextSlider.Double(
				"sleep threshold 2", SwingConstants.HORIZONTAL, 0.0, 2,
				sleepingThreshold2);
		st2Slider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sleepingThreshold2 = (float) (st2Slider.getValue().doubleValue());
				setPhysicalParameters();
			}
		});
		container.add(st2Slider);
		final TextSlider.Double frictionSlider = new TextSlider.Double(
				"friction", SwingConstants.HORIZONTAL, 0.0, 1,
				friction);
		frictionSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				friction = (float) (frictionSlider.getValue().doubleValue());
				setPhysicalParameters();
			}
		});
		container.add(frictionSlider);
		return container;
	}
}
