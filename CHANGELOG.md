# Changelog

## alpha-1.8 — 2026-08-31

- Added a default profile icon when no avatar is selected or the selected image cannot be loaded.
- Corrected backup documentation: profile avatars are local profile data and are not exported with list backups.

## alpha-1.7 — 2026-08-31

- Added profile picture selection from the account settings using the Android image picker.
- Saved and displayed the selected avatar in the profile header.
- Added Room migration 6 to 7 for existing profiles.

## alpha-1.6 — 2026-08-31

- Redesigned the profile as a settings-style hub with name/email header and navigable Lists and Account Settings folders.
- Moved import/export controls fully into the Lists section.
- Moved username editing and account deletion into Account Settings.
- Removed the “sign out on all devices” action; the profile now has one sign-out button at the bottom.

## alpha-1.5 — 2026-08-31

- Added full cross-device export and merge import for all owned lists and titles through the Android system file picker.
- Added a versioned, validated UltimateTracker JSON backup format with new local ID assignment and transactional database writes.
- Embedded local cover images in backup files while preserving portable HTTP(S) covers.
- Excluded accounts, passwords, sessions, audit data and API tokens from exports.

## alpha-1.4 — 2026-08-31

- Guest users now see the sign-in screen first when opening the profile menu.
- Fixed the blank gray screen after signing out by showing the authentication screen.

## alpha-1.3 — 2026-08-31

- Removed the mandatory authentication screen from app startup.
- The app now opens directly to the main menu in guest mode; authentication remains available from the profile menu.

## alpha-1.2 — 2026-08-31

- Replaced the list deletion text action with a red trash icon.
- Added an explicit confirmation warning before deleting a list and its items.

## alpha-1.1 — 2026-08-31

- Added local guest, registration, sign-in, sign-out, session revocation, profile data, and account deletion foundations.
- Added PBKDF2-HMAC-SHA256 password hashing, random hashed session tokens, login throttling, and input validation.
- Added owned personal lists with switching, rename conflict detection, archiving, soft deletion, ordering, and audit events.
- Scoped every media read, write, and delete to the active owned list.
- Added Room migration 5 to 6 that preserves existing titles in the guest default list.
- Added English and Russian account/list UI and documented the server extension boundary.
