# Add local user accounts and owned lists

This ExecPlan is a living document. The sections `Progress`, `Surprises & Discoveries`, `Decision Log`, and `Outcomes & Retrospective` must be kept up to date as work proceeds. The repository contains the governing instructions in `Plan.md`; `AGENTS.md` refers to `.agent/PLANS.md`, but that path is absent, so this plan follows the complete rules stored in `Plan.md`.

## Purpose / Big Picture

UltimateTracker currently treats every saved title as one device-wide collection. After this work, a person can continue as a guest or register and sign in with an email and password, maintain a profile, create and switch between personal lists, and see only titles from the selected list. Existing installations keep their titles: database migration places them in a default list owned by a local guest. The implementation is deliberately local-first because this repository contains no server; password reset by email, email verification, and remote OAuth are represented as future integration boundaries rather than insecure simulations.

## Progress

- [x] (2026-08-31) Read repository instructions, inspect the Android/Room/MVVM architecture, inspect the dirty working tree, and identify version `alpha-1.0` with database version 5.
- [x] (2026-08-31) Decide the local-first ownership, authentication, session, and migration model.
- [x] (2026-08-31) Add Room entities, data access objects, migration 5 to 6, cryptographic helpers, repositories, and tests.
- [x] (2026-08-31) Connect session/list state to the existing media repository and ViewModel without regressing current collection behavior.
- [x] (2026-08-31) Add authentication, profile, session, and list-management UI with English and Russian resources.
- [x] (2026-08-31) Add architecture and API-contract documentation, changelog entry, and version `alpha-1.1`.
- [x] (2026-08-31) Run 10 unit tests and assemble the debug APK; final result is `BUILD SUCCESSFUL`.

## Surprises & Discoveries

- Observation: `AGENTS.md` names `.agent/PLANS.md`, but the repository has no `.agent` directory and stores the full ExecPlan specification in `Plan.md`.
  Evidence: `Get-Content .agent/PLANS.md` reports a missing path while `Plan.md` contains the complete required skeleton.
- Observation: the working tree already contains extensive uncommitted UI, TMDB, rating, and database migrations through version 5.
  Evidence: `git status --short` lists modified tracked files and new plan, search, drawable, test, and design files. This plan treats all of them as user-owned and layers changes on top.
- Observation: sandboxed Gradle could not read the installed Android SDK and initially tried to use the protected `C:\.android` preferences path.
  Evidence: the first runs reported `AccessDeniedException`; setting a workspace Android preferences directory and granting the build access to the installed SDK produced repeatable successful builds.

## Decision Log

- Decision: Implement a local-first account system rather than claiming remote identity features.
  Rationale: there is no backend module or configured identity provider. Email delivery, cross-device identity, and OAuth token exchange cannot be secure inside an APK alone.
  Date/Author: 2026-08-31, Codex.
- Decision: Store password credentials using PBKDF2-HMAC-SHA256 with a random per-account salt, and store session tokens only as SHA-256 hashes in Room while retaining the active raw token in app-private preferences.
  Rationale: this avoids plaintext credentials and makes a copied database insufficient to reuse a session token.
  Date/Author: 2026-08-31, Codex.
- Decision: Migrate all version-5 media rows into list ID 1, owned by guest user ID 1, and make every media query require the active list ID.
  Rationale: existing data survives and ownership checks happen below the UI, preventing an ID-only lookup from exposing another list.
  Date/Author: 2026-08-31, Codex.
- Decision: Reserve collaboration for a future membership table and document its contract, but omit it from the MVP schema.
  Rationale: current requirements ask for readiness without premature complexity; owner IDs and repository authorization provide a clean later migration point.
  Date/Author: 2026-08-31, Codex.

## Outcomes & Retrospective

The local-first account and personal-list MVP is complete at `alpha-1.1`. Users can register, sign in, continue as guest, edit their profile, sign out, revoke all sessions, delete a registered account with confirmation, and create, rename, reorder, archive, restore, delete, and switch lists. Every media repository operation is constrained by the active owned list. Migration 5 to 6 preserves old rows in the guest default list, and guest registration transfers those lists transactionally. Ten unit tests pass and the debug APK builds. Email delivery, email verification, OAuth, remote synchronization, Android Keystore wrapping, and instrumented migration/security tests remain future server/production-hardening work and are clearly documented rather than presented as implemented.

## Context and Orientation

The project is a single Android application module in `app`. `app/src/main/java/com/example/ultimatetracker/data/local/AppDatabase.kt` declares the Room database, which is a typed wrapper around local SQLite. `MediaEntity.kt` stores titles and `MediaDao.kt` reads them. `MediaRepository.kt` maps database rows to `MediaItem`, while `MediaViewModel.kt` exposes observable state to Jetpack Compose screens. `UltimateTrackerApplication.kt` builds the database and repositories. `AppNavigation.kt` connects screens.

An account is a sign-in method attached to one user. A session is a revocable proof of a successful sign-in. A user list is an owned collection. A list item is the existing media row augmented with a required `listId`. Soft deletion means setting a deletion timestamp instead of immediately removing a row; lists and accounts use it where recovery or auditing matters.

## Plan of Work

First add account-domain Room entities and DAOs under `data/local/account`, then update `AppDatabase` to version 6. Migration 5 to 6 creates the guest user, profile, local account/session/list/audit tables, reconstructs `media_items` with a foreign key and list ownership, and copies every old row into the guest default list. Add indexes for normalized email, account provider identity, session token hash and expiry, owner/list ordering, and media ownership.

Next add pure Kotlin security helpers under `security` for email normalization, password validation, PBKDF2 hashing, constant-time verification, random tokens, and token hashing. Add an `AccountRepository` that validates every account/list/session operation, rate-limits failed login attempts, revokes sessions, owns active-user and active-list state, and emits audit rows. Update `MediaDao` and `MediaRepository` so all reads and writes include the selected list and reject records outside it.

Then add an account ViewModel and Compose screens for welcome, registration, login, account/profile settings, active sessions, and list creation/rename/archive/delete/switch. Existing home, detail, and edit screens continue to work but receive data only from the active list. Add localized strings and expose the current list/account in navigation.

Finally document the entity relationship diagram, local service contracts, error model, migration, authorization matrix, edge cases, remote API extension, and testing strategy in `docs/account-architecture.md`. Record `alpha-1.1` in `CHANGELOG.md`, update `versionCode` and `versionName`, run tests and the debug build, and capture results here.

## Concrete Steps

Run commands from `C:\Users\USER\Documents\Projects\UltimateTracker`.

Inspect relevant changes without modifying user work:

    git status --short
    git diff -- app/src/main/java/com/example/ultimatetracker/data/local/AppDatabase.kt

After implementation, execute:

    .\gradlew.bat testDebugUnitTest assembleDebug --stacktrace

The expected final output contains `BUILD SUCCESSFUL`. The APK should be present at `app/build/outputs/apk/debug/app-debug.apk`.

## Validation and Acceptance

On a migrated install, choose guest mode and observe all pre-existing titles in the default list. Create a second list, switch to it, and observe an empty collection; add a title, switch back, and verify that each list retains only its own title. Register with a normalized email and valid password, sign out, sign in with the same credentials, and observe the registered user's profile and lists. Attempt a wrong password repeatedly and observe temporary lockout. Revoke another session and verify it no longer restores. Archive or delete a list and verify another active list is selected. Database and repository tests must prove a title cannot be read, updated, or deleted when only its numeric ID is known but the active list differs.

The debug build and all unit tests must pass. Existing search, ratings, watched-episode progress, custom media types, localization, and TMDB settings must still compile and remain reachable.

## Idempotence and Recovery

Migration 5 to 6 is transactional under Room and uses fixed guest IDs only during the one-time version transition. New repository initialization uses insert-ignore behavior and is safe to repeat. If migration fails, SQLite keeps the old version-5 database transaction intact. No destructive fallback is enabled. Existing uncommitted files must not be reset or replaced wholesale.

## Artifacts and Notes

Final evidence:

    > Task :app:testDebugUnitTest
    > Task :app:assembleDebug
    BUILD SUCCESSFUL in 17s

The JUnit XML reports 10 tests, zero failures, zero errors, and zero skipped tests. The APK is `app/build/outputs/apk/debug/app-debug.apk`. The database moved from version 5 to 6 and the app moved from `alpha-1.0` (`versionCode` 1) to `alpha-1.1` (`versionCode` 2). No secrets, raw passwords, raw session tokens, or password hashes appear in logs or documentation.

## Interfaces and Dependencies

The implementation uses existing Kotlin, coroutines, Room, Compose, and Java Cryptography Architecture APIs; no network or authentication dependency is required. `AccountRepository` exposes initialization, guest entry, registration, login, logout, logout-all, profile update, account deletion, list CRUD, list selection, and session revocation. `MediaRepository` continues to expose observable media and save/delete methods, but internally binds every operation to the active authorized list. `AccountViewModel` translates repository results into UI state and one-time errors.

Future server adapters must implement the documented authentication service boundary and replace local credential issuance while preserving the same user/list ownership model. Email verification, reset delivery, Google OAuth, and cross-device synchronization remain explicitly unsupported until such an adapter exists.

Revision note (2026-08-31): Initial self-contained plan created after repository inspection; it records the absent `.agent/PLANS.md`, dirty-tree constraints, local-only security boundary, migration strategy, and validation criteria.

Revision note (2026-08-31): Updated after implementation to record completed milestones, sandbox/SDK discovery, final scope and limitations, version, migration, ten passing tests, and successful APK assembly.
