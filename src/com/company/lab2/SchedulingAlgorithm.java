package com.company.lab2;

// Run() is called from Scheduling.main() and is where
// the scheduling algorithm written by the user resides.
// User modification should occur within the Run() function.

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Vector;

public class SchedulingAlgorithm {

    public static Results Run(int runtime, Vector userVector, Results result, Vector proportions, int quantum) {
        int comptime = 0;
        int size = 0;
        int completed = 0;
        String resultsFile = "Summary-Processes";

        Vector<Integer> indexes = new Vector<>();
        Vector<Integer> endedProcesses = new Vector<>();
        Vector<Vector<Boolean>> completedProcesses = new Vector<>();

        for (int j = 0; j < userVector.size(); ++j) {
            indexes.add(0);
            size += ((Vector) userVector.get(j)).size();
            endedProcesses.add(0);
            completedProcesses.add(new Vector<>());
            for (int k = 0; k < ((Vector) userVector.get(j)).size(); ++k) {
                Vector<Boolean> vector = completedProcesses.get(j);
                vector.add(false);
            }
        }

        result.schedulingType = "Interactive";
        result.schedulingName = "Fair-Share Scheduling";
        try {
            //BufferedWriter out = new BufferedWriter(new FileWriter(resultsFile));
            //OutputStream out = new FileOutputStream(resultsFile);
            PrintStream out = new PrintStream(new FileOutputStream(resultsFile));

            while (comptime < runtime) {
                for (int j = 0; j < userVector.size(); j++) {
                    for (int k = 0; k < (int) proportions.get(j); k++) {
                        Vector vector = (Vector) userVector.elementAt(j);
                        int index = indexes.get(j);
                        Vector<Boolean> currentCompleted = completedProcesses.get(j);
                        int newIndex = (index + 1) % vector.size();
                        indexes.set(j, newIndex);
                        if(currentCompleted.get(index)){
                            k--;
                            continue;
                        }
                        sProcess process = (sProcess) (vector.get(index));
                        printProcessInfo(out, j, index, process, "registered");

                        while (true) {
                            if (comptime >= runtime) {
                                return result;
                            }
                            if (process.cpudone == process.cputime) {
                                endedProcesses.set(j, endedProcesses.get(j) + 1);
                                Vector<Boolean> currentUserVector = completedProcesses.get(j);
                                currentUserVector.set(index, true);
                                if (endedProcesses.get(j) == vector.size()) {
                                    proportions.set(j, 0);
                                }
                                completed++;
                                printProcessInfo(out, j, index, process, "completed");
                                if (completed == size) {
                                    result.compuTime = comptime;
                                    out.close();
                                    return result;
                                }
                                break;
                            }
                            if(process.ionext == process.currentQuantum){
                                printProcessInfo(out, j, index, process, "quantum ended");
                                process.numblocked++;
                                process.currentQuantum += quantum;
                                break;
                            }
                            if (process.ioblocking == process.ionext) {
                                printProcessInfo(out, j, index, process, "I/O blocked");
                                process.numblocked++;
                                process.ionext = 0;
                                process.currentQuantum = quantum;
                                break;
                            }

                            process.cpudone++;
                            if (process.ioblocking > 0) {
                                process.ionext++;
                            }
                        }
                    }
                }

                comptime++;
            }

            out.close();
        } catch (IOException e) {
            //Handle exceptions
        }
        result.compuTime = comptime;
        return result;
    }

    private static void printProcessInfo(PrintStream out, int j, int index, sProcess process, String info) {
        out.println("User: " + j + " Process: " + index + " " + info + "... (" + process.cputime + " " + process.ioblocking + " " + process.cpudone + " " + process.ionext + ")");
    }
}
