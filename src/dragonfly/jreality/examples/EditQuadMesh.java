package dragonfly.jreality.examples;

import javax.swing.ImageIcon;
import javax.swing.JMenuBar;

import charlesgunn.jreality.tools.MouseTool;
import charlesgunn.jreality.viewer.LoadableScene;
import de.jreality.geometry.SphereUtility;
import de.jreality.jogl.plugin.HelpOverlay;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Viewer;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.StorageModel;
import de.jreality.scene.pick.PickResult;
import de.jreality.scene.tool.Tool;
import de.jreality.scene.tool.ToolContext;
import de.jreality.shader.CommonAttributes;
import de.jreality.util.SceneGraphUtility;
import de.jtem.discretegroup.util.WingedEdge;
import de.jtem.discretegroup.util.WingedEdgeUtility;

public class EditQuadMesh extends LoadableScene {

	SceneGraphComponent theWorld;
	private WingedEdge icosa80;
	double[][] edgeColors;
	double[] blue = {0,0,1,1}, red = {1,0,0,1};
	@Override
	public SceneGraphComponent makeWorld() {
		theWorld = SceneGraphUtility.createFullSceneGraphComponent();
		Appearance ap = theWorld.getAppearance();
		ap.setAttribute(CommonAttributes.LINE_SHADER+"."+CommonAttributes.TUBES_DRAW, true);
		IndexedFaceSet ifs = SphereUtility.tessellatedIcosahedronSphere(1);
		icosa80 = WingedEdgeUtility.convertConvexPolyhedronToWingedEdge(ifs); //new WingedEdge(1.0);
		edgeColors = new double[icosa80.getNumEdges()][];
		for (int i = 0; i<edgeColors.length; ++i) edgeColors[i] = blue;
		icosa80.setEdgeAttributes(Attribute.COLORS, StorageModel.DOUBLE_ARRAY.array(4).createReadOnly(edgeColors));
		theWorld = SceneGraphUtility.createFullSceneGraphComponent("world");
		theWorld.setGeometry(icosa80);
		
		return theWorld;
	}

	@Override
	public void customize(JMenuBar menuBar, Viewer viewer) {
		Tool specialTool = new MouseTool()	{
			
			public void perform(ToolContext tc) {
//				System.err.println("In perform");
				super.perform(tc);
			}


			@Override
			public void deactivate(ToolContext tc) {
				super.deactivate(tc);
//				System.err.println(tc.getCurrentPick().toString());
				if (button == 1)	{
					if (tc.getCurrentPick() == null) return;
					if (tc.getCurrentPick().getPickType() != PickResult.PICK_TYPE_LINE) return;
					int whichEdge = tc.getCurrentPick().getIndex();
					edgeColors[whichEdge] = (edgeColors[whichEdge] == red ? blue : red);
					icosa80.setEdgeAttributes(Attribute.COLORS, StorageModel.DOUBLE_ARRAY.array(4).createReadOnly(edgeColors));
					viewer.render();
					System.err.println("Edge"+whichEdge);
					SceneGraphPath sgp = tc.getRootToLocal();	
				} else if (button == 2)	{
					int redcount = 0;
					for (int i = 0; i<edgeColors.length; ++i)	{
						if (edgeColors[i] == red) redcount++;
					}
					int[] wegmit = new int[redcount];
					redcount = 0;
					for (int i = 0; i<edgeColors.length; ++i)	{
						if (edgeColors[i] == red) {
							wegmit[redcount++] = i;
							System.err.print(i+", ");
						}
					}
					icosa80.removeEdge(wegmit);
				}
				viewer.renderAsync();
			}
			
			@Override
			public ImageIcon getIcon(int size) {
				// TODO Auto-generated method stub
				return null;
			}

			@Override
			public String getName() {
				// TODO Auto-generated method stub
				return null;
			}	


			@Override
			public void registerHelp(HelpOverlay overlay) {
				// TODO Auto-generated method stub
				
			}
			
		};
		theWorld.addTool(specialTool);

	}

	@Override
	public boolean isEncompass() {
		return true;
	}

}
