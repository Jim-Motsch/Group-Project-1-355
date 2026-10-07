# Jimmy's Recommendations: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## MET50-J: Avoid ambiguous or confusing uses of overloading (`Met50J`)

**Description.** Do not give one class several methods with the same name whose parameters look interchangeable, such as `int` and `Integer`. When the operations mean different things, give them different names.

**Why it matters.** At the call site `remove(2)` does not show whether it removes position 2 or the item with id 2, and Java silently picks one. In our demo the caller wanted to remove the id 2 from `[2, 5, 7]` but the `int` overload removed position 2 (the id 7) and left `[2, 5]`. With separate `removeId` and `removePosition` methods the call says what it does and the result was `[5, 7]`.

## MET53-J: Ensure that the clone() method calls super.clone() (`Met53J`)

**Description.** A `clone()` method should start from `super.clone()`, which creates the copy with the same runtime class as the original, and then fix up any mutable fields. Do not build the copy with `new`.

**Why it matters.** A `clone()` that uses a constructor always returns the base class. A subclass that inherits it silently gets back an object of the wrong type, and the failure only appears later, as a `ClassCastException` when the caller casts the result. In our demo the copy of a subclass object was a plain base object and the cast failed; with `super.clone()` the copy was a subclass object and the cast worked.

## ERR54-J: Use a try-with-resources statement to safely handle closeable resources (`Err54J`)

**Description.** Open resources in a try-with-resources statement instead of closing them by hand in a `finally` block. The resources are closed automatically, in reverse order, even if the body throws.

**Why it matters.** In a `try/finally`, an exception thrown by `close()` replaces the exception thrown by the body, so the real cause of the failure is lost. Try-with-resources keeps the body's exception as the main one and attaches the close failure to it as a suppressed exception. In our demo the manual version reported "close failed" and nothing else, while try-with-resources reported "body failed" with "close failed" listed as suppressed. Manual closing is also easy to get wrong when several resources are open at once.
