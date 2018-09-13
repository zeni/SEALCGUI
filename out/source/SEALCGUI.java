import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import processing.serial.*; 

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
 *
 **/


static final int STATE_CONNECT = 1;
static final int STATE_RUNNING = 2;
static final int STATE_SELECT = 0;

static final int TYPE_SERVO = 1;
static final int TYPE_VIBRO = 2;
static final int TYPE_STEPPER = 0;

static final char EOL = '\n';
static final char SEPARATOR = ',';
static final char COLUMN = ':';
// commands
static final int COMMAND_SELECT = 0;
static final int COMMAND_SS = 1; // Set Speed
static final int COMMAND_SD = 2; // Set Direction
static final int COMMAND_RO = 3; // ROtate
static final int COMMAND_ST = 4; // STop
static final int COMMAND_RA = 5; // Rotate Angle (stepper) / Rotate Absolute (servo)
static final int COMMAND_NONE = 6;
static final int COMMAND_RW = 7; // Rotate Wave
static final int COMMAND_SQ = 8; // SeQuence
static final int COMMAND_ERROR = 9;
static final int COMMAND_RP = 10; // Rotate Pause
static final int COMMAND_GS = 11; // Get Speed
static final int COMMAND_GD = 12; // Get Direction
static final int COMMAND_GM = 13; // Get Mode
static final int COMMAND_GI = 14; // Get Id
static final int COMMAND_SA = 15; // Stop All
static final int COMMAND_RR = 16; // Rotate Angle (stepper) / Rotate Relative (servo)
static final int COMMAND_WA = 17; // WAit command (ms)
//modes
static final int MODE_ST = 0;
static final int MODE_RO = 1;
static final int MODE_RA = 2;
static final int MODE_RR = 3;
static final int MODE_RW = 4;
static final int MODE_SQ = 5;
static final int MODE_WA = 6;
static final int MODE_SD = 7;
static final int MODE_RP = 8;
static final int MODE_IDLE = 9;

static final int MAX_SEQ = 10; // max length of sequence for beat
static final int MAX_QUEUE = 10;

Serial myPort;
String myText, inBuffer;
int state;
int nPorts;
String[] portsList;
int bgColor, textBoxColor, textColor;
int offsetX, offsetY;
int offsetText;
int textBoxWidth, inTextBoxHeight;
int textSize, textLead;
Motor[] motors;
int nMotors;
boolean firstChar;
char[] command = new char[2];
int iCommand;
int currentValue, selectedMotor;
int currentCommand;

public void setup() {
	
	bgColor = color(40);
	textBoxColor = color(20);
	textColor = color(230);
	background(bgColor);
	myText = "";
	inBuffer = "";
	textAlign(LEFT, TOP);
	state = STATE_SELECT;
	portsList = Serial.list();
	nPorts = portsList.length;
	offsetX = 5;
	offsetY = 5;
	offsetText = 2;
	textBoxWidth = 400;
	PFont myFont = loadFont("Calibri-16.vlw");
	textSize = 16;
	textFont(myFont, textSize);
	textLead = textSize + 4;
	textLeading(textLead);
	inTextBoxHeight = 300;
	command[0] = 0;
	command[1] = 0;
	iCommand = 0;
	firstChar = true;
	currentValue = -1;
	selectedMotor = 0;
	currentCommand = COMMAND_NONE;
}

public void draw() {
	background(bgColor);
	switch (state) {
		case STATE_SELECT:
			selectPort();
			break;
		case STATE_CONNECT:
			//char a = 0;
			while (myPort.available() > 0)
				inBuffer += myPort.readString();
			if (inBuffer.length() > 0) {
				if (inBuffer.charAt(inBuffer.length() - 1) == '<') {
					sendSetup();
					state = STATE_RUNNING;
				}
			}
			break;
		case STATE_RUNNING:
			writeTextBox(textBoxColor);
			readTextBox();
			for (int i = 0; i < nMotors; i++) {
				motors[i].display();
				motors[i].action();
			}
			break;
	}
}

public void keyPressed() {
	switch (state) {
		case STATE_SELECT:
			if ((PApplet.parseInt(key) >= 48) && (PApplet.parseInt(key) <= 48 + nPorts - 1)) {
				myPort = new Serial(this, portsList[PApplet.parseInt(key) - 48], 115200);
				state = STATE_CONNECT;
			}
			break;
		case STATE_RUNNING:
			switch (key) {
				case BACKSPACE:
					if (myText.length() > 0)
						myText = myText.substring(0, myText.length() - 1);
					break;
				case DELETE:
					myText = "";
					break;
				case ENTER:
				case RETURN:
					myText = myText + '\n';
					sendText();
					for (int i = 0; i < myText.length(); i++) {
						processCommand(myText.charAt(i));
					}
					myText = "";
					break;
				default:
					//processCommand(key);
					myText = myText + key;
					myText = myText.toUpperCase();
					break;
			}
			break;
	}
}

public void updateValue(char a) {
	if (currentValue < 0)
		currentValue = 0;
	currentValue *= 10;
	currentValue += (a - 48);
}

public void processCommand(char a) {
	if ((a >= 48) && (a < 58)) {
		if (firstChar) {
			currentCommand = COMMAND_SELECT;
			selectedMotor = a - 48;
			if (selectedMotor >= nMotors)
				selectedMotor = nMotors - 1;
			for (int i = 0; i < nMotors; i++)
				motors[i].setSelected(false);
			motors[selectedMotor].setSelected(true);
			firstChar = false;
		} else
			updateValue(a);
	} else
		command[iCommand++] = a;
	if (iCommand == 1) {
		switch (command[0]) {
			case COLUMN:
				if (firstChar)
					currentCommand = COMMAND_ERROR;
				else {
					switch (currentCommand) {
						case COMMAND_SQ:
							motors[selectedMotor].columnSQ(currentValue);
							break;
						case COMMAND_RP:
							motors[selectedMotor].columnRP(currentValue);
							break;
					}
				}
				currentValue = -1;
				iCommand = 0;
				command[0] = 0;
				break;
			case SEPARATOR:
			case EOL:
				if (firstChar)
					currentCommand = COMMAND_NONE;
				switch (currentCommand) {
					case COMMAND_SS:
						motors[selectedMotor].SS(currentValue);
						break;
					case COMMAND_SD:
						motors[selectedMotor].fillQ(MODE_SD, currentValue);
						break;
					case COMMAND_RO:
						motors[selectedMotor].fillQ(MODE_RO, currentValue);
						break;
					case COMMAND_ST:
						motors[selectedMotor].fillQ(MODE_ST, currentValue);
						break;
					case COMMAND_RA:
						motors[selectedMotor].fillQ(MODE_RA, currentValue);
						break;
					case COMMAND_RR:
						motors[selectedMotor].fillQ(MODE_RR, currentValue);
						break;
					case COMMAND_RW:
						motors[selectedMotor].fillQ(MODE_RW, currentValue);
						break;
					case COMMAND_SQ:
						motors[selectedMotor].fillQ(MODE_SQ, currentValue);
						break;
					case COMMAND_RP:
						motors[selectedMotor].fillQ(MODE_RP, currentValue);
						break;
					case COMMAND_WA:
						motors[selectedMotor].fillQ(MODE_WA, currentValue);
						break;
					case COMMAND_GS:
						motors[selectedMotor].GS();
						break;
					case COMMAND_GD:
						motors[selectedMotor].GD();
						break;
					case COMMAND_GM:
						motors[selectedMotor].GM();
						break;
					case COMMAND_GI:
						motors[selectedMotor].GI(selectedMotor);
						break;
					case COMMAND_SA:
						for (int i = 0; i < nMotors; i++) {
							motors[i].ST();
						}
						break;
					case COMMAND_SELECT:
					case COMMAND_ERROR:
					case COMMAND_NONE:
						break;
				}
				currentValue = -1;
				firstChar = true;
				iCommand = 0;
				command[0] = 0;
				break;
		}
	} else if (iCommand == 2) {
		currentCommand = COMMAND_ERROR;
		switch (command[0]) {
			case 's':
			case 'S':
				switch (command[1]) {
					case 's':
					case 'S':
						currentCommand = COMMAND_SS; //SS
						break;
					case 'd':
					case 'D':
						currentCommand = COMMAND_SD; //SD
						break;
					case 't':
					case 'T':
						currentCommand = COMMAND_ST; //ST
						break;
					case 'a':
					case 'A':
						currentCommand = COMMAND_SA; //ST
						break;
					case 'q':
					case 'Q':
						currentCommand = COMMAND_SQ; //SQ
						motors[selectedMotor].initSQ();
						break;
				}
				break;
			case 'r':
			case 'R':
				switch (command[1]) {
					case 'o':
					case 'O':
						currentCommand = COMMAND_RO; //RO
						break;
					case 'W':
					case 'w':
						currentCommand = COMMAND_RW; //RW
						break;
					case 'a':
					case 'A':
						currentCommand = COMMAND_RA; //RA
						break;
					case 'p':
					case 'P':
						currentCommand = COMMAND_RP; //RP
						//motors[selectedMotor]->initRP();
						break;
				}
				break;
			case 'g':
			case 'G':
				switch (command[1]) {
					case 's':
					case 'S':
						currentCommand = COMMAND_GS; //GS
						break;
					case 'd':
					case 'D':
						currentCommand = COMMAND_GD; //GD
						break;
					case 'm':
					case 'M':
						currentCommand = COMMAND_GM; //GM
						break;
					case 'i':
					case 'I':
						currentCommand = COMMAND_GI; //GI
						break;
				}
				break;
			case 'w':
			case 'W':
				switch (command[1]) {
					case 'a':
					case 'A':
						currentCommand = COMMAND_WA; //GS
						//motors[selectedMotor]->initWA();
						break;
				}
				break;
		}
		currentValue = -1;
		firstChar = false;
		iCommand = 0;
		command[0] = 0;
		command[1] = 0;
	}
}

public void writeTextBox(int c) {
	fill(c);
	noStroke();
	rect(offsetX, offsetY, textBoxWidth, textLead);
	textAlign(LEFT, CENTER);
	for (int i = 0; i < myText.length(); i++) {
		if ((myText.charAt(i) >= 48) && (myText.charAt(i) < 58)) {
			fill(textColor);
		} else if ((myText.charAt(i) >= 65) && (myText.charAt(i) < 91)) {
			fill(color(255, 0, 0));
		} else
			fill(color(0, 255, 0));
		int w = PApplet.parseInt(textWidth(myText.substring(0, i)));
		text(myText.substring(i, i + 1), offsetX + offsetText + w, offsetY, textBoxWidth, textLead);
	}
	textAlign(LEFT, TOP);
}

public void sendText() {
	writeTextBox(color(255, 0, 0));
	myPort.write(myText);
}

public void readTextBox() {
	while (myPort.available() > 0)
		inBuffer += myPort.readString();
	fill(textBoxColor);
	noStroke();
	rect(offsetX, offsetY + 25, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	scrollText();
	text(inBuffer, offsetX + offsetText, offsetY + 25 + offsetText, textBoxWidth, inTextBoxHeight);
}

public void scrollText() {
	int nLines = 0;
	for (int i = 0; i < inBuffer.length(); i++) {
		if (inBuffer.charAt(i) == '\n') nLines++;
	}
	if (nLines * textLead > inTextBoxHeight) {
		int a = inBuffer.indexOf('\n');
		inBuffer = inBuffer.substring(a + 1);
	}
}

public void selectPort() {
	fill(textBoxColor);
	noStroke();
	rect(offsetX, offsetY, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	String portsListString = "Please select port:\n";
	for (int i = 0; i < nPorts; i++) {
		portsListString += "[" + i + "] ";
		portsListString += portsList[i];
		portsListString += "\n";
	}
	text(portsListString, offsetX + offsetText, offsetY + offsetText, textBoxWidth, 200);
}

public void sendSetup() {
	BufferedReader reader = createReader("setup.txt");
	String line = null;
	int n = 0;
	try {
		while ((line = reader.readLine()) != null) {
			if (n == 0) {
				nMotors = line.charAt(0) - 48;
				motors = new Motor[nMotors];
			} else {
				switch (line.charAt(0) - 48) {
					case 0:
						motors[n - 1] = new Stepper(200);
						motors[n - 1].setGraphics(500 + (n - 1) * 50, 50, 20);
						break;
					case 1:
						motors[n - 1] = new Servo(0, 180);
						motors[n - 1].setGraphics(500 + (n - 1) * 50, 50, 20);
						break;
				}
			}
			myPort.write(line + '\n');
			n++;
		}
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	motors[selectedMotor].setSelected(true);
}
interface Motor {
    public void setSelected(boolean s);
    public void columnSQ(int v);
    public void setGraphics(int x, int y, int r);
    public void display();
    public void initRP();
    public void SS(int v);
    public void setRO(int v);
    public void setRP(int v);
    public void setRA(int v);
    public void setRR(int v);
    public void setRW(int v);
    public void setWA(int v);
    public void VA();
    public void initSQ();
    public void setSQ(int v);
    public void columnRP(int v);
    public void ST();
    public void action();
    public void setSD(int v);
    public String getType();
    public void setNextMode(int m);
    public void GS();
    public void GD();
    public void GM();
    public void GI(int i);
    public void fillQ(int m, int v);
    public void deQ();
    public void initWA();
}
class Servo implements Motor {
    int angleMin, angleMax;
    int angle; // current angle
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int nextMode;
    int steps; // for move/hammer
    int stepsHome; // steps for homing
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int indexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    long timeMS; // for speed
    int speed; // en ms
    int speedRPM; //en RPM
    boolean newBeat;
    int[] modesQ = new int[MAX_QUEUE];
    int[] valuesQ = new int[MAX_QUEUE];
    int sizeQ;
    int pause;
    boolean isPaused;
    int xPos, yPos;
    int radius;
    int type;
    boolean selected;

    Servo(int amin, int amax) {
        angleMin = amin;
        angleMax = amax;
        nSteps = 360;
        for (int j = 0; j < MAX_SEQ; j++)
            seq[j] = 0;
        angleSeq = 0;
        speed = (speedRPM > 0) ? (floor(60.0f / (speedRPM * nSteps) * 1000)) : 0;
    }

    public void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    public void display() {
        noFill();
        if (selected)
            stroke(255, 0, 0);
        else stroke(255);
        ellipse(xPos, yPos, 2 * radius, 2 * radius);
        line(xPos, yPos - radius, xPos, yPos + radius);
    }

    public String getType() {
        return " (servo)";
    }

    public void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    public void setRO(int v) {
        mode = MODE_RO;
    }

    public void initRP() {}

    public void columnRP(int v) {}

    public void setRP(int v) {
        mode = MODE_RP;
    }

    public void setRR(int v) {
        v = (v <= 0) ? 0 : (v % (angleMax - angleMin));
        v = (dir > 0) ? -v : v;
        steps = PApplet.parseInt(abs(v) / 360.0f * nSteps);
        currentSteps = 0;
        mode = MODE_RR;
        timeMS = millis();
    }

    public void setRA(int v) {
        v = (v < angleMin) ? angleMin : ((v > angleMax) ? angleMax : v);
        if (v > angle) {
            v = v - angle;
            currentDir = 1;
        } else {
            v = angle - v;
            currentDir = 0;
        }
        steps = PApplet.parseInt(v / 360.0f * nSteps);
        currentSteps = 0;
        mode = MODE_RA;
        timeMS = millis();
    }

    public void setRW(int v) {
        mode = MODE_RW;
    }

    public void initSQ() {
        angleSeq = 0;
        indexSeq = 0;
        lengthSeq = 0;
        newBeat = true;
    }

    public void columnSQ(int v) {
        v = (v <= 0) ? 0 : v;
        if (angleSeq == 0)
            angleSeq = v;
        else {
            seq[indexSeq] = v;
            indexSeq++;
        }
    }

    public void setSQ(int v) {
        currentDir = dir;
        if (angleSeq == 0) {
            angleSeq = v;
            seq[indexSeq] = 1;
            lengthSeq = 1;
        } else {
            seq[indexSeq] = v;
            indexSeq++;
            lengthSeq = indexSeq;
        }
        angleSeq = PApplet.parseInt(angleSeq / 360.0f * nSteps);
        indexSeq = 0;
        steps = angleSeq;
        angleSeq = 0;
        currentSteps = 0;
        mode = MODE_SQ;
        timeMS = millis();
    }

    // one step servo
    public void servoStep() {
        if (currentDir == 0) {
            angle++;
            if (angle > angleMax) {
                currentSteps = steps;
                angle = angleMax;
            }
        } else {
            angle--;
            if (angle < angleMin) {
                currentSteps = steps;
                angle = angleMin;
            }
        }
    }

    // move one step
    public void moveStep() {
        if (currentSteps >= steps) {
            ST();
        } else {
            currentSteps++;
            servoStep();
            timeMS = millis();
        }
    }

    public void action() {
        switch (mode) {
            case MODE_RO:
            case MODE_RP:
            case MODE_RW:
            case MODE_ST:
                ST();
                break;
            case MODE_IDLE:
                break;
            case MODE_RA:
            case MODE_RR:
                RA();
                break;
            case MODE_SQ:
                SQ();
                break;
            case MODE_WA:
                WA();
                break;
        }
    }

    public void ST() {
        currentSteps = 0;
        mode = MODE_ST;
        deQ();
    }

    public void SD() {
        currentDir = dir;
        deQ();
    }

    // rotate a number of steps
    public void RA() {
        if (speed > 0) {
            if ((millis() - timeMS) > speed)
                moveStep();
        } else {
            ST();
        }
    }

    // continuous hammer movement with pattern of angles
    public void SQ() {
        if (speed > 0) {
            if (newBeat) {
                deQ();
                newBeat = false;
                int a = floor(indexSeq / 2);
                switch (seq[a]) {
                    case 2:
                        currentDir = 1 - dir;
                        break;
                    case 1:
                    case 0:
                        currentDir = dir;
                        break;
                }
            }
            if ((millis() - timeMS) > speed) {
                if (currentSteps >= steps) {
                    currentDir = 1 - currentDir;
                    currentSteps = 0;
                    indexSeq++;
                    if ((indexSeq % 2) == 0)
                        newBeat = true;
                    int a = floor(indexSeq / 2);
                    if (a >= lengthSeq)
                        indexSeq = 0;
                } else {
                    int a = floor(indexSeq / 2);
                    currentSteps++;
                    if (seq[a] > 0) {
                        servoStep();
                    }
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    public void WA() {
        if (isPaused) {
            if ((millis() - timeMS) > pause) {
                isPaused = false;
                ST();
            }
        } else
            isPaused = true;
    }

    public void GM() {

    }

    public void GD() {

    }

    public void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public void setNextMode(int m) {

    }
    public void SS(int v) {

    }
    public void GI(int v) {

    }

    public void deQ() {
        switch (modesQ[0]) {
            case MODE_IDLE:
                break;
            case MODE_ST:
                mode = modesQ[0];
                break;
            case MODE_RO:
                setRO(valuesQ[0]);
                break;
            case MODE_RP:
                setRP(valuesQ[0]);
                break;
            case MODE_RA:
            case MODE_RR:
                setRA(valuesQ[0]);
                break;
            case MODE_RW:
                setRW(valuesQ[0]);
                break;
            case MODE_SQ:
                setSQ(valuesQ[0]);
                break;
            case MODE_SD:
                setSD(valuesQ[0]);
                break;
            case MODE_WA:
                setWA(valuesQ[0]);
                break;
        }
        if (modesQ[0] != MODE_IDLE) {
            for (int i = 1; i < MAX_QUEUE; i++) {
                modesQ[i - 1] = modesQ[i];
                valuesQ[i - 1] = valuesQ[i];
            }
            modesQ[MAX_QUEUE - 1] = MODE_IDLE;
            valuesQ[MAX_QUEUE - 1] = -1;
            sizeQ--;
            sizeQ = (sizeQ < 0) ? 0 : sizeQ;
        }
    }

    public void initWA() {
        pause = 1000;
        isPaused = false;
    }

    public void setWA(int v) {
        isPaused = false;
        v = (v < 0) ? 1000 : v;
        pause = v;
        pause = v;
        mode = MODE_WA;
        timeMS = millis();
    }

    public void GS() {

    }

    public void VA() {

    }
}
class Stepper implements Motor {
    int waveDir; // increasing / decreasing speed
    int turns; // for rotate (0=continuous rotation)
    int realSteps;
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int nextMode;
    int steps; // for move/hammer
    int stepsHome; // steps for homing
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int indexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    long timeMS; // for speed
    int speed; // en ms
    int speedRPM = 12; //en RPM
    boolean newBeat;
    int[] modesQ = new int[MAX_QUEUE];
    int[] valuesQ = new int[MAX_QUEUE];
    int sizeQ;
    int pause;
    boolean isPaused;
    int xPos, yPos;
    int radius;
    int type;
    boolean selected;

    Stepper(int n) {
        waveDir = 0;
        nSteps = n;
        realSteps = currentSteps;
        for (int j = 0; j < MAX_SEQ; j++)
            seq[j] = 0;
        angleSeq = 0;
        speed = (speedRPM > 0) ? (floor(60.0f / (speedRPM * nSteps) * 1000)) : 0;
        mode = MODE_IDLE;
        nextMode = mode;
        speedRPM = 12;
        currentSteps = 0;
        steps = 0;
        stepsHome = steps;
        dir = 0;
        currentDir = dir;
        for (int j = 0; j < MAX_QUEUE; j++)
            modesQ[j] = MODE_IDLE;
        sizeQ = 0;
        indexSeq = 0;
        lengthSeq = 0;
        timeMS = millis();
        pause = 1000;
        isPaused = false;
    }

    public void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    public void display() {
        //println(mode);
        if (mode == MODE_RO)
            fill(255);
        else
            noFill();
        if (selected)
            stroke(255, 0, 0);
        else stroke(255);
        ellipse(xPos, yPos, 2 * radius, 2 * radius);
        line(xPos, yPos - radius, xPos, yPos + radius);
        line(xPos - radius, yPos, xPos + radius, yPos);
    }

    public String getType() {
        return " (stepper)";
    }

    public void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    public void setRO(int v) {
        println(mode);
        if (v <= 0) {
            turns = 0;
        } else {
            turns = v;
        }
        steps = turns * nSteps;
        mode = MODE_RO;
        timeMS = millis();
    }

    public void initRP() {
        turns = 1;
        pause = 1000;
        isPaused = false;
    }

    public void columnRP(int v) {
        turns = (v <= 0) ? 1 : v;
    }

    public void setRP(int v) {
        isPaused = false;
        pause = (v <= 0) ? 1000 : v;
        turns = (turns <= 0) ? 1 : turns;
        steps = turns * nSteps;
        mode = MODE_RP;
        timeMS = millis();
    }

    public void setRR(int v) {
        setRA(v);
    }

    public void setRA(int v) {
        v = (v <= 0) ? 0 : (v % 360);
        steps = PApplet.parseInt(v / 360.0f * nSteps);
        currentSteps = 0;
        mode = MODE_RA;
        timeMS = millis();
    }

    public void setRW(int v) {
        v = (v <= 0) ? 1 : v;
        steps = PApplet.parseInt(nSteps / (2.0f * v));
        currentSteps = 0;
        realSteps = currentSteps;
        waveDir = 0;
        mode = MODE_RW;
        timeMS = millis();
    }

    public void initSQ() {
        angleSeq = 0;
        indexSeq = 0;
        lengthSeq = 0;
        newBeat = true;
    }

    public void columnSQ(int v) {
        v = (v <= 0) ? 0 : v;
        if (angleSeq == 0)
            angleSeq = v;
        else {
            seq[indexSeq] = v;
            indexSeq++;
        }
    }

    public void setSQ(int v) {
        currentDir = dir;
        if (angleSeq == 0) {
            angleSeq = v;
            seq[indexSeq] = 1;
            lengthSeq = 1;
        } else {
            seq[indexSeq] = v;
            indexSeq++;
            lengthSeq = indexSeq;
        }
        angleSeq = PApplet.parseInt(angleSeq / 360.0f * nSteps);
        indexSeq = 0;
        steps = angleSeq;
        angleSeq = 0;
        currentSteps = 0;
        mode = MODE_SQ;
        timeMS = millis();
    }

    // one step stepper
    public void stepperStep() {}

    // move one step
    public void moveStep() {
        if (currentSteps >= steps) {
            ST();
        } else {
            currentSteps++;
            stepperStep();
            timeMS = millis();
        }
    }

    public void action() {
        switch (mode) {
            case MODE_IDLE:
                deQ();
                break;
            case MODE_ST:
                ST();
                break;
            case MODE_SD:
                SD();
                break;
            case MODE_RO:
                RO();
                break;
            case MODE_RP:
                RP();
                break;
            case MODE_RA:
                RA();
                break;
            case MODE_RW:
                RW();
                break;
            case MODE_SQ:
                SQ();
                break;
            case MODE_WA:
                WA();
                break;
        }
    }

    public void ST() {
        currentSteps = 0;
        mode = MODE_IDLE;
        deQ();
    }

    public void SD() {
        currentDir = dir;
        deQ();
    }

    // rotation
    public void RO() {
        fill(255);
        ellipse(xPos, yPos, 2 * radius, 2 * radius);
        if (speed > 0) {
            if ((millis() - timeMS) > speed) {
                if (turns == 0) {
                    currentSteps++;
                    currentSteps %= nSteps;
                    stepperStep();
                    if (currentSteps == 0)
                        deQ();
                    timeMS = millis();
                } else
                    moveStep();
            }
        } else {
            ST();
        }
    }

    // rotation with pause
    public void RP() {
        if (speed > 0) {
            if (isPaused) {
                if ((millis() - timeMS) > pause) {
                    isPaused = false;
                    currentSteps = 0;
                }
            } else {
                if ((millis() - timeMS) > speed) {
                    if (currentSteps >= steps) {
                        isPaused = true;
                        currentSteps = 0;
                        deQ();
                    } else {
                        currentSteps++;
                        stepperStep();
                    }
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    // rotate a number of steps
    public void RA() {
        if (speed > 0) {
            if ((millis() - timeMS) > speed) {
                moveStep();
            }
        } else {
            ST();
        }
    }

    // continuous wave movement (like rotate but with changing speed)
    public void RW() {
        if (speed > 0) {
            int s = (waveDir == 0) ? (speed * (steps - currentSteps)) : (speed * currentSteps);
            if ((millis() - timeMS) > s) {
                if (currentSteps >= steps) {
                    waveDir = 1 - waveDir;
                    currentSteps = 0;
                } else {
                    realSteps++;
                    realSteps %= nSteps;
                    if (realSteps == 0)
                        deQ();
                    currentSteps++;
                    stepperStep();
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    public void WA() {
        if (isPaused) {
            if ((millis() - timeMS) > pause) {
                isPaused = false;
                ST();
            }
        } else {
            isPaused = true;
        }
    }

    // continuous hammer movement with pattern of angles
    public void SQ() {
        if (speed > 0) {
            if (newBeat) {
                deQ();
                newBeat = false;
                int a = floor(indexSeq / 2);
                switch (seq[a]) {
                    case 2:
                        currentDir = 1 - dir;
                        break;
                    case 1:
                    case 0:
                        currentDir = dir;
                        break;
                }
            }
            if ((millis() - timeMS) > speed) {
                if (currentSteps >= steps) {
                    currentDir = 1 - currentDir;
                    currentSteps = 0;
                    indexSeq++;
                    if ((indexSeq % 2) == 0)
                        newBeat = true;
                    int a = floor(indexSeq / 2);
                    if (a >= lengthSeq)
                        indexSeq = 0;
                } else {
                    int a = floor(indexSeq / 2);
                    currentSteps++;
                    if (seq[a] > 0)
                        stepperStep();
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public void initWA() {
        pause = 1000;
        isPaused = false;
    }

    public void setWA(int v) {
        isPaused = false;
        v = (v < 0) ? 1000 : v;
        pause = v;
        pause = v;
        mode = MODE_WA;
        timeMS = millis();
    }

    public void setNextMode(int m) {

    }
    public void SS(int v) {

    }
    public void GI(int v) {

    }

    public void deQ() {
        switch (modesQ[0]) {
            case MODE_IDLE:
                break;
            case MODE_ST:
                mode = modesQ[0];
                break;
            case MODE_RO:
                setRO(valuesQ[0]);
                break;
            case MODE_RP:
                setRP(valuesQ[0]);
                break;
            case MODE_RA:
            case MODE_RR:
                setRA(valuesQ[0]);
                break;
            case MODE_RW:
                setRW(valuesQ[0]);
                break;
            case MODE_SQ:
                setSQ(valuesQ[0]);
                break;
            case MODE_SD:
                setSD(valuesQ[0]);
                break;
            case MODE_WA:
                setWA(valuesQ[0]);
                break;
        }
        if (modesQ[0] != MODE_IDLE) {
            for (int i = 1; i < MAX_QUEUE; i++) {
                modesQ[i - 1] = modesQ[i];
                valuesQ[i - 1] = valuesQ[i];
            }
            modesQ[MAX_QUEUE - 1] = MODE_IDLE;
            valuesQ[MAX_QUEUE - 1] = -1;
            sizeQ--;
            sizeQ = (sizeQ < 0) ? 0 : sizeQ;
        }
    }

    public void GS() {

    }

    public void VA() {

    }
    public void GM() {

    }

    public void GD() {

    }

    public void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
    }
}
  public void settings() { 	size(1000, 600); }
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "SEALCGUI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
