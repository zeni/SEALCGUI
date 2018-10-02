class Stepper {
    int realSteps;
    int absoluteSteps;
    int absoluteStepsIdle;
    int id;
    int nSteps;
    int steps; // for move/hammer
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    long timeMS; // for speed
    int speed; // en ms
    int speedRPM = 12; //en RPM
    boolean newBeat;
    int pause;
    boolean isPaused;
    int xPos, yPos;
    int radius;
    boolean selected;
    int inc;

    Stepper() {}

    Stepper(int n, int i) {
        id = i;
        nSteps = n;
        currentSteps = 0;
        realSteps = currentSteps;
        absoluteSteps = currentSteps;
        absoluteStepsIdle = absoluteSteps;
        speedRPM = 12;
        speed = floor(60000.0 / (speedRPM * nSteps));
        inc = getInc();
        steps = 0;
        dir = 0;
        currentDir = dir;
        pause = 1000;
        isPaused = false;
        timeMS = millis();
    }

    void SS(int v) {
        speedRPM = (v > 60000.0 / nSteps) ? floor(60000.0 / nSteps) : v;
        speed = (speedRPM > 0) ? (floor(60000.0 / (speedRPM * nSteps))) : 0;
        inc = getInc();
    }

    void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
    }

    void setSelected(boolean s) {
        selected = s;
    }

    int getInc() {
        float f = 1000 / frameRate;
        int i = (f > speed) ? round(f / speed) : 1;
        return i;
    }

    void absoluteStepsDir() {
        currentSteps += inc;
        if (currentDir > 0)
            absoluteSteps -= inc;
        else absoluteSteps += inc;
        absoluteSteps %= nSteps;
    }
}