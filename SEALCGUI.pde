/**
 * GUI for SEALC
 *
 **/
import processing.serial.*;

static final int CONNECT = 1;
static final int RUNNING = 2;
static final int SELECT = 0;

Serial myPort;
String myText, inBuffer;
int state;
int nPorts;
String[] portsList;
color bgColor, textBoxColor, textColor;
int offsetX, offsetY;
int offsetText;
int textBoxWidth, inTextBoxHeight;
int textSize, textLead;

void setup() {
	size(1000, 600);
	bgColor = color(40);
	textBoxColor = color(20);
	textColor = color(230);
	background(bgColor);
	myText = "";
	inBuffer = "";
	textAlign(LEFT, TOP);
	state = SELECT;
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
}

void draw() {
	background(bgColor);
	switch (state) {
		case SELECT:
			selectPort();
			break;
		case CONNECT:
			//char a = 0;
			while (myPort.available() > 0)
				inBuffer += myPort.readString();
			if (inBuffer.length() > 0) {
				if (inBuffer.charAt(inBuffer.length() - 1) == '<') {
					sendSetup();
					state = RUNNING;
				}
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
					myText = myText + key;
					myText = myText.toUpperCase();
					break;
			}
			break;
	}
}

void writeTextBox(color c) {
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
		int w = int(textWidth(myText.substring(0, i)));
		text(myText.substring(i, i + 1), offsetX + offsetText + w, offsetY, textBoxWidth, textLead);
	}
	textAlign(LEFT, TOP);
}

void sendText() {
	writeTextBox(color(255, 0, 0));
	myPort.write(myText + "\n");
	myText = "";
}

void readTextBox() {
	while (myPort.available() > 0)
		inBuffer += myPort.readString();
	fill(textBoxColor);
	noStroke();
	rect(offsetX, offsetY + 25, textBoxWidth, inTextBoxHeight);
	fill(textColor);
	scrollText();
	text(inBuffer, offsetX + offsetText, offsetY + 25 + offsetText, textBoxWidth, inTextBoxHeight);
}

void scrollText() {
	int nLines = 0;
	for (int i = 0; i < inBuffer.length(); i++) {
		if (inBuffer.charAt(i) == '\n') nLines++;
	}
	if (nLines * textLead > inTextBoxHeight) {
		int a = inBuffer.indexOf('\n');
		inBuffer = inBuffer.substring(a + 1);
	}
}

void selectPort() {
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