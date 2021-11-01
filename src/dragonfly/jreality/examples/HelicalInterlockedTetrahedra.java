/*
 * Created on Dec 17, 2007
 *
 */
package dragonfly.jreality.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.jreality.texture.RopeTextureFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.math.p5.PlueckerLineGeometry;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.QuadMeshFactory;
import de.jreality.geometry.TubeFactory;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultLineShader;
import de.jreality.shader.DefaultPointShader;
import de.jreality.shader.DefaultPolygonShader;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.core.DiscreteGroup;
import de.jtem.discretegroup.core.DiscreteGroupElement;
import de.jtem.discretegroup.core.DiscreteGroupSceneGraphRepresentation;
import de.jtem.discretegroup.core.DiscreteGroupUtility;
import de.jtem.jrworkspace.plugin.Controller;

public class HelicalInterlockedTetrahedra  extends Assignment {

	double phi = (Math.sqrt(5.0)-1.0)/2.0;
	double phi2 = phi*phi;
	double[] axis = {phi, 0.0, 1.0};
	double radius = .10, translation = 0.0;
	static double k2 = 1/Math.sqrt(3), k3 = Math.acos(1/3.0);
	double beginAngle = 0, fixedAngle = -Math.PI/2 - k3/2;
	private int halfTurns = 6,
		texU = 8, texV = 15;
	double ribbonThickness = .5,
		thicknessTaper = 0.0,
		pitchFactor = 1.0;
	static double[]  specialFrame = Rn.identityMatrix(4);
	private double ribbonWidthFactor = .5;
	private int numSamples = 20, actualNumTetras = 1, numTetras = 1;
	private QuadMeshFactory quadMeshFactory;
	private SceneGraphComponent curveSGC,
			ringsSGC,
			ribbonGeom, 
			framesSGC,
			fundDom,
			oneCopySGC;
	String[] words = {"","a","aa","b","ba","baa","bb","bba","bbaa","aab","aaba","aabaa"};
	int[] color3Indices = {0,2,1,1,0,2,2,1,0,2,1,0},
		color4Indices = {0,0,0,1,1,1,2,2,2,3,3,3},
		texOrient3 = {1,1,1,  -1,-1,-1,  1,1,-1,  -1,-1,1};
	double colorFactor = .0, colorSatFactor=.8;
	boolean showFrames = false,
		oneCopyOnly = false,
		showRibbons = true,
		showCurves = true,
		showCollars = false,
		showTextures = true,
		colorPerTetra = false,
		textureHack = false,
		doNewton = true;
	private DiscreteGroupElement[] tetraGens;
	private DiscreteGroupElement[] tetraEls;
	private DiscreteGroupSceneGraphRepresentation dgsgr;
	
	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		world.addChild(getInterlockedTetrahedra());
		world.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		return world;
	}

	private SceneGraphComponent getInterlockedTetrahedra() {
		double[] line1 = PlueckerLineGeometry.lineFromPoints(null, new double[]{1,1,1,1}, new double[]{-1,-1,1,1});
		double[] line2 = PlueckerLineGeometry.lineFromPoints(null, new double[]{-1,1,1,1}, new double[]{phi+1,0,phi,1});
		double thickness = .2;
		
		double r = .5 * PlueckerLineGeometry.distanceBetweenLines(line1, line2);
		radius = (1-thickness/2.0)*r;
		interlockedTetras = SceneGraphUtility.createFullSceneGraphComponent();
		SceneGraphComponent oneTetra = getRibbonedTetra();
		double[] fiveFold = new double[16], acc = Rn.identityMatrix(4);
		MatrixBuilder.euclidean().rotate(2*Math.PI/5, axis).assignTo(fiveFold);
		for (int i = 0; i< 5; ++i)	{
			SceneGraphComponent cubekit = SceneGraphUtility.createFullSceneGraphComponent();
			cubekit.getTransformation().setMatrix(acc);
//			cubekit.getAppearance().setAttribute("polygonShader.diffuseColor", colors[i]);
			cubekit.addChild(oneTetra);
			interlockedTetras.addChild(cubekit);
			Rn.times(acc,acc,fiveFold);
		}
		setupAppearances();
		updateNumTetras();
		return interlockedTetras; //oneTetra;
	}
	private void updateNumTetras() {
		for (int i = 0; i<5; ++i)	{
			SceneGraphComponent child = interlockedTetras.getChildComponent(i);
			child.setVisible(i < numTetras);
		}
	}

	public SceneGraphComponent getRibbonedTetra()	{
		SceneGraphComponent ribbonTetra = SceneGraphUtility.createFullSceneGraphComponent("ribbon tetra");
		curveSGC = SceneGraphUtility.createFullSceneGraphComponent("curve");
		{
			DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(curveSGC.getAppearance(), true);
			dgs.setShowLines(true);
			//dgs.setShowPoints(false);
			DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
			dls.setTubeDraw(true);
			dls.setTubeRadius(.01);
			DefaultPolygonShader dpls = (DefaultPolygonShader) dls.createPolygonShader("default");
			dpls.setDiffuseColor(Color.white);
			DefaultPointShader dps = (DefaultPointShader) dgs.createPointShader("default");
			dps.setSpheresDraw(true);
			dps.setPointRadius(.005);
			dps.setDiffuseColor(Color.red);
		}
		curveSGC.setGeometry(IndexedLineSetUtility.createCurveFromPoints(getCurve(halfTurns,numSamples), false));
		curveSGC.setVisible(showCurves);
		fundDom = SceneGraphUtility.createFullSceneGraphComponent("fundDom");
		ribbonGeom = SceneGraphUtility.createFullSceneGraphComponent("helix");
		ribbonGeom.setVisible(showRibbons);
		{
			DefaultGeometryShader dgs = 
				ShaderUtility.createDefaultGeometryShader(ribbonGeom.getAppearance(), true);
//			dgs.setShowFaces(true);//(false); //
			dgs.setShowLines(false);//(true);	//
			dgs.setShowPoints(false);
			DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
			dls.setTubeDraw(true);//(false);	//
			dls.setTubeRadius(.01); //setLineWidth(1.0); //
			dls.setDiffuseColor(Color.black); //Color.yellow);//
			DefaultPolygonShader dpos = (DefaultPolygonShader) dls.getPolygonShader();
			dpos.setDiffuseColor(Color.black); //Color.yellow);//
			DefaultPointShader dps = (DefaultPointShader) dgs.createPointShader("default");
			dps.setSpheresDraw(true);
			dps.setPointRadius(.015);
			dps.setDiffuseColor(Color.red);
//			dpos.setDiffuseColor(Color.white);
//			SimpleTextureFactory stf = new SimpleTextureFactory();
//			stf.setType(TextureType.WEAVE);
//			stf.setColor(0, new Color(0,0,0,0));
//			stf.update();
//			ImageData id = stf.getImageData();
//			Texture2D tex2d = TextureUtility.createTexture(ribbonGeom.getAppearance(), "polygonShader", id);
//			MatrixBuilder.euclidean().scale(10,30,1).assignTo(tex2d.getTextureMatrix());
		}
		ringsSGC = SceneGraphUtility.createFullSceneGraphComponent("circle");
		ringsSGC.setGeometry(IndexedLineSetUtility.circle(50));
		{
			DefaultGeometryShader dgs = 
				ShaderUtility.createDefaultGeometryShader(ringsSGC.getAppearance(), true);
			dgs.setShowFaces(false);
			dgs.setShowLines(true);
			dgs.setShowPoints(false);
			DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
			dls.setTubeDraw(false);
			dls.setTubeRadius(.01);
			dls.setDiffuseColor(Color.green);
			DefaultPointShader dps = (DefaultPointShader) dgs.createPointShader("default");
			dps.setSpheresDraw(true);
			dps.setPointRadius(.015);
			dps.setDiffuseColor(Color.orange);
		}
		ringsSGC.setVisible(showCollars);
		framesSGC = SceneGraphUtility.createFullSceneGraphComponent("frames");
		framesSGC.setVisible(showFrames);
		fundDom.addChildren(ringsSGC, curveSGC, ribbonGeom, framesSGC);
		updateGeometry();
		
		SceneGraphComponent threeFoldSGC = SceneGraphUtility.createFullSceneGraphComponent("3fold");
		for (int i = 0; i<3; ++i)	{
			SceneGraphComponent child = SceneGraphUtility.createFullSceneGraphComponent("3fold"+i);
			child.addChild(curveSGC);
			threeFoldSGC.addChild(child);
			MatrixBuilder.euclidean().rotate(2*i*Math.PI/3, new double[]{1,1,1}).assignTo(child);
		}
		DiscreteGroup tetraGroup = new DiscreteGroup();
		tetraGroup.setDimension(3);
		tetraGroup.setMetric(Pn.EUCLIDEAN);
		tetraGroup.setFinite(true);
		tetraGens = new DiscreteGroupElement[3];
		tetraGens[0] = new DiscreteGroupElement();
		tetraGens[0].setWord("a");
		MatrixBuilder.euclidean().rotate(2*Math.PI/3, new double[]{1,1,1}).assignTo(tetraGens[0].getArray());
		tetraGens[1] = new DiscreteGroupElement();
		tetraGens[1].setWord("b");
		MatrixBuilder.euclidean().rotate(2*Math.PI/3, new double[]{-1,1,1}).assignTo(tetraGens[1].getArray());
		tetraGens[2] = new DiscreteGroupElement();
		tetraGens[2].setWord("c");
		MatrixBuilder.euclidean().rotate(2*Math.PI/5, axis).assignTo(tetraGens[2].getArray());
//		tetraGroup.setGenerators(gens);
		tetraEls = new DiscreteGroupElement[12*1];
		for (int i= 0; i<12; ++i)	{
			tetraEls[i] = DiscreteGroupUtility.getElementForWord(tetraGens, words[i]);
			tetraEls[i].setColorIndex(i); //indices[i]);
		}
		for (int j = 1; j<1; ++j)	{
			for (int i= 0; i<12; ++i)	{
				tetraEls[j*12+i] = new DiscreteGroupElement(tetraEls[(j-1)*12+i]);
				tetraEls[j*12+i].getMatrix().multiplyOnLeft(tetraGens[2].getArray());
			}
		}
		tetraGroup.setElementList(tetraEls);
		
		tetraSGC = SceneGraphUtility.createFullSceneGraphComponent("tetra");
		{
			DefaultGeometryShader dgs = 
				ShaderUtility.createDefaultGeometryShader(tetraSGC.getAppearance(), true);
			dgs.setShowFaces(false);
			dgs.setShowLines(true);
			dgs.setShowPoints(true);
			DefaultLineShader dls = (DefaultLineShader) dgs.createLineShader("default");
			dls.setTubeDraw(true);
			dls.setTubeRadius(.01);
			dls.setDiffuseColor(Color.green);
			DefaultPointShader dps = (DefaultPointShader) dgs.createPointShader("default");
			dps.setSpheresDraw(true);
			dps.setPointRadius(.015);
			dps.setDiffuseColor(Color.orange);
		}
		tetraSGC.setGeometry(Primitives.tetrahedron());
		tetraSGC.setVisible(false);
		MatrixBuilder.euclidean().scale(-1).assignTo(tetraSGC);
		ribbonTetra.addChild(tetraSGC);

		oneCopySGC = SceneGraphUtility.createFullSceneGraphComponent("oneCopy");
		oneCopySGC.addChildren(fundDom, threeFoldSGC);
		ribbonTetra.addChild(oneCopySGC);
		dgsgr = new DiscreteGroupSceneGraphRepresentation(tetraGroup, false);
		dgsgr.setWorldNode(fundDom);
		setupRealColors();
		dgsgr.setElementList(tetraEls);
		dgsgr.update();
		updateOneCopy();
		ribbonTetra.addChild(dgsgr.getRepresentationRoot());
		return ribbonTetra;
		
	}

	private void updateOneCopy() {
		oneCopySGC.setVisible(oneCopyOnly);
		dgsgr.getRepresentationRoot().setVisible(!oneCopyOnly);
	}

	Color[] baseColors = {new Color(200, 50, 0), 
			new Color(00,150,200), 
			new Color(200,200,0), 
			new Color(200, 200, 200),new Color(0f, .65f, .8f)};
	Color[] colors = new Color[5];
    Color[] colors2 = {
    		new Color(255,55,20), new Color(100,255,20), new Color(50,50,255),
    		new Color(255,200,20), new Color(255,20,255), new Color(20,255,255)};
    Color[] brightcolors = new Color[6];
	private void setupRealColors()	{
		System.err.println("color sat factor is "+colorSatFactor);
		for (int i = 0; i<5; ++i)	{
		    	float[] rgb = colors2[i].getRGBColorComponents(null);
		    	for (int j = 0; j<3; ++j) rgb[j] = (float) Math.pow(rgb[j], .4);
		    Color tmp = new Color(rgb[0], rgb[1], rgb[2]);
			colors[i] = AnimationUtility.linearInterpolation(Color.white, tmp, colorSatFactor);
		}
	}
	private void setupAppearances() {
		aplists = new Appearance[actualNumTetras*12];
		int[] indices = (halfTurns%2 == 0) ? color4Indices : color3Indices;
		for (int i = 0; i<actualNumTetras; ++i)	{
			float[] scale = {.8f, 1f, 1.2f, .6f};
			for (int j = 0; j<12; ++j)	{
				int k = 12*i+j;
				Color newcolor = null;
//				if (actualNumTetras > 1)	{
//					Color thiscolor = colors[i];
//					float[] rgba = thiscolor.getRGBComponents(null);
//					float jitter = (float) (Math.random()*colorFactor);
//					if (j<3) {
//						rgba[j] += jitter;
//					}
//					for (int m = 0; m<3; ++m) {
//						rgba[m] *= scale[j];
//						if (rgba[m] > 1f) rgba[m] = 1f;
//					}
//					newcolor = new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
////					System.err.println("Color = "+newcolor);					
//				} else 
					newcolor = colors[indices[j]]; //baseColors[j];
				aplists[k] = new Appearance();
				Appearance apk = aplists[k];
				if (!colorPerTetra) {
					if (!showTextures) {
						Object target = newcolor;
						apk.setAttribute("lineShader.polygonShader.diffuseColor", target);
						apk.setAttribute("pointShader.polygonShader.diffuseColor", target);
						apk.setAttribute("polygonShader.diffuseColor", target);
					} else {
						//texapps[i] = new Appearance();
						getTexture2D(newcolor, apk);
					}					
				}
			}
		}
		updateTextureMatrix();
//		if (!colorPerTetra) {
			dgsgr.setAppList(aplists);
			oneCopySGC.setAppearance(aplists[0]);
//		} else {
//			dgsgr.setAppList(null);
//			oneCopySGC.setAppearance(null);
//		}
		for (int i =0; i<5; ++i)	{
			Appearance ap = new Appearance();
			interlockedTetras.getChildComponent(i).setAppearance(ap);
//			System.err.println("color = "+colors[i]);
			if (showTextures) getTexture2D(colors[i], ap);
			else {
				ap.setAttribute("lineShader.polygonShader.diffuseColor", colors[i]);
				ap.setAttribute("pointShader.polygonShader.diffuseColor", colors[i]);
				ap.setAttribute("polygonShader.diffuseColor", colors[i]);				
			}
			//else ap.setAttribute("texture2d", Appearance.INHERITED);
		}
		dgsgr.update();
	}

	private void updateTextureMatrix() {
		for (int k = 0; k<12; ++k)	{
			Matrix foo = new Matrix();
			double utlate = (texOrient3[k] == 1 ? (textureHack ? 1/3.0 : .25) : 0.0);
			MatrixBuilder.euclidean().scale(1.0).scale(texU, texV,1).
				translate(utlate, 0,0).
				scale(texOrient3[k],texOrient3[k],1).
				//translate(.25,0,0).
				assignTo(foo);
			aplists[k].setAttribute("polygonShader.texture2d:textureMatrix", foo);	
		}
	}

	private Texture2D getTexture2D(Color newcolor, Appearance apk) {
		RopeTextureFactory textureFactory = new RopeTextureFactory(apk);
		textureFactory.setN(Math.sqrt(2)*3);
		textureFactory.setM(Math.sqrt(2)*3);
		textureFactory.setAngle(Math.PI/4);
		System.err.println("creating rope texture with color "+newcolor);
		textureFactory.setBand1color(newcolor);
		textureFactory.setGapcolor(new Color(100,100,100,255));
		textureFactory.setBand2color(new Color(255,255,255));
		textureFactory.setBandwidth(.9);
		textureFactory.setShadowwidth(.1);
		textureFactory.update();
		Texture2D tex2d = textureFactory.getTexture2D(); //TextureUtility.createTexture(aplist[k], "polygonShader", id);
		return tex2d;
	}
	private void updateGeometry() {
		double totalangle = (halfTurns)*(Math.PI );
		double[][] frames = getFrames(halfTurns, numSamples);
		Matrix foo = new Matrix(specialFrame);
		MatrixBuilder.euclidean(foo).scale(radius, radius, 1).assignTo(ringsSGC);
		double totalDistance = getTotalDistanceForTurn(halfTurns); //getStraightDistanceForTurn(halfTurns);
		fixedAngle = getFixedAngleForTurn(halfTurns);
		// the ribbon width is input as a fraction of the total radius of the helix
		double ribbonWidth = 2*radius*ribbonWidthFactor;
		double x = Math.sqrt(radius*radius-ribbonWidth*ribbonWidth/4),
			factor = (1-ribbonThickness),
			yfactor = (1-thicknessTaper*ribbonThickness);
		// use the middle of the swept out shape to determine the pitch
		double[][] xsection = {
				{x, ribbonWidth/2, 0, 1},
				{x,-ribbonWidth/2,0,1},
				{factor*x, yfactor*(-ribbonWidth/2.0), 0, 1},
				{factor*x, yfactor*(ribbonWidth/2.0),0,1},
				{x, ribbonWidth/2, 0, 1}};
		double pitchAngle = Math.atan2(x*totalangle, totalDistance);
		System.err.println("Pitch angle= "+pitchAngle);
		double[] mat = MatrixBuilder.euclidean().rotateX(-pitchFactor*pitchAngle).getArray();
		Rn.matrixTimesVector(xsection[0], mat, xsection[0]);
		Rn.matrixTimesVector(xsection[1], mat, xsection[1]);
		Rn.matrixTimesVector(xsection[4], mat, xsection[4]);
		pitchAngle = Math.atan2(factor*x*totalangle, totalDistance);
		mat = MatrixBuilder.euclidean().rotateX(-pitchFactor*pitchAngle).getArray();
		Rn.matrixTimesVector(xsection[2], mat, xsection[2]);
		Rn.matrixTimesVector(xsection[3], mat, xsection[3]);
		int length = frames.length;
		double[] anglelist = new double[length];
		double angle = fixedAngle+beginAngle;
		double dangle = (totalangle/(length-1.0));
		for (int i = 0; i<length; ++i) {
			anglelist[i] = angle;
			angle += dangle;
		}
		int numsteps = 5;
		if (doNewton)
			for (int i = 0; i<numsteps; ++i)
				doOneStep(totalangle, frames, x,  anglelist, i%2);
		if (showFrames)	{
			ribbonGeom.setGeometry(null);
			SceneGraphUtility.removeChildren(framesSGC);
			SceneGraphComponent axes = TubeFactory.getXYZAxes();
			MatrixBuilder.euclidean().scale(.15).assignTo(axes);
			for (int i = 0; i<length; ++i) {
				double[] zrot = MatrixBuilder.euclidean().rotateZ(anglelist[i]).getArray();
				double[] finalFrame = Rn.times(null, frames[i], zrot);
				SceneGraphComponent oneFrame = new SceneGraphComponent(""+i);
				oneFrame.addChild(axes);
				new Matrix(finalFrame).assignTo(oneFrame);
				framesSGC.addChild(oneFrame);
			}
			
		} //else 
		{
			double[][][] verts = new double[length][xsection.length][];
			for (int i = 0; i<length; ++i) {
				double[] zrot = MatrixBuilder.euclidean().rotateZ(anglelist[i]).getArray();
				double[] finalFrame = Rn.times(null, frames[i], zrot);
				verts[i] = Rn.matrixTimesVector(null, finalFrame, xsection);
			}
			quadMeshFactory = new QuadMeshFactory();
			quadMeshFactory.setULineCount(xsection.length);
			quadMeshFactory.setVLineCount(length);
			quadMeshFactory.setVertexCoordinates(verts);
			quadMeshFactory.setGenerateFaceNormals(true);
			quadMeshFactory.setGenerateTextureCoordinates(true);
			quadMeshFactory.setGenerateEdgesFromFaces(true);
			//quadMeshFactory.setGenerateVertexNormals(true);
			quadMeshFactory.update();	
			// hack! recalculate the texture coordinates
			if (textureHack)	{
				double[][] tc = quadMeshFactory.getIndexedFaceSet().getVertexAttributes(Attribute.TEXTURE_COORDINATES).toDoubleArrayArray(null);
				int n = tc.length, f = tc[0].length;
				double[] tchack = {0.0, 1/6.0, 1/2.0, 2/3.0,1.0};
				for (int row = 0; row < length; ++row)	{
					for (int col = 0; col < xsection.length; ++col)	{
						int ind = row*xsection.length + col;
						tc[ind][0] = tchack[col];
					}
				}
				quadMeshFactory.setGenerateTextureCoordinates(false);
				quadMeshFactory.setVertexAttribute(Attribute.TEXTURE_COORDINATES, tc);
				quadMeshFactory.update();				
			}
			ribbonGeom.setGeometry(quadMeshFactory.getGeometry());
			MatrixBuilder.euclidean().translate(translation,translation,translation).assignTo(ribbonGeom);
			quadMeshFactory.getGeometry().setGeometryAttributes(GeometryUtility.QUAD_MESH_SHAPE, null);
			Object bar = quadMeshFactory.getGeometry().getGeometryAttributes(GeometryUtility.QUAD_MESH_SHAPE);
			System.err.println("quad mesh attr = "+bar);
//			IndexedFaceSet ribb = quadMeshFactory.getIndexedFaceSet();
//			ribbonGeom.addChild(IndexedFaceSetUtility.displayFaceNormals(ribb, .1, Pn.EUCLIDEAN));
		}
		curveSGC.setGeometry(IndexedLineSetUtility.createCurveFromPoints(getCurve(halfTurns,numSamples), false));
	}

	private void doOneStep(double totalangle, double[][] frames, double x,
			double[] anglelist, int which) {
		{
		int length = anglelist.length;
		double dangle = (totalangle/(length-1.0));
		double[] singlePoint = {x,0,0,1};
		double[][] points = new double[length][4],
		corePoints = new double[length][4],
		dpoints = new double[length][4];
		double[] newtonDelta = new double[length];
		double da = dangle/10;
		for (int i = 0; i<length; ++i) {
			double[] zrot = MatrixBuilder.euclidean().rotateZ(anglelist[i]).getArray();
			double[] zrot2 = MatrixBuilder.euclidean().rotateZ(anglelist[i]+da).getArray();
			double[] finalFrame = Rn.times(null, frames[i], zrot);
			points[i] = Rn.matrixTimesVector(null, finalFrame, singlePoint);
			Matrix frame = new Matrix(finalFrame);
			corePoints[i] = frame.getColumn(3);
			Rn.times(finalFrame, frames[i], zrot2);
			dpoints[i] = Rn.matrixTimesVector(null, finalFrame, singlePoint);
		}
		double totalD = 0.0;
		double lambda = 1.0;
		for (int i = 1; i<length-1; ++i)	{
			double[] plane = P3.planeFromPoints(null, points[i-1], corePoints[i], points[i+1]);
			double d = Rn.innerProduct(points[i], plane);
			double d2 = Rn.innerProduct(dpoints[i], plane);
			//double slope = (d2-d)/da;
			newtonDelta[i] = (d * da)/(d2-d);
			if ((i%2) == which) anglelist[i] += -lambda * newtonDelta[i];
			System.err.println("newtonDelta = "+newtonDelta[i]);
			totalD += Pn.distanceBetween(points[i], points[i-1], Pn.EUCLIDEAN);
		}
		System.err.println("Total distance = "+totalD);
		}
	}

	private static double getStraightDistanceForTurn(int k)	{
		if (k==0) return 0.0;
		double k1 = ((k-1)*Math.PI/(3*Math.sqrt(3)));
		double d = Math.sqrt(2)*(k1/(k1+1));
		return d;
	}
	
	private static double getTotalDistanceForTurn(int k)	{
		double d = getStraightDistanceForTurn(k);
		double r = (Math.sqrt(2)-d)/Math.sqrt(3);
		double lc = r*Math.PI/3;
		double length = d + lc;
		return length;
	}

	private double getFixedAngleForTurn(int turns) {
		double angle = (1-turns)*Math.PI/2.0 - k3/2;
		return angle;
	}

	protected static double[][] getCurve(int k, int numSamples)	{
		int nn = 2*numSamples-1;
		double[][] verts = new double[nn][];
		double[][] frames = getFrames(k, numSamples);
		for (int i = 0; i<nn; ++i)	{
			Matrix tmp = new Matrix(frames[i]);
			verts[i] = tmp.getColumn(3);
		}
		return verts;
	}

	protected static double[][] getFrames(int nTurns, int numSamples)	{
		int nn = 2*numSamples-1;
		double[][] verts = new double[nn][],
			tangents = new double[nn][], 
			normals = new double[nn][],
			frames = new double[nn][16];
		double d = getStraightDistanceForTurn(nTurns);
		double r = (Math.sqrt(2)-d)/Math.sqrt(3);
		double length = getTotalDistanceForTurn(nTurns);
		double ls = d/length;
		double[] triCenter = {1/3.0, 1/3.0, 1/3.0,1};
		double[] c2vVector = new double[]{-4/3.0, 2/3.0, 2/3.0, 0};
		double sd = d/Math.sqrt(2.0);
		double[] roundingCenter = Rn.add(null, triCenter,
				Rn.times(null, sd, c2vVector)); //){d, -k2*d, 0,1};
		System.err.println("center = "+Rn.toString(roundingCenter));
		// we do our work in the x-y plane with vertical axis z,
		// and the following frame qua isometry maps this work space to the actual model space
		// of the tetrahedron
		double[] xdir = {-1,0,1,0}, ydir = {1,-2,1,0}, zdir = {1,1,1,0};
		Rn.setToLength(xdir, xdir, 1);
		Rn.setToLength(ydir, ydir, 1);
		Rn.setToLength(zdir, zdir, 1);
		double[] tmp = new double[16];
		// where the straight section transforms into the curved section
		// special frame is used to transform some geometry from its natural coord system
		// to the tetrahedron we're using here.
		specialFrame = Rn.transpose(null, new double[]{
				ydir[0], ydir[1], ydir[2], ydir[3],
				zdir[0], zdir[1], zdir[2], zdir[3],
				xdir[0], xdir[1], xdir[2], xdir[3],
				-sd, 1, sd, 1
		});
		for (int i = 0; i<numSamples; ++i)	{
			double t = i/(numSamples-1.0);
			if (t < ls)	{	// straightaway; it's important the test isn't t <= ls -- boundary bug
				double s = t/ls;
				verts[i] = new double[]{-s*sd, 1, s*sd, 1}; //{s*d, -k2,0,1};
				tangents[i] = xdir;
				normals[i] = ydir;
			} else {
				double s = (t - ls)*length/r;
				double cos = Math.cos(s);
				double sin = Math.sin(s);
//				double[] mm = MatrixBuilder.euclidean().rotate(s, new double[]{1,1,1}).getArray();
				double[] xt = //Rn.matrixTimesVector(null, mm, xdir);
					Rn.linearCombination(null, cos, xdir, sin, ydir);
				double[] yt = //Rn.matrixTimesVector(null, mm, ydir);
					Rn.linearCombination(null, sin, xdir, -cos, ydir);
				verts[i] = Rn.add(null, roundingCenter, Rn.times(null, r, yt));
				tangents[i] = xt;
				normals[i] = Rn.times(null, -1, yt);
			}
			System.arraycopy(verts[i], 0, tmp, 12, 4);
			System.arraycopy(tangents[i], 0, tmp, 8, 4);
			System.arraycopy(normals[i], 0, tmp, 0, 4);
			System.arraycopy(zdir, 0, tmp, 4, 4);
			Rn.transpose(frames[i], tmp);
//			if (i>0) {
//				System.err.println("dvert norm = "+Pn.norm(Rn.subtract(null, verts[i-1],verts[i]),0));
//			}
		}
		// continue the frames around the corner by a tricky combination of rotation and reflection
		double[] Lrefl = MatrixBuilder.euclidean().rotate(2*Math.PI/3.0, new double[]{1,1,1}).reflect(xdir).getArray();
		double[] Rrefl = MatrixBuilder.euclidean().scale(1,1,-1).getArray();
		for (int i = 0; i<numSamples-1; ++i)	{
			frames[nn-i-1] = Rn.times(null, Lrefl, 
					Rn.times(null, frames[i], Rrefl));
		}
		return frames;
	}

	private TextSlider.Integer RSlider;
	private TextSlider.Integer numSlider, 
		numTetraSlider,
		texUSlider,
		texVSlider;
	private TextSlider.Double phaseSlider,
		thickSlider,
		widthSlider,
		colorSlider,
		colorSatSlider,
		radiusSlider,
		transSlider,
		taperSlider,
		pitchSlider;
	private SceneGraphComponent tetraSGC;
	private SceneGraphComponent interlockedTetras;
	private Appearance[] aplists;
	@Override
	public  Component getInspector() {
		Box container = Box.createVerticalBox();
		Box vbox = Box.createVerticalBox();
		container.add(vbox);
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "Visibility")));

		final JCheckBox oneCopyB = new JCheckBox("1 copy only");
		oneCopyB.setSelected(oneCopyOnly);
		vbox.add(oneCopyB);
		oneCopyB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				oneCopyOnly = oneCopyB.isSelected();
				updateOneCopy();
			}
			
		});
		
		final JCheckBox newtonCV = new JCheckBox("do Newton");
		newtonCV.setSelected(doNewton);
		vbox.add(newtonCV);
		newtonCV.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				doNewton = newtonCV.isSelected();
				updateGeometry();
			}
			
		});
		
		final JCheckBox showHelixB = new JCheckBox("show helices");
		showHelixB.setSelected(showRibbons);
		vbox.add(showHelixB);
		showHelixB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				showRibbons = showHelixB.isSelected();
				ribbonGeom.setVisible(showRibbons);
			}
			
		});
		
		final JCheckBox showCurveB = new JCheckBox("show core");
		showCurveB.setSelected(showCurves);
		vbox.add(showCurveB);
		showCurveB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				showCurves = showCurveB.isSelected();
				curveSGC.setVisible(showCurves);
			}
			
		});
		
		final JCheckBox showCollarsB = new JCheckBox("show collars");
		showCollarsB.setSelected(showCollars);
		vbox.add(showCollarsB);
		showCollarsB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				showCollars = showCollarsB.isSelected();
				ringsSGC.setVisible(showCollars);
			}
			
		});
		
		final JCheckBox showFramesB = new JCheckBox("show frames");
		showFramesB.setSelected(showCollars);
		vbox.add(showFramesB);
		showFramesB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				showFrames = showFramesB.isSelected();
				framesSGC.setVisible(showFrames);
			}
			
		});
		
		final JCheckBox showTexturesB = new JCheckBox("show texture");
		showTexturesB.setSelected(showTextures);
		vbox.add(showTexturesB);
		showTexturesB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				showTextures = showTexturesB.isSelected();
				setupAppearances();
			}
			
		});
		
		final JCheckBox colorPerTB = new JCheckBox("colors per tetra");
		showTexturesB.setSelected(colorPerTetra);
		vbox.add(colorPerTB);
		colorPerTB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent arg0) {
				colorPerTetra = colorPerTB.isSelected();
				setupAppearances();
			}
			
		});
		
		Box parametersBox = Box.createVerticalBox();
		container.add(parametersBox);
		
		parametersBox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "Parameters")));

		RSlider = new TextSlider.Integer("halfturns",
				SwingConstants.HORIZONTAL, 0, 16, halfTurns);
		RSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				halfTurns = RSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(RSlider);
		
		numSlider = new TextSlider.Integer("samples",
				SwingConstants.HORIZONTAL, 2, 200, numSamples);
		numSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				numSamples = numSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(numSlider);
		
		numTetraSlider = new TextSlider.Integer("number tetras",
				SwingConstants.HORIZONTAL,1, 5, numTetras);
		numTetraSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				numTetras = numTetraSlider.getValue();
				updateNumTetras();
			}
		});
		parametersBox.add(numTetraSlider);
		
		texUSlider = new TextSlider.Integer("tex U",
				SwingConstants.HORIZONTAL, 1, 30, texU);
		texUSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				texU = texUSlider.getValue();
				setupAppearances();
			}
		});
		parametersBox.add(texUSlider);
		
		texVSlider = new TextSlider.Integer("tex V",
				SwingConstants.HORIZONTAL, 1, 50, texV);
		texVSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				texV = texVSlider.getValue();
				setupAppearances();
			}
		});
		parametersBox.add(texVSlider);
		
		radiusSlider = new TextSlider.Double("radius",
				SwingConstants.HORIZONTAL, 0, 1, radius);
		radiusSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				radius = radiusSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(radiusSlider);
		
		transSlider = new TextSlider.Double("translation",
				SwingConstants.HORIZONTAL, -1, 1, translation);
		transSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				translation = transSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(transSlider);
		
		thickSlider = new TextSlider.Double("thickness",
				SwingConstants.HORIZONTAL, 0, 1, ribbonThickness);
		thickSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ribbonThickness = thickSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(thickSlider);
		
		
		taperSlider = new TextSlider.Double("taper factor",
				SwingConstants.HORIZONTAL, -1,1, thicknessTaper);
		taperSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				thicknessTaper = taperSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(taperSlider);
		
		phaseSlider = new TextSlider.Double("phase",
				SwingConstants.HORIZONTAL, -Math.PI, Math.PI, beginAngle);
		phaseSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				beginAngle = phaseSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(phaseSlider);
		
		widthSlider = new TextSlider.Double("width",
				SwingConstants.HORIZONTAL, 0, 1, ribbonWidthFactor);
		widthSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ribbonWidthFactor = widthSlider.getValue();
				updateGeometry();
			}
		});
		parametersBox.add(widthSlider);
		
		colorSlider = new TextSlider.Double("hue range",
				SwingConstants.HORIZONTAL, 0, 1, colorFactor);
		colorSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				colorFactor = colorSlider.getValue();
				setupAppearances();
			}
		});
		parametersBox.add(colorSlider);
		
		colorSatSlider = new TextSlider.Double("color sat",
				SwingConstants.HORIZONTAL, 0, 1, colorSatFactor);
		colorSatSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				colorSatFactor = colorSatSlider.getValue();
				setupRealColors();
				setupAppearances();
			}
		});
		parametersBox.add(colorSatSlider);
		
//		container.setPreferredSize(new Dimension(300, 180));
//		JPanel panel = new JPanel();
//		panel.setName("Parameters");
//		panel.add(container);
//		panel.add(Box.createVerticalGlue());
		return container;
	}

	@Override
	public void restoreStates(Controller c) {
		halfTurns = c.getProperty(getClass(), "halfturns", halfTurns);
		RSlider.setValue(halfTurns);
		numSamples = c.getProperty(getClass(), "numSamples", numSamples);
		numSlider.setValue(numSamples);
		beginAngle = c.getProperty(getClass(), "beginAngle", beginAngle);
		phaseSlider.setValue(beginAngle);
		texU = c.getProperty(getClass(), "texU", texU);
		texUSlider.setValue(texU);
		texV = c.getProperty(getClass(), "texV", texV);
		texVSlider.setValue(texV);
		radius = c.getProperty(getClass(), "radius", radius);
		radiusSlider.setValue(radius);
		ribbonThickness = c.getProperty(getClass(), "ribbonThickness", ribbonThickness);
		thickSlider.setValue(ribbonThickness);
		thicknessTaper = c.getProperty(getClass(), "thicknessTaper", thicknessTaper);
		taperSlider.setValue(thicknessTaper);
		ribbonWidthFactor = c.getProperty(getClass(), "ribbonWidthFactor", ribbonWidthFactor);
		widthSlider.setValue(ribbonWidthFactor);
		colorFactor = c.getProperty(getClass(), "colorFactor", colorFactor);
		colorSlider.setValue(colorFactor);
		colorSatFactor = c.getProperty(getClass(), "colorSatFactor", colorSatFactor);
		colorSatSlider.setValue(colorSatFactor);
//		super.restoreStates(c);
		updateGeometry();
		setupRealColors();
		setupAppearances();
	}

	@Override
	public void storeStates(Controller c) {
		c.storeProperty(getClass(), "halfturns", halfTurns);
		c.storeProperty(getClass(), "numSamples", numSamples);
		c.storeProperty(getClass(), "beginAngle", beginAngle);
		c.storeProperty(getClass(), "texU", texU);
		c.storeProperty(getClass(), "texV", texV);
		c.storeProperty(getClass(), "radius", radius);
		c.storeProperty(getClass(), "ribbonThickness", ribbonThickness);
		c.storeProperty(getClass(), "ribbonWidthFactor", ribbonWidthFactor);
		c.storeProperty(getClass(), "thicknessTaper", thicknessTaper);
		c.storeProperty(getClass(), "colorFactor", colorFactor);
		c.storeProperty(getClass(), "colorSatFactor", colorSatFactor);
//		super.storeStates(c);
	}

	@Override
	public void display(){
		super.display();
		Viewer viewer = jrviewer.getViewer();
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLOR, Color.white); //new Color(20,20,40));
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLORS, Appearance.INHERITED);
		
	}
	
	public static void main(String[] args) {
		new HelicalInterlockedTetrahedra().display();
	}

}
