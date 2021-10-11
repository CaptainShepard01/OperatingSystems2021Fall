package com.company.lab1.myfunctions;

import java.awt.image.ImagingOpException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;

public class FunctionF extends FunctionThread {
    public FunctionF(Pipe.SourceChannel argsChannel, Pipe.SinkChannel resChannel) {
        super(argsChannel, resChannel);
    }

    @Override
    public void run() {
        try {
            ByteBuffer dataBuf = ByteBuffer.allocate(4);
            dataBuf.clear();
            try {
                this.argsChannel.read(dataBuf);
            } catch (IOException e) {
                e.printStackTrace();
            }
            dataBuf.flip();

            int arg = dataBuf.getInt();
            try {
                Thread.currentThread().sleep(5000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            int res = arg * arg;
            ByteBuffer resBuf = ByteBuffer.allocate(4);
            resBuf.clear();
            resBuf.putInt(res);
            resBuf.flip();

            this.resChannel.write(resBuf);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
