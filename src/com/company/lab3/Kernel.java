package com.company.lab3;

import java.io.*;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

public class Kernel extends Thread {
    // The number of virtual pages must be fixed at 63 due to
    // dependencies in the GUI
    private static int virtPageNum = 63;
    private static int physicalPageNum = 32;

    private String output = null;
    private static final String lineSeparator =
            System.getProperty("line.separator");
    private String command_file;
    private String config_file;
    private ControlPanel controlPanel;
    private Vector memVector = new Vector();
    private Vector instructVector = new Vector();
    private int numberOfTicks;
    private Vector pageUsageVector = new Vector();
    private int pageUsed;
    private Set<Integer> physicalMapped = new HashSet<>();
    private String status;
    private boolean doStdoutLog = false;
    private boolean doFileLog = false;
    public int runs;
    public int runcycles;   //number of instructions to run
    public long block = (int) Math.pow(2, 12);
    public static byte addressradix = 10;

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
        long address_limit = (block * virtPageNum + 1) - 1;

        if (config != null) {
            f = new File(config);
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(f));
                while ((line = in.readLine()) != null) {
                    if (line.startsWith("numpages")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            virtPageNum = Common.s2i(st.nextToken()) - 1;
                            if (virtPageNum < 2 || virtPageNum > 63) {
                                System.out.println("MemoryManagement: numpages out of bounds.");
                                System.exit(-1);
                            }
                            address_limit = (block * virtPageNum + 1) - 1;
                        }
                    }
                    if (line.startsWith("numphysical")) {
                        StringTokenizer st = new StringTokenizer(line);
                        while (st.hasMoreTokens()) {
                            tmp = st.nextToken();
                            physicalPageNum = Common.s2i(st.nextToken()) - 1;
                            if (physicalPageNum < 2 || physicalPageNum > 63) {
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
                }
                in.close();
            } catch (IOException e) { /* Handle exceptions */ }

            //initialize virtual pages not mapped to physical
            for (i = 0; i <= virtPageNum; i++) {
                high = (block * (i + 1)) - 1;
                low = block * i;
                memVector.addElement(new Page(i, -1, R, M, 0, 0, high, low));
                StringBuilder usageStringBuilder = new StringBuilder("0".repeat(Math.max(0, numberOfTicks)));

                pageUsageVector.addElement(Integer.parseInt("0".repeat(numberOfTicks), 2));

                /*Integer.toBinaryString(i);*/
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
                            if ((0 > id || id > virtPageNum) || (-1 > physical || physical > physicalPageNum)) {
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
                            Page page = (Page) memVector.elementAt(id);
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
                            address_limit = (block * virtPageNum + 1) - 1;
                        }
                        if (block < 64 || block > Math.pow(2, 26)) {
                            System.out.println("MemoryManagement: pagesize is out of bounds");
                            System.exit(-1);
                        }
                        for (i = 0; i <= virtPageNum; i++) {
                            Page page = (Page) memVector.elementAt(i);
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
        for (i = 0; i < virtPageNum; i++) {
            Page page = (Page) memVector.elementAt(i);
            if (page.physical != -1) {
                map_count++;
                physicalMapped.add(page.physical);
            }
            for (j = 0; j < virtPageNum; j++) {
                Page tmp_page = (Page) memVector.elementAt(j);
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
        /*if (map_count < physicalPageNum) {
            for (i = 0; i < virtPageNum; i++) {
                Page page = (Page) memVector.elementAt(i);
                if (page.physical == -1 && map_count < physicalPageNum) {
                    for(int k = 0;k<(virtPageNum+1)/2;++k){
                        if(!physicalMapped.contains(k)){
                            page.physical = k;
                            physicalMapped.add(k);
                            map_count++;
                            break;
                        }
                    }
                }
            }
        }*/
        for (i = 0; i < virtPageNum; i++) {
            Page page = (Page) memVector.elementAt(i);
            if (page.physical == -1) {
                controlPanel.removePhysicalPage(i);
            } else {
                controlPanel.addPhysicalPage(i, page.physical);
            }
        }
        for (i = 0; i < instructVector.size(); i++) {
            high = block * virtPageNum;
            Instruction instruct = (Instruction) instructVector.elementAt(i);
            if (instruct.addr < 0 || instruct.addr > high) {
                System.out.println("MemoryManagement: Instruction (" + instruct.inst + " " + instruct.addr + ") out of bounds.");
                System.exit(-1);
            }
        }
    }

    public void setControlPanel(ControlPanel newControlPanel) {
        controlPanel = newControlPanel;
    }

    public void getPage(int pageNum) {
        Page page = (Page) memVector.elementAt(pageNum);
        controlPanel.paintPage(page);
    }

    private void printLogFile(String message) {
        String line;
        String temp = "";

        File trace = new File(output);
        if (trace.exists()) {
            try {
                DataInputStream in = new DataInputStream(new FileInputStream(output));
                while ((line = in.readLine()) != null) {
                    temp = temp + line + lineSeparator;
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
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                System.out.println("Error    >>" + e.getMessage());
            }
            step();
        }
    }

    public void step() {
        int i = 0;

        Instruction instruct = (Instruction) instructVector.elementAt(runs);
        controlPanel.instructionValueLabel.setText(instruct.inst);
        controlPanel.addressValueLabel.setText(Long.toString(instruct.addr, addressradix));
        int numberOfPage = Virtual2Physical.pageNum(instruct.addr, virtPageNum, block);
        getPage(numberOfPage);
        if (controlPanel.pageFaultValueLabel.getText() == "YES") {
            controlPanel.pageFaultValueLabel.setText("NO");
        }

        Integer previousUsage = (Integer) pageUsageVector.get(numberOfPage);
        Integer currentUsage = previousUsage >> 1;

        if (instruct.inst.startsWith("READ")) {
            Page page = (Page) memVector.elementAt(numberOfPage);
            if (page.physical == -1) {
                if (physicalMapped.size() == physicalPageNum) {
                    if (doFileLog) {
                        printLogFile("READ " + Long.toString(instruct.addr, addressradix) + " ... page fault");
                    }
                    if (doStdoutLog) {
                        System.out.println("READ " + Long.toString(instruct.addr, addressradix) + " ... page fault");
                    }
                    PageFault.replacePage(memVector, pageUsageVector, numberOfTicks, virtPageNum, numberOfPage, controlPanel);
                    controlPanel.pageFaultValueLabel.setText("YES");
                } else {
                    for (int k = 0; k < physicalPageNum; ++k) {
                        if (!physicalMapped.contains(k)) {
                            page.physical = k;
                            physicalMapped.add(k);
                            break;
                        }
                    }

                    if (doFileLog) {
                        printLogFile("READ " + Long.toString(instruct.addr, addressradix) + " ... okay");
                    }
                    if (doStdoutLog) {
                        System.out.println("READ " + Long.toString(instruct.addr, addressradix) + " ... okay");
                    }
                }

            } else {
                page.R = 1;
                page.lastTouchTime = 0;

                currentUsage += pageUsed;
                pageUsageVector.set(numberOfPage, currentUsage);

                if (doFileLog) {
                    printLogFile("READ " + Long.toString(instruct.addr, addressradix) + " ... okay");
                }
                if (doStdoutLog) {
                    System.out.println("READ " + Long.toString(instruct.addr, addressradix) + " ... okay");
                }
            }
        }
        if (instruct.inst.startsWith("WRITE")) {
            Page page = (Page) memVector.elementAt(numberOfPage);
            if (page.physical == -1) {
                if (physicalMapped.size() == physicalPageNum) {
                    if (doFileLog) {
                        printLogFile("WRITE " + Long.toString(instruct.addr, addressradix) + " ... page fault");
                    }
                    if (doStdoutLog) {
                        System.out.println("WRITE " + Long.toString(instruct.addr, addressradix) + " ... page fault");
                    }
                    PageFault.replacePage(memVector, pageUsageVector, numberOfTicks, virtPageNum, numberOfPage, controlPanel);
                    controlPanel.pageFaultValueLabel.setText("YES");
                } else {
                    for (int k = 0; k < physicalPageNum; ++k) {
                        if (!physicalMapped.contains(k)) {
                            page.physical = k;
                            physicalMapped.add(k);
                            break;
                        }
                    }

                    if (doFileLog) {
                        printLogFile("WRITE " + Long.toString(instruct.addr, addressradix) + " ... okay");
                    }
                    if (doStdoutLog) {
                        System.out.println("WRITE " + Long.toString(instruct.addr, addressradix) + " ... okay");
                    }
                }
            } else {
                page.M = 1;
                page.lastTouchTime = 0;

                currentUsage += pageUsed;
                pageUsageVector.set(numberOfPage, currentUsage);

                if (doFileLog) {
                    printLogFile("WRITE " + Long.toString(instruct.addr, addressradix) + " ... okay");
                }
                if (doStdoutLog) {
                    System.out.println("WRITE " + Long.toString(instruct.addr, addressradix) + " ... okay");
                }
            }
        }
        for (i = 0; i < virtPageNum; i++) {
            Page page = (Page) memVector.elementAt(i);
            if (page.R == 1 && page.lastTouchTime == 10) {
                page.R = 0;
            }
            if (page.physical != -1) {
                page.inMemTime = page.inMemTime + 10;
                page.lastTouchTime = page.lastTouchTime + 10;
            }
        }
        runs++;
        controlPanel.timeValueLabel.setText(Integer.toString(runs * 10) + " (ns)");
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
