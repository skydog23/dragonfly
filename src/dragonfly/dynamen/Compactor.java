package dragonfly.dynamen;

import java.util.WeakHashMap;

import charlesgunn.jreality.geometry.Snake;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Pn;
import de.jreality.scene.Geometry;
import de.jreality.scene.PointSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphVisitor;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.DataList;
import de.jreality.scene.data.StorageModel;
import de.jreality.util.CopyVisitor;
import de.jreality.util.SceneGraphUtility;

public class Compactor  extends SceneGraphVisitor{
	int metric = Pn.HYPERBOLIC;
	WeakHashMap<PointSet, DataList> toWrite = new WeakHashMap<PointSet, DataList>();
	SceneGraphComponent root, newRoot;
	public Compactor(SceneGraphComponent r, int s)	{
		root = r;
		metric = s;
	}
	
	int[] snakeinfo;
	public SceneGraphComponent visit()	{
		Geometry foo = SceneGraphUtility.getFirstGeometry(root);
		if (foo instanceof Snake) {
			snakeinfo  = ((Snake) foo).getInfo();
		} else snakeinfo = null;
		SceneGraphComponent flat = SceneGraphUtility.flatten(root);
		flat.setName("Compact");
		visit(flat);
		for(PointSet ps : toWrite.keySet())	{
			DataList dl = toWrite.get(ps);
			ps.setVertexAttributes(Attribute.COORDINATES, dl);
		}
		if (metric == Pn.ELLIPTIC)	{
			newRoot = new SceneGraphComponent("elliptic compact");
			SceneGraphComponent pointMirror = new SceneGraphComponent("pointMirror");
			newRoot.addChildren(pointMirror, flat);
			pointMirror.addChild(flat);
			MatrixBuilder.euclidean().scale(-1, -1, -1).assignTo(pointMirror);
		} else newRoot = flat;
		return newRoot;
	}
	
		@Override
		public void visit(PointSet p) {
			int length = p.getNumPoints();
			if (snakeinfo != null) {
//				System.err.println("info = "+info[0]+" "+info[1]+" "+info[2]);
				length = snakeinfo[1];
			}
			System.err.println("length = "+length);
			double[][] points = p.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
			int fiber = points[0].length;
			for (int i = 0; i<length; ++i)	{
				double[] point = points[i];
				compactify(point, metric);				
			}
			toWrite.put(p,  StorageModel.DOUBLE_ARRAY.array(fiber).createReadOnly(points));
		}

		public static void compactify(double[] point, int metric) {
			double[] source = point.clone();
			double w = 1;
			int fiber = point.length;
			if (fiber == 4)  {w = source[3] == 0 ? 1 : Math.abs(source[3]); }
			double x = source[0]/w, y = source[1]/w;
			double r2 = x*x + y*y;
			if (metric == Pn.HYPERBOLIC)	{
				// stereographic projection
				double k = 1/(r2+1);
				point[0]  = 2*k*x;
				point[1] = 2*k*y;
				point[2] = k*(1 - r2);
				if (fiber == 4) point[3] = source[3]/w;
			} else if (metric == Pn.ELLIPTIC)	{
				// central projection
				double k = 1/Math.sqrt((r2+1));
				point[0]  = k*x;
				point[1] = k*y;
				point[2] = k;
				if (fiber == 4) point[3] = source[3]/w;				
			}
		}

		@Override
		public void visit(SceneGraphComponent c) {
			c.childrenAccept(this);
		}
	
	protected class FullCopyVisitor extends CopyVisitor {

		@Override
		public void visit(SceneGraphComponent c) {
			super.visit(c);
			SceneGraphComponent newSGC = (SceneGraphComponent) getCopy();
			for (SceneGraphComponent child : c.getChildComponents())	{
				visit(child);
				newSGC.addChild((SceneGraphComponent) getCopy());
			}
		}

		
	}
}
