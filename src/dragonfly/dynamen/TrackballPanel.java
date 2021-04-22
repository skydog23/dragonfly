package dragonfly.dynamen;

import java.awt.Component;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import de.jreality.geometry.TubeFactory;
import de.jreality.math.Matrix;
import de.jreality.math.Rn;
import de.jreality.plugin.JRViewer;
import de.jreality.plugin.JRViewer.ContentType;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Transformation;
import de.jreality.scene.event.TransformationEvent;
import de.jreality.scene.event.TransformationListener;
import de.jreality.scene.tool.Tool;
import de.jreality.tools.DraggingTool;
import de.jreality.tools.RotateTool;
import de.jreality.tools.TrackballRotateTool;
import de.jreality.ui.viewerapp.ViewerApp;

public class TrackballPanel {

	Component comp = null;
	private SceneGraphComponent sgc;
	private SceneGraphComponent axes;
//    ViewerApp viewerApp;
	
	public TrackballPanel()	{
		sgc = new SceneGraphComponent();
		axes = TubeFactory.getXYZAxes();
		sgc.addChild(axes);
		TrackballRotateTool trt = new TrackballRotateTool();
		axes.addChild(trt.getTrackball());
		axes.addTool(trt);
		axes.setTransformation(new Transformation());
		axes.getTransformation().addTransformationListener(new TransformationListener()	{
			
			double[] themat = new double[16];
			public void transformationMatrixChanged(TransformationEvent ev) {
				ev.getMatrix(themat);
				Matrix m = new Matrix(themat);
				System.err.println("current matrix is "+Rn.matrixToString(m.getArray()));
			}

			
		});

//		viewerApp = new ViewerApp(sgc);
//		CameraUtility.encompass(viewerApp.getCurrentViewer());
//       comp = viewerApp.getViewingComponent();
       JRViewer jrv = new JRViewer();
       jrv.addContentSupport(ContentType.Raw);
       jrv.setContent(sgc);
       jrv.encompassEuclidean();
       jrv.startupLocal();
       comp = (Component) jrv.getViewer().getViewingComponent();
	   comp.setMinimumSize(new Dimension(220, 300));
       comp.setPreferredSize(new Dimension(300, 300));
       comp.setMaximumSize(new java.awt.Dimension(32768,32768));
       comp.setMinimumSize(new java.awt.Dimension(10,10));
 	}
	public void removeStandardTools() {
//		SceneGraphComponent toolSGC = viewerApp.getJrScene().getPath("emptyPickPath").getLastComponent();
//		List<Tool> tools = toolSGC.getTools(), toRemove = new ArrayList<Tool>();
//		for (Tool t: tools)	{
//			if (t instanceof RotateTool || t instanceof DraggingTool) toRemove.add(t);
//		}
//		for (Tool t: toRemove)	{
//			toolSGC.removeTool(t);
//			System.err.println("Removing tool");
//		}
//		viewerApp.getJrScene().addPath("emptyPickPath", new SceneGraphPath(viewerApp.getSceneRoot()));
	}
	public Component getComponent()	{
		return comp;
	}
	
	public SceneGraphComponent getSceneGraphComponent() {
		return axes;
	}

}
