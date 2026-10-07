# Martin's Rules: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## ERR08-J: Do not catch NullPointerException or any of its ancestors (`Err08J`)

**Description.** Don't use `catch (NullPointerException)` as a way to deal with null values, and don't use broad handlers such as `catch (Exception)` or `catch (Throwable)` that would also catch an NPE. If a value might be null, test it with an `if` before using it.

**Why it matters.** A catch block can't tell which null caused the exception. It might be the null you expected, or it might be a bug three calls deeper. Catching it hides the bug and the program keeps running with a made-up answer. In our demo a valid input was reported as "null" because of a bug in a helper.

## FIO02-J: Detect and handle file-related errors (`Fio02J`)

**Description.** File operations can fail for ordinary reasons: the file is missing, access is denied, the disk is full. Code must check for those failures and respond to them, either by catching the exception or by checking the returned status. It must not assume the operation worked.

**Why it matters.** If an error is ignored, the program behaves as though the file work succeeded. It may treat a missing config file as an empty one, or report a cleanup that never happened. This leads to wrong results, leftover files, and sensitive data staying on disk. The `java.nio.file` methods throw specific exceptions, which makes the failure easy to see.

## EXP02-J: Do not use the Object.equals() method to compare two arrays (`Exp02J`)

**Description.** Arrays inherit `equals()` from `Object`, which only asks "are these the same array in memory?" To ask "do these hold the same elements?", use `Arrays.equals()`, or `Arrays.deepEquals()` for arrays of arrays.

**Why it matters.** The code compiles and looks correct, but two arrays with identical contents are reported as different. Comparisons used for checks such as validation or authentication then give wrong answers without any error message.

## OBJ10-J: Do not use public static nonfinal fields (`Obj10J`)

**Description.** A field that is `public static` and not `final` is a global variable that anyone can overwrite. Make such fields `private`, and offer a getter and, if changes are needed, a setter that validates the new value. Constants should be `final`.

**Why it matters.** The owning class can't stop other code from putting it into an invalid state, and it can't tell when the value changes. In our demo another piece of code set a retry limit to -1 and the service quietly stopped doing any work. Any code on the classpath, including untrusted code, can do the same.

## EXP00-J: Do not ignore values returned by methods (`Exp00J`)

**Description.** If a method returns a value, the caller should use it or at least check it. This matters most for methods on immutable objects (such as `String.trim()`, which returns a new string) and for methods that report success or failure (such as `Set.add()`, which returns a boolean).

**Why it matters.** Discarding the result makes the call do nothing useful, but the code continues as if it had worked. In our demo the whitespace was never removed, and a duplicate user name was accepted without anyone noticing. These bugs are easy to miss in review because the line looks reasonable.
