package dragonfly.jreality.examples;

import java.awt.Color;

import javax.swing.JMenuBar;

import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;

public class GoodmanMellnikSurface extends LoadableScene {
	@Override
	public SceneGraphComponent makeWorld() {
		IndexedFaceSet boys = getGoodmanMellnikSurface();
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent();
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
//		SliceBoxFactory sbf = new SliceBoxFactory(world);
//		sbf.update();
//		SceneGraphComponent root = new SceneGraphComponent();
//		root.addChild(sbf.getSliceBoxSGC());
//		root.addChild(world);
		return world;
	}
	
	@Override
	public boolean isEncompass() { return true; }
	
	public IndexedFaceSet getGoodmanMellnikSurface() {
		ParametricSurfaceFactory psf = new ParametricSurfaceFactory();
		final double scale = 1/30.0;
		psf.setImmersion(new ParametricSurfaceFactory.Immersion() {
			public void evaluate(double u, double v, double[] xyz, int index) {
				xyz[0] = 6.82683 - 8.32923*(-1 + 3*Math.pow(Math.cos(u),2)) +
				   1.52443*(3 - 30*Math.pow(Math.cos(u),2) +
				      35*Math.pow(Math.cos(u),4)) +
				   24.749*Math.cos(u)*Math.sqrt(1 - Math.pow(Math.cos(u),2))*Math.cos(v) +
				   4.13338*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    (-3*Math.cos(u) + 7*Math.pow(Math.cos(u),3))*Math.cos(v) +
				   2.43089*(-1 + Math.pow(Math.cos(u),2))*Math.cos(2*v) +
				   0.986724*(-1 + Math.pow(Math.cos(u),2))*
				    (-1 + 7*Math.pow(Math.cos(u),2))*Math.cos(2*v) -
				   6.26572*Math.cos(u)*Math.pow(1 - Math.pow(Math.cos(u),2),1.5)*
				    Math.cos(3*v) - 1.15253*Math.pow(-1 + Math.pow(Math.cos(u),2),2)*
				    Math.cos(4*v) - 13.3427*Math.cos(u)*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    Math.sin(v) - 0.551995*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    (-3*Math.cos(u) + 7*Math.pow(Math.cos(u),3))*Math.sin(v) -
				   0.00707669*(-1 + Math.pow(Math.cos(u),2))*Math.sin(2*v) +
				   3.8106*(-1 + Math.pow(Math.cos(u),2))*(-1 + 7*Math.pow(Math.cos(u),2))*
				    Math.sin(2*v) - 21.2974*Math.cos(u)*
				    Math.pow(1 - Math.pow(Math.cos(u),2),1.5)*Math.sin(3*v) -
				   2.43263*Math.pow(-1 + Math.pow(Math.cos(u),2),2)*Math.sin(4*v);

				xyz[1] = 2.80319 - 11.0761*(-1 + 3*Math.pow(Math.cos(u),2)) -
				   1.57916*(3 - 30*Math.pow(Math.cos(u),2) +
				      35*Math.pow(Math.cos(u),4)) +
				   14.538*Math.cos(u)*Math.sqrt(1 - Math.pow(Math.cos(u),2))*Math.cos(v) +
				   9.06055*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    (-3*Math.cos(u) + 7*Math.pow(Math.cos(u),3))*Math.cos(v) -
				   1.09848*(-1 + Math.pow(Math.cos(u),2))*Math.cos(2*v) -
				   12.4112*(-1 + Math.pow(Math.cos(u),2))*
				    (-1 + 7*Math.pow(Math.cos(u),2))*Math.cos(2*v) +
				   33.8551*Math.cos(u)*Math.pow(1 - Math.pow(Math.cos(u),2),1.5)*
				    Math.cos(3*v) + 3.74188*Math.pow(-1 + Math.pow(Math.cos(u),2),2)*
				    Math.cos(4*v) - 23.8569*Math.cos(u)*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    Math.sin(v) + 8.1162*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    (-3*Math.cos(u) + 7*Math.pow(Math.cos(u),3))*Math.sin(v) -
				   0.668554*(-1 + Math.pow(Math.cos(u),2))*Math.sin(2*v) -
				   2.79839*(-1 + Math.pow(Math.cos(u),2))*
				    (-1 + 7*Math.pow(Math.cos(u),2))*Math.sin(2*v) -
				   13.6578*Math.cos(u)*Math.pow(1 - Math.pow(Math.cos(u),2),1.5)*
				    Math.sin(3*v) - 1.15675*Math.pow(-1 + Math.pow(Math.cos(u),2),2)*
				    Math.sin(4*v);

				xyz[2] = 5.51922 - 8.05461*(-1 + 3*Math.pow(Math.cos(u),2)) +
				   0.476394*(3 - 30*Math.pow(Math.cos(u),2) +
				      35*Math.pow(Math.cos(u),4)) +
				   10.293*Math.cos(u)*Math.sqrt(1 - Math.pow(Math.cos(u),2))*Math.cos(v) -
				   13.1672*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    (-3*Math.cos(u) + 7*Math.pow(Math.cos(u),3))*Math.cos(v) +
				   5.90713*(-1 + Math.pow(Math.cos(u),2))*Math.cos(2*v) +
				   3.51352*(-1 + Math.pow(Math.cos(u),2))*
				    (-1 + 7*Math.pow(Math.cos(u),2))*Math.cos(2*v) +
				   12.276*Math.cos(u)*Math.pow(1 - Math.pow(Math.cos(u),2),1.5)*
				    Math.cos(3*v) + 4.42103*Math.pow(-1 + Math.pow(Math.cos(u),2),2)*
				    Math.cos(4*v) + 21.7106*Math.cos(u)*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    Math.sin(v) - 9.24398*Math.sqrt(1 - Math.pow(Math.cos(u),2))*
				    (-3*Math.cos(u) + 7*Math.pow(Math.cos(u),3))*Math.sin(v) -
				   1.46308*(-1 + Math.pow(Math.cos(u),2))*Math.sin(2*v) -
				   4.1966*(-1 + Math.pow(Math.cos(u),2))*(-1 + 7*Math.pow(Math.cos(u),2))*
				    Math.sin(2*v) - 0.644064*Math.cos(u)*
				    Math.pow(1 - Math.pow(Math.cos(u),2),1.5)*Math.sin(3*v) -
				   0.382244*Math.pow(-1 + Math.pow(Math.cos(u),2),2)*Math.sin(4*v);
				Rn.times(xyz, scale, xyz);
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
		psf.setULineCount(40);
		psf.setVLineCount(20);
		psf.setUMin(0.1);
		psf.setUMax(Math.PI-.1);
		psf.setVMin(0.0);
		psf.setVMax(Math.PI);
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
	}



}
