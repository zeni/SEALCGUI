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

void setup() {
	size(800, 600);
	background(20);
	myText = "";
	inBuffer = "";
	textAlign(LEFT, TOP);
	textSize(12);
	state = SELECT;
	portsList = Serial.list();
	nPorts = portsList.length;
}

void draw() {
	background(20);
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
			writeTextBox(color(240));
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
	rect(5, 5, 400, 15);
	fill(20);
	text(myText, 5, 5, 400, 15);
}

void sendText() {
	writeTextBox(color(250, 0, 0));
	myPort.write(myText + "\n");
	myText = "";
}

void readTextBox() {
	while (myPort.available() > 0) {
		inBuffer += myPort.readString();
	}
	fill(240);
	noStroke();
	rect(5, 30, 400, 300);
	fill(20);
	text(inBuffer, 5, 30, 400, 300);
}

void selectPort() {
	fill(240);
	noStroke();
	rect(5, 5, 400, 300);
	fill(20);
	String portsListString = "Please select port:\n";
	for (int i = 0; i < nPorts; i++) {
		portsListString += "[" + i + "] ";
		portsListString += portsList[i];
		portsListString += "\n";
	}
	text(portsListString, 5, 5, 200, 200);
}

void sendSetup() {
	myText = "4";
	myPort.write(myText + "\n");
	myText = "2,2,0,0";
	myPort.write(myText + "\n");
	myText = "0,48,3,6";
	myPort.write(myText + "\n");
	myText = "1,11,15,195";
	myPort.write(myText + "\n");
	myText = "1,10,15,195";
	myPort.write(myText + "\n");
	myText = "";
}