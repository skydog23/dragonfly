package dragonfly.dynamen.ws08;

import java.awt.Component;
import java.io.IOException;
import java.util.prefs.InvalidPreferencesFormatException;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.vecmath.Vector3f;

import com.bulletphysics.collision.shapes.CapsuleShapeX;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;

import de.jreality.bullet.JRBulletUtility;
import de.jreality.bullet.PhysicalObject;
import de.jreality.bullet.PhysicalWorld;
import de.jreality.bullet.tool.ApplyImpulseTool;
import de.jreality.bullet.tool.MoveBodyTool;
import de.jreality.bullet.tool.PhysicsNavigationTool;
import de.jreality.bullet.tool.ShootTool;
import de.jreality.bulletsound.BulletSound;
import de.jreality.bulletsound.CollisionSound;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.vr.ViewerVR;

public class Sample03PhysicsEngine {
	
	protected PhysicalWorld physicalWorld;
	protected static SceneGraphComponent world;
	protected RigidBody[] bodies;
	protected PhysicalParametersInspector ppi;
	public void init(
			int arraySizeX,
			int arraySizeY,
			int arraySizeZ,
			float boxSizeX,
			float boxSizeY,
			float boxSizeZ,
			CollisionShape colShape,
			SceneGraphComponent sgc
	) {
		// maximum number of objects (and allow user to shoot additional boxes)
		int maxProxies = arraySizeX * arraySizeY * arraySizeZ +1024;
//		System.err.println("shape type = "+colShape.getShapeType());
		
		// make physical world with a ground plane
		  float halfSize = 1000f;
		  float gravity = 9.81f;
		DynamicsWorld dw = JRBulletUtility.createDynamicsWorld(maxProxies, halfSize, gravity);
		physicalWorld = new PhysicalWorld(dw, sgc);
		physicalWorld.setGroundPlane(true);

		// Create boxes
		Transform startTransform = new Transform();
		startTransform.setIdentity();
		float mass = 1f;
		Vector3f localInertia = new Vector3f(0, 0, 0);
		colShape.calculateLocalInertia(mass, localInertia);
		
		float start_x = - (arraySizeX - 1) * boxSizeX;
		float start_y = boxSizeY;
		float start_z = - (arraySizeZ - 1) * boxSizeZ;
		int count = 0;
		bodies = new RigidBody[arraySizeX*arraySizeY*arraySizeZ];
		for (int k = 0; k < arraySizeY; k++) {
			for (int i = 0; i < arraySizeX; i++) {
				for (int j = 0; j < arraySizeZ; j++) {
					startTransform.origin.set(
							2 * boxSizeX * i + start_x,
							2 * boxSizeY * k + start_y,
							2 * boxSizeZ * j + start_z
					);
					DefaultMotionState myMotionState = new DefaultMotionState(startTransform);
					RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(
							mass,
							myMotionState,
							colShape,
							localInertia
					);
					rbInfo.additionalDamping = true;
					RigidBody body = new RigidBody(rbInfo);
					bodies[count] = body;
					CollisionSound sound = null;
					try {
						sound = BulletSound.createSource(
								BulletSound.getDefaultMaterial(),
								1.2 + (-0.5 + Math.random())*.1,
								1
						);
					} catch (Exception e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					physicalWorld.add(new PhysicalObject(body));
					count++;
				}
			}
		}
		ppi = new PhysicalParametersInspector(bodies);
		ppi.setPhysicalParameters();
//		physicalWorld.rememberObjects();
	}

	public PhysicalWorld getPhysicalWorld() {
		return physicalWorld;
	}

	private Component getReadme() {
		JPanel panel = new JPanel();
		panel.setName("ReadMe");
		JTextArea textarea = new JTextArea(10,20);
		textarea.setEditable(false);
		textarea.append("This is a simple demo of\n"+
				"the use of the jBullet physics engine\n"+
				"within the jReality scene graph.\n\n"+
				"You begin with a regular grid of\n"+
				"capsules and you can shoot spheres \n"+
				"at the grid, or grab the individual\n"+
				"cubes and move them.\n\n"+
				"Here's how:\n"+
				"    left mouse drag:    grab an object\n"+
				"    middle mouse click:    shoot a bullet\n"+
				"    'r':    reset grid\n\n"+
				"Use mouse click wheel to zoom in and out.\n"+
				"Arrow keys allow walking on terrain\n"+
				"Shift-cntl-f  toggles fullscreen mode.\n"+
				"Click on the tab 'Scene Graph' to explore structure\n"+
				"\nAuthor: Charles Gunn\n"+
				"    gunn at math.tu-berlin.de\n");
		panel.add(textarea);
		return panel;
	}
	public static void main(String[] args) {
		Sample03PhysicsEngine demo = new Sample03PhysicsEngine();
		demo.doIt();
	}
	
	protected void doIt()	{
		int n=3;
		float boxSize = 1.5f;
		
		world = new SceneGraphComponent();
		init(
				n,n,n,
				boxSize, boxSize/2, boxSize,
				new CapsuleShapeX(boxSize/2f, boxSize), //BoxShape(new Vector3f(boxSize, boxSize, boxSize)), //
				world
		);
		PhysicalWorld physicalWorld = getPhysicalWorld();
		physicalWorld.setSoundEnabled(true);
		
		// add tools
		ShootTool shootTool = new ShootTool(physicalWorld, "DragActivation");
		shootTool.setShootShape(new SphereShape(1.5f)); //BoxShape(new Vector3f(1.5f, 1.5f, 1.5f)));
		shootTool.setMass(2.5);
		world.addTool(shootTool);
		world.addTool(new ApplyImpulseTool(physicalWorld, "Duplication")); // double click right mouse
		world.addTool(new MoveBodyTool(physicalWorld, "RotateActivation"));// left mouse
//		world.addTool(new ResetTool(physicalWorld, "DrawPickActivation")); // R-key

		// make a ViewerVR and replace the navigation tool by one that uses jBullet for picking
		ViewerVR vr = ViewerVR.createDefaultViewerVR(null);
		SceneGraphComponent avatar = vr.getAvatarNode();
		avatar.removeTool(vr.getShipNavigationTool());
		avatar.addTool(new PhysicsNavigationTool(physicalWorld));
		
		// set content, undoing the rotation that ViewerVR applies to content
		vr.setDoAlign(false);
		MatrixBuilder.euclidean().rotateX(Math.PI/2).assignTo(world);
		vr.setContent(world);

		// initialize graphics
		ViewerApp va = vr.initialize();
		try {
			vr.importPreferences(Sample03PhysicsEngine.class.getResourceAsStream("JRBasicDemo.xml"));
		} catch (InvalidPreferencesFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		// moving this here seems to avoid deadlock that sometimes appears
		va.setAttachNavigator(true);
		va.setExternalNavigator(false);
		Component insp = ppi.getInspector();
		insp.setName("Physics");
		va.addAccessory(insp);
	    Component readme = getReadme();
	    va.addAccessory(readme);
	    va.setFirstAccessory(readme);		// make it the selected tab at startup
		va.update();
		va.display();
		
		// start physics
		physicalWorld.start();
		
		// start sound
	}
}