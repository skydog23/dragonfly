package dragonfly.dynamen.attic;

import static de.jreality.shader.CommonAttributes.LINE_SHADER;
import static de.jreality.shader.CommonAttributes.METRIC;
import static de.jreality.shader.CommonAttributes.TUBES_DRAW;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.util.TextSlider;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.util.SceneGraphUtility;
import dragonfly.dynamen.ClotheIt;
import dragonfly.dynamen.CompactClotheIt;
import dragonfly.dynamen.FlatClotheIt;


public class TestRigidBodyPlane extends LoadableScene {
	private SceneGraphComponent 
		world, 
			flatMover, 
			compactMover;
	IndexedLineSet avb = null, amb = null;
	double momentOfForce = 2;
	double[] initialMomentum = {0,1,momentOfForce};		// a line close to the origin
	double[] boxDims = {1,2,3};
	int metric = Pn.EUCLIDEAN;
	boolean compactMode = false;
	RigidBodyODE2DOld rbode;
	ClotheIt flatRepn, compactRepn;
	int count = 0;
	Viewer viewer;
	double initialDeltaT;
	TextSlider[] dimSliders;
	JButton runB;
	@Override
	public SceneGraphComponent makeWorld()	{
		
		rbode = new RigidBodyODE2DOld(metric);
		rbode.setMomentum(initialMomentum);
		double[] inertiaMoments = momentsFromDimn(boxDims);
		rbode.setMoments(inertiaMoments);
		rbode.setTimeStep(initialDeltaT = rbode.getTimeStep());
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		world.getAppearance().setAttribute(LINE_SHADER+"."+TUBES_DRAW, true);
		world.getAppearance().setAttribute(METRIC, Pn.HYPERBOLIC);
		MatrixBuilder.euclidean().translate(0,0,-3).assignTo(world);

		flatRepn = new FlatClotheIt(rbode);
		world.addChild(flatMover = flatRepn.fullSceneGraph());
		flatMover.setVisible(!compactMode);
		compactRepn = new CompactClotheIt(rbode);
		world.addChild(compactMover = compactRepn.fullSceneGraph());
		compactMover.setVisible(compactMode);
		
		rbode.addListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
					viewer.renderAsync();
					flatRepn.updateSceneGraphRepn();
					compactRepn.updateSceneGraphRepn();
					System.err.println("updating");
			}			
		});

		return world;
	}

	
	private static double[] momentsFromDimn(double[] dim)	{
		double[] moms = new double[3];
		double M = Rn.innerProduct(dim, dim);
		for (int i = 0; i<3; ++i) 
			moms[i] = M - dim[i]*dim[i];
		Rn.setToLength(moms, moms, 3);
		return moms;
	}

	private void reset()	{
		rbode.setRunMotion(false);
		rbode.resetMotion();
		double[] foo = {0,1,momentOfForce};
		Rn.normalize(foo, foo);
		rbode.setMomentum(foo);
		rbode.setMetric(metric);
		rbode.setMoments(momentsFromDimn(boxDims));
		flatRepn.reset();
		compactRepn.reset();
		count = 0;
		viewer.renderAsync();
	}
	@Override
	public void customize(JMenuBar menuBar, final Viewer viewer) {
		this.viewer = viewer;
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", new Color(20,20,40));
		((Component) viewer.getViewingComponent()).addKeyListener( new KeyAdapter()	{
			@Override
			public void keyPressed(KeyEvent e)	{ 
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_H:
					break;
	
				case KeyEvent.VK_1:
					rbode.setRunMotion(!rbode.isRunMotion());
					runB.setLabel(rbode.isRunMotion() ? "Pause" : "Run");
					break;
					
				case KeyEvent.VK_2:
					reset();
					break;
					
				case KeyEvent.VK_3:
					double mass = rbode.getMass();
					if (e.isShiftDown()) mass = mass/1.1;
					else mass = mass * 1.1;
					rbode.setMass(mass);
					flatRepn.updateSceneGraphRepn();
					viewer.renderAsync();
					break;
					
				case KeyEvent.VK_4:
					if (e.isShiftDown()) rbode.setTimeStep(rbode.getTimeStep()/1.1);
					else rbode.setTimeStep(rbode.getTimeStep()*1.1);
					break;
					
			}

			}
		});

	}
	@Override
	public boolean hasInspector() {return true; }
	@Override
	public Component getInspector(final Viewer viewer) {
		Box container = Box.createVerticalBox();
		Box hbox = Box.createHorizontalBox();
		JComboBox jcb = new JComboBox(new String[]{"Euclidean", "Hyperbolic", "Elliptic"});
	    hbox.setBorder(new EmptyBorder(5,10,5,10));
	    int height = (int)(jcb.getPreferredSize().getHeight());
	    hbox.setMaximumSize(new Dimension(1000, height));   
	    jcb.setMaximumSize(new Dimension(1000, height));   
	    jcb.setAlignmentX(Component.LEFT_ALIGNMENT);
		jcb.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				switch(((JComboBox)e.getSource()).getSelectedIndex()) {
				case 0:
					metric = Pn.EUCLIDEAN;
					break;
				case 1:
					metric = Pn.HYPERBOLIC;
					break;
				case 2:
					metric = Pn.ELLIPTIC;
					break;
				}
				compactRepn.setMetric(metric);
				flatRepn.setMetric(metric);
				reset();
			}
			
		});
		hbox.add(jcb);
		hbox.add(Box.createHorizontalStrut(5));
		final JCheckBox compactModeB = new JCheckBox("Compact view");
		hbox.add(compactModeB);
		compactModeB.setSelected(compactMode);
		compactModeB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				compactMode = compactModeB.isSelected();
				compactMover.setVisible(compactMode);
				flatMover.setVisible(!compactMode);
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		JButton resetB = new JButton("Reset");
		hbox.add(resetB);
		resetB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				runB.setLabel("Run");
				reset();
			}
			
		});
		hbox.add(Box.createHorizontalStrut(5));
		runB = new JButton("Run");
		hbox.add(runB);
		runB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				if (runB.getLabel() == "Run") {
					runB.setLabel("Pause");
					rbode.setRunMotion(true);
				} else {
					runB.setLabel("Run");
					rbode.setRunMotion(false);
				}
				
			}
			
		});
		hbox.add(Box.createHorizontalGlue());
		container.add(hbox);
		final TextSlider aSlider = new TextSlider.DoubleLog("moment of force",  SwingConstants.HORIZONTAL, .01, 1000,momentOfForce);
		aSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				momentOfForce = aSlider.getValue().doubleValue();
				double[] foo = {0,1,momentOfForce};
				Rn.normalize(foo, foo);
				reset();
				rbode.setMomentum(foo);
				flatRepn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		container.add(aSlider);
		final TextSlider bSlider = new TextSlider.Double("time step",  SwingConstants.HORIZONTAL, 0.0, 4, 1);
		bSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				rbode.setTimeStep(initialDeltaT * bSlider.getValue().doubleValue());
				viewer.renderAsync();
			}
		});
		container.add(bSlider);
		final TextSlider cSlider = new TextSlider.Double("mass",  SwingConstants.HORIZONTAL, 0.0, 4, rbode.getMass());
		cSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				rbode.setMass(cSlider.getValue().doubleValue());
				flatRepn.updateSceneGraphRepn();
				viewer.renderAsync();
			}
		});
		container.add(cSlider);
		dimSliders = new TextSlider[3];
		String[] labels = {"xDim","yDim","zDim"};
		for (int i = 0; i<3; ++i)	{
			dimSliders[i] = new TextSlider.Double(labels[i],  SwingConstants.HORIZONTAL, 0.0, 10, boxDims[i]);
			final int j = i;
			dimSliders[i].addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					boxDims[j] = (((TextSlider) e.getSource()).getValue().doubleValue());
					rbode.setMoments(momentsFromDimn(boxDims));
					flatRepn.updateSceneGraphRepn();
					viewer.renderAsync();
				}
			});
			container.add(dimSliders[i]);
		}
		container.setName("Parameters");
		container.add(flatRepn.getViewer2d());
		container.add(Box.createVerticalGlue());
		return container;
	}

}
