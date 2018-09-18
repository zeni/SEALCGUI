class Stepper implements Motor {
    int waveDir; // increasing / decreasing speed
    int turns; // for rotate (0=continuous rotation)
    int realSteps;
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int steps; // for move/hammer
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int indexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    long timeMS; // for speed
    int speed; // en ms
    int speedRPM = 12; //en RPM
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

    Stepper(int n, int i) {
        id = i;
        waveDir = 0;
        nSteps = n;
        realSteps = currentSteps;
        for (int j = 0; j < MAX_SEQ; j++)
            seq[j] = 0;
        angleSeq = 0;
        speedRPM = 12;
        speed = (speedRPM > 0) ? (floor(60.0 / (speedRPM * nSteps) * 1000)) : 0;
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
        indexSeq = 0;
        lengthSeq = 0;
        pause = 1000;
        isPaused = false;
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
        else
            stroke(255);
        pushMatrix();
        translate(xPos, yPos);
        if (currentDir == 0)
            rotateZ(radians(360.0 * currentSteps / nSteps));
        else
            rotateZ(radians(-360.0 * currentSteps / nSteps));
        ellipse(0, 0, 2 * radius, 2 * radius);
        line(0, -radius, 0, 0 + radius);
        line(0 - radius, 0, 0 + radius, 0);
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
        text(s, 0, 0);
        popMatrix();
    }

    String getType() {
        return " (stepper)";
    }

    void SS(int v) {
        speedRPM = (v > 0) ? v : 0;
        speed = (speedRPM > 0) ? (floor(60.0 / (speedRPM * nSteps) * 1000)) : 0;
    }

    void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    void setRO(int v) {
        if (v <= 0) {
            turns = 0;
        } else {
            turns = v;
        }
        steps = turns * nSteps;
        mode = MODE_RO;
        timeMS = millis();
    }

    void initRP() {
        turns = 1;
        pause = 1000;
        isPaused = false;
    }

    void columnRP(int v) {
        turns = (v <= 0) ? 1 : v;
    }

    void setRP(int v) {
        isPaused = false;
        pause = (v <= 0) ? 1000 : v;
        turns = (turns <= 0) ? 1 : turns;
        steps = turns * nSteps;
        mode = MODE_RP;
        timeMS = millis();
    }

    void setRR(int v) {
        setRA(v);
    }

    void setRA(int v) {
        v = (v <= 0) ? 0 : (v % 360);
        steps = int(v / 360.0 * nSteps);
        currentSteps = 0;
        mode = MODE_RA;
        timeMS = millis();
    }

    void setRW(int v) {
        v = (v <= 0) ? 1 : v;
        steps = int(nSteps / (2.0 * v));
        currentSteps = 0;
        realSteps = currentSteps;
        waveDir = 0;
        mode = MODE_RW;
        timeMS = millis();
    }

    void initSQ() {
        angleSeq = 0;
        indexSeq = 0;
        lengthSeq = 0;
        newBeat = true;
    }

    void columnSQ(int v) {
        v = (v <= 0) ? 0 : v;
        if (angleSeq == 0)
            angleSeq = v;
        else {
            seq[indexSeq] = v;
            indexSeq++;
        }
    }

    void setSQ(int v) {
        currentDir = dir;
        if (angleSeq == 0) {
            angleSeq = v;
            seq[indexSeq] = 1;
            lengthSeq = 1;
        } else {
            seq[indexSeq] = v;
            indexSeq++;
            lengthSeq = indexSeq;
        }
        angleSeq = int(angleSeq / 360.0 * nSteps);
        indexSeq = 0;
        steps = angleSeq;
        angleSeq = 0;
        currentSteps = 0;
        mode = MODE_SQ;
        timeMS = millis();
    }

    // move one step
    void moveStep() {
        if (currentSteps >= steps) {
            ST();
        } else {
            currentSteps++;
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
            case MODE_RO:
                RO();
                break;
            case MODE_RP:
                RP();
                break;
            case MODE_RA:
                RA();
                break;
            case MODE_RW:
                RW();
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

    // rotation
    void RO() {
        if (speed > 0) {
            if ((millis() - timeMS) >= speed) {
                if (turns == 0) {
                    currentSteps++;
                    currentSteps %= nSteps;
                    if (currentSteps == 0)
                        deQ();
                    timeMS = millis();
                } else
                    moveStep();
            }
        } else {
            ST();
        }
    }

    // rotation with pause
    void RP() {
        if (speed > 0) {
            if (isPaused) {
                if ((millis() - timeMS) > pause) {
                    isPaused = false;
                    currentSteps = 0;
                }
            } else {
                if ((millis() - timeMS) > speed) {
                    if (currentSteps >= steps) {
                        isPaused = true;
                        currentSteps = 0;
                        deQ();
                    } else
                        currentSteps++;
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    // rotate a number of steps
    void RA() {
        if (speed > 0) {
            if ((millis() - timeMS) > speed) {
                moveStep();
            }
        } else {
            ST();
        }
    }

    // continuous wave movement (like rotate but with changing speed)
    void RW() {
        if (speed > 0) {
            int s = (waveDir == 0) ? (speed * (steps - currentSteps)) : (speed * currentSteps);
            if ((millis() - timeMS) > s) {
                if (currentSteps >= steps) {
                    waveDir = 1 - waveDir;
                    currentSteps = 0;
                } else {
                    realSteps++;
                    realSteps %= nSteps;
                    if (realSteps == 0)
                        deQ();
                    currentSteps++;
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    void WA() {
        if (isPaused) {
            if ((millis() - timeMS) > pause) {
                isPaused = false;
                ST();
            }
        } else {
            isPaused = true;
        }
    }

    // continuous hammer movement with pattern of angles
    void SQ() {
        if (speed > 0) {
            if (newBeat) {
                deQ();
                newBeat = false;
                int a = floor(indexSeq / 2);
                switch (seq[a]) {
                    case 2:
                        currentDir = 1 - dir;
                        break;
                    case 1:
                    case 0:
                        currentDir = dir;
                        break;
                }
            }
            if ((millis() - timeMS) > speed) {
                if (currentSteps >= steps) {
                    currentDir = 1 - currentDir;
                    currentSteps = 0;
                    indexSeq++;
                    if ((indexSeq % 2) == 0)
                        newBeat = true;
                    int a = floor(indexSeq / 2);
                    if (a >= lengthSeq)
                        indexSeq = 0;
                } else {
                    int a = floor(indexSeq / 2);
                    currentSteps++;
                    if (seq[a] > 0);
                    timeMS = millis();
                }
            }
        } else {
            ST();
        }
    }

    void setSelected(boolean s) {
        selected = s;
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
            case MODE_RR:
                setRA(valuesQ[0]);
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

    void fillQ(int m, int v) {
        modesQ[sizeQ] = m;
        valuesQ[sizeQ] = v;
        sizeQ++;
        sizeQ = (sizeQ > MAX_QUEUE) ? MAX_QUEUE : sizeQ;
    }

}