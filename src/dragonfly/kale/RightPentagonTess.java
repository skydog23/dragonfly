/*
 * Created on Aug 2, 2008
 *
 */
package dragonfly.kale;

import java.awt.Color;

import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.geometry.Primitives;
import de.jreality.math.Matrix;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.pick.PickResult;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;

public class RightPentagonTess extends LoadableScene {

	private IndexedFaceSet pentagon;
	private double[][] pentagonVerts;
	private SceneGraphComponent world;

	@Override
	public SceneGraphComponent makeWorld() {
		world = new SceneGraphComponent();
		pentagon = Primitives.regularPolygon(5);
		world.setGeometry(pentagon);
		pentagonVerts = pentagon.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		SceneGraphComponent c1 = new SceneGraphComponent("0");
		c1.setGeometry(pentagon);
		double[] m = P3.makeRotationMatrix(null, pentagonVerts[0], pentagonVerts[1], 2.12688, Pn.EUCLIDEAN);
		Matrix foo = new Matrix(m);
		foo.assignTo(c1);
		world.addChild(c1);
		SceneGraphComponent c2 = new SceneGraphComponent("4");
		c2.setGeometry(pentagon);
		double[] m2 = P3.makeRotationMatrix(null, pentagonVerts[0], pentagonVerts[4], 2.12688, Pn.EUCLIDEAN);
		new Matrix(m2).assignTo(c2);
		world.addChild(c2);
		c2 = new SceneGraphComponent("4");
		c2.setGeometry(pentagon);
		m2 = P3.makeRotationMatrix(null, pentagonVerts[0], pentagonVerts[4], -2.12688, Pn.EUCLIDEAN);
		new Matrix(m2).assignTo(c2);
		c1.addChild(c2);
		world.setAppearance(new Appearance());
		world.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		AbstractTool at = new AbstractTool(
		InputSlot.getDevice("PrimaryAction")){

		{
	   		addCurrentSlot(InputSlot.getDevice("PointerTransformation"), "drags the center point");
	    }
			@Override
			public void perform(ToolContext tc) {
				if (tc.getCurrentPick() == null) return;
				if (tc.getCurrentPick().getPickType() != PickResult.PICK_TYPE_LINE ) return;
				int i0, i1;
				int edge = tc.getCurrentPick().getIndex();
				i0 = edge;
				i1 = (edge+1)%5;
				SceneGraphComponent c2 = new SceneGraphComponent("c1");
				c2.setGeometry(pentagon);
				double angle = 2.12688;
				double[] m2 = P3.makeRotationMatrix(null, pentagonVerts[i0], pentagonVerts[i1], angle, Pn.EUCLIDEAN);
				new Matrix(m2).assignTo(c2);
				System.err.println("pick path = "+tc.getCurrentPick().getPickPath());
				tc.getCurrentPick().getPickPath().getLastComponent().addChild(c2);

			}
			
		};
	
		world.addTool(at);
		
		return world;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub

	}

}
