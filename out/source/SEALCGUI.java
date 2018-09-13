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
					sendText();
					break;
				default:
					processCommand(key);
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
				motors[i].selected = false;
			motors[selectedMotor].selected = true;
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
	myPort.write(myText + "\n");
	myText = "";
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
				motors[n - 1] = new Motor(line.charAt(0) - 48);
				motors[n - 1].setGraphics(500 + (n - 1) * 50, 50, 20);
			}
			myPort.write(line + '\n');
			n++;
		}
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
	motors[selectedMotor].selected = true;
}
class Motor {
    int xPos, yPos;
    int radius;
    int type;
    boolean selected;
    Motor(int t) {
        type = t;
        xPos = 0;
        yPos = 0;
        radius = 0;
        selected = false;
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
        switch (type) {
            case TYPE_STEPPER:
                line(xPos, yPos - radius, xPos, yPos + radius);
                line(xPos - radius, yPos, xPos + radius, yPos);
                break;
            case TYPE_SERVO:
                line(xPos, yPos - radius, xPos, yPos + radius);
                break;
        }
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
