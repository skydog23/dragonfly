/*
 * Created on 25.01.2018
 *
 */
package charlesgunn.jreality.worlds.iscador;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.jreality.geometry.projective.PointCollector;
import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.jogl3.shader.PolygonShader;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.DataList;
import de.jreality.scene.data.StorageModel;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.EffectiveAppearance;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class ProjectiveConicsDemo extends Assignment {
	double param = 0;
	SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
	SceneGraphComponent w1 = SceneGraphUtility.createFullSceneGraphComponent("world1");
	SceneGraphComponent w2 = SceneGraphUtility.createFullSceneGraphComponent("world2");
	double[][] verts;
	int nn = 500;
	IndexedLineSet circle = IndexedLineSetUtility.circle(nn);
	PointCollector pc = new PointCollector(nn+10, 4);
	DataList originalEdges = circle.getEdgeAttributes(Attribute.INDICES);
	Color curveColor = new Color(60, 60, 60);

	@Override
	public SceneGraphComponent getContent() {
		world.addChildren(w1); // w2);
		verts = circle.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		w1.setGeometry(circle);
		Appearance ap = w1.getAppearance();

		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ap.setAttribute(CommonAttributes.SPHERES_DRAW, false);
		ap.setAttribute(CommonAttributes.TUBE_RADIUS, .01);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
//		ap.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
//		
//		ap.setAttribute(CommonAttributes.DIFFUSE_COLOR, Color.white);
//		ap.setAttribute("pointShader.diffuseColor", Color.white);
		ap.setAttribute("lineShader.polygonShader.diffuseColor", curveColor);
		ap.setAttribute("polygonShader.diffuseColor", curveColor);
		ap.setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.unitCube);
		
		
		return world;
	}

	public Component getInspector() {
		Box inspectionPanel =  inspector;
		final TextSlider nSlider = new TextSlider.Double("param",SwingConstants.HORIZONTAL, 0, 2, param);
		nSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				param = nSlider.getValue().doubleValue();
				update();
			}
		});
		inspectionPanel.add(nSlider);
		return inspector;
	}
	protected void update() {
		param = 2 - param;
		double realP = (param < 1) ? param : (1.0/(2.0001-param));
		double[] m = Rn.identityMatrix(4);
		m[13] = -realP;
		double[][] tverts = Rn.matrixTimesVector(null, m, verts);
		tverts = Pn.dehomogenize(tverts, tverts);
		circle = IndexedLineSetUtility.circle(nn);
		w1.setGeometry(circle);
//		w2.setGeometry(circle);

		circle.setVertexAttributes(Attribute.COORDINATES, StorageModel.DOUBLE_ARRAY_ARRAY.createReadOnly(tverts));
//		circle.setEdgeAttributes(Attribute.INDICES, originalEdges);
		IndexedLineSetUtility.removeInfinity(circle, 15);
	}

	@Override
	public void setValueAtTime(double d) {
		// TODO Auto-generated method stub
		super.setValueAtTime(d);
		param = 2 * d;
		update();
	}

	@Override
	public void display() {
		// TODO Auto-generated method stub
		super.display();
		jrviewer.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(255, 192, 96));
	}

	public static void main(String[] args) {
		new ProjectiveConicsDemo().display();
	}

}
