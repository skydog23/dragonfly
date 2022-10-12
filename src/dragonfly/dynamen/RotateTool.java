/**
 *
 * This file is part of jReality. jReality is open source software, made
 * available under a BSD license:
 *
 * Copyright (c) 2003-2006, jReality Group: Charles Gunn, Tim Hoffmann, Markus
 * Schmies, Steffen Weissmann.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * - Neither the name of jReality nor the names of its contributors nor the
 *   names of their associated organizations may be used to endorse or promote
 *   products derived from this software without specific prior written
 *   permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 */

package dragonfly.dynamen;

import de.jreality.math.FactoredMatrix;
import de.jreality.math.Matrix;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.scene.SceneGraphComponent;
import de.jreality.scene.SceneGraphPath;
import de.jreality.scene.Transformation;
import de.jreality.scene.Viewer;
import de.jreality.scene.tool.AbstractTool;
import de.jreality.scene.tool.InputSlot;
import de.jreality.scene.tool.ToolContext;
import de.jreality.tools.AnimatorTask;
import de.jreality.tools.AnimatorTool;

public class RotateTool extends AbstractTool {

	static InputSlot activationSlot = InputSlot.getDevice("RotateActivation");
	static InputSlot evolutionSlot = InputSlot.getDevice("TrackballTransformation");

	public RotateTool() {
		super(activationSlot);
		addCurrentSlot(evolutionSlot);
	}

	transient protected SceneGraphComponent selectedComponent;
	protected boolean tracking = false;
	
	@Override
	public void activate(ToolContext tc) {
		super.activate(tc);
		selectedComponent = tc.getRootToToolComponent().getLastComponent();
		AnimatorTool.getInstance(tc).deschedule(selectedComponent);
		if (selectedComponent.getTransformation() == null)
			selectedComponent.setTransformation(new Transformation());
		System.err.println("Descheduling "+selectedComponent.getName());
		tracking = false;
		oldtime = tc.getTime();
	}

	transient protected Matrix result = new Matrix();
	transient protected Matrix evolution = new Matrix();
	transient long oldtime;
	transient double dt;

	@Override
	public void perform(ToolContext tc) {
		SceneGraphPath toComp = tc.getRootToToolComponent();
		Matrix root2toComp = new Matrix(toComp.getInverseMatrix(null));
		root2toComp.assignFrom(P3.extractOrientationMatrix(null, root2toComp
				.getArray(), P3.originP3, Pn.EUCLIDEAN));
		evolution.assignFrom(tc.getTransformationMatrix(evolutionSlot));
		evolution.conjugateBy(root2toComp);
		result.assignFrom(selectedComponent.getTransformation());
		result.multiplyOnRight(evolution);
		selectedComponent.getTransformation().setMatrix(result.getArray());
		tracking = true;
		dt = .001*(tc.getTime() - oldtime);
		oldtime = tc.getTime();
		tc.getViewer().renderAsync();
	}

  	@Override
	public void deactivate(ToolContext tc) {
  		if (!tracking) return;
    	final Viewer vv = tc.getViewer();
    	AnimatorTask task = new AnimatorTask() {
	        FactoredMatrix e = new FactoredMatrix(evolution, Pn.EUCLIDEAN);
	        double rotAngle = e.getRotationAngle();
	        double[] axis = e.getRotationAxis();
	        {
	        if (rotAngle > Math.PI) rotAngle = -2*Math.PI+rotAngle;
	        }
	        public boolean run(double time, double dt) {
	          	MatrixBuilder m = MatrixBuilder.euclidean(selectedComponent.getTransformation());
	          	m.rotate(0.05*dt*rotAngle, axis);
	          	m.assignTo(selectedComponent);
	          	vv.renderAsync();
	          	return true;
	        }
      };
      AnimatorTool.getInstance(tc).schedule(selectedComponent, task);
  }
  

}
