package dragonfly.jreality.examples;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;

import charlesgunn.jreality.viewer.Assignment;
import de.jreality.geometry.GeometryMergeFactory;
import de.jreality.geometry.IndexedFaceSetUtility;
import de.jreality.geometry.ParametricTriangularSurfaceFactory;
import de.jreality.geometry.ParametricTriangularSurfaceFactory.Immersion;
import de.jreality.geometry.Primitives;
import de.jreality.geometry.RemoveDuplicateInfo;
import de.jreality.geometry.SphereUtility;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.CommonAttributes;
import de.jreality.ui.viewerapp.FileLoaderDialog;
import de.jreality.ui.viewerapp.actions.file.ExportOBJ;
import de.jreality.util.SceneGraphUtility;
import de.jreality.writer.WriterOBJ;

public class JointsForPolyhedronModels extends Assignment {

	double[][] mats = {Rn.identityMatrix(4),
		P3.makeRotationMatrixX(null, Math.PI/2),
		P3.makeRotationMatrixY(null, Math.PI/2),
	};
	Color[] cs = {Color.red, Color.blue, Color.green};
	int resolution = 20;
	boolean debugOneThird = false;
	double
			r1 = 1.0,
			r2 = 0.8,
			holeDepth = 2.0,
			zmax = 3.0;
	SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent("world");
	@Override
	public SceneGraphComponent getContent() {
		SceneGraphComponent cyls = SceneGraphUtility.createFullSceneGraphComponent("cyls"),
				oneThirdSGC = SceneGraphUtility.createFullSceneGraphComponent("one third"),
				annSGC = SceneGraphUtility.createFullSceneGraphComponent("annulus"),
				discSGC = SceneGraphUtility.createFullSceneGraphComponent("disc"),
				disc2SGC = SceneGraphUtility.createFullSceneGraphComponent("disc2"),
				cyl1SGC = SceneGraphUtility.createFullSceneGraphComponent("cyl1"),
				cyl2SGC = SceneGraphUtility.createFullSceneGraphComponent("cyl2");
		world.addChild(cyls);
		oneThirdSGC.addChildren(annSGC, discSGC, disc2SGC, cyl1SGC, cyl2SGC);
			
		IndexedFaceSet cyl = Primitives.cylinder(resolution,r1,0.0,zmax,Math.PI*2);
		cyl1SGC.setGeometry(cyl);
		cyl = Primitives.cylinder(resolution,r2,zmax - holeDepth,zmax,Math.PI*2);
		cyl2SGC.setGeometry(cyl);
		IndexedFaceSet annFaceSet = Primitives.regularAnnulus(resolution, 0, r2);
		annSGC.setGeometry(annFaceSet);
		MatrixBuilder.euclidean().translate(0, 0, zmax).assignTo(annSGC);
		IndexedFaceSet discFaceSet = Primitives.regularPolygon(resolution, 0);
		discSGC.setGeometry(discFaceSet);
		MatrixBuilder.euclidean().translate(0, 0, zmax - holeDepth).assignTo(discSGC);
		disc2SGC.setGeometry(discFaceSet);
//		MatrixBuilder.euclidean().translate(0, 0, -1).assignTo(disc2SGC);
		
		for (int i = 0; i<3; ++i)	{
			SceneGraphComponent child = SceneGraphUtility.createFullSceneGraphComponent("child"+i);
			cyls.addChild(child);
			child.addChild(oneThirdSGC);
			child.setVisible(i == 0 || !debugOneThird);
			child.getAppearance().setAttribute("polygonShader.diffuseColor", cs[i]);
			new Matrix(mats[i]).assignTo(child);
		}
//		cyls.setVisible(false);
		ParametricTriangularSurfaceFactory ptsf = new ParametricTriangularSurfaceFactory();
		ptsf.setImmersion(new Immersion() {
			
			@Override
			public boolean isImmutable() {
				// TODO Auto-generated method stub
				return false;
			}
			
			@Override
			public int getDimensionOfAmbientSpace() {
				// TODO Auto-generated method stub
				return 3;
			}
			
			@Override
			public void evaluate(double u, double v, double[] xyz, int index) {
//				xyz[index+0]=u; 
//				xyz[index+1]=v;				
				double phi = (Math.PI/2)*v, 
						theta = v!=1 ? ((Math.PI/2)*u)/(1 -v) : Math.PI/2*u;
				xyz[0+index] = -Math.cos(phi)*Math.cos(theta);
				xyz[1+index] = Math.cos(phi)*Math.sin(theta);
				xyz[2+index] = -Math.sin(phi);
			}
		});
		ptsf.setSubdivision(resolution/4 + 1);
		ptsf.setGenerateFaceNormals(true);
		ptsf.setGenerateEdgesFromFaces(true);
		ptsf.update();
		SceneGraphComponent sphSGC = SceneGraphUtility.createFullSceneGraphComponent("sphere");
		sphSGC.setGeometry(ptsf.getGeometry());		
		world.addChild(sphSGC);
		sphSGC.setVisible(!debugOneThird);
		world.getAppearance().setAttribute("polygonShader.diffuseColor", Color.white);
//		world.getAppearance().setAttribute(CommonAttributes.EDGE_DRAW,false);
		world.getAppearance().setAttribute(CommonAttributes.TUBES_DRAW, false);
		return world;
	}

	
	@Override
	public void display() {
		// TODO Auto-generated method stub
		super.display();
		
		((Component) viewer.getViewingComponent()).addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e)	{ 
				int m = e.getModifiers();
//				System.err.println("Modifiers = "+m);
				switch(e.getKeyCode())	{
				
				case KeyEvent.VK_1:
					myWriteOBJ();
					break;
					
				}
			}
		});

	}


	protected void myWriteOBJ() {
			
		Component pc = ((Component) viewer.getViewingComponent()).getParent();
		File file = FileLoaderDialog.selectTargetFile(pc, null, "obj", "OBJ Files");
		if (file == null) return;  //dialog cancelled

		try {
	        GeometryMergeFactory mergeFact= new GeometryMergeFactory();
	        mergeFact.setCollectInvisible(false);
	        mergeFact.setGenerateVertexNormals(true);
	        mergeFact.setGenerateFaceNormals(true);
	        IndexedFaceSet result=mergeFact.mergeGeometrySets(world);
	        result = (IndexedFaceSet) RemoveDuplicateInfo.removeDuplicateVertices(result, (Attribute[]) null);
	        boolean orient = IndexedFaceSetUtility.makeConsistentOrientation(result);
	        System.err.println("Export OBJ: oriented = "+orient);
			WriterOBJ.write(result, new FileOutputStream(file));
		} catch (Exception exc) {
			exc.printStackTrace();
		}			
		}



	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new JointsForPolyhedronModels().display();
	}

}
