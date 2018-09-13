class Motor {
    int xPos, yPos;
    int radius;
    int type;
    boolean selected;
    Motor(int t) {
        type = t;
        xPos = 0;
        yPos = 0;
        radius = 0;
        selected = false;
    }

    void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    void display() {
        noFill();
        if (selected)
            stroke(255, 0, 0);
        else stroke(255);
        ellipse(xPos, yPos, 2 * radius, 2 * radius);
        switch (type) {
            case TYPE_STEPPER:
                line(xPos, yPos - radius, xPos, yPos + radius);
                line(xPos - radius, yPos, xPos + radius, yPos);
                break;
            case TYPE_SERVO:
                line(xPos, yPos - radius, xPos, yPos + radius);
                break;
        }
    }
}