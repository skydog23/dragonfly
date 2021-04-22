package dragonfly.animation.facets;

import java.awt.Component;

import charlesgunn.anim.gui.AnimationPanel;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import charlesgunn.anim.util.AnimationUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.util.SceneGraphUtility;

public class OpeningDoorsSS implements SubSequence {

	Appearance ap0, ap1;
	double focus;
	DancingSquares ds;
	double[][] dverts = {{1.1,1,1.0},{-1.1,1,1.0}, {-1.1,-1,1.0},{1.1,-1,1.0},
			{-.37, .78, 1.0},
			{.015, .775, 1.0},
			{.4, .78, 1.0},
			{-.361, -.917, 1.0},
			{.028, -.906, 1.0},
			{.424, -.906, 1.0},
			{-.37,1.0,1.0},
			{.4, 1.0,1.0},
			{-.36,-1.0,1.0},
			{.42,-1.0,1.0}};			// front
		double[][] dtexc = new double[dverts.length][2];
		private SceneGraphComponent leftDoor;
		private SceneGraphComponent rightDoor, translator;
	public OpeningDoorsSS(DancingSquares ds)	{
		this.ds = ds;
	}
	public Component getInspector() {
		return null;
	}

	public SceneGraphComponent getScene() {
		SceneGraphComponent parent = SceneGraphUtility.createFullSceneGraphComponent("parent");
		parent.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
		translator = SceneGraphUtility.createFullSceneGraphComponent("translator");
		SceneGraphComponent doors = SceneGraphUtility.createFullSceneGraphComponent("doors");
		SceneGraphComponent frame = SceneGraphUtility.createFullSceneGraphComponent("frame");
		leftDoor = SceneGraphUtility.createFullSceneGraphComponent("leftDoor");
		rightDoor = SceneGraphUtility.createFullSceneGraphComponent("rightDoor");
		SceneGraphComponent backScene = SceneGraphUtility.createFullSceneGraphComponent("backScene");
		doors.setAppearance(ap0);
		backScene.setAppearance(ap1);
		translator.addChildren(doors);
		doors.addChildren(leftDoor, rightDoor, frame);
		parent.addChildren(translator, backScene);
		double[][] texc = {{1,1} ,{0,1},{0,0},{1,0}};
		double[][] verts = {   {1,1,0},{-1,1,0}, {-1,-1,0},{1,-1,0}};			// front
		IndexedFaceSet square = IndexedFaceSetUtility.constructPolygon(verts);
		square.setVertexAttributes(Attribute.TEXTURE_COORDINATES,StorageModel.DOUBLE_ARRAY.array(2).createReadOnly(texc));
		double aspectRatio1 = (Double) ap1.getAttribute("aspectRatio",Double.class);
		double aspectRatio0 = (Double) ap0.getAttribute("aspectRatio",Double.class);
		double d = 0;
		MatrixBuilder.euclidean().translate(0,0,-4).scale((1+d)*aspectRatio1, (1+d), 1).translate(0,0,-d*ds.initialFocus).assignTo(backScene);
		backScene.setGeometry(square);

		for (int i = 0; i<dverts.length; ++i)	{
			dtexc[i][0] = .5*(dverts[i][0]+1);
			dtexc[i][1] = .5*(dverts[i][1]+1);
		}
		int[][] inds = {{1,2,12,10},
				{7,12,13,9},
				{13,3,0,11},
				{11,10,4,6}
		};
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		ifsf.setVertexCount(dverts.length);
		ifsf.setVertexCoordinates(dverts);
		ifsf.setFaceCount(inds.length);
		ifsf.setFaceIndices(inds);
		ifsf.setVertexTextureCoordinates(dtexc);
		ifsf.update();
		frame.setGeometry(ifsf.getGeometry());
		MatrixBuilder.euclidean().scale(aspectRatio0, 1, 1).translate(0,0,-1).assignTo(doors);
		MatrixBuilder.euclidean().translate(0,0,-.05).assignTo(frame);
		
		ifsf = new IndexedFaceSetFactory();
		double[][] vv = {dverts[4],dverts[7],dverts[8], dverts[5]};
		double[][] tc = {dtexc[4], dtexc[7], dtexc[8], dtexc[5]};
		inds = new int[][]{{0,1,2,3}};
		ifsf.setVertexCount(4);
		ifsf.setVertexCoordinates(vv);
		ifsf.setVertexTextureCoordinates(tc);
		ifsf.setFaceCount(1);
		ifsf.setFaceIndices(inds);
		ifsf.update();
		leftDoor.setGeometry(ifsf.getIndexedFaceSet());
		
		ifsf = new IndexedFaceSetFactory();
		vv = new double[][]{dverts[5],dverts[8],dverts[9], dverts[6]};
		tc = new double[][]{dtexc[5], dtexc[8], dtexc[9], dtexc[6]};
		ifsf.setVertexCount(4);
		ifsf.setVertexCoordinates(vv);
		ifsf.setVertexTextureCoordinates(tc);
		ifsf.setFaceCount(1);
		ifsf.setFaceIndices(inds);
		ifsf.update();
		rightDoor.setGeometry(ifsf.getIndexedFaceSet());
		
		double factor = ds.initialFocus * Math.tan((Math.PI/180.0)*ds.initialFOV/2.0);
		MatrixBuilder.euclidean().translate(0,0,-ds.initialFocus).scale(factor, factor, factor).assignTo(parent);
		
		return parent;
	}

	double wait = Math.PI, speed = 3;
	public void setValueAtTime(double t) {
		double angle = 3*t;
		if (angle > wait) angle=wait;
		MatrixBuilder.euclidean().translate(0,0,.01).rotate(dverts[4], dverts[7], .8*angle).assignTo(leftDoor);
		MatrixBuilder.euclidean().translate(0,0,.01).rotate(dverts[9], dverts[6], .8*angle).assignTo(rightDoor);
//		if (t > wait) {
//			System.err.println("t = "+t);
		double tlate = 2*t-wait;
			double tt = AnimationUtility.hermiteInterpolation(tlate, 0, Math.PI, 0, 5.0); //8.5);
			System.err.println(t+" opening door "+tlate);
			MatrixBuilder.euclidean().translate(0,0,tt).assignTo(translator);			
//		}
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
