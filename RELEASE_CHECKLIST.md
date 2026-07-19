# CP Ally IDE Release Checklist

## Automated verification

- [ ] `mvn -B -ntp test`
- [ ] `mvn -B -ntp package`
- [ ] Confirm the shaded JAR exists and contains `Main-Class: com.example.App`.
- [ ] Verify CI passes on Ubuntu and Windows.
- [ ] Review Surefire reports for skipped or flaky tests.

## Functional smoke test

- [ ] Start offline and confirm the editor opens without waiting for network services.
- [ ] Fetch a cached and uncached Codeforces problem.
- [ ] Switch between full statement and statement-only views.
- [ ] Verify sample tests, custom tests, import/export, duplicate, edit, and per-test run.
- [ ] Verify Run, Stop, timeout, output-limit, compile-error, runtime-error, and accepted-result paths.
- [ ] Restore a source snapshot after a language/problem switch.
- [ ] Test Recent Problems, browser opening, and source-copy/submit-page workflow.
- [ ] Check update behavior for same, older, and newer GitHub release versions.

## UI and accessibility

- [ ] Check Light, Dark, and Ultra Dark themes.
- [ ] Check 100%, 125%, 150%, and 200% display scaling.
- [ ] Navigate the primary workflow with keyboard only.
- [ ] Verify Ctrl+F, Ctrl+H, Ctrl+G, Ctrl+R, Ctrl+Shift+T, and Ctrl+0 from each major pane.
- [ ] Verify icon-only controls have accessible names and visible focus.
- [ ] Open Credits, Diagnostics, Preferences, and execution results with Escape/default-button behavior.
- [ ] Confirm workspace geometry and splitter orientation are unchanged.

## Data and cleanup

- [ ] Upgrade from an older settings file and verify defaults/migration.
- [ ] Verify source, custom-test, problem, LaTeX, icon, and snapshot data survive upgrade.
- [ ] Confirm stale cache cleanup does not remove source or test data.
- [ ] Confirm Diagnostics output contains no source, credentials, or personal tokens.
- [ ] Confirm application shutdown leaves no child compiler/runner processes.
