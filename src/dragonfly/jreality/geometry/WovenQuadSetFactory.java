/*
 * Created on Dec 28, 2006
 *
 */
package dragonfly.jreality.geometry;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.Box;
import javax.swing.SwingConstants;

import charlesgunn.jreality.geometry.BezierCurve;
import charlesgunn.jreality.geometry.BezierPatchMeshTubeFactory;
import charlesgunn.jreality.geometry.GeometryUtilityOverflow;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.BezierPatchMesh;
import de.jreality.geometry.FrameFieldType;
import de.jreality.geometry.TubeUtility.FrameInfo;
import de.jreality.math.Matrix;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.Appearance;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.data.Attribute;
import de.jreality.scene.data.DoubleArrayArray;
import de.jreality.util.SceneGraphUtility;

public class WovenQuadSetFactory {

		IndexedFaceSet surface;
		int[][] faceIndices = null;
		Quadrilateral[] quads = null;
		int metric = Pn.EUCLIDEAN;
		double radiusScale = .35,
			splineTension = 1.0,
			squashFactor = .5, 
			boundaryBoost = 2.0;
//		double[][] crossSection = circle;
		static double x = .5;
		static double[][] circle =  
			{{x, 0, 0, 1},	 
			{x, 1,0, 1}, 
			{0,2, 0, 2}, 
			{-x, 1,0, 1}, 
			{-x, 0, 0, 1},
			{-x, -1,0, 1},
			{0,-2, 0, 2},
			{x, -1,0, 1},
			{x, 0, 0, 1}};
		public static BezierCurve crossSection = new BezierCurve(2, circle);
		int refineLevel = 2;
		boolean debug = false, showTubes = true;
		ArrayList<List<Edge>> curves = new ArrayList<List<Edge>>();
		HashMap<Edge, Edge> edgeNeighbors = new HashMap<Edge, Edge>();
		HashMap<List<Edge>, BezierPatchMeshTubeFactory> curveFactories = new HashMap<List<Edge>, BezierPatchMeshTubeFactory>();
		private Collection<Edge> edgeSet;	
		Appearance[] appearances = null;
		
		SceneGraphComponent world = new SceneGraphComponent("WovenQuadMeshFactory"),
			debugSGC = new SceneGraphComponent("debugWQMF"),
			realSGC = new SceneGraphComponent("realWQMF");
		private ArrayList<Edge> unmatchedEdges;

		public WovenQuadSetFactory(IndexedFaceSet s)	{
			this(s, null);
		}
		
		public WovenQuadSetFactory(IndexedFaceSet s, WovenQuadSetFactory old)	{
			surface = s;
			faceIndices = s.getFaceAttributes(Attribute.INDICES).toIntArrayArray(null);
			if (!isQuadSet(s)) 
				throw new IllegalStateException("Must consist only of quadrilaterals");
			world.addChildren(debugSGC, realSGC);
			if (old != null) {
				setRefineLevel(old.refineLevel);
				setRadiusScale(old.radiusScale);
				setSplineTension(old.splineTension);
				setSquashFactor(old.squashFactor);
				setBoundaryBoost(old.boundaryBoost);				
			}
			getInspector();
			setDebug(debug);
			setup();
		}
		
		public SceneGraphComponent getSceneGraphComponent()	{
			return world;
		}
		boolean isQuadSet(IndexedFaceSet s)	{
			for (int[] face : faceIndices)	{
				if (face.length != 4) return false;
			}
			return true;
		}
		
		
		void setup()	{
			quads = new Quadrilateral[faceIndices.length];
			// put the edges into the hash map
			// and calculate the centers, midpoints, and plane equation for the face
			double[][] verts = new double[4][];
			int count = 0;
			DoubleArrayArray vertDL = surface.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray();
			for (int i = 0; i<faceIndices.length; ++i)	{
				Quadrilateral thisQ = quads[i] = new Quadrilateral();
				thisQ.fIndex = i;
				int[] face = faceIndices[i];
				thisQ.midpoints = new double[4][];
				for (int j = 0; j<4; ++j)	{
					int k = (j+1)%4;
					verts[j] = vertDL.item(face[j]).toDoubleArray(null);
					if (verts[j].length == 3) 
						verts[j] = Pn.homogenize(null, verts[j]);
					Edge thisE = new Edge(face[j], face[k], i, j);
					Edge schonDa = edgeNeighbors.get(thisE);
					if (schonDa != null)	{
						schonDa.f[1] = i;
						schonDa.e[1] = j;
						schonDa.sameType = ((schonDa.e[0] + schonDa.e[1]) % 2) == 0;
						thisQ.neighbors[j] = quads[schonDa.f[0]];
						quads[schonDa.f[0]].neighbors[schonDa.e[0]]=thisQ;
						thisE = schonDa;
					} else {
						edgeNeighbors.put(thisE, thisE);
//						System.err.println("Adding edge "+thisE.v[0]+":"+thisE.v[1]+" "+count+" to list");
						count++;
					}
					thisQ.edges[j] = thisE;
				}
				thisQ.unitLength = 0.0;
				for (int j = 0; j<4; ++j)	{			
					int k = (j+1)%4;
					// TODO this is not non-euclidean safe
					thisQ.midpoints[j] = Rn.linearCombination(null, .5, verts[j], .5, verts[k]);
					thisQ.unitLength += Rn.euclideanDistance(verts[j], verts[k]);
				}
				// unitlength is half the average side length
				thisQ.unitLength *= .125;
				thisQ.center = Rn.average(null, verts);
				thisQ.plane = P3.planeFromPoints(null, thisQ.midpoints[0], thisQ.midpoints[1], thisQ.midpoints[2]);
			}
			// find the neighbors for each face 
			// and set the "directions" based on these neighbors
			for (int i = 0; i<faceIndices.length; ++i)	{
				Quadrilateral thisQ = quads[i];
				int[] face = faceIndices[i];
//				for (int j = 0; j<4; ++j)	{
//					int k = (j+1)%4;
//					// look for the reversed edge 
//					Edge e = new Edge(face[k], face[j], i, j);
//					Edge neighbor = edgeNeighbors.get(e);
//					if (neighbor != null)	{
//						thisQ.neighbors[j] = quads[neighbor.f[1]];
//					}
//				}
				// calculate the "cardinal" directions for this face
				for (int j = 0; j<4; ++j)	{
					int k = (j+2)%4;
					if (thisQ.neighbors[j] != null && thisQ.neighbors[k] != null)
						thisQ.dirs[j] = Rn.subtract(null, thisQ.neighbors[j].center, thisQ.neighbors[k].center);
					else if (thisQ.neighbors[j] != null)	
						thisQ.dirs[j] = Rn.subtract(null, thisQ.neighbors[j].center, thisQ.center);
					else if (thisQ.neighbors[k] != null)	
						thisQ.dirs[j] = Rn.subtract(null, thisQ.center, thisQ.neighbors[k].center);
					else thisQ.dirs[j] = Rn.subtract(null, thisQ.midpoints[j], thisQ.midpoints[k]);
				}
			}
			// finally, propagate orientation: this results in a "checkerboard" pattern
			// for the quadrilaterals
			quads[0].orientation = 0;
			ArrayList<Quadrilateral> inProcess = new ArrayList<Quadrilateral>();
			inProcess.add(quads[0]);
			while (! inProcess.isEmpty())	{
				Quadrilateral thisQ = inProcess.get(0);
				for (int j = 0; j<4; ++j)	{
					Quadrilateral nhbr = thisQ.neighbors[j];
					Edge e = thisQ.edges[j];
					if (nhbr != null)	{
						int m = ((e.sameType ? 1 : 0) + thisQ.orientation)%2;
						if (nhbr.orientation == -1) {
							nhbr.orientation = m;
							inProcess.add(nhbr);
						}
						else {
							if (nhbr.orientation != m)	{
								System.err.println("Inconsistent orientation "+e.toString());
							}
						}
					}
				}
				//System.err.println("In process population: "+inProcess.size());
				inProcess.remove(thisQ);
			}
			
			unmatchedEdges = new ArrayList<Edge>();
			edgeSet = edgeNeighbors.values();
			for (Edge e: edgeSet)	{
				if (e.f[1] == -1)	{
					e.boundary = true;
					unmatchedEdges.add(e);
				}
			}
			
//			Edge e = null;
//			while (!unmatchedEdges.isEmpty())	{
//				if (e == null) e = unmatchedEdges.get(0);
//				findPartnerForEdge( e);
//				if (unmatchedEdges.size() > 0) e = unmatchedEdges.get(0);
//			}
			ArrayList<Edge> remainingEdges = new ArrayList<Edge>();
			remainingEdges.addAll(edgeSet);
			//System.err.println("edge count is "+remainingEdges.size());
			
			// find the curves
			while (! remainingEdges.isEmpty())	{
				Edge beginningEdge = remainingEdges.get(0);
				//System.err.println("Getting edge "+beginningEdge.v[0]+":"+beginningEdge.v[1]);
				Edge currentEdge = beginningEdge;
				// go in one direction from this edge
				int which = 0;
				LinkedList<Edge> thisCurve = new LinkedList<Edge>();
				curves.add(thisCurve);
				currentEdge = extendCurve(remainingEdges, beginningEdge, which, thisCurve);
				if (currentEdge != null)	continue;
				// now go in the other direction if it's not a cycle
				which = 1;  
				currentEdge = extendCurve(remainingEdges, beginningEdge, which, thisCurve);
				//System.err.println("");
			}
		}

		ArrayList<Edge> partners = new ArrayList<Edge>();
		private TextSlider boundaryBoostSlider;
		private TextSlider squashFactorSlider;
		private TextSlider splineTensionSlider;
		private TextSlider radiusScaleSlider;
		private TextSlider refineLevelSlider;
		private Box inspectionPanel;
		private void findPartnerForEdge(Edge e) {
			if ( e != null) unmatchedEdges.remove(e);
			partners.clear();
			for (int i = 0; i<unmatchedEdges.size(); ++i)	{
				Edge f = unmatchedEdges.get(i);
				if (sameFace(e,f)) continue;
				if (e.v[0] == f.v[0] || e.v[0] == f.v[1] || e.v[1] == f.v[0] || e.v[1] == f.v[1])	{
					partners.add(f);
				}
			}
			if (partners.isEmpty()) {
				System.err.println("Edge has no partner: "+e.toString());
				return;
//					continue;
			}
			Edge thePartner = partners.get(0);
			if (partners.size() > 1) {
				for (Edge f : partners)	{
					// avoid connecting corners to themselves
					if (f.f[0] != e.f[0]) {
						thePartner = f;
						break;						
					}
				}
			}
			System.err.println("Found partners: "+e.toString()+" "+thePartner.toString());
			partnerize(e, thePartner);
			partners.remove(thePartner);
			unmatchedEdges.remove(thePartner);
//			if (partners.size() > 1) {
//				e = partners.get(0);
//			}
//			else e = unmatchedEdges.get(0);
		}

		private boolean sameFace(Edge e, Edge f) {
			if (e.f[0] == -1) {
				if (f.f[0] == -1) return e.f[1] == f.f[1];
				else if (f.f[1] == -1) return e.f[1] == f.f[0];
				else return e.f[1] == f.f[1] || e.f[1] == f.f[0];
			} else if (e.f[1] == -1) {
				if (f.f[0] == -1) return e.f[0] == f.f[1];
				else if (f.f[1] == -1) return e.f[0] == f.f[0];
				else return e.f[0] == f.f[1] || e.f[0] == f.f[0];
			}			
			return e.f[0] == f.f[0] || e.f[1] == f.f[0] || e.f[0] == e.f[1] || e.f[1] == f.f[1];
		}

		/**
		 * @param e
		 * @param f
		 */
		private void partnerize(Edge e, Edge f) {
			// Match!
			// TODO find all (possibly two) candidates and choose one;
			// then continue algorithm using the second candidate as e
			e.f[1] = f.f[0];
			e.e[1] = f.e[0];
			f.f[1] = e.f[0];
			f.e[1] = e.e[0];
			quads[e.f[0]].neighbors[e.e[0]] = quads[e.f[1]];
			quads[e.f[1]].neighbors[e.e[1]] = quads[e.f[0]];
			
			quads[f.f[0]].neighbors[f.e[0]] = quads[f.f[1]];
			quads[f.f[1]].neighbors[f.e[1]] = quads[f.f[0]];
		}

		/**
		 * @param remainingEdges
		 * @param currentEdge
		 * @param which
		 * @param thisCurve
		 * @return
		 */
		private Edge extendCurve(ArrayList<Edge> remainingEdges, Edge currentEdge, int w, LinkedList<Edge> thisCurve) {
			int which = w;
			boolean crossing = false;
			if (!thisCurve.contains(currentEdge)) currentEdge.which = (w == 0)?  1 - which : which;
			do {
				if (unmatchedEdges.contains(currentEdge)) {
					findPartnerForEdge(currentEdge);
				}
				remainingEdges.remove(currentEdge);
				if (currentEdge.f[1] != -1 && !crossing && !thisCurve.contains(currentEdge)) {
					if (w == 0 ) thisCurve.addFirst(currentEdge);
					else thisCurve.addLast(currentEdge);					
				}
				// calculate the next edge 
				int nextFace = currentEdge.f[which];
				int nextEdgeIndex = currentEdge.e[which];
				if (nextEdgeIndex != -1)	{
					if (currentEdge.boundary && which == 1)	{ // crossing to another boundary edge
						currentEdge = quads[nextFace].edges[nextEdgeIndex];
						which = 0;
						crossing = true;
					} else {
						currentEdge = quads[nextFace].edges[(nextEdgeIndex+2)%4];	
						which = (currentEdge.f[0] == nextFace) ? 1 : 0;
						crossing = false;
					}
					currentEdge.which = (w == 0)?  1 - which : which;
					//System.err.println("crossing edge "+currentEdge.v[0]+":"+currentEdge.v[1]);
				} else currentEdge = null;
				//System.err.print("<"+nextFace);
			} while ( !(thisCurve.contains(currentEdge)) && currentEdge != null);
			return currentEdge;
		}

		/**
		 * 
		 */
		public void update() {
			// calculate frame matrices which map the x-y plane (where cross section curve lives)
			// into the horizontal and vertical tube cross sections
			SceneGraphUtility.removeChildren(realSGC);
			SceneGraphUtility.removeChildren(debugSGC);
			for (int i = 0; i<quads.length; ++i)	{
				Quadrilateral thisQ = quads[i];
				for (int j = 0; j < 4; ++j)	{
					Matrix m = new Matrix();
					if (thisQ.dirs[j] == null) continue;
					double[] N = Pn.setToLength(null, thisQ.dirs[j], 1.0,metric);
					double[] B = Pn.setToLength(null,
							Pn.polarize(null, thisQ.plane, metric), 1.0, metric);
					// TODO this and the following don't work for non-euclidean metric
					double[] T = Rn.crossProduct(null, N, B);
					double updown = ((thisQ.orientation + j) % 2) == 0 ? 1 : -1;
					double radscale = thisQ.unitLength*radiusScale * squashFactor; //.5*Pn.norm(thisQ.dirs[(j+1)%4], metric)*radiusScale*squashFactor;
					m.setColumn(0, N);
					m.setColumn(1, B);
					m.setColumn(2, T);
					m.setColumn(3, Rn.add(null, 
							Rn.times(null, updown * radscale, B),
							thisQ.center));
					thisQ.frames[(j+1)%4] = Rn.times(null, m.getArray(),
							P3.makeStretchMatrix(null, radscale));
					
				}
			}
			edgeSet = edgeNeighbors.values();
			int[] forward = {0,1,2,3}, backward = {3,2,1,0};
			int[] i;
			for (Edge thisE : edgeSet)	{
				i = thisE.which == 1 ? forward : backward;
				Quadrilateral thisQ = quads[thisE.f[0]];
				int j = thisE.e[0];
				//if (thisQ.neighbors[j] == null) continue;
				if (thisE.f[1] == -1) continue;
				Quadrilateral thatQ = quads[thisE.f[1]];
				int oj = thisE.e[1];
				Matrix mm = new Matrix(thisQ.frames[j]);
				thisE.coreCurve[i[0]] = mm.getColumn(3);
				thisE.binormals[i[0]] = mm.getColumn(1);
				mm = new Matrix(thatQ.frames[oj]);
				thisE.coreCurve[i[3]] = mm.getColumn(3);
				thisE.binormals[i[3]] = mm.getColumn(1);
				double bb = (thisE.boundary) ? boundaryBoost : 1.0;
				double[] tangent1 = Rn.times(null, bb*splineTension*1/6.0, thisQ.dirs[j]);
				double[] tangent2 = Rn.times(null, bb*splineTension*1/6.0, thatQ.dirs[oj]);
				thisE.coreCurve[i[1]] = Rn.add(null, thisE.coreCurve[i[0]], tangent1);
				thisE.coreCurve[i[2]] = Rn.add(null, thisE.coreCurve[i[3]], tangent2);
				thisE.binormals[i[1]] = Rn.linearCombination(null, 2/3.0, thisE.binormals[i[0]], 1/3.0, thisE.binormals[i[3]]);
				thisE.binormals[i[2]] = Rn.linearCombination(null, 1/3.0, thisE.binormals[i[0]], 2/3.0, thisE.binormals[i[3]]);
				thisE.radii[i[0]] =  thisQ.unitLength*radiusScale; //.5*Pn.norm(thisQ.dirs[i[(j+1)%4]], metric) * radiusScale; //
				thisE.radii[i[3]] = thatQ.unitLength*radiusScale; // .5*Pn.norm(thatQ.dirs[i[(oj+1)%4]], metric) * radiusScale; //
				thisE.radii[i[1]] = thisE.radii[i[0]] * .6667  + thisE.radii[i[3]] * .3333;
				thisE.radii[i[2]] = thisE.radii[i[0]] * .3333 +  thisE.radii[i[3]] * .6667;
				
			}
			
			double[][] squashedCrossSection = Rn.matrixTimesVector(null, 
					P3.makeScaleMatrix(null, new double[]{1, squashFactor, 1}),
					crossSection.getControlPoints());
			System.err.println("Found "+curves.size()+" curves");
			int curveCount = 0;
			for (List<Edge> curve : curves)	{
				
				BezierPatchMesh bpm = null;
				boolean coreBezier = false;
				int n = curve.size();
				int numKnots = n*2 + 2;
				double[][] curvePoints = new double[numKnots][];
				double[][] curveBinormals = new double[n+1][];
				double[] curveRadii = new double[numKnots];
				boolean firstTime = true;
				int counter = 0, counter2 = 0;
				Edge e = null;
				for (Edge thisE: curve)	{
					//System.err.println("processing edge "+thisE.v[0]+":"+thisE.v[1]);
					if (thisE.coreCurve == null) {
						throw new IllegalStateException("Null pointers");
					}
					if (firstTime)	{
						curvePoints[counter] = Rn.add(null, thisE.coreCurve[0], 
								Rn.subtract(null, thisE.coreCurve[0], thisE.coreCurve[1]));
//						curveBinormals[counter] = Rn.add(null, thisE.binormals[0], 
//								Rn.subtract(null, thisE.binormals[0], thisE.binormals[1]));
						curveRadii[counter] = thisE.radii[0] * 1.3333 - thisE.radii[1]*.3333;
						firstTime = false;
						counter++;
					}
					curvePoints[counter] = thisE.coreCurve[1];
					curvePoints[counter+1] = thisE.coreCurve[2];
					curveBinormals[counter2++] = thisE.binormals[0];
					curveRadii[counter] = thisE.radii[1];
					curveRadii[counter+1] = thisE.radii[2];
					counter += 2;
					e = thisE;
				}
				curveBinormals[n] = e.binormals[3];
				curvePoints[counter] = Rn.add(null, e.coreCurve[3], 
							Rn.subtract(null, e.coreCurve[3], e.coreCurve[2]));
//				curveBinormals[counter] = Rn.add(null, e.binormals[3], 
//						Rn.subtract(null, e.binormals[3], e.binormals[2]));
				curveRadii[counter] = e.radii[3] * 1.3333 - e.radii[2]*.3333;
				counter++;
				//System.err.println("filled points: "+counter+"Expected: "+curvePoints.length);
				//System.err.println("Point by point: "+Rn.toString(curvePoints));
//				if (!testSplines)	{
//					double[][][] controlPoints = new double[4][circle.length][circle[0].length];
//					controlPoints[0] = Rn.matrixTimesVector(null, thisQ.frames[j], circle);
//					controlPoints[3] = Rn.matrixTimesVector(null, 
//							Rn.times(null, thatQ.frames[oj], flipper), 
//							circle);
//					for (int k = 0; k <controlPoints[0].length; ++k)	{
//						Rn.add(controlPoints[1][k], controlPoints[0][k], tangent1);
//						Rn.add(controlPoints[2][k], controlPoints[3][k], tangent2);
//					}
//					bpm = new BezierPatchMesh(2, 3, controlPoints);
//				} else {
				BezierPatchMeshTubeFactory bpmtf = curveFactories.get(curve);
//				if (bpmtf == null) {
					bpmtf = new BezierPatchMeshTubeFactory(curvePoints);
					curveFactories.put(curve, bpmtf);
//				}
				bpmtf.setMetric(metric);
				bpmtf.setCoreCurveIsCubicBezier(coreBezier);
				bpmtf.setExtendAtEnds(coreBezier);
				bpmtf.setRadii(curveRadii);
				BezierCurve bc = new BezierCurve(crossSection.getDegree(), squashedCrossSection);
				bpmtf.setCrossSection(bc);
				bpmtf.setArcLengthTextureCoordinates(true);
				//bpmtf.setUserBinormals(curveBinormals);
				bpmtf.setFrameFieldType(FrameFieldType.PARALLEL);
				bpmtf.updateFrames();
				FrameInfo[] frames = bpmtf.getFrameField();
				int m = frames.length;
				double[] angles = new double[m];
				for (int k = 0; k<=n; ++k)	{
					FrameInfo fi = frames[4*k]; 
					Matrix matrix = new Matrix(fi.frame);
					double[] binormal = Rn.linearCombination(null, 
							Math.cos(fi.phi), matrix.getColumn(1), -Math.sin(fi.phi),matrix.getColumn(0));
					double phi = Pn.angleBetween(binormal, curveBinormals[k], metric);
					double sign = Rn.innerProduct(matrix.getColumn(2), 
							Rn.crossProduct(null, binormal, curveBinormals[k]),	3);
					if (sign >0) phi = -phi;
					//phi = phi + ( (k%2)==1 ? Math.PI : 0);
					if (k>0) {
						while (phi + Math.PI < angles[4*k-4]) {  phi += 2*Math.PI;}
						while (phi - Math.PI > angles[4*k-4]) {  phi -= 2*Math.PI;}
					}
					angles[4*k] = phi; //(phi - fi.phi)% Math.PI;
					//System.err.println("Angles are: "+angles[4*k]*180.0/3.14159+"."+fi.phi*180.0/Math.PI);
				}
				for (int k = 0; k<m; ++k)	{
					FrameInfo fi = frames[k];
					double rest = ((k%4)/4.0);
					int kk = (k/4);
					kk = kk*4;
					if (rest != 0)	{
						angles[k] = (1-rest)*angles[kk] + rest*angles[kk+4];
					}
					fi.phi += angles[k];
					//System.err.println("Angle is: "+fi.phi*180.0/3.14159);
				}
				//frames[m-1].phi += angles[m-1];
//				bpmtf.setFrameFieldType(FrameFieldType.PARALLEL);
				System.err.println(frames.length+" frames");
				bpmtf.update();
				bpm = bpmtf.getTube();	
				if (debug) {
					debugSGC.addChild(	bpmtf.getFramesSceneGraphRepresentation());
				}

				for (int k = 1; k<= refineLevel; ++k)	bpm.refine();
				IndexedFaceSet qmpatch = BezierPatchMesh.representBezierPatchMeshAsQuadMesh(bpm);
				double[] tm = P3.makeScaleMatrix(null, 1.0, curve.size(), 1);
				GeometryUtilityOverflow.transformTextureCoordinates(qmpatch, tm);
				SceneGraphComponent sgc = new SceneGraphComponent("curve"+curveCount);
				if (appearances != null) {
					sgc.setAppearance(appearances[ curveCount % appearances.length]);
				}
				sgc.setGeometry(qmpatch);
				curveCount++;
				realSGC.addChild(sgc);
			}
		}
				
		private class Quadrilateral {
			int fIndex;
			Edge[] edges = new Edge[4];
			Quadrilateral[] neighbors = new Quadrilateral[4];
			double[][] vertices = new double[4][];
			double[][] midpoints = new double[4][];
			double[] plane;
			double[] center;
			double unitLength;
			double[][] dirs = new double[4][];
			double[][] frames = new double[4][];
			double[][] hcircle, vcircle;
			int orientation = -1;
			
			@Override
			public String toString()	{
				StringBuffer sb = new StringBuffer();
				sb.append("Neighbors: ");
				for (int i = 0; i<4; ++i)
					sb.append(neighbors[i].fIndex+":");
				sb.append("center: ");
				sb.append(Rn.toString(center));
				sb.append("\nplane: ");
				sb.append(Rn.toString(plane));
				sb.append("\ndir0: ");
				sb.append(Rn.toString(dirs[0]));
				sb.append("\ndir1: ");
				sb.append(Rn.toString(dirs[1]));
				sb.append("\nframe[0]: \n");
				sb.append(Rn.matrixToString(frames[0]));
				sb.append("\nframe[1]: \n");
				sb.append(Rn.matrixToString(frames[1]));
				sb.append("\nunitlength: ");
				sb.append(unitLength);
				sb.append("\norientation: ");
				sb.append(orientation);
				return sb.toString();
			}
		}
		
		private class Edge {
// v[0] = -1, v[1] = -1, f[1] = -1, f1 = -1, e[0] = -1, e[1] = -1;
			int[] v = {-1,-1}, f = {-1,-1}, e = {-1,-1};
			int which = 0;
			boolean sameType, boundary = false;
			double[][] coreCurve = new double[4][], binormals = new double[4][];
			double[] radii = new double[4];
			Edge(int i, int j, int k, int m)	{
				v[0] = i>j ? j : i;
				v[1] = i>j ? i : j;
				//v[0] = i;
				//v[1] = j;
				f[0] = k;
				e[0] = m;
			}
			
			@Override
			public boolean equals(Object e)	{
				if ( ! (e instanceof Edge)) return false;
				Edge e0 = (Edge) e;
				return (v[0] == e0.v[0] && v[1] == e0.v[1]);
			}

			@Override
			public int hashCode() {
				return (v[0]<<16)^v[1];
			}	
		}
		
		public BezierCurve getCrossSection() {
			return crossSection;
		}

		public void setCrossSection(double[][] crossSection) {
			this.crossSection = new BezierCurve(2, crossSection);
		}

		public void setCrossSection(BezierCurve bc) {
			this.crossSection = (bc == null) ?  new BezierCurve(2, circle) : bc;
		}

		public double getRadiusScale() {
			return radiusScale;
		}

		public void setRadiusScale(double radiusScale) {
			this.radiusScale = radiusScale;
		}

		public double getSplineTension() {
			return splineTension;
		}

		public void setSplineTension(double splineTension) {
			this.splineTension = splineTension;
		}

		public double getSquashFactor() {
			return squashFactor;
		}

		public void setSquashFactor(double squashFactor) {
			this.squashFactor = squashFactor;
		}

		public double getBoundaryBoost() {
			return boundaryBoost;
		}

		public void setBoundaryBoost(double boundaryBoost) {
			this.boundaryBoost = boundaryBoost;
		}

		public int getRefineLevel() {
			return refineLevel;
		}

		public void setRefineLevel(int refineLevel) {
			this.refineLevel = refineLevel;
		}

		public int getMetric() {
			return metric;
		}

		public void setMetric(int metric) {
			this.metric = metric;
		}

		public boolean isDebug() {
			return debug;
		}

		public void setDebug(boolean debug) {
			this.debug = debug;
			debugSGC.setVisible(debug);
		}

		public boolean isShowTubes() {
			return showTubes;
		}

		public void setShowTubes(boolean showTubes) {
			this.showTubes = showTubes;
			realSGC.setVisible(showTubes);
		}

		public Appearance[] getAppearances() {
			return appearances;
		}

		public void setAppearances(Appearance[] appearances) {
			this.appearances = appearances;
			int n = realSGC.getChildComponentCount();
			for (int i = 0; i<n; ++i)
				realSGC.getChildComponent(i).setAppearance(appearances[ i % appearances.length]);
		}
		
		public Component getInspector()	{
			if (inspectionPanel != null) return inspectionPanel;
			inspectionPanel = Box.createVerticalBox();
			refineLevelSlider = new TextSlider.Integer("refine level",SwingConstants.HORIZONTAL,1, 10, refineLevel);
			refineLevelSlider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					refineLevel = refineLevelSlider.getValue().intValue();
					setRefineLevel(refineLevel);
					update();
				}
			});
			inspectionPanel.add(refineLevelSlider);
			radiusScaleSlider = new TextSlider.Double("radius scale",SwingConstants.HORIZONTAL,0.0,1.0,radiusScale);
			radiusScaleSlider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					radiusScale = radiusScaleSlider.getValue().doubleValue();
					System.err.println("radius scale: "+radiusScale);
					setRadiusScale(radiusScale);
					update();
				}
			});
			inspectionPanel.add(radiusScaleSlider);
			splineTensionSlider = new TextSlider.Double("spline tension",SwingConstants.HORIZONTAL,0.0,2.0,splineTension);
			splineTensionSlider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					splineTension = splineTensionSlider.getValue().doubleValue();
					System.err.println("splineTension "+splineTension);
					setSplineTension(splineTension);
					update();
				}
			});
			inspectionPanel.add(splineTensionSlider);
			squashFactorSlider = new TextSlider.Double("squash factor",SwingConstants.HORIZONTAL,0.0,2.0,squashFactor);
			squashFactorSlider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					squashFactor = squashFactorSlider.getValue().doubleValue();
					System.err.println("squashFactor: "+squashFactor);
					setSquashFactor(squashFactor);
					update();
				}
			});
			inspectionPanel.add(squashFactorSlider);
			boundaryBoostSlider = new TextSlider.Double("boundary boost",SwingConstants.HORIZONTAL,0.0,5.0,boundaryBoost);
			boundaryBoostSlider.addActionListener(new ActionListener()	{
				public void actionPerformed(ActionEvent e)	{
					boundaryBoost = boundaryBoostSlider.getValue().doubleValue();
					System.err.println("boundaryBoost: "+boundaryBoost);
					setBoundaryBoost(boundaryBoost);
					update();
				}
			});
			inspectionPanel.add(boundaryBoostSlider);
			return inspectionPanel;
		}
}

