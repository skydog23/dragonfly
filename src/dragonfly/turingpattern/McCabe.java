package dragonfly.turingpattern;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.SwingConstants;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;

import charlesgunn.anim.core.KeyFrame;
import charlesgunn.anim.core.TimeDescriptor;
import charlesgunn.anim.util.AnimationUtility;
import charlesgunn.util.Colormap;
import charlesgunn.util.TextSlider;
import de.jreality.geometry.BoundingBoxUtility;
import de.jreality.math.P2;
import de.jreality.math.P3;
import de.jreality.math.Pn;
import de.jreality.math.Rn;
import de.jreality.scene.IndexedFaceSet;
import de.jreality.scene.data.Attribute;
import de.jreality.shader.ImageData;
import de.jreality.util.Input;
import de.jreality.util.Rectangle3D;
import de.jtem.discretegroup.core.DiscreteGroupUtility;
import de.jtem.discretegroup.groups.CrystallographicGroup;
import de.jtem.discretegroup.groups.WallpaperGroup;
import discreteGroup.wallpaper.SinusoidColorProvider;

/**
 * TODO
 * 	in step() add a move() method, which redistributes the pattern  to combat the boredom element
 * of random fluctuations.  For example, move in direction of gradient (or perpendicular to).
 * In this regard, research existence and classification of vector fields on these 17 euclidean orbifolds.
 *  refactor SinusoidalColorProvider to insert  superclass with arbitrary number of channels that returns 
 * an array of double values
 *  implement save/restore so that values are stored off with animation file (perhaps this should be done
 * via the automatic reflection properties in the xstream package).
 *  write interactive tool to paint into the texture pattern.
 * @author gunn
 *
 */

public class McCabe {
	static enum FilterType {Flat, Round, Tent, Asymmetric};

	int height =  128;
	int width =  height;
	int current = 0; 
	int count = 0;
	int stepsPerTick = 1;
	boolean loadImage = true;
	
	int[] sizes =  {1,4,13, 40, 121, 364, 1093};//, 364}; //,121};//,15};{1, 3, 10}; //
	double[] weights = new double[sizes.length];//, 1.0};//{2,-1,1.2,1}; //{.333, .333, .333}; //{1,1,1}; //
	boolean[] active = new boolean[weights.length],
			animate = new boolean[weights.length];
	
	int numLevels = sizes.length+1;
	int numWeights = 3; //numLevels-1;
	{
		for (int i = 0; i<sizes.length;  ++i) {
			weights[i] = 1.0;
			active[i] = i < numWeights;
			animate[i] = i < numWeights;
		}
	}
	double delta = 0.06;
	double functionBlend = 0;
	FilterType currentFilter = FilterType.Flat;
	
	double[][][] filters = new double[numLevels][][];
	double[][][] valueArray;
	double s = (1.0/(4*.41+5));
	double[][] specialFilter = {{s*.41, s, s*.41}, {s,s,s},{s*.41,s,s*.41}};
	
	byte[] faceColors;
	Colormap colormap;
	TimeDescriptor td = new TimeDescriptor(.5);
	double[][][] filtered = new double[numLevels][width][height];
	PeriodicSource weightsInterpolator = new PeriodicSource();
	double base = 1.0, amplitude = .5;
	int curGroup = 0;
	WallpaperGroup group; 
	CrystallographicGroup cgroup;
	IndexedFaceSet fundDom;
	double[][] fundDomP; 
	HashMap<Long, Long> indices = new HashMap<Long, Long>();

	
	public McCabe()	{
		if (loadImage)	{
			loadImage();
		}
		colormap = new Colormap();
		colormap.addKeyFrame(0.0, Color.black);
		colormap.setCurrentValue(Color.red);
		colormap.addKeyFrame(td);
		colormap.addKeyFrame(1.0, Color.white);
//		scp.setSaturated(.5);
		weightsInterpolator.setSpeed(.005);
		weightsInterpolator.setC1(1.412); //Math.sqrt(Math.sqrt(5.0)/2 + .5));
		weightsInterpolator.setC2(1.0);
		weightsInterpolator.setC3(1.0/weightsInterpolator.getC1());
		
		if (numWeights > weights.length)
			throw new IllegalStateException("bad weight array: too short");
		if (powersOf3)	{
			for (int i = 0; i<sizes.length; ++i) {
				sizes[i] = (int) ((Math.pow(3, (i+1))-1)/2);
			}
			currentFilter = FilterType.Flat;
		}
		initFilters();
		setSize(width);
		

	}

	private void loadImage() {
		if (map == null)
		try {
			map = ImageData.load(Input.getInput(this.getClass().getResource("cgg3.png")));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void scaleImage()	{
		BufferedImage im = (BufferedImage) map.getImage();
		theImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g2d =  theImage.createGraphics();
		g2d.scale(((double) width)/im.getWidth(), ((double) height)/im.getHeight());
		g2d.drawImage(im, 0, 0, null);
	}
	
	protected void setSize(int size) {
		width = height = size;
		faceColors = new byte[width*height*4];
		valueArray = new double[2][height][width];
		filtered = new double[numLevels][width][height];
		if (loadImage) scaleImage();
		 _resetBoard();
		updateColors();
	}
	
	public int getSize() {
		return width;
	}
	private void initFilters() {
		for (int i = 0; i<sizes.length; ++i) 
			filters[i] = constructFilter(currentFilter, 2*sizes[i]+1);
	}
	
	private double[][] constructFilter(FilterType type, int size) {
		double[][] filter = new double[size][size];
		double sum = 0.0;
		for (int i = 0; i< size; ++i)	{
			double y = -1 + 2*i/(size-1.0);
			for (int j = 0; j < size; ++j)	{
				double x =  -1 + 2* j/(size-1.0);
				double z;
				switch (type) {
				case Flat:
					filter[i][j] = 1.0;
					break;
				case Round:
					z = x*x + y*y;
					if (z > 2.0) filter[i][j] = 0.0;
					else 
						filter[i][j] = 2.0 - z; //Math.sqrt(1-z);
					break;
				case Tent:
				default:
					z = Math.abs(x) + Math.abs(y); //x*x + y*y;
//					if (z > 2.0) filter[i][j] = 0.0;
//					else 
						filter[i][j] = 2.5 - z; //Math.sqrt(1-z);
					//filter[i][j] += .5;
					break;
				case Asymmetric:
					z = 2 + x + y;
					if (z > 4.0) filter[i][j] = 4.0;
					else 
						filter[i][j] = z; //Math.sqrt(1-z);
					break;
				}
				sum += filter[i][j];
			}
		}
		for (int i = 0; i< size; ++i)	{
			for (int j = 0; j < size; ++j)	{
				filter[i][j] /= sum;
			}
		}
		return filter;
	}
	boolean resetDirty = true,
		filterDirty = true;
	
	public void resetBoard() {
		resetDirty = true;
	}
	
	ImageData map = null;
	int[] foop = new int[4];
	void _resetBoard() {
		setupVectorField();
		indices.clear();
		current = count = 0;
		filtered = new double[numLevels][width][height];
		resetDirty = false;
		if (loadImage) { 

			for (int i = 0; i<height; ++i)	{
				for (int j = 0; j<width; ++j)	{
					int[] pixel = theImage.getRaster().getPixel(i, j, foop);
					valueArray[current][i][j] = (double) (pixel[1]/255.0);
//					System.err.println("(i,j)"+i+":"+j+" = "+valueArray[current][i][j]);
				}
			}
		}
		else  {
		minval = 10; maxval = -1.0;
		for (int i = 0; i<height; ++i)	{
			double y = i/(height-1.0);
			for (int j = 0; j<width; ++j)	{
				double x = j/(width -1.0);
				double d = (1-functionBlend)*Math.random(); // + functionBlend*function(x,y);
//				double xr = (10*x)%1.0,
//						yr = (10*y)%1.0;
//				d = ((xr > .5 && yr > .5) || (xr < .5 && yr < .5)) ? 0.0 : 1.0;
////				double d = Math.abs(vectorField[i][j][0]);
				valueArray[current][i][j] = d;
				if (d > maxval) maxval = d;
				if (d < minval) minval = d;
			}
		}
		}
		System.err.println("minval = "+minval+" maxval = "+maxval);
//		normalizeArray();
	}
	
	protected double function(double x, double y)	{
		int i = (int) (width * x), j = (int) (height *y);
		if (i >= width) i = width - 1;
		if (j >= height) j = height -1;
		int[] pixel = theImage.getRaster().getPixel(i, j, foop);
		return (double) (pixel[0]/255.0);
//		return Math.sin(Math.PI*2*x) + Math.cos(Math.PI*2*y);
	}
	double maxval, minval;
	boolean blur = false, waitingToBlur = false;
	boolean justOne = false, 
		powersOf3 = true,
		polishPowersOf3 = true,
		doAttenuate = true,
		animateWeights = false,
		doColormap = false;
	float colorVal = 1.0f;
	public void setDoBlur()  {
		waitingToBlur = true;
	}
	public void step()	{
		if (resetDirty) {_resetBoard();}
		
		if (filterDirty) {
			initFilters();
			filterDirty = false;
		}
		
		maxval = -1.0; 
		minval = 10.0;
		int[] ret = new int[2];
		// first create filtered images: for later optimization!
		int tNW = 0;
		for (int k = 0; k<weights.length; ++k)	{
			if (active[k]) tNW = k+1;
		}
		int step = 1;
		for (int k = 0; k <= tNW; ++k) {
			if (polishPowersOf3 && k==tNW) continue;
			for (int i = 0; i < height; ++i) {
				for (int j = 0; j < width; ++j) {
					double sum = 0;
					if (powersOf3)	{
						double[][] rawValues = (k==0) ? valueArray[current]  : filtered[k-1];
						for (int n = -1; n<=1; n += 1)	{
							for (int m = -1; m<=1; m += 1)	{
								int ir = (i+n*step);
								int jr = (j+m*step);
								indicesFor(ret, jr, ir);
								sum += filters[0][n+1][m+1] * rawValues[ret[1]][ret[0]];														
							}
						}
					} else {
						int size = sizes[k];
						for (int n = -size; n<=size; ++n)	{
							for (int m = -size; m<=size; ++m)	{
								int ir = (i+n);
								int jr = (j+m);
								indicesFor(ret, jr, ir);
								sum += filters[k][n+size][m+size] * valueArray[current][ret[1]][ret[0]]; //bigFilter[n+4][m+4] * 
							}
						}						
					}
					filtered[k][i][j] = sum;
				}
			}
			step *= 3;	
		}
		if (powersOf3 && polishPowersOf3) {
			for (int k = tNW-1; k >= 0; --k) {
//				if (!active[k]) continue;
				for (int i = 0; i < height; ++i) {
					for (int j = 0; j < width; ++j) {
						double sum = 0;
						for (int n = -1; n <= 1; n += 1) {
							for (int m = -1; m <= 1; m += 1) {
								int ir = (i + step* n);
								int jr = (j + step*m);
								indicesFor(ret, jr, ir);
								sum += specialFilter[n + 1][m + 1] * filtered[k][ret[1]][ret[0]];
							}
						}
						filtered[k+1][i][j] = sum;
					}
				}
				step = step/3;
			}
		}
		// then use the filtered images to carry out next step of process
		int maxDiff = -1;
		double maxDD = 0,
			upAttenuate = 1.0,
			downAttenuate = 0.0;
		for (int i = 0; i<height; ++i)	{
			for (int j = 0; j<width; ++j)	{
//				if (!inFundamentalRegion(i,j)) continue;
				double curval = valueArray[current][i][j], val;
				double lowest = filtered[0][i][j];
				if (blur) {
					val = lowest;
				} else {
					val = curval;
					upAttenuate = doAttenuate ? (1-val) : 1.0;
					downAttenuate = doAttenuate ? val : 1.0;
					maxDD = 10;
					maxDiff = -1;
					for (int k = 0; k<tNW; ++k)	{
						if (!active[k]) continue;
						double thisLevel = filtered[k+1][i][j];
						double diff = filtered[k][i][j] - thisLevel;
						if (justOne) {
							if (Math.abs(diff) * weights[k] <= Math.abs(maxDD)) {
								maxDD = diff * weights[k]; 
								maxDiff = k-1;
							}
						} else {
							if ( diff > 0) //lowest> thisLevel)
								 val += upAttenuate*weights[k]* delta;
							else val -=  downAttenuate*weights[k]* delta;												
						}
					}
					if (justOne && maxDiff >= 0)	{
						// the effect of the attenuation factors is as follows:
						// values that are close to 0 or 1 are decreased/increased
						// more slowly (proportional to the difference to 0/1).
						if (maxDD > 0) val += upAttenuate*weights[maxDiff]* delta;
						else  val -=  downAttenuate*weights[maxDiff]* delta;
						//val += maxDD * delta;
					}
				}
				if (val > maxval) maxval = val;
				if (val < minval) minval = val;
				valueArray[1-current][i][j] = val;
			}
		}
		current = 1 - current;
		normalizeArray();
	}

	private void updateWeights() {
		float[] rgba = new float[4];
		int animateCount = 0;
		if (animateWeights)	{
			weightsInterpolator.update();
			double[] value = weightsInterpolator.getValue();
			double sum = 0;
			for (int i = 0; animateCount<3 && i<weights.length; ++i)	{
				if (!active[i] || !animate[i]) continue;
				weights[i] = base+amplitude* (.5 - value[animateCount++])*2;
				sum += weights[i];
				weightSliders[i].setValue(weights[i]);
			}
			// normalize
			double factor = 3.0/(sum);
			for (int i = 0; animateCount<3 && i<weights.length; ++i)	{
				if (!active[i] || !animate[i]) continue;
				weights[i] = weights[i] * factor;
			}
		} else {
			for (int i = 0; i<weights.length; ++i)	{
				if (!active[i]) continue;
				rgba[animateCount++] = (float) Math.abs(weights[i]);
			}
		}
		if (doColormap) {
			float max = 0.0f, sum = 0f;
			for (int i = 0; i<animateCount; ++i) {
				if (rgba[i] > max) max = rgba[i];
				sum += rgba[i];
			} 
			float factor = 1.0f;
			if (sum > colorVal) {
				factor = (colorVal)/sum;
			}
			if (factor < 0.0) factor *= -1;
			factor = (float) ((factor) % 1.0);
//			else if (max < 1.0) {
//				factor = 1f/max;
//				if (factor*sum > colorVal) factor = colorVal/sum;
//			}
			for (int i = 0; i<3; ++i) rgba[i] *= factor;
			foo = new Color(rgba[0], rgba[1], rgba[2]);
			colormap.deleteKeyFrame(td);
			
			colormap.addKeyFrame(new KeyFrame<Color>(td, foo));
		}
	}
	public PeriodicSource getWeightsInterpolator() {
		return weightsInterpolator;
	}

	// the normalized coordinates (x,y) correspond to the middle of the pixel with LL corner = (0.0, 0.0)
	private double[] convertCanvas2Group(int x, int y)	{
		double[] point = P3.originP3.clone();
		point[0] = (x+.5)/(width);
		point[1] = (y+.5)/(height);
		return point;
	}
	// the inverse function of the previous one
	private int[] convertGroup2Canvas(int[] ret, double[] canonPoint) {
		if (ret == null) ret = new int[2];
		// use .49 instead of .5: the values in canonPoint should be near
		// (.5,.5) mod 1, but if they are a bit less that .5, then subtracting
		// .5 pops the value into the neighboring (incorrect pixel). 
		// Possibly consider not subtracting anything!
		// In any case using .49 instead of .5 fixed problems I had observed
		// with the group 22* -- particularly the neighborhood of the rotation center
		// (.5, 1)
		ret[0] = (int) (canonPoint[0] * (width) - .49);
		ret[1] = (int) (canonPoint[1] * (height) - .49);
		return ret;
	}

	private int[] indicesFor(int[] ret, int horiz, int vert)  {
		if (ret == null) ret = new int[2];
		ret[0] = horiz; ret[1] = vert;
		long code = horiz + 16384*vert;
		Long result = indices.get(code);
		// use a hash table to optimize this calculation
		if (result != null) {
			ret[0] = (int) (result.longValue() & 16383);
			ret[1] = (int) (result.longValue() >> 14);
			return ret;
		} 
		double[] cpoint = convertCanvas2Group(horiz, vert);
		double[] canonPoint = DiscreteGroupUtility.getCanonicalRepresentative2(null, cpoint, null, fundDom, group);
		convertGroup2Canvas(ret, canonPoint);
//				System.err.println(horiz+":"+vert+"-->"+ret[0]+":"+ret[1]);
		indices.put(code, new Long(ret[0] + 16384 * ret[1]));
		return ret;
	}

	public void normalizeArray()	{
		int[] inds = new int[2];
		for (int i = 0; i<height; ++i)	{
			for (int j = 0; j<width; ++j)	{
					inds = indicesFor(inds, j, i);
					valueArray[current][i][j] =  AnimationUtility.linearInterpolation(valueArray[current][inds[1]][inds[0]],
					        minval, maxval, 0.0, 1.0);
			}
		}
	}
	int[] channels = {1,0,3,2};
	Color foo;
	float[] bcf = new float[4];
	public void updateColors()	{
		for (int i = 0; i<height; ++i)	{
			for (int j = 0; j<width; ++j)	{
				if (doColormap)	{
					foo = colormap.getValueAtTime(valueArray[current][i][j]);
					foo.getRGBComponents(bcf);
					for (int k=0; k<4; ++k) faceColors[4*(i*width +j)+channels[k]] = (byte) (255 * bcf[k]);
				} else {
					byte[] cb = new byte[3];
					for (int k = 0; k<3; ++k)	{
						byte val = (byte) (255.0 * valueArray[current][i][j]);
						cb[k] = faceColors[4*(i*width +j)+channels[k]] = val;
					}
//					System.err.println(i+":"+j+"val = "+valueArray[current][i][j]);//" color = "+cb[0]+" "+cb[1]+" "+cb[2]);
					faceColors[4*(i*width+j)+channels[3]] = (byte)255;					
				}
			}
		}
	}
	public Image currentValue()	{
		return currentValue.getImage();
	}
	ImageData currentValue;
	public void update()	{
		if ((count % 100) == 0) System.err.println("count = "+(count++)+"badcount = "+DiscreteGroupUtility.badcount);
		count++;
		if (waitingToBlur) {
			blur = true;
			waitingToBlur = false;
		}
		updateWeights();
		for (int i=0; i<stepsPerTick; ++i)
			step();
//		moveByVF();
		updateColors();
		currentValue = new ImageData(faceColors, width, height);
		blur = false;
	}
	
	TextSlider weightSliders[] = new TextSlider[weights.length];
	
	public Component getInspector()	{
		Box container = Box.createVerticalBox();
		Box hbox = Box.createHorizontalBox();
		container.add(hbox);
		Box vbox = Box.createVerticalBox();
		hbox.add(vbox);
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "filter type")));
		JComboBox filtertypes = new JComboBox(FilterType.values());
		filtertypes.addActionListener(new ActionListener() {

			public void actionPerformed(ActionEvent e) {
				System.err.println("selected "+((JComboBox)e.getSource()).getSelectedItem());
				currentFilter = (FilterType) ((JComboBox)e.getSource()).getSelectedItem();
				filterDirty = true;
			}
			
		});
//		filtertypes.setPreferredSize(new Dimension(60,20));
//		filtertypes.setSelectedIndex(2);
		vbox.add(filtertypes);
		
		vbox = Box.createVerticalBox();
		hbox.add(vbox);
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "toggles")));
		
		
		JCheckBox directionButton = new JCheckBox("just one ", justOne);
		directionButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				justOne = ((JCheckBox) e.getSource()).isSelected();
			}
		});
		vbox.add(directionButton);
		
		JCheckBox polishButton = new JCheckBox("polish ", polishPowersOf3);
		polishButton.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				polishPowersOf3 = ((JCheckBox) e.getSource()).isSelected();
			}
		});
		vbox.add(polishButton);

		JCheckBox tubesCB = new JCheckBox("attenuate", doAttenuate);
		tubesCB.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				doAttenuate = ((JCheckBox) e.getSource()).isSelected();
			}
		});
		vbox.add(tubesCB);

		JCheckBox animateWeightsCB = new JCheckBox("animate weights", animateWeights);
		animateWeightsCB.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				animateWeights = ((JCheckBox) e.getSource()).isSelected();
			}
		});
		vbox.add(animateWeightsCB);

		JCheckBox showCMCB = new JCheckBox("show colormap", doColormap);
		showCMCB.addActionListener( new ActionListener()	{
			public void actionPerformed(ActionEvent e)	{
				doColormap = ((JCheckBox) e.getSource()).isSelected();
			}
		});
		vbox.add(showCMCB);

		TextSlider sptSlider = new TextSlider.Integer("steps per tick",  SwingConstants.HORIZONTAL,0,15, stepsPerTick);
		sptSlider.addActionListener(new ActionListener()  {
		  public void actionPerformed(ActionEvent e)  {
		    stepsPerTick = ((TextSlider) e.getSource()).getValue().intValue();
		  }
		});
		container.add(sptSlider);			

//		TextSlider awSlider = new TextSlider.Integer("active weights",  SwingConstants.HORIZONTAL,1,numWeights, numWeights);
//		awSlider.addActionListener(new ActionListener()  {
//		  public void actionPerformed(ActionEvent e)  {
//		    numWeights = ((TextSlider) e.getSource()).getValue().intValue();
//		    for (int i = 0; i<weightSliders.length; ++i)	{
//		    		weightSliders[i].setVisible(i < numWeights);
//		    }
//		  }
//		});
//		container.add(awSlider);			

		TextSlider rSlider = new TextSlider.Integer("size",  SwingConstants.HORIZONTAL,1,200,width);
		rSlider.addActionListener(new ActionListener()  {
		  public void actionPerformed(ActionEvent e)  {
		    width = ((TextSlider) e.getSource()).getValue().intValue();
		    setSize(width);
		  }
		});
		container.add(rSlider);			

		TextSlider dSlider = new TextSlider.DoubleLog("step",  SwingConstants.HORIZONTAL,0.001,0.5,delta);
		dSlider.addActionListener(new ActionListener()  {
		  public void actionPerformed(ActionEvent e)  {
		    delta = ((TextSlider) e.getSource()).getValue().doubleValue();
		  }
		});
		container.add(dSlider);			

		TextSlider mixSlider = new TextSlider.Double("mix",  SwingConstants.HORIZONTAL,0,1,functionBlend);
		mixSlider.addActionListener(new ActionListener()  {
		  public void actionPerformed(ActionEvent e)  {
		    functionBlend = ((TextSlider) e.getSource()).getValue().doubleValue();
		    resetBoard();
		  }
		});
		container.add(mixSlider);			

			vbox = Box.createVerticalBox();
		container.add(vbox);
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "weights")));
		
		for (int i = 0; i<weights.length; ++i)	{
			final int j = i;
			hbox = Box.createHorizontalBox();
			final JCheckBox cb = new JCheckBox("");
			cb.setSelected(active[i]);
			hbox.add(cb);
			final JCheckBox cb2 = new JCheckBox("");
			cb2.setSelected(animate[i]);
			hbox.add(cb2);
			weightSliders[i] = new TextSlider.Double("weight"+i,  SwingConstants.HORIZONTAL,-1,2,1);
			weightSliders[i].addActionListener(new ActionListener()  {
			  public void actionPerformed(ActionEvent e)  {
			    weights[j] = ((TextSlider) e.getSource()).getValue().doubleValue();
			  }
			});
			hbox.add(weightSliders[i]);	
			cb.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					active[j] = cb.isSelected();
					weightSliders[j].setEnabled(active[j]);
				}
			});
			cb2.addActionListener(new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent arg0) {
					animate[j] = cb2.isSelected();
				}
			});
			vbox.add(hbox);
		}

		vbox = Box.createVerticalBox();
		container.add(vbox);
		vbox.setBorder(new CompoundBorder(new EmptyBorder(5, 5, 5, 5),
				BorderFactory.createTitledBorder(BorderFactory
						.createEtchedBorder(), "animated weights parameters")));
		
		TextSlider baseSlider = new TextSlider.Double("base",  SwingConstants.HORIZONTAL,0,2,base);
		baseSlider.addActionListener(new ActionListener()  {
		  public void actionPerformed(ActionEvent e)  {
		    base = ((TextSlider) e.getSource()).getValue().doubleValue();
		  }
		});
		vbox.add(baseSlider);			

		TextSlider varSlider = new TextSlider.Double("amplitude",  SwingConstants.HORIZONTAL,0.0,3.0,amplitude);
		varSlider.addActionListener(new ActionListener()  {
		  public void actionPerformed(ActionEvent e)  {
		    amplitude = ((TextSlider) e.getSource()).getValue().doubleValue();
		  }
		});
		vbox.add(varSlider);			

		vbox.add(weightsInterpolator.getInspector());
		
		return container;
	}
	
	double[][][] vectorField;
	public void moveByVF()	{
		final double[][] move = valueArray[1-current],
			moved = valueArray[current]; //new double[height][width];
		for (int i = 0; i<height; ++i)	{
			for (int j = 0; j<width; ++j)	{
				move[i][j] = 0.0;
			}
		}
		final int[] here = new int[2], horiz = new int[2], vert = new int[2], diag = new int[2];
		for (int i = 0; i<height; ++i)	{
			for (int j = 0; j<width; ++j)	{
				double[] vf = vectorField[i][j];
				int hdir = vf[0] < 0 ? -1 : 1,
					vdir = vf[1] < 0 ? -1 : 1;
				indicesFor(here, j, i);
				indicesFor(horiz,j+hdir, i);
				indicesFor(vert, j, i+vdir);
				indicesFor(diag, j+hdir, i+vdir);
				double x = Math.abs(vf[0]);
				if (x > 1) x= 1;
				double y = Math.abs(vf[1]);
				if (y>1) y = 1;
				double val = moved[here[1]][here[0]];
				move[here[1]][here[0]] += val*(1-x)*(1-y);
				val = moved[horiz[1]][horiz[0]];
				move[horiz[1]][horiz[0]] += val*(x)*(1-y);
				val = moved[vert[1]][vert[0]];
				move[vert[1]][vert[0]] += val*(1-x)*y;
				val = moved[diag[1]][diag[0]];
				move[diag[1]][diag[0]] += val*x*y;
			}
		}
		current = 1-current;
		normalizeArray();
	}
	BarycentricVF244 vf244 = new BarycentricVF244();
	private void setupVectorField() {
		vf244.setRotation(Math.PI/4);
		vf244.setScale(.01);
		vectorField = new double[width][height][2];
		int[] ret = new int[2];
		double[] p41 = {0,0,0,1}, 
			p42 = {1,1,0,1},
			p21 = {1,0,0,1},
			p22 = {0,1,0,1};
		double[][] tc = {p41, p21, p42}, dc = new double[3][4];
		double[] bary = new double[3], P = {0,0,0,1}, vf = new double[4];
		for (int i = 0; i<height; ++i)	{
			double y = i/(height-1.0);
			P[1] = y;
			for (int j = 0; j<width; ++j)	{
				double x = j/(width - 1.0);
				tc[1] = (x>=y) ? p21 : p22;
				//indicesFor(ret, i, j);
				P[0] = x;
				vf244.valueAt(vf, P);
//				for (int k=0; k<3; ++k) {
//					Rn.subtract(dc[k], P, tc[k]);
//				}
//				Hit.convertToBary(bary, tc[0], tc[1], tc[2], P);
//				bary[1] /= 2;
//				bary[2] /= 3;
////				System.err.println("bary = "+Rn.toString(bary));
//				Rn.barycentricTriangleInterp(vf, dc, bary);
//				System.err.println(i+":"+j+" vf = "+Rn.toString(vf));
				vectorField[i][j][0] = vf[0];
				vectorField[i][j][1] = vf[1];
			}
		}
	}

	static double[] jitterM;
	private BufferedImage theImage;
	{ jitterM = Rn.identityMatrix(4); } //P3.makeTranslationMatrix(null, new double[]{.001, .0023, 0}, Pn.EUCLIDEAN); }
	
	public void setGroup(WallpaperGroup group) {
		this.group = group;
		cgroup = CrystallographicGroup.convert2DTo3D(group);
		fundDom = (IndexedFaceSet) group.getDefaultFundamentalRegion();
		Rectangle3D bb = BoundingBoxUtility.calculateBoundingBox(fundDom);
		System.err.println("BB of FD = \n"+bb.toString());
		fundDomP = fundDom.getVertexAttributes(Attribute.COORDINATES).toDoubleArrayArray(null);
		Rn.matrixTimesVector(fundDomP, jitterM, fundDomP);
	}

}
