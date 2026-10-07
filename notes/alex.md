# Alex's Rules: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## IDS00-J: Prevent SQL injection (`Ids00J`)

**Description.** Never build an SQL statement by gluing untrusted text into it. Quote characters in the text can end the intended value and let the attacker add SQL of their own. Use parameterized statements (`PreparedStatement` with `?` placeholders), which send the SQL and the values separately so a value can never become SQL.

**Why it matters.** The attacker can change the meaning of the query: skip the password check, read rows they should not see, or modify and delete data. In our demo, the name `alice' --` cut off the password condition and logged in as alice (whose role is admin) with a wrong password, and `x' OR '1'='1` returned every user. With placeholders the same text was treated as an ordinary (non-existent) name.

**Demo setup.** The demo uses a private in-memory H2 database (`jdbc:h2:mem:`) filled with three demo users. It creates no files and starts no console or network listener.

## FIO08-J: Distinguish between characters or bytes read from a stream and -1 (`Fio08J`)

**Description.** `InputStream.read()` returns an `int`: a value from 0 to 255 for a real byte, or -1 at the end of the stream. Keep the result in an `int` and compare it with -1 before converting it to `byte` or `char`.

**Why it matters.** If the result is stored in a `byte` first, the real data byte 255 (0xFF) becomes -1 and is mistaken for the end of the stream. The loop stops early and the rest of the data is silently dropped, with no exception. In our demo the stream 1, 2, 255, 3, 4 came back as just 1, 2. Binary data such as images or encrypted files contains 0xFF often, so this bug corrupts exactly the data where it is hardest to notice.

## SER12-J: Prevent deserialization of untrusted data (`Ser12J`)

**Description.** Deserialization builds objects of whichever classes the incoming bytes name, and runs those classes' `readObject` code, before the program can look at the result. Do not deserialize data from an untrusted source. If it cannot be avoided, install an `ObjectInputFilter` allow-list that permits only the expected classes and rejects everything else.

**Why it matters.** The sender, not the receiver, decides which of the program's classes are instantiated. Attackers chain classes that already exist on the classpath to run harmful code. In our demo, bytes naming a class we never expect caused its `readObject` to run (shown by a harmless flag and message), while the filter `cert.Ser12J$SafeNote;java.lang.String;!*` rejected the class before any object was built. The expected `SafeNote` still passed through the filter.

**Safety note.** The demo's "attacker" class only sets a flag and prints a line. It is not an exploit.

## EXP03-J: Do not use the equality operators when comparing values of boxed primitives (`Exp03J`)

**Description.** For `Integer`, `Long` and the other wrapper classes, `==` compares whether two references point to the same object, not whether the numbers are equal. Use `equals()`, or unbox to a primitive first.

**Why it matters.** Autoboxing reuses cached objects for small values (for `Integer`, -128 to 127) and creates new objects for larger ones, so `==` gives the right answer in small tests and the wrong answer later. In our demo, `==` on two `Integer`s holding the same number returned true for 100 and 127 but false for 128 and 1000, while `equals()` was always true. The cache range is an implementation detail, so code that relies on it is fragile.

## ERR02-J: Prevent exceptions while logging data (`Err02J`)

**Description.** Logging statements must not be able to throw. Building a log message can fail (a null field, a `toString()` that throws, a bad format string). Keep log expressions simple and guard the logging call so that a failure to log cannot affect the program.

**Why it matters.** Logging is mostly done inside error handling. If the log call throws there, the new exception replaces the one being handled: the original error is lost and the rest of the handler, such as cleanup, is skipped. In our demo a null customer made the log line throw a `NullPointerException`; the real "payment declined" error disappeared and the cleanup never ran. The compliant version caught the logging failure, wrote a fallback log line, and finished the cleanup.
