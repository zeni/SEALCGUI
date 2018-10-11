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

    void display() {
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

    void setSelected(boolean s) {
        selected = s;
    }

    void checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            value = !value;
        }
    }

    void setValue() {
        value = !value;
    }
}