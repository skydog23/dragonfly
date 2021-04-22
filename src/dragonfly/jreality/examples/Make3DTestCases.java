/*
 * Created on Aug 23, 2004
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JMenuBar;

import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import charlesgunn.jreality.geometry.ParametrizedDiamondSurfaceFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.geometry.ThickenedSurfaceFactory;
import de.jreality.math.MatrixBuilder;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;



/**
 * @author gunn
 *
 */
public class Make3DTestCases extends Assignment {

	int refineLevel = 0, steps = 6;
	double holeSize = .2, thickness = .03;
	double holeSizeMin = .2, holeSizeMax = .5,
		thicknessMin = .025, thicknessMax = .1;
	double[][] holethick = {{.2,.04}, {.3, .025}, {.4, .02}};
	int isteps = 3, jsteps = 1;
	double unitTrans = 2.2;
	boolean thicken = true, 
		makeHoles = true, 
		linearHole = true, 
		curvedEdges = true,
		diamondize = false,
		textureize = false;
	boolean doDiamond = false,
		doLabels = true;
	private double[][] profile = new double[][]{{0,0},{0, .5}, {.333,1},{.666, 1},{1, .5},{1,0}};
	double R = .6666;
	double r = .3333;
	IndexedFaceSet surface, thickSurface;
	ThickenedSurfaceFactory tsf, tsf2;
	String info = new Date().toString()+
		"\ndragonfly.examples.Make3DTestCase "+
		"\nthicken\t"+thicken+
		"\nmakeHoles\t"+makeHoles+
		"\nlinearHole\t"+linearHole+
		"\ncurvedEdges\t"+curvedEdges+
		"\ntdiamondize\t"+diamondize+
		"\ntextureize\t"+textureize+
		"\nrefineLevel\t"+refineLevel+
		"\nsteps\t"+steps+
		"\ntholeSizeMin\t"+holeSizeMin+
		"\nholeSizeMax\t"+holeSizeMax+
		"\nthicknessMin\t"+thicknessMin+
		"\ntthicknessMax\t"+thicknessMax+
		"\nisteps\t"+isteps+
		"\njsteps\t"+jsteps;
	IndexedFaceSet[] surfaces = new IndexedFaceSet[4];
	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent root = SceneGraphUtility.createFullSceneGraphComponent();
		root.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, false);
		root.getAppearance().setAttribute("polygonShader.diffuseColor", new Color(255, 255, 200));
		root.getAppearance().setAttribute(CommonAttributes.IGNORE_ALPHA0, false);
		ParametricSurfaceFactory foo = getTorus(23, 11);
		foo.update();
		surfaces[0] = foo.getIndexedFaceSet();
		foo = getTorus(46, 22);
		foo.update();
		surfaces[1] = foo.getIndexedFaceSet();
		foo = getTorus(18, 9);
		foo.update();
		ParametrizedDiamondSurfaceFactory pdsf = new ParametrizedDiamondSurfaceFactory(foo);
		pdsf.update();
		surfaces[2] = pdsf.getIndexedFaceSet();
		foo = getTorus(36, 18);
		foo.update();
		pdsf = new ParametrizedDiamondSurfaceFactory(foo);
		pdsf.update();
		surfaces[3] = pdsf.getIndexedFaceSet();
		ArrayList<SceneGraphComponent> al = new ArrayList<SceneGraphComponent>();
		for (int k = 0; k<4; ++k)	{
			for (int i = 0; i<isteps; ++i)	{
//		holeSize = isteps == 1 ? holeSizeMin : (holeSizeMin+ i*(holeSizeMax - holeSizeMin)/(isteps-1.0));
				holeSize = holethick[i][0];
				thickness = holethick[i][1];
				for (int j = 0; j<jsteps; ++j)	{
//			thickness =  jsteps == 1 ? thicknessMin :(thicknessMin+ j*(thicknessMax - thicknessMin)/(jsteps-1.0));
					tsf = new ThickenedSurfaceFactory(surfaces[k]);
					tsf.setMakeHoles(makeHoles);
					tsf.setLinearHole(linearHole);
					tsf.setCurvedEdges(curvedEdges);
					tsf.setStepsPerEdge(steps);
					tsf.setProfileCurve(profile);
					tsf.setThickness(thickness);
					tsf.setHoleFactor(holeSize);
					tsf.update();
					thickSurface = tsf.getThickenedSurface();
					SceneGraphComponent sgc = new SceneGraphComponent();
					sgc.setGeometry(thickSurface); //Primitives.coloredCube());
					if (doLabels)	{
						SceneGraphComponent label = GeometryUtilityOverflow.boxedSignFromString("__19.03 #"+i+j+"__", .1, .1, null);
						MatrixBuilder.euclidean(label.getTransformation()).translate(0,0,0).scale(.8).rotateX(Math.PI).assignTo(label);
						sgc.addChild(label);					
					}
//					MatrixBuilder.euclidean().translate(i*unitTrans, j*unitTrans,0).assignTo(sgc);
					al.add(sgc);
					root.addChild(sgc);
				}
			}
			
		}
		for (int i = 0; i<al.size() && i < 12; ++i)	{
			SceneGraphComponent sgc = al.get(i);
			int x = i%3;
			int y = i/3;
			MatrixBuilder.euclidean().translate((x+3)*unitTrans, y*unitTrans,0).assignTo(sgc);	
		}
		root = SceneGraphUtility.flatten(root);
		System.err.println(info);
		return root;
	}
	
	private ParametricSurfaceFactory getTorus(int un, int vn)	{
		 ParametricSurfaceFactory foo = new ParametricSurfaceFactory();
		 foo.setImmersion(new ParametricSurfaceFactory.Immersion() {
					public int getDimensionOfAmbientSpace() {
						return 3;
					}

					public void evaluate(double u, double v, double[] xyz,
							int offset) {
						xyz[0] = Math.cos(u) * (R + r * Math.cos(v));
						xyz[1] = Math.sin(u) * (R + r * Math.cos(v));
						xyz[2] = r * Math.sin(v);				
					}

					public boolean isImmutable() {
						return true;
					}
				}

		);
		 foo.setUMin(0);
		 foo.setVMin(0);
		foo.setUMax(2*Math.PI);
		foo.setVMax(2*Math.PI);
		foo.setClosedInUDirection(true);
		foo.setClosedInVDirection(true);
		foo.setULineCount(un);
		foo.setVLineCount(vn);
		foo.setGenerateVertexNormals(true);
		foo.setGenerateFaceNormals(true);
		foo.setGenerateEdgesFromFaces(true);
		return foo;
	}
	@Override
	public void display() {
		super.display();
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.INFO_STRING, info);
	}
	
	public static void main(String[] args) {
		new Make3DTestCases().display();
	}
 }
