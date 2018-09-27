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
 * TODO:
 * - add time compensation when speed (in ms) is lower than frame duration.
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
// commands
static final int COMMAND_SELECT = 25;
static final int COMMAND_SS = 10; // Set Speed
static final int COMMAND_SD = MODE_SD; // Set Direction
static final int COMMAND_RO = MODE_RO; // ROtate
static final int COMMAND_ST = MODE_ST; // STop
static final int COMMAND_RA = MODE_RA; // Rotate Angle (stepper) / Rotate Absolute (servo)
static final int COMMAND_NONE = MODE_IDLE;
static final int COMMAND_RW = MODE_RW; // Rotate Wave
static final int COMMAND_SQ = MODE_SQ; // SeQuence
static final int COMMAND_ERROR = 66;
static final int COMMAND_RP = MODE_RP; // Rotate Pause
static final int COMMAND_GS = 20; // Get Speed
static final int COMMAND_GD = 21; // Get Direction
static final int COMMAND_GM = 22; // Get Mode
static final int COMMAND_GI = 23; // Get Id
static final int COMMAND_SA = 24; // Stop All
static final int COMMAND_RR = MODE_RR; // Rotate Angle (stepper) / Rotate Relative (servo)
static final int COMMAND_WA = MODE_WA; // WAit command (ms)

static final int MAX_SEQ = 10; // max length of sequence for beat
static final int MAX_QUEUE = 10;

Serial myPort;
String myText, inBuffer, history;
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
int motorSize;
int[] commandsList = new int[MAX_QUEUE];
int iCommandsList;
PFont myFont;
String myPortName;
int iPort;
SecondApplet sa;


public void settings() {
	size(600, 800, P3D);
}

public void setup() {
	bgColor = color(40, 50, 50);
	textBoxColor = color(20, 20, 30);
	textColor = color(230);
	background(bgColor);
	inBuffer = "";
	textAlign(LEFT, TOP);
	state = STATE_SELECT;
	portsList = Serial.list();
	nPorts = portsList.length;
	offsetX = 5;
	offsetY = 20;
	offsetText = 2;
	textBoxWidth = 500;
	myFont = loadFont("Arial-BoldMT-16.vlw");
	textSize = 16;
	textFont(myFont, textSize);
	textLead = textSize + 4;
	textLeading(textLead);
	strokeWeight(2);
	inTextBoxHeight = 300;
	delete();
	firstChar = true;
	currentValue = -1;
	selectedMotor = 0;
	currentCommand = COMMAND_NONE;
	motorSize = 30;
	history = "";
	for (int i = 0; i < MAX_QUEUE; i++)
		commandsList[i] = COMMAND_NONE;
	iCommandsList = 0;
	myPortName = "";
	iPort = 0;
}

public void draw() {
	background(bgColor);
	switch (state) {
		case STATE_SELECT:
			selectPort();
			break;
		case STATE_CONNECT:
			text("Connecting to " + myPortName + " ...", 20, 20);
			while (myPort.available() > 0)
				inBuffer += myPort.readString();
			if (inBuffer.length() > 0) {
				if (inBuffer.charAt(inBuffer.length() - 1) == '<') {
					sendSetup();
					sa = new SecondApplet();
					state = STATE_RUNNING;
				} else
					state = STATE_SELECT;
			}
			break;
		case STATE_RUNNING:
			writeTextBox(textBoxColor);
			readTextBox();
			historyBox();
			displayHelp();
			for (int i = 0; i < nMotors; i++) {
				//motors[i].display();
				motors[i].action();
			}
			break;
	}
}

public void backspace() {
	if (myText.length() > 0) {
		if (iCommandsList > 0) {
			char l1 = myText.charAt(myText.length() - 1);
			char l2 = myText.charAt(myText.length() - 2);
			if ((l1 >= 65) && (l1 < 91)) {
				iCommandsList--;
				commandsList[iCommandsList] = COMMAND_NONE;
				iCommand = 1;
				command[0] = l2;
				command[1] = 0;
			}
		}
		myText = myText.substring(0, myText.length() - 1);
	}
}

public void delete() {
	myText = "";
	command[0] = 0;
	command[1] = 0;
	iCommand = 0;
}

public void enter() {
	command[0] = 0;
	command[1] = 0;
	iCommand = 0;
	myText += '\n';
	history += myText;
	sendText();
	for (int i = 0; i < myText.length(); i++)
		processCommand(myText.charAt(i));
	myText = "";
}

public void displayHelp() {
	String s = "";
	int l = 0;
	textFont(myFont, textSize - 2);
	int t = textSize + 2;
	switch (iCommand) {
		case 0:
			break;
		case 1:
			switch (command[0]) {
				case 'S':
					s = "SS\nSD\nSQ\nST";
					l = 4;
					break;
				case 'R':
					s = "RA\nRO\nRP\nRR\nRW";
					l = 5;
					break;
				case 'G':
					s = "GI\nGD\nGM\nGS";
					l = 4;
					break;
				case 'W':
					s = "WA";
					l = 1;
					break;
				default:
					break;
			}
			break;
		case 2:
			l = 1;
			switch (commandsList[iCommandsList - 1]) {
				case COMMAND_RO:
					s = "RO  rotate [0=cont./>0=turns|ms]";
					break;
				case COMMAND_RA:
					s = "RA  rotate absolute [angle]";
					break;
				case COMMAND_RP:
					s = "RP  rotate pause [turns|ms]:[pause ms]";
					break;
				case COMMAND_RR:
					s = "RR  rotate relative [angle]";
					break;
				case COMMAND_RW:
					s = "RW  rotate wave [waves/turn]";
					break;
				case COMMAND_SD:
					s = "SD  set dir [0=CW/1=CCW]";
					break;
				case COMMAND_SQ:
					s = "SQ  sequence [angle]:[0=off/1=dir/2=-dir]:[0-2]:~\n";
					s += "SQ  sequence (vibro) [ms]:[0=off/1=on]:[ms]:[0-1]:~";
					l = 2;
					break;
				case COMMAND_SS:
					s = "SS  set speed [RPM]";
					break;
				case COMMAND_ST:
					s = "ST  stop";
					break;
				case COMMAND_WA:
					s = "WA  wait [ms]";
					break;
				case COMMAND_GD:
					s = "GD  get dir";
					break;
				case COMMAND_GI:
					s = "GI  get index/type";
					break;
				case COMMAND_GM:
					s = "GM  get mode";
					break;
				case COMMAND_GS:
					s = "GS  get speed";
					break;
			}
			break;
	}
	if (s.length() > 0) {
		fill(50, 50, 50);
		stroke(200);
		rect(offsetX + textWidth(myText), offsetY + textLead * 1.5f, textWidth(s) * 1.25f, t * l);
		fill(255);
		text(s, offsetX + textWidth(myText) + offsetText, offsetY + offsetText * 2 + textLead * 1.5f);
	}
	textFont(myFont, textSize);
}

public void processKeys(char k) {
	if ((k >= 97) && (k < 123))
		k -= 32;
	switch (iCommand) {
		case 0:
			if ((k >= 48) && (k < 58)) {
				myText += k;
			} else {
				switch (k) {
					case BACKSPACE:
						backspace();
						break;
					case DELETE:
						delete();
						break;
					case ENTER:
					case RETURN:
						enter();
						break;
					case 'S':
					case 'R':
					case 'G':
					case 'W':
						myText += k;
						command[0] = k;
						iCommand = 1;
						break;
					case SEPARATOR:
						myText += k;
						break;
					default:
						break;
				}
			}
			break;
		case 1:
			switch (k) {
				case BACKSPACE:
					if (myText.length() > 0)
						myText = myText.substring(0, myText.length() - 1);
					iCommand = 0;
					command[0] = 0;
					break;
				case DELETE:
					delete();
					break;
				case 'S':
				case 'R':
				case 'O':
				case 'A':
				case 'W':
				case 'Q':
				case 'D':
				case 'T':
				case 'P':
				case 'M':
				case 'I':
					iCommand = 2;
					command[1] = k;
					break;
				default:
					break;
			}
			switch (command[0]) {
				case 0:
					break;
				case 'S':
					switch (command[1]) {
						case 'S':
							commandsList[iCommandsList++] = COMMAND_SS;
							myText += command[1];
							break;
						case 'T':
							commandsList[iCommandsList++] = COMMAND_ST;
							myText += command[1];
							break;
						case 'D':
							commandsList[iCommandsList++] = COMMAND_SD;
							myText += command[1];
							break;
						case 'Q':
							commandsList[iCommandsList++] = COMMAND_SQ;
							myText += command[1];
							break;
						default:
							iCommand = 1;
							command[1] = 0;
							break;
					}
					break;
				case 'R':
					switch (command[1]) {
						case 'O':
							commandsList[iCommandsList++] = COMMAND_RO;
							myText += command[1];
							break;
						case 'A':
							commandsList[iCommandsList++] = COMMAND_RA;
							myText += command[1];
							break;
						case 'P':
							commandsList[iCommandsList++] = COMMAND_RP;
							myText += command[1];
							break;
						case 'W':
							commandsList[iCommandsList++] = COMMAND_RW;
							myText += command[1];
							break;
						case 'R':
							commandsList[iCommandsList++] = COMMAND_RR;
							myText += command[1];
							break;
						default:
							iCommand = 1;
							command[1] = 0;
							break;
					}
					break;
				case 'W':
					if (command[1] == 'A') {
						commandsList[iCommandsList++] = COMMAND_WA;
						myText += command[1];
					} else {
						iCommand = 1;
						command[1] = 0;
					}
					break;
				case 'G':
					switch (command[1]) {
						case 'S':
							commandsList[iCommandsList++] = COMMAND_GS;
							myText += command[1];
							break;
						case 'M':
							commandsList[iCommandsList++] = COMMAND_GM;
							myText += command[1];
							break;
						case 'I':
							commandsList[iCommandsList++] = COMMAND_GI;
							myText += command[1];
							break;
						case 'D':
							commandsList[iCommandsList++] = COMMAND_GD;
							myText += command[1];
							break;
						default:
							iCommand = 1;
							command[1] = 0;
							break;
					}
					break;
			}
			break;
		case 2:
			if ((k >= 48) && (k < 58))
				myText += k;
			else {
				switch (k) {
					case ENTER:
					case RETURN:
						enter();
						break;
					case SEPARATOR:
						myText += k;
						command[0] = 0;
						command[1] = 0;
						iCommand = 0;
						break;
					case COLUMN:
						if (iCommandsList > 0) {
							switch (commandsList[iCommandsList - 1]) {
								case COMMAND_SQ:
								case COMMAND_RP:
									myText += k;
									break;
							}
						}
						break;
					case BACKSPACE:
						backspace();
						break;
					case DELETE:
						delete();
						break;
					default:
						break;
				}
				break;
			}
			break;
	}
}

public void keyPressed() {
	switch (state) {
		case STATE_SELECT:
			switch (key) {
				case RETURN:
				case ENTER:
					myPort = new Serial(this, portsList[iPort], 115200);
					myPortName = portsList[iPort];
					state = STATE_CONNECT;
					break;
				case CODED:
					switch (key) {
						case LEFT:
							iPort--;
							if (iPort < 0) iPort = nPorts - 1;
							break;
						case RIGHT:
							iPort++;
							if (iPort >= nPorts) iPort = 0;
							break;
					}
					break;
			}
			break;
		case STATE_RUNNING:
			processKeys(key);
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
					case COMMAND_RO:
					case COMMAND_ST:
					case COMMAND_RA:
					case COMMAND_RR:
					case COMMAND_RW:
					case COMMAND_SQ:
					case COMMAND_RP:
					case COMMAND_WA:
						motors[selectedMotor].fillQ(currentCommand, currentValue);
						break;
					case COMMAND_SA:
						for (int i = 0; i < nMotors; i++)
							motors[i].ST();
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
			case 'S':
				switch (command[1]) {
					case 'S':
						currentCommand = COMMAND_SS; //SS
						break;
					case 'D':
						currentCommand = COMMAND_SD; //SD
						break;
					case 'T':
						currentCommand = COMMAND_ST; //ST
						break;
					case 'A':
						currentCommand = COMMAND_SA; //ST
						break;
					case 'Q':
						currentCommand = COMMAND_SQ; //SQ
						motors[selectedMotor].initSQ();
						break;
				}
				break;
			case 'R':
				switch (command[1]) {
					case 'O':
						currentCommand = COMMAND_RO; //RO
						break;
					case 'W':
						currentCommand = COMMAND_RW; //RW
						break;
					case 'A':
						currentCommand = COMMAND_RA; //RA
						break;
					case 'R':
						currentCommand = COMMAND_RR; //RA
						break;
					case 'P':
						currentCommand = COMMAND_RP; //RP
						break;
				}
				break;
			case 'W':
				switch (command[1]) {
					case 'A':
						currentCommand = COMMAND_WA; //WA
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

public void writeTitle(String s) {
	fill(175);
	textFont(myFont, textSize - 2);
	text(s, textBoxWidth - textWidth(s), -textLead * .75f);
	textFont(myFont, textSize);
}

public void writeTextBox(int c) {
	fill(c);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY);
	rect(0, 0, textBoxWidth, textLead * 1.5f);
	textAlign(LEFT, CENTER);
	for (int i = 0; i < myText.length(); i++) {
		if ((myText.charAt(i) >= 48) && (myText.charAt(i) < 58))
			fill(textColor);
		else if ((myText.charAt(i) >= 65) && (myText.charAt(i) < 91))
			fill(color(255, 0, 0));
		else
			fill(color(0, 255, 0));
		int w = PApplet.parseInt(textWidth(myText.substring(0, i)));
		text(myText.substring(i, i + 1), offsetText + w, 5, textBoxWidth, textLead);
	}
	textAlign(LEFT, TOP);
	writeTitle("COMMANDS");
	popMatrix();
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
	pushMatrix();
	translate(offsetX, offsetY + 70);
	rect(0, 0, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	inBuffer = scrollText(inBuffer);
	text(inBuffer, offsetText, offsetText);
	writeTitle("SERIAL MONITOR");
	popMatrix();
}

public void historyBox() {
	fill(textBoxColor);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY + 105 + inTextBoxHeight);
	rect(0, 0, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	history = scrollText(history);
	text(history, offsetText, offsetText);
	writeTitle("HISTORY");
	popMatrix();
}

public String scrollText(String s) {
	int nLines = 0;
	for (int i = 0; i < s.length(); i++)
		if (s.charAt(i) == '\n') nLines++;
	if (nLines * textLead > inTextBoxHeight)
		s = s.substring(s.indexOf('\n') + 1);
	return s;
}

public void selectPort() {
	fill(textBoxColor);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY);
	rect(0, 0, textBoxWidth, textLead * 1.5f);
	fill(textColor);
	textAlign(LEFT, CENTER);
	String portsListString = "Port: [" + iPort + "] ";
	portsListString += portsList[iPort];
	portsListString += "\n";
	text(portsListString, offsetText, offsetText, textBoxWidth, textLead * 1.5f);
	popMatrix();
	textAlign(LEFT, TOP);
}

public void sendSetup() {
	BufferedReader reader = createReader("setup.txt");
	String line = null;
	int n = 0;
	try {
		while ((line = reader.readLine()) != null) {
			switch (n) {
				case 0:
				case 2:
					break;
				case 1:
					nMotors = line.charAt(0) - 48;
					motors = new Motor[nMotors];
					myPort.write(line + '\n');
					break;
				default:
					String[] args = line.split(",");
					switch (PApplet.parseInt(args[0])) {
						case 0:
							motors[n - 3] = new Stepper(PApplet.parseInt(args[1]), n - 3);
							break;
						case 1:
							motors[n - 3] = new Servo(PApplet.parseInt(args[2]), PApplet.parseInt(args[3]), n - 3);
							break;
						case 2:
							motors[n - 3] = new Vibro(n - 3);
							break;
					}
					motors[n - 3].setGraphics(motorSize + offsetX + ((n - 3) % 4) * 5 * motorSize, motorSize * 2 + motorSize * 8 * floor((n - 3) / 4.0f), motorSize);
					myPort.write(line + '\n');
					break;
			}
			n++;
		}
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	motors[selectedMotor].setSelected(true);
}

class SecondApplet extends PApplet {

	SecondApplet() {
		super();
		PApplet.runSketch(new String[] {
			this.getClass().getSimpleName()
		}, this);
	}

	public void settings() {
		size(600, 800, P3D);
	}

	public void setup() {
		frameRate(1000);
		textFont(myFont, textSize);
		textLeading(textLead);
		strokeWeight(2);
		textAlign(LEFT, TOP);
	}

	public void draw() {
		background(bgColor);
		for (int i = 0; i < nMotors; i++)
			motors[i].display();
	}
}
interface Motor {
    public void setSelected(boolean s);
    public void columnSQ(int v);
    public void setGraphics(int x, int y, int r);
    public void display();
    public void SS(int v);
    public void initSQ();
    public void columnRP(int v);
    public void ST();
    public void action();
    public String getType();
    public void fillQ(int m, int v);
}
class Servo implements Motor {
    int angleMin, angleMax;
    int angle; // current angle
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int[] currentSeq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int steps; // for move/hammer
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int indexSeq; // current position in sequence
    int currentIndexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    int currentLengthSeq; // length of seq.
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

    Servo(int amin, int amax, int i) {
        id = i;
        angleMin = amin;
        angleMax = amax;
        angle = angleMin;
        nSteps = 360;
        for (int j = 0; j < MAX_SEQ; j++) {
            seq[j] = 0;
            currentSeq[j] = 0;
        }
        angleSeq = 0;
        mode = MODE_IDLE;
        currentSteps = 0;
        steps = 0;
        dir = 0;
        currentDir = dir;
        for (int j = 0; j < MAX_QUEUE; j++) {
            modesQ[j] = MODE_IDLE;
            valuesQ[j] = -1;
        }
        sizeQ = 0;
        speedRPM = 12;
        speed = floor(60000.0f / (speedRPM * nSteps));
        indexSeq = 0;
        lengthSeq = 0;
        pause = 1000;
        isPaused = false;
        currentIndexSeq = 0;
        currentLengthSeq = 0;
        timeMS = millis();
    }

    public void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    public void display() {
        sa.noFill();
        if (selected)
            sa.stroke(255, 0, 0);
        else sa.stroke(255);
        sa.pushMatrix();
        sa.translate(xPos, yPos);
        sa.rotateZ(radians(angle));
        sa.ellipse(0, 0, 2 * radius, 2 * radius);
        sa.line(0, -radius, 0, radius);
        sa.triangle(0, -radius - 10, -5, -radius, 5, -radius);
        sa.popMatrix();
        sa.pushMatrix();
        sa.translate(xPos - radius, yPos + 2 * radius);
        if (selected)
            sa.fill(255, 0, 0);
        else
            sa.fill(255);
        String s = id + getType() + "\n";
        s += "Speed: " + speedRPM + "RPM\n";
        s += "Dir: " + ((dir > 0) ? "CCW" : "CW") + "\n";
        s += "Mode: ";
        switch (mode) {
            case MODE_ST:
                s += "ST";
                break;
            case MODE_RO:
                s += "RO";
                break;
            case MODE_RA:
                s += "RA";
                break;
            case MODE_RR:
                s += "RR";
                break;
            case MODE_WA:
                s += "WA";
                break;
            case MODE_RW:
                s += "RW";
                break;
            case MODE_RP:
                s += "RP";
                break;
            case MODE_SQ:
                s += "SQ";
                break;
            case MODE_SD:
                s += "SD";
                break;
            case MODE_IDLE:
                s += "IDLE";
                break;
        }
        s += "\nAngle: " + angle;
        sa.text(s, 0, 0);
        sa.popMatrix();
    }

    public void SS(int v) {
        speedRPM = (v > 60000.0f / nSteps) ? floor(60000.0f / nSteps) : v;
        speed = (speedRPM > 0) ? (floor(60000.0f / (speedRPM * nSteps))) : 0;
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
        mode = MODE_IDLE;
    }

    public void columnRP(int v) {}

    public void setRP(int v) {
        mode = MODE_IDLE;
    }

    public void setRR(int v) {
        currentDir = dir;
        steps = (v <= 0) ? 0 : (v % (angleMax - angleMin));
        currentSteps = 0;
        mode = MODE_RR;
        timeMS = millis();
    }

    public void setRA(int v) {
        v = (v < angleMin) ? angleMin : ((v > angleMax) ? angleMax : v);
        if (v >= angle) {
            v -= angle;
            currentDir = 0;
        } else {
            v = angle - v;
            currentDir = 1;
        }
        steps = v;
        currentSteps = 0;
        mode = MODE_RA;
        timeMS = millis();
    }

    public void setRW(int v) {
        mode = MODE_IDLE;
    }

    public void initSQ() {
        indexSeq = 0;
        lengthSeq = 0;
    }

    public void columnSQ(int v) {
        v = (v <= 0) ? 0 : v;
        if (angleSeq == 0)
            angleSeq = v;
        else
            seq[indexSeq++] = v;
    }

    public void setSQ(int v) {
        currentDir = dir;
        newBeat = true;
        if (angleSeq == 0) {
            angleSeq = v;
            indexSeq = 0;
            seq[indexSeq] = 1;
            lengthSeq = 1;
        } else {
            seq[indexSeq++] = v;
            lengthSeq = indexSeq;
        }
        currentLengthSeq = lengthSeq;
        for (int i = 0; i < currentLengthSeq; i++)
            currentSeq[i] = seq[i];
        indexSeq = 0;
        currentIndexSeq = 0;
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
        if (currentSteps >= steps)
            ST();
        else {
            currentSteps++;
            servoStep();
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
            case MODE_RW:
            case MODE_RO:
            case MODE_RP:
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
        mode = MODE_IDLE;
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
        } else
            ST();
    }

    // continuous hammer movement with pattern of angles
    public void SQ() {
        if (speed > 0) {
            if (newBeat) {
                deQ();
                newBeat = false;
                int a = floor(currentIndexSeq / 2);
                currentDir = (currentSeq[a] < 2) ? dir : (1 - dir);
            }
            if ((millis() - timeMS) > speed) {
                int a = floor(currentIndexSeq / 2);
                if (currentSteps >= steps) {
                    currentIndexSeq++;
                    currentSteps = 0;
                    indexSeq++;
                    if (a >= currentLengthSeq)
                        currentIndexSeq = 0;
                    if ((currentIndexSeq % 2) == 0)
                        newBeat = true;
                    else
                        currentDir = 1 - currentDir;
                } else {
                    currentSteps++;
                    if (currentSeq[a] > 0)
                        servoStep();
                }
                timeMS = millis();
            }
        } else
            ST();
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

    public void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
    }

    public void setSelected(boolean s) {
        selected = s;
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
                setRA(valuesQ[0]);
                break;
            case MODE_RR:
                setRR(valuesQ[0]);
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
}
class Stepper implements Motor {
    int waveDir; // increasing / decreasing speed
    int turns; // for rotate (0=continuous rotation)
    int realSteps;
    int absoluteSteps;
    int absoluteStepsIdle;
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int[] currentSeq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int steps; // for move/hammer
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int currentIndexSeq; // current position in sequence
    int indexSeq; // current position in sequence
    int currentLengthSeq; // length of seq.
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
    int inc;

    Stepper(int n, int i) {
        id = i;
        waveDir = 0;
        nSteps = n;
        currentSteps = 0;
        realSteps = currentSteps;
        absoluteSteps = currentSteps;
        absoluteStepsIdle = absoluteSteps;
        for (int j = 0; j < MAX_SEQ; j++) {
            seq[j] = 0;
            currentSeq[j] = 0;
        }
        angleSeq = 0;
        speedRPM = 12;
        speed = floor(60000.0f / (speedRPM * nSteps));
        inc = getInc();
        mode = MODE_IDLE;
        steps = 0;
        dir = 0;
        currentDir = dir;
        for (int j = 0; j < MAX_QUEUE; j++) {
            modesQ[j] = MODE_IDLE;
            valuesQ[j] = -1;
        }
        sizeQ = 0;
        indexSeq = 0;
        lengthSeq = 0;
        pause = 1000;
        isPaused = false;
        currentIndexSeq = 0;
        currentLengthSeq = 0;
        timeMS = millis();
    }

    public void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    public void display() {
        sa.noFill();
        if (selected)
            sa.stroke(255, 0, 0);
        else
            sa.stroke(255);
        sa.pushMatrix();
        sa.translate(xPos, yPos);
        sa.rotateZ(TWO_PI * absoluteSteps / nSteps);
        sa.ellipse(0, 0, 2 * radius, 2 * radius);
        sa.line(0, -radius, 0, radius);
        sa.line(0 - radius, 0, radius, 0);
        sa.triangle(0, -radius - 10, -5, -radius, 5, -radius);
        sa.popMatrix();
        sa.pushMatrix();
        sa.translate(xPos - radius, yPos + 2 * radius);
        if (selected)
            sa.fill(255, 0, 0);
        else
            sa.fill(255);
        String s = id + getType() + "\n";
        s += "Speed: " + speedRPM + "RPM\n";
        s += "Dir: " + ((dir > 0) ? "CCW" : "CW") + "\n";
        s += "Mode: ";
        switch (mode) {
            case MODE_ST:
                s += "ST";
                break;
            case MODE_RO:
                s += "RO";
                break;
            case MODE_RA:
                s += "RA";
                break;
            case MODE_RR:
                s += "RR";
                break;
            case MODE_WA:
                s += "WA";
                break;
            case MODE_RW:
                s += "RW";
                break;
            case MODE_RP:
                s += "RP";
                break;
            case MODE_SQ:
                s += "SQ";
                break;
            case MODE_SD:
                s += "SD";
                break;
            case MODE_IDLE:
                s += "IDLE";
                break;
        }
        s += "\nAngle: " + absoluteSteps * 360.0f / nSteps;
        sa.text(s, 0, 0);
        sa.popMatrix();
    }

    public String getType() {
        return " (stepper)";
    }

    public void SS(int v) {
        speedRPM = (v > 60000.0f / nSteps) ? floor(60000.0f / nSteps) : v;
        speed = (speedRPM > 0) ? (floor(60000.0f / (speedRPM * nSteps))) : 0;
        inc = getInc();
    }

    public void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    public void setRO(int v) {
        turns = (v <= 0) ? 0 : v;
        steps = turns * nSteps;
        mode = MODE_RO;
        timeMS = millis();
    }

    public void columnRP(int v) {
        turns = (v <= 0) ? 1 : v;
    }

    public void setRP(int v) {
        isPaused = false;
        pause = (v <= 0) ? 1000 : v;
        turns = (turns <= 0) ? 1 : turns;
        currentSteps = 0;
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
        indexSeq = 0;
        lengthSeq = 0;
    }

    public void columnSQ(int v) {
        v = (v <= 0) ? 0 : v;
        if (angleSeq == 0)
            angleSeq = v;
        else
            seq[indexSeq++] = v;
    }

    public void setSQ(int v) {
        v = (v <= 0) ? 0 : v;
        newBeat = true;
        if (angleSeq == 0) {
            angleSeq = v;
            indexSeq = 0;
            seq[indexSeq] = 1;
            lengthSeq = 1;
        } else {
            seq[indexSeq++] = v;
            lengthSeq = indexSeq;
        }
        currentLengthSeq = lengthSeq;
        for (int i = 0; i < currentLengthSeq; i++)
            currentSeq[i] = seq[i];
        angleSeq = PApplet.parseInt(angleSeq / 360.0f * nSteps);
        currentIndexSeq = 0;
        steps = angleSeq;
        angleSeq = 0;
        currentSteps = 0;
        mode = MODE_SQ;
        timeMS = millis();
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public void setWA(int v) {
        isPaused = false;
        pause = (v < 0) ? 1000 : v;
        mode = MODE_WA;
        timeMS = millis();
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
        if (speed > 0) {
            if ((millis() - timeMS) >= speed) {
                int iturns = floor(PApplet.parseFloat(currentSteps) / nSteps) + 1;
                if (turns == 0) {
                    absoluteStepsDir();
                    if (currentSteps >= nSteps * iturns) {
                        currentSteps %= nSteps;
                        deQ();
                    }
                } else {
                    if (currentSteps >= steps)
                        ST();
                    else {
                        absoluteStepsDir();
                        if (currentSteps >= nSteps * iturns)
                            deQ();
                    }

                }
                timeMS = millis();
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
                        absoluteSteps = absoluteStepsIdle;
                        deQ();
                    } else {
                        int iturns = floor(PApplet.parseFloat(currentSteps) / nSteps) + 1;
                        absoluteStepsDir();
                        if (currentSteps >= nSteps * iturns)
                            deQ();
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
                if (currentSteps >= steps) {
                    ST();
                    absoluteStepsIdle = steps;
                    absoluteSteps = absoluteStepsIdle;
                } else
                    absoluteStepsDir();
                timeMS = millis();
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
                    realSteps += inc;
                    absoluteStepsDir();
                    if (realSteps >= nSteps) {
                        realSteps %= nSteps;
                        deQ();
                    }
                }
                timeMS = millis();
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

    // continuous hammer movement with pattern of angles
    public void SQ() {
        if (speed > 0) {
            if (newBeat) {
                newBeat = false;
                int a = floor(currentIndexSeq / 2);
                switch (currentSeq[a]) {
                    case 2:
                        currentDir = 1 - dir;
                        break;
                    case 1:
                    case 0:
                        currentDir = dir;
                        break;
                }
                deQ();
            }
            if ((millis() - timeMS) > speed) {
                int a = floor(currentIndexSeq / 2);
                if (currentSteps >= steps) {
                    currentSteps = 0;
                    currentIndexSeq++;
                    if (a >= currentLengthSeq)
                        currentIndexSeq = 0;
                    if ((currentIndexSeq % 2) == 0)
                        newBeat = true;
                    else currentDir = 1 - currentDir;
                } else {
                    if (seq[a] > 0)
                        absoluteStepsDir();
                    else
                        currentSteps += inc;
                }
                timeMS = millis();
            }
        } else {
            ST();
        }
    }

    public void deQ() {
        switch (modesQ[0]) {
            case MODE_IDLE:
                break;
            case MODE_ST:
                absoluteSteps = absoluteStepsIdle;
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

    public void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
    }

}
class Vibro implements Motor {
    int duration;
    boolean isOn;
    int[] durationSeq = new int[MAX_SEQ];
    int[] stateSeq = new int[MAX_SEQ];
    int[] currentDurationSeq = new int[MAX_SEQ];
    int[] currentStateSeq = new int[MAX_SEQ];
    int id;
    int mode;
    int indexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    int currentIndexSeq; // current position in sequence
    int currentLengthSeq; // length of seq.
    long timeMS; // for speed
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

    Vibro(int i) {
        id = i;
        isOn = false;
        mode = MODE_IDLE;
        for (int j = 0; j < MAX_SEQ; j++) {
            durationSeq[j] = 0;
            stateSeq[j] = 0;
            currentDurationSeq[j] = 0;
            currentStateSeq[j] = 0;
        }
        for (int j = 0; j < MAX_QUEUE; j++) {
            modesQ[j] = MODE_IDLE;
            valuesQ[j] = -1;
        }
        sizeQ = 0;
        indexSeq = 0;
        lengthSeq = 0;
        currentIndexSeq = 0;
        currentLengthSeq = 0;
        duration = 0;
        pause = 1000;
        isPaused = false;
        timeMS = millis();
    }

    public void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    public void display() {
        sa.noFill();
        if (selected)
            sa.stroke(255, 0, 0);
        else sa.stroke(255);
        sa.pushMatrix();
        sa.translate(xPos, yPos);
        if (isOn)
            sa.ellipse(random(5), random(5), 2 * radius, 2 * radius);
        else
            sa.ellipse(0, 0, 2 * radius, 2 * radius);
        sa.popMatrix();
        sa.pushMatrix();
        sa.translate(xPos - radius, yPos + 2 * radius);
        if (selected)
            sa.fill(255, 0, 0);
        else
            sa.fill(255);
        String s = id + getType() + "\n";
        s += "Mode: ";
        switch (mode) {
            case MODE_ST:
                s += "ST";
                break;
            case MODE_RO:
                s += "RO";
                break;
            case MODE_RA:
                s += "RA";
                break;
            case MODE_RR:
                s += "RR";
                break;
            case MODE_WA:
                s += "WA";
                break;
            case MODE_RW:
                s += "RW";
                break;
            case MODE_RP:
                s += "RP";
                break;
            case MODE_SQ:
                s += "SQ";
                break;
            case MODE_SD:
                s += "SD";
                break;
            case MODE_IDLE:
                s += "IDLE";
                break;
        }
        sa.text(s, 0, 0);
        sa.popMatrix();
    }

    public String getType() {
        return " (vibro)";
    }

    public void setRO(int v) {
        duration = (v <= 0) ? 0 : v;
        mode = MODE_RO;
        timeMS = millis();
    }

    public void columnRP(int v) {
        duration = (v <= 0) ? 1000 : v;
    }

    public void setRP(int v) {
        isPaused = false;
        pause = (v <= 0) ? 1000 : v;
        duration = (duration <= 0) ? 1000 : duration;
        mode = MODE_RP;
        timeMS = millis();
    }

    public void initSQ() {
        indexSeq = 0;
        lengthSeq = 0;
        newBeat = true;
    }

    public void columnSQ(int v) {
        if (indexSeq % 2 == 0) {
            v = (v <= 0) ? 0 : v;
            durationSeq[indexSeq / 2] = v;
        } else {
            v = (v <= 0) ? 0 : 1;
            stateSeq[(indexSeq - 1) / 2] = v;
        }
        indexSeq++;
    }

    public void setSQ(int v) {
        v = (v <= 0) ? 0 : 1;
        newBeat = true;
        stateSeq[(indexSeq - 1) / 2] = v;
        indexSeq++;
        lengthSeq = indexSeq / 2;
        indexSeq = 0;
        currentLengthSeq = lengthSeq;
        for (int i = 0; i < currentLengthSeq; i++) {
            currentDurationSeq[i] = durationSeq[i];
            currentStateSeq[i] = stateSeq[i];
        }
        currentIndexSeq = 0;
        mode = MODE_SQ;
        timeMS = millis();
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
            case MODE_RA:
            case MODE_RR:
            case MODE_RW:
                break;
            case MODE_RO:
                RO();
                break;
            case MODE_RP:
                RP();
                break;
            case MODE_SQ:
                SQ();
                break;
            case MODE_WA:
                WA();
                break;
        }
    }

    // rotation
    public void RO() {
        if (!isOn)
            isOn = true;
        if (duration == 0)
            deQ();
        else {
            if ((millis() - timeMS) > duration)
                ST();
        }
    }

    // rotation with pause
    public void RP() {
        if (isPaused) {
            if ((millis() - timeMS) > pause) {
                isPaused = false;
                isOn = true;
                timeMS = millis();
            } else
                deQ();
        } else {
            if (!isOn)
                isOn = true;
            if ((millis() - timeMS) > duration) {
                isPaused = true;
                isOn = false;
                deQ();
                timeMS = millis();
            }
        }
    }

    public void ST() {
        isOn = false;
        mode = MODE_IDLE;
        deQ();
    }

    // continuous hammer movement with pattern of angles
    public void SQ() {
        if (newBeat) {
            deQ();
            isOn = currentStateSeq[currentIndexSeq] > 0;
            newBeat = false;
        }
        if ((millis() - timeMS) > currentDurationSeq[currentIndexSeq]) {
            newBeat = true;
            currentIndexSeq++;
            if (currentIndexSeq >= currentLengthSeq)
                currentIndexSeq = 0;
            timeMS = millis();
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
            isOn = false;
        }
    }

    public void setSelected(boolean s) {
        selected = s;
    }

    public void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
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
        mode = MODE_WA;
        timeMS = millis();
    }

    public void SS(int v) {}

    public void SD() {}

    public void RA() {}

    public void setRR(int v) {}

    public void setRA(int v) {}

    public void setRW(int v) {}

    public void setSD(int v) {}
}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "SEALCGUI" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}
