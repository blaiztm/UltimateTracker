# UltimateTracker portable backup format

Version `alpha-1.5` exports a UTF-8 JSON document marked with `format: "ultimate-tracker-backup"` and `schemaVersion: 1`. The root contains export time, app version, and an ordered `lists` array. Each list stores its title, position, archived state, and items. Items store title, media type, length, genre/keyword arrays, watch category, review, rating, watched episode count, timestamps, and cover data.

HTTP and HTTPS cover locations remain URLs. Device-local covers are embedded as MIME type plus Base64 bytes and are restored into app-private storage during import. One cover is limited to 5 MiB and the full file to 50 MiB. A backup is limited to 500 lists and 50,000 items; names, tags, ratings, progress, timestamps, enum values, and image metadata are validated before any database mutation.

Database IDs and owner IDs are never trusted or exported. Import uses merge behavior: it creates new lists owned by the currently active user and new media rows mapped to those new list IDs. Existing destination lists are not replaced. Re-importing the same file intentionally creates another independent copy.

Account email, profile avatar, password hash/salt, sessions, raw session tokens, TMDB token, audit events, and deleted rows are excluded. The file is therefore a collection backup, not an account/profile credential backup. Users should still protect it because reviews and viewing history may be private.

The Android Storage Access Framework handles both operations through `CreateDocument` and `OpenDocument`, so local storage, cloud drives, and other document providers can be used without broad filesystem permission. Import parses and validates the entire file before a Room transaction. If database insertion fails, the transaction rolls back and any cover files created during that attempt are removed.
