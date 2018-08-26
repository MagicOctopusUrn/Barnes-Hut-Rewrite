import java.awt.Color;

public class Body extends Thread implements Comparable<Body> {
	private static final double G = 6.673e-11; // gravitational constant
	private static final double solarmass = 1.98892e30;
	
	public double collisions;
	public double rx, ry; // holds the cartesian positions
	public double vx, vy; // velocity components
	public double fx, fy; // force components
	public double mass; // mass
	public double radius;
	public Color color; // color (for fun)

	// create and initialize a new Body
	public Body(double rx, double ry, double vx, double vy, double mass, double radius) {
		this.rx = rx;
		this.ry = ry;
		this.vx = vx;
		this.vy = vy;
		this.mass = mass;
		try {
			int red = (int) Math.floor(mass * 254 / (2.0 * solarmass * 10 + 1e20));
			int blue = 255;
			int green = (int) Math.floor(mass * 254 / (2.0 * solarmass * 10 + 1e20));
			color = new Color(red, green, blue);
		} catch (java.lang.IllegalArgumentException e) {
			color = Color.YELLOW;
		}
		this.radius = radius;
	}

	// update the velocity and position using a timestep dt
	public void update(double dt) {
		vx += dt * fx / mass;
		vy += dt * fy / mass;
		rx += dt * vx;
		ry += dt * vy;
	}

	// returns the distance between two bodies
	public double distanceTo(Body b) {
		double dx = rx - b.rx;
		double dy = ry - b.ry;
		return Math.sqrt(dx * dx + dy * dy);
	}

	// set the force to 0 for the next iteration
	public void resetForce() {
		fx = 0.0;
		fy = 0.0;
	}

	// compute the net force acting between the body a and b, and
	// add to the net force acting on a
	public void addForce(Body b) {
		Body a = this;
		double EPS = 3E4; // softening parameter (just to avoid infinities)
		double dx = b.rx - a.rx;
		double dy = b.ry - a.ry;
		double dist = Math.sqrt(dx * dx + dy * dy);
		double F = (G * a.mass * b.mass) / (dist * dist + EPS * EPS);
		a.fx += F * dx / dist;
		a.fy += F * dy / dist;
	}
	
	public boolean in(Quad q) {
		return q.contains(this.rx, this.ry);
	}

	// convert to string representation formatted nicely
	public String toString() {
		return "" + rx + ", " + ry + ", " + vx + ", " + vy + ", " + mass;
	}

	public static Body add(Body a, Body b) {
		Body c = new Body(a.rx, a.ry, a.vx, a.vy, a.mass + b.mass, Math.sqrt(a.area() + b.area() / (2.0 * Math.PI)));
		c.fx = a.fx + b.fx;
		c.fy = a.fy + b.fy;
		return c;
	}
	
	public int collide(Body[] bodies, int N) {
		for (int i = 1; i < bodies.length; i++) {
			if (bodies[i] != null) {
				if(Body.distance(this, bodies[i]) / 1e18 < (this.radius + bodies[i].radius)/1000.0) {
					Body collision = add(this, bodies[i]);

					this.rx = collision.rx;
					this.ry = collision.ry;
					this.vx = collision.vx;
					this.vy = collision.vy;
					this.mass = collision.mass;
					this.color = collision.color;
					this.radius = collision.radius;
					
					bodies[i] = null;

					for (int j = 0; j < N; j++) {
						if (bodies[j] == null) {
							for (int k = j + 1; k < N; k++) {
								bodies[k - 1] = bodies[k];
							}
							bodies[N - 1] = null;
							break;
						}
					}
					
					N--;
					i--;
				}
			}
		}

		return N;
	}
	
	public static double distance(Body a, Body b) {
		return Math.sqrt(Math.pow(b.rx - a.rx, 2) + Math.pow(b.ry - a.ry, 2));
	}

	@Override
	public int compareTo(Body o) {
		return -(int) Math.sqrt(this.mass * this.mass + o.mass * o.mass);
	}
	
	public double area() {
		return 2 * Math.PI * radius * radius;
	}
}