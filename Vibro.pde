class Vibro implements Motor {
    int duration;
    boolean isOn;
    int[] durationSeq = new int[MAX_SEQ];
    int[] stateSeq = new int[MAX_SEQ];
    int[] currentDurationSeq = new int[MAX_SEQ];
    int[] currentStateSeq = new int[MAX_SEQ];
    int id;
    int mode;
    int indexSeq; // current position in sequence
    int lengthSeq; // length of seq.
    int currentIndexSeq; // current position in sequence
    int currentLengthSeq; // length of seq.
    long timeMS; // for speed
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

    Vibro(int i) {
        id = i;
        isOn = false;
        mode = MODE_IDLE;
        for (int j = 0; j < MAX_SEQ; j++) {
            durationSeq[j] = 0;
            stateSeq[j] = 0;
            currentDurationSeq[j] = 0;
            currentStateSeq[j] = 0;
        }
        for (int j = 0; j < MAX_QUEUE; j++) {
            modesQ[j] = MODE_IDLE;
            valuesQ[j] = -1;
        }
        sizeQ = 0;
        indexSeq = 0;
        lengthSeq = 0;
        currentIndexSeq = 0;
        currentLengthSeq = 0;
        duration = 0;
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
        else stroke(255);
        pushMatrix();
        translate(xPos, yPos);
        if (isOn)
            ellipse(random(5), random(5), 2 * radius, 2 * radius);
        else
            ellipse(0, 0, 2 * radius, 2 * radius);
        popMatrix();
        pushMatrix();
        translate(xPos - radius, yPos + 2 * radius);
        if (selected)
            fill(255, 0, 0);
        else
            fill(255);
        String s = id + getType() + "\n";
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
        return " (vibro)";
    }

    void setRO(int v) {
        duration = (v <= 0) ? 0 : v;
        mode = MODE_RO;
        timeMS = millis();
    }

    void columnRP(int v) {
        duration = (v <= 0) ? 1000 : v;
    }

    void setRP(int v) {
        isPaused = false;
        pause = (v <= 0) ? 1000 : v;
        duration = (duration <= 0) ? 1000 : duration;
        mode = MODE_RP;
        timeMS = millis();
    }

    void initSQ() {
        indexSeq = 0;
        lengthSeq = 0;
        newBeat = true;
    }

    void columnSQ(int v) {
        if (indexSeq % 2 == 0) {
            v = (v <= 0) ? 0 : v;
            durationSeq[indexSeq / 2] = v;
        } else {
            v = (v <= 0) ? 0 : 1;
            stateSeq[(indexSeq - 1) / 2] = v;
        }
        indexSeq++;
    }

    void setSQ(int v) {
        v = (v <= 0) ? 0 : 1;
        newBeat = true;
        stateSeq[(indexSeq - 1) / 2] = v;
        indexSeq++;
        lengthSeq = indexSeq / 2;
        indexSeq = 0;
        currentLengthSeq = lengthSeq;
        for (int i = 0; i < currentLengthSeq; i++) {
            currentDurationSeq[i] = durationSeq[i];
            currentStateSeq[i] = stateSeq[i];
        }
        currentIndexSeq = 0;
        mode = MODE_SQ;
        timeMS = millis();
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
            case MODE_RA:
            case MODE_RR:
            case MODE_RW:
                break;
            case MODE_RO:
                RO();
                break;
            case MODE_RP:
                RP();
                break;
            case MODE_SQ:
                SQ();
                break;
            case MODE_WA:
                WA();
                break;
        }
    }

    // rotation
    void RO() {
        if (!isOn)
            isOn = true;
        if (duration == 0)
            deQ();
        else {
            if ((millis() - timeMS) > duration)
                ST();
        }
    }

    // rotation with pause
    void RP() {
        if (isPaused) {
            if ((millis() - timeMS) > pause) {
                isPaused = false;
                isOn = true;
                timeMS = millis();
            } else
                deQ();
        } else {
            if (!isOn)
                isOn = true;
            if ((millis() - timeMS) > duration) {
                isPaused = true;
                isOn = false;
                deQ();
                timeMS = millis();
            }
        }
    }

    void ST() {
        isOn = false;
        mode = MODE_IDLE;
        deQ();
    }

    // continuous hammer movement with pattern of angles
    void SQ() {
        if (newBeat) {
            deQ();
            isOn = currentStateSeq[currentIndexSeq] > 0;
            newBeat = false;
        }
        if ((millis() - timeMS) > currentDurationSeq[currentIndexSeq]) {
            newBeat = true;
            currentIndexSeq++;
            if (currentIndexSeq >= currentLengthSeq)
                currentIndexSeq = 0;
            timeMS = millis();
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
            isOn = false;
        }
    }

    void setSelected(boolean s) {
        selected = s;
    }

    void fillQ(int m, int v) {
        if (sizeQ >= MAX_QUEUE) {
            sizeQ--;
            for (int i = 0; i < sizeQ; i++) {
                modesQ[i] = modesQ[i + 1];
                valuesQ[i] = valuesQ[i + 1];
            }
        }
        modesQ[sizeQ] = m;
        valuesQ[sizeQ++] = v;
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
        mode = MODE_WA;
        timeMS = millis();
    }

    void SS(int v) {}

    void SD() {}

    void RA() {}

    void setRR(int v) {}

    void setRA(int v) {}

    void setRW(int v) {}

    void setSD(int v) {}
}