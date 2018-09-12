class Stepper {
	int xPos, yPos;
	int radius;
	Stepper(int x, int y, int r) {
		xPos = x;
		yPos = y;
		radius = r;
	}
	void display() {
		ellipse(xPos, yPos, radius, radius);
	}
}