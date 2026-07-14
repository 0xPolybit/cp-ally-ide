# CP Ally IDE UI Verification Checklist

This checklist is the baseline for UI modernization work. Run it before and after every UI slice that changes the affected surface.

## Environment

- [ ] Windows 10/11
- [ ] JDK 17+
- [ ] Built from the current branch with `mvn -DskipTests clean package`
- [ ] Application data directory backed up or disposable for cache/settings tests
- [ ] Screen scaling recorded: ________
- [ ] Display resolution recorded: ________
- [ ] Application version/build: ________

## Startup and shell

- [ ] Application starts and displays the splash without errors.
- [ ] Main window opens at the persisted size and location.
- [ ] Main window opens correctly when maximized state was persisted.
- [ ] Minimum window size remains usable at `1000x680`.
- [ ] Menu bar contains File, Edit, Run, and Help menus.
- [ ] Menu accelerators work: `Ctrl+N`, `Ctrl+E`, `Ctrl+R`, `Ctrl+P`, `Ctrl+Shift+T`, `Ctrl+=`, `Ctrl+-`, `Ctrl+0`.
- [ ] Window close persists window state and current source.
- [ ] Restart restores window size, position, divider locations, language, editor settings, and theme.

## Problem selection and loading

- [ ] Empty first-run state shows problem-code input and fetch action.
- [ ] Blank input produces a clear validation message.
- [ ] Invalid code produces a clear validation message.
- [ ] A cached problem loads without requiring a network request.
- [ ] An uncached problem loads from Codeforces.
- [ ] Loading state is visible while the fetch is in progress.
- [ ] Fetch failure returns the user to a usable state and provides a readable error.
- [ ] Refresh re-fetches the current problem without losing the editor source.
- [ ] `Ctrl+N` opens the choose-problem flow.
- [ ] `Ctrl+E` opens the empty workspace after confirmation.
- [ ] Empty workspace does not persist source into the normal problem cache.
- [ ] `cpally://problem/<code>` cold launch opens the requested problem.
- [ ] `cpally://problem/<code>` hot handoff opens the requested problem in the running instance.
- [ ] Minimized-instance deep link restores and focuses the main window.

## Problem statement

- [ ] Problem title and code are visible.
- [ ] Time limit, memory limit, and input/output mode are readable.
- [ ] Statement-only view excludes samples from the statement region.
- [ ] Test-case region contains the extracted samples.
- [ ] Headings, paragraphs, lists, tables, and code blocks render legibly.
- [ ] LaTeX formulas render or show a useful fallback.
- [ ] LaTeX limitation warning is readable and does not hide the statement.
- [ ] Images and inline icons render at acceptable quality.
- [ ] Copy controls copy the intended input/output to the clipboard.
- [ ] External Codeforces and practice-sheet links open correctly.
- [ ] Problem pane scrolls normally.
- [ ] Problem zoom works with controls, `Ctrl+wheel`, and `Ctrl+0`.
- [ ] Zoom re-rendering keeps the document usable and starts at the expected position.
- [ ] Authenticated user verdict status appears only when a user is configured.

## Editor

- [ ] Editor is unavailable before a problem or empty workspace is opened.
- [ ] Editor becomes editable after a problem/workspace is opened.
- [ ] Correct language template is inserted for each supported language family.
- [ ] Cached source is restored when returning to a problem/language.
- [ ] Switching language saves the previous language source and loads the new template/cache.
- [ ] Syntax highlighting matches the selected language.
- [ ] Line numbers and editor gutter remain readable.
- [ ] Bracket auto-pairing works for `()`, `[]`, and `{}`.
- [ ] Backspace removes an adjacent empty pair correctly.
- [ ] Selecting text and typing an opening bracket wraps the selection.
- [ ] Editor zoom works with controls, shortcuts, and wheel input.
- [ ] Tab/spaces and tab width preferences affect the editor.
- [ ] Editor remains usable with long lines and large source files.
- [ ] Focus/caret is visible and typing does not unexpectedly lose focus.

## Runtime and execution

- [ ] Runtime support status is visible for the selected language.
- [ ] Missing toolchain status explains which command is unavailable.
- [ ] `Ctrl+R` and the Run control execute the current source.
- [ ] Run control is disabled when no runnable test/input is available.
- [ ] Running state is visible and conflicting actions are appropriately disabled.
- [ ] Sample tests execute and show pass/fail results.
- [ ] Custom tests execute after being added.
- [ ] `YES`/`NO` comparison is case-insensitive.
- [ ] Execution time is displayed.
- [ ] Peak memory is displayed when available.
- [ ] Runtime error output is visible.
- [ ] Compilation error output is visible.
- [ ] Timeout output is visible.
- [ ] Empty-problem execution with empty input works.

## Test cases

- [ ] Sample tests are displayed in order.
- [ ] Test input is readable in a monospaced area.
- [ ] Expected output is readable in a monospaced area.
- [ ] `Ctrl+Shift+T` opens the add-custom-test flow.
- [ ] Empty custom input is rejected.
- [ ] Custom input/output newlines are normalized correctly.
- [ ] Custom tests can be removed.
- [ ] Removing a custom test keeps remaining labels/order correct.
- [ ] Switching problems clears old custom tests.
- [ ] No-test-case state is understandable.

## Preferences and dialogs

- [ ] Preferences opens with `Ctrl+P`.
- [ ] Cancel leaves settings unchanged.
- [ ] Save persists editor font size, editor scheme, app theme, tabs/spaces, tab spacing, and autosave.
- [ ] Theme-specific editor schemes are filtered correctly.
- [ ] Autosave interval accepts valid values and rejects out-of-range values.
- [ ] Runtime support dialog opens and closes with Escape.
- [ ] Profile dialog shows loading, success, and error states.
- [ ] Execution results dialog is scrollable.
- [ ] Dialogs remain usable at increased display scaling.
- [ ] Dialogs open on-screen and are centered relative to the main window.

## Themes and scaling

Run the relevant sections above under each exposed application theme:

- [ ] Light
- [ ] Dark
- [ ] Ultra Dark

At each theme:

- [ ] Text has sufficient contrast.
- [ ] Inputs, buttons, menus, tabs, split panes, scrollbars, and dialogs use the theme.
- [ ] Editor colors remain distinct from application-shell colors.
- [ ] Success/warning/error states are distinguishable without relying only on color.
- [ ] Icons have an appropriate light/dark asset.

Repeat at:

- [ ] 100% display scaling
- [ ] 125% display scaling
- [ ] 150% display scaling
- [ ] 200% display scaling

## Baseline defect log

Record pre-existing issues here so a later UI slice is not blamed for them:

| ID | Surface/state | Observation | Reproduction | Screenshot/reference | Introduced by slice? |
|---|---|---|---|---|---|
| B-001 | | | | | No / Yes |
| B-002 | | | | | No / Yes |
| B-003 | | | | | No / Yes |

## Review sign-off

- Tester: ____________________
- Date: ____________________
- Branch/commit: ____________________
- Build command/result: ____________________
- Manual result: Pass / Pass with known defects / Fail
- Follow-up issues: ____________________
