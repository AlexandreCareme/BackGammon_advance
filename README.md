## Third-party code

The backgammon board diagrams under `Overleaf/` use a modified version of
[latex_backgammon](https://github.com/psygo/latex_backgammon) by Philippe Fanaro
(renamed `bg-latex-diagrams.sty`), used under the BSD 4-Clause License —
see `Overleaf/bg-latex-diagrams/LICENSE.txt`. This is exclusively used to create Backgammon graphics for the Proposal and Report.  

# JavaFX + Maven Project Template (Java 25)

This is a **template repository**: it's a starting point, not a shared
codebase. Your group gets your own independent copy of it (either via
"Use this template" on GitHub, or a repo created for you), and your
changes only affect your copy — nothing you do here affects the
original template or any other group's repository.

This template isn't tied to any single assignment — it's a general
Java 25 + Maven + JavaFX starting point, reused across different course
projects. The specific assignment instructions (what to actually build)
live separately; this repo and README just cover the tooling.

## Prerequisites

- **Java 25** (the current LTS release) installed and on your `PATH`.
  If you don't have it, get it from
  [Adoptium Temurin 25](https://adoptium.net/temurin/releases?version=25&os=any&arch=any)
  — pick the installer for your OS.
- **[IntelliJ IDEA](https://www.jetbrains.com/idea/download)** (Community or Ultimate).

## Getting the project into IntelliJ

1. On your repository's GitHub page, click the green **Code** button
   and copy the HTTPS URL (looks like
   `https://github.com/<you>/<your-repo>.git`).
2. In IntelliJ: **File > New > Project from Version Control**, paste
   the URL, and choose a local folder to clone it into.
3. IntelliJ will open the project and automatically import it as a
   Maven project (this can take a minute the first time, while it
   downloads dependencies).
4. To run the app: open `Main.java` and click the green run arrow
   next to `main`. (Not `App.java` directly — see the comment in
   `Main.java` for why.)
5. To run the tests: right-click `src/test/java` and choose
   **Run 'All Tests'**, or run `mvn test` from a terminal.

For the day-to-day workflow of making changes (branches, commits,
Pull Requests) once you're set up, see **[GIT_WORKFLOW.md](GIT_WORKFLOW.md)**.

If you're new to git or want a more general introduction to
IntelliJ's Git tooling beyond this project's specific workflow,
JetBrains has a good tutorial:
[Working with Git — IntelliJ IDEA tutorial](https://www.jetbrains.com/help/idea/working-with-git-tutorial.html).

## The tech stack

- **Java 25** — the language version this project targets.
- **Maven** — the build tool; `pom.xml` at the repo root defines
  dependencies and how to build/run/test the project. You shouldn't
  need to edit it for normal coursework.
- **JavaFX 25 (OpenJFX)** — the UI toolkit. This project builds its
  UI directly in Java code (see `App.java`), **not** with FXML files.
  If you look at outside JavaFX tutorials, some of them use FXML
  (separate `.fxml` layout files) — that's a valid alternative
  approach in general, but not what this project uses, so don't be
  thrown off if a tutorial's structure looks different from ours.
- Run the app from IntelliJ via `Main.java` as above.
  Alternative, it can be run from the command line with `mvn javafx:run`.

## Testing with JUnit

Tests in this project use **JUnit 5**, the standard testing framework
for Java. If you haven't used it before, the short version:

- A test is a plain method annotated `@Test` that calls your code and
  checks the result with an `assert...` call (e.g. `assertEquals`,
  `assertTrue`). If nothing throws, the test passes.
- Tests live under `src/test/java`, mirroring the package structure of
  `src/main/java` — they're never shipped with the app, only run
  during development and CI.
- `src/test/java/gameproject/util/ResourceLoaderTest.java` is a small
  self-contained example worth reading first — it doesn't depend on
  any of your own code, just to show the pattern.
- Run all tests with `mvn test`, or individual tests directly from
  IntelliJ's gutter icons.

## Repository layout

```
pom.xml                          Maven project config — dependencies, Java/JavaFX versions
GIT_WORKFLOW.md                  How to branch, commit, and open PRs for this project
AGENTS.md                        Rules for AI coding assistants working in this repo
README.md                        This file

src/main/java/gameproject/
  App.java                       JavaFX entry point (extends Application) — builds the UI
  Main.java                  Use this to run from an IDE (see file comment for why)
  ...                            Organize your own code into packages that make sense
                                  for your project (e.g. separating plain logic classes
                                  with no JavaFX dependencies makes them easier to unit test)

src/main/resources/gameproject/
                                 Non-code assets (images, icons, etc.) loaded at runtime
                                  via getResourceAsStream — see App.java for the pattern

src/test/java/gameproject/       Unit tests, mirroring the package structure of src/main

.github/                         PR template, GitHub Actions CI, AI-assistant instructions
```

The rule of thumb: **`src/main` is what ships** (your actual
application code), **`src/test` is what checks it** (never shipped,
only run during development/CI). Resources under either one are
non-code files (images, small data files) loaded from the classpath
rather than written as Java code.

## Questions?

Start with `GIT_WORKFLOW.md` for anything git/GitHub-related, and ask
your TA or instructor for anything specific to the assignment itself.
