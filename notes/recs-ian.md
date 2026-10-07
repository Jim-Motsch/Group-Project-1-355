# Ian's Recommendations: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## OBJ50-J: Never confuse the immutability of a reference with that of the referenced object (`Obj50J`)

**Description.** `final` on a variable only means the variable cannot be made to point at a different object. The object it points to can still change. If the contents must stay fixed, use an object that is itself immutable (for example `List.of(...)`, `String`, or a class with no setters).

**Why it matters.** Readers see `final` and assume the data is constant. In our demo a `final List` of permissions accepted `add("admin")`, so any code holding the list could grant itself rights. The same field declared with `List.of("read")` rejected the change with an `UnsupportedOperationException`.

## OBJ56-J: Provide sensitive mutable classes with unmodifiable wrappers (`Obj56J`)

**Description.** If a mutable class has to be shown to code you do not control, give that code an unmodifiable wrapper instead of the real object. The wrapper supports the read operations, follows later changes made by the owner, and throws on any attempt to change it.

**Why it matters.** Handing out the real object lets every receiver change it. In our demo untrusted code set a timeout to 0 on the real settings object; given the read-only wrapper, the same call was rejected and the timeout stayed 30. When the owner changed its own setting to 60, the wrapper showed 60, so it is a live read-only view and not a stale copy.

## MET51-J: Do not use overloaded methods to differentiate between runtime types (`Met51J`)

**Description.** Java picks which overload to call from the declared type of the argument, at compile time, not from the type of the object at run time. Do not rely on overloads to react to the real type. Use one method that checks `instanceof`, or use polymorphism (an overridden method).

**Why it matters.** The code looks as if it handles each type separately, but a value held in an `Object` variable always gets the `Object` version. In our demo the string `"hello"` stored in an `Object[]` was classified as "object" by the overloaded `kind()`, while the single method with `instanceof` correctly said "string". The bug is silent: nothing fails, the wrong branch just runs.
