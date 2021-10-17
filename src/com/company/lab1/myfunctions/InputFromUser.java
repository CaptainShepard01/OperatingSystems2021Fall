package com.company.lab1.myfunctions;

import java.util.Scanner;

public class InputFromUser implements Runnable {
    private volatile boolean isInput = false;
    private volatile String inputString = "";

    public boolean isInput() {
        return isInput;
    }

    public String getInputString() {
        isInput = false;
        return inputString;
    }

    @Override
    public void run() {
        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (!isInput) {
                inputString = scanner.nextLine();
                isInput = true;
            }
        }
    }
}
