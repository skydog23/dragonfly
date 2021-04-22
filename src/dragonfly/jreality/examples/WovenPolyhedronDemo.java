/*
 * Created on Jan 29, 2004
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;

import charlesgunn.jreality.geometry.BezierCurve;
import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import charlesgunn.jreality.texture.RopeTextureFactory;
import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.examples.CatenoidHelicoid;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.SphereUtility;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.Scene;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.tutorial.util.TextSlider;
import de.jreality.util.Input;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.groups.ArchimedeanSolids;
import de.jtem.discretegroup.util.WingedEdge;
import de.jtem.discretegroup.util.WingedEdgeUtility;
import de.jtem.discretegroup.util.WingedEdge.Edge;
import dragonfly.jreality.geometry.WovenQuadSetFactory;


public class WovenPolyhedronDemo extends LoadableScene {
	SceneGraphComponent icokit;
	boolean tryFlatten = true;
	private WovenQuadSetFactory wovenquadmeshfactory;

		double standardSlope = 0.0;
		boolean debug = false;
		private SceneGraphComponent geometrysgc;
		IndexedFaceSet originalQuadSet = null;
		private SceneGraphComponent originalSGC;
		private TextSlider radiusScaleSlider;
		Appearance textureAp = new Appearance("texture"), 
			noTextureAp = new Appearance("no texture");
		private Appearance[] apps = new Appearance[6];
		private Appearance[] texapps = new Appearance[6], currentAps = apps;
		Hashtable<String, IndexedFaceSet> examples = new Hashtable<String, IndexedFaceSet>();
	
		@Override
		public SceneGraphComponent makeWorld()	{
			SceneGraphComponent theWorld = SceneGraphUtility.createFullSceneGraphComponent("world");
			theWorld.getAppearance().setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR,	Color.white);
			geometrysgc = SceneGraphUtility.createFullSceneGraphComponent("geometry");
			originalSGC = SceneGraphUtility.createFullSceneGraphComponent();
			originalSGC.getAppearance().setAttribute("polygonShader.transparency", .15);
			originalSGC.getAppearance().setAttribute("transparencyEnabled", true);
			originalSGC.getAppearance().setAttribute("lineShader.polygonShader.diffuseColor", Color.white);
			originalSGC.getAppearance().setAttribute("pointShader.polygonShader.diffuseColor", Color.white);
			originalSGC.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, true);
			originalSGC.getAppearance().setAttribute(CommonAttributes.FACE_DRAW, false);
			theWorld.addChild(geometrysgc);
			theWorld.addChild(originalSGC);

			RopeTextureFactory textureFactory = new RopeTextureFactory(textureAp);
			geometrysgc.setAppearance(noTextureAp);
		    Color[] colors = {
		    		new Color(255,55,0), new Color(100,255,0), new Color(50,50,255),
		    		new Color(255,200,0), new Color(255,0,255), new Color(0,255,255)};
		    Color[] colors2 = {
		    		new Color(255,55,20), new Color(100,255,20), new Color(50,50,255),
		    		new Color(255,200,20), new Color(255,20,255), new Color(20,255,255)};
		    Color[] brightcolors = new Color[6];
		    for (int i = 0; i<6; ++i)	{
		    	float[] rgb = colors2[i].getRGBColorComponents(null);
		    	for (int j = 0; j<3; ++j) rgb[j] = (float) Math.pow(rgb[j], .4);
		    	brightcolors[i] = new Color(rgb[0], rgb[1], rgb[2]);
		    	apps[i] = new Appearance();
		    	apps[i].setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, brightcolors[i]);
		    	texapps[i] = new Appearance();
				textureFactory = new RopeTextureFactory(texapps[i]);
				textureFactory.setN(Math.sqrt(2)*3);
				textureFactory.setM(Math.sqrt(2)*3);
				textureFactory.setAngle(Math.PI/4);
				textureFactory.setBand1color(brightcolors[i]);
				textureFactory.setGapcolor(new Color(100,100,100,255));
				textureFactory.setBand2color(new Color(255,255,200));
				textureFactory.setBandwidth(.9);
				textureFactory.setShadowwidth(.1);
				textureFactory.update();
		    }
		    examples.put("Cube", Primitives.coloredCube());
		    examples.put("Rhombic dodecahedron", ArchimedeanSolids.archimedeanSolid("3.4.3.4").polarize());
		    examples.put("Triacontahedron", ArchimedeanSolids.archimedeanSolid("3.5.3.5").polarize());
			examples.put("Diamond Checkerboard", GeometryUtilityOverflow.diamondize(GeometryUtilityOverflow.plainQuadMesh(2,2,5,6)));
		    IndexedFaceSet ifs = Primitives.torus(2.3, 1.5,5,5);
		    GeometryUtilityOverflow.removeBoundaryDuplicates(ifs);
			examples.put("Torus", ifs);
		   	ifs = GeometryUtilityOverflow.diamondize(ifs);
			examples.put("Diamond torus", ifs);
		     ifs = Primitives.torus(2.3, 1.5, 12, 12);
		    GeometryUtilityOverflow.removeBoundaryDuplicates(ifs);
			examples.put("Hires Torus", ifs);
		   	ifs = GeometryUtilityOverflow.diamondize(ifs);
			examples.put("Hires Diamond torus", ifs);
		   	examples.put("Helicoid", new CatenoidHelicoid(8));

			ifs = SphereUtility.tessellatedIcosahedronSphere(1); //Primitives.icosahedron();
			WingedEdge we = WingedEdgeUtility.convertConvexPolyhedronToWingedEdge(ifs); //new WingedEdge(1.0);
			standardSlope = slopeFor(we.vertexList.get(8).point, we.vertexList.get(9).point);
			Vector<Edge> v = new Vector<Edge>();
			int ne = we.getEdgeAttributes(Attribute.INDICES).toIntArrayArray().getLength();
			int[] special = {65, 7, 44, 79, 113, 41, 100, 6};
			for (int i = 0; i<special.length; ++i)	
				v.add(we.edgeList.get(special[i]));
			for (int i = 0; i<ne; ++i)	{
				Edge e = we.edgeList.get(i);
				double slope2 = slopeFor(e.v0.point, e.v1.point);
				double d = Math.abs( standardSlope - slope2); //e.v0.point[0] - e.v1.point[0]);
				if (d<.3) v.add(e);
			}
			System.err.println("Deleting "+v.size()+" edges");
			for (Edge e: v)
				we.removeEdge(e);
			examples.put("40-gon", we);
			 ParametricSurfaceFactory foo = new ParametricSurfaceFactory();
			 foo.setImmersion(new ParametricSurfaceFactory.Immersion() {
						double a = 1;

						double b = 1;

						public int getDimensionOfAmbientSpace() {
							return 3;
						}

						public void evaluate(double u, double v, double[] xyz,
								int offset) {
//							xyz[0] = Math.cos(u) * (R + r * Math.cos(v));
//							xyz[2] = Math.sin(u) * (R + r * Math.cos(v));
//							xyz[1] = r * Math.sin(v);
							xyz[0] = Math.cos(u) * Pn.cosh(b*v);
							xyz[1] = Math.sin(u) * Pn.cosh(b*v);
							xyz[2] = a*Pn.sinh(b*v);
//							xyz[0] = o;
//							xyz[1] = v;
//							xyz[2] = u*u-v*v;
					
						}

						public boolean isImmutable() {
							return true;
						}
					}

			);
			 foo.setUMin(0);
			 foo.setVMin(-2);
			foo.setUMax(2*Math.PI);
			foo.setVMax(2);
			foo.setClosedInUDirection(true);
			foo.setULineCount(15);
			foo.setVLineCount(8);
			foo.setGenerateVertexNormals(true);
			foo.setGenerateFaceNormals(true);
			foo.setGenerateEdgesFromFaces(true);
			foo.update();
			IndexedFaceSet bar = foo.getIndexedFaceSet();
			GeometryUtilityOverflow.removeBoundaryDuplicates(bar);
			examples.put("hyperboid",	bar	   );
			bar = GeometryUtilityOverflow.diamondize(bar);
			examples.put("diamond hyperboid",	bar	   );

			try {
				SceneGraphComponent sgc  = Readers.read(
						Input.getInput("http://www.math.tu-berlin.de/~gunn/Documents/Models/jrs/icosa40-01.jrs")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
				Geometry g = SceneGraphUtility.getFirstGeometry(sgc);
				examples.put("icosa 40 v. 2",(IndexedFaceSet) g);
				sgc  = Readers.read(
						Input.getInput("http://www.math.tu-berlin.de/~gunn/Documents/Models/jrs/icosa40-02.jrs")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
				g = SceneGraphUtility.getFirstGeometry(sgc);
				examples.put("icosa 40 v. 3",(IndexedFaceSet) g);
				sgc  = Readers.read(
						Input.getInput("http://www.math.tu-berlin.de/~gunn/Documents/Models/jrs/icosa40-03-hex.jrs")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
				g = SceneGraphUtility.getFirstGeometry(sgc);
				examples.put("icosa 40 v. 4",(IndexedFaceSet) g);
				sgc  = Readers.read(
						Input.getInput("http://www.math.tu-berlin.de/~gunn/Documents/Models/jrs/icosa40-04-odd.jrs")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
				g = SceneGraphUtility.getFirstGeometry(sgc);
				examples.put("icosa 40 v. 5",(IndexedFaceSet) g);
				sgc  = Readers.read(
						Input.getInput("http://www.math.tu-berlin.de/~gunn/Documents/Models/jrs/icosa40-05-odd.jrs")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
				g = SceneGraphUtility.getFirstGeometry(sgc);
				examples.put("icosa 40 v. 6",(IndexedFaceSet) g);
				Input.getInput("/Users/gunn/Documents/Models/jrs/icosa40-08.jrs"); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
				g = SceneGraphUtility.getFirstGeometry(sgc);
				examples.put("icosa 40 v. 8",(IndexedFaceSet) g);
//				sgc  = Readers.read(
//						Input.getInput("/homes/geometer/gunn/Documents/Models/OBJ/s245.obj")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
//				g = SceneGraphUtility.getFirstGeometry(sgc);
//				examples.put("s245",(IndexedFaceSet) g);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			MatrixBuilder.euclidean().rotateX(Math.PI/3).assignTo(theWorld);
			replaceExample("Cube");
			
			return theWorld;
		}

		BezierCurve linearTest = new BezierCurve(1, new double[][]{{1,1,0},{-1,1,0},{-1,-1,0},{1,-1,0},{1,1,0}});
		private Component factoryinspector;
		private Box inspectionPanel;
		private void replaceExample(String name) {
			
			if (examples.get(name) == null) 
				throw new IllegalStateException("No such thing named "+name);
			originalQuadSet = examples.get(name);
		   	wovenquadmeshfactory = new WovenQuadSetFactory(originalQuadSet, wovenquadmeshfactory);
			wovenquadmeshfactory.setAppearances(currentAps);
			wovenquadmeshfactory.update();
			
			SceneGraphComponent test = wovenquadmeshfactory.getSceneGraphComponent();
			SceneGraphUtility.removeChildren(geometrysgc);
			geometrysgc.addChild(test);
			originalSGC.setGeometry(originalQuadSet);
		}

		private void updateInspector() {
			if (factoryinspector != null && inspectionPanel != null) 
				inspectionPanel.remove(factoryinspector);
			factoryinspector = wovenquadmeshfactory.getInspector();
			if (inspectionPanel != null) 
				inspectionPanel.add(factoryinspector);
			inspectionPanel.validate();
		}
	
		private double slopeFor(double[] point, double[] point2) {
			double dx = point[0] - point2[0];
			double dz = point[2] - point2[2];
			double dy = point[1] - point2[1];
			if (Math.abs(dx) < 10E-8) {
				if (Math.abs(dz) > 10E-8 && Math.abs(dy) < 10E-8) return standardSlope;
				return 10E16;
			}
			return dy/dx;
		}
		@Override
		public boolean isEncompass() {
			return true;
		}

		@Override
		public void customize(JMenuBar menuBar, final Viewer viewer) {
			JMenu testM = new JMenu("Examples");
			ButtonGroup bg = new ButtonGroup();
			Set<String> gnames = examples.keySet();
			for (final String name : gnames)	{
				JMenuItem jm = testM.add(new JRadioButtonMenuItem(name));
				jm.addActionListener( new ActionListener() {
					public void actionPerformed(ActionEvent e)	{
						replaceExample(name);
						updateInspector();
					}
				});
				bg.add(jm);
			}
			menuBar.add(testM,0);
			viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(8,0,20));
			((Component) viewer.getViewingComponent()).addKeyListener( new KeyAdapter()	{

				@Override
				public void keyPressed(KeyEvent e)	{ 
					switch(e.getKeyCode())	{
						
					case KeyEvent.VK_H:
						System.out.println("	0:  toggle texture map");
						System.out.println("	1:  toggle visibility");
						System.out.println("	2:  increase/decrease refine level");
						System.out.println("	3:  increase/decrease squash factor");
						System.out.println("	4:  increase/decrease spline tension");
						System.out.println("	5:  increase/decrease boundary boost");
						System.out.println("	6:  increase/decrease radius scale");
						System.out.println("	7:  toggle appearances");
						break;
		
					case KeyEvent.VK_0:
						Scene.executeWriter( geometrysgc, new Runnable() {
							
							public void run() {
								System.err.println("Appearance is "+geometrysgc.getAppearance());
								if (geometrysgc.getAppearance() == textureAp) geometrysgc.setAppearance(noTextureAp);
								else geometrysgc.setAppearance(textureAp);
							}
						}
						);
						viewer.render();
						break;
						
					case KeyEvent.VK_1:
						originalSGC.setVisible(!originalSGC.isVisible());
						viewer.render();
						break;
						
					case KeyEvent.VK_7:
						currentAps = wovenquadmeshfactory.getAppearances() == apps ? texapps : apps;
						wovenquadmeshfactory.setAppearances(currentAps);
//						wovenquadmeshfactory.update();
						viewer.render();
						break;
						
					}

				}

			});
		}
		@Override
		public boolean hasInspector() {return true; }
		@Override
		public Component getInspector(Viewer viewer) {	
			inspectionPanel = Box.createVerticalBox();
			Box hbox = Box.createHorizontalBox();
			JCheckBox directionButton = new JCheckBox("debug", debug);
			directionButton.addActionListener( new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					debug = ((JCheckBox) e.getSource()).isSelected();
					wovenquadmeshfactory.setDebug(debug);
					geometrysgc.setAppearance( debug ?  noTextureAp : textureAp );
					wovenquadmeshfactory.update();
				}
			});
			hbox.add(directionButton);

			JCheckBox tubesCB = new JCheckBox("show tubes", true);
			tubesCB.addActionListener( new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					boolean foo = ((JCheckBox) e.getSource()).isSelected();
					wovenquadmeshfactory.setShowTubes(foo);
				}
			});
			hbox.add(tubesCB);


			JCheckBox xsecCB = new JCheckBox("linear x-sec", false);
			xsecCB.addActionListener( new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					boolean foo = ((JCheckBox) e.getSource()).isSelected();
					wovenquadmeshfactory.setCrossSection(foo ?  linearTest : null);
					wovenquadmeshfactory.update();
					}
			});
			hbox.add(xsecCB);

			inspectionPanel.add(hbox);
			updateInspector();
			return inspectionPanel;
		}
		
	
}
