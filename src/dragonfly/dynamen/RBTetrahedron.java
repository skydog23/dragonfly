/*
 * Created on Jul 30, 2011
 *
 */
package dragonfly.dynamen;

import java.awt.Color;

import javax.swing.JMenuBar;

import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.CameraUtility;
import de.jreality.util.SceneGraphUtility;

public class RBTetrahedron extends LoadableScene {

	@Override
	public SceneGraphComponent makeWorld() {
		SceneGraphComponent sgc = SceneGraphUtility.createFullSceneGraphComponent("world");
		sgc.setGeometry(tetrahedron());
		Appearance ap = sgc.getAppearance();
		ap.setAttribute(CommonAttributes.FACE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute(CommonAttributes.LEVEL_OF_DETAIL, 1.0);
		ap.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.blue);
		ap.setAttribute(CommonAttributes.POINT_RADIUS, .3);
		ap.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		ap.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.TEXT_SHADER+"."+CommonAttributes.TEXT_SCALE,
				.005);
		ap.setAttribute(CommonAttributes.POINT_SHADER+"."+CommonAttributes.TEXT_SHADER+"."+CommonAttributes.TEXT_OFFSET,
				new double[]{.2,0,.2});
		return sgc;
	}

	@Override
	public void customize(JMenuBar menuBar, PluginSceneLoader psl) {
		// TODO Auto-generated method stub
		super.customize(menuBar, psl);
		psl.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, Color.white);
		CameraUtility.getCamera(psl.getViewer()).setFieldOfView(28);
	}

	static private double[][] tetrahedronVerts3 =  
	{{1,1,1},{1,-1,-1},{-1,1,-1},{-1,-1,1}};

	static private double[] tetrahedronRadii = {.2, .5, .35, .7};
	static private double[][] tetrahedronColors = {
		{0d, 1d, 0d},
		{0d, 0d, 1d},
		{1d, 0d, 0d},
		{1d, 0d, 1d}
	};
	
	static private int[][] tetrahedronIndices = {
		{0,1,2},
		{2,1,3},
		{1,0,3},
		{0,2,3}};

	public static IndexedFaceSet tetrahedron()	{

		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		ifsf.setVertexCount(4);
		ifsf.setVertexCoordinates(tetrahedronVerts3);
		ifsf.setVertexAttribute(Attribute.RELATIVE_RADII, tetrahedronRadii);
//		ifsf.setVertexColors(tetrahedronColors);
//		ifsf.setVertexLabels(new String[]{"m0", "m1","m2","m3"});
		ifsf.setFaceCount(4);
		ifsf.setFaceIndices(tetrahedronIndices);
		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.update();
		
		return ifsf.getIndexedFaceSet();
	}

}
