/*
 * Created on Mar 3, 2008
 *
 */
package dragonfly.turingpattern;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import charlesgunn.util.TextSlider;
import de.jreality.math.Rn;
import de.jtem.jrworkspace.plugin.sidecontainer.widget.ShrinkPanel;
import discreteGroup.wallpaper.AbstractColorProvider;

public class PeriodicSource  {
	{
		int clahpahboling = (int) (System.currentTimeMillis()%23);
		for (int i = 0; i<clahpahboling; ++i) Math.random();
	}
	public transient ShrinkPanel shrinkPanel;

	private double[] value = new double[4];
	private double num = 0;
	
	public double[] getValue() {
		return value;
	}

	public void setValue(double[] value) {
		this.value = value;
		num = value.length;
	}
	
	double foo = Math.random();
	double c1 = 1.27, c2 = 1.0, c3 = .786, 
			c4 = 0.0,
			c5 = Math.random(),
			c6 = Math.random();
	
	public double getC1() {
		return c1;
	}

	public void setC1(double c1) {
		this.c1 = c1;
	}

	public double getC2() {
		return c2;
	}

	public void setC2(double c2) {
		this.c2 = c2;
	}

	public double getC3() {
		return c3;
	}

	public void setC3(double c3) {
		this.c3 = c3;
	}

	public double getC4() {
		return c4;
	}

	public void setC4(double c4) {
		this.c4 = c4;
	}
	double dt = .005;
	double t = 0;
	double saturated = 1.0;
	public void update()	{
		t += dt;
		double isaturated = 1.0 - saturated;
		// generate numbers between 0 and 1
		value[0] = .5 + .5*Math.sin(c1*t+c4);
		value[1] = .5 + .5*Math.sin(c2*t+c5);
		value[2] = .5 + .5*Math.sin(c3*t+c6);
		double mid;
		if (value[0] > value[1])
			if (value[1] > value[2]) mid = value[1];  // 012
			else {
				if (value[0] > value[2]) mid = value[2]; // 021
				else mid = value[0]; // 201
			}
		 
		else {
			if (value[1] < value[2]) mid = value[1];   // 210
			else {
				if (value[0] < value[2]) mid = value[2];  // 120
				else mid = value[0];   // 102
			}
		}
		double factor = 1.0/mid;
//		for (int i = 0; i<3; ++i) value[i] *= factor;
	}
	
	public void setSpeed(double s)	{
		dt = s;
	}
	
	public double getSpeed()	{
		return dt;
	}

	public double getSaturated() {
		return saturated;
	}

	public void setSaturated(double saturated) {
		this.saturated = saturated;
	}
	transient JPanel panel;

	public JComponent getInspector() {
		if (panel != null) return shrinkPanel;
		shrinkPanel = new ShrinkPanel(this.getClass().getName());
		
		shrinkPanel.removeAll();
		shrinkPanel.setLayout(new ShrinkPanel.MinSizeGridBagLayout());
		 panel = new JPanel();
//		c.anchor = GridBagConstraints.PAGE_START;
			TextSlider c1slider = new TextSlider.Double("red speed",SwingConstants.HORIZONTAL, -3, 3, c1);
			c1slider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					c1 = ((TextSlider) e.getSource()).getValue().doubleValue()-.01;
				}
			});
			TextSlider c2slider = new TextSlider.Double("green speed",SwingConstants.HORIZONTAL, -3, 3, c2);
			c2slider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					c2 = ((TextSlider) e.getSource()).getValue().doubleValue()-.01;
				}
			});
			TextSlider c3slider = new TextSlider.Double("blue speed",SwingConstants.HORIZONTAL, -3, 3, c3);
			c3slider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					c3 = ((TextSlider) e.getSource()).getValue().doubleValue()-.01;
				}
			});
		TextSlider cslider = new TextSlider.Double("global speed",SwingConstants.HORIZONTAL,0.0,.1,dt);
		cslider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				dt = ((TextSlider) e.getSource()).getValue().doubleValue();
			}
		});
//		box.add(cslider);
		TextSlider sslider = new TextSlider.Double("saturation",SwingConstants.HORIZONTAL,0.0,1,saturated);
		sslider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				saturated = ((TextSlider) e.getSource()).getValue().doubleValue();
			}
		});
//		box.add(sslider, c);
//		c.anchor = GridBagConstraints.PAGE_START;
//		c.fill = GridBagConstraints.NONE;
		Insets insets = new Insets(1,0,1,0);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.insets = insets;
		c.weighty = 0.0;
		c.anchor = GridBagConstraints.WEST;
		panel.setLayout(new GridBagLayout());

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		panel.add(c1slider, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		panel.add(c2slider, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		panel.add(c3slider, c);

		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		panel.add(cslider, c);
		
		c.gridwidth = GridBagConstraints.REMAINDER;
		c.weightx = 1.0;
		panel.add(sslider, c);
		
		shrinkPanel.add(panel, c);
		return shrinkPanel;
	}

	@Override
	public String toString() {
		return Rn.toString(value);

	}
	
}
