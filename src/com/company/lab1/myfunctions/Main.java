package com.company.lab1.myfunctions;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Pipe;
import java.time.Instant;
import java.util.Optional;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) throws IOException {
        while (true) {
            Pipe fPipeArgs = Pipe.open();
            Pipe.SinkChannel fArgsSinkCh = fPipeArgs.sink();
            Pipe.SourceChannel fArgsSourceCh = fPipeArgs.source();

            Pipe gPipeArgs = Pipe.open();
            Pipe.SinkChannel gArgsSinkCh = gPipeArgs.sink();
            Pipe.SourceChannel gArgsSourceCh = gPipeArgs.source();

            Pipe fPipeRes = Pipe.open();
            Pipe.SinkChannel fResSinkCh = fPipeRes.sink();
            Pipe.SourceChannel fResSourceCh = fPipeRes.source();

            Pipe gPipeRes = Pipe.open();
            Pipe.SinkChannel gResSinkCh = gPipeRes.sink();
            Pipe.SourceChannel gResSourceCh = gPipeRes.source();

            Thread fThread, gThread;

            fThread = new Thread(new FunctionThread(fArgsSourceCh, fResSinkCh, FunctionThread.functionNumber.FUNC_F));
            gThread = new Thread(new FunctionThread(gArgsSourceCh, gResSinkCh, FunctionThread.functionNumber.FUNC_G));

            fThread.setDaemon(true);
            gThread.setDaemon(true);

            System.out.print("Enter x: ");
            Scanner input = new Scanner(System.in);
            int x = input.nextInt();

            ByteBuffer argsBuf = ByteBuffer.allocate(4);
            argsBuf.clear();
            argsBuf.putInt(x);
            argsBuf.flip();
            fArgsSinkCh.write(argsBuf);
            argsBuf.flip();
            gArgsSinkCh.write(argsBuf);

            fThread.start();
            gThread.start();

            Optional<Double> fRes = Optional.empty(), gRes = Optional.empty();

            ByteBuffer fResBuf;
            ByteBuffer gResBuf;
            ByteBuffer sizeBuf = ByteBuffer.allocate(4);

            boolean readFromF = false;
            boolean readFromG = false;

            Keyboard keyboard = new Keyboard();
            //Thread to scan for input from keyboard to handle user cancellation
            Thread cancellator = new Thread(keyboard);
            cancellator.setDaemon(true);
            cancellator.start();

            String fResString = "", gResString = "";

            int lastAttemptF = 0, lastAttemptG = 0;
            boolean cancelledByUser = false;
            while (true) {
                if (keyboard.isCancelled()) {
                    cancelledByUser = true;
                    break;
                }
                if (!readFromF) {
                    if (!fResSinkCh.isBlocking()) {
                        sizeBuf.clear();
                        if (fResSourceCh.read(sizeBuf) > 0) {
                            keyboard.setStartTimeGeneral(Instant.now());
                            if (keyboard.isAttemptToCancel()) {
                                keyboard.setAttemptToCancel(false);
                                System.out.println("Overriden by system");
                                System.out.println("If you want to stop program, enter d(one, finish)");
                            }

                            sizeBuf.flip();
                            int size = sizeBuf.getInt();

                            fResBuf = ByteBuffer.allocate(size);
                            fResBuf.clear();
                            fResSourceCh.read(fResBuf);
                            fResBuf.flip();

                            if (size == 8) {
                                Double res = fResBuf.getDouble();
                                fRes = Optional.of(res);
                                System.out.printf("f returned %f with %d attempts%n", fRes.get(), lastAttemptF);
                                readFromF = true;
                            } else {
                                String result = "";
                                fRes = Optional.empty();
                                while (fResBuf.hasRemaining()) {
                                    result += (char) fResBuf.get();
                                }
                                fResString = result;
                                lastAttemptF++;
                                if (fResString.contains("Maximum")) {
                                    System.out.println(fResString);
                                    readFromF = true;
                                }
                            }
                        }
                    }
                }
                if (!readFromG) {
                    if (!gResSinkCh.isBlocking()) {
                        sizeBuf.clear();
                        if (gResSourceCh.read(sizeBuf) > 0) {
                            keyboard.setStartTimeGeneral(Instant.now());
                            if (keyboard.isAttemptToCancel()) {
                                keyboard.setAttemptToCancel(false);
                                System.out.println("overriden by system");
                                System.out.println("If you want to stop program, enter d(one, finish)");
                            }

                            sizeBuf.flip();
                            int size = sizeBuf.getInt();

                            gResBuf = ByteBuffer.allocate(size);
                            gResBuf.clear();
                            gResSourceCh.read(gResBuf);
                            gResBuf.flip();

                            if (size == 8) {
                                Double res = gResBuf.getDouble();
                                gRes = Optional.of(res);
                                System.out.printf("g returned %f %d attempts%n", gRes.get(), lastAttemptG);
                                readFromG = true;
                            } else {
                                String result = "";
                                gRes = Optional.empty();
                                while (gResBuf.hasRemaining()) {
                                    result += (char) gResBuf.get();
                                }
                                gResString = result;
                                lastAttemptG++;
                                if (gResString.contains("Maximum")) {
                                    System.out.println(gResString);
                                    readFromG = true;
                                }
                            }
                        }
                    }
                }
                if (readFromF && readFromG) {
                    break;
                }
            }

            if (fRes.isPresent() && gRes.isPresent()) {
                System.out.printf("Result of f(%d) * g(%d) = %f%n", x, x, fRes.get() * gRes.get());
            } else if (!cancelledByUser) {
                if (!fResString.equals("")&&!fResString.contains("Maximum")) {
                    System.out.println(fResString);
                }
                if (!gResString.equals("")&&!gResString.contains("Maximum")) {
                    System.out.println(gResString);
                }
            }

            keyboard.setDone();

            System.out.println("Want to try again with another argument?\ny(es)/n(o)");
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String ans = scanner.nextLine();
                if (ans.equals("n")) {
                    System.exit(0);
                }
                //TODO fix "Need to press y twice"
                if (ans.equals("y")) {
                    break;
                }
            }
        }
    }
}
