# Ian's Rules: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## IDS07-J: Sanitize untrusted data passed to the Runtime.exec() method (`Ids07J`)

**Description.** Text that comes from a user must not be pasted into a command line that a shell will interpret. Characters such as `&`, `|` and `;` end the intended command and start another one. Accept only input that matches a strict whitelist (for example letters, digits, dot, underscore and hyphen), and pass the program and its arguments to the operating system as separate array elements instead of one joined string.

**Why it matters.** If the attacker controls part of the command, they control what the program does with the application's rights: reading or deleting files, starting other programs, opening network connections. In our demo the input `hello & echo INJECTED` made the shell run a second command of the attacker's choosing; the whitelist stopped it before any process was started. For safety the injected command is only a harmless `echo`.

**Java 17 / Windows note.** The demo uses `cmd /c` on Windows and `sh -c` elsewhere, because `echo` is a shell built-in on Windows. A whitelist is still needed even with an argument array, since the shell parses what it receives.

## IDS16-J: Prevent XML Injection (`Ids16J`)

**Description.** Untrusted text must not be concatenated into XML. A user can type tags of their own and so add, change or duplicate elements. Build documents with an XML API, which escapes markup characters in text, and validate each value against what it is supposed to be (for example "digits only" for a quantity).

**Why it matters.** The receiving code trusts the structure of the document. In our demo a crafted quantity inserted a second `<price>` element ahead of the real one, and the reader took the attacker's 1.00 instead of 500.00. Building the document with the DOM API turned the injected tags into harmless text (`&lt;`, `&gt;`), and the digits-only check rejected the input outright. The demo keeps all XML in memory and turns on secure processing and disables DOCTYPE declarations in the parser.

## OBJ05-J: Do not return references to private mutable class members (`Obj05J`)

**Description.** A getter that returns the object held in a private field gives the caller a live handle to it. If the object is mutable (an array, a `Date`, a collection), the caller can change the class's internal state even though the field is private. Return a copy instead (`clone()`, `new Date(...)`, an unmodifiable view).

**Why it matters.** The class loses control of its own invariants. In our demo the caller changed a booking's check-in time and a room number just by modifying what the getters returned; the compliant version stayed at its original values. Private fields give no protection if the objects behind them are handed out.

## OBJ11-J: Be wary of letting constructors throw exceptions (`Obj11J`)

**Description.** When a constructor throws, the half-built object still exists until the garbage collector removes it. An attacker can subclass the class and override `finalize()` to capture `this` from the failed object (a finalizer attack). Defend by recording successful construction in a flag that is set as the last step of the constructor and checked by every method, or by making the class `final`, or by making `finalize()` final and empty.

**Why it matters.** A constructor is often where the security check lives (a password, a permission). If the object can be rescued after the check failed, the attacker gets a usable object without passing the check. In our demo the attacker rescued the object from the failed constructor and the noncompliant `openSafe()` ran; the compliant one threw because the flag was never set.

**Java 17 limitation and what varies.** `finalize()` is deprecated, so compiling gives a deprecation warning (the same as in MET12-J). Whether and when the finalizer runs is up to the JVM, so the demo requests garbage collection repeatedly for up to about 3 seconds and prints a "VARIES" message if the attacker never got the object. The attack succeeded in every run we observed on JDK 17.0.12 (Temurin) on Windows, but finalizer timing varies by JVM and run, so other setups may print the VARIES message.

## OBJ13-J: Ensure that references to mutable objects are not exposed (`Obj13J`)

**Description.** Mutable objects can be exposed in ways other than getters. A constructor that stores the caller's own array or `Date` keeps sharing it with the caller, and a `public static final` array is only constant in name because its elements can be overwritten. Copy mutable input when it comes in, and use unmodifiable collections (such as `List.of`) for shared constants.

**Why it matters.** Code far away from the class can silently change its state or the program's shared constants. In our demo the caller edited its own `Date` and array after construction and the noncompliant event changed with them, and one line of code turned the role `guest` into `admin` in the shared constant. The compliant event kept its own copies, and the unmodifiable list rejected the write.
