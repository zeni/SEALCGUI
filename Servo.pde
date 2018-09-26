class Servo implements Motor {
    int angleMin, angleMax;
    int angle; // current angle
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int[] currentSeq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int steps; // for move/hammer
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int indexSeq; // current position in sequence
    int currentIndexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    int currentLengthSeq; // length of seq.
    long timeMS; // for speed
    int speed; // en ms
    int speedRPM; //en RPM
    boolean newBeat;
    int[] modesQ = new int[MAX_QUEUE];
    int[] valuesQ = new int[MAX_QUEUE];
    int sizeQ;
    int pause;
    boolean isPaused;
    int xPos, yPos;
    int radius;
    int type;
    boolean selected;

    Servo(int amin, int amax, int i) {
        id = i;
        angleMin = amin;
        angleMax = amax;
        angle = angleMin;
        nSteps = 360;
        for (int j = 0; j < MAX_SEQ; j++) {
            seq[j] = 0;
            currentSeq[j] = 0;
        }
        angleSeq = 0;
        mode = MODE_IDLE;
        currentSteps = 0;
        steps = 0;
        dir = 0;
        currentDir = dir;
        for (int j = 0; j < MAX_QUEUE; j++) {
            modesQ[j] = MODE_IDLE;
            valuesQ[j] = -1;
        }
        sizeQ = 0;
        speedRPM = 12;
        speed = floor(60000.0 / (speedRPM * nSteps));
        indexSeq = 0;
        lengthSeq = 0;
        pause = 1000;
        isPaused = false;
        currentIndexSeq = 0;
        currentLengthSeq = 0;
        timeMS = millis();
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
        pushMatrix();
        translate(xPos, yPos);
        rotateZ(radians(angle));
        ellipse(0, 0, 2 * radius, 2 * radius);
        line(0, -radius, 0, radius);
        triangle(0, -radius - 10, -5, -radius, 5, -radius);
        popMatrix();
        pushMatrix();
        translate(xPos - radius, yPos + 2 * radius);
        if (selected)
            fill(255, 0, 0);
        else
            fill(255);
        String s = id + getType() + "\n";
        s += "Speed: " + speedRPM + "RPM\n";
        s += "Dir: " + ((dir > 0) ? "CCW" : "CW") + "\n";
        s += "Mode: ";
        switch (mode) {
            case MODE_ST:
                s += "ST";
                break;
            case MODE_RO:
                s += "RO";
                break;
            case MODE_RA:
                s += "RA";
                break;
            case MODE_RR:
                s += "RR";
                break;
            case MODE_WA:
                s += "WA";
                break;
            case MODE_RW:
                s += "RW";
                break;
            case MODE_RP:
                s += "RP";
                break;
            case MODE_SQ:
                s += "SQ";
                break;
            case MODE_SD:
                s += "SD";
                break;
            case MODE_IDLE:
                s += "IDLE";
                break;
        }
        s += "\nAngle: " + angle;
        text(s, 0, 0);
        popMatrix();
    }

    void SS(int v) {
        speedRPM = (v > 60000.0 / nSteps) ? floor(60000.0 / nSteps) : v;
        speed = (speedRPM > 0) ? (floor(60000.0 / (speedRPM * nSteps))) : 0;
    }

    String getType() {
        return " (servo)";
    }

    void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    void setRO(int v) {
        mode = MODE_IDLE;
    }

    void columnRP(int v) {}

    void setRP(int v) {
        mode = MODE_IDLE;
    }

    void setRR(int v) {
        currentDir = dir;
        steps = (v <= 0) ? 0 : (v % (angleMax - angleMin));
        currentSteps = 0;
        mode = MODE_RR;
        timeMS = millis();
    }

    void setRA(int v) {
        v = (v < angleMin) ? angleMin : ((v > angleMax) ? angleMax : v);
        if (v >= angle) {
            v = v - angle;
            currentDir = 0;
        } else {
            v = angle - v;
            currentDir = 1;
        }
        steps = v;
        currentSteps = 0;
        mode = MODE_RA;
        timeMS = millis();
    }

    void setRW(int v) {
        mode = MODE_IDLE;
    }

    void initSQ() {
        indexSeq = 0;
        lengthSeq = 0;
    }

    void columnSQ(int v) {
        v = (v <= 0) ? 0 : v;
        if (angleSeq == 0)
            angleSeq = v;
        else
            seq[indexSeq++] = v;
    }

    void setSQ(int v) {
        currentDir = dir;
        newBeat = true;
        if (angleSeq == 0) {
            angleSeq = v;
            indexSeq = 0;
            seq[indexSeq] = 1;
            lengthSeq = 1;
        } else {
            seq[indexSeq++] = v;
            lengthSeq = indexSeq;
        }
        currentLengthSeq = lengthSeq;
        for (int i = 0; i < currentLengthSeq; i++)
            currentSeq[i] = seq[i];
        indexSeq = 0;
        currentIndexSeq = 0;
        steps = angleSeq;
        angleSeq = 0;
        currentSteps = 0;
        mode = MODE_SQ;
        timeMS = millis();
    }

    // one step servo
    void servoStep() {
        if (currentDir == 0) {
            angle++;
            if (angle > angleMax) {
                currentSteps = steps;
                angle = angleMax;
            }
        } else {
            angle--;
            if (angle < angleMin) {
                currentSteps = steps;
                angle = angleMin;
            }
        }
    }

    // move one step
    void moveStep() {
        if (currentSteps >= steps)
            ST();
        else {
            currentSteps++;
            servoStep();
            timeMS = millis();
        }
    }

    void action() {
        switch (mode) {
            case MODE_IDLE:
                deQ();
                break;
            case MODE_ST:
                ST();
                break;
            case MODE_SD:
                SD();
                break;
            case MODE_RW:
            case MODE_RO:
            case MODE_RP:
                break;
            case MODE_RA:
            case MODE_RR:
                RA();
                break;
            case MODE_SQ:
                SQ();
                break;
            case MODE_WA:
                WA();
                break;
        }
    }

    void ST() {
        currentSteps = 0;
        mode = MODE_IDLE;
        deQ();
    }

    void SD() {
        currentDir = dir;
        deQ();
    }

    // rotate a number of steps
    void RA() {
        if (speed > 0) {
            if ((millis() - timeMS) > speed)
                moveStep();
        } else
            ST();
    }

    // continuous hammer movement with pattern of angles
    void SQ() {
        if (speed > 0) {
            if (newBeat) {
                deQ();
                newBeat = false;
                int a = floor(currentIndexSeq / 2);
                currentDir = (currentSeq[a] < 2) ? dir : (1 - dir);
            }
            if ((millis() - timeMS) > speed) {
                int a = floor(currentIndexSeq / 2);
                if (currentSteps >= steps) {
                    currentIndexSeq++;
                    currentSteps = 0;
                    indexSeq++;
                    if (a >= currentLengthSeq)
                        currentIndexSeq = 0;
                    if ((currentIndexSeq % 2) == 0)
                        newBeat = true;
                    else
                        currentDir = 1 - currentDir;
                } else {
                    currentSteps++;
                    if (currentSeq[a] > 0)
                        servoStep();
                }
                timeMS = millis();
            }
        } else
            ST();
    }

    void WA() {
        if (isPaused) {
            if ((millis() - timeMS) > pause) {
                isPaused = false;
                ST();
            }
        } else
            isPaused = true;
    }

    void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
    }

    void setSelected(boolean s) {
        selected = s;
    }

    void deQ() {
        switch (modesQ[0]) {
            case MODE_IDLE:
                break;
            case MODE_ST:
                mode = modesQ[0];
                break;
            case MODE_RO:
                setRO(valuesQ[0]);
                break;
            case MODE_RP:
                setRP(valuesQ[0]);
                break;
            case MODE_RA:
                setRA(valuesQ[0]);
                break;
            case MODE_RR:
                setRR(valuesQ[0]);
                break;
            case MODE_RW:
                setRW(valuesQ[0]);
                break;
            case MODE_SQ:
                setSQ(valuesQ[0]);
                break;
            case MODE_SD:
                setSD(valuesQ[0]);
                break;
            case MODE_WA:
                setWA(valuesQ[0]);
                break;
        }
        if (modesQ[0] != MODE_IDLE) {
            for (int i = 1; i < MAX_QUEUE; i++) {
                modesQ[i - 1] = modesQ[i];
                valuesQ[i - 1] = valuesQ[i];
            }
            modesQ[MAX_QUEUE - 1] = MODE_IDLE;
            valuesQ[MAX_QUEUE - 1] = -1;
            sizeQ--;
            sizeQ = (sizeQ < 0) ? 0 : sizeQ;
        }
    }

    void initWA() {
        pause = 1000;
        isPaused = false;
    }

    void setWA(int v) {
        isPaused = false;
        v = (v < 0) ? 1000 : v;
        pause = v;
        pause = v;
        mode = MODE_WA;
        timeMS = millis();
    }
}