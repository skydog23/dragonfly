package dragonfly.jreality.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JMenuBar;

import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.math.Complex;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.geometry.SliceBoxFactory;
import de.jreality.math.Rn;
import de.jreality.plugin.experimental.ViewerKeyListener;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

public class BoysSurface extends LoadableScene {
	SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent();
	@Override
	public SceneGraphComponent makeWorld() {
		IndexedFaceSet boys = getBoysSurface();
		world.setGeometry(boys);
		Appearance ap1 = world.getAppearance();
		ap1.setAttribute(CommonAttributes.FACE_DRAW, true);
		ap1.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap1.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap1.setAttribute(CommonAttributes.SMOOTH_SHADING, false);
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+"name", "twoSide");
//		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+".front"+"name", "implode");
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+".front."+CommonAttributes.DIFFUSE_COLOR, new Color(0,204,204));
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+".back."+CommonAttributes.DIFFUSE_COLOR, new Color(204,204,0));
//		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+".implodeFactor", .6);
		ap1.setAttribute(CommonAttributes.POLYGON_SHADER+".vertexShader", "simple");
		ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, true);
		ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(210, 150, 0));
		ap1.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .012);
		ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.SPHERES_DRAW, false);
		ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.SPECULAR_COLOR, new Color(0,255, 255));
		ap1.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(250, 0, 100));
		SliceBoxFactory sbf = new SliceBoxFactory(world);
		sbf.update();
		SceneGraphComponent root = new SceneGraphComponent();
		root.addChild(sbf.getSliceBoxSGC());
//		root.addChild(world);
		return root;
	}
	
	@Override
	public boolean isEncompass() { return false; }
	
	public IndexedFaceSet getBoysSurface() {
		ParametricSurfaceFactory psf = new ParametricSurfaceFactory();
		psf.setImmersion(new ParametricSurfaceFactory.Immersion() {
			public void evaluate(double u, double v, double[] xyz, int index) {
				v = Math.PI*(1/(1-.09))*((Math.pow(.3+.7*Math.log(v), 2)-.09));
				if(v==Math.PI){
					xyz[0]=0;
					xyz[1]=0;
					xyz[2]=0.5;
				}
				else{
					Complex c = new Complex(v,u);
					c = Complex.exp(null, c);
					Complex c2 = Complex.times(null, c, c);
					Complex ic2 = Complex.invert(null, c2);
					Complex c3 = Complex.times(null, c2, c);
					Complex ic3 = Complex.invert(null, c3);
					Complex nenner = Complex.subtract(null, c3, ic3);
					Complex.add(nenner, nenner, new Complex(Math.sqrt(5),0));
					Complex inenner = Complex.invert(null, nenner);
					Complex tmp = Complex.subtract(null, c2, ic2);
					xyz[0] = Complex.times(null, inenner, Complex.times(tmp, tmp, Complex.I)).re;
					xyz[1] = Complex.times(null, inenner, Complex.add(tmp, c2, ic2)).re;
					xyz[2] = Complex.times(null, inenner, Complex.times(null, 
							Complex.add(null, c3, ic3),
							Complex.I)).re * 2/3.0 + 1.0/3.0;
//					x = c2.minus(c2.invert()).timesI().divide(nenner).re;
//					y = c2.plus(c2.invert()).divide(nenner).re;
//					z = c3.plus(c3.invert()).times(2./3).timesI().divide(nenner).re + 0.33333;
				}
				double norm2 = Rn.innerProduct(xyz, xyz);
				if(norm2!=0) Rn.times(xyz, 1.0/norm2, xyz);
			}

			public int getDimensionOfAmbientSpace() {
				return 3;
			}

			public boolean isImmutable() {
				// TODO Auto-generated method stub
				return false;
			}

			
		});
		
		psf.setClosedInUDirection(true);
		psf.setClosedInVDirection(false);
		psf.setULineCount(89);
		psf.setVLineCount(51);
		psf.setUMin(0);
		psf.setUMax(Math.PI*2/1.0);
		psf.setVMin(1.0);
		psf.setVMax(Math.E);
		psf.setGenerateFaceNormals(true);
		psf.setGenerateTextureCoordinates(true);
		psf.setGenerateVertexNormals(true);
		psf.setGenerateEdgesFromFaces(true);
		psf.update();
		IndexedFaceSet boys = psf.getIndexedFaceSet();
		return boys;
	}
	@Override
	public void customize(JMenuBar menuBar, Viewer v) {
		final Viewer viewer = v;
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(0,0, 20));
		((Component) viewer.getViewingComponent()).addKeyListener(new KeyAdapter()	{
			
		    @Override
			public void keyPressed(KeyEvent e)	{ 
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_H:
					System.out.println("	7:  increase/decrease implode factor increment");
					System.out.println("shift-7:  decrease plane movement increment");
					break;

				case KeyEvent.VK_7:
					ViewerKeyListener.modulateValueAdditive(world.getAppearance(), CommonAttributes.POLYGON_SHADER+".implodeFactor", 0.5, .1, -1.0, 1.0, !e.isShiftDown());
				    viewer.renderAsync();
					break;
					}
			}
		});
	}



}
