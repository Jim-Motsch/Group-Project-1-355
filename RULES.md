# Rule Assignments

Source: docs/IT355GroupProject1Rules.pdf. Titles are CERT's official names.

## Martin
- ERR08-J (P12): Do not catch NullPointerException or any of its ancestors
- FIO02-J (P12): Detect and handle file-related errors
- EXP02-J (P9): Do not use the Object.equals() method to compare two arrays
- OBJ10-J (P8): Do not use public static nonfinal fields
- EXP00-J (P8): Do not ignore values returned by methods

## James
- SER01-J (P18): Do not deviate from the proper signatures of serialization methods
- MET01-J (P8): Never use assertions to validate method arguments
- SER04-J (P18): Do not allow serialization and deserialization to bypass the security manager
- SER08-J (P18): Minimize privileges before deserializing from a privileged context
- MET00-J (P9): Validate method arguments

## Jimmy
- SER05-J (P12): Do not serialize instances of inner classes
- MET06-J (P8): Do not invoke overridable methods in clone()
- MET12-J (P8): Do not use finalizers
- MET05-J (P8): Ensure that constructors do not call overridable methods
- ERR01-J (P8): Do not allow exceptions to expose sensitive information

## Ian
- IDS07-J (P12): Sanitize untrusted data passed to the Runtime.exec() method
- IDS16-J (P12): Prevent XML Injection
- OBJ05-J (P12): Do not return references to private mutable class members
- OBJ11-J (P12): Be wary of letting constructors throw exceptions
- OBJ13-J (P12): Ensure that references to mutable objects are not exposed

## Alex
- IDS00-J (P18): Prevent SQL injection
- FIO08-J (P18): Distinguish between characters or bytes read from a stream and -1
- SER12-J (P9): Prevent deserialization of untrusted data
- EXP03-J (P9): Do not use the equality operators when comparing values of boxed primitives
- ERR02-J (P12): Prevent exceptions while logging data

## Recommendations (3 each, from Rec 05, 06, 07, 13)

### Martin
- ERR50-J: Use exceptions only for exceptional conditions
- FIO50-J: Do not make assumptions about file creation
- OBJ51-J: Minimize the accessibility of classes and their members

### James
- MET54-J: Always provide feedback about the resulting value of a method
- MET55-J: Return an empty array or collection instead of a null value for methods that return an array or collection
- FIO53-J: Use the serialization methods writeUnshared() and readUnshared() with care

### Jimmy
- MET50-J: Avoid ambiguous or confusing uses of overloading
- MET53-J: Ensure that the clone() method calls super.clone()
- ERR54-J: Use a try-with-resources statement to safely handle closeable resources

### Ian
- OBJ50-J: Never confuse the immutability of a reference with that of the referenced object
- OBJ56-J: Provide sensitive mutable classes with unmodifiable wrappers
- MET51-J: Do not use overloaded methods to differentiate between runtime types

### Alex
- ERR51-J: Prefer user-defined exceptions over more general exception types
- ERR52-J: Avoid in-band error indicators
- FIO51-J: Identify files using multiple file attributes

## Notes for Java 17
- SER04-J, SER08-J: the Security Manager is deprecated for removal. Demonstrate
  the concept and explain the limitation instead of faking behavior.
- MET12-J: finalizers are deprecated. Demo compiles with a deprecation warning.
- IDS00-J: use an in-memory H2 database.