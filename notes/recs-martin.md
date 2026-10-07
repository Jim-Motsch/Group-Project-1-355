# Martin's Recommendations: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## ERR50-J: Use exceptions only for exceptional conditions (`Err50J`)

**Description.** Exceptions are for things that go wrong unexpectedly, not for ordinary control flow. Reaching the end of an array is expected, so test the loop bound instead of looping forever and catching `ArrayIndexOutOfBoundsException`.

**Why it matters.** It makes the code harder to read, because a normal outcome looks like a failure. It also hides real bugs: a `catch` cannot tell the expected end of the loop from a mistake in the loop body that throws the same exception. In our demo both loops summed `{10, 20, 30}` to 60, but the exception version needed an exception to stop. When the body accidentally read `a[i + 1]`, the exception version quietly returned a wrong answer (50), while the bounded loop surfaced the bug.

## FIO50-J: Do not make assumptions about file creation (`Fio50J`)

**Description.** Checking that a file does not exist and then creating it are two separate steps, and another process can create the file in between. Create the file in one atomic step that fails if it already exists (`CREATE_NEW`), and handle that failure.

**Why it matters.** With the check-then-create pattern, the default write opens the file and truncates whatever another process just put there, so its data is lost and the program still reports success. In our demo the other process's data was replaced by ours; with `CREATE_NEW` the create failed with `FileAlreadyExistsException`, the other file was left alone, and the program could report the collision. On shared machines this is also a security problem, since an attacker can plant a file in a predictable place.

**What is simulated.** The other process is a piece of code that runs exactly between the check and the create, so the demo gives the same result every run. All files live in a temporary directory that the demo deletes.

## OBJ51-J: Minimize the accessibility of classes and their members (`Obj51J`)

**Description.** Give classes and members the least visibility they need: fields `private`, helper methods `private`, and a small public API for the operations clients actually need.

**Why it matters.** Anything public becomes something other code can rely on or misuse, and it is hard to change later. In our demo, a public list let client code store a blank message and then clear the whole inbox. The class with a private list accepted only valid messages through `add()`, rejected the blank one, and gave clients no way to wipe or corrupt the data. A small public surface is also easier to test and document.
