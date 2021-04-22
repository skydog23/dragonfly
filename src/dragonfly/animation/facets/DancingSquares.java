package dragonfly.animation.facets;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;

import charlesgunn.anim.core.Animated;
import charlesgunn.anim.core.KeyFrameAnimatedDelegate;
import charlesgunn.anim.core.KeyFrameAnimatedDouble;
import charlesgunn.anim.core.KeyFrameAnimatedInteger;
import charlesgunn.anim.core.KeyFrameAnimatedIsometry;
import charlesgunn.anim.core.TimeMapper;
import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.io.ImportExport;
import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.anim.util.AnimationUtility.InterpolationTypes;
import charlesgunn.jreality.newtools.TexturePlacementTool;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import charlesgunn.util.TextSlider;
import de.jreality.math.FactoredMatrix;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Scene;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.ImageData;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.SceneGraphUtility;
import discreteGroup.imulogo.IMULogo;

public class DancingSquares extends LoadableScene {

	int n = 5, m = 4;
	// get these values from IMULogo
	public double initialFOV = IMULogo.initialFOV*2, 
		initialFocus = IMULogo.initialFocus/2,
		eyeSeparationFactor = IMULogo.eyeSeparationFactor,
		eyeSeparation = initialFocus / eyeSeparationFactor;
	Dimension NTSCdim = new Dimension(1400, 1050),
		PALdim = new Dimension(1000,800); //720, 572);		// PAL
	Dimension dim = PALdim;
	protected double aspectRatio = dim.getWidth()/(dim.getHeight());
	
	private Texture2D frontTex, backTex, leftFrontTex, rightFrontTex;
	
	boolean matheonKrimi = false;
	
	String imagePath = matheonKrimi ? "/homes/geometer/gunn/Pictures/textures/matheonKrimi/" :
			"/homes/geometer/gunn/Pictures/textures/borromeanRings/";
	String[] krimiNames = {"Matheon_Stern-GCred.png",
			"Logo-Berliner-Krimimarathon-Negativ.jpg",
			"shutterstock_1_1250x1000-2.jpg",
			"shutterstock_2_1250x1000.jpg",
		"shutterstock_3_1250x1000.jpg",
		"shutterstock_4_1250x1000.jpg",
		"transpBlack-1250x1000.png",
		"transpBlack-1250x1000.png"};
	String[] borromeanNames = 	{"stereoPhotoBegin-02.tiff","" +
			"temple11.jpg",
			"temple08.jpg",
			"beer_mat2-black-5x4.png",
			"woodStool-black-5x4.png",
			"cremona-black-5x4.png",
			"begins04s.png"};
	String[] imageNames = matheonKrimi ? krimiNames : borromeanNames;
	double foo = aspectRatio; //matheonKrimi ? 3.0/2.0 : aspectRatio;
	double[] krimiARS =  {foo, foo, foo, foo, foo, foo, foo, foo},
		borromeanARS =  {foo, foo, foo, foo, foo, foo, foo, foo};
	double[] texAspectRatios = matheonKrimi ? krimiARS : borromeanARS ;
	int numTexs = imageNames.length;
	Appearance[] frontAps = new Appearance[numTexs],
		backAps = new Appearance[numTexs];
	Texture2D[] frontTexs = new Texture2D[numTexs],
		backTexs = new Texture2D[numTexs];
	Appearance currentAp;
	Texture2D currentTex;
	SubSequence[] ss = new SubSequence[numTexs];		// actually need one less!
	SceneGraphComponent[] sgcs = new SceneGraphComponent[numTexs];
	
	Appearance frontAp = new Appearance(), backAp = new Appearance();
	double time = 0.0;
	int step = 0, oldStep = -1;
	boolean cullBack = true, doStereoTex = false;
	private TextSlider timeSlider, stepSlider;
	private TexturePlacementTool textool;
	private SceneGraphComponent theWorld;
	private KeyFrameAnimatedInteger stepAnim;
	private double tmin;
	private double tmax, subTime;
	boolean isStereo = true;
	@Override
	public SceneGraphComponent makeWorld() {
		theWorld = SceneGraphUtility.createFullSceneGraphComponent("world");
		theWorld.getAppearance().setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		setupTextures();
		int sscnt = 0;
		rc = new RotatingCube(this);
		ss[sscnt] = rc;
		ss[sscnt].setAppearance0(frontAps[sscnt]);
		ss[sscnt].setAppearance1(backAps[sscnt+1]);
		theWorld.addChild(sgcs[sscnt] = ss[sscnt].getScene());
		sscnt++;
		
		exx = new ExplosionSS(this);
		ss[sscnt] = exx;
		ss[sscnt].setAppearance0(frontAps[sscnt]);
		ss[sscnt].setAppearance1(backAps[sscnt+1]);
		sgcs[sscnt] = ss[sscnt].getScene();
		sgcs[sscnt].setVisible(false);
		theWorld.addChild(sgcs[sscnt]);
		sscnt++;

		hss = new HelixSS(this);
		ss[sscnt] = hss;
		hss.setNumStrips(40);
		hss.setAppearance0(frontAps[sscnt]);
		hss.setAppearance1(backAps[sscnt+1]);
		sgcs[sscnt] = hss.getScene();
		sgcs[sscnt].setVisible(false);
		theWorld.addChild(sgcs[sscnt]);
		sscnt++;

		DiagonalFacetSS dfss = new DiagonalFacetSS(this);
		ss[sscnt] = dfss;
		dfss.setN(5);
		dfss.setM(4);
		dfss.setAppearance0(frontAps[sscnt]);
		dfss.setAppearance1(backAps[sscnt+1]);
		sgcs[sscnt] = dfss.getScene();
		sgcs[sscnt].setVisible(false);
		theWorld.addChild(sgcs[sscnt]);
		sscnt++;

		exx = new ExplosionSS(this);
		exx.setType(0);
		ss[sscnt] = exx;
		ss[sscnt].setAppearance0(frontAps[sscnt]);
		ss[sscnt].setAppearance1(backAps[sscnt+1]);
		sgcs[sscnt] = ss[sscnt].getScene();
		sgcs[sscnt].setVisible(false);
		theWorld.addChild(sgcs[sscnt]);
		sscnt++;


		
//		dfss = new DiagonalFacetSS(this);
//		ss[sscnt] = dfss;
//		dfss.setN(10);
//		dfss.setM(8);
//		XYFacetSS xyfss = new XYFacetSS(this);
//		xyfss.setN(5);
//		xyfss.setM(4);
//		ss[sscnt] = xyfss;
//		ss[sscnt].setAppearance0(frontAps[sscnt]);
//		ss[sscnt].setAppearance1(backAps[sscnt+1]);
//		sgcs[sscnt] = xyfss.getScene();
//		sgcs[sscnt].setVisible(false);
//		theWorld.addChild(sgcs[sscnt]);
//		sscnt++;
		

		OpeningDoorsSS od = new OpeningDoorsSS(this);
//		rc = new RotatingCube(this);
		ss[sscnt] = od;
		ss[sscnt].setAppearance0(frontAps[sscnt]);
		ss[sscnt].setAppearance1(backAps[sscnt+1]);
		sgcs[sscnt] = ss[sscnt].getScene();
		if (!matheonKrimi) sgcs[sscnt].getChildComponent(1).setVisible(false);
		sgcs[sscnt].setVisible(false);
		theWorld.addChild(sgcs[sscnt]);
		sscnt++;
		
//		exx = new ExplosionSS(this);
//		ss[sscnt] = exx;
//		ss[sscnt].setAppearance0(frontAps[sscnt]);
//		ss[sscnt].setAppearance1(backAps[sscnt+1]);
//		sgcs[sscnt] = ss[sscnt].getScene();
//		sgcs[sscnt].setVisible(false);
//		theWorld.addChild(sgcs[sscnt]);
//		sscnt++;
//
		numTexs = sscnt;
		
		theWorld.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		theWorld.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, false);
		theWorld.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);
		
		textool = new TexturePlacementTool();
		updateWorld();
		return theWorld;
	}

	public void updateWorld()	{
//		System.err.println("Step = "+step);
		if (step != oldStep)	{
			if (oldStep >= 0) sgcs[oldStep].setVisible(false);
			if (step >= numTexs) step = numTexs-1;
			sgcs[step].setVisible(true);
//			if (stepAnim != null && stepAnim.getKeyFrames().size() >= step+1) {
//				tmin = stepAnim.getKeyFrames().get(step).getTime();
//				if (step < (numTexs-1)) tmax = stepAnim.getKeyFrames().get(step+1).getTime();
//			} 
			if (animTime != null)
				tmin = animTime.getKeyFrames().get(ap.getInspectedKeyFrame()).getValue();
			currentTex = frontTexs[step];
			textool.setTexture2d(currentTex);
			oldStep = step;
		}
//		if (tmax != tmin) subTime = (time - tmin)/(tmax-tmin);
		subTime = time - tmin;
//		else subTime = time;
		if (viewer!= null) {
			Runnable runnable = new Runnable() {
				public void run() {
					if (ss[step] != null) ss[step].setValueAtTime(time - tmin);
				}
			};
			Scene.executeWriter(viewer.getSceneRoot(), runnable);
			viewer.renderAsync();
		} else
			if (ss[step] != null) ss[step].setValueAtTime(time - tmin);
	}
	
	void setupTextures()	{
		ImageData imageData = null;
		for (int i = 0; i<numTexs; ++i)	{
			frontAps[i] = new Appearance();
			frontAps[i].setAttribute("aspectRatio", texAspectRatios[i]);
			frontTexs[i] = (Texture2D) AttributeEntityUtility
		       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", frontAps[i], true);
			try {
				imageData = ImageData.load(Input.getInput(imagePath+imageNames[i]));
			} catch (IOException e) {
				e.printStackTrace();
			}
					
			frontTexs[i].setImage(imageData);
			frontTexs[i].setRepeatS(Texture2D.GL_CLAMP_TO_EDGE);
			frontTexs[i].setRepeatT(Texture2D.GL_CLAMP_TO_EDGE);
			frontTexs[i].setApplyMode(Texture2D.GL_MODULATE);
			Matrix tmp = new Matrix();
			MatrixBuilder.euclidean().translate(0,1,0).scale(1,-1,1).assignTo(tmp);
			frontTexs[i].setTextureMatrix(tmp);
			backAps[i] = new Appearance();
			backAps[i].setAttribute("aspectRatio", texAspectRatios[i]);
			backTexs[i] = (Texture2D) AttributeEntityUtility
		       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", backAps[i], true);
			try {
				imageData = ImageData.load(Input.getInput(imagePath+imageNames[i]));
			} catch (IOException e) {
				e.printStackTrace();
			}
					
			backTexs[i].setImage(imageData);
			backTexs[i].setRepeatS(Texture2D.GL_CLAMP_TO_EDGE);
			backTexs[i].setRepeatT(Texture2D.GL_CLAMP_TO_EDGE);
			backTexs[i].setApplyMode(Texture2D.GL_MODULATE);
			tmp = new Matrix();
			MatrixBuilder.euclidean().translate(0,1,0).scale(1,-1,1).assignTo(tmp);
			if (i == 5)	{
				double[] mat = {
						.2445747,0,0,.45,
						0,-.2445747,0,.95,
						0,0,1,0,
						0,0,0,1
				};
				tmp = new Matrix(mat);
			}
			backTexs[i].setTextureMatrix(tmp);
		}
		if (true) return;
		if (doStereoTex)	{
			frontAp.setAttribute("polygonShadername", "stereo");
			leftFrontTex = (Texture2D) AttributeEntityUtility
		       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d.left", frontAp, true);
			try {
				imageData = ImageData.load(Input.getInput(
						"http://www.math.tu-berlin.de/~gunn/Pictures/borromeanRings/s03s-3096L.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
					
			leftFrontTex.setImage(imageData);
			leftFrontTex.setRepeatS(Texture2D.GL_CLAMP_TO_EDGE);
			leftFrontTex.setRepeatT(Texture2D.GL_CLAMP_TO_EDGE);
			leftFrontTex.setApplyMode(Texture2D.GL_MODULATE);
//			temple11mat = new Matrix();
//			MatrixBuilder.euclidean().translate(0,1,0).scale(1,-1,1).assignTo(temple11mat);
//			leftFrontTex.setTextureMatrix(temple11mat);
			rightFrontTex = (Texture2D) AttributeEntityUtility
		       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d.right", frontAp, true);
			try {
				imageData = ImageData.load(Input.getInput(
						"http://www.math.tu-berlin.de/~gunn/Pictures/borromeanRings/s03s-3096R.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
					
			rightFrontTex.setImage(imageData);
			rightFrontTex.setRepeatS(Texture2D.GL_CLAMP_TO_EDGE);
			rightFrontTex.setRepeatT(Texture2D.GL_CLAMP_TO_EDGE);
			rightFrontTex.setApplyMode(Texture2D.GL_MODULATE);
		
//			temple11mat = new Matrix();
//			MatrixBuilder.euclidean().translate(0,1,0).scale(1,-1,1).assignTo(temple11mat);
//			rightFrontTex.setTextureMatrix(temple11mat);
		} else {
			frontTex = (Texture2D) AttributeEntityUtility
		       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", frontAp, true);
			try {
				imageData = ImageData.load(Input.getInput(
						"http://www.math.tu-berlin.de/~gunn/Pictures/borromeanRings/s03s-3096.png"));
			} catch (IOException e) {
				e.printStackTrace();
			}
				
			frontTex.setImage(imageData);
			frontTex.setRepeatS(Texture2D.GL_CLAMP_TO_EDGE);
			frontTex.setRepeatT(Texture2D.GL_CLAMP_TO_EDGE);
			frontTex.setApplyMode(Texture2D.GL_MODULATE);
			Matrix tmp = new Matrix();
			MatrixBuilder.euclidean().translate(0,1,0).scale(1,-1,1).assignTo(tmp);
			frontTex.setTextureMatrix(tmp);

		}

	}

	@Override
	public Component getInspector(Viewer v) {
		Box inspectionPanel =  Box.createVerticalBox();
		JCheckBox ttB = new JCheckBox("texture tool");
		ttB.setSelected(false);
		ttB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				if (((JCheckBox)e.getSource()).isSelected())	{
					textool.setTexture2d(currentTex);
					if (!theWorld.getTools().contains(textool)) theWorld.addTool(textool);
				} else 
					if (theWorld.getTools().contains(textool)) theWorld.removeTool(textool);
			}
			
		});
		inspectionPanel.add(ttB);
		
		timeSlider = new TextSlider.Double("time",SwingConstants.HORIZONTAL,0.0,30.0,0.0);
		timeSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				time = timeSlider.getValue().doubleValue();
				updateWorld();
			}
		});
		inspectionPanel.add(timeSlider);
		stepSlider = new TextSlider.Integer("step",SwingConstants.HORIZONTAL,0, 10, 0);
		stepSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				step = stepSlider.getValue().intValue();
				updateWorld();
			}
		});
		inspectionPanel.add(stepSlider);
		inspectionPanel.setName("dancing squares");
		return inspectionPanel;

	}
	@Override
	public boolean hasInspector() {
		return true;
	}
	
	private void setupAnimation()	{
		animPlugin = psloader.getAnimationPlugin();
		List<Animated> animated = animPlugin.getAnimated(); //= new Vector<KeyFrameAnimated>();//
	    ap = animPlugin.getAnimationPanel();
		ap.getRecordPrefs().setDimension(dim);
		
//		SceneGraphAnimator sga = new SceneGraphAnimator(viewer.getSceneRoot());
//		sga.setAnimateCamera(false);
		animPlugin.getSga().setDefaultInterp(InterpolationTypes.LINEAR);
//		sga.setName("sga");
//		sga.init();
//		animated.add(sga);
		
		KeyFrameAnimatedDelegate<Integer> idd = new KeyFrameAnimatedDelegate<Integer> () {

			public void propagateCurrentValue(Integer t) {
				step = t;
				stepSlider.setValue(t);
				updateWorld();
			}

			public Integer gatherCurrentValue(Integer t) {
				return step;
			}
			
		};
		stepAnim = new KeyFrameAnimatedInteger(idd );
		stepAnim.setName("step");
		stepAnim.setInterpolationType(InterpolationTypes.CONSTANT);
		animated.add(stepAnim);
		
		KeyFrameAnimatedDelegate<Double> dd = new KeyFrameAnimatedDelegate<Double> () {

			public void propagateCurrentValue(Double t) {
				time = t;
				timeSlider.setValue(t);
				updateWorld();
			}

			public Double gatherCurrentValue(Double t) {
				return time;
			}
			
		};
		animTime = new KeyFrameAnimatedDouble(dd );
		animTime.setName("time");
		animTime.setInterpolationType(InterpolationTypes.CUBIC_HERMITE);
		animated.add(animTime);
		
		TimeMapper tm = new TimeMapper() {

			public double remapTime(double t, double t0, double t1) {
				double x = AnimationUtility.linearInterpolation(
						t, t0, t1, 1, Math.exp(1));
				double y = Math.log(x);
				return AnimationUtility.linearInterpolation(y, 0, 1, t0, t1);
			}
			
		};
		
		for (int i = 0; i<numTexs; ++i)	{
			final int which = i;
			KeyFrameAnimatedDelegate<FactoredMatrix>	animTexMatDel = new KeyFrameAnimatedDelegate<FactoredMatrix>() {
				public FactoredMatrix gatherCurrentValue(FactoredMatrix t) {
					t.assignFrom(frontTexs[which].getTextureMatrix().getArray());
					t.update();
//					printKeys();
					return t;
				}

				public void propagateCurrentValue(FactoredMatrix t) {
					Matrix tmp = new Matrix(t.getArray());
					frontTexs[which].setTextureMatrix(tmp);
//					System.err.println("texture matrix = "+Rn.matrixToString(t.getArray()));
				}
				
			};
			KeyFrameAnimatedIsometry kfai = new KeyFrameAnimatedIsometry(animTexMatDel);
			kfai.setName("texs"+i);
			kfai.setInterpolationType(InterpolationTypes.LINEAR);
			kfai.setTimeMapper(tm);
			animated.add(kfai);
		}
		
		ImportExport.readInto(ap, this.getClass().getResourceAsStream(matheonKrimi ? "matheonKrimi-10.xml" : "borromean-12.xml"));
		animPlugin.getAnimationPanel().getRecordPrefs().setCurrentDirectoryPath(matheonKrimi ? "/gunn_local/Movies/matheonKrimi" : "/gunn_local/Movies/IMULogo");
		animPlugin.getAnimationPanel().setResourceDir("src/dragonfly/animation/facets/");

		//		AnimationPanelListenerImpl apl = new AnimationPanelListenerImpl(viewer, "application");
//		apl.setAnimated(animated);
//		ap.addAnimationPanelListener(apl);

	}
	
//	void printKeys()	{
//		List<KeyFrame<FactoredMatrix>> foo = temple11tex.getKeyFrames();
//		for (KeyFrame<FactoredMatrix> bar : foo)	 {
//			System.err.println("Time = "+bar.getTime());
//			System.err.println("key = "+Rn.matrixToString(bar.getValue().getArray()));
//		}
//
//	}
	AnimationPanel ap;
	AnimationPlugin animPlugin;
	PluginSceneLoader psloader;
	Viewer viewer;
	SceneGraphPath avatarPath;
	static DancingSquares ls = new DancingSquares();
	private KeyFrameAnimatedDouble animTime;
	private HelixSS hss;
	private RotatingCube rc;
	private ExplosionSS exx;
	@Override
	public void customize(JMenuBar menuBar, PluginSceneLoader psl) {
//		Viewer va = val.viewerAp;
//	    ViewerApp va = new ViewerApp(world, null, null, null);
		psloader = psl;
	    viewer = psl.getViewer();
		charlesgunn.jreality.tools.ToolManager tm = charlesgunn.jreality.tools.ToolManager.toolManagerForViewer(viewer);
//		tm.setActive(false);
			
	    CameraUtility.getCamera(viewer).setFieldOfView(initialFOV);
//	    setCameraDistance(initialFocus);
//	     try {
//			TextureUtility.setBackgroundTexture(viewer.getSceneRoot().getAppearance(),
//					 ImageData.load(new Input(this.getClass().getResource("doorStereo1024.png"))));
//		} catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	    viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", new Color(0,0,0,0)); //Appearance.INHERITED); //
	    viewer.getSceneRoot().getAppearance().setAttribute("backgroundColors", Appearance.INHERITED);
//		if (viewer.hasViewingComponent() && viewer instanceof de.jreality.jogl.JOGLViewer) {
//			de.jreality.jogl.JOGLViewer joglv = (de.jreality.jogl.JOGLViewer) viewer;
//			((Component) joglv.getViewingComponent()).addKeyListener(
//					new ViewerKeyListener(joglv, new HelpOverlay(joglv), 
//							InteractiveViewer.perfInfoOverlayFor(joglv)));			
//		}
//	    va.setAttachNavigator(true);
//	    va.addAccessory(getInspector(null), "Parameters");
//	    va.update();
//	    (va.getViewingComponent()).setPreferredSize(null);  //need change of preferredSize (PropertyChangeEvent)
//	    (va.getViewingComponent()).setPreferredSize(dim);
//		
//		va.getFrame().pack();
//		(va.getViewingComponent()).requestFocusInWindow();
//		va.display();
		setupAnimation();
//		Component panel = ap.getPanel();
//		JFrame frame = new JFrame("Animation Panel");
//		frame.getContentPane().add(panel);
//		frame.pack();
//		frame.setVisible(true);
//		ap.read("/Users/gunn/Documents/animations/borromean-07.xml");

	}

	boolean reproduceOldStereoFrames = false;
	void setCameraDistance(double d)	{
		System.err.println("cam distance set to "+d);
//		CameraUtility.getCameraNode(viewer).getTransformation().setMatrix(
//				P3.makeTranslationMatrix(null, new double[]{0,0,d},Pn.EUCLIDEAN));
		double focus = reproduceOldStereoFrames ? 10 : IMULogo.frontOfScreenFactor*d;
		CameraUtility.getCamera(viewer).setFocus(focus);
		double eyeSeparationFoo = reproduceOldStereoFrames ? .75 : focus/IMULogo.eyeSeparationFactor;
		CameraUtility.getCamera(viewer).setEyeSeparation(eyeSeparationFoo);
//		if (reproduceOldStereoFrames) CameraUtility.getCamera(viewer).setFieldOfView(15.152);
	}

	
}
