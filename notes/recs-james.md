# James's Recommendations: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## MET54-J: Always provide feedback about the resulting value of a method (`Met54J`)

**Description.** A method that can fail, or can decide to do nothing, should tell its caller what happened, by returning a result (such as a `boolean`) or by throwing an exception. A `void` method that silently skips its work gives no feedback.

**Why it matters.** The caller cannot tell success from failure and may carry on as if the work was done. In our demo, withdrawing 150 from a balance of 100 did nothing, and the `void` version gave the caller no way to know; the version that returned `false` let the caller report "refused: insufficient funds". Silent failures are hard to debug because the problem shows up later, far from its cause.

## MET55-J: Return an empty array or collection instead of a null value for methods that return an array or collection (`Met55J`)

**Description.** When there are no results, return an empty array or collection (for example `List.of()` or an empty list), not `null`.

**Why it matters.** Callers usually loop over the returned collection. If "nothing found" is `null`, every caller has to remember a special check, and the first one who forgets gets a `NullPointerException`. In our demo, searching for a name that does not exist crashed the caller for the `null` version; the empty-list version simply ran the loop zero times. When results exist, both versions behave the same.

## FIO53-J: Use the serialization methods writeUnshared() and readUnshared() with care (`Fio53J`)

**Description.** `writeObject` remembers objects it has already written and writes only a back-reference the second time, so shared objects stay shared after reading. `writeUnshared` always writes a fresh copy, and `readUnshared` refuses to read a back-reference. Use them only when you really want independent copies, and use the matching pair on both sides.

**Why it matters.** Using the unshared methods on an object that is meant to be shared silently breaks the object graph. In our demo one `Counter` was written twice. With `writeUnshared`/`readUnshared` the two references came back as separate objects, so changing one left the other at 5; with `writeObject`/`readObject` they were the same object and both showed 99. Mixing the styles fails outright: reading normally written bytes with `readUnshared` threw an `InvalidObjectException` ("cannot read back reference as unshared").
