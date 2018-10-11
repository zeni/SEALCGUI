class Input {
    Stepper[] steppers = new Stepper[N_STEPPERS];
    Pot[] pots = new Pot[N_STEPPERS];
    boolean selected;
    int xPos, yPos;
    int height, width;
    int id;
    int selectedPot;

    Input() {}

    Input(int _id) {
        id = _id;
        selectedPot = POT_LEVEL;
        selected = false;
        yPos = 10;
        width = 260;
        height = 700;
        xPos = 10 * (id + 1) + id * width;
        for (int i = 0; i < N_STEPPERS; i++) {
            steppers[i] = new Stepper(48, i);
            pots[i] = new Pot(i);
        }
        pots[POT_LEVEL].setSelected(true);
    }

    void display() {
        fill(230);
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(250);
        noFill();
        pushMatrix();
        translate(xPos, yPos);
        for (int i = 0; i < N_STEPPERS; i++)
            pots[i].display();
        pots[selectedPot].display();
        popMatrix();
    }

    void setSelected(boolean s) {
        selected = s;
    }

    int checkSelected(int x, int y) {
        if ((x > xPos) && (x < xPos + width) && (y > yPos) && (y < yPos + height)) {
            int s = -1;
            int i = 0;
            while (s < 0) {
                s = pots[i++].checkSelected(x - xPos, y - yPos);
                if (s >= 0) {
                    if (s != selectedPot) {
                        pots[s].setSelected(true);
                        pots[selectedPot].setSelected(false);
                        selectedPot = s;
                    }
                } else {
                    if (i == N_STEPPERS) s = 0;
                }
            }
            return id;
        } else return -1;
    }

    void checkPotSelected(int x, int y) {
        if (pots[selectedPot].checkSelectedPotOnly(x - xPos, y - yPos) >= 0)
            pots[selectedPot].setAngle();
    }

    void unselect() {
        for (int i = 0; i < N_STEPPERS; i++)
            pots[i].unselect();
    }
}