/**
 * GUI for SEALC
 *
 **/
import processing.serial.*;

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
String myText, inBuffer, history;
int state;
int nPorts;
String[] portsList;
color bgColor, textBoxColor, textColor;
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

void setup() {
	size(1200, 800, P3D);
	frameRate(1000);
	bgColor = color(40, 50, 50);
	textBoxColor = color(20, 20, 30);
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
	textBoxWidth = 500;
	PFont myFont = loadFont("Arial-BoldMT-16.vlw");
	textSize = 16;
	textFont(myFont, textSize);
	textLead = textSize + 4;
	textLeading(textLead);
	strokeWeight(2);
	inTextBoxHeight = 300;
	command[0] = 0;
	command[1] = 0;
	iCommand = 0;
	firstChar = true;
	currentValue = -1;
	selectedMotor = 0;
	currentCommand = COMMAND_NONE;
	motorSize = 30;
	history = "";
}

void draw() {
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
			historyBox();
			for (int i = 0; i < nMotors; i++) {
				motors[i].display();
				motors[i].action();
			}
			break;
	}
}

void keyPressed() {
	switch (state) {
		case STATE_SELECT:
			if ((int(key) >= 48) && (int(key) <= 48 + nPorts - 1)) {
				myPort = new Serial(this, portsList[int(key) - 48], 115200);
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
					history += myText;
					sendText();
					for (int i = 0; i < myText.length(); i++) {
						processCommand(myText.charAt(i));
					}
					myText = "";
					break;
				default:
					myText = myText + key;
					myText = myText.toUpperCase();
					break;
			}
			break;
	}
}

void updateValue(char a) {
	if (currentValue < 0)
		currentValue = 0;
	currentValue *= 10;
	currentValue += (a - 48);
}

void processCommand(char a) {
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
					case 'r':
					case 'R':
						currentCommand = COMMAND_RR; //RA
						break;
					case 'p':
					case 'P':
						currentCommand = COMMAND_RP; //RP
						//motors[selectedMotor]->initRP();
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

void writeTextBox(color c) {
	fill(c);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY);
	rect(0, 0, textBoxWidth, textLead * 1.5);
	textAlign(LEFT, CENTER);
	for (int i = 0; i < myText.length(); i++) {
		if ((myText.charAt(i) >= 48) && (myText.charAt(i) < 58)) {
			fill(textColor);
		} else if ((myText.charAt(i) >= 65) && (myText.charAt(i) < 91)) {
			fill(color(255, 0, 0));
		} else
			fill(color(0, 255, 0));
		int w = int(textWidth(myText.substring(0, i)));
		text(myText.substring(i, i + 1), offsetText + w, 5, textBoxWidth, textLead);
	}
	popMatrix();
	textAlign(LEFT, TOP);
}

void sendText() {
	writeTextBox(color(255, 0, 0));
	myPort.write(myText);
}

void readTextBox() {
	while (myPort.available() > 0)
		inBuffer += myPort.readString();
	fill(textBoxColor);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY + 50);
	rect(0, 0, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	inBuffer = scrollText(inBuffer);
	text(inBuffer, offsetText, offsetText, textBoxWidth, inTextBoxHeight);
	popMatrix();
}

void historyBox() {
	fill(textBoxColor);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY + 80 + inTextBoxHeight);
	rect(0, 0, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	history = scrollText(history);
	text(history, offsetText, offsetText, textBoxWidth, inTextBoxHeight);
	popMatrix();
}

String scrollText(String s) {
	int nLines = 0;
	for (int i = 0; i < s.length(); i++) {
		if (s.charAt(i) == '\n') nLines++;
	}
	if (nLines * textLead > inTextBoxHeight) {
		int a = s.indexOf('\n');
		s = s.substring(a + 1);
	}
	return s;
}

void selectPort() {
	fill(textBoxColor);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY);
	rect(0, 0, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	String portsListString = "Please select port:\n";
	for (int i = 0; i < nPorts; i++) {
		portsListString += "[" + i + "] ";
		portsListString += portsList[i];
		portsListString += "\n";
	}
	text(portsListString, offsetText, offsetText, textBoxWidth, inTextBoxHeight);
	popMatrix();
}

void sendSetup() {
	BufferedReader reader = createReader("setup.txt");
	String line = null;
	int n = 0;
	try {
		while ((line = reader.readLine()) != null) {
			if (n == 0) {
				nMotors = line.charAt(0) - 48;
				motors = new Motor[nMotors];
			} else {
				String[] args = line.split(",");
				switch (int(args[0])) {
					case 0:
						motors[n - 1] = new Stepper(int(args[1]), n - 1);
						break;
					case 1:
						motors[n - 1] = new Servo(int(args[2]), int(args[3]), n - 1);
						break;
					case 2:
						motors[n - 1] = new Vibro(n - 1);
						break;
				}
				motors[n - 1].setGraphics(textBoxWidth + 100 + ((n - 1) % 4) * 5 * motorSize, motorSize * 2 + motorSize * 8 * floor((n - 1) / 4.0), motorSize);
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