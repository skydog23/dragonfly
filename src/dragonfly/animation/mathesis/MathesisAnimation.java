package dragonfly.animation.mathesis;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import charlesgunn.anim.core.AnimatedThing;
import charlesgunn.anim.core.Settable;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.anim.sets.AnimatedDoubleSet;
import charlesgunn.anim.sets.AnimatedRectangle2DSet;
import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.Primitives;
import de.jreality.jogl.AbstractViewer;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.ImageData;
import de.jreality.shader.Texture2D;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.SceneGraphUtility;

public class MathesisAnimation extends LoadableScene implements Settable {

	static final int LEFT = 0;
	static final int RIGHT = 1;
	static final int[][] indices4 = {{0,1,2,3,0}};
	static final int[][] face4 = {{0,1,2,3}};
	static final int[][] indices23 = {{0,1,2,0},{2,3,0,2}};
	static Matrix conjor = new Matrix();
	transient double t;
	transient private OneHalf left2, right2, twoSquareLeft, twoSquareRight;
	transient Viewer theViewer;
	transient double angle = .97; //1.048388; // measured from scanned image of Euclid thm 47 Math.PI/3.0;
	transient double scaler = .41;
	transient double xcenter = .418, ycenter = .535; //.4125, ycenter = .5;  //.462
	transient private SceneGraphComponent imageSGC, twoSquaresSGC;
	// animation proper
	transient double[] dkeys = {0,1,4,1,5,1,4,1};
	transient double[] values = {0,0,1,1,2,2,3,3};
	transient AnimatedDoubleSet pipedTime = new AnimatedDoubleSet(new double[][]{dkeys},
			new double[][]{values}, new int[]{2});
	
	transient private Texture2D tex2d;
	transient private Timer timer;
	transient boolean startAnimating = false, renderOffscreen = false;
	transient AbstractViewer joglViewer;
	static {
		MatrixBuilder.euclidean().translate(1,0,0).assignTo(conjor);
	}
	@Override
	public SceneGraphComponent makeWorld() {
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		Appearance ap = world.getAppearance();
//		ap.setAttribute(CommonAttributes.TUBES_DRAW, true);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR,new Color(50,255,50));
		ap.setAttribute(SceneGraphAnimator.ANIMATED, false);

		double c = Math.cos(angle);
		double s = Math.sin(angle);
		double[][] fullfigure = {
				{c*c, c*s, 0},
				{0,0,0},
				{1,0,0},
				{0,-1,0},
				{1,-1,0},
				{-c*s,c*c,0},
				{c*c-c*s,c*c+c*s,0},
				{c*c+c*s,c*s+s*s,0},
				{1+c*s,s*s,0}
		};
		int[][] edges = {{0,1},{1,2},{2,0},{1,5},{5,6},{6,0},{0,7},{7,8},{8,2},{2,4},{4,3},{3,1}};
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		ifsf.setVertexCount(fullfigure.length);
		ifsf.setVertexCoordinates(fullfigure);
		ifsf.setEdgeCount(edges.length);
		ifsf.setEdgeIndices(edges);
		ifsf.update();
		wireframeSGC = SceneGraphUtility.createFullSceneGraphComponent("wireframe");
		collectorSGC = SceneGraphUtility.createFullSceneGraphComponent("collector");
		wireframeSGC.setGeometry(ifsf.getGeometry());
		ap = wireframeSGC.getAppearance();
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR,new Color(0,255,0));
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.TUBE_RADIUS, .005);
		left2 = new OneHalf(LEFT, angle);
		right2 = new OneHalf(RIGHT, angle);
		imageSGC = SceneGraphUtility.createFullSceneGraphComponent("image");
		double ar = 512/391.0;
		imageSGC.setGeometry(Primitives.texturedQuadrilateral( 
				new double[]{0,0,0,   ar,0,0,  ar,1,0,   0,1,0}));
		MatrixBuilder.euclidean().scale(6).translate(-.5*ar,-.5,.01).assignTo(imageSGC);
		ap = imageSGC.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		ap.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		tex2d = (Texture2D) AttributeEntityUtility
	       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", ap, true);
		try {
			ImageData id = ImageData.load(Input.getInput(
					"http://www.math.tu-berlin.de/~gunn/Pictures/twoPages512.jpg")); // weaveRGBABright.png"));
			tex2d.setImage(id);
		} catch (IOException e) {
			e.printStackTrace();
		}
	  	
		tex2d.setApplyMode(Texture2D.GL_MODULATE);
//		tex2d.setCombineModeColor(Texture2D.GL_INTERPOLATE);
//		tex2d.setOperand2Color(Texture2D.GL_SRC_ALPHA);
		Matrix tm = new Matrix();
		MatrixBuilder.euclidean().translate(0, 0, 0).translate(.5,.5,0).scale(2).scale(1,-1,1).assignTo(tm);
		tex2d.setTextureMatrix(tm);
		SceneGraphComponent animSGC = SceneGraphUtility.createFullSceneGraphComponent();
		animSGC.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, false);

		MatrixBuilder.euclidean().scale(1.03).translate(-.49,-.05,0).assignTo(animSGC);
		twoSquareLeft = new OneHalf(LEFT, angle);
		twoSquareRight = new OneHalf(RIGHT, angle);
		twoSquareLeft.mySetValueAtTime(3.0);
		twoSquareRight.mySetValueAtTime(3.0);
		twoSquaresSGC = SceneGraphUtility.createFullSceneGraphComponent("two squares");
		twoSquaresSGC.addChildren(twoSquareLeft.getSceneGraphComponent(), twoSquareRight.getSceneGraphComponent());
		twoSquaresSGC.setVisible(false);
		animSGC.addChildren(wireframeSGC,collectorSGC);
		collectorSGC.getAppearance().setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		collectorSGC.addChildren(left2.getSceneGraphComponent(), right2.getSceneGraphComponent(), twoSquaresSGC);
		world.addChildren(animSGC, imageSGC);
		return world;
	}
//	public boolean isEncompass() { return true; }
	transient double zoomTime = 5.0;
	transient double fadeTime = 5.0;
	transient double mainTime = 10.0;
	transient double endTime = 3.0;
	transient double animationTime = zoomTime + fadeTime + 2*mainTime + endTime, someOtherTime = 0;
	transient int  ticksPerSecond=renderOffscreen ? 25 : 50, totalTicks, tick = 0;
	transient double[][] imageDKeys = {{0,zoomTime}};
	transient Matrix toImageCenter = new Matrix();
	transient Rectangle2D[][] valueRecs = {{
			new Rectangle2D.Double(0,0,1,1),
			new Rectangle2D.Double(xcenter - scaler/2, (ycenter - scaler/2), scaler, scaler)
	}};
	transient double[][] rotateVals = {{0,-Math.PI*.093}};
	transient AnimatedRectangle2DSet rectSet = new AnimatedRectangle2DSet(imageDKeys, valueRecs, 
		 new AnimationUtility.InterpolationTypes[]{AnimationUtility.InterpolationTypes.LINEAR});
	transient AnimatedDoubleSet rotateSet = new AnimatedDoubleSet(imageDKeys, rotateVals, 
			new AnimationUtility.InterpolationTypes[]{AnimationUtility.InterpolationTypes.CUBIC_HERMITE});
	
	// fade out image to reveal green figure
	transient double[][] fadeDKeys = {{0, fadeTime}};
	transient double[][] fadeValues = {{0.0, 1.0}};
	transient AnimatedDoubleSet fadeSet = new AnimatedDoubleSet(fadeDKeys, fadeValues);
	
	@Override
	public void customize(JMenuBar menuBar, final Viewer viewer) {
		theViewer = viewer;
		if (viewer instanceof AbstractViewer) joglViewer = (AbstractViewer) viewer;
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(20,20,20));
		MatrixBuilder.euclidean().translate(0,0,2.5).assignTo(CameraUtility.getCameraNode(viewer));
		someOtherTime = pipedTime.getTMax();
		totalTicks = ticksPerSecond * 10; //(int) (totalTime*ticksPerSecond);
		MatrixBuilder.euclidean().translate(xcenter, 1.0*ycenter, 0).assignTo(toImageCenter);
		origImageM = imageSGC.getTransformation().getMatrix();
		origTM = tex2d.getTextureMatrix().getArray();		
		AnimationPlugin apl = psl.getAnimationPlugin();
		AnimatedThing me = new AnimatedThing(this);
		me.setName(getClass().getName());
		apl.getAnimated().add(me);
		try {
			apl.getAnimationPanel().read(new Input(this.getClass().getResource("mathesis-anim.xml")));
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}		
		((Component) viewer.getViewingComponent()).addKeyListener( new KeyAdapter()	{

			@Override
			public void keyPressed(KeyEvent e)	{ 
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_H:
					System.out.println("	1:  toggle animation");
					break;
	
				case KeyEvent.VK_1:
					startAnimating = !startAnimating;
					if (startAnimating) timer.start();
					else timer.stop();
					break;
				}
			}
		});
		timer = new Timer(1000/ticksPerSecond, new ActionListener() {
			double[] values = new double[1];
			Rectangle2D[] rectVal = new Rectangle2D[1];
			double[] origTM = tex2d.getTextureMatrix().getArray();
			Matrix currentTM = new Matrix();
			public void actionPerformed(ActionEvent e) { 
				double time = tick/((double) ticksPerSecond);
				setValueAtTime(time);
				tick++;
				if (time >animationTime) {
					time = animationTime;
					tick = 0;
					endAnimation();
					timer.stop();
				}
			};
//				double t1, t;
//				int relativeTick = tick;
//				if (relativeTick <= zoomTime*ticksPerSecond) {
//					twoSquaresSGC.setVisible(false);
//					t1 = (((double) relativeTick) / ticksPerSecond);
//					rectSet.getValuesAtTime(t1, rectVal);
//					//System.err.println("Rect = "+rectVal[0]);
//					currentTM.assignFrom(Rn.times(null, 
//							origTM,
//							AnimationUtility.matrixFromRectangle(null, rectVal[0])));
//					tex2d.setTextureMatrix(currentTM);	
//					rotateSet.getValuesAtTime(t1, values);
//					double[] rotater = Rn.conjugateByMatrix(null, P3.makeRotationMatrixZ(null, -values[0]), toImageCenter.getArray());
//					imageSGC.getTransformation().setMatrix(Rn.times(null,  origImageM, rotater));
//					renderFrame();
//					tick++;
//					return;
//				} 
//				relativeTick -= zoomTime*ticksPerSecond;
//				imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
//				if (relativeTick < fadeTime * ticksPerSecond) {
//					t1 = (((double) relativeTick) / ticksPerSecond);
//					fadeSet.getValuesAtTime(t1, values);
//					System.err.println("Value is "+values[0]);
//					imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY, values[0]);
//					float val = (float) (1-values[0]);
//					tex2d.setBlendColor(new Color(val,val,val,val));
//					renderFrame();
//					tick++;
//					return;
//				}
//				relativeTick -= fadeTime*ticksPerSecond;
//				imageSGC.setVisible(false);
//				if (relativeTick < 2 *totalTicks)	{
//					t1 = (relativeTick > totalTicks) ?  (animationTime*(2*totalTicks-relativeTick)/totalTicks) :((animationTime*relativeTick)/totalTicks);
//					t = pipedTime.getValuesAtTime(t1, values)[0];
//					if (relativeTick > totalTicks) {
//						twoSquaresSGC.setVisible(true);
//					}
//					left2.mySetValueAtTime(t);
//					right2.mySetValueAtTime(t);
//					renderFrame();
//					tick++;
//					return;
//				}
//				relativeTick -= 2*totalTicks;
//				if (relativeTick < endTime*ticksPerSecond)	{
//					tick++;
//					renderFrame();
//					return;
//				}
//				tick = 0;
//				timer.stop();
//			}
//			
		});
		if (startAnimating) timer.start();
	}
	@Override
	public boolean hasInspector() {return true; }
	@Override
	public Component getInspector(final Viewer viewer) {	
		Box inspectionPanel =  Box.createVerticalBox();
		timeSlider = new TextSlider.Double("time",SwingConstants.HORIZONTAL,0.0,animationTime,t);
		timeSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				t = timeSlider.getValue().doubleValue();
				System.err.println("time: "+t);
				psl.getAnimationPlugin().getAnimationPanel().setCurrentTime(t);
				viewer.renderAsync();
			}
		});
		inspectionPanel.add(timeSlider);
		JCheckBox linearHoleButton = new JCheckBox("animate", startAnimating);
		linearHoleButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				startAnimating= ((JCheckBox) e.getSource()).isSelected();
//				psl.getAnimationPlugin().getAnimationPanel().setPaused(!startAnimating);
				if (startAnimating) 
					timer.start();
				else 
					timer.stop();
				System.err.println("Timer is "+startAnimating);
			}
		});
		inspectionPanel.add(linearHoleButton);
		inspectionPanel.setName("ReadMe");
		return inspectionPanel;
	}
	protected class OneHalf	{
		int which;
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		SceneGraphComponent sgc = new SceneGraphComponent();
		Matrix m = new Matrix(), m1 = new Matrix();
		double[][] verts = new double[4][3];
		double alpha, cos, sin;
		OneHalf(int w, double a){
			which = w;
			alpha = a;
			cos = Math.cos(alpha);
			sin = Math.sin(alpha);
			if (which == LEFT) verts = new double[][]{{0,0,0},{0,-1,0},{cos*cos,-1,0},{cos*cos,0,0}};
			else verts = new double[][]{{cos*cos,0,0},{cos*cos,-1,0},{1,-1,0},{1,0,0}};
			ifsf.setVertexCount(4);
			ifsf.setVertexCoordinates(verts);
			ifsf.setEdgeCount(1);
			ifsf.setEdgeIndices(indices4);
			ifsf.setFaceCount(1);
			ifsf.setFaceIndices(face4);
			ifsf.update();
			sgc.setGeometry(ifsf.getIndexedFaceSet());
			MatrixBuilder.euclidean().rotateZ(Math.PI/2).assignTo(m1);
			Appearance ap = new Appearance();
			sgc.setAppearance(ap);
			ap.setAttribute("polygonShader.diffuseColor",which == LEFT ? 
					new Color(1f,1f,0f) :
						new Color(0f,1f,1f));
			ap.setAttribute("lineShader.diffuseColor",which == LEFT ? 
					new Color(1f,1f,0f) :
						new Color(0f,1f,1f));
		}
		
		public SceneGraphComponent getSceneGraphComponent()	{
			return sgc;
		}
		
		public void mySetValueAtTime(double t)	{
			double frac = t % 1.0;
			int phase = (int) t;
			m.assignIdentity();
			switch(phase)	{
			case 0:
				m.assignFrom(getShearUp(frac));
				break;
			case 1:
				m.assignFrom(getRotateRound(frac));
				break;
			default:
				frac = 1.0;
			case 2:
				m.assignFrom(getShearAcross(frac));
				break;
			}
			if (which == RIGHT) m.conjugateBy(conjor);
			m.assignTo(sgc);
		}

		private double[] getShearAcross(double t) {
			Matrix moo = new Matrix();
			if (which == LEFT)	moo.assignFrom(makeShear(null, 
					new double[]{-Math.sin(alpha),Math.cos(alpha)}, 
					-t*Math.tan(alpha)));
			else moo.assignFrom(makeShear(null, 
					new double[]{Math.cos(alpha),Math.sin(alpha)}, 
					t/Math.tan(alpha)));
			moo.multiplyOnRight(getRotateRound(1.0));
			return moo.getArray();
		}

		private double[] getRotateRound(double t) {
			Matrix moo = new Matrix();
			if (which == LEFT)	MatrixBuilder.euclidean().rotateZ(t*Math.PI/2).assignTo(moo);
			else MatrixBuilder.euclidean().rotateZ(-t*Math.PI/2).assignTo(moo);
			moo.multiplyOnRight(getShearUp(1.0));
			return moo.getArray();
		}

		private double[] getShearUp(double t) {
			Matrix moo = new Matrix();
			if (which == LEFT)	moo.setEntry(1, 0, t * Math.tan(alpha));
			else {
				moo.setEntry(1,0,-t/Math.tan(alpha));
			}
			return moo.getArray();
		}
		
	}
	
	public static double[] makeShear(double[] dst, double[] direction, double shearFactor) {
		if (dst == null) dst = new double[16];
		Rn.setIdentityMatrix(dst);
		double d = Rn.innerProduct(direction, direction, 2);
		d = 1.0/Math.sqrt(d);
		double x = direction[0] * d; double y = direction[1] * d;
		dst[0] = 1-x*y*shearFactor;
		dst[1] = x*x*shearFactor;
		dst[4] = -y*y*shearFactor;
		dst[5] = 1+x*y*shearFactor;
		return dst;
	}
	transient double[] origImageM;
	transient Rectangle2D[] rectVal = new Rectangle2D[1];
	transient double[] origTM;
	transient Matrix currentTM = new Matrix();
	transient private SceneGraphComponent wireframeSGC, collectorSGC;
	private TextSlider timeSlider;
	public void setValueAtTime(double time) {
		if (time == 0.0) 
			startAnimation();
//		public void actionPerformed(ActionEvent e) {
		timeSlider.setValue(time);
			double t1, t;
			double[] voolois = new double[1];
			if (time <= zoomTime) {
				twoSquaresSGC.setVisible(false);
				imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY, 0.0);
				imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, false);
				t1 = time; //(((double) relativeTick) / ticksPerSecond);
				rectSet.getValuesAtTime(t1, rectVal);
//				System.err.println("Time = "+time+" Rect = "+rectVal[0]);
				currentTM.assignFrom(Rn.times(null, 
						origTM,
						AnimationUtility.matrixFromRectangle(null, rectVal[0])));
				tex2d.setTextureMatrix(currentTM);	
				rotateSet.getValuesAtTime(t1, voolois);
//				System.err.println("Rotate Value at time "+time+" is "+voolois[0]);
				double[] rotater = Rn.conjugateByMatrix(null, P3.makeRotationMatrixZ(null, -voolois[0]), toImageCenter.getArray());
				imageSGC.getTransformation().setMatrix(Rn.times(null,  origImageM, rotater));
//				renderFrame();
//				tick++;
				return;
			}
//			} else 
//				relativeTick -= zoomTime*ticksPerSecond;
			imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
			time -= zoomTime;
			if (time < fadeTime) {
				t1 = time; //(((double) relativeTick) / ticksPerSecond);
				fadeSet.getValuesAtTime(t1, voolois);
//				System.err.println("Fade Value at time "+time+" is "+voolois[0]);
				imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY, voolois[0]);
				float val = (float) (1-voolois[0]);
				tex2d.setBlendColor(new Color(val,val,val,val));
//					renderFrame();
//					tick++;
				return;
			}
//			relativeTick -= fadeTime*ticksPerSecond;
			time -= fadeTime;
			imageSGC.setVisible(false);
			if (time < 2 *mainTime)	{
				t1 = (someOtherTime/mainTime)* ((time > mainTime) ?  ((2*mainTime-time)) :((time)));
				t = pipedTime.getValuesAtTime(t1, voolois)[0];
				if (time > mainTime) {
					twoSquaresSGC.setVisible(true);
//					twoSquaresSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, false);
				}
//				System.err.println("Main time "+time);
				left2.mySetValueAtTime(t);
				right2.mySetValueAtTime(t);
//				renderFrame();
//				tick++;
				return;
			}
			time -= 2*mainTime;
			
			if (time < endTime)	{
//				tick++;
//				renderFrame();
				return;
			}
//			tick = 0;
//			timer.stop();
	}
	
	public void startAnimation()	{
		System.err.println("In startAnimation");
		twoSquaresSGC.setVisible(false);
		imageSGC.setVisible(true);
		imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY, 0.0);
		imageSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, false);
		collectorSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
		collectorSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY, 0.25);
	}

	public void stopAnimation()	{
		System.err.println("In endAnimation");
		collectorSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, false);
		collectorSGC.getAppearance().setAttribute(CommonAttributes.TRANSPARENCY, 0.0);
	}
//	public static void main(String[] args) {
//
//		final ViewerApp va = TestViewerApp.mainImpl(
//				new String[]{"dragonfly.animation.mathesis.MathesisAnimation"});
//		//viewerApp options
//		va.setAttachNavigator(false);
//		va.setExternalNavigator(false);
//		va.setAttachBeanShell(false);
//		va.setExternalBeanShell(false);
//
//		va.update();
//		va.display();
//	}

}
