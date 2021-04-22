/*
 * Created on Mar 9, 2011
 *
 */
package dragonfly.dynamen;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import charlesgunn.jreality.texture.SimpleTextureFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.jreality.viewer.PluginSceneLoader;
import charlesgunn.math.Biquaternion;
import charlesgunn.math.Biquaternion.Metric;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.QuadMeshFactory;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.Camera;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.util.CameraUtility;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;
import de.jtem.projgeom.PlueckerLineGeometry;

public class EarthMotion extends Assignment {


	int steps = 100;
	double R = 1.0;
	double w1 = 1.0;
	double w2 = 3.0 * w1; //366.25 * w1;
	double dd = 23.5*Math.PI/180.0;
	boolean finiteSphere = true,
		colorLines = true,
		showTexture = false,
		linesOnly = true;
	double sphereRadius = 3.0,
		texscale = 30.0,
		clipDistance = .1;
	int samplesPerLine = 8;
	SceneGraphComponent velStates = SceneGraphUtility.createFullSceneGraphComponent("velstates"),
		lemnoSGC = SceneGraphUtility.createFullSceneGraphComponent("lemniscateSurface"),
		lemno2SGC = SceneGraphUtility.createFullSceneGraphComponent("lemniscateSurface");
	SceneGraphComponent theLine = SceneGraphUtility.createFullSceneGraphComponent("theLine"),
		focalLineSGC =  SceneGraphUtility.createFullSceneGraphComponent("focalLines"),
		focalLines[] = new SceneGraphComponent[2],
		theSurface = SceneGraphUtility.createFullSceneGraphComponent("Surface");
	PointRangeFactory plLineFactory = new PointRangeFactory(),
		focalLineFac[] = new PointRangeFactory[2];
	QuadMeshFactory qmf = new QuadMeshFactory();
	Appearance textureAp = new Appearance(),
		noTextureAp = theSurface.getAppearance();

	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
		plLineFactory = new PointRangeFactory();
		focalLines[0] = SceneGraphUtility.createFullSceneGraphComponent("f0");
		focalLines[1] = SceneGraphUtility.createFullSceneGraphComponent("f1");
		focalLineSGC.addChildren(focalLines[0], focalLines[1]);
		focalLineSGC.getAppearance().setAttribute("lineShader.diffuseColor", Color.yellow);
		focalLineFac[0] = new PointRangeFactory();
		focalLineFac[1] = new PointRangeFactory();
		updateLines();
		focalLines[0].setGeometry(focalLineFac[0].getLine());
		focalLines[1].setGeometry(focalLineFac[1].getLine());
		theSurface.setGeometry(qmf.getQuadMesh());
		theSurface.setVisible(false);
		SimpleTextureFactory stf = new SimpleTextureFactory();
		stf.setType(SimpleTextureFactory.TextureType.WEAVE);
		stf.setColor(0, new Color(255, 255, 255, 0));
		stf.update();
		texture2d = TextureUtility.createTexture(textureAp, "polygonShader", stf.getImageData(), true);
		Matrix m = new Matrix();
		MatrixBuilder.euclidean().scale(texscale, texscale, 1).assignTo(m);
		texture2d.setTextureMatrix(m);
		textureAp.setAttribute("polygonShader.diffuseColor",Color.white);
		if (showTexture) theSurface.setAppearance(textureAp);

		qmf.getQuadMesh().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);			
		theLine.setGeometry(plLineFactory.getLine());
		theLine.getAppearance().setAttribute("lineShader.diffuseColor", Color.red);
		SceneGraphComponent circle = SceneGraphUtility.createFullSceneGraphComponent("circle");
		world.getAppearance().setAttribute("lineShader.diffuseColor", Color.white);
		world.getAppearance().setAttribute(CommonAttributes.TUBES_DRAW, false);
		circle.setGeometry(IndexedLineSetUtility.circle(steps, 0.0, 0.0,  R));
		world.addChildren(velStates, lemnoSGC, lemno2SGC, theLine, focalLineSGC, theSurface, circle);
		return world;
	}

	double[][][] sverts = new double[steps][][];
	PointRangeFactory[][] prfactory = new PointRangeFactory[2][steps];
	TextSlider[] pluckerSliders = new TextSlider[6];
	private void updateLines() {
		if (linesOnly)	{
			double[] dirV = new double[]{0, rawPluckerLine[0], rawPluckerLine[1], rawPluckerLine[2]},
				baseP = new double[]{1, rawPluckerLine[5], rawPluckerLine[4], rawPluckerLine[3]};
			pluckerLine = PlueckerLineGeometry.lineFromPoints(null, baseP, dirV);
		} else {
			pluckerLine = rawPluckerLine.clone();
		}
		Biquaternion bivel = new Biquaternion(pluckerLine, Metric.metricForCurvature(Pn.EUCLIDEAN));
		Biquaternion bivelaxis = Biquaternion.axisForBivector(null, bivel);
		double[] velaxis = Biquaternion.lineComplexFromBivector(null, bivelaxis);//axis);
		double[] vv  = PlueckerLineGeometry.permuteCoordinates(null, velaxis, new int[]{3,0,1,2}); //); //PlueckerLineGeometry.dualizeLine(null, velaxis)
//		Rn.setToLength(pluckerLine, pluckerLine, 1.0);
		pluckerMatrix = PlueckerLineGeometry.inducedP5ProjFromP3Proj(
				pluckerMatrix, PlueckerLineGeometry.lineToSkewMatrix(null, pluckerLine));
		
//		if (pluckerSliders[0] != null) 
//			for (int i = 0; i<6; ++i)	{
//				pluckerSliders[i].setValue(pluckerLine[i]);
//			}
		if (velStates.getChildComponentCount() != steps) {
			SceneGraphUtility.removeChildren(velStates);
			SceneGraphUtility.removeChildren(lemnoSGC);
			SceneGraphUtility.removeChildren(lemno2SGC);
			for (int i =0; i<steps; ++i)	{
				SceneGraphComponent child = new SceneGraphComponent("V"+i);
				child.setAppearance(new Appearance());
				velStates.addChild(child);
				child = new SceneGraphComponent("Lem"+i);
				child.setAppearance(new Appearance());
				lemnoSGC.addChild(child);
				child = new SceneGraphComponent("Lem2"+i);
				child.setAppearance(new Appearance());
				lemno2SGC.addChild(child);
			}
			sverts = new double[steps][][];	
			prfactory = new PointRangeFactory[2][steps];
		}
		qmf.setClosedInUDirection(false);
		qmf.setClosedInVDirection(false);
		qmf.setULineCount(samplesPerLine);
		qmf.setVLineCount(steps);
		qmf.setGenerateEdgesFromFaces(true);
		qmf.setGenerateTextureCoordinates(true);
		qmf.setGenerateFaceNormals(true);
		qmf.setGenerateVertexNormals(true);
		for (int i =0; i<steps; ++i)	{
			SceneGraphComponent child = velStates.getChildComponent(i);
			double[] velocityForTime = velocityForTime(2*Math.PI*(i/(steps-1.0)));
			PointRangeFactory ilsf = representationForLine(velocityForTime, null);
			child.setGeometry(ilsf.getLine());
			SceneGraphComponent[] children  = new SceneGraphComponent[]{lemnoSGC.getChildComponent(i),
					lemno2SGC.getChildComponent(i)};
			double[][] lemnoForTime = lemnoForVelocity(velocityForTime); //2*Math.PI*(i/(steps-1.0)));
//			double[] lemnoForTime2 = lemnoForTime(2*Math.PI*(i/(steps-1.0)));
//			System.err.println("my vel = "+Rn.toString(velocityForTime));
//			System.err.println("my conj = "+Rn.toString(lemnoForTime));
//			System.err.println("mma conj = "+Rn.toString(lemnoForTime2));
			// need to maintain continuity of the intersections of line with sphere
			double[] oldP = null;
			int j = 0;
			int n = lemnoForTime == null ? 0 : lemnoForTime.length;
			for (; j<n; ++j)	{
				if (i>0 && prfactory[j][i-1] != null) oldP = prfactory[j][i-1].getSamples()[0];
				prfactory[j][i] = representationForLine(lemnoForTime[j], oldP);
				children[j].setGeometry(prfactory[j][i].getLine());
				sverts[i] = prfactory[j][i].getLine().getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);	
			}
			for (;j < 2; ++j)	{
				children[j].setGeometry(null);
			}

			if (colorLines) {
				double p1 = PlueckerLineGeometry.innerProduct(velocityForTime, 
						PlueckerLineGeometry.polarize(null, velocityForTime, Pn.EUCLIDEAN));
				double p2 = PlueckerLineGeometry.innerProduct(velocityForTime, velocityForTime);
				double pitch = (p1 == 0 ? 1.0 : p2/p1);
//				System.err.println("Pitch = "+pitch);
				pitch = i/(steps-1.0);
//				if (pitch > 1.0) pitch = 1.0;
//				if (pitch < -1.0) pitch = -1.0;
				Color thisone;
				if (pitch > .75) thisone = AnimationUtility.linearInterpolation(pitch, .75, 1.0, Color.white, Color.blue);
				else if (pitch > .5) thisone = AnimationUtility.linearInterpolation(pitch, .5, .75, Color.red, Color.white);
				else if (pitch > .25) thisone = AnimationUtility.linearInterpolation(pitch, .25, .5, Color.white, Color.red);
				else thisone = AnimationUtility.linearInterpolation(pitch, 0, .25, Color.blue, Color.white);
				child.getAppearance().setAttribute("lineShader.diffuseColor", thisone);
				children[0].getAppearance().setAttribute("lineShader.diffuseColor", thisone);				
				children[1].getAppearance().setAttribute("lineShader.diffuseColor", thisone);				
			} else {
				child.getAppearance().setAttribute("lineShader.diffuseColor", Appearance.INHERITED);
				children[0].getAppearance().setAttribute("lineShader.diffuseColor", Appearance.INHERITED);				
				children[1].getAppearance().setAttribute("lineShader.diffuseColor", Appearance.INHERITED);				
				
			}
		
		}
//		qmf.setVertexCoordinates((double[][][]) null);
		qmf.setVertexCoordinates(sverts);
		qmf.update();
//		SceneGraphUtility.removeChildren(theSurface);
//		theSurface.addChild(IndexedFaceSetUtility.displayFaceNormals(qmf.getIndexedFaceSet(), 1.0, Pn.EUCLIDEAN));
		// the "red line": with respect to this line, the lemniscatory ruled surface is defined
		plLineFactory.setFiniteSphere(finiteSphere);
		plLineFactory.setSphereRadius(sphereRadius);
		plLineFactory.setNumberOfSamples(samplesPerLine);	
//		Biquaternion bivel = new Biquaternion(pluckerLine, Metric.metricForCurvature(Pn.EUCLIDEAN));
//		Biquaternion bivelaxis = Biquaternion.axisForBivector(null, bivel);
//		double[] velaxis = Biquaternion.lineComplexFromBivector(null, bivelaxis);//axis);
//		double[] vv  = PlueckerLineGeometry.permuteCoordinates(null, velaxis, new int[]{3,0,1,2}); //); //PlueckerLineGeometry.dualizeLine(null, velaxis)
		plLineFactory.setPluckerLine(vv); //velaxis);
		plLineFactory.update();
		theLine.setGeometry(plLineFactory.getLine());
		plLineFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);			
		
		// update the focal lines.  Find intersection of p
		double[][] pl = {{0,0,0,1}, {1,0,0,0}};	
		double[][] pt = {{0, Math.sin(dd), -Math.cos(dd), 0},{0,  w2 * Math.sin(dd), (w1 - w2 * Math.cos(dd)),0}};
		for (int i = 0; i<2; ++i)	{
			double[] foo = PlueckerLineGeometry.permuteCoordinates(null, pluckerLine, new int[]{3,0,1,2});
			double[] pt00 = PlueckerLineGeometry.lineIntersectPlane(null, foo, pl[i]);
			double[] ln0 = PlueckerLineGeometry.lineFromPoints(null, pt[i], pt00);
			focalLineFac[i].setFiniteSphere(finiteSphere);
			focalLineFac[i].setSphereRadius(sphereRadius);
			focalLineFac[i].setNumberOfSamples(samplesPerLine);
			if (PlueckerLineGeometry.isValidLine(ln0)) {
//				System.err.println(i+" found line "+Rn.toString(ln0));
				focalLineFac[i].setElement0(pt[i]);
				focalLineFac[i].setElement1(pt00);
//				focalLineFac[i].setPluckerLine(ln0);
				focalLineFac[i].update();	
				focalLines[i].setGeometry(focalLineFac[i].getLine());

			}
		}		
	}
	
	@Override
	public void display() {
		super.display();
		viewer = jrviewer.getViewer();
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", Color.black);
		viewer.getSceneRoot().getAppearance().setAttribute(CommonAttributes.DEPTH_FUDGE_FACTOR, .9999);
		CameraUtility.encompass(jrviewer.getViewer());
		cam = CameraUtility.getCamera(jrviewer.getViewer());
		cam.setNear(.5);
		cam.setFar(25.0);
	}

	PointRangeFactory representationForLine(double[] vel, double[] oldP)	{
		Biquaternion bivel = new Biquaternion(vel, Metric.metricForCurvature(Pn.EUCLIDEAN));
		Biquaternion bivelaxis = Biquaternion.axisForBivector(null, bivel);
		double[] velaxis = Biquaternion.lineComplexFromBivector(null, bivelaxis);//axis);
		double[] vv  = PlueckerLineGeometry.permuteCoordinates(null, velaxis, new int[]{3,0,1,2});
//		System.err.println("line = "+Rn.toString(vv));
		PointRangeFactory localVelFactory = new PointRangeFactory();
		localVelFactory.setFiniteSphere(finiteSphere);
		localVelFactory.setSphereRadius(sphereRadius);
		localVelFactory.setNumberOfSamples(samplesPerLine);	
		localVelFactory.setOldPoint(oldP);
		localVelFactory.setPluckerLine(vv); //velaxis);
		localVelFactory.update();
		if (localVelFactory.getLine() != null)
			localVelFactory.getLine().setGeometryAttributes(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);	
		if (localVelFactory.getLine().getVertexAttributes(Attribute.COORDINATES) == null)
			System.err.println("invalid line");
		return localVelFactory;
	}
	double p01 = 1,p02, p03, p12, p31, p23;
	double[] rawPluckerLine = {p01, p02, p03, p12, p31, p23}, pluckerLine;
	double[] pluckerMatrix;
	private Camera cam;
	private Viewer viewer;
	private Texture2D texture2d = null;
	double[] velocityForTime(double t)	{
		double[] line = new double[]{
				(R*(w1 - w2*Math.cos(dd))*Math.sin(t*w1))/2.,
				   -(R*(w1 - w2*Math.cos(dd))*Math.cos(t*w1))/2.,
				   (R*w2*Math.cos(t*w1)*Math.sin(dd))/2.,-(w2*Math.cos(dd))/2.,(w2*Math.sin(dd))/2.,0};
		line = PlueckerLineGeometry.dualizeLine(null, line);
		return line;
	}
	
	double[][] lemnoForVelocity(double[] vel) {
//		double[] line = Rn.matrixTimesVector(null, pluckerMatrix, PlueckerLineGeometry.dualizeLine(null, vel));
//		return line; //PlueckerLineGeometry.dualizeLine(null, line);
		double k0 = PlueckerLineGeometry.innerProduct(pluckerLine, pluckerLine);
		double k1 = 2*PlueckerLineGeometry.innerProduct(pluckerLine, vel);
		double k2 = PlueckerLineGeometry.innerProduct(vel, vel);
		double[][] conj = null;
		if (k0 <= 10E-8) conj = new double[][]{Rn.linearCombination(null, -k2, pluckerLine, k1, vel)};
		else {
			double d = k1*k1 - 4*k0*k2;
			if (d < 0) {
//				System.err.println("d = "+d);
				return null;
			}
			double disc = Math.sqrt(d);
			double alpha1 = (1.0/(2*k0))*(-k1 + disc),
				alpha2 = (1.0/(2*k0))*(-k1 - disc);
			conj = new double[][]{Rn.linearCombination(null, alpha1, pluckerLine, 1, vel),
					Rn.linearCombination(null, alpha2, pluckerLine, 1, vel)};
		}
		return conj; //PlueckerLineGeometry.dualizeLine(null, conj);
	}
	
	double[] lemnoForTime(double t)	{
		
		p01 = pluckerLine[5];
		p02 = pluckerLine[4];
		p03 = pluckerLine[3];
		p12 = pluckerLine[2];
		p31 = pluckerLine[1];
		p23 = pluckerLine[0];
		double[] line = new double[]{

				(R*(4*p01*w1*w2*Math.cos(t*w1)*Math.sin(dd) + 
				        4*(w1 - w2*Math.cos(dd))*Math.sin(t*w1)*
				         (p02*w2*Math.sin(dd) + Math.cos(t*w1)*(-(p31*R*w1) + p12*R*w2*Math.sin(dd)) + 
				           p23*R*w1*Math.sin(t*w1) - 
				           w2*Math.cos(dd)*(p03 - p31*R*Math.cos(t*w1) + p23*R*Math.sin(t*w1)))))/2.,
				   2*R*Math.cos(t*w1)*(-(Math.pow(w2,2)*Math.pow(Math.cos(dd),2)*
				         (p03 - p31*R*Math.cos(t*w1) + p23*R*Math.sin(t*w1))) + 
				      R*w1*(Math.cos(t*w1)*(p31*w1 - p12*w2*Math.sin(dd)) - p23*w1*Math.sin(t*w1)) + 
				      w2*Math.cos(dd)*(p03*w1 + p02*w2*Math.sin(dd) + 
				         Math.cos(t*w1)*(-2*p31*R*w1 + p12*R*w2*Math.sin(dd)) + 
				         2*p23*R*w1*Math.sin(t*w1))),
				   2*R*w2*Math.cos(t*w1)*Math.sin(dd)*
				    (p03*w1 + p02*w2*Math.sin(dd) + 
				      Math.cos(t*w1)*(-(p31*R*w1) + p12*R*w2*Math.sin(dd)) + p23*R*w1*Math.sin(t*w1) - 
				      w2*Math.cos(dd)*(p03 - p31*R*Math.cos(t*w1) + p23*R*Math.sin(t*w1))),
				   2*w2*(p12*R*w1*Math.cos(t*w1)*Math.sin(dd) + 
				      w2*Math.pow(Math.cos(dd),2)*(p03 - p31*R*Math.cos(t*w1) + p23*R*Math.sin(t*w1)) + 
				      Math.cos(dd)*(-(p02*w2*Math.sin(dd)) + 
				         R*Math.cos(t*w1)*(p31*w1 - p12*w2*Math.sin(dd)) - p23*R*w1*Math.sin(t*w1))),
				   2*w2*Math.sin(dd)*(w2*(p02 + p12*R*Math.cos(t*w1))*Math.sin(dd) + 
				      p23*R*w1*Math.sin(t*w1) - w2*Math.cos(dd)*
				       (p03 - p31*R*Math.cos(t*w1) + p23*R*Math.sin(t*w1))),
				   2*p23*R*w1*w2*Math.cos(t*w1)*Math.sin(dd),0};
		line = Rn.times(null, .25, PlueckerLineGeometry.dualizeLine(null, line));
		return line;
	}
	
	String[][] pluckernames = {{"vx", "vy", "vz", "px","py","pz"},{"p01", "p02", "p03", "p12", "p31", "p23"}};

	@Override
	public Component getInspector() {	
		Box inspectionPanel =  Box.createVerticalBox();
		final TextSlider nSlider = new TextSlider.Integer("line #",SwingConstants.HORIZONTAL, 1,1000,steps);
		nSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				steps = nSlider.getValue().intValue();
				updateLines();
			}
		});
		inspectionPanel.add(nSlider);
		final TextSlider timeSlider = new TextSlider.Double("w2",SwingConstants.HORIZONTAL, -1.0, 10.0, w2);
		timeSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				w2 = timeSlider.getValue().doubleValue();
				updateLines();
			}
		});
		inspectionPanel.add(timeSlider);
		final TextSlider radiusSlider = new TextSlider.DoubleLog("clip radius",SwingConstants.HORIZONTAL, 0.1, 50.0, sphereRadius);
		radiusSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				sphereRadius = radiusSlider.getValue().doubleValue();
				updateLines();
			}
		});
		inspectionPanel.add(radiusSlider);
		final TextSlider clipSlider = new TextSlider.DoubleLog("zclip plane",SwingConstants.HORIZONTAL, 0.1, 50.0, clipDistance);
		clipSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				cam.setNear(clipSlider.getValue().doubleValue());
				viewer.renderAsync();
			}
		});
		inspectionPanel.add(clipSlider);
		
		Box hbox = Box.createHorizontalBox();
		inspectionPanel.add(hbox);
		final JCheckBox visBox = new JCheckBox("Clip sphere");
		visBox.setSelected(finiteSphere);
		visBox.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				finiteSphere = visBox.isSelected();
				updateLines();
			}
		});
		hbox.add(visBox);
		
		final JCheckBox velBox = new JCheckBox("Show vel");
		velBox.setSelected(velStates.isVisible());
		velBox.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				velStates.setVisible(velBox.isSelected());
			}
		});
		hbox.add(velBox);
		final JCheckBox colorBox = new JCheckBox("Color lines");
		colorBox.setSelected(colorLines);
		colorBox.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				colorLines=colorBox.isSelected();
				updateLines();
			}
		});
		hbox.add(colorBox);
		hbox = Box.createHorizontalBox();
		inspectionPanel.add(hbox);
		final JCheckBox surfBox = new JCheckBox("Show surf");
		surfBox.setSelected(theSurface.isVisible());
		surfBox.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				theSurface.setVisible(surfBox.isSelected());
			}
		});
		hbox.add(surfBox);
		final JCheckBox textBox = new JCheckBox("Show tex");
		textBox.setSelected(showTexture);
		textBox.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				showTexture = textBox.isSelected();
				theSurface.setAppearance( showTexture ?textureAp : null);
			}
		});
		hbox.add(textBox);
		Box vbox = Box.createVerticalBox();
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "Projection point")));
		inspectionPanel.add(vbox);
		final JCheckBox lineBox = new JCheckBox("Lines only");
		lineBox.setSelected(linesOnly);
		lineBox.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				linesOnly = lineBox.isSelected();
				updateLabels();
				updateLines();
			}
		});
		vbox.add(lineBox);
		for (int i =0; i<6; ++i)	{
			final int j = i;
			int which = linesOnly ? 0 : 1;
			pluckerSliders[i] = new TextSlider.Double(pluckernames[which][i],SwingConstants.HORIZONTAL, -2, 2, rawPluckerLine[i]);
			pluckerSliders[i].addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					rawPluckerLine[j] = pluckerSliders[j].getValue().doubleValue();
					updateLines();
				}
			});
			vbox.add(pluckerSliders[i]);

		}
		return inspectionPanel;
	}

	protected void updateLabels() {
		int which = linesOnly ? 0 : 1;
		for (int i =0; i<6; ++i)	{
			pluckerSliders[i].getLabel().setText(pluckernames[which][i]);
		}
	}

	public static void main(String[] args) {
		new EarthMotion().display();
	}
}
