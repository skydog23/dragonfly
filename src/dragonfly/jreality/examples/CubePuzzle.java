/*
 * Created on Jan 24, 2010
 *
 */
package dragonfly.jreality.examples;

import java.util.ArrayList;

import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.geometry.Primitives;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.plugin.JRViewer;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.util.SceneGraphUtility;

public class CubePuzzle extends LoadableScene {

	public SceneGraphComponent world = SceneGraphUtility.createFullSceneGraphComponent();
	int[][][] grid = new int[5][5][5];
	double[][] bounds = {{2,2,2},{2,2,2}};
	int[] puzzle  = {2, 2, 2, 2, 1, 1, 1, 2, 2, 1, 1, 2, 1, 2, 1, 1, 2};
	double[] solution = new double[puzzle.length];
	@Override
	public SceneGraphComponent makeWorld() {
		
		return world;
	}
	
	double[] currentPoint = {2,2,2};
	public void solveIt()	{
		grid[2][2][2] = 1;
		int parts = puzzle.length;
		// add the first segment in the positive x-direction
		doOneStep(0, 1, currentPoint, grid, bounds);
	}

	double[][] directions = {{-1,0,0},{1,0,0}, {0,-1,0},{0,1,0}, {0,0,-1},{0,0,1}};
	int[][] otherdirections = {{2,3,4,5}, {0,1,4,5}, {0,1,2,3}};
	private boolean doOneStep(int step, int direction, double[] cp, int[][][] grid2, double[][] bounds2) {
		if (step >= puzzle.length) {grid = grid2; return true;}
		int length = puzzle[step];
//		System.err.println("At step "+step+" dir "+direction);
		double[] nextP = cp.clone();
		for (int blocks = 0; blocks < length; ++blocks)	{
			Rn.add(nextP, nextP, directions[direction]);
			if (!canAdd(nextP, grid2, bounds2)) return false;
		} 
		nextP = cp.clone();
		for (int blocks = 0; blocks < length; ++blocks)	{
			Rn.add(nextP, nextP, directions[direction]);
			add(nextP, grid2, bounds2);
		} 
		solution[step] = direction;
		// added this segment successfully; now recur on the next step
		int[] dirs = otherdirections[direction/2];
		for (int i =0; i<4; ++i)	{
			double[][] bc = copyBounds(bounds2);
			int[][][] gc = copyGrid(grid2, bc);
			if (doOneStep(step+1, dirs[i], nextP.clone(), gc, bc)) return true;
		}
		return false;
	}

	public static void main(String[] args)	{
		CubePuzzle cp = new CubePuzzle();
		cp.solveIt();
		System.err.println("Soln = "+Rn.toString(cp.solution));
		JRViewer.display(cp.showGrid(cp.grid));
	}
	public boolean canAdd(double[] point, int[][][] grid, double[][] bounds)	{
		// goes outside 3x3x3 subgrid?
		System.err.println("Checking point "+Rn.toString(point));
		for (int i = 0; i<3; ++i)	{
			if ((point[i] + 2) < bounds[1][i] || (point[i] - 2) > bounds[0][i]) {
				System.err.println("Out of bounds");
				return false;
			}
		}
		// already occupied
		int[] ipoint = new int[]{(int) point[0], (int) point[1], (int) point[2]};
		if (grid[ipoint[2]][ipoint[1]][ipoint[0]] != 0) {
			System.err.println("already occupied");
			return false;
		}
		return true;
	}
	public void add(double[] point, int[][][] grid, double[][] bounds)	{
		int[] ipoint = new int[]{(int) point[0], (int) point[1], (int) point[2]};
		// it fits, insert and update bounds
		grid[ipoint[2]][ipoint[1]][ipoint[0]] = 1;
		System.err.println("Setting point "+Rn.toString(point));
		for (int i =0; i<3; ++i)	{
			if (ipoint[i] < bounds[0][i]) bounds[0][i] = ipoint[i];
			if (ipoint[i] > bounds[1][i]) bounds[1][i] = ipoint[i];
		}
	}
	
	int[][][] copyGrid(int[][][] g, double[][] b)	{
		int[][][] newG = new int[5][5][5];
		for (int i = (int) b[0][0]; i <= b[1][0]; ++i) 
			for (int j = (int) b[0][1]; j <= b[1][1]; ++j) 
				for (int k = (int) b[0][2]; k <= b[1][2]; ++k) 
					newG[k][j][i] = g[k][j][i];
		return newG;
	}
	
	double[][] copyBounds(double[][] b)	{
		double[][] nb = new double[2][3];
		for (int i = 0; i<2; ++i)	for (int j = 0; j<3; ++j)	
			nb[i][j] = b[i][j];
		return nb;
	}
	
	SceneGraphComponent showGrid(int[][][] g)	{
		SceneGraphComponent sgc = new SceneGraphComponent();
		ArrayList<SceneGraphComponent> list = new ArrayList<SceneGraphComponent>();
		for (int i = 0; i <= 4; ++i) 
			for (int j = 0; j <=4; ++j) 
				for (int k = 0; k <=4; ++k) {
					if (grid[k][j][i] == 0) continue;
					SceneGraphComponent child = new SceneGraphComponent();
					MatrixBuilder.euclidean().translate(2*i,2*j,2*k).assignTo(child);
					sgc.addChild(child);
					child.setGeometry(Primitives.coloredCube());
				}
		return sgc;
	}
}
