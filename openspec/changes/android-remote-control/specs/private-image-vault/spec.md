## ADDED Requirements

### Requirement: Authenticated encrypted private persistence

The app MUST encrypt all persisted originals, previews, thumbnails, inputs, masks, drafts, prompts, configurations, credentials and indexes using authenticated encryption with unique nonces and a non-exportable Android Keystore key. Ciphertext SHALL reside only in app-private no-backup storage with opaque filenames; disk caches and plaintext intermediate files SHALL be prohibited.

#### Scenario: Inspect private storage
- **WHEN** an image and its history are saved and the app is restarted
- **THEN** the image reopens correctly while disk inspection reveals no plaintext media, prompts, metadata or secrets

#### Scenario: Tampered ciphertext
- **WHEN** an encrypted object is changed outside the app
- **THEN** authenticated decryption fails and the app shows a recoverable error without displaying corrupted content

### Requirement: No Gallery publication or external persistence

The app SHALL NOT write generated content to MediaStore, the Android Gallery, shared/external directories, media scanners, exported providers, cloud backups or device transfers. This version SHALL offer no export/share path or broad media permission. Imported files SHALL be copied into the encrypted vault without altering the original.

#### Scenario: Generation succeeds
- **WHEN** the Mac returns an image and the encrypted save completes
- **THEN** the image appears only in the app library and MediaStore has no new item

#### Scenario: Backup and transfer
- **WHEN** Android backup or device transfer runs
- **THEN** vault data, credentials and encryption keys are excluded through manifest and extraction rules

### Requirement: Atomic saves and key lifecycle

The app SHALL commit complete authenticated ciphertext and its encrypted index atomically, recover incomplete writes, and report storage failure without plaintext fallback. Key invalidation SHALL preserve unreadable data until the user explicitly chooses reset. Uninstall SHALL remove access to the vault.

#### Scenario: Disk full or process termination
- **WHEN** storage fills or the process dies during a save
- **THEN** relaunch finds either a complete decryptable image or a recoverable incomplete write, never a falsely successful plaintext output

#### Scenario: Key invalidation
- **WHEN** the vault key becomes unavailable or invalid
- **THEN** the app explains that existing images are unreadable and does not silently replace the key or delete the vault

### Requirement: Limit incidental disclosure

The app SHALL protect sensitive windows from screenshots/recents capture and SHALL keep notifications and logs free of images, prompts and secrets. Image decoding SHALL use memory-only caches.

#### Scenario: Backgrounded viewer
- **WHEN** the user backgrounds the image viewer or generation emits a notification
- **THEN** recents and notifications do not expose image content or sensitive prompt text
