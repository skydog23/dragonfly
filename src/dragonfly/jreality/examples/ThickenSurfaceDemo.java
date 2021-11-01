/*
 * Created on Aug 23, 2004
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Hashtable;
import java.util.Set;
import java.util.logging.Level;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingConstants;

import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import charlesgunn.jreality.geometry.ParametrizedDiamondSurfaceFactory;
import charlesgunn.jreality.geometry.ParametrizedTriangleMeshSurfaceFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.GlobalProperties;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.pathcurve.PathCurveUtility;
import charlesgunn.util.TextSlider;
import de.jreality.examples.CatenoidHelicoid;
import de.jreality.geometry.BoundingBoxUtility;
import de.jreality.geometry.GeometryMergeFactory;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.ParametricSurfaceFactory;
import de.jreality.geometry.PolygonalTubeFactory;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.RemoveDuplicateInfo;
import de.jreality.geometry.SphereUtility;
import de.jreality.geometry.ThickenedSurfaceFactory;
import de.jreality.jogl.JOGLConfiguration;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.plugin.basic.ViewMenuBar;
import de.jreality.reader.Readers;
import de.jreality.scene.Appearance;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphNode;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.scene.data.DataList;
import de.jreality.scene.data.StorageModel;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.ImageData;
import de.jreality.shader.Texture2D;
import de.jreality.ui.viewerapp.SelectionManager;
import de.jreality.ui.viewerapp.SelectionManagerImpl;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;
import discreteGroup.imulogo.BorromeanRingsKnot;



/**
 * @author gunn
 *
 */
public class ThickenSurfaceDemo extends Assignment {

	SceneGraphComponent sgc1, root;
	IndexedFaceSet surface, initialSurface, thickSurface;
	Gerbil currentGerbil;
	int refineLevel = 0, steps = 6;
	double holeSize = .5, thickness = .1, shiftAlongNormal = .5, extentScale = 1;
	Rectangle3D currentBound;
	boolean thicken = true, 
		makeHoles = true, 
		linearHole = false, 
		curvedEdges = false,
		diamondize = false,
		textureize = true,
		isFaceColors = true,
		showNormals = false,
		mergeBound = true,
		nonorientable = false,
		onSphere = false, 		// don't use vertex normals; vertex normals are same as vertex coordinates
		flatten = false;
	Object theTexture = null;
//	private double[][] profile = new double[][]{{0,0},{.02, .25}, {.333,.5},{.666, .5},{.98, .25},{1,0}};
	private double[][] profile = new double[][]{{0,0},{0, .25}, {.333,.5},{.666, .5},{1, .25},{1,0}};
	ThickenedSurfaceFactory thickenSurfaceFactory;
	Appearance texap = null, ap = new Appearance();
	Hashtable<String, Gerbil> examples = new Hashtable<String, Gerbil>();
	private TextSlider extentSlider;
	double diamondFactor = .38, oldDiamondFactor;
	private IndexedFaceSet currentSurface;
	@Override
	public SceneGraphComponent getContent() {
//		DragEventTool pointMergerTool = new DragEventTool();
//		pointMergerTool.addPointDragListener(new PointDragListener() {
//			PointDragEvent pickedPt1 = null, pickedPt2 = null;
//			public void pointDragStart(PointDragEvent e) {
//				System.err.println("drag start of vertex no "+e.getIndex());				
//			}
//
//			public void pointDragged(PointDragEvent e) {
//			}
//
//			public void pointDragEnd(PointDragEvent e) {
//				System.err.println("in drag end "+e.getIndex());
//				if (pickedPt1 == null) {pickedPt1 = e; return; }
//				pickedPt2 = e;
//				if (pickedPt1.getPointSet() != pickedPt2.getPointSet()) return;
//				double[] middle = Rn.times(null, .5, Rn.add(null, pickedPt1.getPosition(), pickedPt2.getPosition()));
//				double[][] points=pickedPt1.getPointSet().getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
//				System.err.println("middle = "+Rn.toString(middle));
//		        points[pickedPt1.getIndex()]=middle;
//		        points[pickedPt2.getIndex()]=middle;
//		        pickedPt1.getPointSet().setVertexAttributes(Attribute.COORDINATES,StorageModel.DOUBLE_ARRAY.array(middle.length).createReadOnly(points));			
//				pickedPt1 = pickedPt2 = null;
//			}
//			
//		});
		root = SceneGraphUtility.createFullSceneGraphComponent("root");
		sgc1 = SceneGraphUtility.createFullSceneGraphComponent("sgc1");
		texap = new Appearance();
		texap.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		if (textureize) sgc1.setAppearance(texap);
//		double[] c1 = {0,.3,1, 1}, c2 = {1.0, 0, 0, 1};
		double[] c1 = {1,1, 0,1}, c2 = {.4,1,0, 1};
		double blend = 0.0;
		byte[] im = new byte[128 * 128 * 4];
		int k1 = 50, k2 = 128-50;
		for (int i = 0; i<128; ++i)	{
			for (int j = 0; j< 128; ++j)	{
				int I = 4*(i*128+j);
				if (j <= k1 ) { blend = 1.0; }
				else if (j >= k2) { blend = 0.0; }
				else {
					blend = 1.0-(1.0*(j-k1))/(k2-k1);
				}
				double[] bc = Rn.linearCombination(null, blend, c1, 1.0-blend, c2);
				for (int k=0; k<4; ++k) im[I+k] = (byte) (255 * bc[k]);
//				if (sq < 4096)	
//					{ int blah= (int) (255- Math.floor(Math.abs(sq/16.0)));
//					if (blah == 0) blah = 1;
					
//				}
//				else
//					{im[I] =  im[I+1] = im[I+2] = im[I+3]  = 1;  }
			}
		}
		Texture2D tex2d = (Texture2D) AttributeEntityUtility
		.createAttributeEntity(Texture2D.class, "polygonShader.texture2d", texap, true);	
		theTexture = texap.getAttribute("polygonShader.texture2d");
		ImageData it = new ImageData(im, 128, 128);
		tex2d.setImage(it);
		tex2d.setApplyMode(Texture2D.GL_MODULATE);
		tex2d.setRepeatS(Texture2D.GL_MIRRORED_REPEAT);
		tex2d.setRepeatT(Texture2D.GL_REPEAT);
		Matrix tm = new Matrix();
		if (nonorientable)
			MatrixBuilder.euclidean().scale(2,1,1).scale(-1,1,1).translate(.5,0,0).assignTo(tm);
		tex2d.setTextureMatrix(tm);
		
		 ParametricSurfaceFactory foo = new ParametricSurfaceFactory();
		 foo.setImmersion(new ParametricSurfaceFactory.Immersion() {
					double R = .6666;

					double r = .3333;
					double a =1, b=1;
					public int getDimensionOfAmbientSpace() {
						return 3;
					}

					public void evaluate(double u, double v, double[] xyz,
							int offset) {
						xyz[0] = Math.cos(u) * (R + r * Math.cos(v));
						xyz[2] = Math.sin(u) * (R + r * Math.cos(v));
						xyz[1] = r * Math.sin(v);
//						xyz[0] = u;
//						xyz[1] = v;
//						xyz[2] = u*u-v*v;
				
					}

					public boolean isImmutable() {
						return true;
					}
				}

		);
//		 foo.setUMin(-1);
//		 foo.setVMin(-1);
//		foo.setUMax(1);
//		foo.setVMax(1);
		 foo.setUMin(0);
		 foo.setVMin(0);
		foo.setUMax(2*Math.PI);
		foo.setVMax(2*Math.PI);
		foo.setClosedInUDirection(false);
		foo.setClosedInVDirection(true);
		foo.setULineCount(25);
		foo.setVLineCount(13);
		foo.setGenerateVertexNormals(true);
		foo.setGenerateFaceNormals(true);
		foo.setGenerateEdgesFromFaces(true);
		foo.update();
		IndexedFaceSet bar = foo.getIndexedFaceSet();
		bar = GeometryUtilityOverflow.diamondize(bar);
		examples.put("old diamond torus",new Gerbil(bar));
//		SceneGraphComponent polarform = ArchimedeanSolids.polarForm("*235", "100", .5);
//		GeometryMergeFactory gmf = new GeometryMergeFactory();
//		IndexedFaceSet flat= gmf.mergeGeometrySets(polarform);
//		//IndexedFaceSet flat = (IndexedFaceSet) SceneGraphUtility.getFirstGeometry((polarform));
//		examples.put("polar form", new Gerbil(flat));
		ParametrizedDiamondSurfaceFactory pdsf = new ParametrizedDiamondSurfaceFactory(foo);
		pdsf.update();
		bar = pdsf.getIndexedFaceSet();
		IndexedFaceSet copy = (IndexedFaceSet) RemoveDuplicateInfo.removeDuplicateVertices(bar, 10E-8);
		examples.put("new diamond torus",new Gerbil(copy));
		foo.setULineCount(21);
		foo.setVLineCount(13);
		foo.update();
		ParametrizedTriangleMeshSurfaceFactory ptsf = new ParametrizedTriangleMeshSurfaceFactory(foo);
		ptsf.update();
		bar = ptsf.getIndexedFaceSet();
		copy = (IndexedFaceSet) RemoveDuplicateInfo.removeDuplicateVertices(bar, 10E-8);
		examples.put("new triangle torus",new Gerbil(copy));
		// try another way to get a hyperboloid
		int around1 = 5; //2; //
		int updown1 = 14; //5; //
		double windingNumber = around1/((double)updown1);
		double stickAngle = 2*Math.PI * windingNumber;
		int updown = 11; // 6; //17;//
		int around = 1 + (int) (Math.PI*(updown-1)/stickAngle);
		System.err.println("updown, around: "+updown+" "+around);
		double[][] verts = new double[around*updown][];
		int[][] faces = new int[(around-1)*(updown-2)][4];
		double slope = 2, scale = 2;
		double[] P = {scale,0,0}, V = {0,scale,slope};
		for (int i = 0; i<updown; ++i)	{
//			double t = scale * (i-(updown-1.0)/2.0)/(updown-1.0);
			double angle =(i*stickAngle/(1.0*(updown-1))) - stickAngle/2;
			double t = Math.tan(angle);
			verts[i] = Rn.add(null, P, Rn.times(null, t, V));
		}
		
		for (int i = 1; i<around; ++i)	{
			double angle = ( Math.PI * 2.0 * i)/(around-1.0);
			Matrix m = new Matrix();
			MatrixBuilder.euclidean().rotateZ(angle).assignTo(m);
			for (int j = 0; j<updown; ++j)	{
				verts[i*updown+j] = Rn.matrixTimesVector(null, m.getArray(), verts[j]);
			}
		}
		for (int i = 0; i<around-1; ++i)	{
			for (int j = 0; j<(updown-2); ++j)	{
				int index = i*(updown-2)+j;
				faces[index][0] = j+1+i*updown;
				faces[index][1] = j+2+i*updown;
				faces[index][2] = j+1+((i+1)%around)*updown;
				faces[index][3] = j  +((i+1)%around)*updown;
			}
		}
		IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
		ifsf.setVertexCount(verts.length);
		ifsf.setVertexCoordinates(verts);
		ifsf.setFaceCount(faces.length);
		ifsf.setFaceIndices(faces);
		ifsf.setGenerateEdgesFromFaces(true);
		ifsf.setGenerateFaceNormals(true);
		ifsf.update();
		copy = (IndexedFaceSet) RemoveDuplicateInfo.removeDuplicateVertices(ifsf.getIndexedFaceSet(), 10E-8);
		examples.put("hyperboloid", new Gerbil(copy));
//		tex2d.setTextureMatrix(new Matrix(P3.makeScaleMatrix(null, 5,15,1)));
//		RopeTextureFactory textureFactory = new RopeTextureFactory(ap1);
//		textureFactory.setN(5);
//		textureFactory.setM(5);
//		textureFactory.setBand1color(Color.yellow);
//		textureFactory.setBand2color(Color.blue);
//		textureFactory.update();
		examples.put("tessellated icosa",
				new Gerbil(SphereUtility.tessellatedIcosahedronSphere(refineLevel))); 
//		double[][] pts = {{0,0,0},{0,1,0},{1,0,0}};
		int n = 13;
		verts = new double[n][3];
		double angle = 0, delta = Math.PI * 2 / (n-1);
		for (int j = 0; j<n; ++j) {
			angle = j * delta;
			verts[j][0] = Math.cos(angle);
			verts[j][1] = Math.sin(angle);
		}
		   double[][] mysection = verts;
		   IndexedLineSet torus1 = Primitives.discreteTorusKnot(1,.5, 2, 3, 100);//		   double[][] verts = new double[250][3];
		   PolygonalTubeFactory ptf = new PolygonalTubeFactory(torus1, 0);
		   ptf.setClosed(true);
//		   ptf.setVertexColorsEnabled(true);
		   ptf.setRadius(.37);
		   ptf.setGenerateEdges(true);
		   ptf.setCrossSection(mysection);
		   ptf.setMatchClosedTwist(true);
//		   ptf.setTwists(6);
//		   double[][] vcolors = ils.getVertexAttributes(Attribute.COLORS).toDoubleArrayArray(null);
//		   ptf.setVertexColors(vcolors);
		   ptf.update();
		   examples.put("trefoil knot",new Gerbil(ptf.getTube()));
		   examples.put("helicoid", new Gerbil(new CatenoidHelicoid(10)));
//		surface = IndexedFaceSetUtility.constructPolygon(pts); //Primitives.plainQuadMesh(1, 1, 1, 1);
		   BorromeanRingsKnot.setCounts(10, 3, 5);
		   BorromeanRingsKnot borrCurveFactory = new BorromeanRingsKnot();
		   double[][] optcurve = borrCurveFactory.getCurveDescriptor(1).getCurve();
			PolygonalTubeFactory	logoTubeFactory = new PolygonalTubeFactory(optcurve);
			logoTubeFactory.setCrossSection(verts);
			logoTubeFactory.setGenerateTextureCoordinates(true);
			logoTubeFactory.setArcLengthTextureCoordinates(true);
			logoTubeFactory.setMatchClosedTwist(true);
			logoTubeFactory.setRadius( 0.27058570093415335* .76);
			logoTubeFactory.update();
			System.err.println("Tube has vertex count: "+logoTubeFactory.getTube().getNumPoints());
			surface = logoTubeFactory.getTube();
			GeometryUtilityOverflow.removeBoundaryDuplicates(surface);
			Matrix[] mlist2 = {new Matrix(),
					new Matrix(P3.makeRotationMatrix(null, new double[]{1,1,1}, 2*Math.PI/3)),
					new Matrix(P3.makeRotationMatrix(null, new double[]{1,1,1}, 4*Math.PI/3))
			};
			examples.put("borromean ring",new Gerbil(surface, mlist2));
		try {
			SceneGraphComponent sgc  = Readers.read(
					Input.getInput("/gunn_local/Models/geomview/tope25-ref.oogl")); //OBJ/Genus2_stl/Polyhedron_2_10_20332.jvx"));// 
			Geometry g = SceneGraphUtility.getFirstGeometry(sgc);
			if (g instanceof IndexedFaceSet) surface = (IndexedFaceSet) g;
			examples.put("self-intersecting sphere",new Gerbil(surface));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		examples.put("cube",new Gerbil(Primitives.coloredCube())); // DiscreteGroupUtility.archimedeanSolid("3.4.3.4");//bar; //new CatenoidHelicoid(10); //
		IndexedFaceSet pinecone = (IndexedFaceSet) PathCurveUtility.makeSemiImWorld(0).getGeometry();
		examples.put("pine cone",new Gerbil(pinecone)); // DiscreteGroupUtility.archimedeanSolid("3.4.3.4");//bar; //new CatenoidHelicoid(10); //

		Matrix moo = new Matrix();
		MatrixBuilder.euclidean().rotateZ(Math.PI*(1+1/16.0)).assignTo(moo);
		Matrix[] mlist = {new Matrix(), moo};
		examples.put("moebius band", new Gerbil(getMoebiusBand(), mlist));
		
		currentGerbil = examples.get("moebius band");
		//surface = (examples.get("cube").surface);
//		root.addTool(pointMergerTool);
//		root.addChild(sgc1);
		root2 = SceneGraphUtility.createFullSceneGraphComponent("root2");
		root2.addChild(root);
		root2.getAppearance().setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR,	Color.yellow);
		root2.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW,false);
		root2.getAppearance().setAttribute(CommonAttributes.SMOOTH_SHADING, false);
		replaceExample("moebius band");
//		initializeFactory();
//		updateGeometry();

		return root2;
	}

	
	private void replaceExample(String name) {
		
		if (examples.get(name) == null) 
			throw new IllegalStateException("No such thing named "+name);
		currentGerbil = examples.get(name);
		initializeFactory();
		updateGeometry();

	}
	private void initializeFactory() {
		surface = currentGerbil.surface;
		Matrix[] mlist = currentGerbil.tforms;
		sgc1.setGeometry(surface);
		int numchildren = mlist.length;
		System.err.println("length = "+numchildren);
		SceneGraphUtility.removeChildren(root);
		for (int i = 0; i<numchildren; ++i)	{
			SceneGraphComponent child = SceneGraphUtility.createFullSceneGraphComponent("child"+i);
			mlist[i].assignTo(child);
			//			Appearance ap = new Appearance();
//			ap.setAttribute(CommonAttributes.POLYGON_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, 
//					IMULogo.logoColors[i]);
//			child.setAppearance(ap);
			child.addChild(sgc1);
			root.addChild(child);
		}
		currentSurface = surface;
		if (diamondize && surface.getGeometryAttributes(GeometryUtility.QUAD_MESH_SHAPE) != null)	{
			currentSurface = GeometryUtilityOverflow.diamondize(surface, diamondFactor);
			oldDiamondFactor = diamondFactor;
			System.err.println("activating diamond factor = "+diamondFactor);
		}
		if (thickenSurfaceFactory == null)
			thickenSurfaceFactory = new ThickenedSurfaceFactory(currentSurface);
		else thickenSurfaceFactory.setSurface(currentSurface);
		thickenSurfaceFactory.setThickness(thickness);
		thickenSurfaceFactory.setMakeHoles(makeHoles);
		thickenSurfaceFactory.setLinearHole(linearHole);
		thickenSurfaceFactory.setCurvedEdges(curvedEdges);
		thickenSurfaceFactory.setHoleFactor(holeSize);
		thickenSurfaceFactory.setStepsPerEdge(steps);
		thickenSurfaceFactory.setProfileCurve(profile);
		thickenSurfaceFactory.setMergeDuplicateBoundaryVerts(mergeBound);
	}
	private void updateGeometry() {
		thickenSurfaceFactory.update();
		thickSurface = thickenSurfaceFactory.getThickenedSurface();
//		JoinGeometry.removeDuplicatePoints(thickSurface);
		SceneGraphUtility.removeChildren(sgc1);
		if (thicken) sgc1.setGeometry(thickSurface);
		else {
			sgc1.setGeometry(currentSurface);
		}
		if (showNormals)
			sgc1.addChild(GeometryUtilityOverflow.displayFaceNormals(thicken? thickSurface : surface, .1));
		if (viewer != null) {
			CameraUtility.encompass(viewer);
			viewer.render();
		}
		currentBound = BoundingBoxUtility.calculateBoundingBox(root);
		extentScale = Rn.maxNorm(currentBound.getExtent());
		if (extentSlider != null) extentSlider.setValue(extentScale);
		MatrixBuilder.euclidean().assignTo(root);
		if (flatten) {
			SceneGraphUtility.removeChildren(root2);
			root2.addChild(root);
			SceneGraphComponent tmp =  SceneGraphUtility.flatten(root);
			SceneGraphUtility.removeChildren(root2);
			root2.addChild(tmp);
		}
	}
	
	public void setGeometry(SceneGraphNode input, Matrix[] mlist)	{
		IndexedFaceSet ifs = null;
		if (input instanceof IndexedFaceSet)	{
			ifs = (IndexedFaceSet) input;
		} else if (input instanceof SceneGraphComponent)	{
	        GeometryMergeFactory mergeFact= new GeometryMergeFactory();
	        IndexedFaceSet result=mergeFact.mergeGeometrySets(((SceneGraphComponent) input));
	        result = (IndexedFaceSet) RemoveDuplicateInfo.removeDuplicateVertices(result, (Attribute[]) null);
	        boolean orient = IndexedFaceSetUtility.makeConsistentOrientation(result);
	        System.err.println("set geometry oriented = "+orient);
	        IndexedFaceSetUtility.calculateAndSetFaceNormals(result);
	        ifs = result;
		}
        if (onSphere) {
	        	double[][] pos = ifs.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
	        	if (pos[0].length == 3)
	        		Pn.homogenize(pos, pos);
	        	for (int i = 0; i<pos.length; ++i) pos[i][3] = 0.0;
	        	Rn.normalize(pos, pos);
	        	DataList newV = StorageModel.DOUBLE_ARRAY.array(4).createReadOnly(pos);
	        	ifs.setVertexAttributes(Attribute.NORMALS, null);
	        	ifs.setVertexAttributes(Attribute.NORMALS, newV);
        }
        else 
	        	IndexedFaceSetUtility.calculateAndSetVertexNormals(ifs);
		currentGerbil = new Gerbil(ifs, mlist); // null); //
		examples.put(input.getName(), currentGerbil);
		initializeFactory();
		updateGeometry();
	}
	

	@Override
	public void display() {
		super.display();
		viewer = jrviewer.getViewer();
		jrviewer.encompassEuclidean();
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);
//		SLShader myfog = new SLShader("myfog");
//		myfog.addParameter("signore", new Double(2.0));
//		myfog.addParameter("distance", new Double(5.0));
//		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.RMAN_VOLUME_ATMOSPHERE_SHADER, myfog);
		viewer.getSceneRoot().setPickable(true);
		JMenu testM = new JMenu("Examples");
		ButtonGroup bg = new ButtonGroup();
		Set<String> gnames = examples.keySet();
		for (final String name : gnames)	{
			JMenuItem jm = testM.add(new JRadioButtonMenuItem(name));
			jm.addActionListener( new ActionListener() {
				public void actionPerformed(ActionEvent e)	{
					replaceExample(name);
				}
			});
			bg.add(jm);
		}
		JMenuItem jm = testM.add(new JMenuItem("Load ..."));
		jm.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				loadFile();
			}
		});
		bg.add(jm);
		ViewMenuBar vmb = jrviewer.getPlugin(ViewMenuBar.class);
		vmb.addMenu(null, 2.0, testM, "File");

		((Component) viewer.getViewingComponent()).addKeyListener( new KeyAdapter()	{

			@Override
			public void keyPressed(KeyEvent e)	{ 
				switch(e.getKeyCode())	{
					
				case KeyEvent.VK_H:
					System.out.println("	1:  toggle holes");
					System.out.println("	2:  increase/decrease refine level");
					System.out.println("	3:  increase/decrease number of steps per edge");
					System.out.println("	4:  increase/decrease hole size");
					System.out.println("	5:  increase/decrease thickness");
					break;
	
				case KeyEvent.VK_1:
					makeHoles = !makeHoles;
					thickenSurfaceFactory.setMakeHoles(makeHoles);
					updateGeometry();
					viewer.render();
					break;
					
				case KeyEvent.VK_2:
					if (e.isShiftDown()) {
						refineLevel--;
						if (refineLevel < 0) refineLevel = 0;
					}
					else refineLevel++;
					surface = SphereUtility.tessellatedIcosahedronSphere(refineLevel); //Primitives.cube(); //
					initializeFactory();
					updateGeometry();

					viewer.render();
					break;
					
				case KeyEvent.VK_3:
					if (e.isShiftDown()) {
						steps--;
						if (steps < 1) steps = 1;
					}
					else steps++;
					thickenSurfaceFactory.setStepsPerEdge(steps);
					updateGeometry();
					viewer.render();
					break;

				case KeyEvent.VK_4:
					if (e.isShiftDown()) {
						holeSize /= 1.1;
					}
					else {
						holeSize *= 1.1;
						if (holeSize > 1.0) holeSize = 1.0;
					}
					thickenSurfaceFactory.setHoleFactor(holeSize);
					updateGeometry();
					viewer.render();
					break;

				case KeyEvent.VK_5:
					if (e.isShiftDown()) {
						thickness /= 1.1;
					}
					else {
						thickness *= 1.1;
					}
					thickenSurfaceFactory.setThickness(thickness);
					updateGeometry();
					viewer.render();
					break;
					
				case KeyEvent.VK_6:
					thickenSelection();
					break;
				case KeyEvent.VK_7:
					if (sgc1.getAppearance() == texap) sgc1.setAppearance(ap);
					else sgc1.setAppearance(texap);
					viewer.render();
					break;
				}

			}

		});
	}

	protected void loadFile() {
		JFileChooser fc = new JFileChooser(GlobalProperties.resourceDir);
		JOGLConfiguration.theLog.log(Level.INFO,"FCI resource dir is: "+GlobalProperties.resourceDir);
		int result = fc.showOpenDialog(((Component)viewer.getViewingComponent()));
		SceneGraphComponent sgc = null;
		if (result == JFileChooser.APPROVE_OPTION)	{
			File file = fc.getSelectedFile();
            try {
    			sgc = Readers.read(Input.getInput(file));
				Geometry g = SceneGraphUtility.getFirstGeometry(sgc);
				if (g instanceof IndexedFaceSet) surface = (IndexedFaceSet) g;
				examples.put("loaded geometry",new Gerbil(surface));
				currentGerbil = examples.get("loaded geometry");
				initializeFactory();
				updateGeometry();
				GlobalProperties.resourceDir = file.getAbsolutePath();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	private void thickenSelection()	{
		SelectionManager sm = SelectionManagerImpl.selectionManagerForViewer(viewer);
		SceneGraphPath sgp = sm.getSelection().getSGPath();
		if (!(sgp.getLastElement() instanceof IndexedFaceSet))	return;
		surface = (IndexedFaceSet) sgp.getLastElement();
		SceneGraphComponent container = sgp.getLastComponent();
		container.setGeometry(null);
		initializeFactory();
		updateGeometry();
	}

	@Override
	public Component getInspector() {	
		Box inspectionPanel =  Box.createVerticalBox();
		Box buttonBox = Box.createVerticalBox();
		Box b1 = Box.createHorizontalBox();
		b1.add(buttonBox);
//		b1.add(Box.createHorizontalGlue());
		inspectionPanel.add(b1);
		JCheckBox thickButton = new JCheckBox("thicken", thicken);
		thickButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				thicken = ((JCheckBox) e.getSource()).isSelected();
				//tsf.setMakeHoles(makeHoles);
				updateGeometry();
			}
		});
		buttonBox.add(thickButton);
		JCheckBox directionButton = new JCheckBox("make holes", makeHoles);
		directionButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				makeHoles = ((JCheckBox) e.getSource()).isSelected();
				thickenSurfaceFactory.setMakeHoles(makeHoles);
				updateGeometry();
			}
		});
		buttonBox.add(directionButton);
		JCheckBox linearHoleButton = new JCheckBox("linear hole", linearHole);
		linearHoleButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				linearHole = ((JCheckBox) e.getSource()).isSelected();
				thickenSurfaceFactory.setLinearHole(linearHole);
				updateGeometry();
			}
		});
		buttonBox.add(linearHoleButton);
		JCheckBox mergeBoundButton = new JCheckBox("merge boundary", mergeBound);
		mergeBoundButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				mergeBound = ((JCheckBox) e.getSource()).isSelected();
				thickenSurfaceFactory.setMergeDuplicateBoundaryVerts(mergeBound);
				updateGeometry();
			}
		});
		buttonBox.add(mergeBoundButton);
		JCheckBox curvedEdgesButton = new JCheckBox("curved edges", curvedEdges);
		curvedEdgesButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				curvedEdges = ((JCheckBox) e.getSource()).isSelected();
				thickenSurfaceFactory.setCurvedEdges(curvedEdges);
				updateGeometry();
			}
		});
		buttonBox.add(curvedEdgesButton);
		JCheckBox diamondizeButton = new JCheckBox("diamondize", diamondize);
		diamondizeButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				diamondize = ((JCheckBox) e.getSource()).isSelected();
//				initializeFactory();
			}
		});
		buttonBox.add(diamondizeButton);
		JCheckBox textureizeButton = new JCheckBox("textureize", textureize);
		textureizeButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				textureize = ((JCheckBox) e.getSource()).isSelected();
//				if (textureize) sgc1.setAppearance(texap);
//				else sgc1.setAppearance(null);
				if (textureize) texap.setAttribute("polygonShader.texture2d", theTexture);
				else texap.setAttribute("polygonShader.texture2d", Appearance.INHERITED);
////				initializeFactory();
			}
		});
		buttonBox.add(textureizeButton);
		JCheckBox fcButton = new JCheckBox("face colors", isFaceColors);
		fcButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				isFaceColors = ((JCheckBox) e.getSource()).isSelected();
				thickenSurfaceFactory.setKeepFaceColors(isFaceColors);
				updateGeometry();
			}
		});
		JCheckBox showNormalsButton = new JCheckBox("showNormals", showNormals);
		showNormalsButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				showNormals = ((JCheckBox) e.getSource()).isSelected();
//				initializeFactory();
			}
		});
		buttonBox.add(showNormalsButton);
		JCheckBox flatButton = new JCheckBox("flatten", flatten);
		flatButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				flatten = ((JCheckBox) e.getSource()).isSelected();
				if (!flatten) {
					SceneGraphUtility.removeChildren(root2);
					root2.addChild(root);
				} else updateGeometry();
			}
		});
		buttonBox.add(flatButton);
		final TextSlider thicknessSlider = new TextSlider.Double("thickness",SwingConstants.HORIZONTAL,0.0,1.0,thickness);
		thicknessSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				thickness = thicknessSlider.getValue().doubleValue();
				System.err.println("thickness: "+thickness);
				thickenSurfaceFactory.setThickness(thickness);
				updateGeometry();
			}
		});
		inspectionPanel.add(thicknessSlider);
		final TextSlider holeFactorSlider = new TextSlider.Double("hole factor",SwingConstants.HORIZONTAL,0.0,2.0,holeSize);
		holeFactorSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				holeSize = holeFactorSlider.getValue().doubleValue();
				System.err.println("holeSize "+holeSize);
				thickenSurfaceFactory.setHoleFactor(holeSize);
				updateGeometry();
			}
		});
		inspectionPanel.add(holeFactorSlider);
		final TextSlider shiftSlider = new TextSlider.Double("normal shift",SwingConstants.HORIZONTAL,0.0,1.0,shiftAlongNormal);
		shiftSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				shiftAlongNormal = shiftSlider.getValue().doubleValue();
				System.err.println("shift along normal "+shiftAlongNormal);
				thickenSurfaceFactory.setShiftAlongNormal(shiftAlongNormal);
				updateGeometry();
			}
		});
		inspectionPanel.add(shiftSlider);
		extentSlider = new TextSlider.Double("extent",SwingConstants.HORIZONTAL,0.0,50,extentScale);
		extentSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				double newExtent = extentSlider.getValue().doubleValue();
				double ratio = newExtent/extentScale;
//				extentScale = newExtent;
				MatrixBuilder.euclidean().scale(ratio).assignTo(root);
			}
		});
		inspectionPanel.add(extentSlider);
		final TextSlider diamondSlider = new TextSlider.Double("diamond factor",SwingConstants.HORIZONTAL,0.0,1.0, diamondFactor);
		diamondSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				diamondFactor = diamondSlider.getValue().doubleValue();
				initializeFactory();
				updateGeometry();
			}
		});
		inspectionPanel.add(diamondSlider);
		final TextSlider stepSlider = new TextSlider.Integer("steps per edge",SwingConstants.HORIZONTAL,1,25,steps);
		stepSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				steps =stepSlider.getValue().intValue();
				System.err.println("steps "+steps);
				thickenSurfaceFactory.setStepsPerEdge(steps);
				updateGeometry();
			}
		});
		inspectionPanel.add(stepSlider);
		final TextSlider refineLevelSlider = new TextSlider.Integer("tessellation level",SwingConstants.HORIZONTAL,0,4,refineLevel);
		refineLevelSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				refineLevel =refineLevelSlider.getValue().intValue();
				System.err.println("steps "+steps);
				surface = SphereUtility.tessellatedIcosahedronSphere(refineLevel); //Primitives.cube(); //
				thickenSurfaceFactory = new ThickenedSurfaceFactory(surface);
				thickenSurfaceFactory.setThickness(thickness);
				thickenSurfaceFactory.setMakeHoles(makeHoles);
				thickenSurfaceFactory.setHoleFactor(holeSize);
				thickenSurfaceFactory.setStepsPerEdge(steps);
				thickenSurfaceFactory.setProfileCurve(profile);
				updateGeometry();
			}
		});
		inspectionPanel.add(refineLevelSlider);
		inspectionPanel.add(Box.createVerticalGlue());
		inspectionPanel.setName("Parameters");
		return inspectionPanel;
	}
	
	public IndexedFaceSet getMoebiusBand()	{
		 ParametricSurfaceFactory foo = new ParametricSurfaceFactory();
		 foo.setImmersion(new ParametricSurfaceFactory.Immersion() {
					double R = .6666;

					double r = .2;
					double a =1, b=1;
					public int getDimensionOfAmbientSpace() {
						return 3;
					}

					public void evaluate(double u, double v, double[] xyz,
							int offset) {
						double u2 = u/2;
						xyz[0] = Math.cos(u) * (R + r * v * Math.cos(u2));
						xyz[1] = Math.sin(u) * (R + r * v * Math.cos(u2));
						xyz[2] = r * v* Math.sin(u2);
//						xyz[0] = u;
//						xyz[1] = v;
//						xyz[2] = u*u-v*v;
				
					}

					public boolean isImmutable() {
						return true;
					}
				}

		);
//		 foo.setUMin(-1);
//		 foo.setVMin(-1);
//		foo.setUMax(1);
//		foo.setVMax(1);
		 foo.setUMin(0);
		 foo.setVMin(-1);
		foo.setUMax(2*Math.PI);
		foo.setVMax(1);
		foo.setClosedInUDirection(false);
		foo.setClosedInVDirection(false);
		foo.setULineCount(17);
		foo.setVLineCount(2);
		foo.setGenerateVertexNormals(true);
		foo.setGenerateFaceNormals(true);
		foo.setGenerateEdgesFromFaces(true);
		foo.update();
		IndexedFaceSet bar = foo.getIndexedFaceSet();
		return bar;
	}
	static Matrix[] idlist = {new Matrix()};
	private SceneGraphComponent root2;
	private class Gerbil {
		IndexedFaceSet surface;
		Matrix[] tforms;
		
		Gerbil(IndexedFaceSet s)	{
			this(s, null);
		}
		Gerbil(IndexedFaceSet s, Matrix[] tf) {
			surface = s;
			tforms = (tf == null ? idlist : tf);
		}
	}
	
	public static void main(String[] args) {
		ThickenSurfaceDemo spd = new ThickenSurfaceDemo();
		spd.display();
	}


	public ThickenedSurfaceFactory getThickenSurfaceFactory() {
		return thickenSurfaceFactory;
	}


	public void setThickenSurfaceFactory(
			ThickenedSurfaceFactory thickenSurfaceFactory) {
		this.thickenSurfaceFactory = thickenSurfaceFactory;
	}

	public boolean isOnSphere() {
		return onSphere;
	}

	public void setOnSphere(boolean onSphere) {
		this.onSphere = onSphere;
	}


 }
