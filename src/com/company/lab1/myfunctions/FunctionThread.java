package com.company.lab1.myfunctions;

import java.nio.channels.Pipe;

public class FunctionThread implements Runnable{
    protected final Pipe.SourceChannel argsChannel;
    protected final Pipe.SinkChannel resChannel;

    public FunctionThread(Pipe.SourceChannel argsChannel, Pipe.SinkChannel resChannel) {
        this.argsChannel = argsChannel;
        this.resChannel = resChannel;
    }

    @Override
    public void run() {

    }
}
