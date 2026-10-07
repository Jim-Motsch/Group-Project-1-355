# James's Rules: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## SER01-J: Do not deviate from the proper signatures of serialization methods (`Ser01J`)

**Description.** Java looks up the special serialization hooks (`writeObject`, `readObject`, and so on) by reflection and only uses them if the declaration matches exactly, for example `private void readObject(ObjectInputStream)`. If the name is right but the modifier, parameters or return type is off, the compiler says nothing and the hook is simply skipped.

**Why it matters.** The custom logic you wrote, such as encrypting a field or checking values, never runs, and nothing warns you. In our demo a `public` hook meant that a transient secret was dropped from the stream and came back as `null`. The same mistake would also skip validation or security checks you put in those hooks.

## MET01-J: Never use assertions to validate method arguments (`Met01J`)

**Description.** `assert` statements are for checking assumptions about our own code and are turned off unless the JVM is started with `-ea`. Checks on arguments that callers pass in must be normal `if` statements that throw an exception such as `IllegalArgumentException`.

**Why it matters.** In a normal production run the assertion doesn't exist, so bad input goes straight through. In our demo a withdrawal of -50 was accepted and increased the balance. The program's safety should not depend on a command-line flag.

## SER04-J: Do not allow serialization and deserialization to bypass the security manager (`Ser04J`)

**Description.** If a class limits who may create instances by putting a security-manager check in its constructor, that check does not run when an object is rebuilt from a serialized stream, because deserialization does not call the constructor. The class must repeat the same check inside `readObject` (and `writeObject` if the data should not be exported either).

**Why it matters.** Without the repeated check, anyone who can supply bytes to an `ObjectInputStream` can create objects that the security policy was meant to forbid. In our demo, a policy that denied the permission blocked `new` but did not block deserialization of the unguarded class.

**Java 17 limitation.** The Security Manager is deprecated for removal. Our demo installs one at runtime, which JDK 17 still allows, and prints a warning. JDK 18 to 23 need a startup flag, and JDK 24 and later cannot enable it at all, so Part B of this demo cannot run there. Part A, which counts the check calls, works on any JDK.

## SER08-J: Minimize privileges before deserializing from a privileged context (`Ser08J`)

**Description.** Deserialization runs the `readObject` methods of the classes in the stream, steered by the stream's data. If this happens inside a privileged block, those methods run with the full privileges of the calling code. The code should instead deserialize with a restricted access-control context, so only the permissions that are really needed are available.

**Why it matters.** An attacker doesn't need to supply their own code. They can send data that makes a trusted class do something harmful with the caller's privileges. In our demo the same bytes wrote a file under full privileges and were blocked by an `AccessControlException` under the restricted context.

**Java 17 limitation.** This relies on the Security Manager and `AccessController`, both deprecated for removal. The demo installs a security manager at runtime with a permissive policy. It does not show real policy files or signed code, and it cannot run on JDK 24 or later.

## MET00-J: Validate method arguments (`Met00J`)

**Description.** A method should check its parameters (null, range, format, allowed characters) before using them, particularly when they come from outside the class. Invalid input should be rejected with a clear exception.

**Why it matters.** Unchecked arguments give wrong answers, cause confusing failures deep inside the code, or let a caller steer the program somewhere unintended. In our demo a 150% discount produced a negative price, and a file name containing `../` produced a path outside the upload directory.
