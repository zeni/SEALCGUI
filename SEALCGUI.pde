/**
 * GUI for SEALC
 *
 **/
import processing.serial.*;

static final int CONNECT = 0;
static final int RUNNING = 1;
static final int SELECT = 2;

Serial myPort;
String myText, inBuffer;
int state;
int nPorts;
String[] portsList;
color bgColor, textBoxColor, textColor;
int offsetX, offsetY;
int offsetText;
int textBoxWidth;

void setup() {
	size(1000, 600);
	bgColor = color(40);
	textBoxColor = color(20);
	textColor = color(230);
	background(bgColor);
	myText = "";
	inBuffer = "";
	textAlign(LEFT, TOP);
	textSize(12);
	state = SELECT;
	portsList = Serial.list();
	nPorts = portsList.length;
	offsetX = 5;
	offsetY = 5;
	offsetText = 2;
	textBoxWidth = 400;
	PFont myFont = loadFont("ArialMT-12.vlw");
	textFont(myFont, 12);
}

void draw() {
	background(bgColor);
	switch (state) {
		case SELECT:
			selectPort();
			break;
		case CONNECT:
			char a = 0;
			while (myPort.available() > 0) {
				a = myPort.readChar();
			}
			if (a == '<') {
				sendSetup();
				state = RUNNING;
			}
			break;
		case RUNNING:
			writeTextBox(textBoxColor);
			readTextBox();
			break;
	}
}

void keyPressed() {
	switch (state) {
		case SELECT:
			if ((int(key) >= 48) && (int(key) <= 48 + nPorts - 1)) {
				myPort = new Serial(this, portsList[int(key) - 48], 115200);
				state = CONNECT;
			}
			break;
		case RUNNING:
			switch (key) {
				case BACKSPACE:
					if (myText.length() > 0) {
						myText = myText.substring(0, myText.length() - 1);
					}
					break;
				case DELETE:
					myText = "";
					break;
				case ENTER:
				case RETURN:
					sendText();
					break;
				default:
					myText = myText + key;
					break;
			}
			break;
	}
}

void writeTextBox(color c) {
	fill(c);
	noStroke();
	rect(offsetX, offsetY, textBoxWidth, 20);
	fill(textColor);
	text(myText, offsetX + offsetText, offsetY + offsetText, textBoxWidth, 20);
}

void sendText() {
	writeTextBox(color(255, 0, 0));
	myPort.write(myText + "\n");
	myText = "";
}

void readTextBox() {
	while (myPort.available() > 0) {
		inBuffer += myPort.readString();
	}
	fill(textBoxColor);
	noStroke();
	rect(offsetX, offsetY + 25, textBoxWidth, 300);
	fill(textColor);
	text(inBuffer, offsetX + offsetText, offsetY + 25 + offsetText, textBoxWidth, 300);
}

void selectPort() {
	fill(textBoxColor);
	noStroke();
	rect(offsetX, offsetY, textBoxWidth, 300);
	fill(textColor);
	String portsListString = "Please select port:\n";
	for (int i = 0; i < nPorts; i++) {
		portsListString += "[" + i + "] ";
		portsListString += portsList[i];
		portsListString += "\n";
	}
	text(portsListString, offsetX + offsetText, offsetY + offsetText, textBoxWidth, 200);
}

void sendSetup() {
	BufferedReader reader = createReader("setup.txt");
	String line = null;
	try {
		while ((line = reader.readLine()) != null) {
			myPort.write(line + '\n');
		}
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
}