/**
 * GUI for SEALC
 * TODO:
 * - add time compensation when speed (in ms) is lower than frame duration.
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
int[] commandsList = new int[MAX_QUEUE];
int iCommandsList;
PFont myFont;
String myPortName;
int iPort;

void setup() {
	size(1200, 800, P3D);
	frameRate(1000);
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

void draw() {
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
					state = STATE_RUNNING;
				}
			}
			break;
		case STATE_RUNNING:
			writeTextBox(textBoxColor);
			readTextBox();
			historyBox();
			displayHelp();
			for (int i = 0; i < nMotors; i++) {
				motors[i].display();
				motors[i].action();
			}
			break;
	}
}

void backspace() {
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
			if (l1 == SEPARATOR) iCommand = 2;
		}
		myText = myText.substring(0, myText.length() - 1);
	}
}

void delete() {
	myText = "";
	command[0] = 0;
	command[1] = 0;
	iCommand = 0;
	iCommandsList = 0;
}

void enter() {
	command[0] = 0;
	command[1] = 0;
	iCommand = 0;
	myText += '\n';
	history += myText;
	sendText();
	for (int i = 0; i < myText.length(); i++)
		processCommand(myText.charAt(i));
	myText = "";
	iCommandsList = 0;
}

void displayHelp() {
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
		rect(offsetX + textWidth(myText), offsetY + textLead * 1.5, textWidth(s) * 1.25, t * l);
		fill(255);
		text(s, offsetX + textWidth(myText) + offsetText, offsetY + offsetText * 2 + textLead * 1.5);
	}
	textFont(myFont, textSize);
}

void addCommandList(int c) {
	if (iCommandsList >= MAX_QUEUE) {
		iCommandsList--;
		for (int i = 0; i < iCommandsList; i++)
			commandsList[i] = commandsList[i + 1];
	}
	commandsList[iCommandsList++] = c;
	myText += command[1];
}

void processKeys(char k) {
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
							addCommandList(COMMAND_SS);
							break;
						case 'T':
							addCommandList(COMMAND_ST);
							break;
						case 'D':
							addCommandList(COMMAND_SD);
							break;
						case 'Q':
							addCommandList(COMMAND_SQ);
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
							addCommandList(COMMAND_RO);
							break;
						case 'A':
							addCommandList(COMMAND_RA);
							break;
						case 'P':
							addCommandList(COMMAND_RP);
							break;
						case 'W':
							addCommandList(COMMAND_RW);
							break;
						case 'R':
							addCommandList(COMMAND_RR);
							break;
						default:
							iCommand = 1;
							command[1] = 0;
							break;
					}
					break;
				case 'W':
					if (command[1] == 'A') {
						addCommandList(COMMAND_WA);
					} else {
						iCommand = 1;
						command[1] = 0;
					}
					break;
				case 'G':
					switch (command[1]) {
						case 'S':
							addCommandList(COMMAND_GS);
							break;
						case 'M':
							addCommandList(COMMAND_GM);
							break;
						case 'I':
							addCommandList(COMMAND_GI);
							break;
						case 'D':
							addCommandList(COMMAND_GD);
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

void keyPressed() {
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
					switch (keyCode) {
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

void writeTitle(String s) {
	fill(175);
	textFont(myFont, textSize - 2);
	text(s, textBoxWidth - textWidth(s), -textLead * .75);
	textFont(myFont, textSize);
}

void writeTextBox(color c) {
	fill(c);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY);
	rect(0, 0, textBoxWidth, textLead * 1.5);
	textAlign(LEFT, CENTER);
	for (int i = 0; i < myText.length(); i++) {
		if ((myText.charAt(i) >= 48) && (myText.charAt(i) < 58))
			fill(textColor);
		else if ((myText.charAt(i) >= 65) && (myText.charAt(i) < 91))
			fill(color(255, 0, 0));
		else
			fill(color(0, 255, 0));
		int w = int(textWidth(myText.substring(0, i)));
		text(myText.substring(i, i + 1), offsetText + w, 5, textBoxWidth, textLead);
	}
	textAlign(LEFT, TOP);
	writeTitle("COMMANDS");
	popMatrix();
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
	translate(offsetX, offsetY + 70);
	rect(0, 0, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	inBuffer = scrollText(inBuffer);
	text(inBuffer, offsetText, offsetText);
	writeTitle("SERIAL MONITOR");
	popMatrix();
}

void historyBox() {
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

String scrollText(String s) {
	int nLines = 0;
	for (int i = 0; i < s.length(); i++)
		if (s.charAt(i) == '\n') nLines++;
	if (nLines * textLead > inTextBoxHeight)
		s = s.substring(s.indexOf('\n') + 1);
	return s;
}

void selectPort() {
	fill(textBoxColor);
	noStroke();
	pushMatrix();
	translate(offsetX, offsetY);
	rect(0, 0, textBoxWidth, textLead * 1.5);
	fill(textColor);
	textAlign(LEFT, CENTER);
	String portsListString = "Port: [" + iPort + "] ";
	portsListString += portsList[iPort];
	portsListString += "\n";
	text(portsListString, offsetText, offsetText, textBoxWidth, textLead * 1.5);
	popMatrix();
	textAlign(LEFT, TOP);
}

void sendSetup() {
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
					switch (int(args[0])) {
						case 0:
							motors[n - 3] = new Stepper(int(args[1]), n - 3);
							break;
						case 1:
							motors[n - 3] = new Servo(int(args[2]), int(args[3]), n - 3);
							break;
						case 2:
							motors[n - 3] = new Vibro(n - 3);
							break;
					}
					motors[n - 3].setGraphics(textBoxWidth + 100 + ((n - 3) % 4) * 5 * motorSize, motorSize * 2 + motorSize * 8 * floor((n - 3) / 4.0), motorSize);
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