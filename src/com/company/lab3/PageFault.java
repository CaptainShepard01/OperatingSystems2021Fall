/* It is in this file, specifically the replacePage function that will
   be called by MemoryManagement when there is a page fault.  The 
   users of this program should rewrite PageFault to implement the 
   page replacement algorithm.
*/

// This PageFault file is an example of the FIFO Page Replacement
// Algorithm as described in the Memory Management section.
package com.company.lab3;

import java.util.Vector;

public class PageFault {

    /**
     * The page replacement algorithm for the memory management simulator.
     * This method gets called whenever a page needs to be replaced.
     * <p>
     * The page replacement algorithm included with the simulator is
     * FIFO (first-in first-out).  A while or for loop should be used
     * to search through the current memory contents for a canidate
     * replacement page.  In the case of FIFO the while loop is used
     * to find the proper page while making sure that virtualPageNum is
     * not exceeded.
     * <pre>
     *   Page page = ( Page ) mem.elementAt( oldestPage )
     * </pre>
     * This line brings the contents of the Page at oldestPage (a
     * specified integer) from the mem vector into the page object.
     * Next recall the contents of the target page, newPageNum.
     * Set the physical memory address of the page to be added equal
     * to the page to be removed.
     * <pre>
     *   controlPanel.removePhysicalPage( oldestPage )
     * </pre>
     * Once a page is removed from memory it must also be reflected
     * graphically.  This line does so by removing the physical page
     * at the oldestPage value.  The page which will be added into
     * memory must also be displayed through the addPhysicalPage
     * function call.  One must also remember to reset the values of
     * the page which has just been removed from memory.
     *
     * @param mem            is the vector which contains the contents of the pages
     *                       in memory being simulated.  mem should be searched to find the
     *                       proper page to remove, and modified to reflect any changes.
     * @param pageUsage      is the vector which contains the last numberOfTicks bits of virtual pages usage
     * @param numberOfTicks  is how many bits of virtual pages usage we store
     * @param virtualPageNum is the number of virtual pages in the
     *                       simulator (set in Kernel.java).
     * @param newPageNum     is the requested page which caused the
     *                       page fault.
     * @param controlPanel   represents the graphical element of the
     *                       simulator, and allows one to modify the current display.
     */
    public static void replacePage(Vector mem, Vector pageUsage, int numberOfTicks, int virtualPageNum, int newPageNum, ControlPanel controlPanel) {
        int LRUPage = -1;
        int leastUsage = Integer.parseInt("1".repeat(numberOfTicks));

        for (int i = 0; i < pageUsage.size(); ++i) {
            Page page = (Page) mem.elementAt(i);
            if (page.physical != -1) {
                if (page.R != 1) {
                    int currentLeastUsage = (Integer) pageUsage.get(i);
                    if (currentLeastUsage < leastUsage) {
                        leastUsage = currentLeastUsage;
                        LRUPage = i;
                    }
                } else {
                    page.R = 0;
                }
            }
        }

        if (LRUPage == -1) {
            LRUPage = 0;
        }

        Page page = (Page) mem.elementAt(LRUPage);
        Page newPage = (Page) mem.elementAt(newPageNum);
        pageUsage.set(LRUPage, Integer.parseInt("0".repeat(numberOfTicks)));
        newPage.physical = page.physical;
        controlPanel.removePhysicalPage(LRUPage);
        controlPanel.addPhysicalPage(newPage.physical, newPageNum);
        page.inMemTime = 0;
        page.lastTouchTime = 0;
        page.R = 0;
        page.M = 0;
        page.physical = -1;
    }
}
