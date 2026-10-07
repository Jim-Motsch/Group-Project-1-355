# Jimmy's Rules: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## SER05-J: Do not serialize instances of inner classes (`Ser05J`)

**Description.** An inner class (a nested class that is not `static`) secretly holds a reference to the object of the outer class that created it. When the inner object is serialized, Java tries to serialize that outer object as well. If the inner class has to be serializable, declare it `static` so it only carries its own data.

**Why it matters.** Two things go wrong. If the outer class is not serializable, serialization fails with an exception. If it is serializable, its fields are written into the stream along with the inner object's, so a private value such as an API key can leak without anyone intending it. The compiler-generated hidden field is also not a stable format, which causes trouble when the classes change or are compiled by a different compiler.

## MET06-J: Do not invoke overridable methods in clone() (`Met06J`)

**Description.** A `clone()` method should not call methods that a subclass is allowed to override. Do the copying inside `clone()` itself, and let a subclass customize the result by overriding `clone()` and calling `super.clone()` first.

**Why it matters.** Right after `super.clone()`, the copy still shares its mutable fields with the original. If an overridable method runs at that point, the subclass code can change the shared data and silently corrupt the original object. In our demo, the original list gained an extra "cloned" entry just because a copy was made.

## MET12-J: Do not use finalizers (`Met12J`)

**Description.** Don't rely on `finalize()` to release resources or do cleanup. The garbage collector decides when, and whether, to call it. Release resources explicitly instead, for example by implementing `AutoCloseable` and using try-with-resources.

**Why it matters.** There is no guarantee a finalizer ever runs. An object that is still reachable is never finalized, so a file or connection tied to one can stay open for as long as the program runs. Finalizers also slow down garbage collection and can be abused by attackers (for example by bringing a half-built object back to life), which is why they are deprecated and slated for removal. Try-with-resources closes the resource at a known point, every time.

**Java 17 limitation and what varies.** On Java 17 `finalize()` is deprecated (marked for removal from JDK 18), so compiling the demo gives a deprecation warning. Scenario 1 of the demo relies on a language guarantee (reachable objects are not finalized), so its result is stable. Scenario 2 depends on garbage-collector timing, so it is labeled VARIES: the number may differ between runs and JVMs.

## MET05-J: Ensure that constructors do not call overridable methods (`Met05J`)

**Description.** A constructor should only call methods that cannot be overridden (`private`, `static` or `final`). If the subclass needs to influence the superclass's setup, pass the value through the constructor instead.

**Why it matters.** The superclass constructor runs before the subclass has set up its own fields. An overriding method called from there sees default values such as `null` or `0`. In our demo the label came out as "null!" instead of "Sub!". These bugs are confusing because the subclass code looks correct when read on its own.

## ERR01-J: Do not allow exceptions to expose sensitive information (`Err01J`)

**Description.** Error messages shown to users should be generic. Details such as file paths, host names, user names, or which specific check failed should go only to a protected internal log. Failures with different causes should look the same from the outside.

**Why it matters.** Detailed errors help an attacker learn how the system is built. In our demo the raw exception revealed the internal folder where account files live, and separate "no such user" and "wrong password" messages let someone find out which user names exist. A short generic message plus a detailed server-side log keeps the information useful for developers and useless for attackers.
