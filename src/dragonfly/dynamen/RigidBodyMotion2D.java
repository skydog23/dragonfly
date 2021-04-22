/*
 * Created on Mar 6, 2008
 *
 */
package dragonfly.dynamen;

import static de.jreality.shader.CommonAttributes.*;
import static de.jreality.shader.CommonAttributes.EDGE_DRAW;
import static de.jreality.shader.CommonAttributes.POLYGON_SHADER;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import charlesgunn.gui.TextSlider;
import charlesgunn.jreality.examples.jogl.LoadableScene;
import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.jreality.texture.SimpleTextureFactory.TextureType;
import charlesgunn.util.MyMidiSynth;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P2;
import de.jreality.math.P3;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Cylinder;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;
import de.jreality.shader.Texture2D;
import de.jreality.tools.ActionTool;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.Box;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import de.jreality.util.PickUtility;
import de.jreality.util.SceneGraphUtility;

public class RigidBodyMotion2D extends LoadableScene {

	SceneGraphComponent 
		world, 
			tableSGC,
				gunSGC,
					puckSGC, 	// euclidean motion applied here
						puckLocalSGC,	// the geometry of the puck
							puckItself,
					turretSGC, 
					moveCylSGC, 
						cylSGC,
							bulletSGC,
						bulletMassSGC;
	boolean shooting = false;
	boolean doSound = true;
	double bulletMass = .01;
	double[] initialMomentum = {0,.5,.5};
	double[] bulletLine = {0,-1,0, 0}, p2gM = new double[16], g2bM = new double[16], b2pM = new double[16];
	SceneGraphPath gun2puck, gun2bullet;
	RigidBodyODEE2 rbode = new RigidBodyODEE2();
	MyMidiSynth midi = new MyMidiSynth();
	int[] patterns = {0xf0f, 0x1e1e, 0x3c3c, 0x7878};
	@Override
	public SceneGraphComponent makeWorld() {
		setDoSound(doSound);
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		tableSGC = SceneGraphUtility.createFullSceneGraphComponent("table");
		puckSGC = SceneGraphUtility.createFullSceneGraphComponent("puck");
		puckLocalSGC = rbode.getSceneGraphRepresentation(); //SceneGraphUtility.createFullSceneGraphComponent("puck local");
		puckItself = puck(); //SceneGraphUtility.createFullSceneGraphComponent("puck itself");
		gunSGC = SceneGraphUtility.createFullSceneGraphComponent("gun");
		turretSGC = SceneGraphUtility.createFullSceneGraphComponent("turret");
		moveCylSGC = SceneGraphUtility.createFullSceneGraphComponent("move cyl");
		cylSGC = SceneGraphUtility.createFullSceneGraphComponent("cyl");
		bulletSGC = SceneGraphUtility.createFullSceneGraphComponent("bullets");
		bulletMassSGC = SceneGraphUtility.createFullSceneGraphComponent("bullet mass");
		Appearance ap = world.getAppearance();
		ap.setAttribute(EDGE_DRAW, false);
		ap = tableSGC.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.green);
		puckItself.setAppearance(new Appearance());
		ap = puckItself.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white);
		Texture2D tex2d = (Texture2D) AttributeEntityUtility
	       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", ap, true);		
		SimpleTextureFactory stf = new SimpleTextureFactory();
		stf.setType(TextureType.CHECKERBOARD);
		stf.setSize(16);
		stf.setColor(0, Color.red);
		stf.setColor(1, Color.blue);
		stf.update();
		tex2d.setImage(stf.getImageData());
		Matrix foo = new Matrix();
		MatrixBuilder.euclidean().scale(8,8,1).assignTo(foo);
		tex2d.setTextureMatrix(foo);
		rbode.setObject(puckItself);
		
		ap = turretSGC.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.white);
		tex2d = (Texture2D) AttributeEntityUtility
	       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", ap, true);		
		stf = new SimpleTextureFactory();
		stf.setType(TextureType.CHECKERBOARD);
		stf.setSize(16);
		stf.setColor(0, Color.black);
		stf.setColor(1, Color.white);
		stf.update();
		tex2d.setImage(stf.getImageData());
		foo = new Matrix();
		MatrixBuilder.euclidean().scale(1,16,1).assignTo(foo);
		tex2d.setTextureMatrix(foo);
		MatrixBuilder.euclidean().scale(.5, .5, 1).assignTo(turretSGC);
		MatrixBuilder.euclidean().translate(-2,0,0).assignTo(puckSGC);
		turretSGC.addChild(puck());
		
		rbode.setMomentum(initialMomentum.clone());
		rbode.setRunMotion(true);
		cylSGC.setGeometry(new Cylinder());
		MatrixBuilder.euclidean().rotateZ(Math.PI/2).translate(0,.5,0).rotateX(-Math.PI/2).scale(.03,.03,.5).assignTo(cylSGC);
		PickUtility.setPickable(cylSGC, false);
		ap = cylSGC.getAppearance();
		ap.setAttribute(POLYGON_SHADER+"."+DIFFUSE_COLOR, Color.yellow);
		
		IndexedLineSet bullets = IndexedLineSetUtility.createCurveFromPoints(new double[][]{{0,0,0},{0,0,10}}, false);
		bulletSGC.setGeometry(bullets);
		bulletSGC.setVisible(false);
		ap = bulletSGC.getAppearance();
		ap.setAttribute(EDGE_DRAW, true);
		ap.setAttribute(LINE_SHADER+"."+DIFFUSE_COLOR, Color.white);
		ap.setAttribute(LINE_SHADER+"."+LINE_STIPPLE, true);
		ap.setAttribute(LINE_SHADER+"."+LINE_FACTOR, 4);
		ap.setAttribute(LINE_SHADER+"."+LINE_STIPPLE_PATTERN, 0x5);
		ap.setAttribute(LINE_SHADER+"."+LINE_WIDTH, 2.0);
		ap.setAttribute(LINE_SHADER+"."+TUBES_DRAW, false);

		IndexedFaceSet bmslider = Primitives.texturedQuadrilateral(null);
		bulletMassSGC.setGeometry(bmslider);
		MatrixBuilder.euclidean().translate(-.5,0,puckThickness/2).scale(1,1,.15).rotateX(Math.PI/2).assignTo(bulletMassSGC);
	
		world.addChild(tableSGC);
		tableSGC.addChildren(gunSGC);
		gunSGC.addChildren(puckSGC, turretSGC, moveCylSGC);
		puckSGC.addChild(puckLocalSGC);
		gun2puck = new SceneGraphPath(puckSGC, puckLocalSGC);
		gun2bullet = new SceneGraphPath(moveCylSGC);
		moveCylSGC.addChildren(cylSGC, bulletMassSGC);
		cylSGC.addChild(bulletSGC);
		MatrixBuilder.euclidean().translate(0, 0, 1).rotateX(Math.PI/2).translate(0,0,puckThickness/2).assignTo(tableSGC);
//		puckLocalSGC.setTransformation(rbode.getSceneGraphRepresentation().getTransformation());
		
		addTools();
		
		return world;
	}

	private void reset() {
		shooterTimer.stop();
		shooting = false;
		clip.loop(0);
		rbode.setMomentum(initialMomentum.clone());
		System.err.println("mom = "+Rn.toString(rbode.getMomentum()));
		rbode.resetMotion();
//		puckLocalSGC.getTransformation().setMatrix(Rn.identityMatrix(4));
	}

	private void addTools()	{
		bulletMassSGC.addTool(new AbstractTool(InputSlot.getDevice("AllDragActivation")) {

	    	Matrix m = new Matrix();
			{
	    		addCurrentSlot(InputSlot.getDevice("PointerTransformation"), "drags the texture");
	    	}
	    	
			public void activate(ToolContext tc) {
			}

			public void perform(ToolContext tc) {
				if (tc.getCurrentPick() == null) return;
				double[] texCoords = tc.getCurrentPick().getTextureCoordinates();
				if (texCoords == null) return; 	// can happen!
				System.err.println("tex coords is "+Rn.toString(texCoords));
				bulletMass = .01 *( 1-2*texCoords[0] );
			}

			public void deactivate(ToolContext tc) {
			}

			public String getDescription(InputSlot slot) {
				return null;
			}

			public String getDescription() {
				return null;
			}
	    	
	    });
		turretSGC.addTool( new AbstractTool(InputSlot.getDevice("AllDragActivation")) {

	    	private double[] origTexCoords;
	    	Matrix m = new Matrix();
			{
	    		addCurrentSlot(InputSlot.getDevice("PointerTransformation"), "drags the texture");
	    	}
	    	
			public void activate(ToolContext tc) {
				origTexCoords = tc.getCurrentPick().getTextureCoordinates();
				m.assignFrom(moveCylSGC.getTransformation());
			}

			public void perform(ToolContext tc) {
				if (tc.getCurrentPick() == null) return;
				double[] texCoords = tc.getCurrentPick().getTextureCoordinates();
				if (texCoords == null) return; 	// can happen!
//				System.err.println("tex coords is "+Rn.toString(texCoords));
				if (texCoords[0] > .5)	{	// dragging on top, shoot!
					bulletSGC.setVisible(true);
					if (!shooting) {
						shooterTimer.start();
						shooting = true;
						rbode.setRunMotion(true);
						clip.loop(Clip.LOOP_CONTINUOUSLY);
						clip.start();
					}
				} else {
					if (shooting) {
						shooterTimer.stop();
						shooting = false;
						rbode.setRunMotion(false);
						clip.loop(0);
//						clip.stop();
					}
					bulletSGC.setVisible(false);
					double angle = (texCoords[1]-origTexCoords[1]) * Math.PI*2;
					double[] mm = Rn.times(null, m.getArray(), P3.makeRotationMatrixZ(null, angle));
					moveCylSGC.getTransformation().setMatrix(mm);					
				}
			}

			public void deactivate(ToolContext tc) {
//				bulletSGC.setVisible(false);
				shooterTimer.stop();
				rbode.setRunMotion(false);
				shooting = false;
				clip.loop(0);
			}

			public String getDescription(InputSlot slot) {
				return null;
			}

			public String getDescription() {
				return null;
			}
	    	
	    });

	}
	@Override
	public boolean isEncompass() {
		return true;
	}
	double puckThickness = .35,
		rounding = .05;
	private Timer shooterTimer;
	private SceneGraphComponent puck()	{
		IndexedFaceSet puck;
		double[][] verts = {
				{0,.0001,0}, 
				{0,1,0},
				{puckThickness-rounding, 1,0},
				{puckThickness-(1-Math.sqrt(.5))*rounding, 1-(1-Math.sqrt(.5))*rounding,0},
				{puckThickness, 1-rounding, 0},
				{puckThickness,0.01,0}};
		puck = GeometryUtilityOverflow.surfaceOfRevolutionAsIFS(verts, 50, Math.PI*2);
		IndexedFaceSetUtility.calculateAndSetEdgesFromFaces(puck);
		SceneGraphComponent holder = new SceneGraphComponent("puck");
		holder.setGeometry(puck);
		MatrixBuilder.euclidean().rotateY(-Math.PI/2).translate(-puckThickness/2,0,0).assignTo(holder);
		return holder;
	}
	
	int[] indices = {8,9,10,11,12,13, 14, 15};
	Clip clip;
	private void setDoSound(boolean doSound) {
		InputStream is = this.getClass().getResourceAsStream("zipp.wav");
        AudioInputStream audioInputStream = null;
		try {
			audioInputStream = AudioSystem.getAudioInputStream(is);
		} catch (UnsupportedAudioFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
 		AudioFormat format = audioInputStream.getFormat();
		DataLine.Info info = new DataLine.Info(Clip.class, format); // format is an AudioFormat object
		if (!AudioSystem.isLineSupported(info)) {
		    // Handle the error.
		    }
		    // Obtain and open the line.
		try {
		    clip = (Clip) AudioSystem.getLine(info);
		    clip.open(audioInputStream);
		    System.err.println("Got clip");
		} catch (LineUnavailableException ex) {
			ex.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (doSound)	{
			midi.open();
			for (int j = 0; j<indices.length; ++j)	{
				midi.getSynthesizer().loadInstrument(midi.getInstruments()[indices[j]]);
				midi.getChannels()[j].getChannel().programChange(indices[j]);			
			}
		} else midi.close();
	}

	Viewer viewer;
	public void customize(final Viewer v) {
		viewer = v;
		puckLocalSGC.getTransformation().addTransformationListener(new TransformationListener() {

			public void transformationMatrixChanged(TransformationEvent ev) {
				viewer.renderAsync();
			}
			
		});

		shooterTimer = new Timer(20, new ActionListener() {
			int count = 0;
			public void actionPerformed(ActionEvent arg0) {
				bulletSGC.getAppearance().setAttribute(LINE_SHADER+"."+LINE_STIPPLE_PATTERN, patterns[0]<<count);

				updateDynamics();

				count = (count+1)%8;				
				if (doSound && count == 0)  {
//					midi.getChannels()[count].getChannel().noteOn((int) (24+64), 
//							midi.getChannels()[count].getVelocity());
				}
			}
			
		});
		ActionTool actionTool = new ActionTool(InputSlot.getDevice("PanelActivation"));
		turretSGC.addTool( actionTool);
		actionTool.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				reset();
				viewer.renderAsync();
			}
			
		});
		
	}

	public void insertTabs(JTabbedPane tabs) {
		JPanel panel = new JPanel();
		tabs.addTab("parameters", panel);
		Box box = Box.createVerticalBox();
		panel.add(box);
		final TextSlider massSlider = new TextSlider.Double("object mass",SwingConstants.HORIZONTAL,.1, 5,rbode.getMass());
		massSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				double mass =massSlider.getValue().doubleValue();
				rbode.setMass(mass);
				rbode.update();
				viewer.renderAsync();
			}
		});
		box.add(massSlider);
		final TextSlider momentSlider = new TextSlider.Double("moment of inertia",SwingConstants.HORIZONTAL,0,5,rbode.getMoment());
		momentSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				double moment =massSlider.getValue().doubleValue();
				rbode.setMoment(moment);
				rbode.update();
			}
		});
		box.add(momentSlider);
		final TextSlider bulletMassSlider = new TextSlider.Double("bullet mass",SwingConstants.HORIZONTAL,-1, 1, 1);
		bulletMassSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				bulletMass = .01*bulletMassSlider.getValue().doubleValue();
			}
		});
		box.add(bulletMassSlider);
	}


	private void updateDynamics() {
		gun2puck.getMatrix(p2gM);
		gun2bullet.getInverseMatrix(g2bM);
		double[] mm = Rn.transpose(null, Rn.times(b2pM, g2bM, p2gM));
//		System.err.println("mm = "+Rn.matrixToString(mm));
		double[] bl = Rn.matrixTimesVector(null, mm, bulletLine); //bulletLine; //
		double[] bl3 = {bl[0], bl[1], bl[3]};
//		System.err.println("bullet line = "+Rn.toString(bl3));
		rbode.setMomentum(Rn.add(null, rbode.getMomentum(), Rn.times(null, bulletMass, bl3)));
	}

}
