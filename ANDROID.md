# Draw Things Android

A native Android control plane for the real Draw Things Mac app. All AI runs on the Mac. Android connects first, sends generation/editing controls, and stores received images in an encrypted app-only library without publishing to Gallery.

This repository is a fork of [Draw Things Community](https://github.com/drawthingsai/draw-things-community). The upstream public source contains the engine, data models and remote protocol; the complete Apple app interface lives in a private repository. The Android interface will be implemented natively. This is an independent project, not an official Draw Things Android release. Upstream licensing and attribution are retained.

## Current state

Project setup and specification only. There is no Android APK or implemented Android client yet. Existing Swift engine/CLI targets belong to upstream and are not Android application code.

- Open change: [android-remote-control](openspec/changes/android-remote-control/proposal.md)
- [Design and verified source constraints](openspec/changes/android-remote-control/design.md)
- [Beads implementation references](openspec/changes/android-remote-control/tasks.md)
- Local checkout: `~/Code/HomeAgentic/draw-things-android`
- Feature branch: `spec/android-remote-control`
- Origin: `https://github.com/ryderdavid/draw-things-android.git`
- Upstream: `https://github.com/drawthingsai/draw-things-community.git`

## Working on the project

Use `bd prime`, `bd ready`, and `bd show dta-7ou` from this checkout. The repository-local Dolt database is authoritative; do not use the personal master board here. OpenSpec defines the product contract and Beads owns task status. Run `openspec validate android-remote-control --strict --no-interactive` to validate the change.

First prove the wire codec against the running official Mac app. Its gRPC image fields carry tensors, not ordinary PNG/JPEG files; configuration is FlatBuffers. Android must not link upstream inference targets or ship model weights, including learned preview decoders. LAN and device-managed Tailscale routes use the same endpoint configuration. Server installation and VPN management are outside this app.

Acceptance requires an actual Mac-generated image reopened from the encrypted Android vault, plus audited image-interface parity. A Connected badge or mocked output is insufficient. App-only storage describes the Android copy; Mac retention is controlled by the Mac app.
