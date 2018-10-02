class Pot {
    int xPos, yPos;
    int radius;
    int id;
    boolean selected;
    int angle;

    Pot() {}

    Pot(int i) {
        id = i;
        radius = 30;
        xPos = 0;
        yPos = (radius * 2 + 20) * (id + 1);
        selected = false;
    }

    void display() {
        noFill();
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(20);
        pushMatrix();
        translate(xPos, yPos);
        rotateZ(-PI * FULL_ANGLE / 360.0);
        ellipse(0, 0, 2 * radius, 2 * radius);
        triangle(0, -radius - 10, -5, -radius, 5, -radius);
        popMatrix();
    }

    void setSelected(boolean s) {
        selected = s;
    }

    int checkSelected(int x, int y) {
        if (distance(x, y)) {
            return id;
        } else return -1;
    }

    boolean distance(int x, int y) {
        float d = sqrt(pow(x - xPos, 2) + pow(y - yPos, 2));
        if (d < radius) return true;
        else return false;
    }
}