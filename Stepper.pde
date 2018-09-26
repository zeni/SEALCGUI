class Stepper implements Motor {
    int waveDir; // increasing / decreasing speed
    int turns; // for rotate (0=continuous rotation)
    int realSteps;
    int absoluteSteps;
    int absoluteStepsIdle;
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
    int currentIndexSeq; // current position in sequence
    int indexSeq; // current position in sequence
    int currentLengthSeq; // length of seq.
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
    int inc;

    Stepper(int n, int i) {
        id = i;
        waveDir = 0;
        nSteps = n;
        currentSteps = 0;
        realSteps = currentSteps;
        absoluteSteps = currentSteps;
        absoluteStepsIdle = absoluteSteps;
        for (int j = 0; j < MAX_SEQ; j++) {
            seq[j] = 0;
            currentSeq[j] = 0;
        }
        angleSeq = 0;
        speedRPM = 12;
        speed = floor(60000.0 / (speedRPM * nSteps));
        inc = getInc();
        mode = MODE_IDLE;
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
        currentIndexSeq = 0;
        currentLengthSeq = 0;
        timeMS = millis();
    }

    void setGraphics(int x, int y, int r) {
        xPos = x;
        yPos = y;
        radius = r;
    }

    void display(SecondApplet sa) {
        noFill();
        if (selected)
            stroke(255, 0, 0);
        else
            stroke(255);
        pushMatrix();
        translate(xPos, yPos);
        rotateZ(TWO_PI * absoluteSteps / nSteps);
        ellipse(0, 0, 2 * radius, 2 * radius);
        line(0, -radius, 0, radius);
        line(0 - radius, 0, radius, 0);
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
        s += "\nAngle: " + absoluteSteps * 360.0 / nSteps;
        text(s, 0, 0);
        popMatrix();
    }

    String getType() {
        return " (stepper)";
    }

    void SS(int v) {
        speedRPM = (v > 60000.0 / nSteps) ? floor(60000.0 / nSteps) : v;
        speed = (speedRPM > 0) ? (floor(60000.0 / (speedRPM * nSteps))) : 0;
        inc = getInc();
    }

    void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    void setRO(int v) {
        turns = (v <= 0) ? 0 : v;
        steps = turns * nSteps;
        mode = MODE_RO;
        timeMS = millis();
    }

    void columnRP(int v) {
        turns = (v <= 0) ? 1 : v;
    }

    void setRP(int v) {
        isPaused = false;
        pause = (v <= 0) ? 1000 : v;
        turns = (turns <= 0) ? 1 : turns;
        currentSteps = 0;
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
        v = (v <= 0) ? 0 : v;
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
        angleSeq = int(angleSeq / 360.0 * nSteps);
        currentIndexSeq = 0;
        steps = angleSeq;
        angleSeq = 0;
        currentSteps = 0;
        mode = MODE_SQ;
        timeMS = millis();
    }

    void setSelected(boolean s) {
        selected = s;
    }

    void setWA(int v) {
        isPaused = false;
        pause = (v < 0) ? 1000 : v;
        mode = MODE_WA;
        timeMS = millis();
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
                int iturns = floor(float(currentSteps) / nSteps) + 1;
                if (turns == 0) {
                    absoluteStepsDir();
                    if (currentSteps >= nSteps * iturns) {
                        currentSteps %= nSteps;
                        deQ();
                    }
                } else {
                    if (currentSteps >= steps)
                        ST();
                    else {
                        absoluteStepsDir();
                        if (currentSteps >= nSteps * iturns)
                            deQ();
                    }

                }
                timeMS = millis();
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
                        absoluteSteps = absoluteStepsIdle;
                        deQ();
                    } else {
                        int iturns = floor(float(currentSteps) / nSteps) + 1;
                        absoluteStepsDir();
                        if (currentSteps >= nSteps * iturns)
                            deQ();
                    }
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
                if (currentSteps >= steps) {
                    ST();
                    absoluteStepsIdle = steps;
                    absoluteSteps = absoluteStepsIdle;
                } else
                    absoluteStepsDir();
                timeMS = millis();
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
                    realSteps += inc;
                    absoluteStepsDir();
                    if (realSteps >= nSteps) {
                        realSteps %= nSteps;
                        deQ();
                    }
                }
                timeMS = millis();
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
        } else
            isPaused = true;
    }

    // continuous hammer movement with pattern of angles
    void SQ() {
        if (speed > 0) {
            if (newBeat) {
                newBeat = false;
                int a = floor(currentIndexSeq / 2);
                switch (currentSeq[a]) {
                    case 2:
                        currentDir = 1 - dir;
                        break;
                    case 1:
                    case 0:
                        currentDir = dir;
                        break;
                }
                deQ();
            }
            if ((millis() - timeMS) > speed) {
                int a = floor(currentIndexSeq / 2);
                if (currentSteps >= steps) {
                    currentSteps = 0;
                    currentIndexSeq++;
                    if (a >= currentLengthSeq)
                        currentIndexSeq = 0;
                    if ((currentIndexSeq % 2) == 0)
                        newBeat = true;
                    else currentDir = 1 - currentDir;
                } else {
                    if (seq[a] > 0)
                        absoluteStepsDir();
                    else
                        currentSteps += inc;
                }
                timeMS = millis();
            }
        } else {
            ST();
        }
    }

    void deQ() {
        switch (modesQ[0]) {
            case MODE_IDLE:
                break;
            case MODE_ST:
                absoluteSteps = absoluteStepsIdle;
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