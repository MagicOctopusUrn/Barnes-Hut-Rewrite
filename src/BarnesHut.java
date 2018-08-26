
// required to paint on screen
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

//Start the applet and define a few necessary variables
public class BarnesHut extends JFrame implements ActionListener {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7706184019155504851L;
	public int N = 100;
	public Body bodies[] = new Body[10000];
	public JTextField t1;
	public JLabel l1,l2;
	public JButton b1;
	public JButton b2;
	public JSlider t;
	public boolean shouldrun = false;

	private ChartPanel chartPanel;

	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				BarnesHut bh = new BarnesHut();
				bh.setVisible(true);
				bh.setSize(600, 600);
			}
		});
	}

// The first time we call the applet, this function will start
	public BarnesHut() {
		startBodies();
		t1 = new JTextField("100", 5);
		b2 = new JButton("Restart");
		b1 = new JButton("Stop");
		l1 = new JLabel("Number of bodies:");
		b1.addActionListener(this);
		b2.addActionListener(this);
		t = new JSlider(1, 50);
		JPanel buttonPanel = new JPanel();
		buttonPanel.add(l1);
		buttonPanel.add(t1);
		buttonPanel.add(b2);
		buttonPanel.add(b1);
		buttonPanel.add(t);
		buttonPanel.add(l2 = new JLabel(""));
		this.add(buttonPanel, BorderLayout.NORTH);

		this.chartPanel = new ChartPanel(null);
		this.add(this.chartPanel, BorderLayout.CENTER);
	}

// This method gets called when the applet is terminated. It stops the code
	public void stop() {
		shouldrun = false;
	}

	// the bodies are initialized in circular orbits around the central mass.
	// This is just some physics to do that
	public static double circlev(double rx, double ry) {
		double solarmass = 1.98892e30;
		double r2 = Math.sqrt(rx * rx + ry * ry);
		double numerator = (6.67e-11) * 1e6 * solarmass;
		return Math.sqrt(numerator / r2);
	}

	public void collapseBodies() {

	}

	// Initialize N bodies
	public void startBodies() {
		double radiusU = 1e18; // radius of universe
		double solarmass = 1.98892e30;
		for (int i = 0; i < N; i++) {
			double px = 1e18 * exp(-1.8) * (0.5 - Math.random());
			double py = 1e18 * exp(-1.8) * (0.5 - Math.random());
			double magv = circlev(px, py);

			double absangle = Math.atan(Math.abs(py / px));
			double thetav = Math.PI / 2 - absangle;
			double phiv = Math.random() * Math.PI;
			double vx = -1 * Math.signum(py) * Math.cos(thetav) * magv;
			double vy = Math.signum(px) * Math.sin(thetav) * magv;
			// Orient a random 2D circular orbit

			if (Math.random() <= .5) {
				vx = -vx;
				vy = -vy;
			}
			double mass = (Math.random() * 2.0) * solarmass * 10 + 1e10;
			double radius = Math.floor(mass * 5 / (2.0 * solarmass * 10 + 1e10));
			bodies[i] = new Body(px, py, vx, vy, mass, radius);
		}
		bodies[0] = new Body(0, 0, 0, 0, 1e6 * solarmass, 10);// put a heavy body in the center
	}

	// The BH algorithm: calculate the forces
	public void addforces(Quad q, int N) {
		BHTree thetree = new BHTree(q);
		// If the body is still on the screen, add it to the tree
		for (int i = 0; i < N; i++) {
			if (bodies[i] != null && bodies[i].in(q))
				thetree.insert(bodies[i]);
		}
		// Now, use out methods in BHTree to update the forces,
		// traveling recursively through the tree
		for (int i = 0; i < N; i++) {
			if (bodies[i] != null) {
				bodies[i].resetForce();
				if (bodies[i].in(q)) {
					thetree.updateForce(bodies[i]);
					// Calculate the new positions on a time step dt (1e11 here)
					bodies[i].update(10 * 1e11 / t.getValue());
				}
			}
		}
	}

	// A function to return an exponential distribution for position
	public static double exp(double lambda) {
		return -Math.log(1 - Math.random()) / lambda;
	}

	public boolean action(Event e, Object o) {
		N = Integer.parseInt(t1.getText());
		if (N > 10000) {
			t1.setText("10000");
			N = 10000;
		}

		startBodies();
		repaint();

		return true;
	}

	public void actionPerformed(ActionEvent evt) {
		// Get label of the button clicked in event passed in
		String arg = evt.getActionCommand();
		if (arg.equals("Restart")) {
			shouldrun = false;

			try {
				Thread.sleep(1000L / 100L);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			this.bodies = new Body[10000];
			shouldrun = true;
			N = Integer.parseInt(t1.getText());
			if (N > 10000) {
				t1.setText("10000");
				N = 10000;
			}

			startBodies();
			renderer = null;
			this.start();
		} else if (arg.equals("Stop"))
			stop();
	}

	public void start() {
		Thread t = new Thread() {
			public void run() {
				int step = 0;
				while (shouldrun) {
					chartPanel.setChart(getVelocityChart(bodies));
					// go through the Barnes-Hut algorithm (see the function below)

					for (int i = 0; i < N; i++) {
						if (bodies[i] != null) {
							Quad q = new Quad(bodies[i].rx, bodies[i].ry, 2 * 1e18);

							addforces(q, N);
						}
					}

					/*
					 * Collision isn't working
					if (step % 10 == 0) {
						for (int i = 1; i < N; i++) {
							if (bodies[i] != null) {
								N = bodies[i].collide(bodies, N);
							}
						}
					}
					*/

					try {
						Thread.sleep(1000L / 60L);
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					l2.setText(""+step++);
				}
			}
		};
		t.start();
	}

	private XYLineAndShapeRenderer renderer;

	public JFreeChart getVelocityChart(Body[] bodies) {
		double solarmass = 1.98892e30;
		boolean render = false;
		if (renderer == null) {
			render = true;
			renderer = new XYLineAndShapeRenderer();
		}
		XYSeriesCollection dataset = new XYSeriesCollection();
		for (int i = 0; i < N; i++) {
			Body body = bodies[i];
			if (body != null) {
				XYSeries bodySeries = new XYSeries("Particle " + i, false);

				bodySeries.add((int) Math.round(body.rx * 1000 / 1e18), (int) Math.round(body.ry * 1000 / 1e18));

				dataset.addSeries(bodySeries);

				double m = body.radius;
				if (render) {
					renderer.setSeriesShape(i, new Ellipse2D.Double(-m / 2, -m / 2, m, m));
					renderer.setSeriesPaint(i, body.color);
				}
			}
		}

		String chartTitle = "Particle Interaction";
		String xAxisLabel = "X Axis";
		String yAxisLabel = "Y Axis";

		JFreeChart chart = ChartFactory.createScatterPlot(chartTitle, xAxisLabel, yAxisLabel, (XYDataset) dataset);

		chart.getXYPlot().setRenderer(renderer);

		chart.removeLegend();

		NumberAxis nax = (NumberAxis) chart.getXYPlot().getDomainAxis();
		NumberAxis nay = (NumberAxis) chart.getXYPlot().getRangeAxis();

		nax.setRangeAboutValue(0, 2000);
		nay.setRangeAboutValue(0, 2000);

		return chart;
	}

	public static double distance(double x, double y, double x2, double y2) {
		return Math.sqrt(Math.pow(x2 - x, 2) + Math.pow(y2 - y, 2));
	}

}