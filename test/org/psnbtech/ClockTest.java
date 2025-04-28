package org.psnbtech;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class ClockTest {
    /**
     * Tests a single cycle elapsed for Clock cycles. Assert true if the clock
     * has at least once cycle, and assert false after it was consumed.
     */
    @Test
    void testSingleCycle() throws InterruptedException {
        Clock c = new Clock(100);        // 10 ms per cycle
        Thread.sleep(15);
        c.update();
        assertTrue(c.hasElapsedCycle());
        assertFalse(c.hasElapsedCycle());   // consumed the only cycle
    }

    /**
     * Tests the pausing of the clock. Assert False if there are cycles still
     * existing, and Assert True if the Clock was paused.
     */
    @Test
    void testPauseStopsCounting() throws InterruptedException {
        Clock c = new Clock(50);         // 20 ms per cycle
        c.setPaused(true);
        Thread.sleep(30);
        c.update();
        assertFalse(c.peekElapsedCycle());
        assertTrue(c.isPaused());
    }

    /**
     * This extends on the previous test and checks whether or not the clock
     * still runs properly after being paused. This asserts False on no cycles
     * present after pausing. Then asserts True on cycles present after resuming.
     */
    @Test
    void testResumeAfterPause() throws InterruptedException {
        Clock c = new Clock(20);         // 50 ms per cycle
        c.setPaused(true);
        Thread.sleep(60);
        c.update();                      // still paused
        assertFalse(c.peekElapsedCycle());

        c.setPaused(false);
        Thread.sleep(60);
        c.update();                      // now running
        assertTrue(c.peekElapsedCycle());  // at least one cycle elapsed
    }

    /**
     * Reveals bug: cyclesPerSecond == 0 leaves millisPerCycle == Infinity
     * so the clock never ticks. We expect an exception, but none is thrown.
     */
    @Test
    void testZeroCyclesPerSecondBug() throws InterruptedException {
        Clock c = new Clock(0);          // should be invalid
        Thread.sleep(40);
        c.update();
        assertFalse(c.peekElapsedCycle(),
                "Bug: clock created with 0 CPS should not silently ignore cycles");
    }
}