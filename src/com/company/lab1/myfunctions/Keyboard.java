package com.company.lab1.myfunctions;

import java.time.Duration;
import java.time.Instant;

public class Keyboard implements Runnable {
    private volatile boolean isCancelled = false;
    private volatile boolean attemptToCancel = false;
    private long timeFrame = 5000;
    private long maxTime = 10000;
    private Instant startTimeGeneral = Instant.now();
    private Instant endTimeGeneral;
    private boolean done = false;

    public void setDone() {
        this.done = true;
    }

    public boolean isCancelled() {
        return isCancelled;
    }

    public boolean isAttemptToCancel() {
        return attemptToCancel;
    }

    public void setAttemptToCancel(boolean attemptToCancel) {
        this.attemptToCancel = attemptToCancel;
    }

    public void setStartTimeGeneral(Instant startTimeGeneral) {
        this.startTimeGeneral = startTimeGeneral;
    }

    @Override
    public void run() {
        String waiting = "If you want to stop program, enter d(one, finish)";
        System.out.println(waiting);
        Instant startTime = Instant.now();
        Instant endTime;

        InputFromUser input = new InputFromUser();
        Thread inputThread = new Thread(input);
        inputThread.setDaemon(true);
        inputThread.start();

        boolean hasWritten = false;

        while (!done) {
            if(done){
                inputThread.interrupt();
            }

            endTimeGeneral = Instant.now();
            long timeTaken = Duration.between(startTimeGeneral, endTimeGeneral).toMillis();
            if(timeTaken >= maxTime && !hasWritten){
                System.out.printf("Computation is taking too long (more than %d milliseconds), stop application?%ny(es)/n(o)%n", maxTime);
                hasWritten = true;
            }

            if (attemptToCancel) {
                endTime = Instant.now();
                long interval = Duration.between(startTime, endTime).toMillis();
                if (interval >= timeFrame) {
                    System.out.printf("action is not confirmed within %d milliseconds, proceeding...%n", timeFrame);
                    System.out.println(waiting);
                    attemptToCancel = false;
                    continue;
                }
            }

            if (input.isInput()) {
                String inputString = input.getInputString();
                if (!attemptToCancel && inputString.equals("d")) {
                    attemptToCancel = true;
                    System.out.println("Please, confirm that computation should be stopped y(es, stop)/n(ot yet).");
                    startTime = Instant.now();
                } else if (attemptToCancel) {
                    if (inputString.equals("y")) {
                        System.out.println("Cancelled by user!");
                        isCancelled = true;
                        attemptToCancel = false;
                        return;
                    } else {
                        System.out.println("action is denied, proceeding...");
                        System.out.println(waiting);
                        attemptToCancel = false;
                    }
                } else if (hasWritten && inputString.equals("y")){
                    System.out.println("Cancelled by user!");
                    isCancelled = true;
                    attemptToCancel = false;
                    return;
                }
            }
        }
    }
}
