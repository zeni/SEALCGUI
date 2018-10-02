import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class SEALCGUI extends PApplet {

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

int bgColor, textColor;
int textSize, textLead;
Input[] inputs;
int selectedInput;
PFont myFont;

public void setup() {
	
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

public void draw() {
	background(bgColor);
	for (int i = 0; i < N_INPUTS; i++)
		inputs[i].display();
}

public void keyPressed() {}

public void mouseReleased() {
	if (mouseButton == LEFT) {
		int s = -1;
		int i = 0;
		while (s < 0) {
			s = inputs[i++].checkSelected(mouseX, mouseY);
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
class Input {
    Stepper[] steppers = new Stepper[N_STEPPERS];
    Pot[] pots = new Pot[N_STEPPERS];
    boolean selected;
    int xPos, yPos;
    int height, width;
    int id;
    int selectedPot;

    Input() {}

    Input(int _id) {
        id = _id;
        selectedPot = POT_LEVEL;
        selected = false;
        yPos = 10;
        width = 250;
        height = 700;
        xPos = 10 * (id + 1) + id * width;
        for (int i = 0; i < N_STEPPERS; i++) {
            steppers[i] = new Stepper(48, i);
            pots[i] = new Pot(i);
        }
        pots[POT_LEVEL].setSelected(true);
    }

    public void display() {
        fill(230);
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(250);
        rect(xPos, yPos, width, height);
        pushMatrix();
        translate(xPos + width * .25f, yPos);
        for (int i = 0; i < N_STEPPERS; i++)
            pots[i].display();
        popMatrix();
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public int checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            int s = -1;
            int i = 0;
            while (s < 0) {
                s = pots[i++].checkSelected(PApplet.parseInt(x - xPos - width * .25f), y - yPos);
                if (s >= 0) {
                    if (s != selectedPot) {
                        pots[s].setSelected(true);
                        pots[selectedPot].setSelected(false);
                        selectedPot = s;
                    }
                } else {
                    if (i == N_STEPPERS) s = 0;
                }
            }
            return id;
        } else return -1;
    }
}
class Pot {
    int xPos, yPos;
    int radius;
    int id;
    boolean selected;
    int angle;

    Pot() {}

    Pot(int i) {
        id = i;
        radius = 30;
        xPos = 0;
        yPos = (radius * 2 + 20) * (id + 1);
        selected = false;
    }

    public void display() {
        noFill();
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(20);
        pushMatrix();
        translate(xPos, yPos);
        rotateZ(-PI * FULL_ANGLE / 360.0f);
        ellipse(0, 0, 2 * radius, 2 * radius);
        triangle(0, -radius - 10, -5, -radius, 5, -radius);
        popMatrix();
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public int checkSelected(int x, int y) {
        if (distance(x, y)) {
            return id;
        } else return -1;
    }

    public boolean distance(int x, int y) {
        float d = sqrt(pow(x - xPos, 2) + pow(y - yPos, 2));
        if (d < radius) return true;
        else return false;
    }
}
class Slider {
    int height, width;
    int pos;

    Slider() {}
}
class Stepper {
    int realSteps;
    int absoluteSteps;
    int absoluteStepsIdle;
    int id;
    int nSteps;
    int steps; // for move/hammer
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    long timeMS; // for speed
    int speed; // en ms
    int speedRPM = 12; //en RPM
    boolean newBeat;
    int pause;
    boolean isPaused;
    int xPos, yPos;
    int radius;
    boolean selected;
    int inc;

    Stepper() {}

    Stepper(int n, int i) {
        id = i;
        nSteps = n;
        currentSteps = 0;
        realSteps = currentSteps;
        absoluteSteps = currentSteps;
        absoluteStepsIdle = absoluteSteps;
        speedRPM = 12;
        speed = floor(60000.0f / (speedRPM * nSteps));
        inc = getInc();
        steps = 0;
        dir = 0;
        currentDir = dir;
        pause = 1000;
        isPaused = false;
        timeMS = millis();
    }

    public void SS(int v) {
        speedRPM = (v > 60000.0f / nSteps) ? floor(60000.0f / nSteps) : v;
        speed = (speedRPM > 0) ? (floor(60000.0f / (speedRPM * nSteps))) : 0;
        inc = getInc();
    }

    public void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public int getInc() {
        float f = 1000 / frameRate;
        int i = (f > speed) ? round(f / speed) : 1;
        return i;
    }

    public void absoluteStepsDir() {
        currentSteps += inc;
        if (currentDir > 0)
            absoluteSteps -= inc;
        else absoluteSteps += inc;
        absoluteSteps %= nSteps;
    }
}
  public void settings() { 	size(1200, 800, P3D); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "SEALCGUI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
