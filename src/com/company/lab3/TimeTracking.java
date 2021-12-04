package com.company.lab3;

public class TimeTracking {
    public static void main(String[] args) {
        MemoryManagement.main(new String[]{"src/com/company/lab3/commandsFIFO", "src/com/company/lab3/memoryFIFO.conf", "false"});
        MemoryManagement.runSimulation();

        MemoryManagement.main(new String[]{"src/com/company/lab3/commandsAging", "src/com/company/lab3/memoryAging.conf", "true"});
        MemoryManagement.runSimulation();
    }
}
