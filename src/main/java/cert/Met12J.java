package cert;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * MET12-J: Do not use finalizers.
 *
 * <p>Owner: Jimmy
 *
 * <p>A finalizer runs only if and when the garbage collector decides to
 * finalize an unreachable object. There is no guarantee about when, or whether,
 * that happens, and an object that is still reachable is never finalized at all.
 * A resource that is released only by a finalizer can therefore stay open
 * indefinitely. Explicit release through {@link AutoCloseable} and
 * try-with-resources runs at a known point, every time.
 *
 * <p><b>Java 17 note:</b> {@code Object.finalize()} has been deprecated since
 * Java 9 and is marked for removal from JDK 18 on (JEP 421). This class
 * intentionally overrides it to show the problem, so <b>compiling it with
 * {@code --release 17} produces a [deprecation] warning</b> (shown by Maven
 * because the pom enables {@code showDeprecation}); the warning is not
 * suppressed on purpose.
 *
 * <p><b>What is deterministic and what is not:</b> Scenario 1 relies on a Java
 * guarantee (reachable objects are never finalized), so its result does not
 * vary. Scenario 2 depends on garbage-collector timing and is labeled VARIES;
 * its numbers may differ between runs, JVMs and machines.
 */
public class Met12J {

    /** Open handles of the finalizer-based resource. */
    static final AtomicInteger finalizerOpen = new AtomicInteger();
    /** Open handles of the AutoCloseable resource. */
    static final AtomicInteger closableOpen = new AtomicInteger();
    /** Stands in for a cache or registry that keeps its handles reachable. */
    static final List<Object> registry = new ArrayList<>();

    /** NONCOMPLIANT resource: released only by a finalizer. */
    static class FinalizerHandle {
        FinalizerHandle() {
            finalizerOpen.incrementAndGet();
        }

        @Override
        protected void finalize() { // violates MET12-J (deprecated for removal)
            finalizerOpen.decrementAndGet();
        }
    }

    /** COMPLIANT resource: released explicitly via close(). */
    static class ClosableHandle implements AutoCloseable {
        private boolean closed;

        ClosableHandle() {
            closableOpen.incrementAndGet();
        }

        /** Pretends to use the resource. */
        void use() {
        }

        @Override
        public void close() {
            if (!closed) {
                closed = true;
                closableOpen.decrementAndGet();
            }
        }
    }

    /** Requests a collection and waits briefly so a finalizer thread would have a chance to run. */
    private static void gcAndWait() throws InterruptedException {
        System.gc();
        Thread.sleep(500);
    }

    /**
     * Runs the deterministic scenario, then the labeled-variable one.
     *
     * @param args unused
     * @throws InterruptedException if the wait is interrupted
     */
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== MET12-J: Do not use finalizers ===");

        System.out.println("\n-- Scenario 1 (DETERMINISTIC): handle also held by a registry, then System.gc() --");
        FinalizerHandle fh = new FinalizerHandle();
        registry.add(fh);
        fh = null; // only the registry refers to it now
        gcAndWait();
        System.out.println("noncompliant: finalizer-released handles still open = " + finalizerOpen.get());

        ClosableHandle ch = new ClosableHandle();
        try (ClosableHandle h = ch) {
            h.use();
            registry.add(h);
        }
        gcAndWait();
        System.out.println("compliant   : try-with-resources handles still open = " + closableOpen.get());
        System.out.println("-> a reachable object is never finalized (guaranteed), so the finalizer never ran;");
        System.out.println("   try-with-resources released its handle at the end of the block regardless.");

        System.out.println("\n-- Scenario 2 (VARIES between runs/JVMs): handle that is genuinely unreachable --");
        int before = finalizerOpen.get();
        new FinalizerHandle(); // created and immediately unreachable
        System.out.println("noncompliant: right after use, open = " + (finalizerOpen.get() - before)
                + "  (stays 1 unless a GC happens to run first)");
        gcAndWait();
        int after = finalizerOpen.get() - before;
        System.out.println("noncompliant: after System.gc() + 500 ms, open = " + after
                + "  [VARIES: 0 if the GC honored the hint and the finalizer thread ran, else 1; never guaranteed]");
        int closableBefore = closableOpen.get();
        try (ClosableHandle h = new ClosableHandle()) {
            h.use();
        }
        System.out.println("compliant   : right after the try block, open = "
                + (closableOpen.get() - closableBefore) + "  [always 0, no GC involved]");
    }
}
