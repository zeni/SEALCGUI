interface Motor {
    void setSelected(boolean s);
    void columnSQ(int v);
    void setGraphics(int x, int y, int r);
    void display();
    void SS(int v);
    void initSQ();
    void columnRP(int v);
    void ST();
    void action();
    String getType();
    void fillQ(int m, int v);
    void deQ();
}