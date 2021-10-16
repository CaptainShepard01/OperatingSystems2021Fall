package com.company.lab1.myfunctions;

import os.lab1.compfuncs.basic.DoubleOps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class FunctionF implements Runnable {
    final Pipe.SourceChannel argsChannel;
    final Pipe.SinkChannel resChannel;

    public FunctionF(Pipe.SourceChannel argsChannel, Pipe.SinkChannel resChannel) {
        this.argsChannel = argsChannel;
        this.resChannel = resChannel;
    }

    @Override
    public void run() {
        try {
            ByteBuffer dataBuf = ByteBuffer.allocate(4);
            dataBuf.clear();
            this.argsChannel.read(dataBuf);
            dataBuf.flip();

            int arg = dataBuf.getInt();

            Optional<Double> res = DoubleOps.trialF(arg);

            ByteBuffer resBuf, sizebuf;
            sizebuf = ByteBuffer.allocate(4);
            sizebuf.clear();
            if (res.isPresent()) {
                sizebuf.putInt(8);
                sizebuf.flip();
                this.resChannel.write(sizebuf);

                resBuf = ByteBuffer.allocate(8);
                resBuf.clear();
                resBuf.putDouble(res.get());
                resBuf.flip();
                this.resChannel.write(resBuf);
            } else {
                String info = "Something went wrong!";

                sizebuf.putInt(info.length());
                sizebuf.flip();
                this.resChannel.write(sizebuf);

                resBuf = ByteBuffer.allocate(info.length());
                resBuf.clear();
                resBuf.put(info.getBytes(StandardCharsets.UTF_8));
                resBuf.flip();
                this.resChannel.write(resBuf);
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
