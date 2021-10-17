package com.company.lab1.myfunctions;

import os.lab1.compfuncs.advanced.DoubleOps;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Optional;

public class FunctionThread implements Runnable {
    protected final Pipe.SourceChannel argsChannel;
    protected final Pipe.SinkChannel resChannel;
    protected ByteBuffer dataBuf, resBuf, sizeBuf;
    protected int maximumAtempts = 10;

    public enum functionNumber {
        FUNC_F,
        FUNC_G
    }

    protected functionNumber function;

    public FunctionThread(Pipe.SourceChannel argsChannel, Pipe.SinkChannel resChannel, functionNumber function) {
        this.argsChannel = argsChannel;
        this.resChannel = resChannel;
        this.dataBuf = ByteBuffer.allocate(4);
        this.function = function;
    }

    @Override
    public void run() {

        try {
            dataBuf.clear();
            this.argsChannel.read(dataBuf);
            dataBuf.flip();

            int arg = dataBuf.getInt();

            for (int i = 0; i < maximumAtempts; ++i) {
                resChannel.configureBlocking(true);
                Optional<Optional<Double>> res = (function == functionNumber.FUNC_F ? DoubleOps.trialF(arg) : DoubleOps.trialG(arg));

                sizeBuf = ByteBuffer.allocate(4);
                sizeBuf.clear();
                String info = "Soft fail at function " + (function == functionNumber.FUNC_F ? "f" : "g") + ". Atempt #" + (i + 1) + (i == maximumAtempts - 1 ? ". Maximum attempts reached!" : "");

                if (res.isPresent()) {
                    if (res.get().isPresent()) {
                        sizeBuf.putInt(8);
                        sizeBuf.flip();
                        this.resChannel.write(sizeBuf);

                        resBuf = ByteBuffer.allocate(8);
                        resBuf.clear();
                        resBuf.putDouble(res.get().get());

                        resBuf.flip();
                        this.resChannel.write(resBuf);
                    } else {
                        sizeBuf.putInt(info.length());
                        sizeBuf.flip();
                        this.resChannel.write(sizeBuf);

                        resBuf = ByteBuffer.allocate(info.length());
                        resBuf.clear();
                        resBuf.put(info.getBytes(StandardCharsets.UTF_8));
                        resBuf.flip();
                        this.resChannel.write(resBuf);
                    }
                } else {
                    sizeBuf.putInt(info.length());
                    sizeBuf.flip();
                    this.resChannel.write(sizeBuf);

                    resBuf = ByteBuffer.allocate(info.length());
                    resBuf.clear();
                    resBuf.put(info.getBytes(StandardCharsets.UTF_8));
                    resBuf.flip();
                    this.resChannel.write(resBuf);
                }
                resChannel.configureBlocking(false);
            }

        } catch (IOException | InterruptedException e) {
            String info = "Hard fail at function " + (function == functionNumber.FUNC_F ? "f" : "g") + ".";

            sizeBuf.putInt(info.length());
            sizeBuf.flip();
            try {
                this.resChannel.write(sizeBuf);

                resBuf = ByteBuffer.allocate(info.length());
                resBuf.clear();
                resBuf.put(info.getBytes(StandardCharsets.UTF_8));
                resBuf.flip();

                this.resChannel.write(resBuf);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }

    }
}
