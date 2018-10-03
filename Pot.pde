class Pot {
    int xPos, yPos;
    int height, width;
    int radius;
    int id;
    boolean selected;
    float angle;
    RangeSlider[] sliders = new RangeSlider[2];
    String name;
    int selectedSlider;

    Pot() {}

    Pot(int _id) {
        id = _id;
        radius = 30;
        xPos = 0;
        height = (radius * 3);
        yPos = height * id;
        width = 250;
        selected = false;
        for (int i = 0; i < 2; i++)
            sliders[i] = new RangeSlider(i);
        switch (id) {
            case POT_GAIN:
            case POT_LEVEL:
            case POT_FX:
                angle = -FULL_ANGLE * .5;
                break;
            default:
                angle = 0;
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
        rotateZ(radians(angle));
        ellipse(0, 0, 2 * radius, 2 * radius);
        triangle(0, -radius - 10, -5, -radius, 5, -radius);
        popMatrix();
        pushMatrix();
        translate(xPos + radius * 3 + 20, yPos + .5 * radius);
        for (int i = 0; i < 2; i++)
            sliders[i].display();
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
            return id;
        } else {
            /*int s = -1;
            int i = 0;
            while (s < 0) {
                s = sliders[i++].checkSelected(x - (xPos + radius * 3 + 20), int(y - (yPos + .5 * radius)));
                if (s >= 0) {
                    sliders[s].setValue();
                } else {
                    if (i == 2) s = 0;
                }
            }*/
            return -1;
        }
    }

    int checkSelectedPotOnly(int x, int y) {
        if (distance(x - 2 * radius, int(y - 1.5 * radius))) {
            return id;
        } else {
            if (selectedSlider < 0) {
                int s = -1;
                int i = 0;
                while (s < 0) {
                    s = sliders[i++].checkSelected(x - (xPos + radius * 3 + 20), int(y - (yPos + .5 * radius)));
                    if (s >= 0) {
                        selectedSlider = s;
                        sliders[s].setSelected(true);
                        sliders[s].setValue();
                    } else {
                        if (i == 2) s = 0;
                    }
                }
            } else {
                if (sliders[selectedSlider].checkSelected(x - (xPos + radius * 3 + 20), int(y - (yPos + .5 * radius))) >= 0)
                    sliders[selectedSlider].setValue();
            }
            return -1;
        }
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
        for (int i = 0; i < 2; i++)
            sliders[i].setSelected(false);
    }
}