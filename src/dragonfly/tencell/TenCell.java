package dragonfly.tencell;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;
import javax.swing.border.TitledBorder;

import charlesgunn.anim.core.AnimatedAdaptor;
import charlesgunn.anim.core.KeyFrame;
import charlesgunn.anim.core.KeyFrameAnimatedBoolean;
import charlesgunn.anim.core.KeyFrameAnimatedDelegate;
import charlesgunn.anim.core.KeyFrameAnimatedDouble;
import charlesgunn.anim.core.KeyFrameAnimatedTransformation;
import charlesgunn.anim.core.TimeDescriptor;
import charlesgunn.anim.jreality.SceneGraphAnimator;
import charlesgunn.anim.plugin.AnimationPlugin;
import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.anim.util.AnimationUtility.InterpolationTypes;
import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import charlesgunn.jreality.newtools.FlyTool2;
import charlesgunn.jreality.plugin.TermesSpherePlugin;
import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.jreality.texture.SimpleTextureFactory.TextureType;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.math.Biquaternion;
import charlesgunn.math.Biquaternion.Metric;
import charlesgunn.math.p5.PlueckerLineGeometry;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.PointSetUtility;
import de.jreality.geometry.TubeUtility;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.plugin.JRViewer;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.Geometry;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.PointSet;
import de.jreality.scene.Scene;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Transformation;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.AttributeEntityUtility;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.toolsystem.ToolSystem;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.RenderTrigger;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.core.DiscreteGroup;
import de.jtem.discretegroup.core.DiscreteGroupColorPicker;
import de.jtem.discretegroup.core.DiscreteGroupElement;
import de.jtem.discretegroup.core.DiscreteGroupSceneGraphRepresentation;
import de.jtem.discretegroup.util.WingedEdge;
import de.jtem.discretegroup.util.WingedEdgeUtility;

public class TenCell extends Assignment  {

	transient private SceneGraphComponent world, kiteSGC, triSGC, oneThing, scene, scene2, beams;
	transient private  SceneGraphComponent oneAxis = SceneGraphUtility.createFullSceneGraphComponent("oneAxis"),
			polarAxis =  SceneGraphUtility.createFullSceneGraphComponent("polarAxis"),			
			allLines =  SceneGraphUtility.createFullSceneGraphComponent("allLines"),
			allTetrahedra =  SceneGraphUtility.createFullSceneGraphComponent("allTetrahedra");
	transient private SceneGraphComponent[][] bigArray = new SceneGraphComponent[4][];

	transient private  SceneGraphComponent dirdom = SceneGraphUtility.createFullSceneGraphComponent("dirdom"),
			singleAnimatedDirdom = SceneGraphUtility.createFullSceneGraphComponent("singleAnimDirdom"); //,
//			curveSGC = SceneGraphUtility.createFullSceneGraphComponent("helix curve");
	transient private SceneGraphComponent listofTetrahedra[] = new SceneGraphComponent[10],
			listOfLines[] = new SceneGraphComponent[10],
			beamList[] = new SceneGraphComponent[10],
			dirdomList[] = new SceneGraphComponent[10],
			axisList[] = new SceneGraphComponent[10],
			polarAxisList[] = new SceneGraphComponent[10];
	transient private IndexedFaceSetFactory kiteFactory, triFactory;
//	transient private PointCollector helixCurve = new PointCollector(1000, 4);
	transient double[][] crossSection;
//	transient private CurveCollector helixTube = new CurveCollector(1000, 9, 4);
	transient private boolean showRingOfSix = false,
			metricFactorChanged = true;
	transient double implodeTri =  .35,
			implodeHex = .35,
			scaleShape = .25,
			beamRadius = .05,
			tubeRadius = .037,
			helixTubeRadius = .015;
	transient double c = 1.0/3.0, hex2triRatio = 5.0/9.0;
	transient double  metricFactor = Math.sqrt(hex2triRatio);
	transient double originalMetricFactor = metricFactor;
	transient double[][] verts = {
			{c,c, c, metricFactor},
			{0,0,1,metricFactor},
			{c, -c,1,metricFactor},
			{2*c,-c, 2*c,metricFactor}};
	transient double[][] triverts = {
			{2*c,-c, 2*c,metricFactor}, 
			{c, -c,1,metricFactor},
			{hex2triRatio,-hex2triRatio,hex2triRatio,metricFactor}};
	transient double[][] axis111 = {{1,1,1,1},{-1,-1,1,1},{-1,1,-1,1},{1,-1,-1,1}};
	transient private Color[] 		colorList = new Color[] {
			new Color(0, 100, 250), //Color.blue,
			Color.green,
			Color.red,
			new Color(255, 255, 0), // Color.orange
			Color.magenta,
			Color.cyan,
			Color.pink,
			Color.yellow,
			Color.gray
	};
	transient int whichAnim = 1;		// 0 = dropper: one FD moves around and leaves "copies" behind
										// 1 = clifford: fly through the middle; then fly and rotate in clifford translation
	transient 	private TextSlider scaleSlider, tubeRadSlider, metricSlider;
	transient private WingedEdge fundamentalDomainWE = new WingedEdge();
	transient private TermesSpherePlugin tsp;

	public int getMetric() {
		return Pn.ELLIPTIC;
	}

	@Override
	public SceneGraphComponent getContent() {
		if (scene2 != null) return scene2;
		scene = SceneGraphUtility.createFullSceneGraphComponent("scene");
		scene2 = SceneGraphUtility.createFullSceneGraphComponent("scene2");
		scene2.getAppearance().setAttribute(CommonAttributes.METRIC, Pn.ELLIPTIC);
		scene2.getAppearance().setAttribute(CommonAttributes.SMOOTH_SHADING, false);
//		world.getAppearance().setAttribute(CommonAttributes.METRIC, Pn.ELLIPTIC);
//		scene.getAppearance().setAttribute(CommonAttributes.VERTEX_DRAW, false);
		scene.getAppearance().setAttribute("oneGLSL", true);  // don't ask!
		scene.getAppearance().setAttribute(CommonAttributes.TUBES_DRAW, false);
		scene.getAppearance().setAttribute(CommonAttributes.SPHERES_DRAW, false);
//		scene.getAppearance().setAttribute(CommonAttributes.SCALE, .008);
//		scene.getAppearance().setAttribute(CommonAttributes.ALIGNMENT, SwingConstants.BOTTOM);
//		scene.getAppearance().setAttribute(CommonAttributes.OFFSET, new double[]{0,0,0});
		scene.getAppearance().setAttribute(CommonAttributes.METRIC, Pn.ELLIPTIC);
		scene.getAppearance().setAttribute("diffuseColor", Color.white);
		scene.getAppearance().setAttribute(CommonAttributes.AMBIENT_COEFFICIENT, .1);
		scene.getAppearance().setAttribute(CommonAttributes.SPECULAR_COEFFICIENT, .3);
		scene.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
		scene2.addChild(scene);
		scene.addChildren(allLines, allTetrahedra); //, curveSGC);
		if (whichAnim == 0) scene.addChild(singleAnimatedDirdom);
// 		allLines.getAppearance().setAttribute(CommonAttributes.LIGHTING_ENABLED, false);
		allLines.getAppearance().setAttribute(CommonAttributes.SMOOTH_SHADING, true);
		allLines.getAppearance().setAttribute(CommonAttributes.TUBES_DRAW, false);
//		allLines.getAppearance().setAttribute(CommonAttributes.USE_GLSL, true);
//		allLines.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, true);
		allLines.getAppearance().setAttribute(CommonAttributes.LINE_WIDTH, 1.0);
		allLines.getAppearance().setAttribute("polygonShader."+CommonAttributes.SPECULAR_COEFFICIENT, 0.0);
		allLines.getAppearance().setAttribute("polygonShader."+CommonAttributes.AMBIENT_COEFFICIENT, .08);
		allLines.getAppearance().setAttribute("polygonShader."+CommonAttributes.DIFFUSE_COEFFICIENT, .9);

		SceneGraphComponent lines = getSymmetryAxes();

		createFundamentalRegion();
		WingedEdge we = getWingedEdge();
		we.setMetric(Pn.ELLIPTIC);
		SceneGraphComponent wingedEdge = new SceneGraphComponent("Winged edge");
		wingedEdge.setGeometry(we);
		beams = WingedEdgeUtility.createBeamsOnEdges(beams, we, null, (originalMetricFactor/metricFactor)*beamRadius, 6, 1 );
		beams.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, false);
		beams.getAppearance().setAttribute(CommonAttributes.AMBIENT_COEFFICIENT, .2);
		SimpleTextureFactory stf = new SimpleTextureFactory();
		stf.setType(TextureType.STRIPES);
		stf.update();
		stf.setColor(0, Color.white);
		stf.setColor(1, new Color(0,100, 200));
		stf.setParams(new double[]{0.4, 0.6, 1.0});
		stf.setIndices(new int[]{0,1,0});
		stf.update();
		Texture2D tex2d = TextureUtility.createTexture(beams.getAppearance(), "polygonShader", stf.getImageData());
		beams.getAppearance().setAttribute("polygonShader.diffuseColor", new Color(250, 255, 255));
		dirdom.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW, false);
		dirdom.getAppearance().setAttribute("lineShader.diffuseColor", new Color(255, 255, 255));

		SceneGraphComponent id = listofTetrahedra[0] = SceneGraphUtility.createFullSceneGraphComponent("id");
		id.addChildren(beams);
		SceneGraphComponent polar = listofTetrahedra[1] = SceneGraphUtility.createFullSceneGraphComponent("polar");
		polar.addChildren(beams);
			id.addChildren(dirdom, lines);
			polar.addChildren(dirdom, lines);
		for (int i = 0; i<listofTetrahedra.length; ++i)	{
			listofTetrahedra[i] = SceneGraphUtility.createFullSceneGraphComponent(""+i);
			listOfLines[i] = SceneGraphUtility.createFullSceneGraphComponent(""+i);
			beamList[i] = SceneGraphUtility.createFullSceneGraphComponent(""+i);
			dirdomList[i] = SceneGraphUtility.createFullSceneGraphComponent(""+i);
			axisList[i] = SceneGraphUtility.createFullSceneGraphComponent(""+i);
			polarAxisList[i] = SceneGraphUtility.createFullSceneGraphComponent(""+i);
			axisFactories[i] = new PointRangeFactory();
			axisFactories[i].setFiniteSphere(false);
			axisFactories[i].setNumberOfSamples(22);
			axisFactories[i].update();
			polarFactories[i] = new PointRangeFactory();
			polarFactories[i].setFiniteSphere(false);
			polarFactories[i].setNumberOfSamples(22);
			polarFactories[i].update();
			polarAxisList[i].setVisible(i==0);
			axisList[i].setVisible(i>=7);
			allTetrahedra.addChild(listofTetrahedra[i]);
			allLines.addChild(listOfLines[i]);
			listofTetrahedra[i].addChildren(beamList[i], dirdomList[i]); //, lines);
			dirdomList[i].setVisible(i>3);
			beamList[i].addChild(beams);
			dirdomList[i].addChild(dirdom);
			listofTetrahedra[i].getAppearance().setAttribute("polygonShader.diffuseColor", colorList[i/2]);
			listofTetrahedra[i].getAppearance().setAttribute("lineShader.diffuseColor", colorList[i/2]);
			listOfLines[i].addChildren(axisList[i], polarAxisList[i]);
		}
		setupLineAps();
		bigArray[0] = beamList;
		bigArray[1] = dirdomList;
		bigArray[2] = axisList;
		bigArray[3] = polarAxisList;
		SceneGraphComponent filler = new SceneGraphComponent("filler");
		filler.addChild(dirdom);
		filler.setTransformation(listofTetrahedra[9].getTransformation());
		singleAnimatedDirdom.addChildren(filler);
		singleAnimatedDirdom.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		update();
		return scene2;
	}

	transient boolean doStripes = true;
	transient double[] params3 = {.5,.75, 1};
	transient int[] indices3 = {2,0,1};
	transient double[] params4 = {.5, .666, .8333, 1.0};
	transient int[] indices4 = {3,0,1,2};
	private void setupLineAps()	{
		int i = 0;
		for (int j = 0; j<5; ++j)	{
			for (int k = j+1; k<5; ++k)	{
				Appearance axisAp = new Appearance();
				Appearance polarAp = new Appearance();
				if (!doStripes)	{
					axisAp.setAttribute("lineShader.diffuseColor", Color.red);
					axisAp.setAttribute("polygonShader.diffuseColor", Color.red);
					Color axisColor = new Color(100,200,200);
					polarAp.setAttribute("lineShader.diffuseColor", axisColor);
					polarAp.setAttribute("polygonShader.diffuseColor", axisColor);					
				} else {
					SimpleTextureFactory gradient = new SimpleTextureFactory();
					gradient.setType(TextureType.STRIPES);
					gradient.setColor(0, colorList[j]);
					gradient.setColor(1, colorList[k]);
					gradient.setColor(2, Color.white);
					gradient.setParams(params3);
					gradient.setIndices(indices3);
//					gradient.setColor(2, Color.green); //colorList[1]);
					//		gradient.setChannels(new int[]{3,0,2,1});
					gradient.update();
				   Texture2D tex2d = (Texture2D) AttributeEntityUtility
				       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", axisAp  , true);	//new Appearance(), true); //
				   tex2d.setImage(gradient.getImageData());
				   Matrix m = new Matrix();
				   MatrixBuilder.euclidean(m).scale(3,3,1).skew(1,0, 1.0/3.0).rotateZ(Math.PI/2.0);
				   tex2d.setTextureMatrix(m);
					axisList[i].setAppearance(axisAp);
					gradient.setColor(3, Color.white);
					gradient.setParams(params4);
					gradient.setIndices(indices4);
					int[] cmp = getComplement(new int[]{j,k});
					for (int n = 0; n<3; ++n) gradient.setColor(n, colorList[cmp[n]]);
					//		gradient.setChannels(new int[]{3,0,2,1});
					gradient.update();
				   tex2d = (Texture2D) AttributeEntityUtility
				       .createAttributeEntity(Texture2D.class, "polygonShader.texture2d", polarAp  , true);	//new Appearance(), true); //
				   tex2d.setImage(gradient.getImageData());
				   m = new Matrix();
				   MatrixBuilder.euclidean(m).scale(3,3,1).skew(1,0,3.0).rotateZ(Math.PI/2.0);
//				   MatrixBuilder.euclidean().rotateZ(Math.PI/4).scale(4,100,1).assignTo(m);
//					   m.setEntry(3,1, -1);
				   tex2d.setTextureMatrix(m);
					polarAxisList[i].setAppearance(polarAp);
					
				}

				i++;
			}
		}
		
	}
	private int[] getComplement(int[] is) {
		int[] res = new int[3];
		int count = 0;
		for (int i =0; i<5; ++i)	{
			if (is[0] != i && is[1] != i) res[count++] = i;
		}
		return res;
	}

	transient double[][] centerPoints = new double[5][];
	transient PointRangeFactory[] axisFactories = new PointRangeFactory[10], polarFactories = new PointRangeFactory[10];
	{
		centerPoints[0] = P3.originP3;

	}
	protected void update() {
		IndexedFaceSet indexedFaceSet = kiteFactory.getIndexedFaceSet();
		indexedFaceSet.setName("kite");
		indexedFaceSet.setGeometryAttributes("metric", Pn.ELLIPTIC);
		IndexedFaceSet kite = IndexedFaceSetUtility.implode(indexedFaceSet, implodeHex);
		kite.setName("kiteImploded");
		kiteSGC.setGeometry(kite); //indexedFaceSet); //
//		kiteSGC.addChild(IndexedFaceSetUtility.displayFaceNormals(kite, .2, Pn.ELLIPTIC));
		indexedFaceSet = triFactory.getIndexedFaceSet();
		indexedFaceSet.setName("tri");
		indexedFaceSet.setGeometryAttributes("metric", Pn.ELLIPTIC);
		IndexedFaceSet tri = IndexedFaceSetUtility.implode(indexedFaceSet, implodeTri);
		triSGC.setGeometry(tri); //indexedFaceSet); //
//		triSGC.addChild(IndexedFaceSetUtility.displayFaceNormals(tri, .2, Pn.ELLIPTIC));
		tri.setName("triImploded");
		MatrixBuilder.euclidean().scale(scaleShape).assignTo(oneThing);
		if (!metricFactorChanged) return;
		metricFactorChanged = false;
		System.err.println("updating geometry");
		for (int i = 0; i<10; ++i)	{
			listofTetrahedra[i].getTransformation().setReadOnly(false);
		}
		// update all the geometry
		getWingedEdge();
		beams = WingedEdgeUtility.createBeamsOnEdges(beams, fundamentalDomainWE, null, (originalMetricFactor/metricFactor)*beamRadius, 6, 1 );
		double[][] kiteVerts = {{0,0,1,metricFactor},{c, -c,1,metricFactor},{2*c,-c, 2*c,metricFactor},{c,c, c, metricFactor},};
		kiteFactory.setVertexCoordinates(kiteVerts);
		kiteFactory.setVertexTextureCoordinates(new double[] { 1,0,1,1,0,1,0,0});
		triFactory.setMetric(Pn.ELLIPTIC);
		kiteFactory.update();
		double[][] triverts = {{c, -c,1,metricFactor},{2*c,-c, 2*c,metricFactor}, {hex2triRatio,-hex2triRatio,hex2triRatio,metricFactor}};
		triFactory.setVertexCoordinates(triverts);
		triFactory.setMetric(Pn.ELLIPTIC);
		triFactory.update();
		Pn.normalize(kiteVerts, kiteVerts, Pn.ELLIPTIC);
		Pn.normalize(triverts, triverts, Pn.ELLIPTIC);
//		System.err.println("kite verts = "+Rn.toString(kiteVerts));
//		System.err.println("tri verts = "+Rn.toString(triverts));
		// update tform for polar copy
		double[] hexPlane = {1,1,1, -3*c/metricFactor};
		double[] triPlane = {1,1,1, 3*hex2triRatio/metricFactor};
		MatrixBuilder.elliptic().reflect(hexPlane).reflect(triPlane).rotate(Math.PI/3.0, axis111[0]).assignTo(listofTetrahedra[1]);
		for (int i = 0; i<4; ++i)	{
			double[] plane = axis111[i].clone();
			plane[3] = 0.0;
			double[] pt1 = axis111[i].clone();
			Rn.times(pt1, -c, pt1);
			pt1[3] = metricFactor;
			double[] pt2 = axis111[i].clone();
			Rn.times(pt2, c, pt2);
			pt2[3] = metricFactor;
			Matrix ferd = new Matrix();
			MatrixBuilder.elliptic().translate(pt1, pt2).rotate(Math.PI/3.0, axis111[i]).reflect(plane).assignTo(listofTetrahedra[2*i+2]);
//			MatrixBuilder.elliptic().rotate(verts[0], verts[1], Math.PI).assignTo(copy[2*i]);
			ferd.assignFrom(listofTetrahedra[2*i+2].getTransformation());
//			System.err.println("i = "+i);
//			P3.orthonormalizeMatrix(null, ferd.getArray(), 10E-6, Pn.ELLIPTIC);
			pt1[3] *= 3/5.0;  pt2[3] *= 3.0/5.0;
//			double d = Pn.distanceBetween(pt1, pt2, Pn.ELLIPTIC);
//			System.err.println("Distance = "+d);
//			MatrixBuilder.elliptic().translate(pt2, pt1).assignTo(ferd);
//			System.err.println("Translation = "+Rn.matrixToString(ferd.getArray()));
//			P3.orthonormalizeMatrix(ferd.getArray(), ferd.getArray(), 10E-6, Pn.ELLIPTIC);
			MatrixBuilder.elliptic().translate(pt2, pt1).rotate(2*Math.PI/3.0, axis111[i]).reflect(plane).assignTo(ferd); //(copy[2*i+1]);			
//			P3.orthonormalizeMatrix(ferd.getArray(), ferd.getArray(), 10E-6, Pn.ELLIPTIC);
			ferd.assignTo(listofTetrahedra[2*i+3]);
		}
		
		for (int i = 0; i<10; ++i)	{
			listofTetrahedra[i].getTransformation().setReadOnly(true);
		}
		// update the lines, first the center points
		
		for (int i = 1; i<5; ++i)	{
			centerPoints[i] = Rn.matrixTimesVector(centerPoints[i], listofTetrahedra[2*i].getTransformation().getMatrix(), centerPoints[0]);
		}
		int i = 0;
		for (int j = 0; j<5; ++j)	{
			for (int k = j+1; k<5; ++k)	{
				int jj = j, kk = k;
				if (j == 0 && k == 1 || j==0 && k == 2 || j == 1 && k == 2 || j == 3 && k == 4) {
					jj = k;
					kk = j;
				}
				axisFactories[i].setElement0(centerPoints[jj]);
				axisFactories[i].setElement1(centerPoints[kk]);
				axisFactories[i].update();
				double[] polarAxisD = PlueckerLineGeometry.polarize(null, axisFactories[i].getPluckerLine(), Pn.ELLIPTIC);
				polarFactories[i].setPluckerLine(polarAxisD);
				if (!(j == 0 && k == 1 || j==0 && k == 3 || j == 1 && k == 3 || j == 2 && k == 4)) {
					double[] e0 = polarFactories[i].getElement0().clone(),
							e1 = polarFactories[i].getElement1().clone();
					polarFactories[i].setElement0(e1);
					polarFactories[i].setElement1(e0);
				}
				polarFactories[i].update();
				i++;
			}
		}
		updateAxisPairs();

	}
	protected void updateAxisPairs() {
		for (int i = 0; i<10; ++i)	{
			Geometry geometry = axisFactories[i].getTubedLine(null, tubeRadius, 24).getGeometry();
			axisList[i].setGeometry(geometry); //getLine());//
			SceneGraphUtility.removeChildren(axisList[i]);
//			SceneGraphComponent displayFaceNormals2 = IndexedFaceSetUtility.displayFaceNormals( (IndexedFaceSet) geometry, .02, Pn.ELLIPTIC);
			SceneGraphComponent displayFaceNormals2 = PointSetUtility.displayVertexNormals((PointSet) geometry, .05, Pn.ELLIPTIC);
			displayFaceNormals2.getAppearance().setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
//			axisList[i].addChild(displayFaceNormals2);
			Geometry geometry2 = polarFactories[i].getTubedLine(null, tubeRadius, 24).getGeometry();
			polarAxisList[i].setGeometry(geometry2); //getLine());
			SceneGraphUtility.removeChildren(polarAxisList[i]);
///			SceneGraphComponent displayFaceNormals = IndexedFaceSetUtility.displayFaceNormals((IndexedFaceSet) geometry2, .02, Pn.ELLIPTIC);
			SceneGraphComponent displayFaceNormals = PointSetUtility.displayVertexNormals((PointSet) geometry2, .05, Pn.ELLIPTIC);
			displayFaceNormals.getAppearance().setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
//			polarAxisList[i].addChild(displayFaceNormals);
		}
	}


	protected SceneGraphComponent getSymmetryAxes() {
		PointRangeFactory prf = new PointRangeFactory();
		prf.setElement0(new double[]{0,0,0,1});
		prf.setElement1(new double[]{1,1,1,0});
		prf.setFiniteSphere(false);
		prf.setNumberOfSamples(22);
		prf.update();
		SceneGraphComponent lines = SceneGraphUtility.createFullSceneGraphComponent("axes");
		oneAxis.getAppearance().setAttribute("lineShader.diffuseColor", Color.red);
		oneAxis.getAppearance().setAttribute("polygonShader.diffuseColor", Color.red);
//		oneAxis.setGeometry(axisFac.getIndexedFaceSet());
		oneAxis.setGeometry(prf.getLine());
		oneAxis.setVisible(!showRingOfSix);
		double[] polarAxisD = PlueckerLineGeometry.polarize(null, prf.getPluckerLine(), Pn.ELLIPTIC);
		prf = new PointRangeFactory();
		prf.setFiniteSphere(false);
		prf.setPluckerLine(polarAxisD);
		prf.update();
		polarAxis = SceneGraphUtility.createFullSceneGraphComponent("polarAxis");
		Color axisColor = new Color(100,200,200);
		polarAxis.getAppearance().setAttribute("lineShader.diffuseColor", axisColor);
		polarAxis.getAppearance().setAttribute("polygonShader.diffuseColor", axisColor);
		polarAxis.setGeometry(prf.getLine()); //polarAxisFac.getIndexedFaceSet());
		return lines;
	}

	protected SceneGraphComponent createFundamentalRegion() {

		kiteFactory = IndexedFaceSetUtility.constructPolygonFactory(kiteFactory, verts, Pn.ELLIPTIC);
		triFactory = IndexedFaceSetUtility.constructPolygonFactory(triFactory, triverts, Pn.ELLIPTIC);
		world = SceneGraphUtility.createFullSceneGraphComponent("world");
		kiteSGC = SceneGraphUtility.createFullSceneGraphComponent("kite");
		kiteSGC.setGeometry(kiteFactory.getIndexedFaceSet());
		triSGC = SceneGraphUtility.createFullSceneGraphComponent("tri");
		triSGC.setGeometry(triFactory.getIndexedFaceSet());
//		kiteSGC.setVisible(false);
		world.addChildren(kiteSGC, triSGC);
		Appearance ap = world.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
//		ap.setAttribute(CommonAttributes.TUBES_DRAW, false);
		ap.setAttribute(CommonAttributes.TUBE_RADIUS, .005);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.white);
		ap.setAttribute(CommonAttributes.METRIC, Pn.ELLIPTIC);
//		ap.setAttribute(CommonAttributes.FLIP_NORMALS_ENABLED, true);
		ap.setAttribute("polygonShader.ambientCoefficient", .1);
		ap.setAttribute("polygonShader.diffuseCoefficient", .8);
		ap.setAttribute("polygonShader.specularCoefficient", 0.3);
		ap.setAttribute(CommonAttributes.USE_GLSL, true);
		ap.setAttribute("lineShader."+CommonAttributes.USE_GLSL, false);
		ap.setAttribute("oneGLSL", true);
		SimpleTextureFactory stf = new SimpleTextureFactory();
		stf.setType(TextureType.GRAPH_PAPER);
		stf.setColor(0, Color.white);
		stf.setColor(1, new Color(0, 125, 255,0));
		stf.setParams(new double[]{.3, .5, 0.8, 1.0});
		stf.setIndices(new int[]{0,1,0,1});
		stf.update();
//		Texture2D tex2d = TextureUtility.createTexture(kiteSGC.getAppearance(), "polygonShader", stf.getImageData());
		Matrix foo = new Matrix();
		MatrixBuilder.euclidean().scale(4.0).assignTo(foo);
//		tex2d.setTextureMatrix(foo);
		DiscreteGroup dg = new DiscreteGroup();
		dg.setDimension(3);
		dg.setMetric(Pn.ELLIPTIC);
		double[][] refl = {{1,1,0,0},{1,-1,0,0},{1,0,-1,0}};
		DiscreteGroupElement dge[] = new DiscreteGroupElement[3];
		for (int i= 0; i<3; ++i)	{
			double[] m = P3.makeReflectionMatrix(null, refl[i], Pn.ELLIPTIC);
			dge[i] = new DiscreteGroupElement(Pn.ELLIPTIC, m);
		}
		dg.setGenerators(dge);
		dg.setFinite(true);
		dg.setColorPicker(new MyColorPicker());
		dg.update();
		Appearance[] aps = new Appearance[4];
		for (int i = 0; i<4;++i)	{
			aps[i] = new Appearance();
			aps[i].setAttribute("polygonShader.diffuseColor", colorList[i]);
		} 
		DiscreteGroupSceneGraphRepresentation dgsgr = new DiscreteGroupSceneGraphRepresentation(dg);
		dgsgr.setWorldNode(world);
		world.getAppearance().setAttribute(SceneGraphAnimator.ANIMATED, false);
//		dgsgr.setAppList(aps);
		dgsgr.update();
		oneThing = dgsgr.getRepresentationRoot(); //showWE ? wingedEdge : dgsgr.getRepresentationRoot();
		dirdom.addChildren(oneThing); //, beams);
		dirdom.getAppearance().setAttribute(CommonAttributes.TUBES_DRAW, false);
		dirdom.getAppearance().setAttribute(CommonAttributes.SPHERES_DRAW, false);
		return dirdom;
	}

	protected WingedEdge getWingedEdge() {
		double a = 1.0/metricFactor; //Math.sqrt(5.0)/4;
		fundamentalDomainWE.init();
		fundamentalDomainWE.setMetric(Pn.ELLIPTIC);
		double[][] planes = new double[8][];
		double[][] planes4 = new double[][] {{1,1,1,-a},{1,-1,-1,-a},{-1,1,-1,-a},{-1,-1,1,-a}};
		double k = 5/3.0;
		for (int i = 0; i<4; ++i) {
			// the origin should be "inside" all the planes
			planes[i] = planes4[i];
			if (planes[i][3] > 0) Rn.times(planes[i],-1,planes[i]);
			planes[i+4] = planes[i].clone();
			// the other 4 planes are parallel the first ones, designed to cut off the opposite corners
			planes[i+4][3] =  -(k)*planes[i+4][3];
			Rn.times(planes[i+4], -1.0, planes[i+4]);
		}
		
		for (int i = 0; i<8; ++i)	{
			fundamentalDomainWE.cutWithPlane(planes[i]);
		}
		fundamentalDomainWE.update();
		return fundamentalDomainWE;
	}
	

	public static final class MyColorPicker extends DiscreteGroupColorPicker {
		double[] spaceD = {1,1,1,1};
		@Override
		public int calculateColorIndexForElement(DiscreteGroupElement dge) {
			double[] spaceDiag = Rn.matrixTimesVector(null, dge.getArray(), spaceD);
			boolean neg[] = new boolean[4];
			for (int i = 0; i<4; ++i)	{
				 neg[i] = spaceDiag[i] < 0;
			}
			if (neg[0] && neg[1]) return 1;
			if (neg[1] && neg[2]) return 2;
			if (neg[2] && neg[0]) return 3;
			else return 0;
		}	
	}
	transient SceneGraphComponent movingScrew = SceneGraphUtility.createFullSceneGraphComponent("moving screw");


	@Override
	public void setupJRViewer(JRViewer v) {
		// TODO Auto-generated method stub
		super.setupJRViewer(v);
		tsp = new TermesSpherePlugin();
		v.registerPlugin(tsp);
	}

	transient SphericalLights sl = new SphericalLights();
	public void display() {
		super.display();
		final Viewer viewer = jrviewer.getViewer();
		RenderTrigger renderTrigger = ToolSystem.getToolSystemForViewer(viewer).getRenderTrigger();
		renderTrigger.setAsync(true);
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.METRIC, Pn.ELLIPTIC);
//		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.RENDER_S3, true);
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.USE_GLSL, true);
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, new Color(0,0,0,255));
		final SceneGraphComponent camnode = CameraUtility.getCameraNode(viewer);
		Scene.executeWriter(viewer.getSceneRoot(), new Runnable() {
			
			@Override
			public void run() {
				FlyTool2 tool = new FlyTool2();
				tool.setGain(.4);
				camnode.addTool(tool);
				SceneGraphUtility.removeLights(viewer);
				scene.addChild(sl.makeLights());
			}
		});
		
		Camera cam = CameraUtility.getCamera(viewer);
		cam.setNear(.01);
		cam.setFar(-.01);
//		((Component) viewer.getViewingComponent()).addKeyListener(getKeyAdapter());
		Transformation transformation = CameraUtility.getCameraNode(viewer).getTransformation();
		transformation.setMatrix(
				P3.makeLookatMatrix(null, P3.originP3, new double[]{1,1,-1,1}, Math.PI/12, Pn.ELLIPTIC));

		AnimationPlugin apl = animationPlugin;
		apl.getAnimationPanel().getRecordPrefs().setCurrentDirectoryPath("/Users/gunn/Movies/tencell/");
		apl.setAnimateCamera(false);
		apl.setAnimateSceneGraph(false);

		// add the camera node by hand to animation system
		KeyFrameAnimatedTransformation T = new KeyFrameAnimatedTransformation(transformation, Pn.ELLIPTIC);	
		T.setInterpolationType(InterpolationTypes.CUBIC_HERMITE);
		T.setName(CameraUtility.getCameraNode(viewer).getName()+" Tform");
//		System.err.println("Adding keyframe for "+T.getName());
		apl.getAnimated().add(T);					

		de.jreality.plugin.basic.Scene scenep = jrviewer.getController().getPlugin(de.jreality.plugin.basic.Scene.class);
		transformation = scenep.getContentComponent().getTransformation();
		T = new KeyFrameAnimatedTransformation(transformation, Pn.ELLIPTIC);	
		T.setInterpolationType(InterpolationTypes.CUBIC_HERMITE);
		T.setName(scenep.getContentComponent()+" Tform");
//		System.err.println("Adding keyframe for "+T.getName());
		apl.getAnimated().add(T);					


		switch(whichAnim)	{
		case 0:
			KeyFrameAnimatedDelegate<Double> screwd = new KeyFrameAnimatedDelegate<Double> () {
				public void propagateCurrentValue(Double t) {
//					if (crossSection == null) return;
					Biquaternion bq = Biquaternion.exp(null, axis, angleScrew, 3*Math.PI*t);
					double[] mat = Biquaternion.matrixFromBiquaternion(null, bq);
					singleAnimatedDirdom.getTransformation().setMatrix(mat);
					double[] mat2 = Rn.times(null, mat, listofTetrahedra[9].getTransformation().getMatrix());
//					double[] v = Rn.matrixTimesVector(null, mat2, P3.originP3);
//					helixCurve.addPoint(v);
//					double[][] crossSectionT = Rn.matrixTimesVector(null, mat, crossSection);
//					helixTube.addCurve(crossSectionT);
//					System.err.println("helix curve add = "+Rn.toString(v));
				}
				public Double gatherCurrentValue(Double t) {
					return null;
				}			
			};
			KeyFrameAnimatedDouble screw = new KeyFrameAnimatedDouble(screwd);
			screw.setInterpolationType(InterpolationTypes.CUBIC_HERMITE);//LINEAR); //
			screw.setName("screwD");
			KeyFrameAnimatedBoolean[] visibilo = new KeyFrameAnimatedBoolean[7];
			TimeDescriptor td = new TimeDescriptor(0.0);
			final int[] indices = {9,6,5,8,7,4};
			for (int i = 0; i<7; ++i)	{
				final int j = i;
				KeyFrameAnimatedDelegate<Boolean> visibiloDel = new KeyFrameAnimatedDelegate<Boolean> () {
					public void propagateCurrentValue(Boolean t) {
						if (j < 6) dirdomList[indices[j]].setVisible(t);
						else singleAnimatedDirdom.setVisible(t);
					}
					public Boolean gatherCurrentValue(Boolean t) {
						return null;
					}			
				};
				visibilo[i] = new KeyFrameAnimatedBoolean(visibiloDel);
				visibilo[i].addKeyFrame(new KeyFrame<Boolean>(td, i == 6));
				apl.getAnimated().add(visibilo[i]);
			}

			double[] keytimes = new double[7];		
			double step = 1.0/6.0;
			{
				for (int i = 0; i<7; ++i)	{
					keytimes[i] = i*(step);
					td  = new TimeDescriptor(keytimes[i]);
					visibilo[i].addKeyFrame(new KeyFrame<Boolean>(td, i < 6));
					visibilo[i].setWritable(false);
					screw.addKeyFrame(new KeyFrame<Double>(td, keytimes[i]));
				}
			}
			// don't allow other key frames to be set from control panel
			screw.setWritable(false);
			apl.getAnimated().add(screw);
			break;
			
		case 1:
			apl.getAnimated().add(anim1);
			break;
		}
	
		
		KeyFrameAnimatedDelegate<Double> dd = new KeyFrameAnimatedDelegate<Double> () {

			public void propagateCurrentValue(Double t) {
				scaleShape = t;
				scaleSlider.setValue(t);
				update();
			}

			public Double gatherCurrentValue(Double t) {
				return scaleShape;
			}
			
		};

		KeyFrameAnimatedDouble animAlpha = new KeyFrameAnimatedDouble(dd );
		animAlpha.setName("animAlpha");
		apl.getAnimated().add(animAlpha);
		
		dd = new KeyFrameAnimatedDelegate<Double> () {

			public void propagateCurrentValue(Double t) {
				tubeRadius = t;
				tubeRadSlider.setValue(t);
				update();
			}

			public Double gatherCurrentValue(Double t) {
				return tubeRadius;
			}
			
		};
		KeyFrameAnimatedDouble animTubeRadius = new KeyFrameAnimatedDouble(dd );
		animTubeRadius.setName("animTubeRadius");
		apl.getAnimated().add(animTubeRadius);

		KeyFrameAnimatedDelegate<Double> mfdfad = new KeyFrameAnimatedDelegate<Double> () {
			public void propagateCurrentValue(Double t) {
				if (t == metricFactor) return;
				metricFactor = t;
				metricFactorChanged = true;
				metricSlider.setValue(t);
				update();
			}
			public Double gatherCurrentValue(Double t) {
				return metricFactor;
			}			
		};
		KeyFrameAnimatedDouble metricFactorD = new KeyFrameAnimatedDouble(mfdfad );
		metricFactorD.setName("metricFactor");
		apl.getAnimated().add(metricFactorD);

		KeyFrameAnimatedDelegate<Double> ihkfad = new KeyFrameAnimatedDelegate<Double> () {
			public void propagateCurrentValue(Double t) {
				if (t == implodeHex) return;
				implodeHex = t;
				impHexSlider.setValue(t);
				update();
			}
			public Double gatherCurrentValue(Double t) {
				return implodeHex;
			}			
		};
		KeyFrameAnimatedDouble implodeHexD = new KeyFrameAnimatedDouble(ihkfad );
		implodeHexD.setName("implodeHex");
		apl.getAnimated().add(implodeHexD);

		KeyFrameAnimatedDelegate<Double> itkfad = new KeyFrameAnimatedDelegate<Double> () {
			public void propagateCurrentValue(Double t) {
				if (t == implodeTri) return;
				implodeTri = t;
				impTriSlider.setValue(t);
				update();
			}
			public Double gatherCurrentValue(Double t) {
				return implodeTri;
			}			
		};
		KeyFrameAnimatedDouble implodeTriD = new KeyFrameAnimatedDouble(itkfad );
		implodeTriD.setName("implodeTri");
		apl.getAnimated().add(implodeTriD);

//		Camera c = ((TermesSpherePlugin)psl.getController().getPlugin(TermesSpherePlugin.class)).getTermes().getTermesCamera();
//		KeyFrameAnimatedBean<Camera> ab = new KeyFrameAnimatedBean<Camera>(c);
//		ab.setName("cameraBean");
//		apl.getAnimated().add(ab);					

	}
	
	
//	KeyAdapter ka = null;
//	public KeyAdapter getKeyAdapter() {
//		if (ka == null)	{
//			ka = new KeyAdapter()	{
//				boolean rotating = false;
//				boolean beyondInfinity = false;
//				public void keyPressed(KeyEvent e)	{ 
//					switch(e.getKeyCode())	{
//						
//					case KeyEvent.VK_H:
//						break;
//		
//				}
//		
//				}
//			};
//		}
//		return ka;
//	}

	public void endAnimation() {
		System.err.println("|in end animation");
//		if (helixCurve.getCount()<2) return;
//		PolygonalTubeFactory ptf = new PolygonalTubeFactory(helixCurve.getCurve(), 0);
//		ptf.update();
//		if (curveSGC != null) scene.removeChild(curveSGC);
//		curveSGC = ptf.getFramesSceneGraphRepresentation();
//		curveSGC.setName("frame field");
//		scene.addChild(curveSGC);
//		curveSGC.setGeometry(helixTube.getMesh());
	}

//	@Override
	public void startAnimation() {
		super.startAnimation();
//		helixTube.setMetric(Pn.ELLIPTIC);
		// we need to know where to position the initial cross section
		// we do this by figuring out the initial point P of the curve
		// which is the center of cell 9.  We then calculate the polar plane
		// of this point wrt the velocity state given by  (1 + (1/3)I) polaraxis
		// Finally, the polar point of this plane is the velocity vector at P.
		// 
//		double[] mat = listofTetrahedra[9].getTransformation().getMatrix();
//		double[] P = new Matrix(mat).getColumn(3);
//		double[] velstate = Rn.linearCombination(null, 1.0, pline, 1.0/3.0, line);
//		System.err.println("vel state = "+Rn.toString(velstate));
//		double[] PP = {P[3], P[0], P[1], P[2]};
//		double[] pl = PlueckerLineGeometry.nullPlane(null, velstate, PP);
//		double[] conjPlane = new double[]{pl[1], pl[2], pl[3], pl[0]};
//		double[] velvector = conjPlane; // in elliptic metric, polar operator is the "identity"
//		System.err.println("point = "+Rn.toString(P)+" null plane = "+Rn.toString(velvector)+" P.Pi = "+Pn.innerProduct(P, velvector, Pn.ELLIPTIC));
//		// sigh... the formula for the vector field of velstate at P doesn't compute; do it by hand
//		Biquaternion bq = Biquaternion.exp(null, axis, angleScrew, .01);
//		double[] mat3 = Biquaternion.matrixFromBiquaternion(null, bq);
//		double[] image = Rn.matrixTimesVector(null, mat3, P);
//		double[] velvector2 = Pn.normalize(null,Rn.subtract(null, image, P), Pn.ELLIPTIC);
//		System.err.println("velvector2 = "+Rn.toString(velvector2));
//		double[] imat = Rn.inverse(null, mat);
//		double[] vvPullback = Rn.matrixTimesVector(null, imat, velvector2);
//		double[] xyPlane2CurvePlane = Rn.times(null, 
//				Rn.times(null, listofTetrahedra[9].getTransformation().getMatrix(), 
//				P3.makeRotationMatrix(null, new double[]{0,0,1},vvPullback)),
//			P3.makeScaleMatrix(null, helixTubeRadius));
//		crossSection = Rn.matrixTimesVector(null,
//					xyPlane2CurvePlane,
//					Pn.homogenize(null, TubeUtility.octagonalCrossSection));
//		helixCurve.reset();
//		helixTube.reset();
//		curveSGC.setGeometry(helixTube.getMesh());
	}

	@Override
	public void setValueAtTime(double d) {
		super.setValueAtTime(d);
		if (isAnimating && tsp.getTermes().isUpdateOnRender())
			tsp.getTermes().update();
	}

	boolean[][] archive = new boolean[4][10];
	boolean[] visCat = {true, true, true, true};
	
	@Override
	public Component getInspector() {
		getContent();
		Box container = Box.createVerticalBox();
		scaleSlider = new TextSlider.Double("scale",  SwingConstants.HORIZONTAL, 0, 1.0, scaleShape);
	    scaleSlider.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				scaleShape = ((TextSlider) arg0.getSource()).getValue().doubleValue();
				update();	
			}
	    });
	    container.add(scaleSlider);
		tubeRadSlider = new TextSlider.DoubleLog("axis radius",  SwingConstants.HORIZONTAL, 0.001, .5, tubeRadius);
	    tubeRadSlider.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				tubeRadius = ((TextSlider) arg0.getSource()).getValue().doubleValue();
				updateAxisPairs();
			}
	    });
	    container.add(tubeRadSlider);
		final TextSlider beamRad = new TextSlider.DoubleLog("beam radius",
				SwingConstants.HORIZONTAL,.001,.3, beamRadius);
		beamRad.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				double t = ((TextSlider) arg0.getSource()).getValue().doubleValue();
				adjustBeamRadius(t);
				beams = WingedEdgeUtility.createBeamsOnEdges(beams, fundamentalDomainWE, null, (originalMetricFactor/metricFactor)*beamRadius, 6, 1 );
				update();
			}
			
		});
		container.add(beamRad);
		metricSlider = new TextSlider.DoubleLog("metric factor",  SwingConstants.HORIZONTAL, .01, 25.0, metricFactor);
	    metricSlider.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				metricFactor = ((TextSlider) arg0.getSource()).getValue().doubleValue();
				metricFactorChanged = true;
                update();
				
			}
	    });
	    container.add(metricSlider);
		impTriSlider = new TextSlider.Double("implode tri",  SwingConstants.HORIZONTAL,-1.0,1.0, implodeTri);
	    impTriSlider.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				implodeTri = ((TextSlider) arg0.getSource()).getValue().doubleValue();
                update();
				
			}
	    });
	    container.add(impTriSlider);
		impHexSlider = new TextSlider.Double("implode hex",  SwingConstants.HORIZONTAL,-1.0,1.0, implodeHex);
	    impHexSlider.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				// TODO Auto-generated method stub
				implodeHex = ((TextSlider) arg0.getSource()).getValue().doubleValue();
                update();
				
			}
	    });
	    container.add(impHexSlider);
	    container.add(sl.getInspector());
	    
	    Box vbox = Box.createVerticalBox();
		TitledBorder title = BorderFactory.createTitledBorder(
				BorderFactory.createEtchedBorder(), "Visibility");
		vbox.setBorder(title);
	    Box hbox = Box.createHorizontalBox();
	    container.add(vbox);
	    final JCheckBox[][] bloops = new JCheckBox[5][10];
	    final JCheckBox[] bloopsBig = new JCheckBox[4];
	    String[] labels = { "beams", "cell", "axis", "polar axis"};
	    String[] suffix = {"01","02","03","04","12","13","14","23","24","34"};
    	hbox = Box.createHorizontalBox();
    	vbox.add(hbox);
    	for (int j = 0; j<4; ++j) {
    		bloopsBig[j] = new JCheckBox(labels[j]);
    		bloopsBig[j].setSelected(visCat[j]);
    		hbox.add(bloopsBig[j]);
    		final int column = j;
    		bloopsBig[j].addActionListener(new ActionListener() {
				
					public void actionPerformed(ActionEvent arg0) {
					visCat[column] = (((JCheckBox)arg0.getSource()).isSelected());
					updateDisplay(column);
				}

				private void updateDisplay(int column) {
					if (visCat[column])	{
						for (int i = 0; i<10; ++i)	{
							bigArray[column][i].setVisible(archive[column][i]);
							bloops[column][i].setEnabled(true);
						}
					} else {
						for (int i = 0; i<10; ++i)	{
							archive[column][i] = bigArray[column][i].isVisible();
							bigArray[column][i].setVisible(false);
							bloops[column][i].setEnabled(false);
						}							
					}
				}
			});
    	}
	    for (int i = 0; i<10; ++i)	{
	    	hbox = Box.createHorizontalBox();
	    	vbox.add(hbox);
	    	for (int k = 0; k<4; ++k) {
	    		bloops[k][i] = new JCheckBox(labels[k]+(k<2 ? i : suffix[i]));
	    		bloops[k][i].setSelected(bigArray[k][i].isVisible());
				archive[k][i] = bigArray[k][i].isVisible();
	    		hbox.add(bloops[k][i]);
	    		final int row = i, column = k;
	    		bloops[k][i].addActionListener(new ActionListener() {
					
					public void actionPerformed(ActionEvent arg0) {
						bigArray[column][row].setVisible(((JCheckBox)arg0.getSource()).isSelected());
					}
				});
	    	}
	    }
	    return container;
	}

	protected void adjustBeamRadius(double t) {
		beamRadius = t;
//		beamRadius = (originalMetricFactor/metricFactor) * t;
	}

	private double[] randomino() {
		return new double[]{Math.random(), Math.random(), Math.random(), Math.random()};
	}

	transient 	double dangle = .005, time = 0.0, speed = Math.PI;
	transient 	double[] pt1 = {metricFactor, c,c,c}, 
			pt3 = {metricFactor,-c,-c,-c};
//	double[] pt2 = Pn.dragTowards(null, pt1, pt3, dangle * Math.PI/3.0, Pn.ELLIPTIC);
//	double[] mm = P3.makeScrewMotionMatrix(null, pt1, pt2, dangle * Math.PI, Pn.ELLIPTIC);
	transient 	double[] line, pline;
	transient 	Biquaternion axis, angle, angleRot, angleTrans, angleScrew, fullRot, fullTrans, fullScrew;
	//			IsometryAxis ia = new IsometryAxis(mm, Metric.ELLIPTIC);
	{
		line = PlueckerLineGeometry.lineFromPoints(null, pt1, pt3);
		Rn.normalize(line, line);
		pline = PlueckerLineGeometry.polarize(null, line, Pn.ELLIPTIC);
		Rn.normalize(pline, pline);
		System.err.println("screw motion: "+Rn.toString(pline));
		axis = new Biquaternion(pline, Metric.ELLIPTIC);
		angleRot = new Biquaternion(Metric.ELLIPTIC);
		angleRot.getRealPart().re =  0.0;
		angleRot.getDualPart().re = 0.0; //1.0/3.0;
		fullRot = Biquaternion.exp(null, axis, angleRot, speed);
		angleTrans = new Biquaternion(Metric.ELLIPTIC);
		angleTrans.getRealPart().re =  -1.0;
		angleTrans.getDualPart().re = 0;
		fullTrans = Biquaternion.exp(null, axis, angleTrans, speed);
		angleScrew = new Biquaternion(Metric.ELLIPTIC);
		angleScrew.getRealPart().re =  -1.0;
		angleScrew.getDualPart().re = 1.0/3.0;
		fullScrew = Biquaternion.exp(null, axis, angleScrew, speed);
	}
	
	public static void main(String[] argc)	{
//		Secure.setProperty(SystemProperties.AUTO_RENDER, "false");
		new TenCell().display();
	}

	transient private TextSlider impHexSlider;
	transient private TextSlider impTriSlider;
	AnimatedAdaptor anim1 = new AnimatedAdaptor()	{
		transient 	double[] keytimes = {1.6,4.8,8.0};
		@Override
		public void setValueAtTime(double t) {
			Biquaternion step = null;
			double[] mat = Rn.identityMatrix(4);
			if (t<=keytimes[0])	{
				t = AnimationUtility.hermiteInterpolation(t, 0, keytimes[0], 0, 1);
				step = Biquaternion.exp(null, axis, angleRot, 0.0*speed); //t*speed);			
				mat = Biquaternion.matrixFromBiquaternion(null, step);
			} else if (t <= keytimes[1]) {
				t = AnimationUtility.hermiteInterpolation(t, keytimes[0], keytimes[1], 0, 1);
				step = Biquaternion.exp(null, axis, angleTrans, t*speed);			
				mat = Rn.times(null, Biquaternion.matrixFromBiquaternion(null, fullRot), Biquaternion.matrixFromBiquaternion(null, step));
			} else if (t <= keytimes[2]) {
				t = AnimationUtility.hermiteInterpolation(t, keytimes[1], keytimes[2], 0, 1);
				step = Biquaternion.exp(null, axis, angleScrew, t*speed);			
				mat = Biquaternion.matrixFromBiquaternion(null, step);
				mat = Rn.times(null, Rn.times(null, Biquaternion.matrixFromBiquaternion(null, fullRot),
						Biquaternion.matrixFromBiquaternion(null, fullTrans)), Biquaternion.matrixFromBiquaternion(null, step));
			} 
			if (t <= 3.0) {
//				System.err.println("setting matrix");
				scene.getTransformation().setMatrix(mat);
			}
		}

		@Override
		public String getName() {
			return "TenCell-anim1";
		}

		@Override
		public void startAnimation() {
			isAnimating = true;
		}

		@Override
		public void endAnimation() {
			isAnimating = false;
		}

		
	};
	
	transient AnimatedAdaptor anim2 = new AnimatedAdaptor() {
//		@Override
		public void startAnimation() {
			super.startAnimation();
			// we need to know where to position the initial cross section
			// we do this by figuring out the initial point P of the curve
			// which is the center of cell 9.  We then calculate the polar plane
			// of this point wrt the velocity state given by  (1 + (1/3)I) polaraxis
			// Finally, the polar point of this plane is the velocity vector at P.
			// 
			double[] mat = listofTetrahedra[9].getTransformation().getMatrix();
			double[] P = new Matrix(mat).getColumn(3);
			double[] velstate = Rn.linearCombination(null, 1.0, pline, 1.0/3.0, line);
			System.err.println("vel state = "+Rn.toString(velstate));
			double[] PP = {P[3], P[0], P[1], P[2]};
			double[] pl = PlueckerLineGeometry.nullPlane(null, velstate, PP);
			double[] conjPlane = new double[]{pl[1], pl[2], pl[3], pl[0]};
			double[] velvector = conjPlane; // in elliptic metric, polar operator is the "identity"
			System.err.println("point = "+Rn.toString(P)+" null plane = "+Rn.toString(velvector)+" P.Pi = "+Pn.innerProduct(P, velvector, Pn.ELLIPTIC));
			// sigh... the formula for the vector field of velstate at P doesn't compute; do it by hand
			Biquaternion bq = Biquaternion.exp(null, axis, angleScrew, .01);
			double[] mat3 = Biquaternion.matrixFromBiquaternion(null, bq);
			double[] image = Rn.matrixTimesVector(null, mat3, P);
			double[] velvector2 = Pn.normalize(null,Rn.subtract(null, image, P), Pn.ELLIPTIC);
			System.err.println("velvector2 = "+Rn.toString(velvector2));
			double[] imat = Rn.inverse(null, mat);
			double[] vvPullback = Rn.matrixTimesVector(null, imat, velvector2);
			double[] xyPlane2CurvePlane = Rn.times(null, 
					Rn.times(null, listofTetrahedra[9].getTransformation().getMatrix(), 
					P3.makeRotationMatrix(null, new double[]{0,0,1},vvPullback)),
				P3.makeScaleMatrix(null, helixTubeRadius));
			crossSection = Rn.matrixTimesVector(null,
						xyPlane2CurvePlane,
						Pn.homogenize(null, TubeUtility.octagonalCrossSection));
//			helixCurve.reset();
//			helixTube.reset();
//			curveSGC.setGeometry(helixTube.getMesh());
		}

	};

}
