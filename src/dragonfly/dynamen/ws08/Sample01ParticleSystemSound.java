package dragonfly.dynamen.ws08;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import charlesgunn.util.MyMidiSynth;
import charlesgunn.util.TextSlider;
import de.jreality.math.MatrixBuilder;
import de.jreality.math.Rn;
import de.jreality.scene.SceneGraphComponent;

/**
 * A subclass of {@link Sample01ParticleSystem} which supports simple MIDI sound 
 * generation for the <i>ball hits wall</i> event.
 * 
 * @author Charles Gunn
 *
 */
public class Sample01ParticleSystemSound extends Sample01ParticleSystem {

	MyMidiSynth midi = new MyMidiSynth();
	int[] indices = { 12, 13, 15, 106, 107, 108, 114,115,116};
	String[] names = {"marimba","xylophone","dulcimer",
			"shamisen","koto", "kalimba",
			"steel drum", "woodblock", "taiko drum"
	};
	int currentInstrument = indices[0], currentChannel = 0;
	double pitch = 4;
	boolean doSound = true;
	
	@Override
	protected SceneGraphComponent makeWorld()	{
		super.makeWorld();
		setDoSound(doSound);
		return world;
	}
	static boolean canDoMidi = true;
	private void setDoSound(boolean doSound) {
		if (!canDoMidi) return;
		if (doSound)	{
			canDoMidi = midi.open();
			if (!canDoMidi) return;
			for (int j = 0; j<indices.length; ++j)	{
				midi.getSynthesizer().loadInstrument(midi.getInstruments()[indices[j]]);
				midi.getChannels()[j].channel.programChange(indices[j]);			
			}
			} else midi.close();
	}
	
	@Override
	protected Component getInspector() {
		Box container = (Box) super.getInspector();
		if (!canDoMidi) return container;
		Box vbox = Box.createVerticalBox();
		vbox.setBorder(BorderFactory.createTitledBorder(BorderFactory
				.createEtchedBorder(), "Sound"));
		final JCheckBox doSoundBox = new JCheckBox("Active", doSound );
		doSoundBox.addActionListener( new ActionListener() {
			public void actionPerformed(ActionEvent e)	{
				doSound = doSoundBox.isSelected();
				setDoSound(doSound);
			}
		});
		Box hbox = Box.createHorizontalBox();
		hbox.add(Box.createHorizontalGlue());
		hbox.setPreferredSize(new Dimension(300, 40));
		hbox.setMaximumSize(new Dimension(300, 40));
		vbox.add(hbox);
		hbox.add(doSoundBox);
		final JComboBox instCB = new JComboBox(names);
		instCB.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				currentChannel = instCB.getSelectedIndex();
				currentInstrument = indices[currentChannel];
				midi.getChannels()[currentChannel].channel.programChange(currentInstrument);			
			}
			
		});
		hbox.add(Box.createHorizontalGlue());
		JLabel label = new JLabel("Instrument:");
		hbox.add(label);
		hbox.add(instCB);
		hbox.add(Box.createHorizontalGlue());
		final TextSlider.DoubleLog freqSlider = new TextSlider.DoubleLog(
				"Pitch", SwingConstants.HORIZONTAL, 1, 8,
				pitch);
		freqSlider.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				pitch = freqSlider.getValue();
				
			}
		});
		vbox.add(freqSlider);
		container.add(vbox);
		return container;
	}

	@Override
	protected ChangeListener getChangeListener() {
		return new ChangeListener() {
			int count = 0;
			// we need to be updated when the state changes since the shape of the cylindrical "shooter"
			// depends on a property of the particle system
			public void stateChanged(ChangeEvent ce) {
				ParticleSystemEvent e = (ParticleSystemEvent) ce;
				switch (e.eventType)	{
				case ParticleSystemEvent.FRAME_CHANGE:
					MatrixBuilder.euclidean().scale(psf.getPositionSpread(), psf.getPositionSpread(), 2).translate(0,0,-1).assignTo(cylinderProper);	
					break;
				case ParticleSystemEvent.PARAMETER_CHANGE:
					break;
				case ParticleSystemEvent.PARTICLE_BOUNCE:
					if (canDoMidi && doSound) {
						double[] v = ((Particle) e.getSource()).getVelocity();
						double s = Rn.euclideanNorm(v);
						midi.getChannels()[currentChannel].channel.noteOn(
								(int) (24+s*16*pitch), 
								midi.getChannels()[currentChannel].getVelocity());
						count++;
					}
					break;
				}
			
			}
			
		};
	}

	public static void main(String[] args) {
		final Sample01ParticleSystemSound ps = new Sample01ParticleSystemSound();
		ps.doIt();
	}


}
