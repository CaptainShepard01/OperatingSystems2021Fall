package com.company.lab2;

public class sProcess {
    public int cputime;
    public int ioblocking;
    public int cpudone;
    public int ionext;
    public int numblocked;
    public int currentQuantum = 0;

    public sProcess (int cputime, int ioblocking, int cpudone, int ionext, int numblocked) {
        this.cputime = cputime;
        this.ioblocking = ioblocking;
        this.cpudone = cpudone;
        this.ionext = ionext;
        this.numblocked = numblocked;
    }
}

