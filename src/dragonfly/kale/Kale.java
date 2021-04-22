/*
 * Created on Jul 31, 2008
 *
 */
package dragonfly.kale;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JColorChooser;
import javax.swing.JMenuBar;
import javax.swing.SwingConstants;

import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.jreality.viewer.LoadableScene;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.GeometryMergeFactory;
import de.jreality.geometry.IndexedFaceSetFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.QuadMeshFactory;
import de.jreality.geometry.RemoveDuplicateInfo;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.ViewerApp;
import de.jreality.util.SceneGraphUtility;

public class Kale extends LoadableScene {

	int numCurves =10, numInitPoints = 15;
	double powerLaw = .9, widthFactor = .8, asymm = .03, firstOrder = .5;
	double stepsize = .2;
	boolean mergeDups = false;
	Color color2 = Color.yellow, color1 = Color.blue;
	IndexedFaceSetFactory ifsf = new IndexedFaceSetFactory();
	
	List<double[][]> curves = new ArrayList<double[][]>();
	SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
	private Color[] baseColors = {color1, color2};
	@Override
	public SceneGraphComponent makeWorld() {
		return updateKale();
	}

	private SceneGraphComponent updateKale() {
//		for (int i = 0; i<numCurves; ++i)	{
//			SceneGraphComponent sgc = new SceneGraphComponent(""+i);
//			world.addChild(sgc);
//		}
		System.err.println("Updating kale");
		SceneGraphUtility.removeChildren(world);
		curves.clear();
		curves.add(new double[][]{{0,0,0}});
		double[][] lastRow = new double[numInitPoints][]; //{seedPoints[1],seedPoints[2],seedPoints[3],seedPoints[4]};
		for (int i = 0; i<lastRow.length; ++i)	{
			double angle = (2.0*i)/(lastRow.length)*Math.PI;
			lastRow[i] = new double[]{Math.cos(angle), Math.sin(angle), asymm*Math.sin(3*angle)};
		}
		curves.add(lastRow);
		double lastRowLength = Math.PI*2;
		double k = 2*Math.exp(powerLaw)*lastRowLength/(Math.exp(powerLaw*2)-1);
		double radius = 1;
		for (int i = 0; i<numCurves; ++i)	{
			if (i > 1) {
				double width = Math.pow(widthFactor, i-1);
				radius += width;
				curves.add(extend(curves.get(i-2), curves.get(i-1), 
						width, k*Pn.sinh(powerLaw*radius)));
			}
//			IndexedLineSet ils = IndexedLineSetUtility.createCurveFromPoints(curves.get(i), true);
//			world.getChildComponent(i).setGeometry(ils);
		}
		// fill in with strips of polygons
		for (int i = 0; i<numCurves-1; ++i)	{
			double fr1 = i/(numCurves-1.0), fr2 = (i+1)/(numCurves-1.0);
			SceneGraphComponent sgc = new SceneGraphComponent("strip"+i);
			Color c1 = AnimationUtility.linearInterpolation(baseColors[0],baseColors[1], fr1);
			Color c2 = AnimationUtility.linearInterpolation(baseColors[0], baseColors[1], fr2);
			IndexedFaceSet ifs = fillInStrip(curves.get(i), curves.get(i+1),
					c1, c2);
			sgc.setGeometry(ifs);
			world.addChild(sgc);
		}
		world.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		world.getAppearance().setAttribute("lineShader."+CommonAttributes.TUBES_DRAW, false);
	
		if (!mergeDups) return world;
		GeometryMergeFactory gmf = new GeometryMergeFactory();
		IndexedFaceSet foo = gmf.mergeGeometrySets(world);
		SceneGraphComponent moo = new SceneGraphComponent("merged");
		System.err.println("before vertex count: "+foo.getNumPoints());
		foo = (IndexedFaceSet) RemoveDuplicateInfo.removeDuplicateVertices(foo, 10E-4,
				Attribute.COORDINATES, Attribute.COLORS); //);//
		System.err.println("after vertex count: "+foo.getNumPoints());
		moo.setAppearance(new Appearance());
		moo.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
		moo.getAppearance().setAttribute("lineShader."+CommonAttributes.TUBES_DRAW, false);
		moo.setGeometry(foo);
		foo.setFaceAttributes(Attribute.NORMALS, null);
		foo.setVertexAttributes(Attribute.NORMALS, null);
		foo.setFaceAttributes(Attribute.COLORS, null);
		IndexedFaceSetUtility.calculateAndSetNormals(foo);
		return moo; //world;
	}

	private IndexedFaceSet fillInStrip(double[][] ds, double[][] ds2,
			Color c1, Color c2) {
		int n = ds2.length;
		double[][][] verts = new double[2][n+1][];
		Color[] vcolors = new Color[2*n+2];
		for (int i = 0; i<=n; ++i)	{
			double t = (i/((double)n));
			verts[1][i] = ds2[i%n];
			verts[0][i] = linearInterpolate(t, ds);
			vcolors[n+1+i] = c2;
			vcolors[i] = c1;
		}
		QuadMeshFactory qmf = new QuadMeshFactory();
		qmf.setULineCount(n+1);
		qmf.setVLineCount(2);
		qmf.setClosedInUDirection(false);
		qmf.setClosedInVDirection(false);
		qmf.setGenerateEdgesFromFaces(true);
		qmf.setGenerateFaceNormals(true);
		qmf.setGenerateVertexNormals(true);
		qmf.setVertexCoordinates(verts);
		qmf.setVertexColors(vcolors);
		qmf.update();
		
		return qmf.getIndexedFaceSet();
	}

	private double[][] extend(
			double[][] row0, 
			double[][] row1, 
			double width, 
			double length) {
		double[][] row2 = new double[2*row1.length][];
		int n = row2.length;
		double[][]diffs = new double[n][];
		for (int i=0; i<n; ++i)	{
			double t = (i/((double)n));
			double[] p0 = linearInterpolate(t, row0);
			double[] p1 = linearInterpolate(t, row1);
			diffs[i] = Rn.subtract(null, p1, p0);
			Rn.setToLength(diffs[i], diffs[i], width);
			row2[i] = Rn.add(null, diffs[i], p1);
		}
		double ll = 0.0;
		double[] lengths = new double[n];
		for (int i=0; i<n; ++i)	{
			lengths[i] = Rn.euclideanDistance(row2[i], row2[((i+1)%n)]);
			ll += lengths[i];
		}
		double factor = length/ll;
		double log = Math.log(factor);
		System.err.println("desired length = "+length);
		System.err.println("length = "+ll);
		double[][] nrow2 = row2; //new double[n][];
		int count = 0;
		while ( Math.abs(ll-length) > 10E-2)	{
			ll = 0;
			for (int i = 0; i<n; ++i)	{
				int prev = (i+n-1)%n, pprev = (i+n-2)%n;
				int next = (i+1)%n, nnext = (i+2)%n;
				double[] dir = null;
				double t = (i/((double)n));
				double[] p1 = linearInterpolate(t, row1);
				double det = Rn.determinant(squareMatrix(
						row2[prev], row2[i], row2[next], diffs[i]));
				// if the three points are collinear ...
				if (Math.abs(det) < 10E-4)	{
//					System.err.println("det = "+det);
					// calculate a direction orthogonal to the line they lie on
					double[] diff = Rn.subtract(null, row2[next], row2[prev]);
					dir = Rn.crossProduct(null,diffs[i], diff);
				if ( (i%2)==1) Rn.times(dir,-1,dir);
//					if ( (Math.random() > .5)) Rn.times(dir,-1,dir);
				} else {
					// push the point away from its neighbors
					double[] dir1 = Rn.subtract(null, row2[i], row2[next]);
					double[] dir2 = Rn.subtract(null, row2[i], row2[prev]);
					dir = Rn.linearCombination(null, Rn.euclideanNormSquared(dir2), dir1, Rn.euclideanNormSquared(dir1), dir2);//Rn.add(null, dir1, dir2);
					double[] dir3 = Rn.subtract(null, row2[i], row2[nnext]);
					double[] dir4 = Rn.subtract(null, row2[i], row2[pprev]);
					double[] dir5 = Rn.linearCombination(null, Rn.euclideanNormSquared(dir4), dir3, Rn.euclideanNormSquared(dir3), dir4);//Rn.add(null, dir1, dir2);
					Rn.linearCombination(dir, firstOrder, dir,1-firstOrder, dir5);
				}
				Rn.setToLength(dir, dir, log*stepsize);
//				Rn.times(dir, log*stepsize, dir);
//				System.err.println("dir = "+Rn.toString(dir));
				nrow2[i] = Rn.add(null, row2[i], dir);
				// keep the width of the band constant
				double[] ndiff = Rn.subtract(null, nrow2[i], p1);
				Rn.setToLength(ndiff, ndiff, width);
				Rn.add(nrow2[i], p1, ndiff);
			}
//			double[][] eqrow = new double[n][];
//			for (int i = 0; i<n; ++i)	{
//				double t = (i/((double)n));
//				
//			}
			for (int i=0; i<n;++i)	
				ll += Rn.euclideanDistance(row2[i], row2[(i+1)%n]);
			factor = length/ll;
			log = Math.log(factor);
			System.err.println("length = "+ll);
			if (count++ > 40) break;
		}
		return nrow2;
	}
	
	private double[] squareMatrix(double[] ds, double[] ds2, double[] ds3,
			double[] ds4) {
		double[] squareMatrix = new double[16];
		for (int i = 0; i<4; ++i) squareMatrix[4*(i+1)-1] = 1.0;
		System.arraycopy(ds, 0, squareMatrix, 0, ds.length);
		System.arraycopy(ds2, 0, squareMatrix, 4, ds2.length);
		System.arraycopy(ds3, 0, squareMatrix, 8, ds3.length);
		System.arraycopy(ds4, 0, squareMatrix, 12, ds4.length);
		return squareMatrix;
	}

	private double[] linearInterpolate(double t, double[][] curve)	{
		int n = curve.length;
		int m = ((int) (n*t));
		double fr = (n*t)-m;
		m = m %n;
		int p = (m+1)%n;
		return AnimationUtility.linearInterpolation(fr, 0,1, curve[m], curve[p]);
	}

	@Override
	public boolean isEncompass() {return true; }
	
	@Override
	public void customize(JMenuBar menuBar, Viewer viewer) {
		viewer.getSceneRoot().getAppearance().setAttribute("backgroundColor", Color.black);
	}
	class Branch extends SceneGraphComponent {
		SceneGraphComponent left, right;
		Branch(int n, String name, double scale, double rotate) {
			setName(name);
			IndexedLineSet stem = IndexedLineSetUtility.createCurveFromPoints(
					new double[][]{{-1,0,0},{0,0,0}}, false);
			setGeometry(stem);
			MatrixBuilder.euclidean().rotateZ(rotate).scale(scale).translate(1,0,0).assignTo(this);
			if (n > 0) {
				left = new Branch(n-1, name+"l", scale, Math.PI/4);
				right = new Branch(n-1, name+"r", scale, -Math.PI/4);
				addChildren(left, right);				
			}
		}
	}
	
	@Override
	public boolean hasInspector() {
		return true;
	}

	@Override
	public Component getInspector(Viewer v) {
		    Box container = Box.createVerticalBox();
		    final TextSlider RSlider = new TextSlider.Integer("number curves",  SwingConstants.HORIZONTAL, 2, 20, numCurves);
		    RSlider.addActionListener(new ActionListener()  {
		      public void actionPerformed(ActionEvent e)  {
		        numCurves = RSlider.getValue().intValue();
		        updateKale();
		      }
		    });
		    container.add(RSlider);
		    final TextSlider ISlider = new TextSlider.Integer("initial points",  SwingConstants.HORIZONTAL, 2, 40, numInitPoints);
		    ISlider.addActionListener(new ActionListener()  {
		      public void actionPerformed(ActionEvent e)  {
		        numInitPoints = ISlider.getValue().intValue();
		        updateKale();
		      }
		    });
		    container.add(ISlider);
		    final TextSlider rSlider = new TextSlider.Double("power law",  SwingConstants.HORIZONTAL, 0.0, 2.0, powerLaw);
		    rSlider.addActionListener(new ActionListener()  {
		      public void actionPerformed(ActionEvent e)  {
		        powerLaw = rSlider.getValue().doubleValue();
		        updateKale();
		      }
		    });
		    container.add(rSlider);
		    final TextSlider rtSlider = new TextSlider.Double("width factor",  SwingConstants.HORIZONTAL, 0.0, 1.0, widthFactor);
		    rtSlider.addActionListener(new ActionListener()  {
		      public void actionPerformed(ActionEvent e)  {
		        widthFactor = rtSlider.getValue().doubleValue();
		        updateKale();
		      }
		    });
		    container.add(rtSlider);
		    final TextSlider foSlider = new TextSlider.Double("first order",  SwingConstants.HORIZONTAL, 0.0, 1.0, firstOrder);
		    foSlider.addActionListener(new ActionListener()  {
		      public void actionPerformed(ActionEvent e)  {
		        firstOrder = foSlider.getValue().doubleValue();
		        updateKale();
		      }
		    });
		    container.add(foSlider);
		    final TextSlider stepSlider = new TextSlider.Double("step size",  SwingConstants.HORIZONTAL, 0.0, 1.0, stepsize);
		    stepSlider.addActionListener(new ActionListener()  {
		      public void actionPerformed(ActionEvent e)  {
		        stepsize = stepSlider.getValue().doubleValue();
		        updateKale();
		      }
		    });
		    container.add(stepSlider);
			Box hbox = Box.createHorizontalBox();
			final JButton[] colorsb = new JButton[2];
			for (int i = 0; i<2; ++i)	{
				colorsb[i] = new JButton("color "+i);
				colorsb[i].setBackground(baseColors[i]);
				final int j = i;
				colorsb[i].addActionListener(new ActionListener()	{
					public void actionPerformed(ActionEvent e)	{
						Color color = JColorChooser.showDialog(null, "Select color ",  null);
						if (color != null) updateColors(color, j);
						colorsb[j].setBackground(color);
					}
				});
				hbox.add(colorsb[i]);
			}
			container.add(hbox);
		    container.setName("Parameters");
		    return container;
		  }

	private void updateColors(Color color, int i)	{
		baseColors[i] = color;
		updateKale();
	}

	public static void main(String[] args) {
		ViewerApp.display(new Kale().makeWorld());
	}


}
