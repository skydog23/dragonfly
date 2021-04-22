/*
 * Created on Jan 29, 2004
 *
 */
package dragonfly.dynamen.attic;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.JMenuBar;
import javax.swing.Timer;

import charlesgunn.jreality.geometry.Snake;
import charlesgunn.jreality.tools.ToolManager;
import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.geometry.Primitives;
import de.jreality.math.Quaternion;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.SelectionManagerImpl;
import de.jreality.util.SceneGraphUtility;
import de.jtem.numericalMethods.calculus.odeSolving.Extrap;
import de.jtem.numericalMethods.calculus.odeSolving.ODE;


public class TestRigidBody extends LoadableScene {
	SceneGraphComponent icokit;
	Timer moveit = null;
	private SceneGraphComponent body, world, avCurveSGC, amCurveSGC;
	IndexedLineSet avb = null, amb = null;
	private Extrap extrap;
	private ODE ode;
	private double deltaT = .05;
	private double time = 0;
	private double[] angularVelocity = new double[3];
	int snakeLength = 10000;
	private double[][] avCurvePoints = new double[snakeLength][3];
	Snake avCurve = new Snake(avCurvePoints);
	private double[][] amCurvePoints = new double[snakeLength][3];
	Snake amCurve = new Snake(amCurvePoints);
	double[] initialAngularVelocity = new double[]{0.1,1,0};
	double a = 1, b = 2, c = 3;
	double m1 = b*b+c*c;
	double m2 = a*a+c*c;
	double m3 = a*a+b*b;
	double[] moments = new double[]{m1,m2,m3};
	RigidBodySO3ODE rbode = new RigidBodySO3ODE(new double[]{m1,m2,m3});
	double[] inertiaTensor = Rn.diagonalMatrix(null, new double[] {m1,m2,m3});
	
		@Override
		public SceneGraphComponent makeWorld()	{
			
			world = SceneGraphUtility.createFullSceneGraphComponent("world");
//			world.getAppearance().setAttribute(CommonAttributes.SMOOTH_SHADING, false);
			world.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, true);
			//body=rbode.getSceneGraphRepresentation();
			body = SceneGraphUtility.createFullSceneGraphComponent("body");
			body.setGeometry(Primitives.box(a, b, c, true));
			body.getAppearance().setAttribute("rigidBodyODE", rbode);
			body.getAppearance().setAttribute(CommonAttributes.FACE_DRAW, false);
			world.addChild(body);

			avCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("avCurve");
			avCurveSGC.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, false);
			avCurveSGC.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(0,0,100));
			avCurveSGC.setGeometry(avCurve);
			body.addChild(avCurveSGC);
			
			amCurveSGC = SceneGraphUtility.createFullSceneGraphComponent("amCurve");
			amCurveSGC.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, false);
			amCurveSGC.getAppearance().setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, new Color(100,50,50));
			amCurveSGC.setGeometry(amCurve);
			body.addChild(amCurveSGC);
			
			ode = rbode.getODE();
			extrap = new Extrap(7);
			extrap.setAbsTol(10E-10);
			return world;
		}
	
		@Override
		public boolean isEncompass() {
			return true;
		}

		boolean runMotion = false;
		@Override
		public void customize(JMenuBar menuBar, final Viewer viewer) {
			SceneGraphPath sgp = new SceneGraphPath();
			sgp.push(viewer.getSceneRoot());
			sgp.push(world);
			sgp.push(body);
			SelectionManagerImpl.selectionManagerForViewer(viewer).setSelectionPath(sgp);
			InertialRotateShapeTool irst = new InertialRotateShapeTool();
			ToolManager.toolManagerForViewer(viewer).addTool(irst);
			ToolManager.toolManagerForViewer(viewer).activateTool(irst);
			moveit = new Timer(20, new ActionListener()	{
				Quaternion motion;
				double[] matrix = new double[16];
				int count = 0;
				double[] solution = {1,0,0,0,initialAngularVelocity[0], initialAngularVelocity[1], initialAngularVelocity[2]};
				//System.err.println("i: "+i+"Solution: "+Rn.toString(solution));
				public void actionPerformed(ActionEvent e) {
					extrap.odex(ode, solution, time,time+deltaT);	
					motion = new Quaternion(solution[0],solution[1], solution[2], solution[3]);
					//System.err.println("Motion has norm "+Quaternion.length(motion));
					//Quaternion.normalize(motion, motion);
					//Quaternion.asDouble(solution, motion);
					Quaternion.quaternionToRotationMatrix(matrix, motion);
					body.getTransformation().setMatrix(matrix);
					System.arraycopy(solution, 4, angularVelocity, 0, 3);
					rbode.setAngularVelocity(angularVelocity);
					rbode.update();
				
					System.arraycopy(angularVelocity, 0, avCurvePoints[(count%snakeLength)],0,3);
					int[] snakeinfo = avCurve.getInfo();
					snakeinfo[0] = (count < snakeLength) ? 0 : (count+1)%snakeLength;
					snakeinfo[2] = (count < snakeLength) ? count+1 : snakeLength;
					if (count > 10) avCurve.update();
					int count2 = ((count+1)%snakeLength);
					if (count2 == 0) 
						System.err.println("multiple of snakeLength");
					System.arraycopy(rbode.getAngularMomentum(), 0, amCurvePoints[(count%snakeLength)],0,3);
					snakeinfo = amCurve.getInfo();
					snakeinfo[0] = (count < snakeLength) ? 0 : (count+1)%snakeLength;
					snakeinfo[2] = (count < snakeLength) ? count+1 : snakeLength;
					if (count > 10) amCurve.update();
					count++;
					
					viewer.render();
				}
				
			});
			((Component) viewer.getViewingComponent()).addKeyListener( new KeyAdapter()	{
				@Override
				public void keyPressed(KeyEvent e)	{ 
					switch(e.getKeyCode())	{
						
					case KeyEvent.VK_H:
						break;
		
					case KeyEvent.VK_2:
						runMotion = !runMotion;
						if (runMotion) moveit.start();
						else moveit.stop();
						break;
						
					case KeyEvent.VK_3:
						if (e.isShiftDown()) deltaT = deltaT/1.1;
						else deltaT = deltaT * 1.1;
						break;
						
				}

				}
			});

		}


			
	
}
