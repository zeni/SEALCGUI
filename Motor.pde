interface Motor {
    void setSelected(boolean s);
    void columnSQ(int v);
    void setGraphics(int x, int y, int r);
    void display();
    void initRP();
    void SS(int v);
    void setRO(int v);
    void setRP(int v);
    void setRA(int v);
    void setRR(int v);
    void setRW(int v);
    void setWA(int v);
    void VA();
    void initSQ();
    void setSQ(int v);
    void columnRP(int v);
    void ST();
    void action();
    void setSD(int v);
    String getType();
    void GS();
    void GD();
    void GM();
    void GI(int i);
    void fillQ(int m, int v);
    void deQ();
    void initWA();
}