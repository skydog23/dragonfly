/*
 * Created on Aug 2, 2014
 *
 */
package dragonfly;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import de.jreality.geometry.Primitives;
import de.jreality.jogl.JOGLViewer;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.DirectionalLight;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.Light;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.StereoViewer;
import de.jreality.tools.RotateTool;
import de.jreality.toolsystem.ToolSystem;
import de.jreality.util.RenderTrigger;

public class TestStereo {
	public static void main(String[] args) {

        StereoViewer stereoViewer = null;

        SceneGraphComponent rootNode = new SceneGraphComponent("root");
        SceneGraphComponent cameraNode = new SceneGraphComponent("camera");
        SceneGraphComponent geometryNode = new SceneGraphComponent("geometry");
        SceneGraphComponent lightNode = new SceneGraphComponent("light");
        rootNode.setAppearance(new Appearance());
        rootNode.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
        rootNode.getAppearance().setAttribute("lineShader.diffuseColor", Color.white);

        rootNode.addChild(geometryNode);
        rootNode.addChild(cameraNode);
        cameraNode.addChild(lightNode);

        Light dl=new DirectionalLight();
        lightNode.setLight(dl);

        IndexedFaceSet ifs = Primitives.icosahedron();
        geometryNode.setGeometry(ifs);

        RotateTool rotateTool = new RotateTool();
        geometryNode.addTool(rotateTool);

        MatrixBuilder.euclidean().translate(0, 0, 3).assignTo(cameraNode);

        Camera camera = new Camera();
        camera.setStereo(true);
        camera.setEyeSeparation(2*camera.getEyeSeparation());
        cameraNode.setCamera(camera);
        SceneGraphPath camPath = new SceneGraphPath(rootNode, cameraNode);
        camPath.push(camera);

       JOGLViewer viewer = new JOGLViewer();

        viewer.setSceneRoot(rootNode);
        viewer.setCameraPath(camPath);

        viewer.setStereoType(3);
        //viewer.setStereoType(0);

        ToolSystem toolSystem = ToolSystem.toolSystemForViewer(viewer);
        toolSystem.initializeSceneTools();

        JFrame frame = new JFrame();
        frame.setVisible(true);
        frame.setSize(640, 480);
        frame.getContentPane().add((Component) viewer.getViewingComponent());
        frame.validate();
        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent arg0) {
                System.exit(0);
            }
        });

        RenderTrigger rt = new RenderTrigger();
        rt.addSceneGraphComponent(rootNode);
        rt.addViewer(viewer);
    }
}
