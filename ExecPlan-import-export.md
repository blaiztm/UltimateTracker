# Add portable list backup import and export

This ExecPlan is a living document maintained according to the complete repository rules in `Plan.md`. `AGENTS.md` refers to `.agent/PLANS.md`, but that path is absent.

## Purpose / Big Picture

Users can save all personal lists and their media entries to one portable UltimateTracker backup file, move that file to another Android device, and import it into the same application. Imported records receive new local identifiers and belong to the currently active user. The backup includes transferable metadata and embeds app-local cover images, but excludes passwords, sessions, account identity, and API secrets.

## Progress

- [x] (2026-08-31) Inspect account/list/media entities, repositories, application wiring, profile UI, and current dirty tree.
- [x] (2026-08-31) Define a versioned JSON backup with ownership remapping and defensive limits.
- [x] (2026-08-31) Add pure backup models/codec, snapshot queries, transactional importer, cover-image portability, and tests.
- [x] (2026-08-31) Add Android system file picker actions, import confirmation, and visible success/error status to the profile screen.
- [x] (2026-08-31) Document format and limitations, add `alpha-1.5` changelog/version, run 14 tests, and build the app successfully.

## Surprises & Discoveries

- Observation: local cover values can be Android `content://` URIs whose permissions and bytes do not exist on another device.
  Evidence: `MediaItem.coverUri` is a string populated by the system photo picker. A portable backup must embed those bytes rather than merely copy the URI.

## Decision Log

- Decision: Export all non-deleted lists, including archived lists, and all non-deleted items owned by those lists.
  Rationale: a backup should preserve the complete user collection rather than only the currently displayed list.
  Date/Author: 2026-08-31, Codex.
- Decision: Import uses merge semantics: it creates new lists and items under the current user and never trusts exported database IDs.
  Rationale: this avoids collisions, overwrites, and cross-user ownership violations while allowing repeated imports to be understandable and recoverable.
  Date/Author: 2026-08-31, Codex.
- Decision: Use a versioned JSON document with strict list/item/string/image/total-size limits and enum/range validation before database writes.
  Rationale: backup files are untrusted input and must not exhaust memory, inject invalid Room values, or partially mutate data.
  Date/Author: 2026-08-31, Codex.
- Decision: Embed local cover bytes as Base64 and retain HTTP(S) cover URLs as URLs.
  Rationale: local content URIs are device-specific, while remote URLs are already portable. Credentials and secrets are never part of the format.
  Date/Author: 2026-08-31, Codex.

## Outcomes & Retrospective

The portable backup feature is complete in `alpha-1.5`. Export writes every non-deleted owned list and item, including archived lists and embedded local covers, through the Android document picker. Import validates the entire schema and limits before merging new list/item IDs under the active user in one Room transaction. Existing records are never overwritten and credentials/secrets are excluded. Four new codec tests bring the suite to 14 passing tests; the debug APK builds successfully. A remaining production-hardening opportunity is an instrumented end-to-end test against real Room and a fake `ContentResolver`, because JVM tests currently focus on the pure format boundary.

## Context and Orientation

`AccountRepository` publishes the active user/list through `ActiveIdentityStore`. `UserListEntity` stores owned list metadata and `MediaEntity` stores an item with a foreign key to a list. `AccountScreen` is the profile/list-management screen. Android's Storage Access Framework is the system UI used by `CreateDocument` and `OpenDocument`; it lets users choose a destination/provider without broad storage permissions.

The new `BackupCodec` converts validated plain Kotlin backup objects to and from JSON. `BackupRepository` obtains database snapshots, reads/writes cover bytes, opens user-selected URIs through `ContentResolver`, and performs import writes inside one Room transaction. The ViewModel exposes progress and a summarized result to Compose.

## Plan of Work

Add DAO snapshot queries scoped by owner/list. Add backup data classes and a codec with format marker `ultimate-tracker-backup`, schema version 1, export metadata, list objects, and item objects. Each item contains fields currently represented by `MediaItem`; local covers contain MIME type and Base64 bytes. Decode validates every field and rejects unknown future schema versions with a useful error.

Add `BackupRepository` using `Dispatchers.IO`. Export obtains lists for the active user and media only through their list IDs, embeds bounded cover bytes, serializes, and writes atomically as far as the selected document provider supports. Import limits input bytes, fully parses and validates before mutation, materializes embedded covers in app-private storage, then creates lists/items with new IDs in a Room transaction. If database insertion fails, newly written covers are cleaned up.

Wire the repository in `UltimateTrackerApplication` and `AccountViewModel`. Add export/import buttons to `AccountScreen` using `ActivityResultContracts.CreateDocument` and `OpenDocument`, show confirmation before import, disable actions while running, and report counts or errors. Add localized English/Russian resources.

Add codec tests for round trip, bad marker/version, malformed enum/ranges, and limits. Document the format and security behavior. Raise app version from `alpha-1.4` to `alpha-1.5`, update `CHANGELOG.md`, run `testDebugUnitTest assembleDebug`, and keep unrelated dirty-tree work untouched.

## Concrete Steps

Run from `C:\Users\USER\Documents\Projects\UltimateTracker`:

    .\gradlew.bat testDebugUnitTest assembleDebug --stacktrace

Expected final output contains `BUILD SUCCESSFUL`; the APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Validation and Acceptance

Create multiple active/archived lists containing ratings, reviews, tags, watched progress, remote covers, and a locally chosen cover. Export to a `.utracker.json` document. On a clean install or another device, continue as guest or sign in, choose Import, confirm, and select that file. Observe newly created lists with the same metadata/items and a working embedded local cover. Existing destination lists remain unchanged. Importing malformed, oversized, wrong-format, or newer-version files displays an error and creates no database rows. Exporting creates no credential/session/token fields.

Unit tests and debug assembly must pass. Existing account, list, search, edit, and settings behavior must continue compiling.

## Idempotence and Recovery

Export is read-only. Import intentionally merges on every run, so repeated imports create repeated independent copies rather than overwrite data. Database writes are transactional. Embedded cover files created for a failed transaction are deleted; successful files remain app-private. Cancelling either system picker makes no changes.

## Artifacts and Notes

No password hash, salt, raw session token, account email, TMDB token, database ID, or audit history is exported. Final evidence:

    > Task :app:testDebugUnitTest
    > Task :app:assembleDebug
    BUILD SUCCESSFUL in 44s

JUnit reports 14 tests, zero failures, zero errors, and zero skipped. The generated APK is `app/build/outputs/apk/debug/app-debug.apk`.

## Interfaces and Dependencies

`BackupCodec.encode(payload): String` and `BackupCodec.decode(json): BackupPayload` are deterministic format boundaries. `BackupRepository.exportTo(uri, appVersion)` returns exported list/item counts. `BackupRepository.importFrom(uri)` returns imported list/item counts. Failures are represented as stable backup errors suitable for localized UI messages. The only added test dependency is the JVM implementation of `org.json`; Android already provides the same API at runtime.

Revision note (2026-08-31): Initial plan records portable-cover handling, merge/ownership semantics, untrusted-input limits, UI flow, validation, and versioning.

Revision note (2026-08-31): Completed implementation and recorded schema behavior, UI confirmation/results, portable cover handling, 14 passing tests, successful APK assembly, and the remaining instrumented-test opportunity.
