package cert;

/**
 * OBJ56-J: Provide sensitive mutable classes with unmodifiable wrappers.
 *
 * <p>Owner: Ian
 *
 * <p>Recommendation. If a mutable class has to be shown to code you do not
 * control, hand out an unmodifiable wrapper that supports reading (and follows
 * later changes by the owner) but throws on every attempt to change it.
 */
public class Obj56J {

    /** The sensitive mutable class. */
    static class Settings {
        private int timeoutSeconds = 30;

        int getTimeoutSeconds() {
            return timeoutSeconds;
        }

        void setTimeoutSeconds(int s) {
            timeoutSeconds = s;
        }
    }

    /** Unmodifiable wrapper: reads through to the real Settings, rejects writes. */
    static class ReadOnlySettings extends Settings {
        private final Settings target;

        ReadOnlySettings(Settings target) {
            this.target = target;
        }

        @Override
        int getTimeoutSeconds() {
            return target.getTimeoutSeconds();
        }

        @Override
        void setTimeoutSeconds(int s) {
            throw new UnsupportedOperationException("settings are read-only here");
        }
    }

    /** Stand-in for untrusted code that is given a Settings object and tries to change it. */
    private static String untrusted(Settings s) {
        try {
            s.setTimeoutSeconds(0);
            return "changed timeout to " + s.getTimeoutSeconds();
        } catch (UnsupportedOperationException e) {
            return "write rejected (" + e.getMessage() + "), timeout = " + s.getTimeoutSeconds();
        }
    }

    /**
     * Gives the real object to untrusted code, then the wrapper.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== OBJ56-J: Provide sensitive mutable classes with unmodifiable wrappers ===");

        System.out.println("\n-- Scenario 1: untrusted code receives the settings and calls setTimeoutSeconds(0) --");
        Settings real = new Settings();
        System.out.println("noncompliant (real object handed out): " + untrusted(real));

        Settings owner = new Settings();
        System.out.println("compliant    (read-only wrapper)     : " + untrusted(new ReadOnlySettings(owner)));

        System.out.println("\n-- Scenario 2: the owner changes its own settings; the wrapper follows --");
        ReadOnlySettings view = new ReadOnlySettings(owner);
        owner.setTimeoutSeconds(60);
        System.out.println("compliant view now reads: " + view.getTimeoutSeconds());
    }
}
