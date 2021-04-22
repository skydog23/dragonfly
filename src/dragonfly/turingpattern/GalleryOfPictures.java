/*
 * Created on 14.03.2016
 *
 */
package dragonfly.turingpattern;

import java.awt.Color;
import java.io.IOException;

import charlesgunn.jreality.viewer.Assignment;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.ImageData;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.util.Input;
import de.jreality.util.SceneGraphUtility;

public class GalleryOfPictures extends Assignment {

	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		
		IndexedFaceSet ifs = Primitives.texturedQuadrilateral(
				new double[]{0,0,0, 1,0,0,  1,.75,0,  0,.75,0});
		world.setGeometry(ifs);
		
		Appearance ap = world.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute("polygonShader.diffuseColor", Color.white);
		ap.setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		String[] files = {"22X-02.png",
//				"22X-03.png",
				"22X-04.png",
//				"22X-05.png",
				"22X-07.png",
				"22X-08.png",
				"22X-09.png",
				"22X-16.png",
				"22X-19.png",
				"22X-22.png",
				"22X-24.png",
				"22X-26.png",
				"22X-28.png",
				"22X-34.png"
		};
		ImageData id = null;
		for (int row = 0; row<4; ++row)	{
			for (int column = 0; column<3; ++column)	{
				SceneGraphComponent child = SceneGraphUtility.createFullSceneGraphComponent("child");
				world.addChild(child);
				child.setGeometry(ifs);
				double xgap = .03, ygap = .03;
				MatrixBuilder.euclidean().translate((1+xgap) * column,  (.75+ygap) *row, 0).assignTo(child);
				int which = 3*row+column;
				try {
					id = ImageData.load(Input.getInput(this.getClass().getResource(files[which])));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				ap = child.getAppearance();
				Texture2D tex2d = TextureUtility.createTexture(ap, "polygonShader", id);
//				tex2d.setRepeatS(Texture2D.GL_CLAMP);
//				tex2d.setRepeatT(Texture2D.GL_CLAMP);
//				tex2d.set
				Matrix tm = new Matrix();
				MatrixBuilder.euclidean().scale(.75).assignTo(tm);
				tex2d.setTextureMatrix(tm);
				
			}
		}
		return world;
	}
	
	public static void main(String[] args) {
		new GalleryOfPictures().display();
	}
}
