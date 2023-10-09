/*
 * Created on 1 Oct 2023
 *
 */
package dragonfly.penrose;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.SwingConstants;

import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.jreality.geometry.projective.LineUtility;
import charlesgunn.jreality.newtools.FlyTool;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.math.p5.PlueckerLineGeometry;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.PointSetFactory;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.tools.DragEventTool;
import de.jreality.tools.PointDragEvent;
import de.jreality.tools.PointDragListener;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.groups.ArchimedeanSolids;
import de.jtem.discretegroup.util.WingedEdge;

public class Cuboctahedron extends Assignment {
	
	final Color c1 = new Color(255, 0, 255),
			c2 = new Color(0,255,200),
			cz = new Color(200,200,0),
			vc1 = new Color(255,0,0),
			vc2 = new Color(0,255,0),
			vc3 = new Color(100,100,255);

	SceneGraphComponent
		world,
			linesSGC,
				olinesSGC,
				nlinesSGC,
			pointsSGC,
			cuboctahedronSGC;

	WingedEdge co = ArchimedeanSolids.archimedeanSolid("3.4.3.4");

	@Override
	public SceneGraphComponent getContent() {
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		linesSGC = SceneGraphUtility.createFullSceneGraphComponent("lines");
		olinesSGC = SceneGraphUtility.createFullSceneGraphComponent("olines");
		nlinesSGC = SceneGraphUtility.createFullSceneGraphComponent("nlines");
		pointsSGC = SceneGraphUtility.createFullSceneGraphComponent("points");
		cuboctahedronSGC = SceneGraphUtility.createFullSceneGraphComponent("triangles");
		world.addChildren(linesSGC,pointsSGC,cuboctahedronSGC);
		linesSGC.addChildren(olinesSGC,nlinesSGC);
		MatrixBuilder.euclidean().rotateX(-Math.PI/2).assignTo(world);

		
		Appearance ap = pointsSGC.getAppearance();
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		ap.setAttribute("pointShader.pointRadius", .03);
		ap.setAttribute("pointShader.diffuseColor", Color.red);
		ap = linesSGC.getAppearance();
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute(CommonAttributes.PICKABLE, false);
		ap = olinesSGC.getAppearance();
		ap.setAttribute("lineShader.tubeRadius", .005);
		ap = cuboctahedronSGC.getAppearance();
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute(CommonAttributes.PICKABLE, true);
		ap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, true);
		ap.setAttribute("polygonShader."+CommonAttributes.TRANSPARENCY,.8 );
		ap = world.getAppearance();
		ap.setAttribute(CommonAttributes.PICKABLE, true);
		ap.setAttribute("lineShader.tubeRadius", .01);
		ap.setAttribute("lineShader.diffuseColor", Color.white);
		ap.setAttribute("pointShader.pointRadius", .03);
		ap.setAttribute("pointShader.diffuseColor", Color.white);

		cuboctahedronSGC.setGeometry(co);
		initLines();
		return world;
	}

	double a = 0,b= .2, c = .4, d = .6,e=.3,f=.7;
	
	
	private void initLines() {
		int[][] ei = co.getEdgeAttributes(Attribute.INDICES).toIntArrayArray(null);
		double[][] vv = co.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		double[][] lines = new double[ei.length][];
		
		for (int i = 0;i<ei.length; ++i)	{
			lines[i] = PlueckerLineGeometry.lineFromPoints(null, vv[ei[i][0]], vv[ei[i][1]]);
			System.err.println("processing edge "+i);
		}
		
		LineUtility.sceneGraphForCurveOfLines(olinesSGC, lines, null, -1, false);

	}
	private void update() {
		MatrixBuilder.elliptic().translate(a, a, a).assignTo(world);
	}
	
	
	@Override
	public void display() {
		super.display();
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", Color.white);
		SceneGraphComponent cameraNode = CameraUtility.getCameraNode(viewer);
	    FlyTool flytool = new FlyTool();
	    flytool.setGain(.15);
		cameraNode.addTool(flytool);
		CameraUtility.getCamera(viewer).setFar(-1.0);
	}
	@Override
	public Component getInspector() {
		super.getInspector();
		final TextSlider<Double> as = new TextSlider.Double("a",
				SwingConstants.HORIZONTAL, -1, 1, a);
		as.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				a = as.getValue();
				update();
			}
		});
		inspector.add(as);
		
		final TextSlider<Double> bs  = new TextSlider.Double("b",
				SwingConstants.HORIZONTAL, -1, 1, b);
		bs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				b = bs.getValue();
				update();
			}
		});
		inspector.add(bs);
		
		final TextSlider<Double> cs  = new TextSlider.Double("c",
				SwingConstants.HORIZONTAL, -1, 1, c);
		cs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				c = cs.getValue();
				update();
			}
		});
		inspector.add(cs);
		
		final TextSlider<Double> ds  = new TextSlider.Double("d",
				SwingConstants.HORIZONTAL, -1, 1, d);
		ds.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				d = ds.getValue();
				update();
			}
		});
		inspector.add(ds);
		
		
		final TextSlider<Double> es  = new TextSlider.Double("e",
				SwingConstants.HORIZONTAL, -1,1, e);
		es.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				e = es.getValue();
				update();
			}
		});
		inspector.add(es);
		
		final TextSlider<Double> fs  = new TextSlider.Double("f",
				SwingConstants.HORIZONTAL, -1,1, f);
		fs.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				f = fs.getValue();
				update();
			}
		});
		inspector.add(fs);
		
		return inspector;
	}


	public static void main(String[] args) {
		new Cuboctahedron().display();
	}

	

}
