/*
 * Created on 12.07.2017
 *
 */
package charlesgunn.jreality.worlds.iscador;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.SwingConstants;

import org.omg.CORBA.OMGVMCID;

import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.jreality.geometry.projective.PointCollector;
import charlesgunn.jreality.geometry.projective.PointRangeFactory;
import charlesgunn.jreality.viewer.Assignment;
import charlesgunn.util.TextSlider;
import de.jreality.backends.label.LabelUtility;
import de.jreality.geometry.BallAndStickFactory;
import de.jreality.geometry.GeometryUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.SphereUtility;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.shader.CommonAttributes;
import de.jreality.shader.DefaultGeometryShader;
import de.jreality.shader.DefaultPointShader;
import de.jreality.shader.ImageData;
import de.jreality.shader.ShaderUtility;
import de.jreality.shader.Texture2D;
import de.jreality.shader.TextureUtility;
import de.jreality.util.CameraUtility;
import de.jreality.util.Input;
import de.jreality.util.Rectangle3D;
import de.jreality.util.SceneGraphUtility;

public class Iscador3DEarth extends Assignment {

	private static final Color hisciaGreen = new Color(66,204, 39);
	double latitude = Math.toRadians(47 + 29/60),
			longitude = -Math.toRadians(7+37/60),
			compLat = Math.PI/2-latitude; // latitude of Arlesheim
	double earthRadius = 6371;   // radius of earth (average) in km
	double diskRadius = 250;		// unrealistically large disk, big enough to see
	double rpm = 50/1440.0,
			ratio = 1.0;			// rotations of disk per minute
	double criticalValue;    // rpm at which disk velocity balances earth velocity
	transient SceneGraphComponent 
		world = SceneGraphUtility.createFullSceneGraphComponent("world"),
		  undoEarth = SceneGraphUtility.createFullSceneGraphComponent("undo earth"),
			earth = SceneGraphUtility.createFullSceneGraphComponent("earth"),
				earthChild = SceneGraphUtility.createFullSceneGraphComponent("earth child"),
				disk2 = SceneGraphUtility.createFullSceneGraphComponent("disk2"),
					disk1 = SceneGraphUtility.createFullSceneGraphComponent("disk1"),
					pointOnBndySGC = SceneGraphUtility.createFullSceneGraphComponent("point"),
			orbitOfPoint =  SceneGraphUtility.createFullSceneGraphComponent("orbit of point"),
			kineme =  SceneGraphUtility.createFullSceneGraphComponent("kineme"),
				kinemes[] = new SceneGraphComponent[3];
	
	transient SceneGraphPath world2disk;
	transient PointCollector pc = new PointCollector(1000, 4);
//	transient String earthTextureName = "http://page.math.tu-berlin.de/~gunn/Pictures/textures/Earth2048Light.jpg"; //grid256.jpg")); //
	transient String earthTextureName = "/Volumes/SamsungSSD1T/gunn_local/Movies/mistletoeMachine/pictures/earthNASA-5400x2700.jpg"; //grid256.jpg")); //
	transient double[] pointOnBndy = {1,0,0,1};
	transient boolean undoEarthB = true, stopEarth = false,
			kinemePhase = false;
	Color[] kinemeC = {Color.white, hisciaGreen, Color.red};
	@Override
	public SceneGraphComponent getContent() {
		IndexedFaceSet sphere = SphereUtility.sphericalPatch(0.0, 0.0, 360.0, 179.999, 80, 40, 1.0);
		earth.addChild(earthChild);
		earthChild.setGeometry(sphere);
		Appearance ap = earthChild.getAppearance();
		ap.setAttribute(CommonAttributes.EDGE_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		ImageData earthImage = null;
		try {
			earthImage = ImageData.load(Input.getInput(earthTextureName));
			System.err.println("Earth image loaded "+earthImage.getWidth());
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (kinemePhase)	{
			earth.addChild(Primitives.wireframeSphere(50, 25));
			earthChild.setVisible(false);
		}

		ap = world.getAppearance(); //new Appearance();
		ap.setAttribute("polygonShader.diffuseColor", Color.white);
		Texture2D earthTexture2d = TextureUtility.createTexture(ap, "polygonShader", 0, earthImage);
		
		double zmax = .05;
		disk1.addChild(Primitives.closedCylinder(100, 1, 0, zmax, 2*Math.PI));
		ap = disk1.getAppearance();
		ap.setAttribute("polygonShader.diffuseColor", Color.white);
		ap.setAttribute("lineShader.diffuseColor", hisciaGreen);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
		ap.setAttribute("lineShader."+CommonAttributes.TUBES_DRAW, true);
		ap.setAttribute("lineShader."+CommonAttributes.TUBE_RADIUS, .025);

		for (int i = 0; i<3; ++i)	{
			kinemes[i] = SceneGraphUtility.createFullSceneGraphComponent("kineme"+i);
			kineme.addChild(kinemes[i]);
			ap = kinemes[i].getAppearance();
			ap.setAttribute("lineShader.diffuseColor", kinemeC[i]);
			ap.setAttribute("pointShader.diffuseColor", kinemeC[i]);
		}
		ap = kineme.getAppearance();
		ap.setAttribute(GeometryUtility.BOUNDING_BOX, Rectangle3D.EMPTY_BOX);
		ap.setAttribute("lineShader.tubeRadius", .01);
		ap.setAttribute("pointShader.pointRadius", .01);
		ap.setAttribute( CommonAttributes.AMBIENT_COEFFICIENT, .05);
		ap.setAttribute(CommonAttributes.VERTEX_DRAW	, true);
		updateDiskTransform();
		
//		pointOnBndySGC.setGeometry(Primitives.point(pointOnBndy));
		pointOnBndySGC.setGeometry(IndexedLineSetUtility.createCurveFromPoints(new double[][]{P3.originP3, pointOnBndy}, false));
		MatrixBuilder.euclidean().translate(0,0,zmax*1.1).assignTo(pointOnBndySGC);
		int[] stippleVals = {15*257,165*257, 234*257, 15*257, 165*257, 234*257,0, 0, 0, 0};
		ap = pointOnBndySGC.getAppearance();
	    DefaultGeometryShader dgs = ShaderUtility.createDefaultGeometryShader(ap, false);
		dgs.setShowPoints(true);
	    DefaultPointShader pointShader = (DefaultPointShader)dgs.getPointShader();
	    pointShader.setPointRadius(.04);
	    pointShader.setSpheresDraw(true);
	    Color pointColor = Color.yellow;
		pointShader.setDiffuseColor(pointColor);
//		ap.setAttribute(CommonAttributes.VERTEX_DRAW, true);
		ap.setAttribute(CommonAttributes.EDGE_DRAW, true);
//		ap.setAttribute(CommonAttributes.VERTEX_SHADER+"."+CommonAttributes.DIFFUSE_COLOR, Color.red);
//		ap.setAttribute("vertexShader.pointRadius", .04);
		ap.setAttribute("lineShader.diffuseColor", pointColor);
		ap.setAttribute("lineShader.tubeRadius", .025);
		ap.setAttribute(CommonAttributes.TUBES_DRAW, true);
		ap.setAttribute(CommonAttributes.LINE_STIPPLE, true);
		ap.setAttribute(CommonAttributes.LINE_STIPPLE_PATTERN, stippleVals[0]);
		SceneGraphComponent labelForDisk = getLabelForDisk();
		labelForDisk.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		MatrixBuilder.euclidean().translate(0,0,.051).assignTo(labelForDisk);
		disk1.addChildren(labelForDisk, pointOnBndySGC);
		
		undoEarth.addChildren(earth, orbitOfPoint);
		world.addChildren(undoEarth);
		orbitOfPoint.setGeometry(pc.getCurve());
		MatrixBuilder.euclidean().scale(1.003).assignTo(orbitOfPoint);
		ap = orbitOfPoint.getAppearance();
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, false);
		ap.setAttribute(CommonAttributes.VERTEX_SHADER+"."+CommonAttributes.POINT_RADIUS, .001);
		ap.setAttribute(CommonAttributes.VERTEX_SHADER+"."+CommonAttributes.POINT_SIZE, 2);
		ap.setAttribute(CommonAttributes.VERTEX_SHADER+".diffuseColor", pointColor);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBE_RADIUS, .002);
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.LINE_WIDTH, 2);
		ap.setAttribute(CommonAttributes.LINE_SHADER+".diffuseColor", pointColor);
		earth.addChildren(disk2, kineme);
		disk2.addChild(disk1);
		
		// default is looking down on the north pole
		MatrixBuilder.euclidean().
			rotateX(latitude).
			rotateY(longitude).
			rotateY(-Math.PI/2).			// rotate by 90 degrees
			rotateX(-Math.PI/2).			// first rotate so we're looking down the y-axis
			assignTo(world);
		
		world.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		
		world2disk = (SceneGraphPath) SceneGraphUtility.getPathsBetween(earth, disk1).get(0);
		return world;
	}

	public static SceneGraphComponent getLabelForDisk()	{
		SceneGraphComponent label = SceneGraphUtility.createFullSceneGraphComponent("label");
		IndexedFaceSet square = Primitives.texturedQuadrilateral();
		label.setGeometry(square);
		BufferedImage bi = LabelUtility.createImageFromString("A", Font.getFont("Monospaced"), Color.black, new Color(0,0,0,0));
		ImageData id = null;
		try {
			id = ImageData.load(new Input(Iscador3DEarth.class.getResource("hiscia.png")));
		} catch (IOException e) {
			e.printStackTrace();
		}
		Appearance appearance = label.getAppearance();
		appearance.setAttribute(CommonAttributes.VERTEX_DRAW, false);
		appearance.setAttribute(CommonAttributes.EDGE_DRAW, false);
		
		Texture2D tex2d = TextureUtility.createTexture(appearance, "polygonShader", id);
		tex2d.setRepeatS(Texture2D.GL_CLAMP);
		Matrix tm = MatrixBuilder.euclidean().translate(.5,.5,0).scale(.95).translate(-.5,-.5,0).getMatrix();
		tex2d.setTextureMatrix(tm);
		double w = bi.getWidth(), h = bi.getHeight();
		MatrixBuilder.euclidean().scale(1.3).scale(w/h, 1, 1).rotateZ(-Math.PI/2).translate(-.5,-.5,0.0).assignTo(label);
		SceneGraphComponent holder = SceneGraphUtility.createFullSceneGraphComponent("label holder");
		holder.addChild(label);
		return holder;
	}
	private void updateDiskTransform() {
		MatrixBuilder.euclidean().
			rotateZ(-longitude).
			rotateY(-latitude).
			translate(1,0,0).
			rotateY(Math.PI/2).
			scale(diskRadius/earthRadius).
			assignTo(disk2);
		// calculate critical value
		double angleE = Math.PI/2 - latitude;
		// calculate the r per day at which the disk rotation matches the earth rotation velocity at this latitude
		criticalValue = (earthRadius/diskRadius)*(Math.cos(latitude)-(diskRadius/earthRadius)*Math.sin(angleE));
		updateRPM();
	}

	protected void updateRPM() {
		// convert to rpm and scale by desired ratio wrt critical value
		rpm = criticalValue * ratio/1440;
	}

	private void updateKineme() {
		// TODO Auto-generated method stub
//		PointRangeFactory.line(pt0, pt1)
		double[] NP = {0,0,1,1},
				Arlesheim = {Math.cos(latitude)*Math.cos(-longitude), 
						Math.cos(latitude)*Math.sin(-longitude), 
						Math.sin(latitude), 1},
				resultant = Rn.add(null, NP, Rn.times(null, criticalValue*ratio, Arlesheim));
		Pn.dehomogenize(resultant, resultant);
		resultant[3] = 0;
		Rn.normalize(resultant, resultant);
		resultant[3] = 1.0;
//		System.err.println("Arlesheim = "+Rn.toString(Arlesheim));
//		System.err.println("Resultant = "+Rn.toString(resultant));
		MatrixBuilder.euclidean().scale(1.2).assignTo(kineme);
		double[] flipm = Rn.diagonalMatrix(null, new double[]{-1,-1,-1,1});
//		kinemes[0].setGeometry(PointRangeFactory.line(P3.originP3, NP));
//		kinemes[1].setGeometry(PointRangeFactory.line(P3.originP3, Arlesheim));
//		kinemes[2].setGeometry(PointRangeFactory.line(P3.originP3, resultant));
		double[][] points = {NP, Arlesheim, resultant},
				flipPoints = Rn.matrixTimesVector(null, flipm, points);
		for (int i = 0; i<3; ++i)	{
			IndexedLineSet axis = IndexedLineSetUtility.createCurveFromPoints(new double[][]{flipPoints[i], points[i]}, false);
			BallAndStickFactory basf = new BallAndStickFactory(axis);
			basf.setBallRadius(.012);
			basf.setStickRadius(.012);
			basf.setShowArrows(true);
			basf.setArrowScale(.03);
			basf.setArrowSlope(3);
			basf.setArrowPosition(1.0);
			basf.setStickColor(kinemeC[i]);
			basf.setBallColor(kinemeC[i]);
			basf.setArrowColor(kinemeC[i]);
			basf.update();
			SceneGraphComponent tubedIcosa = basf.getSceneGraphComponent();
			kinemes[i].removeAllChildren();
			kinemes[i].addChild(tubedIcosa);
		}
	}

	int acount = 0;
	double fps = 30.0;
	double lastStoppedD = 0;
	double maxR = 2.00, // speed of iscador machine
			minR=0.001, still = .1;

	@Override
	public void setValueAtTime(double d) {
		// TODO Auto-generated method stub
		super.setValueAtTime(d);
		if (kinemePhase)		{
			double tmp = AnimationUtility.linearInterpolation(d, still, 1-still, 0, 1);
			ratio = AnimationUtility.linearInterpolation(d, still, 1-still, minR, maxR);
			updateRPM();
			updateKineme();
			double acc = tmp*tmp;
			double beginningAngle = rpm*1440*3*acc;
			calculateOrbitOfPoint(beginningAngle);
			MatrixBuilder.euclidean().rotateZ(beginningAngle).assignTo(disk1);

		} else {
			acount++;
//			d = acount/fps;
			if (stopEarth) {
				MatrixBuilder.euclidean().assignTo(earth);
				MatrixBuilder.euclidean().assignTo(undoEarth);
				lastStoppedD=d;
			} else {
				MatrixBuilder.euclidean().rotateZ(d-lastStoppedD).assignTo(earth);
				if (undoEarthB)	{
					Matrix m = new Matrix(earth.getTransformation());
					m.invert();
					m.assignTo(undoEarth);
				}
			}
			MatrixBuilder.euclidean().rotateZ(rpm*1440*d).assignTo(disk1);
			
			// add the point
			double[] w2d = world2disk.getMatrix(null);
			double[] pw = Rn.matrixTimesVector(null, w2d, pointOnBndy);
			pc.addPoint(pw);
			orbitOfPoint.setVisible(pc.getCount()>1);
	
		}
	}

	private void calculateOrbitOfPoint(double ba)	{
		if (world2disk == null) return;
		orbitOfPoint.setVisible(false);
		pc.reset();
		double delta = .01;
		for (int i = 0; i<=300; ++i)	{
			double d = (Math.PI/300)*i-Math.PI;
			MatrixBuilder.euclidean().rotateZ(d).assignTo(earth);
			if (undoEarthB)	{
				Matrix m = new Matrix(earth.getTransformation());
				m.invert();
				m.assignTo(undoEarth);
			}
			MatrixBuilder.euclidean().rotateZ(rpm*1440*d+ba).assignTo(disk1);
			double[] w2d = world2disk.getMatrix(null);
			double[] pw = Rn.matrixTimesVector(null, w2d, pointOnBndy);
			pc.addPoint(pw);
		}
		MatrixBuilder.euclidean().assignTo(disk1);
		orbitOfPoint.setVisible(true);
		
	}
	@Override
	public void startAnimation() {
		// TODO Auto-generated method stub
		super.startAnimation();
		acount = 0;
		orbitOfPoint.setVisible(false);
		pc.reset();
	}


	@Override
	public void display() {
		// TODO Auto-generated method stub
		super.display();
		renewBG();
	    CameraUtility.encompass(jrviewer.getViewer());
	    animationPlugin.setAnimateSceneGraph(false);
	    
		animationPlugin.getAnimationPanel().setResourceDir("src/charlesgunn/jreality/worlds/iscador/");
		try {
			animationPlugin.getAnimationPanel().read(new Input(this.getClass().getResource("mistletoeMachine-anim.xml")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		animationPlugin.setAnimateCamera(true);
//		animationPlugin.setAnimateSceneGraph(true);
	}

	private void renewBG() {
		Color[] backgroundArray = new Color[4];
		backgroundArray[3] = new Color(0,0,40);
		backgroundArray[2] = new Color(0,20,60);
		backgroundArray[1] = new Color(190,100,140);
		backgroundArray[0] = new Color(160,50,140);
	    jrviewer.getViewer().getSceneRoot().getAppearance().setAttribute(CommonAttributes.BACKGROUND_COLORS, backgroundArray);
	}


	@Override
	public Component getInspector() {
		// TODO Auto-generated method stub
		Box hbox = Box.createHorizontalBox();
		inspector.add(hbox);
		JCheckBox whichGeomB = new JCheckBox("hold earth fixed");
		whichGeomB.setSelected(undoEarthB);
		whichGeomB.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				undoEarthB = ((JCheckBox) arg0.getSource()).isSelected();
			}
		});
		hbox.add(whichGeomB);
		JCheckBox noMo = new JCheckBox("stop earth motion");
		noMo.setSelected(stopEarth);
		noMo.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				stopEarth = ((JCheckBox) arg0.getSource()).isSelected();
			}
		});
		hbox.add(noMo);
		JButton bgB = new JButton("renew bg");
		bgB.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				renewBG();
			}
		});
		hbox.add(bgB);
		
		JButton sgB = new JButton("reset sg");
		sgB.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent arg0) {
				pc.reset();
				setValueAtTime(0);
			}
		});
		hbox.add(sgB);
		
		final TextSlider diskRSlider = new TextSlider.Double("disk rad (km)",SwingConstants.HORIZONTAL, 0.0, 1000, diskRadius);
		diskRSlider.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				diskRadius = diskRSlider.getValue().doubleValue();
				updateDiskTransform();
			}
		});
		inspector.add(diskRSlider);
		final TextSlider diskRPMSlidr = new TextSlider.Double("rpm:rpmCrit",SwingConstants.HORIZONTAL, 0.0, 2.0, ratio );
		diskRPMSlidr.addActionListener(new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				ratio = diskRPMSlidr.getValue().doubleValue();
				updateRPM();
			}
		});
		inspector.add(diskRPMSlidr);
		return inspector;
	}


	public static void main(String[] args) {
		new Iscador3DEarth().display();
	}

}
