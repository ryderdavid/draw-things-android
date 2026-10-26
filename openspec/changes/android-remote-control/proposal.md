## Why

Android needs a private, full-featured control interface for an existing Draw Things Mac app. All AI work must stay on that Mac; Android must require a working server connection and keep received images in an encrypted, app-only library.

## What Changes

- Add a native Android client with mandatory first-run Mac connection and no local inference backend or fallback.
- Bring the complete applicable image-generation interface to Android: prompts, server model selection, generation settings, LoRAs, controls, input images, masks, canvas operations, progress, cancellation, and private project/history browsing.
- Remove local model installation, local compute settings, cloud subscriptions/accounts, hosted compute, and execution-mode selectors from the Android product.
- Speak the existing Draw Things remote protocol directly to the real Mac app. Tailscale is an external network route, not an Android app integration.
- Encrypt outputs, thumbnails, drafts, inputs, and sensitive metadata within app-private storage. Never automatically publish to Android Gallery/MediaStore.
- Establish protocol and feature-parity verification before claiming compatibility or full interface coverage.

## Capabilities

### New Capabilities

- `mac-connection`: Mandatory authenticated connection, trust, compatibility checks, and reconnection.
- `remote-generation`: Mac-only execution, protocol serialization, streaming progress, cancellation, and recovery.
- `android-control-interface`: Native interface covering supported Draw Things image workflows without local/cloud compute choices.
- `private-image-vault`: Encrypted app-private media and history with no public media writes.

### Modified Capabilities

None. This is a new Android product in a fork; upstream Apple/server behavior is unchanged.

## Impact

Planned Android modules live under `android/`, isolated from upstream Swift/Bazel inference targets. Protocol sources are `Libraries/GRPC/Models/Sources/imageService/imageService.proto` and `Libraries/DataModels/Sources/config.fbs`. The public repository does not contain the complete iOS/macOS interface; this is a new native client, not a mechanical Swift UI port. Retain upstream license and attribution. No server installation, configuration change, or image generation is performed by this setup change.
