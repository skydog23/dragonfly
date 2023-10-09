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

public class Octahedron extends Assignment {
	
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
			octahedronSGC;

	@Override
	public SceneGraphComponent getContent() {
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		linesSGC = SceneGraphUtility.createFullSceneGraphComponent("lines");
		olinesSGC = SceneGraphUtility.createFullSceneGraphComponent("olines");
		nlinesSGC = SceneGraphUtility.createFullSceneGraphComponent("nlines");
		pointsSGC = SceneGraphUtility.createFullSceneGraphComponent("points");
		octahedronSGC = SceneGraphUtility.createFullSceneGraphComponent("triangles");
		world.addChildren(linesSGC,pointsSGC,octahedronSGC);
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
		ap = octahedronSGC.getAppearance();
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

		octahedronSGC.setGeometry(initOctahedron());
		pointsSGC.setGeometry(initPoints());
		
		
		DragEventTool t = new DragEventTool();
		t.addPointDragListener(new PointDragListener() {

			public void pointDragStart(PointDragEvent e) {
				System.out.println("drag start of vertex no "+e.getIndex());				
			}

			public void pointDragged(PointDragEvent e) {
				PointSet pointSet = e.getPointSet();
				double[][] pts=new double[pointSet.getNumPoints()][];
		        pointSet.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(pts);
		        double[] pos3 = e.getPosition().clone();
		        for (int i = 0; i<3; ++i)	ov[e.getIndex()][i] = pos3[i];
		        ov[e.getIndex()][3] = 1.0;
//		        ov[e.getIndex()]=pos3;
//		        System.err.println(e.getIndex()+" npt = "+Rn.toString(pos3));
//		        pointSet.setVertexAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY_ARRAY.createReadOnly(pts));			
		        update();
			}

			public void pointDragEnd(PointDragEvent e) {
//				System.err.println("points = \n"+Rn.toString(ov));
			}
			
		});
		
		world.addTool(t);

		update();
		return world;
	}

	double a = .8,b= .2, c = .4, d = .6,e=.3,f=.7;
	
	double[][] ov = {
			{1,0,0,1},
			{-1,0,0,1},
			{0,1,0,1},
			{0,-1,0,1},
			{0,0,1,1},
			{0,0,-1,1}
	};
	int[][] ei = {{0,2},{2,4},{4,0},    // top face
			{5,0},{2,5},				// ring of six 
			{1,2},{4,1},
			{3,4},{0,3},
			{1,3},{3,5},{1,5},
	};
	int[] edges31 = {3,5,7},
			edges32 = {4,6,8};
	
	final int[] top = {0,1,2},
			bottom = {9,10,11},
			ring = {3,4,5,6,7,8};
	
	int[][] fi = {
			{0,2,4},
			{2,0,5},  // 05
			{5,1,2},  // 25
			{4,2,1},  // 21
			{1,3,4},  // 41
			{0,4,3},  // 43
			{3,5,0},  // 03
			{1,3,5}
	};
	
	int[][] nbhrpls = {
			{1,3,5},
			{2,4,6}
	};
	double[][] opl  = new double[8][],
			nv = new double[12][];
	
	IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
	PointSetFactory psf = new PointSetFactory();
	
	private void update() {
		ifsf.setVertexCoordinates(ov);
		ifsf.update();
		
		for (int i = 0;i<nv.length; ++i)	{
			int j = ei[i][0], k = ei[i][1];
			nv[i] = AnimationUtility.linearInterpolation(null, .5, 0.0, 1.0, ov[j], ov[k]);
		}
		
		for (int i = 0;i<8; ++i)	{
			int j = fi[i][0], k = fi[i][1], m = fi[i][2];
			opl[i] = PlueckerLineGeometry.planeFromPoints(null, ov[j], ov[k], ov[m]);
		}
		
		double[][] olines = new double[12][],
				nlines = new double[8][];
		
		for (int i = 0; i<olines.length; ++i)	{
			olines[i] = PlueckerLineGeometry.lineFromPoints(null, ov[ei[i][0]], ov[ei[i][1]]);
		}
		int j=top[0], k = top[1], m = top[2];
		nlines[0] = Rn.add(null,
				Rn.times(null, a, olines[j]),
				Rn.add(null, 
						Rn.times(null,b, olines[k]),
						Rn.times(null,c, olines[m])));


		// calculate the three points on the zig-zag edges
		// and the lines joining these with the original three points
		double[] ts = {d,e,f};
		for (int i = 0; i<3; ++i)	{
			nv[i] = PlueckerLineGeometry.lineIntersectPlane(null, nlines[0], opl[nbhrpls[0][i]]);
			int j1 = ei[edges31[i]][0], j2 = ei[edges31[i]][1];
			nv[edges31[i]] = Rn.linearCombination(null, ts[i], ov[j1], 1-ts[i], ov[j2]);
			nlines[i+1] = PlueckerLineGeometry.lineFromPoints(null, nv[i], nv[edges31[i]]);
			nv[edges32[i]] = PlueckerLineGeometry.lineIntersectPlane(null, nlines[i+1], opl[nbhrpls[1][i]]);
		}
		for (int i = 0; i<3; ++i)	{
			nlines[i+4] = PlueckerLineGeometry.lineFromPoints(null, nv[edges31[i]], nv[edges32[(i+2)%3]]);
			nv[9+i] = PlueckerLineGeometry.lineIntersectPlane(null, nlines[i+4], opl[7]);
		}
		nlines[7] = PlueckerLineGeometry.lineFromPoints(null, nv[10], nv[11]);
		LineUtility.sceneGraphForCurveOfLines(olinesSGC, olines, null, 5, true);
		LineUtility.sceneGraphForCurveOfLines(nlinesSGC, nlines, null, 50, true);
		for (int i = 0; i<8; ++i)	{
			Appearance ap = new Appearance();
			ap.setAttribute("lineShader.diffuseColor", fc[redirect[i]]);
			nlinesSGC.getChildComponent(i).setAppearance(ap);
		}
		psf.setVertexCoordinates(nv);
		psf.update();
		

	}
	
	Color[] fc = {Color.darkGray, Color.yellow, Color.blue, Color.green, Color.magenta,
			new Color(255,170,0), Color.cyan, Color.lightGray}; //{c1,c2,c2,c2,c2,c2,c2,c1};
	int[] redirect = {0, 1, 3, 5, 6, 2, 4,7};
	private IndexedFaceSet initOctahedron() {
		ifsf.setVertexCount(ov.length);
		ifsf.setVertexCoordinates(ov);
		ifsf.setEdgeCount(12);
		ifsf.setEdgeIndices(ei);
		ifsf.setFaceCount(fi.length);
		ifsf.setFaceColors(fc);
		ifsf.setFaceIndices(fi);
		ifsf.setGenerateFaceNormals(true);
		ifsf.update();
		return ifsf.getIndexedFaceSet();
	}
	
	private PointSet initPoints() {
		psf.setVertexCount(nv.length);
		return psf.getPointSet();
	}
	
	
	@Override
	public void display() {
		super.display();
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", Color.white);
		SceneGraphComponent cameraNode = CameraUtility.getCameraNode(viewer);
	    FlyTool flytool = new FlyTool();
	    flytool.setGain(.15);
		cameraNode.addTool(flytool);

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
		new Octahedron().display();
	}

	

}
