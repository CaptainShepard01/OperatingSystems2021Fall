package com.company.lab3;

import java.io.*;
import java.util.*;

public class Kernel extends Thread {
    // The number of virtual pages must be fixed at 64 due to
    // dependencies in the GUI
    private static int virtualPageNumber = 64;
    private static int physicalPageNumber = 32;

    private final boolean isAgingAlgorithm;

    private String output = null;
    private int pageFaultCount = 0;
    private static final String lineSeparator =
            System.getProperty("line.separator");
    private String command_file;
    private String config_file;
    private ControlPanel controlPanel;
    private final Vector<Page> memVector = new Vector();
    private final Vector<Instruction> instructVector = new Vector();
    private int numberOfTicks;
    private final Vector<Integer> pageUsageVector = new Vector();
    private int pageUsed;
    private final Set<Integer> physicalMapped = new HashSet<>();
    private boolean doStdoutLog = false;
    private boolean doFileLog = false;
    public int runs;
    public int runcycles;   //number of instructions to run
    public long block = (int) Math.pow(2, 12);
    public static byte addressradix = 10;

    private int sleepTime = 500;
    private int clockTick = 2;

    public Kernel(boolean isAgingAlgorithm) {
        this.isAgingAlgorithm = isAgingAlgorithm;
    }

    public void init(String commands, String config) {
        File f;
        command_file = commands;
        config_file = config;
        String line;
        String tmp = null;
        String command = "";
        byte R = 0;
        byte M = 0;
        int i;
        int j;
        int id;
        int physical;
        int physical_count = 0;
        int inMemTime;
        int lastTouchTime;
        int map_count = 0;
        double power;
        long high;
        long low;
        long addr;
        long address_limit = (block * virtualPageNumber) - 1;

        if (config != null) {
            f = new File(config);
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("numpages")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            virtualPageNumber = Common.s2i(st.nextToken());
                            if (virtualPageNumber < 2 || virtualPageNumber > 64) {
                                System.out.println("MemoryManagement: numpages out of bounds.");
                                System.exit(-1);
                            }
                            address_limit = (block * virtualPageNumber) - 1;
                        }
                    }
                    if (line.startsWith("numphysical")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            physicalPageNumber = Common.s2i(st.nextToken());
                            if (physicalPageNumber < 2 || physicalPageNumber > 64) {
                                System.out.println("MemoryManagement: numphysical out of bounds.");
                                System.exit(-1);
                            }
                        }
                    }
                    if (line.startsWith("numticks")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            st.nextToken();
                            numberOfTicks = Common.s2i(st.nextToken());
                            pageUsed = Integer.parseInt("1" + "0".repeat(numberOfTicks - 1), 2);
                        }
                    }
                    if (line.startsWith("sleeptime")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            st.nextToken();
                            sleepTime = Common.s2i(st.nextToken());
                        }
                    }
                    if (line.startsWith("clocktick")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            st.nextToken();
                            clockTick = Common.s2i(st.nextToken());
                        }
                    }
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }

            //initialize virtual pages not mapped to physical
            for (i = 0; i < virtualPageNumber; i++) {
                high = (block * (i + 1)) - 1;
                low = block * i;
                memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
                StringBuilder usageStringBuilder = new StringBuilder("0".repeat(Math.max(0, numberOfTicks)));

                pageUsageVector.addElement(Integer.parseInt("0".repeat(numberOfTicks), 2));
            }

            //initialize actual mapping
            f = new File(config);
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("memset")) {
                        StringTokenizer st = new StringTokenizer(line);
                        st.nextToken();
                        while (st.hasMoreTokens()) {
                            id = Common.s2i(st.nextToken());
                            tmp = st.nextToken();
                            if (tmp.startsWith("x")) {
                                physical = -1;
                            } else {
                                physical = Common.s2i(tmp);
                            }
                            if ((0 > id || id >= virtualPageNumber) || (-1 > physical || physical >= physicalPageNumber)) {
                                System.out.println("MemoryManagement: Invalid page value in " + config);
                                System.exit(-1);
                            }
                            R = Common.s2b(st.nextToken());
                            if (R < 0 || R > 1) {
                                System.out.println("MemoryManagement: Invalid R value in " + config);
                                System.exit(-1);
                            }
                            M = Common.s2b(st.nextToken());
                            if (M < 0 || M > 1) {
                                System.out.println("MemoryManagement: Invalid M value in " + config);
                                System.exit(-1);
                            }
                            inMemTime = Common.s2i(st.nextToken());
                            if (inMemTime < 0) {
                                System.out.println("MemoryManagement: Invalid inMemTime in " + config);
                                System.exit(-1);
                            }
                            lastTouchTime = Common.s2i(st.nextToken());
                            if (lastTouchTime < 0) {
                                System.out.println("MemoryManagement: Invalid lastTouchTime in " + config);
                                System.exit(-1);
                            }
                            Page page = memVector.elementAt(id);
                            if (R != 0 || M != 0) {
                                pageUsageVector.set(id, Integer.parseInt("0".repeat(numberOfTicks), 2));
                            }
                            page.physical = physical;
                            page.R = R;
                            page.M = M;
                            page.inMemTime = inMemTime;
                            page.lastTouchTime = lastTouchTime;
                        }
                    }
                    if (line.startsWith("enable_logging")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            if (st.nextToken().startsWith("true")) {
                                doStdoutLog = true;
                            }
                        }
                    }
                    if (line.startsWith("log_file")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                        }
                        if (tmp.startsWith("log_file")) {
                            doFileLog = false;
                            output = "tracefile";
                        } else {
                            doFileLog = true;
                            doStdoutLog = false;
                            output = tmp;
                        }
                    }
                    if (line.startsWith("pagesize")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            st.nextToken();
                            tmp = st.nextToken();
                            if (tmp.startsWith("power")) {
                                power = Integer.parseInt(st.nextToken());
                                block = (int) Math.pow(2, power);
                            } else {
                                block = Long.parseLong(tmp, 10);
                            }
                            address_limit = (block * virtualPageNumber) - 1;
                        }
                        if (block < 64 || block > Math.pow(2, 26)) {
                            System.out.println("MemoryManagement: pagesize is out of bounds");
                            System.exit(-1);
                        }
                        for (i = 0; i < virtualPageNumber; i++) {
                            Page page = memVector.elementAt(i);
                            page.high = (block * (i + 1)) - 1;
                            page.low = block * i;
                        }
                    }
                    if (line.startsWith("addressradix")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            st.nextToken();
                            tmp = st.nextToken();
                            addressradix = Byte.parseByte(tmp);
                            if (addressradix < 0 || addressradix > 20) {
                                System.out.println("MemoryManagement: addressradix out of bounds.");
                                System.exit(-1);
                            }
                        }
                    }
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }
        }

        //generating instructions vector
        f = new File(commands);
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(f));
            while ((line = in.readLine()) != null) {
                if (line.startsWith("READ") || line.startsWith("WRITE")) {
                    if (line.startsWith("READ")) {
                        command = "READ";
                    }
                    if (line.startsWith("WRITE")) {
                        command = "WRITE";
                    }
                    StringTokenizer st = new StringTokenizer(line);
                    st.nextToken();
                    tmp = st.nextToken();
                    if (tmp.startsWith("random")) {
                        instructVector.addElement(new Instruction(command, Common.randomLong(address_limit)));
                    } else {
                        if (tmp.startsWith("bin")) {
                            addr = Long.parseLong(st.nextToken(), 2);
                        } else if (tmp.startsWith("oct")) {
                            addr = Long.parseLong(st.nextToken(), 8);
                        } else if (tmp.startsWith("hex")) {
                            addr = Long.parseLong(st.nextToken(), 16);
                        } else {
                            addr = Long.parseLong(tmp);
                        }
                        if (0 > addr || addr > address_limit) {
                            System.out.println("MemoryManagement: " + addr + ", Address out of range in " + commands);
                            System.exit(-1);
                        }
                        instructVector.addElement(new Instruction(command, addr));
                    }
                }
            }
            in.close();
        } catch (IOException e) { /* Handle exceptions */ }

        runcycles = instructVector.size();
        if (runcycles < 1) {
            System.out.println("MemoryManagement: no instructions present for execution.");
            System.exit(-1);
        }
        if (doFileLog) {
            File trace = new File(output);
            trace.delete();
        }
        runs = 0;
        for (i = 0; i < virtualPageNumber; i++) {
            Page page = memVector.elementAt(i);
            if (page.physical != -1) {
                map_count++;
                physicalMapped.add(page.physical);
            }
            for (j = 0; j < virtualPageNumber; j++) {
                Page tmp_page = memVector.elementAt(j);
                if (tmp_page.physical == page.physical && page.physical >= 0) {
                    physical_count++;
                }
            }
            if (physical_count > 1) {
                System.out.println("MemoryManagement: Duplicate physical page's in " + config);
                System.exit(-1);
            }
            physical_count = 0;
        }

        //not sure if it is a great practice to map virtual pages in count of physical from the start
        if (map_count < physicalPageNumber) {
            for (i = 0; i < virtualPageNumber; i++) {
                Page page = memVector.elementAt(i);
                if (page.physical == -1 && map_count < physicalPageNumber) {
                    for (int k = 0; k < physicalPageNumber; ++k) {
                        if (!physicalMapped.contains(k)) {
                            page.physical = k;
                            physicalMapped.add(k);
                            controlPanel.addPhysicalPage(page.id, k);
                            break;
                        }
                    }
                }
            }
        }
        for (i = 0; i < virtualPageNumber; i++) {
            Page page = memVector.elementAt(i);
            if (page.physical == -1) {
                controlPanel.removePhysicalPage(i);
            } else {
                controlPanel.addPhysicalPage(i, page.physical);
            }
        }
        for (i = 0; i < instructVector.size(); i++) {
            Instruction instruct = instructVector.elementAt(i);
            if (instruct.addr < 0 || instruct.addr > address_limit) {
                System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " + instruct.addr + ") out of bounds.");
                System.exit(-1);
            }
        }
    }

    public void setControlPanel(ControlPanel newControlPanel) {
        controlPanel = newControlPanel;
    }

    public void getPage(int pageNum) {
        Page page = memVector.elementAt(pageNum);
        controlPanel.paintPage(page);
    }

    private void printLogFile(String message) {
        String line;
        StringBuilder temp = new StringBuilder();

        File trace = new File(output);
        if (trace.exists()) {
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(output));
                while ((line = in.readLine()) != null) {
                    temp.append(line).append(lineSeparator);
                }
                in.close();
            } catch (IOException e) {
                System.out.println("Error    >>" + e.getMessage());
            }
        }
        try {
            PrintStream out = new PrintStream(new FileOutputStream(output));
            out.print(temp);
            out.print(message);
            out.close();
        } catch (IOException e) {
            System.out.println("Error    >>" + e.getMessage());
        }
    }

    public void run() {
        step();
        while (runs != runcycles) {
            try {
                Thread.sleep(sleepTime);
            } catch (InterruptedException e) {
                System.out.println("Error    >>" + e.getMessage());
            }
            step();
        }
        printLogFile("\nNumber of page faults: " + pageFaultCount);
    }

    private void logInstruction(Instruction instruction, String status) {
        if (instruction.inst.startsWith("READ")) {
            log(instruction, "READ", status);
        } else if (instruction.inst.startsWith("WRITE")) {
            log(instruction, "WRITE", status);
        }
    }

    private void log(Instruction instruction, String operation, String status) {
        if (doFileLog) {
            printLogFile(operation + " " + Long.toString(instruction.addr, addressradix) + " ... " + status);
        }
        if (doStdoutLog) {
            System.out.println(operation + " " + Long.toString(instruction.addr, addressradix) + " ... " + status);
        }
    }

    int currentTick = 0;

    public void step() {
        Instruction instruct = instructVector.elementAt(runs);
        controlPanel.instructionValueLabel.setText(instruct.inst);
        controlPanel.addressValueLabel.setText(Long.toString(instruct.addr, addressradix));
        int numberOfPage = Address2Page.pageNum(instruct.addr, virtualPageNumber, block);
        getPage(numberOfPage);
        if (Objects.equals(controlPanel.pageFaultValueLabel.getText(), "YES")) {
            controlPanel.pageFaultValueLabel.setText("NO");
        }

        Integer currentUsage = pageUsageVector.get(numberOfPage);

        String instructionString = instruct.inst;

        Page page = memVector.elementAt(numberOfPage);

        if (page.physical == -1) {
            if (physicalMapped.size() == physicalPageNumber) {
                replacePage(numberOfPage);
                controlPanel.pageFaultValueLabel.setText("YES");

            } else {
                for (int k = 0; k < physicalPageNumber; ++k) {
                    if (!physicalMapped.contains(k)) {
                        page.physical = k;
                        physicalMapped.add(k);
                        controlPanel.addPhysicalPage(page.id, k);
                        break;
                    }
                }
            }
            logInstruction(instruct, "page fault");
            pageFaultCount++;
        } else {
            page.lastTouchTime = 0;


            logInstruction(instruct, "okay");
        }

        //in order not to add extra 1 highest bit
        if (currentUsage < pageUsed) {
            currentUsage += pageUsed;
            pageUsageVector.set(numberOfPage, currentUsage);
        }

        page.R = 1;
        if (instructionString.startsWith("WRITE")) {
            page.M = 1;
        }

        for (int i = 0; i < virtualPageNumber; i++) {
            Page otherPage = memVector.elementAt(i);
            if (currentTick == clockTick) {
                if (otherPage.R == 1 && otherPage.lastTouchTime == 10) {
                    otherPage.R = 0;
                }
                currentUsage = pageUsageVector.get(i);
                currentUsage = currentUsage >> 1;
                pageUsageVector.set(i, currentUsage);
            }
            if (otherPage.physical != -1) {
                otherPage.inMemTime = otherPage.inMemTime + 10;
                otherPage.lastTouchTime = otherPage.lastTouchTime + 10;
            }
        }

        if (currentTick == clockTick) {
            currentTick = 0;
        }

        controlPanel.timeValueLabel.setText(runs * 10 + " (ns)");
        runs++;
    }

    private void replacePage(int numberOfPage) {
        if (isAgingAlgorithm)
            PageFaultAging.replacePage(memVector, pageUsageVector, numberOfTicks, numberOfPage, controlPanel);
        else
            PageFaultFIFO.replacePage(memVector, virtualPageNumber, numberOfPage, controlPanel);
    }

    public void reset() {
        memVector.removeAllElements();
        instructVector.removeAllElements();
        controlPanel.statusValueLabel.setText("STOP");
        controlPanel.timeValueLabel.setText("0");
        controlPanel.instructionValueLabel.setText("NONE");
        controlPanel.addressValueLabel.setText("NULL");
        controlPanel.pageFaultValueLabel.setText("NO");
        controlPanel.virtualPageValueLabel.setText("x");
        controlPanel.physicalPageValueLabel.setText("0");
        controlPanel.RValueLabel.setText("0");
        controlPanel.MValueLabel.setText("0");
        controlPanel.inMemTimeValueLabel.setText("0");
        controlPanel.lastTouchTimeValueLabel.setText("0");
        controlPanel.lowValueLabel.setText("0");
        controlPanel.highValueLabel.setText("0");
        init(command_file, config_file);
    }
}
