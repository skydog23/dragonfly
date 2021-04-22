package dragonfly.dynamen.ws08;

import java.util.ArrayList;
import java.util.List;

import de.jreality.geometry.IndexedLineSetUtility;
import de.jreality.geometry.QuadMeshFactory;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedLineSet;
import de.jreality.scene.Scene;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.DataList;
import de.jreality.scene.data.StorageModel;

public class SpringedILS extends SpringParticleSystemFactory {

	
	private IndexedLineSet ils;
	int numVerts = 50;
	boolean oneD = true;
	public SpringedILS()	{
		if (oneD) initializeCurve();
		else initializeMesh();
	}
	
	@Override
	public void initializeParticles() {
		System.err.println("Resetting");
		double[] masses = oneD ? initializeCurve() : initializeMesh();
		
		if (particles == null) particles = new ArrayList<Particle>();
		else particles.clear();
		int[][] edges = ils.getEdgeAttributes(Attribute.INDICES).toIntArrayArray(null);
		DataList dl = ils.getVertexAttributes(Attribute.attributeForName("masses"));
		if (dl != null) masses = dl.toDoubleArray(null);
		positions = initialPositions.clone();
		Scene.executeWriter(ils, new Runnable() {

			public void run() {
				ils.setVertexAttributes(Attribute.COORDINATES, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(positions));
			}
			
		});
		int num = positions.length;
		velocities = new double[num][3];
		SpringParticle[] array = new SpringParticle[num];
		for (int i = 0; i<num; ++i)	{
			SpringParticle sp = new SpringParticle(i);
			sp.setMass(masses[i]);
			sp.setPosition(positions[i]);
			array[i] = sp;
			particles.add(sp);
		}
		int count = 0;
		int numEdges = edges.length;
		for (int i = 0; i<numEdges; ++i)	{
			int[] edge = edges[i];
			int n = edge.length;
			for (int j = 0; j<n-1; ++j)	{
				int next = (j+1);
				int iv1 = edge[j], iv2 = edge[next];
				double[] v1 = positions[iv1], v2 = positions[iv2];
				double length = Rn.euclideanDistance(v1, v2);
				Spring spr = new Spring(array[iv1], array[iv2], length, stiffness, damping);
				List<Spring> spr1 = array[iv1].getSprings();
				if (!spr1.contains(spr)) {count++; spr1.add(spr);}
				List<Spring> spr2 = array[iv2].getSprings();
				if (!spr2.contains(spr)) {count++; spr2.add(spr);}
			}
		}
		System.err.println("created "+count+" springs.");
		
	}

	@Override
	public void update() {
		super.update();
		Scene.executeWriter(ils, new Runnable() {

			public void run() {
				ils.setVertexAttributes(Attribute.COORDINATES, StorageModel.DOUBLE_ARRAY.array(3).createReadOnly(positions));
				System.err.println("updated SpringILS");
			}
			
		});
		
	}
	
	private double[] initializeCurve() {
		double[][] verts = new double[numParticles][];
		for (int i = 0; i<numParticles; ++i)	{
			double x = (i-numParticles/2)/(numParticles/2.0);
			verts[i] = new double[]{x, 0, 1-.001*x*x};
			if (i == numParticles/2) verts[i][2] = 1.1;
		}
		ils = IndexedLineSetUtility.createCurveFromPoints(ils, verts, false);
		double[] masses = new double[ils.getNumPoints()];
		for (int i = 0; i<masses.length; ++i) masses[i] = 1.0/numParticles;
		masses[0] = masses[masses.length-1] = 0.0;
		ils.setVertexAttributes(Attribute.attributeForName("masses"), StorageModel.DOUBLE_ARRAY.createReadOnly(masses));
		initialPositions = ils.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		return masses;
	}

	private double[] initializeMesh()	{
		double[][] verts = new double[numParticles*numParticles][];
		double[] masses = new double[numParticles*numParticles];
		double mass = 1.0/(numParticles*numParticles);
		for (int i = 0; i<numParticles; ++i)	{
			double x = (i-numParticles/2)/(numParticles/2.0);
			for (int j = 0; j<numParticles; ++j)	{
				double y = (j-numParticles/2)/(numParticles/2.0);
				verts[i*numParticles+j] = new double[]{x, y, 1+.001*(x*x+y*y)};
				//if (i == numParticles/2 && j == numParticles/2) verts[i*numParticles+j][2] = 1.1;
				masses[i*numParticles+j] =  x*x+y*y < .8 ? 0 : mass;
			}
		}
		QuadMeshFactory qmf = new QuadMeshFactory();
		qmf.setClosedInUDirection(false);
		qmf.setClosedInUDirection(false);
		qmf.setULineCount(numParticles);
		qmf.setVLineCount(numParticles);
		qmf.setVertexCoordinates(verts);
		qmf.setGenerateEdgesFromFaces(true);
		qmf.setGenerateFaceNormals(true);
		qmf.update();
		ils = qmf.getIndexedLineSet(); //IndexedLineSetUtility.createCurveFromPoints(ils, verts, false);
		ils.setVertexAttributes(Attribute.attributeForName("masses"), StorageModel.DOUBLE_ARRAY.createReadOnly(masses));
		initialPositions = ils.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);		
		return masses;
	}

	public IndexedLineSet getIls() {
		return ils;
	}

	public void setIls(IndexedLineSet ils) {
		this.ils = ils;
	}


}
