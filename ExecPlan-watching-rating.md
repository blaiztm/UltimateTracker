# Add watching episode progress and per-score colors

This ExecPlan follows `Plan.md`. It is a living record of the implementation.

## Purpose / Big Picture

Users can record how many episodes of a series or anime they have watched while its status is “Watching”, see progress such as `40/48 episodes` on collection cards, and distinguish ratings from 1 through 10 by a smooth color scale with a special diamond-turquoise 10.

## Progress

- [x] (2026-08-31) Add persisted watched-episode field and migration.
- [x] (2026-08-31) Add conditional form input and validation.
- [x] (2026-08-31) Render progress and rating colors in cards.
- [x] (2026-08-31) Run unit tests and debug build.

## Surprises & Discoveries

- The Room database is currently version 4; the next migration must preserve existing rows.

## Decision Log

- Decision: Store watched episode count as a non-negative integer defaulting to zero and show/edit it only for non-movie items in `WATCHING` status. Rationale: movies have duration rather than episodes, and zero gives a useful starting progress without nullable UI state.

## Outcomes & Retrospective

The feature is implemented and the debug build plus unit tests pass. Progress is visible on home cards for watching series/anime, and existing databases migrate additively.

## Context and Orientation

`MediaItem` and `MediaEntity` define the saved media record. `MediaViewModel.MediaFormState` feeds `EditScreen`; `HomeScreen.MediaCard` renders collection cards; `DetailScreen` renders full details. Room schema changes are registered in `UltimateTrackerApplication`.

## Plan of Work

Add `watchedEpisodes` to model/entity/form, update mapping and save/edit paths, and migrate Room 4 to 5. Add a conditional numeric field with validation against total episodes. Show progress only while watching and color each rating value using interpolated red-to-green colors, with rating 10 explicitly diamond turquoise.

## Concrete Steps

Run from `C:\Users\USER\Documents\Projects\UltimateTracker`:

    .\\gradlew.bat testDebugUnitTest assembleDebug

Expected result is `BUILD SUCCESSFUL`.

## Validation and Acceptance

Create or edit a series, choose “Watching”, enter total episodes 48 and watched episodes 40, save, and observe `40/48 episodes` on its home card. Change status to Planned and confirm the progress input/card text disappears. Verify ratings 1–9 vary across a red/yellow/green gradient and 10 is diamond turquoise.

## Idempotence and Recovery

The migration is additive and assigns zero to existing rows. Re-running the build is safe. If UI validation fails, existing saved data remains untouched until Save succeeds.

## Artifacts and Notes

The migration adds a non-null integer column with default zero; no destructive data operation is required.

## Interfaces and Dependencies

Use existing Kotlin, Jetpack Compose Material 3, and Room APIs. Keep `MediaItem.watchedEpisodes` and `MediaEntity.watchedEpisodes` as `Int`.
