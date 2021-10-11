package com.company.lab1.myfunctions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.util.Scanner;

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

        int x = 5;
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

        int fRes = -1, gRes = -1;

        ByteBuffer fResBuf = ByteBuffer.allocate(4);
        fResBuf.clear();
        ByteBuffer gResBuf = ByteBuffer.allocate(4);
        gResBuf.clear();

        boolean readFromF = false;
        boolean readFromG = false;
        while (true) {
            if (!readFromF) {
                if (fResSrcCh.read(fResBuf) > 0) {
                    fResBuf.flip();
                    fRes = fResBuf.getInt();
                    readFromF = true;
                }
            }
            if (!readFromG) {
                if (gResSrcCh.read(gResBuf) > 0) {
                    gResBuf.flip();
                    gRes = gResBuf.getInt();
                    readFromG = true;
                }
            }
            if (readFromF && readFromG) {
                break;
            }
        }
        System.out.printf("Result of %d^2 + 7*%d = %d%n", x, x, fRes + gRes);
    }
}
