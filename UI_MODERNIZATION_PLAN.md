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

## 9. Phased implementation plan

Each phase should be its own reviewable PR. Do not combine fetch-service changes with visual refactors.

### Phase 0 — Baseline and guardrails

**Files:** documentation/tests only.

Tasks:

1. Capture screenshots for empty, loaded, running, successful results, compile failure, preferences, profile, Light, Dark, and Ultra Dark states.
2. Add `docs/ui-verification-checklist.md`.
3. Add unit tests for pure functions already adjacent to the UI:
   - `SampleTestCaseCollector`,
   - editor theme/name mapping after extraction,
   - zoom clamp logic after extraction,
   - settings defaults/migration.
4. Ensure GitHub Actions runs `mvn test` once tests exist.

**Exit criteria:** repeatable baseline and no application behavior changes.

### Phase 1 — Design system foundation

**New files:**

- `UiTokens.java`
- `ThemeManager.java`
- `StatusBadge.java`
- `InlineNotice.java`
- `SectionHeader.java`
- focused component styling helpers

**Modified files:**

- `AppThemePalette.java`
- `UiIconLoader.java`
- `MainWindow.java` only to consume the theme manager

Tasks:

1. Separate accent from success colors.
2. Centralize spacing, dimensions, typography, and icon sizing.
3. Centralize FlatLaf/UIManager setup.
4. Replace hard-coded execution colors.
5. Establish visible focus and hover behavior.

**Exit criteria:** no major layout change; primary controls and statuses use shared tokens/components in all themes.

### Phase 2 — Action and state extraction

**New files:**

- `ActionRegistry.java`
- UI state enums/records

Tasks:

1. Convert menu items, buttons, and shortcuts to shared Swing actions.
2. Make action enabled state derive from explicit problem/execution/runtime state.
3. Add stale-request protection for problem fetch completion.
4. Preserve all shortcuts and deep-link behavior.

**Exit criteria:** behavior remains identical, but labels/listeners no longer encode state ad hoc.

### Phase 3 — Extract workspace components

**New files:**

- `WorkspaceBar.java`
- `ProblemViewPanel.java`
- `EditorPanel.java`
- `ApplicationStatusBar.java`

Tasks:

1. Move widget construction out of `MainWindow`.
2. Keep existing horizontal and vertical splits during extraction.
3. Give statement and editor explicit local headers.
4. Move zoom and connection/autosave state to the status bar.
5. Replace initial placeholder source text with proper empty states.

**Exit criteria:** `MainWindow` primarily coordinates components/services and is substantially smaller; existing workflows remain intact.

### Phase 4 — Shell visual modernization

Tasks:

1. Add the workspace bar below the menu.
2. Redesign pane headers and primary Run action.
3. Reduce divider weight and normalize spacing.
4. Replace cryptic status labels with badges/text.
5. Make the initial/loaded layout stable instead of replacing the entire left panel.
6. Remove the forced splash delay.

**Exit criteria:** the main window matches the target hierarchy and works at minimum size and common scaling factors.

### Phase 5 — Statement and error-state modernization

Tasks:

1. Add native statement metadata header.
2. Convert LaTeX warning into `InlineNotice`.
3. Add inline loading/error/retry states.
4. Improve statement CSS and link/copy affordances.
5. Preserve scroll position during zoom and refresh.
6. Present sheet links consistently.

**Exit criteria:** fetching and reading do not cause disruptive layout swaps or generic modal errors.

### Phase 6 — Tests and results tool window

**New/refactored files:**

- `BottomToolWindow.java`
- `TestCasesView.java`
- `ExecutionResultsView.java`

Tasks:

1. Refactor `TestCasesPanel` into data/state plus master/detail view.
2. Build native results components from `ExecutionReport`.
3. Open Results automatically after execution.
4. Focus the first failure.
5. Retain optional detach/open-in-dialog action.
6. Persist bottom tool-window size/open state with new settings keys.

**Exit criteria:** edit → run → inspect → edit requires no mandatory modal dialog.

### Phase 7 — Dialog consolidation and live theming

Tasks:

1. Introduce and adopt `DialogShell`.
2. Rebuild Preferences into categories.
3. Migrate execution fallback dialog, test editor dialog, support, profile, About, and update dialogs.
4. Implement live theme refresh where reliable.
5. Ensure default buttons, Escape behavior, focus order, and responsive sizing.

**Exit criteria:** all dialogs share styling and keyboard behavior; theme changes do not leave stale components.

### Phase 8 — Accessibility and release polish

Tasks:

1. Audit keyboard traversal and focus rings.
2. Add accessible names/descriptions to icon-only controls.
3. Ensure statuses include text, not color alone.
4. Test 100%, 125%, 150%, and 200% scaling.
5. Test minimum, maximized, and multi-monitor placement.
6. Update README screenshots and shortcuts.
7. Run the complete manual verification matrix.

**Exit criteria:** UI modernization release candidate is stable, documented, and passes CI/manual checks.

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

Do not implement UI modernization directly on either branch. After they are merged, branch from updated `main`.

Recommended sequence:

1. `docs/ui-baseline-and-checklist`
2. `feat/ui-design-system`
3. `refactor/ui-actions-and-state`
4. `refactor/ui-workspace-components`
5. `feat/ui-shell-modernization`
6. `feat/ui-statement-states`
7. `feat/ui-tests-results-tool-window`
8. `feat/ui-dialogs-accessibility`

Each PR should:

- contain one coherent surface or architectural step,
- include before/after screenshots for visual changes,
- list manual test cases performed,
- preserve keyboard shortcuts,
- avoid unrelated service/repository changes,
- update this plan's status/checklist.

---

## 14. Recommended first implementation PR

Create `feat/ui-design-system` after the CI and Codeforces branches are merged.

### Scope

1. Add `UiTokens`.
2. Add `ThemeManager` and move `MainWindow.applyAppTheme()` logic into it.
3. Separate accent and success colors in `AppThemePalette`.
4. Add `StatusBadge`, `InlineNotice`, and `SectionHeader`.
5. Replace hard-coded execution colors and the most repeated border/padding values.
6. Standardize primary/secondary/icon button styling.
7. Add tests for theme selection and any extracted pure mappings.
8. Capture screenshots in all exposed themes.

### Explicitly defer

- No split-pane relocation.
- No bottom tool window yet.
- No result-dialog replacement yet.
- No service changes.
- No settings format change.

### Definition of done

- Application behavior is unchanged.
- Light, Dark, and Ultra Dark render consistently.
- Accent, success, warning, and error are visually distinct.
- Focus states are visible.
- New components are demonstrably reused rather than being speculative abstractions.
- Maven build/tests pass on Windows and Linux.
- This plan is updated with completed items and any changed decisions.

---

## 15. Plan tracking checklist

- [ ] Phase 0: Baseline and guardrails
- [ ] Phase 1: Design system foundation
- [ ] Phase 2: Action and state extraction
- [ ] Phase 3: Workspace component extraction
- [ ] Phase 4: Shell visual modernization
- [ ] Phase 5: Statement and error states
- [ ] Phase 6: Tests/results bottom tool window
- [ ] Phase 7: Dialog consolidation and live theming
- [ ] Phase 8: Accessibility and release polish

### Decisions locked for the first cycle

- [x] Retain Java 17, Swing, FlatLaf, and RSyntaxTextArea.
- [x] Preserve the side-by-side statement/editor workflow.
- [x] Keep existing caches, settings keys, deep links, and execution semantics.
- [x] Refactor architecture before major layout changes.
- [x] Use native Swing components for shell/status/results and retain HTML for the problem document.
- [x] Keep UI modernization in dedicated, small branches.
