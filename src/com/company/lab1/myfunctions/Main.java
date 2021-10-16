package com.company.lab1.myfunctions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Optional;

public class Main {

    public static void main(String[] args) throws IOException {

        Pipe fPipeArgs = Pipe.open();
        Pipe.SinkChannel fArgsSkCh = fPipeArgs.sink();
        Pipe.SourceChannel fArgsSrcCh = fPipeArgs.source();

        Pipe gPipeArgs = Pipe.open();
        Pipe.SinkChannel gArgsSkCh = gPipeArgs.sink();
        Pipe.SourceChannel gArgsSrcCh = gPipeArgs.source();

        Pipe fPipeRes = Pipe.open();
        Pipe.SinkChannel fResSkCh = fPipeRes.sink();
        Pipe.SourceChannel fResSrcCh = fPipeRes.source();

        Pipe gPipeRes = Pipe.open();
        Pipe.SinkChannel gResSkCh = gPipeRes.sink();
        Pipe.SourceChannel gResSrcCh = gPipeRes.source();

        Thread fThread = new Thread(new FunctionF(fArgsSrcCh, fResSkCh));
        Thread gThread = new Thread(new FunctionG(gArgsSrcCh, gResSkCh));

        int x = 0;
        /*System.out.print("Enter x: ");
        Scanner keyboard = new Scanner(System.in);
        x = keyboard.nextInt();*/

        ByteBuffer argsBuf = ByteBuffer.allocate(4);
        argsBuf.clear();
        argsBuf.putInt(x);
        argsBuf.flip();
        fArgsSkCh.write(argsBuf);
        argsBuf.flip();
        gArgsSkCh.write(argsBuf);

        fThread.start();
        gThread.start();

        Optional<Double> fRes, gRes;

        ByteBuffer fResBuf;
        ByteBuffer gResBuf;
        ByteBuffer sizeBuf = ByteBuffer.allocate(4);
        sizeBuf.clear();

        boolean readFromF = false;
        boolean readFromG = false;
        while (true) {
            if (!readFromF) {
                if (fResSrcCh.read(sizeBuf) > 0) {
                    sizeBuf.flip();
                    int size = sizeBuf.getInt();

                    fResBuf = ByteBuffer.allocate(size);
                    fResBuf.clear();
                    fResSrcCh.read(fResBuf);
                    fResBuf.flip();

                    if(size == 8){
                        System.out.println(fResBuf.getDouble());
                    }
                    else {
                        String result = "";
                        while(fResBuf.hasRemaining()){
                            result += (char)fResBuf.get();
                        }
                        System.out.println(result);
                    }

                    readFromF = true;
                    sizeBuf.clear();
                }
            }
            if (!readFromG) {
                if (gResSrcCh.read(sizeBuf) > 0) {
                    sizeBuf.flip();
                    int size = sizeBuf.getInt();

                    gResBuf = ByteBuffer.allocate(size);
                    gResBuf.clear();
                        gResSrcCh.read(gResBuf);
                        gResBuf.flip();

                    if(size == 8){
                        System.out.println(gResBuf.getDouble());
                    }
                    else {
                        String result = "";
                        while (gResBuf.hasRemaining()){
                            result += (char)gResBuf.get();
                        }
                        System.out.println(result);
                    }

                    readFromG = true;
                }
            }
            if (readFromF && readFromG) {
                break;
            }
        }
        System.out.printf("Result of f(%d) * g(%d) = %d%n", x, x, 5 * 5);
        try {
            fThread.join();
            gThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
