class HSlider implements Slider {
    int height, width;
    int xPos, yPos;
    float value;
    int id;
    boolean selected;


    HSlider() {}

    HSlider(int i) {
        id = i;
        height = 20;
        width = 100;
        xPos = 0;
        yPos = id * (height + 10);
        value = 50;
        selected = false;
    }

    void display() {
        pushMatrix();
        translate(xPos, yPos);
        fill(30);
        noStroke();
        rect(0, 0, width * value / 100, height);
        noFill();
        stroke(50);
        rect(0, 0, width, height);
        popMatrix();
    }

    void setSelected(boolean s) {
        selected = s;
    }

    int checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            return id;
        } else return -1;
    }

    void setValue() {
        int x = mouseX - pmouseX;
        int inc = 2;
        if (x != 0) {
            value += ((x > 0) ? inc : -inc);
            if (value < 0) value = 0;
            if (value > 100) value = 100;
        }
    }
}