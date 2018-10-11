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
static final int N_SLIDERS = 2;
static final int POT_GAIN = 0;
static final int POT_HIGH = 1;
static final int POT_MID = 2;
static final int POT_LOW = 3;
static final int POT_FX = 4;
static final int POT_PAN = 5;
static final int POT_LEVEL = 6;
final static int FULL_ANGLE = 300;
final static int HALF_ANGLE = FULL_ANGLE / 2;

int bgColor, textColor;
int textSize, textLead;
Input[] inputs;
int selectedInput;
int mouseX0, mouseY0;
PFont myFont;

public void setup() {
	
	frameRate(1000);
	inputs = new Input[N_INPUTS];
	for (int i = 0; i < N_INPUTS; i++)
		inputs[i] = new Input(i);
	bgColor = color(40, 50, 50);
	textColor = color(230);
	background(bgColor);
	textAlign(CENTER, CENTER);
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

public void mousePressed() {
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
			} else
			if (i == N_INPUTS) s = 0;
		}
	}
}

public void mouseReleased() {
	for (int i = 0; i < N_INPUTS; i++)
		inputs[i].unselect();
}

public void mouseDragged() {
	if (mouseButton == LEFT)
		inputs[selectedInput].checkPotSelected(mouseX0, mouseY0);
}
class HSlider implements Slider {
    int height, width;
    int xPos, yPos;
    float value;
    int id;
    boolean selected;


    HSlider() {}

    HSlider(int i) {
        id = i;
        height = 20;
        width = 100;
        xPos = 0;
        yPos = id * (height + 10);
        value = 50;
        selected = false;
    }

    public void display() {
        pushMatrix();
        translate(xPos, yPos);
        fill(30);
        noStroke();
        rect(0, 0, width * value / 100, height);
        noFill();
        stroke(50);
        rect(0, 0, width, height);
        popMatrix();
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public int checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            return id;
        } else return -1;
    }

    public void setValue() {
        int x = mouseX - pmouseX;
        int inc = 2;
        if (x != 0) {
            value += ((x > 0) ? inc : -inc);
            if (value < 0) value = 0;
            if (value > 100) value = 100;
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
        width = 260;
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
        noFill();
        pushMatrix();
        translate(xPos, yPos);
        for (int i = 0; i < N_STEPPERS; i++)
            pots[i].display();
        pots[selectedPot].display();
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
                s = pots[i++].checkSelected(x - xPos, y - yPos);
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

    public void checkPotSelected(int x, int y) {
        if (pots[selectedPot].checkSelectedPotOnly(x - xPos, y - yPos) >= 0)
            pots[selectedPot].setAngle();
    }

    public void unselect() {
        for (int i = 0; i < N_STEPPERS; i++)
            pots[i].unselect();
    }
}
class Pot {
    int xPos, yPos;
    int xOffset, yOffset;
    int height, width;
    int radius;
    int id;
    boolean selected;
    float angle;
    Slider[] sliders;
    String name;
    int selectedSlider;
    TickBox sweepTickBox;

    Pot() {}

    Pot(int _id) {
        id = _id;
        radius = 30;
        xPos = 0;
        height = (radius * 3);
        yPos = height * id;
        width = 260;
        xOffset = xPos + radius * 3 + 20;
        yOffset = yPos + PApplet.parseInt(.5f * radius);
        selected = false;
        sliders = new Slider[N_SLIDERS];
        sliders[1] = new RangeSlider(1);
        sliders[0] = new HSlider(0);
        sweepTickBox = new TickBox(0);
        switch (id) {
            case POT_GAIN:
                angle = -HALF_ANGLE;
                name = "GAIN";
                break;
            case POT_LEVEL:
                angle = -HALF_ANGLE;
                name = "LEVEL";
                break;
            case POT_PAN:
                angle = 0;
                name = "PAN";
                break;
            case POT_HIGH:
                angle = 0;
                name = "HI";
                break;
            case POT_MID:
                angle = 0;
                name = "MID";
                break;
            case POT_LOW:
                angle = 0;
                name = "LOW";
                break;
            case POT_FX:
                angle = -HALF_ANGLE;
                name = "FX";
                break;
        }
        selectedSlider = -1;
    }

    public void display() {
        fill(200);
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(20);
        pushMatrix();
        translate(xPos, yPos);
        rect(0, 0, width, height);
        translate(2 * radius, 1.5f * radius);
        if (selected)
            fill(255, 0, 0);
        else
            fill(20);
        text(name, 0, 0);
        noFill();
        rotateZ(radians(angle));
        ellipse(0, 0, 2 * radius, 2 * radius);
        triangle(0, -radius - 10, -5, -radius, 5, -radius);
        popMatrix();
        pushMatrix();
        translate(xOffset, yOffset);
        for (int i = 0; i < N_SLIDERS; i++)
            sliders[i].display();
        translate(120, 0);
        sweepTickBox.display();
        popMatrix();

    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public boolean getSelected() {
        return selected;
    }

    public int checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            sweepTickBox.checkSelected(x - xOffset - 120, y - yOffset);
            return id;
        } else
            return -1;
    }

    public int checkSelectedPotOnly(int x, int y) {
        if (distance(x - 2 * radius, PApplet.parseInt(y - 1.5f * radius)))
            return id;
        else {
            if (selectedSlider < 0) {
                int s = -1;
                int i = 0;
                while (s < 0) {
                    s = sliders[i++].checkSelected(x - xOffset, y - yOffset);
                    if (s >= 0) {
                        selectedSlider = s;
                        sliders[selectedSlider].setSelected(true);
                        sliders[selectedSlider].setValue();
                    } else {
                        if (i == N_SLIDERS) s = 0;
                    }
                }
            } else {
                if (sliders[selectedSlider].checkSelected(x - xOffset, y - yOffset) >= 0)
                    sliders[selectedSlider].setValue();
            }
        }
        return -1;
    }

    public boolean distance(int x, int y) {
        float d = sqrt(pow(x - xPos, 2) + pow(y - yPos, 2));
        if (d < radius) return true;
        else return false;
    }

    public void setAngle() {
        int y = mouseY - pmouseY;
        int x = mouseX - pmouseX;
        if ((y != 0) && (abs(y) >= abs(x))) {
            angle += ((y > 0) ? -10 : 10);
            if (angle < -FULL_ANGLE * .5f) angle = -FULL_ANGLE * .5f;
            if (angle > FULL_ANGLE * .5f) angle = FULL_ANGLE * .5f;
        } else if ((x != 0) && (abs(x) > abs(y))) {
            angle += ((x > 0) ? 10 : -10);
            if (angle < -FULL_ANGLE * .5f) angle = -FULL_ANGLE * .5f;
            if (angle > FULL_ANGLE * .5f) angle = FULL_ANGLE * .5f;
        }
    }

    public void unselect() {
        selectedSlider = -1;
        for (int i = 0; i < N_SLIDERS; i++)
            sliders[i].setSelected(false);
    }
}
class RangeSlider implements Slider {
    int height, width;
    int xPos, yPos;
    float valueStart, valueEnd;
    int id;
    boolean valueStartSelected;
    boolean selected;


    RangeSlider() {}

    RangeSlider(int i) {
        id = i;
        height = 20;
        width = 100;
        xPos = 0;
        yPos = id * (height + 10);
        valueEnd = 50;
        valueStart = 0;
        valueStartSelected = true;
        selected = false;
    }

    public void display() {
        pushMatrix();
        translate(xPos, yPos);
        fill(30);
        noStroke();
        rect(width * valueStart / 100, 0, width * (valueEnd - valueStart) / 100, height);
        noFill();
        stroke(50);
        rect(0, 0, width, height);
        noStroke();
        fill(50);
        rect(width * valueStart / 100 - 5, -2, 10, height + 4);
        rect(width * valueEnd / 100 - 5, -2, 10, height + 4);
        popMatrix();
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public int checkSelected(int x, int y) {
        if (selected) {
            if (valueStartSelected)
                x += width * valueStart / 100;
            else
                x += width * valueEnd / 100;
            return id;
        } else {
            if (checkHandle(x, y, valueStart)) {
                valueStartSelected = true;
                return id;
            } else if (checkHandle(x, y, valueEnd)) {
                valueStartSelected = false;
                return id;
            } else
                return -1;
        }
    }

    public boolean checkHandle(int x, int y, float v) {
        return ((x > xPos + width * v / 100 - 5) && (x < xPos + width * v / 100 + 5) && (y > yPos - 2) && (y < yPos + height + 4));
    }

    public void setValue() {
        int x = mouseX - pmouseX;
        int inc = 2;
        if (x != 0) {
            if (valueStartSelected) {
                valueStart += ((x > 0) ? inc : -inc);
                if (valueStart < 0) valueStart = 0;
                if (valueStart > valueEnd) valueStart = valueEnd;
            } else {
                valueEnd += ((x > 0) ? inc : -inc);
                if (valueEnd < valueStart) valueEnd = valueStart;
                if (valueEnd > 100) valueEnd = 100;
            }
        }
    }
}
interface Slider {
    public void display();
    public void setSelected(boolean s);
    public int checkSelected(int x, int y);
    public void setValue();
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
class TickBox {
    int height, width;
    int xPos, yPos;
    boolean value;
    int id;
    boolean selected;

    TickBox() {}

    TickBox(int i) {
        id = i;
        height = 20;
        width = 20;
        xPos = 0;
        yPos = 0;
        value = false;
        selected = false;
    }

    public void display() {
        pushMatrix();
        translate(xPos, yPos);
        noFill();
        stroke(50);
        rect(0, 0, width, height);
        if (value) {
            fill(30);
            noStroke();
            rect(2, 2, width - 4, height - 4);
        }
        popMatrix();
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public void checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            value = !value;
        }
    }

    public void setValue() {
        value = !value;
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
