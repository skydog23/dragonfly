package dragonfly.jreality.examples;

import java.awt.Color;

import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import de.jreality.bullet.JRBulletUtility;
import de.jreality.bullet.PhysicalWorld;
import de.jreality.bullet.plugin.ApplyImpulse;
import de.jreality.bullet.plugin.MoveBody;
import de.jreality.bullet.plugin.Physics;
import de.jreality.bullet.plugin.ProbeBodies;
import de.jreality.bullet.plugin.image.ImageHook;
import de.jreality.math.MatrixBuilder;
import de.jreality.plugin.JRViewer;
import de.jreality.plugin.basic.View;
import de.jreality.plugin.experimental.ViewerKeyListenerPlugin;
import de.jreality.plugin.scene.Avatar;
import de.jreality.renderman.shader.SLShader;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.CameraUtility;
import de.jreality.util.SceneGraphUtility;
import de.jtem.jrworkspace.plugin.Controller;
import de.jtem.jrworkspace.plugin.Plugin;
import de.jtem.jrworkspace.plugin.PluginInfo;
import discreteGroup.demo.ArchimedeanSolidsDemo;


/**
 * Demonstration of construction of physical objects from indexed face sets, including support
 * for transformations along paths.
 * 
 * @author brinkman
 */
public class ArchieFallsDemo extends Plugin {

	private SceneGraphComponent root2;
	private static Viewer viewer;

	@Override
	public PluginInfo getPluginInfo() {
		PluginInfo info = new PluginInfo();
		info.name = "Archie Falls Demo";
		info.vendorName = "Charles Gunn"; 
		info.icon = ImageHook.getIcon("radioactive1.png");
		return info;
	}

	@Override
	public void install(Controller c) throws Exception {
		Physics physics =  c.getPlugin(Physics.class);
		final PhysicalWorld pw = physics.getPhysicalWorld();
		SceneGraphComponent root = pw.getPhysicsRoot();
		root2 = SceneGraphUtility.createFullSceneGraphComponent("real root");
		SLShader sls = new SLShader("ambientOcclusion2");
//		sls.addParameter("maxvariation", new Float(.05));
//		root2.getAppearance().setAttribute(RMAN_SURFACE_SHADER, sls);
//		IndexedFaceSet ico = Primitives.sharedIcosahedron;
		SceneGraphComponent worldSolids = new SceneGraphComponent("world solids");
		ArchimedeanSolidsDemo acd = new ArchimedeanSolidsDemo();
		//acd.makeWorld();
		worldSolids =acd.makeWorld(); // acd.getPlatonic(); //
		SceneGraphComponent worldPolar = new SceneGraphComponent("world polars");
		acd = new ArchimedeanSolidsDemo();
		acd.showPolars = true; acd.showSolids = false; acd.transpPolars = false;
		acd.pap.setAttribute(CommonAttributes.TRANSPARENCY_ENABLED, false);
		worldPolar = acd.makeWorld();
		
		SceneGraphComponent platos;
		acd = new ArchimedeanSolidsDemo();
		acd.makeWorld();
		platos =acd.getPlatonic(); //

		// create profile curve for bowl
		int res = 50;
		double[][] profile = new double[res][3];
		for (int i = 0; i<res; ++i)	{
			profile[i][1] = i/(res-1.0);
			profile[i][0] = profile[i][1]*profile[i][1];
			profile[i][2] = 0;
		}
		IndexedFaceSet bowl = GeometryUtilityOverflow.surfaceOfRevolutionAsIFS(profile, res, Math.PI*2);
		SceneGraphComponent bowlSGC = SceneGraphUtility.createFullSceneGraphComponent("bowl");
		SceneGraphComponent bowlSGC2 = SceneGraphUtility.createFullSceneGraphComponent("bowl2");
		SceneGraphComponent bowlSolid = new SceneGraphComponent("bowl solid"),
			bowlPolar  = new SceneGraphComponent("bowl polar"), 
			bowlPlato = new SceneGraphComponent("bowl plato");
		bowlSolid.addChild(bowlSGC);
		bowlSolid.addChild(worldSolids);
		bowlPolar.addChild(bowlSGC);
		bowlPolar.addChild(worldPolar);
		bowlPlato.addChild(bowlSGC);
		bowlPlato.addChild(platos);
		bowlSGC2.setGeometry(bowl);
		// this is now consistent with prman paraboloid
		MatrixBuilder.euclidean().rotateY(Math.PI/2).assignTo(bowlSGC2);
		double gscale = 1.4, rscale = 1.0, zscale = .6;
		String rmanbowl = "TransformBegin\nRotate -90 1 0 0\n"+
			String.format("Paraboloid %g %g %g 360\n", (Double) (gscale*rscale), 0.0, (Double) (gscale*zscale))+
			"TransformEnd";
		bowlSGC2.getAppearance().setAttribute(CommonAttributes.RMAN_PROXY_COMMAND, rmanbowl );
		bowlSGC2.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);
		bowlSGC2.getAppearance().setAttribute("mass", 0.0);
		bowlSGC2.getAppearance().setAttribute(CommonAttributes.DIFFUSE_COLOR, new Color(100,255,100));
	// this stretches the paraboloid
		MatrixBuilder.euclidean().rotateX(Math.PI/2).scale(gscale*rscale, gscale*rscale, gscale*zscale).assignTo(bowlSGC);
		bowlSGC.addChild(bowlSGC2);
//		bowlSGC = JRBulletUtility.addSGCToPhysicalWorld(pw, bowlSGC, 0);
//		root2.addChild(bowlSGC);
		
		MatrixBuilder.euclidean().translate(0,1.5,0).rotateX(1).assignTo(worldSolids);
		MatrixBuilder.euclidean().translate(0,0,0).assignTo(bowlSolid);
		bowlSolid = JRBulletUtility.addSGCToPhysicalWorld(pw, bowlSolid, 5);
		root2.addChild(bowlSolid);
		MatrixBuilder.euclidean().translate(0,1.5,0.0).rotateX(0).assignTo(worldPolar);
		MatrixBuilder.euclidean().translate(3,0,0.0).assignTo(bowlPolar);
		bowlPolar = JRBulletUtility.addSGCToPhysicalWorld(pw, bowlPolar, 5);
		root2.addChild(bowlPolar);
		MatrixBuilder.euclidean().translate(0,1.5,0.0).rotateX(0).assignTo(platos);
		MatrixBuilder.euclidean().translate(1.5,0,2.6).assignTo(bowlPlato);
		bowlPlato = JRBulletUtility.addSGCToPhysicalWorld(pw,bowlPlato, 5);
		root2.addChild(bowlPlato);
		root.addChild(root2);
//		pw.stop();
//		Timer timer = new Timer(40, new ActionListener() {
//
//			public void actionPerformed(ActionEvent e) {
//				Scene.executeWriter(root2, new Runnable() {
//					public void run() {
//						pw.step((float).04);
//					}
//				});
//			}
//			
//		});
//		timer.start();
	}

	public static void main(String[] args) throws InterruptedException {
		JRViewer v = new JRViewer();
		v.addBasicUI();
		v.addVRSupport();
		//v.addAudioSupport();
		v.registerPlugin(new ViewerKeyListenerPlugin());
		v.registerPlugin(new ApplyImpulse());
		v.registerPlugin(new ProbeBodies());
		v.registerPlugin(new MoveBody());
		ArchieFallsDemo p = new ArchieFallsDemo();
		v.registerPlugin(p);

		v.startup();
		MatrixBuilder.euclidean().translate(0,0,5).assignTo(v.getPlugin(Avatar.class).getAvatar());
		v.getPlugin(View.class).getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.RMAN_GLOBAL_INCLUDE_FILE, "quality.rib");
		viewer = v.getPlugin(View.class).getViewer();
		CameraUtility.encompass(viewer, p.root2, true);
//		viewer.getSceneRoot().getAppearance().setAttribute(RMAN_SHADOWS_ENABLED, true);
//		SceneGraphUtility.removeLights(v.getPlugin(View.class).getViewer().getCurrentViewer());
//		CameraUtility.getCameraNode(v.getPlugin(View.class).getViewer()).addChild(GlobalProperties.makeLightsS());

	}
}
