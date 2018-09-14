class Servo implements Motor {
    int angleMin, angleMax;
    int angle; // current angle
    int[] seq = new int[MAX_SEQ]; // seq. of angles for beat
    int angleSeq; // angle value for seq.
    int id;
    int nSteps;
    int mode;
    int steps; // for move/hammer
    int stepsHome; // steps for homing
    int dir;
    int currentDir;
    int currentSteps; // for move/hammer
    int indexSeq; // current position in sequence
    int lengthSeq; // length of seq.
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
        nSteps = 360;
        for (int j = 0; j < MAX_SEQ; j++)
            seq[j] = 0;
        angleSeq = 0;
        speed = (speedRPM > 0) ? (floor(60.0 / (speedRPM * nSteps) * 1000)) : 0;
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
        line(xPos, yPos - radius, xPos, yPos + radius);
    }

    void SS(int v) {
        speedRPM = (v > 0) ? v : 0;
        speed = (speedRPM > 0) ? (floor(60.0 / (speedRPM * nSteps) * 1000)) : 0;
    }

    String getType() {
        return " (servo)";
    }

    void setSD(int v) {
        v = (v > 0) ? 1 : v;
        dir = (v < 0) ? (1 - dir) : v;
        mode = MODE_SD;
    }

    void setRO(int v) {}

    void initRP() {}

    void columnRP(int v) {}

    void setRP(int v) {}

    void setRR(int v) {
        v = (v <= 0) ? 0 : (v % (angleMax - angleMin));
        v = (dir > 0) ? -v : v;
        steps = int(abs(v) / 360.0 * nSteps);
        currentSteps = 0;
        mode = MODE_RR;
        timeMS = millis();
    }

    void setRA(int v) {
        v = (v < angleMin) ? angleMin : ((v > angleMax) ? angleMax : v);
        if (v > angle) {
            v = v - angle;
            currentDir = 1;
        } else {
            v = angle - v;
            currentDir = 0;
        }
        steps = int(v / 360.0 * nSteps);
        currentSteps = 0;
        mode = MODE_RA;
        timeMS = millis();
    }

    void setRW(int v) {}

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
        if (currentSteps >= steps) {
            ST();
        } else {
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
        mode = MODE_ST;
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
        } else {
            ST();
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
                    if (seq[a] > 0) {
                        servoStep();
                    }
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