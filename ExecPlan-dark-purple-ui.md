# Redesign UTracker with a dark purple visual system

This ExecPlan is a living document and follows the repository guidance in `Plan.md` because `.agent/PLANS.md` is not present in the working tree.

## Purpose / Big Picture

UTracker should feel cohesive and premium instead of inheriting the default Material colors. After this change, every Compose screen uses a deliberate dark-purple Material 3 color scheme, with lavender accents for actions and readable high-contrast text. A static preview in `design/utracker-dark-purple-preview.svg` provides a quick visual check.

## Progress

- [x] (2026-08-31) Inspect existing theme and confirm all screens consume `MaterialTheme`.
- [x] (2026-08-31) Add dark-purple Material 3 color scheme and window status-bar colors.
- [x] (2026-08-31) Add a visual preview artifact.
- [ ] Build and run Android verification (blocked by environment: Android tooling cannot access `C:\.android`).

## Surprises & Discoveries

- Observation: The project has only `Theme.kt`; it does not have a separate `Color.kt` palette file.
  Evidence: `app/src/main/java/com/example/ultimatetracker/ui/theme/Theme.kt` directly calls `darkColorScheme()` and `lightColorScheme()`.
- Observation: The requested `.agent/PLANS.md` path is absent; `Plan.md` contains the repository's ExecPlan rules.
- Observation: Debug build reaches Android Gradle Plugin configuration with JDK 17 but fails before compilation because the sandbox denies access to `C:\.android`.
  Evidence: `AccessDeniedException: C:\.android` from `assembleDebug`.

## Decision Log

- Decision: Use one dark scheme regardless of system light/dark mode.
  Rationale: The user explicitly requested a dark-purple UI and the brand should remain visually consistent.
  Date/Author: 2026-08-31 / Codex
- Decision: Keep screen-specific layouts unchanged and centralize colors in `Theme.kt`.
  Rationale: Existing screens already use Material theme tokens, so this gives broad coverage with low regression risk.
  Date/Author: 2026-08-31 / Codex

## Outcomes & Retrospective

The theme implementation and preview are complete. The build was attempted with Java 8 and then the bundled JDK 17; both were blocked by environment/tooling access before source compilation.

## Context and Orientation

`MainActivity.kt` wraps navigation in `UltimateTrackerTheme`. The screens under `app/src/main/java/com/example/ultimatetracker/ui/screens` use `MaterialTheme.colorScheme` and Material 3 components. Therefore the main implementation point is `ui/theme/Theme.kt`; no per-screen color replacement is required unless a hard-coded color becomes unreadable.

## Plan of Work

Define named dark-purple Material 3 colors in `Theme.kt`, pass them to `darkColorScheme`, and configure the Android status/navigation bars to match. Add a lightweight SVG preview showing the app bar, search field, filters, cards, and floating action button in the same palette. Build the debug application to catch resource or Kotlin errors.

## Concrete Steps

Run from `C:\Users\USER\Documents\Projects\UltimateTracker`:

    .\gradlew.bat assembleDebug

Expected result: `BUILD SUCCESSFUL` and a debug APK under `app/build/outputs/apk/debug/`.

## Validation and Acceptance

The build must complete successfully. In the running app, the top bar, cards, text fields, chips, dialogs, buttons, and floating action button must all render with dark-purple surfaces and lavender/purple accents, while primary text remains readable.

## Idempotence and Recovery

The changes are additive and safe to reapply. To revert only this redesign, restore `Theme.kt` and remove the preview SVG and this plan; no data or database files are changed.

## Artifacts and Notes

    app/src/main/java/com/example/ultimatetracker/ui/theme/Theme.kt
    design/utracker-dark-purple-preview.svg

## Interfaces and Dependencies

Use only the existing AndroidX Compose Material 3 dependency and `androidx.core` window-insets APIs already present in the project. Keep the public entry point `UltimateTrackerTheme(content: @Composable () -> Unit)` unchanged.

Plan update: created during the 2026-08-31 redesign because the referenced `.agent/PLANS.md` file was unavailable; `Plan.md` supplied the applicable rules.
