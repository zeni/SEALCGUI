/**
 * GUI for SEALC
 * TODO:
 * - add time compensation when speed (in ms) is lower than frame duration.
 **/
//import processing.serial.*;
final static int N_INPUTS = 2;
static final int N_STEPPERS = 7;
static final int POT_GAIN = 0;
static final int POT_HIGH = 1;
static final int POT_MID = 2;
static final int POT_LOW = 3;
static final int POT_FX = 4;
static final int POT_PAN = 5;
static final int POT_LEVEL = 6;
final static int FULL_ANGLE = 300;

color bgColor, textColor;
int textSize, textLead;
Input[] inputs;
int selectedInput;
int mouseX0, mouseY0;
PFont myFont;

void setup() {
	size(1200, 800, P3D);
	frameRate(1000);
	inputs = new Input[N_INPUTS];
	for (int i = 0; i < N_INPUTS; i++)
		inputs[i] = new Input(i);
	bgColor = color(40, 50, 50);
	textColor = color(230);
	background(bgColor);
	textAlign(LEFT, TOP);
	myFont = loadFont("Arial-BoldMT-16.vlw");
	textSize = 16;
	textFont(myFont, textSize);
	textLead = textSize + 4;
	textLeading(textLead);
	strokeWeight(2);
	selectedInput = 0;
	inputs[0].setSelected(true);
}

void draw() {
	background(bgColor);
	for (int i = 0; i < N_INPUTS; i++)
		inputs[i].display();
}

void keyPressed() {}

void mousePressed() {
	if (mouseButton == LEFT) {
		mouseY0 = mouseY;
		mouseX0 = mouseX;
		int s = -1;
		int i = 0;
		while (s < 0) {
			s = inputs[i++].checkSelected(mouseX0, mouseY0);
			if (s >= 0) {
				if (s != selectedInput) {
					inputs[s].setSelected(true);
					inputs[selectedInput].setSelected(false);
					selectedInput = s;
				}
			} else {
				if (i == N_INPUTS) s = 0;
			}
		}
	}
}

void mouseReleased() {
	for (int i = 0; i < N_INPUTS; i++)
		inputs[i].unselect();
}

void mouseDragged() {
	if (mouseButton == LEFT) {
		/*int s = -1;
		int i = 0;
		while (s < 0) {
			s = inputs[i++].checkSelected(mouseX0, mouseY0);
			if (s >= 0) {
				inputs[s].checkPotSelected(mouseX0, mouseY0);
			} else {
				if (i == N_INPUTS) s = 0;
			}
		}*/
		inputs[selectedInput].checkPotSelected(mouseX0, mouseY0);
	}
}