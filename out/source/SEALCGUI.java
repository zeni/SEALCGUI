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


static final int CONNECT = 1;
static final int RUNNING = 2;
static final int SELECT = 0;

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

public void setup() {
	
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

public void draw() {
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

public void keyPressed() {
	switch (state) {
		case SELECT:
			if ((PApplet.parseInt(key) >= 48) && (PApplet.parseInt(key) <= 48 + nPorts - 1)) {
				myPort = new Serial(this, portsList[PApplet.parseInt(key) - 48], 115200);
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
	try {
		while ((line = reader.readLine()) != null) {
			myPort.write(line + '\n');
		}
		reader.close();
	} catch (IOException e) {
		e.printStackTrace();
	}
}
class Stepper {
	int xPos, yPos;
	int radius;
	Stepper(int x, int y, int r) {
		xPos = x;
		yPos = y;
		radius = r;
	}
	public void display() {
		ellipse(xPos, yPos, radius, radius);
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
