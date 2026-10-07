package cert;

import java.util.ArrayList;
import java.util.List;

/**
 * OBJ51-J: Minimize the accessibility of classes and their members.
 *
 * <p>Owner: Martin
 *
 * <p>Recommendation. Every member that is more visible than needed is a member
 * other code can depend on or misuse. Make fields private and offer only the
 * operations clients really need, so the class keeps control of its own rules.
 */
public class Obj51J {

    /** NONCOMPLIANT: the internal list and a helper are public. */
    static class NoncompliantInbox {
        public List<String> messages = new ArrayList<>();      // violates OBJ51-J

        public void trimForDisplay() {                          // internal helper, needlessly public
            messages.removeIf(String::isBlank);
        }
    }

    /** COMPLIANT: private state, a narrow public API, the helper is private. */
    static class CompliantInbox {
        private final List<String> messages = new ArrayList<>();

        public boolean add(String m) {
            if (!isValid(m)) {
                return false;
            }
            messages.add(m);
            return true;
        }

        public int count() {
            return messages.size();
        }

        /** Internal rule; private because no client needs it. */
        private boolean isValid(String m) {
            return m != null && !m.isBlank();
        }
    }

    /**
     * Acts as unrelated client code that reaches into each inbox.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        System.out.println("=== OBJ51-J: Minimize the accessibility of classes and their members ===");

        NoncompliantInbox bad = new NoncompliantInbox();
        bad.messages.add("hello");
        bad.messages.add("   ");           // client puts junk straight into the list
        System.out.println("noncompliant: client added \"hello\" and a blank; stored = " + bad.messages);
        bad.messages.clear();               // client wipes the data
        System.out.println("noncompliant: client called messages.clear(); stored = " + bad.messages);

        CompliantInbox good = new CompliantInbox();
        System.out.println("compliant   : add(\"hello\") -> " + good.add("hello")
                + ", add(blank) -> " + good.add("   ") + "; count = " + good.count());
        System.out.println("compliant   : no way to clear or corrupt the list; count still = " + good.count());
        System.out.println("-> the public field let any code break the inbox's rules; the narrow API kept them.");
    }
}
