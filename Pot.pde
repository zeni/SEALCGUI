class Pot {
    int xPos, yPos;
    int xOffset, yOffset;
    int height, width;
    int radius;
    int id;
    boolean selected;
    float angle;
    Slider[] sliders;
    String name;
    int selectedSlider;
    TickBox sweepTickBox;

    Pot() {}

    Pot(int _id) {
        id = _id;
        radius = 30;
        xPos = 0;
        height = (radius * 3);
        yPos = height * id;
        width = 260;
        xOffset = xPos + radius * 3 + 20;
        yOffset = yPos + int(.5 * radius);
        selected = false;
        sliders = new Slider[N_SLIDERS];
        sliders[1] = new RangeSlider(1);
        sliders[0] = new HSlider(0);
        sweepTickBox = new TickBox(0);
        switch (id) {
            case POT_GAIN:
                angle = -HALF_ANGLE;
                name = "GAIN";
                break;
            case POT_LEVEL:
                angle = -HALF_ANGLE;
                name = "LEVEL";
                break;
            case POT_PAN:
                angle = 0;
                name = "PAN";
                break;
            case POT_HIGH:
                angle = 0;
                name = "HI";
                break;
            case POT_MID:
                angle = 0;
                name = "MID";
                break;
            case POT_LOW:
                angle = 0;
                name = "LOW";
                break;
            case POT_FX:
                angle = -HALF_ANGLE;
                name = "FX";
                break;
        }
        selectedSlider = -1;
    }

    void display() {
        fill(200);
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(20);
        pushMatrix();
        translate(xPos, yPos);
        rect(0, 0, width, height);
        translate(2 * radius, 1.5 * radius);
        if (selected)
            fill(255, 0, 0);
        else
            fill(20);
        text(name, 0, 0);
        noFill();
        rotateZ(radians(angle));
        ellipse(0, 0, 2 * radius, 2 * radius);
        triangle(0, -radius - 10, -5, -radius, 5, -radius);
        popMatrix();
        pushMatrix();
        translate(xOffset, yOffset);
        for (int i = 0; i < N_SLIDERS; i++)
            sliders[i].display();
        translate(120, 0);
        sweepTickBox.display();
        popMatrix();

    }

    void setSelected(boolean s) {
        selected = s;
    }

    boolean getSelected() {
        return selected;
    }

    int checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            sweepTickBox.checkSelected(x - xOffset - 120, y - yOffset);
            return id;
        } else
            return -1;
    }

    int checkSelectedPotOnly(int x, int y) {
        if (distance(x - 2 * radius, int(y - 1.5 * radius)))
            return id;
        else {
            if (selectedSlider < 0) {
                int s = -1;
                int i = 0;
                while (s < 0) {
                    s = sliders[i++].checkSelected(x - xOffset, y - yOffset);
                    if (s >= 0) {
                        selectedSlider = s;
                        sliders[selectedSlider].setSelected(true);
                        sliders[selectedSlider].setValue();
                    } else {
                        if (i == N_SLIDERS) s = 0;
                    }
                }
            } else {
                if (sliders[selectedSlider].checkSelected(x - xOffset, y - yOffset) >= 0)
                    sliders[selectedSlider].setValue();
            }
        }
        return -1;
    }

    boolean distance(int x, int y) {
        float d = sqrt(pow(x - xPos, 2) + pow(y - yPos, 2));
        if (d < radius) return true;
        else return false;
    }

    void setAngle() {
        int y = mouseY - pmouseY;
        int x = mouseX - pmouseX;
        if ((y != 0) && (abs(y) >= abs(x))) {
            angle += ((y > 0) ? -10 : 10);
            if (angle < -FULL_ANGLE * .5) angle = -FULL_ANGLE * .5;
            if (angle > FULL_ANGLE * .5) angle = FULL_ANGLE * .5;
        } else if ((x != 0) && (abs(x) > abs(y))) {
            angle += ((x > 0) ? 10 : -10);
            if (angle < -FULL_ANGLE * .5) angle = -FULL_ANGLE * .5;
            if (angle > FULL_ANGLE * .5) angle = FULL_ANGLE * .5;
        }
    }

    void unselect() {
        selectedSlider = -1;
        for (int i = 0; i < N_SLIDERS; i++)
            sliders[i].setSelected(false);
    }
}