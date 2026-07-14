# UI Baseline — Slice 00

This directory records the pre-modernization UI baseline for CP Ally IDE.

## Baseline source

- Application screenshot: [`../../screenshot.png`](../../screenshot.png)
- Full verification checklist: [`../ui-verification-checklist.md`](../ui-verification-checklist.md)
- Modernization plan: [`../../UI_MODERNIZATION_PLAN.md`](../../UI_MODERNIZATION_PLAN.md)

The existing screenshot represents the loaded dark-theme workspace: a Codeforces statement and test cases on the left, and the editor/runtime toolbar on the right. It is retained as the first visual comparison artifact rather than duplicated under `docs/`.

## Current layout contract

The following structure is the behavior and layout baseline that later UI slices must preserve until an explicit slice changes it:

1. FlatLaf application menu/title bar at the top.
2. Horizontal split between the problem workspace and editor.
3. Vertical split between the problem statement and test cases inside the problem workspace.
4. Statement rendered in a scrollable HTML `JEditorPane`.
5. Test cases represented by sample/custom tabs with input and expected-output panes.
6. Editor rendered by RSyntaxTextArea with language selection and Run action.
7. Independent problem/editor zoom controls.
8. User verdict status displayed near the problem view when a Codeforces handle is configured.
9. Window, divider, language, theme, editor, and autosave settings persisted across restarts.

## Baseline state matrix

| State | Entry/action | Expected current behavior | Visual artifact |
|---|---|---|---|
| Empty first run | Launch without selecting a problem | Problem-code field and Codeforces fetch form are shown; editor is disabled with a starter message | Not separately captured; use current application state |
| Loaded cached problem | Open a previously fetched code | Cache loads without a new statement fetch; source cache is restored per language | `../../screenshot.png` represents loaded workspace |
| Loaded uncached problem | Fetch a new code | Loading panel appears, then statement/test cases/editor become available | Capture during future manual run |
| Running | Press Run / `Ctrl+R` | Run control disables, execution state changes to Running, then results dialog opens | Capture during future manual run |
| Passing result | Execute a correct solution | Results show passed count, time, memory, and per-case output | Capture during future manual run |
| Compile/runtime failure | Execute invalid or failing source | Results dialog shows compilation/runtime details | Capture during future manual run |
| Preferences | `Ctrl+P` | Modal preferences form opens with theme/editor/autosave settings | Capture during future manual run |
| Profile | File → Show Profile | Loading profile dialog transitions to profile/error state | Capture during future manual run |
| Light theme | Preferences → Light, restart | Light FlatLaf shell and editor/application colors render | Capture during future manual run |
| Dark theme | Default/restart | Dark shell and editor render | `../../screenshot.png` |
| Ultra Dark theme | Preferences → Ultra Dark, restart | Ultra Dark shell and editor render | Capture during future manual run |
| Empty workspace | `Ctrl+E` | Empty problem view opens; source remains ephemeral and is not cached | Capture during future manual run |
| Custom test | `Ctrl+Shift+T` | Add-test dialog opens; new custom case appears and can be removed | Capture during future manual run |
| Deep link | `cpally://problem/<code>` | Cold launch or hot handoff opens the requested problem | Capture/log verification during future manual run |

## Current visual observations

These observations are intentionally descriptive rather than prescriptive:

- The statement/editor split is the strongest current workflow and must be preserved through the first architecture slices.
- The top menu bar, zoom controls, editor Run icon, runtime indicator, and language selector are visually separated rather than forming one clear command hierarchy.
- The problem pane has strong title and metadata treatment, but the LaTeX warning competes with the statement content.
- Test cases occupy a narrow lower region with a tab strip and nested split, making long input/output difficult to scan.
- The bright green accent is used for general emphasis as well as success state.
- Several global statuses are spread between the menu bar, editor toolbar, problem pane, and labels.
- The interface uses many fixed dimensions and manual borders/paddings.

## Baseline validation record

Slice 00 validation was performed on 2026-07-14:

- `mvn -B -ntp test` — passed; no test sources exist yet.
- `mvn -B -ntp -DskipTests clean package` — passed; shaded JAR produced.
- Existing `screenshot.png` reviewed and recorded as the loaded Dark-theme visual artifact.
- Full interactive checklist added at `../ui-verification-checklist.md` for the next manual UI session.

The checklist is intentionally a reusable verification template. Its blank sign-off and defect-log fields must be completed during an interactive session before the first visual code slice is reviewed. This prevents unobserved display-specific behavior from being treated as a regression or as an approved baseline.

This directory should receive additional screenshots only when they are captured from a reproducible environment. Do not use generated or speculative screenshots as visual baselines.

## Baseline boundary

Slice 00 intentionally changes no Java source, theme, layout, persistence, cache, network, or execution behavior. Its only purpose is to make later visual changes reviewable and regressions attributable.
