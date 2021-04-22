package dragonfly.animation.facets;

import java.awt.Component;

import charlesgunn.anim.gui.AnimationPanel;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.util.SceneGraphUtility;

public class RotatingCube implements SubSequence {
	Appearance ap0, ap1;
	double focus;
	DancingSquares ds;
	private SceneGraphComponent rotate;
	private double factor;
	private double aspectRatio0;
	private double aspectRatio1;
	public RotatingCube(DancingSquares ds)	{
		this.ds = ds;
	}
	public Component getInspector() {
		return null;
	}

	public SceneGraphComponent getScene() {
		SceneGraphComponent parent = SceneGraphUtility.createFullSceneGraphComponent("rotatingCube");
		rotate = SceneGraphUtility.createFullSceneGraphComponent("rotate");
		SceneGraphComponent face1 = SceneGraphUtility.createFullSceneGraphComponent("face1");
		SceneGraphComponent face2 = SceneGraphUtility.createFullSceneGraphComponent("face2");
		face1.setAppearance(ap0);
		face2.setAppearance(ap1);
		rotate.addChildren(face1, face2);
		parent.addChild(rotate);
		double[][] texc = {{1,1} ,{0,1},{0,0},{1,0}};
		double[][] verts = {   {1,1,1},{-1,1,1}, {-1,-1,1},{1,-1,1}};			// front
		IndexedFaceSet square = IndexedFaceSetUtility.constructPolygon(verts);
		square.setVertexAttributes(Attribute.TEXTURE_COORDINATES,StorageModel.DOUBLE_ARRAY.array(2).createReadOnly(texc));
		aspectRatio0 = (Double) ap0.getAttribute("aspectRatio",Double.class);
		aspectRatio1 = (Double) ap1.getAttribute("aspectRatio",Double.class);
		MatrixBuilder.euclidean().scale(aspectRatio0, 1, aspectRatio1).assignTo(face1);
		face1.setGeometry(square);
		MatrixBuilder.euclidean().rotateY(Math.PI/2).scale(aspectRatio1, 1, aspectRatio0).assignTo(face2);
		face2.setGeometry(square);
		
		factor = ds.initialFocus * Math.tan((Math.PI/180.0)*ds.initialFOV/2.0);
		MatrixBuilder.euclidean().translate(0,0,-aspectRatio1*factor-ds.initialFocus).scale(factor, factor, factor).assignTo(parent);
//		MatrixBuilder.euclidean().translate(0,0,-ds.initialFocus).scale(factor, factor, factor).assignTo(parent);
		
		return parent;
	}

	public void setValueAtTime(double t) {
		MatrixBuilder.euclidean().translate(0,0,(t/(Math.PI))*factor*(aspectRatio1-aspectRatio0)).rotateY(-t).assignTo(rotate);
	}

	public void setupAnimation(AnimationPanel ap) {
		
	}

	public void setAppearance0(Appearance ap) {
		ap0 = ap;
	}

	public void setAppearance1(Appearance ap) {
		ap1 = ap;
	}


}
