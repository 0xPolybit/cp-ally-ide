# CP Ally IDE UI Modernization Plan

> Persistent implementation plan for modernizing the desktop interface without rewriting the application or disrupting the competitive-programming workflow.

**Status:** Proposed  
**Last reviewed:** 2026-07-14  
**Application baseline:** Java 17, Swing, FlatLaf 3.6, RSyntaxTextArea 3.5.4  
**Plan scope:** UI architecture, visual design, interaction design, accessibility, and UI-focused testing  
**Out of scope:** Replacing Codeforces integration, execution semantics, caches, deep links, or installer technology

---

## 1. Executive decision

Modernize the existing Swing application incrementally. **Do not rewrite it in JavaFX, Electron, or a browser shell.**

The current application already has the correct high-level product shape:

- problem statement on the left,
- code editor on the right,
- tests available in the same window,
- local execution one action away.

That side-by-side workflow is the product's main advantage and should be preserved. The modernization should make it feel deliberate, coherent, responsive, and current—not replace it with a different navigation model.

The recommended sequence is:

1. Introduce design tokens and reusable Swing component styles.
2. Separate view components and UI state from `MainWindow`.
3. Modernize the frame, workspace headers, and status presentation.
4. Improve statement, editor, test-case, and result surfaces.
5. Consolidate dialogs and accessibility behavior.
6. Add visual and interaction regression checks.

---

## 2. Codebase review summary

The project contains roughly 8,000 lines of Java across 30 source files. UI responsibilities are concentrated in the following areas.

### 2.1 Startup and frame lifecycle

| File | UI responsibility |
|---|---|
| `App.java` | Initializes diagnostics, enforces single instance, shows splash, constructs the main window, handles cold/hot deep links. |
| `SplashScreenWindow.java` | Transparent logo-only splash with a forced minimum display duration. |
| `MainWindow.java` | Creates the main frame, menu bar, split panes, fetch form, problem view, editor, status controls, preferences actions, execution flow, zoom, user state, and autosave integration. |
| `InstanceServer.java` | Sends deep-link events to `MainWindow`; behavior must remain intact during UI extraction. |

### 2.2 Theme and assets

| File | UI responsibility |
|---|---|
| `AppThemePalette.java` | Defines Light, Dark, Ultra Dark, and additional contrast/light palettes. |
| `UiIconLoader.java` | Loads and scales PNG assets and chooses light/dark variants. |
| `MainWindow.applyAppTheme()` | Writes FlatLaf/UIManager defaults for controls, menus, split panes, and scrollbars. |
| `MainWindow.EditorTheme` | Defines separate editor syntax palettes inside the main window. |
| `assets/` | Logo and themed PNG icons for run, add, copy, metrics, input, memory, time, and hint actions. |

### 2.3 Primary workspace

| File | UI responsibility |
|---|---|
| `MainWindow.java` | Owns the horizontal statement/editor split and vertical statement/test split. |
| `ProblemHtmlRenderer.java` | Produces theme-aware HTML/CSS, statement-only/full views, metadata, copy actions, sheet links, and warnings. |
| `LatexImageRenderer.java` | Converts Codeforces math into cached supersampled images. |
| `TestCasesPanel.java` | Shows sample/custom tests in tabs, provides add/remove interactions, and displays input/expected output. |
| `RSyntaxTextArea` usage in `MainWindow` | Provides the editor, syntax styles, themes, gutter, pairing, zoom, and language templates. |

### 2.4 Feedback and dialogs

| File | UI responsibility |
|---|---|
| `ExecutionResultsDialog.java` | Displays successful reports or compile failures in a fixed-size modal dialog. |
| `ExecutionResultFormatter.java` | Generates result HTML, including status, time, memory, input, actual output, expected output, and errors. |
| `PreferencesDialog.java` | Displays appearance/editor/autosave settings in a single `GridBagLayout` form. |
| `SupportDialogs.java` | Displays runtime-support details and credits. |
| `UserProfileDialog.java` | Displays loading, error, profile identity, account data, and statistics cards. |
| `JOptionPane` calls in `MainWindow`/`TestCasesPanel` | Handle confirmations, warnings, fetch errors, updates, user input, and cache clearing. |

### 2.5 Persistent state that the redesign must preserve

`AppSettings` and `SettingsRepository` persist:

- frame position and dimensions,
- horizontal and vertical divider positions,
- maximized state,
- selected language,
- editor font size and color scheme,
- application theme,
- tabs/spaces settings,
- autosave settings,
- Codeforces username.

`ProblemCacheRepository` and `ProgramCacheRepository` preserve problem HTML and per-language source history. UI work must not change these formats.

### 2.6 Behavior that must not regress

- `cpally://problem/<code>` cold launch and hot handoff.
- Fetching cached and uncached problems.
- Problem refresh without losing source.
- Empty workspace behavior.
- Source restoration per problem and language.
- Autosave and shutdown persistence.
- All current language templates and syntax modes.
- Sample extraction and custom test cases.
- Local execution, timeout, output comparison, memory/time reporting.
- Codeforces account/profile/verdict integration.
- Independent statement and editor zoom.
- Existing keyboard shortcuts.

---

## 3. Current UI assessment

The current screenshot confirms that the core layout is useful but visual hierarchy is weak.

### 3.1 Strengths to retain

- Statement and editor are visible simultaneously.
- The code editor receives most of the right-side area.
- Problem metadata is easy to find at the top of the statement.
- Test cases stay in the main window instead of requiring a terminal.
- Dark theme is coherent enough to use for long sessions.
- Split panes give users control over available space.
- The interface avoids distracting project-management features irrelevant to contests.

### 3.2 Problems visible in the current shell

1. **The top area is fragmented.** Menus are far left, zoom is far right, and editor controls appear on a second visual plane. There is no clear application/workspace header.
2. **The Run action appears as a floating triangle.** It lacks a visible label and does not form a clear command group with language/runtime state.
3. **Runtime state is cryptic.** `Yes`, a hint icon, `Status: Idle`, and `Java 21` require interpretation rather than reading as one sentence or status group.
4. **The problem and editor panes lack local headers.** The user cannot immediately distinguish global actions from statement-specific or editor-specific actions.
5. **The LaTeX warning dominates the statement.** It is useful, but its size and contrast compete with the actual problem content.
6. **Test cases are cramped below the statement.** A tab strip plus a nested horizontal split consumes height, and there is no summary of case count or execution state.
7. **The statement/test divider and main divider are visually heavy.** Grip dots and thick separators attract more attention than the content.
8. **Status information is spread across the screen.** Connectivity, user verdict, runtime availability, autosave, and execution do not share a consistent presentation.
9. **Spacing is inconsistent.** UI classes use many unrelated hard-coded paddings, control sizes, and fixed dialog dimensions.
10. **The dark palette accent is very bright green.** It works for success but is also used as a general accent, causing selected/focus states to compete with semantic success states.

### 3.3 Structural problems in the implementation

1. `MainWindow` is approximately 2,600 lines and constructs almost every primary widget.
2. UI construction, async orchestration, state transitions, settings persistence, and service calls are interleaved.
3. Theme colors are manually assigned in many classes, making live theme changes incomplete.
4. `TestCasesPanel` stores a palette snapshot and cannot naturally react to theme changes.
5. Results are rendered into HTML and shown in a modal, preventing quick edit/rerun cycles.
6. Multiple dialogs duplicate header, footer, sizing, Escape, background, and border behavior.
7. Many controls are made non-focusable to reduce visual noise, which weakens keyboard accessibility.
8. Fixed sizes (`860x620`, `640x560`, `500x350`, etc.) do not adapt well to scaling or small displays.
9. Loading is represented by replacing panel contents rather than by explicit view state.
10. The application theme and editor theme systems are related but implemented in different places.

---

## 4. Modernization goals and non-goals

### Goals

- Preserve the fast statement/editor workflow.
- Make the next primary action obvious at every state.
- Use one consistent visual language across the shell, statement, editor, tests, and dialogs.
- Reduce modal interruptions during fetching and execution.
- Make loading, errors, autosave, runtime support, and verdicts explicit and readable.
- Support keyboard use and visible focus.
- Make light/dark themes complete and switchable with minimal stale styling.
- Break `MainWindow` into testable UI components without changing service behavior.
- Keep the application responsive on the EDT.

### Non-goals

- No JavaFX/Electron rewrite.
- No project explorer, Git integration, debugger, terminal emulator, or plugin system.
- No multi-file programming model in the first modernization cycle.
- No changes to Codeforces scraping or API behavior as part of UI PRs.
- No redesign of cache formats.
- No installer rewrite.
- No animation framework; only lightweight feedback where Swing supports it safely.

---

## 5. Target user experience

### 5.1 Main frame

Keep one main horizontal split:

```text
┌────────────────────────────────────────────────────────────────────────────┐
│ Menu bar                                                                    │
├────────────────────────────────────────────────────────────────────────────┤
│ Workspace bar: [2208A / Choose problem] [Refresh]    [User] [Preferences]  │
├──────────────────────────────────┬─────────────────────────────────────────┤
│ Statement header                 │ Editor header                           │
│ D. Binary Cut                    │ Main.java     Java 21     Ready          │
│ 2s · 256 MB · Verdict            │ [Run ▶ Ctrl+R] [More…]                  │
├──────────────────────────────────┼─────────────────────────────────────────┤
│                                  │                                         │
│ Problem statement                │ Code editor                             │
│                                  │                                         │
├──────────────────────────────────┴─────────────────────────────────────────┤
│ Collapsible bottom tool window: Tests | Results                            │
├────────────────────────────────────────────────────────────────────────────┤
│ Status bar: Codeforces online · Saved · E 100% · P 100%                    │
└────────────────────────────────────────────────────────────────────────────┘
```

The exact bottom-panel span can be evaluated during implementation:

- **Preferred:** tests/results span the full window, giving input/output enough horizontal room and keeping execution feedback near the editor.
- **Fallback:** retain tests below the statement initially, but use the new test/result component architecture so moving it later is straightforward.

### 5.2 Empty/first-run state

Instead of a centered form that later disappears, show a stable workspace shell with:

- a large problem code field,
- `Fetch problem` primary action,
- `Open empty editor` secondary action,
- connectivity status,
- recently cached problems (optional, derived from existing cache in a later phase),
- disabled statement/editor surfaces with helpful empty-state copy.

This prevents the layout from changing dramatically after the first fetch.

### 5.3 Loaded problem state

- Statement header shows problem code/title, limits, verdict, refresh, and external-link action.
- The document begins directly with problem content; the LaTeX limitation becomes a compact dismissible info row.
- Practice-sheet membership appears as a compact related-content section rather than raw injected styling.
- Editor header shows source context, language, runtime readiness, and a labelled Run button.
- Bottom tool window shows tests and results with compact counts.

### 5.4 Running state

- Run button becomes `Running…` with an indeterminate progress indicator.
- Conflicting actions are disabled.
- Status bar reports the active operation.
- Tests remain visible.
- When complete, the Results tab opens automatically and focuses the first failed case.

### 5.5 Error state

Use inline banners for recoverable errors:

- fetch failed → statement-area banner with Retry and Details,
- missing runtime → editor-header banner/badge with Setup details,
- compilation failed → Results panel with compiler output and Run again,
- profile failure → profile-dialog error panel.

Reserve modal dialogs for destructive confirmation, application updates, and operations requiring an explicit decision.

---

## 6. Design system specification

Introduce a small design system before changing layouts.

### 6.1 Spacing and dimensions

Create `UiTokens.java` with semantic constants rather than scattered numeric values.

| Token | Value | Use |
|---|---:|---|
| `SPACE_1` | 4 px | icon/text micro-gap |
| `SPACE_2` | 8 px | control gap |
| `SPACE_3` | 12 px | compact panel padding |
| `SPACE_4` | 16 px | normal panel padding |
| `SPACE_5` | 24 px | section separation |
| `CONTROL_HEIGHT` | 32 px | normal buttons/fields |
| `CONTROL_HEIGHT_LARGE` | 38 px | primary fetch/run actions |
| `ICON_SMALL` | 16 px | inline/status icons |
| `ICON_NORMAL` | 20 px | toolbar icons |
| `ICON_LARGE` | 24 px | primary action icons |
| `DIVIDER_SIZE` | 8 px | split pane dividers |

Use FlatLaf client properties for corner arcs and component styling where possible instead of custom painting.

### 6.2 Typography

Define roles, not one-off font sizes:

- `TITLE`: problem/dialog title, 18–20 pt semibold.
- `SECTION`: workspace/test section title, 14–16 pt semibold.
- `BODY`: standard UI text, 13–14 pt.
- `CAPTION`: metadata and secondary state, 11–12 pt.
- `CODE`: monospaced editor/test content.

Respect the platform/FlatLaf font family. Only force monospaced fonts for source, input/output, and compiler logs.

### 6.3 Color semantics

Refine `AppThemePalette` so accent and success are distinct:

- **Accent:** selection, focus, primary actions, links.
- **Success:** accepted/passed/ready only.
- **Warning:** degraded, unknown, timeout.
- **Error:** failed, unavailable, destructive.
- **Info:** neutral notices such as the LaTeX limitation.

Add palette fields if required:

- `infoColor`,
- `linkColor`,
- `disabledTextColor`,
- `focusColor`,
- `subtleBorderColor`,
- `hoverBackground`,
- `pressedBackground`.

Do not use hard-coded colors in `MainWindow.setExecutionRunningState()` or dialogs.

### 6.4 Reusable components

Create focused reusable classes/helpers:

- `UiButtonFactory` or `UiComponents` for primary, secondary, quiet, icon, and destructive buttons.
- `StatusBadge` for `Ready`, `Running`, `Accepted`, `Offline`, etc.
- `InlineNotice` for info/warning/error banners with optional actions.
- `SectionHeader` for title, subtitle, and right-aligned actions.
- `EmptyStatePanel` for first-run/no-tests/error states.
- `ThemedCodeArea` for test input/output/compiler logs.
- `DialogShell` for consistent modal/modeless windows.

Avoid a giant generic UI utility. Each abstraction should encode a repeated visual pattern and remain easy to inspect.

### 6.5 Icons

- Keep existing PNGs during the first phases.
- Standardize all icon-only controls to 16/20/24 px tokens.
- Provide a tooltip and accessible name for every icon-only action.
- Replace textual `x`, `+`, and floating status glyphs with consistent themed icons.
- Later migrate common controls to SVG through FlatLaf Extras only if introducing that dependency is justified.

---

## 7. UI architecture changes

### 7.1 Reduce `MainWindow` to orchestration

Target responsibilities for `MainWindow`:

- create the frame and major regions,
- connect actions to services,
- coordinate application-level state,
- persist window-level settings,
- handle deep links.

Move widget construction and local rendering into components.

### 7.2 Proposed components

| Component | Responsibility |
|---|---|
| `WorkspaceBar` | Problem chooser, current problem identity, refresh, empty workspace, user/profile, preferences. |
| `ProblemViewPanel` | Statement header, inline notices, document scroll pane, loading/error/empty states. |
| `EditorPanel` | Editor header, Run action, language selector, runtime badge, source editor, editor-local actions. |
| `BottomToolWindow` | Collapsible Tests/Results tabs and resize/persistence behavior. |
| `TestCasesView` | Case list, selected-case detail, add/remove/copy actions. |
| `ExecutionResultsView` | Summary, case statuses, selected-result detail, compiler/runtime output. |
| `ApplicationStatusBar` | Connectivity, autosave, operation state, zoom, optional update status. |
| `ThemeManager` | FlatLaf setup, palette publication, component-tree refresh. |
| `ActionRegistry` | Shared Swing `Action` instances for menu items, buttons, and shortcuts. |

### 7.3 Shared actions

Replace duplicate listeners with Swing `Action` objects:

- choose problem,
- fetch/refresh,
- open empty,
- run,
- add test,
- preferences,
- zoom in/out/reset,
- clear cache,
- user/profile actions.

Menus, visible buttons, and keyboard shortcuts should reference the same action. Enabled state and labels then remain synchronized automatically.

### 7.4 Explicit UI state

Introduce immutable state records/enums instead of inferring state from installed panels and label strings.

```java
enum ProblemLoadState { EMPTY, LOADING, LOADED, ERROR }
enum ExecutionState { IDLE, RUNNING, COMPLETED, FAILED }
enum RuntimeState { CHECKING, READY, MISSING, UNSUPPORTED }
enum SaveState { CLEAN, DIRTY, SAVING, DISABLED }
enum ConnectivityState { CHECKING, ONLINE, DEGRADED, OFFLINE }
```

Suggested records:

```java
record ProblemViewState(
    ProblemLoadState state,
    String problemCode,
    ProblemDetails details,
    String errorMessage
) {}

record EditorViewState(
    String language,
    RuntimeState runtimeState,
    SaveState saveState,
    ExecutionState executionState
) {}
```

The UI renders state; services remain synchronous/background-worker-friendly and do not gain Swing dependencies.

### 7.5 EDT and request safety

- Continue using `SwingWorker` for network and execution work.
- Assign each fetch/profile request an identity and ignore stale completion callbacks.
- Keep all component mutation on the EDT.
- Preserve the current autosave `invokeAndWait` handoff.
- Do not perform toolchain detection synchronously on the EDT unless cached.
- Theme updates occur on the EDT and call `updateComponentTreeUI` only through `ThemeManager`.

---

## 8. Surface-by-surface redesign

## 8.1 Menus and workspace bar

Keep native application menus because they provide discoverability and keyboard conventions. Simplify their visual role and add a workspace bar below them.

Workspace bar contents:

- problem-code input/current problem button,
- Fetch or Refresh depending on state,
- Empty workspace action,
- flexible spacer,
- Codeforces user/avatar action,
- Preferences action.

Move zoom controls out of the menu bar into the status bar or local pane headers. Displaying `E 100% | P 100%` is useful but currently too prominent.

## 8.2 Problem statement

- Add a native Swing statement header above `JEditorPane`; do not encode app-level status in generated HTML.
- Keep the Codeforces document itself in `ProblemHtmlRenderer`.
- Move title, verdict, practice-sheet links, and primary actions to native controls where practical.
- Render time/memory/input-output metadata compactly under the title.
- Convert the LaTeX warning into a compact info notice with external-link action and optional session dismissal.
- Use a narrower readable content width when the pane becomes very wide.
- Preserve scroll position across zoom re-rendering by storing scrollbar proportion or document offset.
- Keep copy links and external hyperlinks functional.
- Avoid using `div#pageContent` content as presentation without validation; service validation remains separate from UI work.

## 8.3 Editor

Editor header layout:

```text
Main.<ext> / problem code     [Runtime: Ready] [Language ▾] [Run ▶]
```

- Use a labelled primary Run button, not an icon-only floating triangle.
- Show the actual runtime status (`Ready`, `g++ missing`, `Unsupported`) instead of `Yes/No`.
- Keep detailed toolchain information behind the badge or a `More` menu.
- Add autosave state near the source identity (`Saved`, `Saving`, `Autosave off`).
- Keep editor/theme settings in Preferences; avoid crowding the header.
- Preserve line numbers, folding indicator, bracket behavior, syntax schemes, and zoom.
- Ensure disabled/empty editor state contains a centered, useful empty-state overlay rather than placeholder source text.

## 8.4 Test cases

Replace one-tab-per-case as the primary navigation with a master/detail design:

```text
[Test 1] Sample       [Test 2] Sample       [+ Custom]
------------------------------------------------------
Input                        Expected output
...
```

For many cases, switch the top row into a compact list/scroll strip. Each item can later show pass/fail state.

Required interactions:

- add custom test,
- remove custom test,
- copy input,
- copy expected output,
- clear validation message,
- identify sample vs custom,
- preserve test ordering.

Use standardized code areas and avoid nested split dividers when a two-column grid is sufficient.

## 8.5 Execution results

Replace generated result HTML as the primary UI with native Swing master/detail components.

Summary row:

- passed count,
- failed count,
- timeout count,
- unknown count,
- total duration if useful.

Case list:

- status icon + text,
- case name,
- time,
- memory.

Detail:

- input,
- expected,
- actual,
- stderr,
- explanatory note.

Keep `ExecutionResultFormatter` temporarily for compatibility, then reduce it to data formatting helpers. Eliminate `IDK BRUH`; use `UNKNOWN` or `NO EXPECTED OUTPUT` in user-facing results.

The result view should live in `BottomToolWindow`. A detachable/resizable dialog may remain as an optional action, but running code should not force a modal interruption.

## 8.6 Preferences

Reorganize into categories:

### Appearance
- application theme,
- editor color scheme,
- editor font size,
- optional UI density later.

### Editor
- tabs/spaces,
- tab width.

### Saving
- autosave enabled,
- autosave interval.

Use short descriptions, aligned controls, default/reset actions, and immediate preview where safe. If full live app-theme switching remains risky, preview the palette and clearly say that applying it restarts the UI/application.

Expose only theme variants that are intentionally supported. `AppThemePalette` currently defines variants not reachable through Preferences; either expose them or remove/defer them to avoid dead design states.

## 8.7 Profile, support, and about

- Reuse `DialogShell`.
- Preserve the profile card design; it is already closer to the target style than most dialogs.
- Add an indeterminate progress indicator or skeleton instead of plain `Loading profile...` text.
- Make profile dialog resizable if content can exceed available screen height.
- Use the same status badges and button styles as the main window.
- Keep runtime support details concise and copyable.
- Move credits into About rather than maintaining a visually separate dialog style.

## 8.8 Splash and startup

- Remove the forced three-second minimum splash delay.
- Show the splash only while meaningful startup work runs, with a maximum target under one second on a warm start.
- Add application name/version beneath the logo only if it improves perceived polish.
- Never block a deep-link launch solely to satisfy splash duration.

---

## 9. Sequential implementation slices

The modernization is divided into small, dependency-ordered slices. **Implement and merge one slice before starting the next.** Each slice should be one branch and normally one PR. A slice must leave the application releasable; no PR may depend on unmerged code from a later slice.

### Slice rules

1. Branch from the latest `main` after the previous slice is merged.
2. Keep service behavior, caches, deep links, and language names unchanged unless the slice explicitly says otherwise.
3. Include before/after screenshots for visible changes.
4. Run `mvn test` and `mvn -DskipTests clean package`.
5. Complete the listed manual checks.
6. Update the tracking checklist in this document.
7. Do not mix opportunistic cleanup into a slice.

### Slice map

| Slice | Branch suggestion | Main outcome | Depends on | Risk |
|---:|---|---|---|---|
| 00 | `docs/ui-baseline` | Baseline screenshots and verification checklist | none | Low |
| 01 | `test/ui-logic-baseline` | JUnit setup and tests for existing pure UI-adjacent logic | 00 | Low |
| 02 | `feat/ui-tokens-palette` | Shared spacing/type/color tokens | 01 | Low |
| 03 | `refactor/ui-theme-manager` | Central FlatLaf/theme application | 02 | Medium |
| 04 | `feat/ui-primitives` | Reusable badges, notices, headers, buttons, empty states | 03 | Low |
| 05 | `refactor/ui-actions` | Shared Swing actions for menu/button/shortcut parity | 04 | Medium |
| 06 | `refactor/ui-view-state` | Explicit problem/runtime/execution/save states | 05 | Medium |
| 07 | `feat/ui-workspace-status-bars` | Workspace bar and application status bar | 06 | Medium |
| 08 | `refactor/ui-editor-panel` | Extract and modernize editor surface | 07 | Medium |
| 09 | `refactor/ui-problem-panel` | Extract stable problem/empty/loading/error surface | 08 | High |
| 10 | `feat/ui-shell-polish` | Integrate headers, dividers, stable shell, faster splash | 09 | Medium |
| 11 | `feat/ui-statement-modernization` | Native metadata, notice, CSS, scroll preservation | 10 | Medium |
| 12 | `refactor/ui-test-cases` | Test-case model plus native master/detail view | 11 | Medium |
| 13 | `feat/ui-native-results` | Native non-modal execution results view | 12 | High |
| 14 | `feat/ui-bottom-tool-window` | Collapsible Tests/Results tool window and persistence | 13 | High |
| 15 | `feat/ui-dialog-shell` | Shared responsive dialog infrastructure | 14 | Low |
| 16 | `feat/ui-preferences` | Categorized preferences and theme behavior | 15 | Medium |
| 17 | `refactor/ui-secondary-dialogs` | Profile, support, About, update, test-entry migration | 16 | Medium |
| 18 | `feat/ui-accessibility-polish` | Keyboard, scaling, contrast, docs, final screenshots | 17 | Medium |

---

### Slice 00 — Capture the baseline

**Purpose:** establish what must remain working before source changes begin.

**Add:**

- `docs/ui-verification-checklist.md`
- `docs/ui-baseline/README.md`
- baseline screenshots, or links to release artifacts if binary images should stay out of Git

**Work:**

1. Record empty, loaded, running, pass, compile-error, preferences, profile, Light, Dark, and Ultra Dark states.
2. Record current focus order and all documented shortcuts.
3. Document minimum-window and maximized behavior.
4. Record cached fetch, uncached fetch, empty editor, language switch, custom test, and deep-link scenarios.

**Do not:** modify Java source or visual styling.

**Validation:** execute the checklist once against the current build.

**Done when:** reviewers can compare every later visual PR against an explicit behavioral and screenshot baseline.

---

### Slice 01 — Add UI-logic test foundations

**Purpose:** protect pure behavior before extracting it from `MainWindow`.

**Modify:** `pom.xml`, `.github/workflows/ci.yml` if the CI branch has been merged.

**Add tests for:**

- `SampleTestCaseCollector`,
- `AppSettings.defaults`,
- settings load fallback/default behavior,
- output summary/format helpers that can run headlessly.

**Work:**

1. Add JUnit 5 and Surefire.
2. Change CI from skipped tests to `mvn test`/`mvn verify`.
3. Keep all tests headless; do not open Swing windows in CI.

**Do not:** refactor production UI merely to increase coverage.

**Validation:** tests pass on Windows and Linux; shaded JAR still builds.

**Done when:** the repository has a reliable test command used by CI.

---

### Slice 02 — Introduce UI tokens and repair color semantics

**Purpose:** remove arbitrary visual constants before building components.

**Add:** `UiTokens.java`.

**Modify:** `AppThemePalette.java` and only the smallest number of consumers needed to compile.

**Work:**

1. Define spacing, control heights, icon sizes, divider size, and typography roles.
2. Separate general accent from semantic success.
3. Add explicit info/link/disabled/focus/subtle-border colors if required.
4. Keep `Light`, `Dark`, and `Ultra Dark`; decide whether unused variants are exposed or deferred.
5. Add palette selection/contrast-oriented unit tests where practical.

**Do not:** change the main layout or introduce reusable widgets yet.

**Manual checks:** all themes launch; editor syntax themes remain unchanged; accepted/passed still read as success.

**Done when:** new UI code can avoid raw spacing and semantic color constants.

---

### Slice 03 — Centralize FlatLaf and theme application

**Purpose:** create one authoritative path for applying and refreshing application themes.

**Add:** `ThemeManager.java`.

**Modify:** `MainWindow.java`, `App.java` only if startup ownership needs adjustment.

**Work:**

1. Move `MainWindow.applyAppTheme()` UIManager configuration into `ThemeManager`.
2. Expose current palette and a single EDT-safe refresh method.
3. Replace hard-coded execution-state colors with palette semantics.
4. Define how long-lived components subscribe to or receive theme updates.
5. Keep editor syntax theme application separate but explicitly coordinated.

**Do not:** promise live theme switching yet; establish the mechanism first.

**Manual checks:** launch in each theme, menus/dialog controls/scrollbars render correctly, startup order is unchanged.

**Done when:** `MainWindow` no longer owns global Look and Feel configuration.

---

### Slice 04 — Build reusable UI primitives

**Purpose:** encode repeated visual patterns before modernizing real surfaces.

**Add:**

- `StatusBadge.java`
- `InlineNotice.java`
- `SectionHeader.java`
- `EmptyStatePanel.java`
- focused button/style helpers

**Work:**

1. Support info/success/warning/error/neutral badge variants with text.
2. Support notice title/body, optional details, and optional action.
3. Support section title/subtitle/right-side actions.
4. Standardize primary, secondary, quiet, destructive, and icon-only buttons.
5. Give icon-only controls tooltips and accessible names by construction.

**Adopt narrowly in:** one low-risk dialog or status area to prove reuse.

**Do not:** convert every screen in this slice.

**Manual checks:** all variants in Light/Dark/Ultra Dark; keyboard focus visible.

**Done when:** primitives are reused and require no service knowledge.

---

### Slice 05 — Create a shared action registry

**Purpose:** ensure menus, buttons, and keyboard shortcuts use the same enabled state and behavior.

**Add:** `ActionRegistry.java` or small domain-specific action classes.

**Modify:** `MainWindow.java` menu/button/keybinding construction.

**Convert:** choose problem, open empty, refresh, run, add test, preferences, zoom, cache clearing, user/profile, exit.

**Work:**

1. Use Swing `Action` instances for visible buttons and menu items.
2. Preserve every current accelerator.
3. Centralize action enabled-state updates.
4. Keep action callbacks delegated to existing orchestration methods.

**Do not:** redesign controls or move panels.

**Manual checks:** invoke every action from menu, button where present, and shortcut; deep-link opening still works.

**Done when:** there is one implementation and one enabled state per user action.

---

### Slice 06 — Introduce explicit view state

**Purpose:** stop deriving state from label text, booleans, and which component is installed.

**Add:** problem, runtime, execution, save, and connectivity state enums/records.

**Modify:** `MainWindow.java` state transitions and action enablement.

**Work:**

1. Model empty/loading/loaded/error problem states.
2. Model checking/ready/missing/unsupported runtime states.
3. Model idle/running/completed/failed execution states.
4. Model clean/dirty/saving/disabled save states.
5. Add a request identity so stale problem-fetch completions cannot replace newer selections.
6. Map state to existing labels/components without changing layout.

**Do not:** move network or execution logic into state classes.

**Automated checks:** state-to-action enablement and state transition tests.

**Done when:** UI state is explicit, typed, and independent of displayed strings.

---

### Slice 07 — Add workspace and application status bars

**Purpose:** establish clear global context without moving the main content.

**Add:** `WorkspaceBar.java`, `ApplicationStatusBar.java`.

**Work:**

1. Workspace bar: problem chooser/current code, fetch/refresh, empty action, user/profile, preferences.
2. Status bar: Codeforces connectivity, autosave state, operation state, editor/problem zoom.
3. Bind controls to `ActionRegistry` and typed view state.
4. Remove zoom controls and scattered global status from the embedded menu bar after parity is verified.

**Do not:** extract the editor/problem panes yet.

**Manual checks:** empty and loaded states, signed in/out, zoom changes, online/offline, autosave on/off, minimum width.

**Done when:** global actions and status have stable, readable locations.

---

### Slice 08 — Extract and modernize the editor panel

**Purpose:** remove editor widget construction from `MainWindow` and clarify its controls.

**Add:** `EditorPanel.java`; optionally extract editor-theme mapping to its own class.

**Move without behavior change:**

- `RSyntaxTextArea` creation,
- scroll pane/gutter setup,
- editor zoom application,
- syntax/theme application,
- language selector,
- runtime badge,
- Run button and local header.

**Work:**

1. Use a labelled primary `Run` button.
2. Replace `Yes/No` with `Ready`, `<tool> missing`, or `Unsupported`.
3. Show save state near source identity.
4. Replace placeholder source text with a real empty-state overlay.
5. Keep templates, cached source restoration, bracket pairing, and wheel behavior identical.

**Do not:** change `CodeExecutionService` or language display strings.

**Manual checks:** every language family, cached source, empty workspace, zoom, typing/pairs, missing runtime, Ctrl+R.

**Done when:** editor construction and editor-local state are owned by `EditorPanel`.

---

### Slice 09 — Extract a stable problem panel

**Purpose:** stop replacing the entire left container during fetch transitions.

**Add:** `ProblemViewPanel.java`.

**Move:** problem entry/empty/loading/error/loaded presentation, statement scroll pane, submission status placement.

**Work:**

1. Use a stable card/state layout for EMPTY, LOADING, LOADED, and ERROR.
2. Show fetch errors inline with Retry and Details.
3. Keep `JEditorPane`, hyperlink handling, copy payloads, and scroll behavior intact.
4. Keep test cases in their existing location for this slice.
5. Expose narrow methods/view-state setters instead of raw widget access.

**Do not:** rewrite `ProblemHtmlRenderer` or move the test panel yet.

**Manual checks:** cached/uncached fetch, invalid code, bot-check/network failure, refresh, rapid problem switching, external/copy links.

**Done when:** problem state changes no longer rebuild the main left-side hierarchy.

---

### Slice 10 — Integrate and polish the main shell

**Purpose:** complete the new visual hierarchy after component extraction.

**Modify:** `MainWindow.java`, extracted workspace components, `SplashScreenWindow.java`, `App.java` startup timing.

**Work:**

1. Add statement and editor section headers.
2. Normalize outer padding and control gaps with tokens.
3. Reduce split divider size/visual weight.
4. Keep sensible minimum widths and resize clamping.
5. Remove the forced three-second splash delay; show it only during actual startup.
6. Ensure initial and loaded layouts occupy the same stable frame.

**Do not:** move tests/results or alter statement HTML yet.

**Manual checks:** first launch, warm launch, cold/hot deep links, minimum/maximized window, resize during fetch.

**Done when:** the shell matches the target hierarchy while preserving the existing content arrangement.

---

### Slice 11 — Modernize statement presentation

**Purpose:** improve reading and make metadata/notices native and consistent.

**Modify:** `ProblemViewPanel.java`, `ProblemHtmlRenderer.java`, possibly `RenderedProblemView.java` if structured metadata is needed.

**Work:**

1. Place title/code, limits, verdict, refresh, and external link in a native header.
2. Convert the large LaTeX warning into `InlineNotice`.
3. Improve HTML typography, code blocks, samples, links, and copy affordances.
4. Present practice-sheet links consistently.
5. Preserve scrollbar position during zoom/re-render.
6. Keep LaTeX image caching/rendering behavior intact.

**Do not:** modify Codeforces transport/fetch validation in this UI slice.

**Manual checks:** short/long/image-heavy/LaTeX-heavy statements, all themes, all zoom ranges, copy links, sheet links.

**Done when:** the problem document is easier to read and native app status is outside generated HTML.

---

### Slice 12 — Separate test-case data from its view

**Purpose:** prepare test cases for reuse in a bottom tool window and result integration.

**Add:** a small `TestCaseModel`/controller and `TestCasesView.java`.

**Refactor:** `TestCasesPanel.java` and custom-test dialog invocation.

**Work:**

1. Preserve sample collection and custom ordering.
2. Create master/detail case navigation.
3. Standardize input/expected code areas.
4. Add copy, add, and remove actions.
5. Identify Sample versus Custom with text/badge.
6. Keep execution-spec generation behavior identical.

**Do not:** move the component or show result statuses yet.

**Automated checks:** ordering, renumbering, expected-output optionality, delete behavior.

**Done when:** test data can be displayed independently of its current parent split pane.

---

### Slice 13 — Build native execution results

**Purpose:** replace HTML-only modal results with a reusable native view.

**Add:** `ExecutionResultsView.java` and result view-model mapping if useful.

**Refactor:** `ExecutionResultsDialog.java`, `ExecutionResultFormatter.java`.

**Work:**

1. Add summary counts.
2. Add result master list with status text, time, and memory.
3. Add detail sections for input, expected, actual, stderr, and note.
4. Focus/select the first failed case.
5. Use `NO EXPECTED OUTPUT` instead of `IDK BRUH`.
6. Keep a dialog wrapper temporarily so behavior remains available before Slice 14.

**Do not:** change execution comparison or process behavior.

**Automated checks:** mapping for pass, wrong answer, timeout, runtime error, compile error, unknown expected output.

**Done when:** one native component renders every `ExecutionReport` state.

---

### Slice 14 — Add the Tests/Results bottom tool window

**Purpose:** eliminate mandatory modal interruption from the edit/run/inspect loop.

**Add:** `BottomToolWindow.java`.

**Modify:** main shell composition, `AppSettings`, `SettingsRepository`, run-completion handling.

**Work:**

1. Add collapsible `Tests` and `Results` tabs spanning the workspace.
2. Move `TestCasesView` into Tests.
3. Show `ExecutionResultsView` after execution and select first failure.
4. Add collapse/expand and optional detach action.
5. Persist open state, selected tab, and height using new keys with defaults.
6. Preserve old divider settings and migrate safely.

**Do not:** remove the optional result dialog until the tool window is proven stable.

**Manual checks:** run repeatedly, edit after result, collapse/expand, restart persistence, minimum window, large test data.

**Done when:** edit → run → inspect → edit works without a required modal dialog.

---

### Slice 15 — Introduce a shared dialog shell

**Purpose:** standardize secondary windows before migrating their content.

**Add:** `DialogShell.java`.

**Work:**

1. Standard header/content/footer spacing.
2. Default button and Escape-to-close behavior.
3. Responsive preferred/minimum sizing and screen bounds.
4. Theme refresh support.
5. Optional modal/modeless configuration.
6. Adopt it in one low-risk dialog, such as runtime support.

**Do not:** redesign all dialog contents in one PR.

**Manual checks:** keyboard behavior, resizing, multi-monitor positioning, all themes.

**Done when:** the shell is proven reusable without content-specific assumptions.

---

### Slice 16 — Rebuild Preferences

**Purpose:** make configuration understandable and prepare reliable theme changes.

**Modify:** `PreferencesDialog.java`, theme/settings integration.

**Work:**

1. Categorize Appearance, Editor, and Saving settings.
2. Add descriptions and Reset/Cancel/Apply behavior.
3. Clarify exposed application themes and editor-scheme compatibility.
4. Implement live preview if stable; otherwise explicitly require restart.
5. Preserve every existing setting and default.

**Do not:** add unrelated new preferences beyond optional UI density if it is already needed.

**Manual checks:** save/cancel/reset, each theme and editor scheme, invalid bounds, restart/persistence.

**Done when:** settings are grouped, understandable, keyboard accessible, and backward compatible.

---

### Slice 17 — Migrate secondary dialogs

**Purpose:** finish visual consistency outside the main workspace.

**Modify:** `SupportDialogs.java`, `UserProfileDialog.java`, `ExecutionResultsDialog.java`, custom-test entry, About/update/error flows.

**Work:**

1. Adopt `DialogShell` incrementally.
2. Add profile progress state and responsive sizing.
3. Consolidate Credits into About.
4. Standardize confirmations and error-details disclosure.
5. Ensure compiler logs and runtime details are selectable/copyable.
6. Remove obsolete result-dialog code only after the bottom tool window has parity.

**Manual checks:** loading/error/success for each dialog, Enter/Escape, screen bounds, all themes.

**Done when:** all secondary surfaces share layout, styling, sizing, and keyboard conventions.

---

### Slice 18 — Accessibility, scaling, and release polish

**Purpose:** close the modernization with systematic quality checks.

**Work:**

1. Audit Tab/Shift+Tab order and restore focusability where disabled unnecessarily.
2. Add accessible names/descriptions to icons and custom components.
3. Verify every semantic status includes text and not only color.
4. Test 100%, 125%, 150%, and 200% scaling.
5. Test minimum/maximized/multi-monitor placement.
6. Run the full UI verification checklist.
7. Update README screenshots, feature descriptions, and shortcuts.
8. Remove compatibility UI paths and dead styles confirmed unused.

**Do not:** introduce a new visual direction in the polish slice.

**Done when:** all acceptance criteria in Section 11 pass and the modernization is ready for release.

---

## 10. Settings and migration strategy

Never reinterpret existing keys with incompatible meanings.

Keep:

- all current window keys,
- `window.dividerLocation`,
- `window.testCasesDividerLocation`,
- language/editor/theme/autosave/user keys.

Add new keys only when the corresponding feature lands:

```properties
window.bottomToolOpen=true
window.bottomToolHeight=280
window.bottomToolTab=tests
ui.latexNoticeDismissed=false
ui.density=comfortable
```

Migration rules:

- Missing keys use defaults.
- Old divider values continue to seed the new layout where possible.
- Settings write failures remain non-fatal.
- Problem and source cache formats are untouched.
- Language display strings remain stable so cached code continues to resolve.

---

## 11. Testing and verification matrix

### Automated

- Unit tests for view-state reducers/mapping logic.
- Unit tests for settings defaults and new-key migration.
- Unit tests for result summary/status mapping.
- Unit tests for sample/custom test ordering.
- Headless construction tests only where reliable; do not make CI depend on a display server.
- Maven build and tests on Windows and Linux.

### Manual states

| Area | Required cases |
|---|---|
| Startup | normal, deep link, secondary-instance handoff, warm cache |
| Problem | empty, loading, cached, uncached, invalid, refresh, fetch failure |
| Statement | long title, image-heavy, LaTeX-heavy, many samples, no samples |
| Editor | disabled, cached source, language switch, long lines, zoom extremes |
| Runtime | ready, missing compiler, unsupported language, detection delay |
| Execution | pass, wrong answer, timeout, runtime error, compile error, no expected output |
| Tests | one sample, many samples, add/remove custom, long input/output |
| User | signed out, valid user, invalid user, avatar failure, verdict states |
| Theme | Light, Dark, Ultra Dark, mid-session theme change |
| Window | minimum, normal, maximized, resize during fetch/run, second monitor |
| Scale | 100%, 125%, 150%, 200% |
| Keyboard | all documented shortcuts, Tab/Shift+Tab, Enter default, Escape close |

### Acceptance criteria

1. The complete solve loop remains available in one window.
2. The user can identify Fetch, Run, language, runtime readiness, and result status without opening a menu.
3. Running code does not force a modal results interruption.
4. Fetch errors include Retry and readable details.
5. The UI remains usable at `1000x680` and scales cleanly above it.
6. Every primary action is keyboard accessible.
7. Focus is visible.
8. Semantic state is not conveyed by color alone.
9. Theme switching leaves no obvious stale colors.
10. Existing settings, caches, deep links, autosave, and execution behavior do not regress.
11. No network/process operation blocks the EDT.
12. CI remains green on Windows and Linux.

---

## 12. Risks and mitigations

| Risk | Mitigation |
|---|---|
| Large `MainWindow` refactor causes regressions | Extract components without layout changes first; modernize only after behavior parity. |
| Swing theme updates leave stale colors | Central `ThemeManager`; avoid storing immutable palette snapshots in long-lived views. |
| Bottom tool window reduces editor space | Make it collapsible, resizable, persisted, and keyboard-toggleable. |
| Native result components duplicate formatter logic | Introduce result view models and keep formatter temporarily during migration. |
| HTML statement differs across themes/zoom | Keep renderer isolated; test representative Codeforces statements and preserve scroll state. |
| Accessibility conflicts with non-focusable controls | Use shared actions and normal tab behavior; only exclude truly decorative components. |
| New settings break old installations | Add-only keys with defaults; never change cache formats. |
| UI work conflicts with service fixes | Use small UI-only branches and avoid touching fetch/execution services except for typed state adapters. |
| Scope expands into a full IDE rewrite | Enforce non-goals and phase exit criteria. |

---

## 13. Branch and PR strategy

The existing branches are separate concerns:

- `fix/codeforces-fetch-resilience`
- `ci/github-actions-setup`
- `docs/ui-modernization-plan` (this document)

Do not implement modernization on those branches. Merge prerequisite work, update local `main`, then create the branch listed for the next incomplete slice in Section 9.

### Sequential merge policy

1. Only one modernization slice should be active unless a second branch is documentation-only.
2. Every implementation branch starts from `main` after the previous slice merges.
3. Avoid stacked PRs; they obscure regressions and complicate visual review.
4. If a slice grows beyond its declared scope, split it before review rather than expanding the PR.
5. Fix defects introduced by a slice on that slice's branch before moving forward.
6. Update this plan in the implementation PR by checking the slice and recording material decisions.

### Required PR content

Each slice PR should include:

- slice number and title,
- exact scope and explicit exclusions,
- screenshots for every affected theme/state,
- automated checks run,
- manual checklist cases run,
- settings/cache compatibility statement,
- keyboard/accessibility impact,
- follow-up items deferred to later slices.

---

## 14. Recommended next work

Start with **Slice 00 (`docs/ui-baseline`)**, not the design system. The baseline is required before judging later visual work.

After Slice 00 merges, complete Slice 01 so CI protects existing pure behavior. Only then begin visual source changes in Slice 02.

### Slice 00 immediate actions

1. Build the current shaded JAR. **Completed:** `mvn -B -ntp -DskipTests clean package` passed on 2026-07-14.
2. Capture the states listed in Slice 00 using the existing UI. **Partially completed:** the existing loaded Dark-theme screenshot is recorded; the checklist identifies remaining interactive captures.
3. Add `docs/ui-verification-checklist.md`. **Completed.**
4. Record current shortcuts, focus behavior, window sizes, and divider defaults. **Completed in the checklist and baseline README.**
5. Perform the checklist once and record known pre-existing defects separately from modernization regressions. **Interactive sign-off remains required before Slice 01/02 visual review.**
6. Mark Slice 00 complete below and merge before creating Slice 01. **Completed for the documentation baseline.**

---

## 15. Slice tracking checklist

- [x] Slice 00 — Capture the baseline
- [x] Slice 01 — Add UI-logic test foundations
- [x] Slice 02 — Introduce UI tokens and repair color semantics
- [x] Slice 03 — Centralize FlatLaf and theme application
- [x] Slice 04 — Build reusable UI primitives
- [x] Slice 05 — Create a shared action registry
- [x] Slice 06 — Introduce explicit view state
- [x] Slice 07 — Add workspace and application status bars
- [ ] Slice 08 — Extract and modernize the editor panel
- [ ] Slice 09 — Extract a stable problem panel
- [ ] Slice 10 — Integrate and polish the main shell
- [ ] Slice 11 — Modernize statement presentation
- [ ] Slice 12 — Separate test-case data from its view
- [ ] Slice 13 — Build native execution results
- [ ] Slice 14 — Add the Tests/Results bottom tool window
- [ ] Slice 15 — Introduce a shared dialog shell
- [ ] Slice 16 — Rebuild Preferences
- [ ] Slice 17 — Migrate secondary dialogs
- [ ] Slice 18 — Accessibility, scaling, and release polish

### Decisions locked for the first cycle

- [x] Retain Java 17, Swing, FlatLaf, and RSyntaxTextArea.
- [x] Preserve the side-by-side statement/editor workflow.
- [x] Keep existing caches, settings keys, deep links, and execution semantics.
- [x] Refactor architecture before major layout changes.
- [x] Use native Swing components for shell/status/results and retain HTML for the problem document.
- [x] Keep UI modernization in dedicated, small branches.
