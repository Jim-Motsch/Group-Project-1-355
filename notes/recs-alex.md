# Alex's Recommendations: Notes for the Report

Written in our own words (not copied from CERT). Code: `src/main/java/cert/`.

## ERR51-J: Prefer user-defined exceptions over more general exception types (`Err51J`)

**Description.** Define exception classes for the specific failures your code can report (for example `InsufficientFundsException`) instead of throwing `RuntimeException` or `Exception` with a message.

**Why it matters.** With a general type, a caller can only catch everything the same way, or parse the message text, which breaks as soon as the wording changes. Distinct types let each failure get the reaction that fits it and let other failures pass through. In our demo the generic version gave the caller one catch-all reaction for both "insufficient funds" and "account frozen", while the typed version offered an overdraft for the first and referred the customer to the bank for the second.

## ERR52-J: Avoid in-band error indicators (`Err52J`)

**Description.** Do not signal an error with a value that could also be a legitimate result (such as `-1`, `null` or an empty string). Return a type that can express absence, such as `Optional`/`OptionalInt`, or throw an exception.

**Why it matters.** A real result that happens to equal the indicator is mistaken for the error. In our demo a lookup returned `-1` for "unknown city", but Oslo's real temperature was also -1 °C, so the caller reported Oslo as "unknown". With `OptionalInt`, Oslo printed `-1 C` and a missing city printed "unknown", and for a normal city (Cairo) both versions agreed.

## FIO51-J: Identify files using multiple file attributes (`Fio51J`)

**Description.** A file name or path string is not a reliable identity for a file. Different strings can name the same file, and the same name can belong to different files. To decide whether two paths are the same file, ask the file system (`Files.isSameFile`), which compares attributes such as the file key, instead of comparing text.

**Why it matters.** Code that trusts names can be fooled, or can simply be wrong: a blocklist or lock keyed by path text is bypassed with a different spelling, and a "same file" check by name can confuse two unrelated files. In our demo `data.txt` and `sub/../data.txt` are one file, which the path comparison called different and `isSameFile` called the same; `data.txt` and `other/data.txt` are two files, which the name comparison called the same and `isSameFile` called different.

**Demo setup.** The files live in a temporary directory that the demo creates and deletes.
