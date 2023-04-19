/*
 * Created on 7 Apr 2023
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Transformation;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.core.DiscreteGroup;
import de.jtem.discretegroup.core.DiscreteGroupElement;
import de.jtem.discretegroup.core.DiscreteGroupSceneGraphRepresentation;

public class TetraChainPattern extends Assignment {

	double[][] pts = {{0,0,0,1},{1,0,0,1},{.5, Math.sqrt(3.0)/2,0,1}};
	double[][] tab = new double[4][];
	SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world"),
			tlateSGC = SceneGraphUtility.createFullSceneGraphComponent("tlate"),
			tlate2SGC = SceneGraphUtility.createFullSceneGraphComponent("tlate2"),
			onerowSGC = SceneGraphUtility.createFullSceneGraphComponent("onerow"),
			triSGC = SceneGraphUtility.createFullSceneGraphComponent("tri"),
			tabSGC = SceneGraphUtility.createFullSceneGraphComponent("tab");
	SceneGraphComponent universe = new SceneGraphComponent("universe");
	double[] plane = P3.planeFromPoints(null, pts[1], pts[2], new double[] {0,0,1,0});
	int tcount = 10;
	@Override
	public SceneGraphComponent getContent() {
		Appearance ap = world.getAppearance();
		IndexedFaceSet tri = IndexedFaceSetUtility.constructPolygon(pts);
		triSGC.setGeometry(tri);
		ap = world.getAppearance();
		ap.setAttribute(CommonAttributes.FACE_DRAW, false);
		ap.setAttribute(CommonAttributes.TUBES_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		
		DiscreteGroup oneRowDG = new DiscreteGroup();
		DiscreteGroupElement dge[] = new DiscreteGroupElement[8];
		Matrix ref = MatrixBuilder.euclidean().reflect(plane).getMatrix();
		for (int i = 0; i<4; ++i)	{
			Matrix rot = MatrixBuilder.euclidean().translate(i,0,0).getMatrix();
			String rw = (new String[] {"","t","tt","ttt"})[i];
			for (int j = 0; j<2; ++j) {
				Matrix reft = j == 0 ? new Matrix() : ref;
				dge[2*i+j] = new DiscreteGroupElement();
				dge[2*i+j].setMatrix(Matrix.times(rot, reft));
				String rfw = j == 0 ? "" : "R";
				dge[2*i+j].setWord(rw+rfw);
			}
		}
		oneRowDG.setElementList(dge);
		DiscreteGroupSceneGraphRepresentation dgsgr = new DiscreteGroupSceneGraphRepresentation(oneRowDG);
		dgsgr.setWorldNode(triSGC);
		dgsgr.update();
		// add a tab for gluing on the LHS
		Matrix m = MatrixBuilder.euclidean().rotate(Math.PI/3, 0, 0, 1).translate(0,.2,0).translate(.5,0,0).scale(.8).translate(-.5,0,0).getMatrix();
		tab[0] = pts[0];
		tab[1] = pts[2];
		tab[2] = m.multiplyVector(pts[1]);
		tab[3] = m.multiplyVector(pts[0]);
		IndexedFaceSet tabIFS = IndexedFaceSetUtility.constructPolygon(tab);
		tabSGC.setGeometry(tabIFS);
		onerowSGC.addChildren(dgsgr.getRepresentationRoot(), tabSGC);
		updateTranslate();
		world.addChild(tlateSGC);
		tlate2SGC.addChild(world);
		MatrixBuilder.euclidean().translate(3,0,0).assignTo(tlate2SGC);
		universe.addChildren(world); //, tlate2SGC);
		updateAngle();
		return universe;
	}

	private void updateAngle() {
		double a4Angle = Math.atan(Math.sqrt(2.0)),
				angle = Math.atan2((tcount-2)*Math.sqrt(3), tcount + 8),
		        correction = a4Angle - angle;
		MatrixBuilder.euclidean().rotate(correction,0,0,1).assignTo(universe);
	}

	private void updateTranslate() {
		tlateSGC.removeAllChildren();
		Matrix tlate = MatrixBuilder.euclidean().translate(pts[0], pts[2]).getMatrix();
		Matrix acc = new Matrix();
		for (int i = 0; i<tcount; ++i)	{
			SceneGraphComponent child = new SceneGraphComponent("child"+i);
			child.addChild(onerowSGC);
			tlateSGC.addChild(child);
			child.setTransformation(new Transformation(acc.getArray()));
			acc = Matrix.times(acc,tlate);
		}
	}

	@Override
	public void display() {
		// TODO Auto-generated method stub
		super.display();
		jrviewer.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, Color.white);

	}

	@Override
	public Component getInspector() {
		Box container = Box.createVerticalBox();
		final TextSlider cSlider = new TextSlider.Integer("count",  SwingConstants.HORIZONTAL, 1,15,tcount);
		cSlider.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				tcount = cSlider.getValue().intValue();
				updateTranslate();	
				updateAngle();
			}
		});
		container.add(cSlider);
		inspector.add(container);
		return inspector;
	}

	public static void main(String[] args) {
		new TetraChainPattern().display();
	}

}
