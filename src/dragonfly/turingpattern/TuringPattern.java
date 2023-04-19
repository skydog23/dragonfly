package dragonfly.turingpattern;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.Timer;

import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.plugin.JRViewer;
import de.jreality.plugin.basic.Shell;
import de.jreality.plugin.basic.ViewPreferences;
import de.jreality.plugin.experimental.ViewerKeyListenerPlugin;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.scene.pick.Graphics3D;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultPolygonShader;
import de.jreality.shader.ImageData;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import de.jreality.util.CameraUtility;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.core.DiscreteGroupSimpleConstraint;
import de.jtem.discretegroup.core.DiscreteGroupViewportConstraint;
import de.jtem.discretegroup.groups.WallpaperGroup;
import de.jtem.discretegroup.plugin.TessellatedContent;
import de.jtem.jrworkspace.plugin.Controller;
import de.jtem.jrworkspace.plugin.Plugin;

/**
 * This example shows an animated 2d texture which maps a <i>live</i> game of life onto a quadrilateral.
 * @author Charles Gunn
 *
 */
public class TuringPattern extends Assignment  {
	static int count = 0;
	transient Viewer viewer;
	transient PluginSceneLoader psl;
	transient private McCabe  mccabe = new McCabe();
	transient private Timer timer;
	boolean running = false;
	transient private BufferedImage bi;
	transient private Graphics2D tex2dGC;
	transient AnimationPlugin ap;
	transient Runnable texRun;
	private Texture2D tex2d;
	private SceneGraphComponent worldSGC,
		turing = SceneGraphUtility.createFullSceneGraphComponent("turing");
	DiscreteGroupViewportConstraint viewportConstraint = new DiscreteGroupViewportConstraint(3.0,2,30.0,30, null);
	DiscreteGroupSimpleConstraint masterConstraint  = 
			new DiscreteGroupSimpleConstraint(30.0, 30, 10000);
//	private DiscreteGroupSceneGraphRepresentation dgsgr;
	TessellatedContent tessellatedContent = new TessellatedContent();
	boolean saveParameters = true;
	FileOutputStream parameters;
	private OutputStreamWriter parameterWriter;
	
	@Override
	public void setupJRViewer(JRViewer v) {
		// TODO Auto-generated method stub
		tessellatedContent.setupJRViewer(v);
		super.setupJRViewer(v);

	}

	@Override
	public List<Plugin> getPluginsToRegister() {
		// TODO Auto-generated method stub
		pluginsToLoad.add(new Shell());
//		pluginsToLoad.add(contentPlugin);
//		pluginsToLoad.add(new ContentTools());
//		pluginsToLoad.add(new ContentLoader());
		pluginsToLoad.add(new ViewPreferences());
		animationPlugin = new AnimationPlugin();
		pluginsToLoad.add(animationPlugin);
		pluginsToLoad.add(new ViewerKeyListenerPlugin());
		pluginsToLoad.add(shrinkPanelPlugin);
		pluginsToLoad.add(tessellatedContent);
		return pluginsToLoad;
	}
	@Override
	public void startAnimation() {
		// TODO Auto-generated method stub
		super.startAnimation();
		if (saveParameters)	{
			java.util.Date todaysDate = new java.util.Date();
			DateFormat df2 = new SimpleDateFormat("yyyy.MM.dd:HH:mm:ss");
			String testDateString = df2.format(todaysDate);
			String pixdir = ap.getAnimationPanel().getRecordPrefs().getCurrentDirectoryPath();
			df2 = new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss");
			testDateString = df2.format(todaysDate);
			String parameterWriteFile = pixdir+"/"+testDateString+".txt";
			System.err.println("File name = "+parameterWriteFile);
			File file = new File(parameterWriteFile);
			try {
				parameters = new FileOutputStream(file);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				parameterWriter = new OutputStreamWriter(parameters, "UTF-8");
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				parameterWriter.write("# Turing pattern parameters "+testDateString+"\n");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.err.println("Starting animation");
	}

	@Override
	public void endAnimation() {
		// TODO Auto-generated method stub
		super.endAnimation();
		try {
			parameterWriter.flush();
			parameterWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		timer.stop();
		System.err.println("Ending animation");
	}

	
	@Override
	public void setValueAtTime(double d) {
		// TODO Auto-generated method stub
		super.setValueAtTime(d);
		texRun.run();
		TuringPattern.this.viewer.renderAsync();		
		if (TuringPattern.this.saveParameters && 
				ap.getAnimationPanel().isRecording() &&
				parameterWriter != null) {
			String value = mccabe.getWeightsInterpolator().toString();
			try {
				parameterWriter.write(value+"\n");
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	@Override
	public SceneGraphComponent getContent() {
		worldSGC = new SceneGraphComponent("world");
		worldSGC.setGeometry(Primitives.texturedQuadrilateral());
		Appearance ap = new Appearance();
		worldSGC.setAppearance(ap);
		DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(ap, true);
		dgs.setShowLines(false);
		dgs.setShowPoints(false);
		DefaultPolygonShader dps = (DefaultPolygonShader) dgs.createPolygonShader("default");
		dps.setDiffuseColor(Color.white);
		ap.setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		
		tex2d = (Texture2D) AttributeEntityUtility
	       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", ap, true);
		final int width = 64, height = width;
		bi = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
		final ImageData id = new ImageData(bi);
		tex2d.setImage(id);
		// shouldn't have to do this; but if I comment it out, updating the texture doesn't work
		bi = (BufferedImage) id.getImage();
//		tex2d.setRepeatS(Texture2D.GL_MIRRORED_REPEAT);
//		tex2d.setRepeatT(Texture2D.GL_MIRRORED_REPEAT);
		tex2d.setRepeatS(Texture2D.GL_CLAMP); //GL_REPEAT); //
		tex2d.setRepeatT(Texture2D.GL_CLAMP);//REPEAT); //
		tex2d.setMagFilter(Texture2D.GL_NEAREST); //LINEAR_MIPMAP_LINEAR);
		tex2d.setMinFilter(Texture2D.GL_NEAREST); //LINEAR_MIPMAP_LINEAR);
 		tex2d.setAnimated(true);
 		tex2d.setMipmapMode(true);
 		Matrix foo = new Matrix();
 		MatrixBuilder.euclidean().translate(.5,.5,0).rotateZ(Math.PI/2).translate(-.5, -.5, 0).assignTo(foo);
 		tex2dGC = bi.createGraphics();

		texRun = new Runnable() {
 		
			public void run() {
//				System.err.println("in turing pattern texture runnable" + count++);
					mccabe.update();
					updateImage(tex2d);
			}
 			
 		};
		
		tessellatedContent.setFollowsCamera(false);
		tessellatedContent.setClipToCamera(true);
		replaceGroup("22X");
		return turing;
	}
	
	private void replaceGroup(
			String gname) {
		WallpaperGroup group = WallpaperGroup.instanceOfGroup(gname);//"22X");//"244");//"O"); //"*2222");//
		worldSGC.setGeometry(group.getDefaultFundamentalRegion());
		mccabe.setGroup(group);
		tessellatedContent.setGroup(group, false);
		IndexedFaceSet ifs = (IndexedFaceSet) group.getDefaultFundamentalRegion();
		double[][] pts = ifs.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		tessellatedContent.getViewportConstraint().setPoints(pts);
		tessellatedContent.setContent(worldSGC);
		tessellatedContent.getTheRepn().update();
	}
	@Override
	public void display() {
		// con't call super() since the content is handled differently here
		setupJRViewer(jrviewer);
		jrviewer.startup();
		getContent();
		CameraUtility.getCamera(jrviewer.getViewer()).setFieldOfView(60);
		tessellatedContent.setContent(worldSGC);
		viewportConstraint.setGraphicsContext(new Graphics3D(jrviewer.getViewer()));
		viewportConstraint.setZtlate(.5);
		viewportConstraint.setFudge(1.2);
		tessellatedContent.setViewportConstraint(viewportConstraint);
		tessellatedContent.setMasterConstraint(masterConstraint);
		// comment out the following to get transparent black background
		Appearance app = jrviewer.getViewer().getSceneRoot().getAppearance();
		app.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		app.setAttribute("backgroundColors", Appearance.INHERITED);
		app.setAttribute("backgroundColor", new Color(0,0,0,0));
		this.viewer = jrviewer.getViewer();
		MatrixBuilder.euclidean().translate(0,0,6).assignTo(
				CameraUtility.getCameraNode(viewer));
		
			Controller c = jrviewer.getController();
			ap = c.getPlugin(AnimationPlugin.class);
			ap.getAnimationPanel().setResourceDir("src/dragonfly/turingPattern");
//			ap.getAnimationPanel().read("src/dragonfly/turingpattern/turingPattern-anim-01.xml");

//			ap.getAnimationPanel().addAnimationPanelListener(new AnimationPanelListener() {
//				
//				public void setState(Object o) {
//					
//				}
//				
//				public void printState() {
//					
//				}
//				
//				public Object getState() {
//					return null;
//				}
//				
//				public String getName() {
//					return "TuringPattern";
//				}
//				
//				public void actionPerformed(AnimationPanelEvent e) {
//					System.err.println("Got animation event "+e.type);
//					switch(e.type) {
//					case PLAYBACK_STARTED:
//						timer.stop();
//						break;
//					case SET_VALUE_AT_TIME:
//						texRun.run();
//						TuringPattern.this.viewer.renderAsync();		
//						if (TuringPattern.this.saveParameters && 
//								e.source.isRecording() &&
//								parameterWriter != null) {
//							String value = mccabe.getWeightsInterpolator().toString();
//							try {
//								parameterWriter.write(value+"\n");
//							} catch (IOException e1) {
//								// TODO Auto-generated catch block
//								e1.printStackTrace();
//							}
//						}
//						break;
//					default:
//						break;
//					}
//				}
//			});
 		timer = new Timer(20, new ActionListener()	{

			public void actionPerformed(ActionEvent e) {
				texRun.run();
				TuringPattern.this.viewer.renderAsync();		
			}
 			
 		});
		timer.start();
		
		((Component) viewer.getViewingComponent()).addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
					switch(e.getKeyCode())	{
				
				case KeyEvent.VK_H:
					System.err.println("	1: reset pattern");
					System.out.println("	2: do blur");
					System.out.println("	3: toggle run");
					break;
	
				case KeyEvent.VK_1:
					timer.stop();
					mccabe.resetBoard();
					timer.start();
					break;
				case KeyEvent.VK_2:
					mccabe.setDoBlur();
					break;
				case KeyEvent.VK_3:
					if (timer.isRunning()) timer.stop();
					else timer.start();
					break;
				}
			}
			
		});
	}
	
	
	@Override
	public File getPropertyFile() {
		// TODO Auto-generated method stub
		return super.getPropertyFile();
	}
	@Override
	public Component getInspector() {
		super.getInspector();
		Box container = Box.createVerticalBox();

		Box vbox = Box.createHorizontalBox();
		container.add(vbox);
		final JButton runb = new JButton("pause");
		runb.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				if (timer.isRunning()) {
					timer.stop();
					runb.setText("run");
				}
				else {
					timer.start();
					runb.setText("pause");
				}
			}
		});
		vbox.add(runb);

		JButton oneStep = new JButton("step");
		oneStep.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				int oldsteps = mccabe.stepsPerTick;
				mccabe.stepsPerTick = 1;
				mccabe.update();
				mccabe.stepsPerTick = oldsteps;
				updateImage(tex2d);
			}
		});
		vbox.add(oneStep);
		JButton resetb = new JButton("reset");
		resetb.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				mccabe.resetBoard();
				mccabe.update();
				updateImage(tex2d);
			}
		});
		vbox.add(resetb);

		// list all the groups in the menu
		JMenuBar groupM = new JMenuBar();
		JMenu gm = new JMenu("Group");
		groupM.add(gm);
		ButtonGroup bg = new ButtonGroup();
		boolean working[] = {true, true, false,true,false,true,true,true,true,true,true,true,false, false,true,true,false,false,false};
		final String[] gnames = WallpaperGroup.names;
		for (int i = 0; i<gnames.length; ++i)	{
			if (!working[i]) continue;
			final int j = i;
			JMenuItem jm = gm.add(new JRadioButtonMenuItem(gnames[i]));
			jm.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e)	{
					replaceGroup(gnames[j]);
				}
			});
			bg.add(jm);
		}
//		theMenuBar.add(testM);
		vbox.add(groupM);

		container.add(mccabe.getInspector());
		return container;
	}
	private void updateImage(final Texture2D tex2d) {
		Image current = mccabe.currentValue();
		if (bi.getWidth() != mccabe.getSize()) {
			ImageData id = new ImageData(current);
			tex2d.setImage(id);
			bi = (BufferedImage) id.getImage();
			tex2dGC = bi.createGraphics();
		}
		tex2dGC.drawImage(current, 0, 0, null);
		jrviewer.getViewer().renderAsync();
	}
	public static void main(String[] args) {
		new TuringPattern().display();
	}
}
