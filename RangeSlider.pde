class RangeSlider implements Slider {
    int height, width;
    int xPos, yPos;
    float valueStart, valueEnd;
    int id;
    boolean valueStartSelected;
    boolean selected;


    RangeSlider() {}

    RangeSlider(int i) {
        id = i;
        height = 20;
        width = 100;
        xPos = 0;
        yPos = id * (height + 10);
        valueEnd = 50;
        valueStart = 0;
        valueStartSelected = true;
        selected = false;
    }

    void display() {
        pushMatrix();
        translate(xPos, yPos);
        fill(30);
        noStroke();
        rect(width * valueStart / 100, 0, width * (valueEnd - valueStart) / 100, height);
        noFill();
        stroke(50);
        rect(0, 0, width, height);
        noStroke();
        fill(50);
        rect(width * valueStart / 100 - 5, -2, 10, height + 4);
        rect(width * valueEnd / 100 - 5, -2, 10, height + 4);
        popMatrix();
    }

    void setSelected(boolean s) {
        selected = s;
    }

    int checkSelected(int x, int y) {
        if (selected) {
            if (valueStartSelected)
                x += width * valueStart / 100;
            else
                x += width * valueEnd / 100;
            return id;
        } else {
            if (checkHandle(x, y, valueStart)) {
                valueStartSelected = true;
                return id;
            } else if (checkHandle(x, y, valueEnd)) {
                valueStartSelected = false;
                return id;
            } else
                return -1;
        }
    }

    boolean checkHandle(int x, int y, float v) {
        return ((x > xPos + width * v / 100 - 5) && (x < xPos + width * v / 100 + 5) && (y > yPos - 2) && (y < yPos + height + 4));
    }

    void setValue() {
        int x = mouseX - pmouseX;
        int inc = 2;
        if (x != 0) {
            if (valueStartSelected) {
                valueStart += ((x > 0) ? inc : -inc);
                if (valueStart < 0) valueStart = 0;
                if (valueStart > valueEnd) valueStart = valueEnd;
            } else {
                valueEnd += ((x > 0) ? inc : -inc);
                if (valueEnd < valueStart) valueEnd = valueStart;
                if (valueEnd > 100) valueEnd = 100;
            }
        }
    }
}