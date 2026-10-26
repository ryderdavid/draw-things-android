# Development verification — 2026-10-26

This records observed behavior, not completion of the full OpenSpec change.

## Tested environment

Official Draw Things Mac **1.20260716.0**, running on the local Mac with its API Server
enabled at port 7859, TLS on, Bridge Mode off, Model Browsing on. Response Compression
was tested off and on and left on. No shared secret was configured during these tests.
Android client used an independently read SHA-256 leaf certificate pin over the LAN.
Test device: physical Amazon KFONWI (Fire HD 8), Android API 28. APK version 0.1.0.
The public protocol source baseline is `c185aa2c24deca36b5d690ccea66b0a57c596a7a`.

## Observations

| Check | Evidence |
| --- | --- |
| Official server inventory | Android Echo returned four installed models over pinned TLS. |
| Raw result | Real 512 × 512 generation; 197434-byte PNG encrypted and reopened byte-for-byte. |
| Compressed result | Same test with Response Compression enabled; fpzip decoded; 197661-byte PNG encrypted and reopened byte-for-byte. |
| Actual app workflow | Opened saved workspace, selected Connect Mac, successfully connected, pressed Generate on Mac; UI reported “Saved 1 image in your private library” and library count increased from 3 to 4. |
| Gallery isolation | MediaStore external images remained at five entries before and after all three real generation runs. |
| Protocol regressions | Seven JVM tests pass: schema/defaults/unsigned seed, RGB/half tensor values, bounds, latent and nonfinite rejection, mask labels, request and endpoint validation. |
| Vault regressions | Four physical-device tests pass: ciphertext/tampering/randomness, payloads from 65535 through 1048576 bytes, interrupted atomic replacement, no backup/media permissions. |
| Native decoder regression | Physical-device malformed, truncated, and invalid-bound fpzip inputs reject without a native crash; five total device tests pass. |
| Build | Debug app and instrumentation APKs build; native reader compiled for arm64-v8a, armeabi-v7a, x86_64. Only the physical test device ABI was executed. |
| OpenSpec | `openspec validate android-remote-control --strict --no-interactive` passes. |

The live generation prompt was a small red ceramic cube on a pale gray tabletop,
using the installed `moodypromix_zitv13_f16.ckpt`, 512 × 512, four steps, seed 42,
guidance 1, shift 3. Tests deliberately start real Mac work only with explicit endpoint
and model arguments. A failed pre-fix encryption test left one unreadable development
record; the library reports corruption without silently deleting it.

Direct large-payload Keystore GCM failed on this device. DTR2 envelope encryption
resolved the failure: the non-exportable master key wraps a random per-record data key;
software GCM encrypts the payload. Success was verified on both fixture data and actual
Mac output. No screenshots are captured because sensitive windows use FLAG_SECURE.

## Remaining acceptance boundaries

Beads epic `dta-7ou` remains authoritative and open. Full iOS image-interface parity is
not audited. The generic advanced settings panel is provisional; server-side controls,
reference inputs, outpaint, embedding UX, and model-specific compatibility need further
work. Imported-image/mask workflows are implemented but not yet live-tested against
the Mac. Connection authentication failures, TLS rotation, interrupted jobs, key-loss
reset UI, disk-full behavior, process-death drafts, large batches, and release signing
need acceptance coverage. The current debug build is not a production security release.
