## Context

Source baseline: `drawthingsai/draw-things-community` at `c185aa2c24deca36b5d690ccea66b0a57c596a7a`, inspected 2026-10-26. Origin is Ryder's fork, upstream remains the official repository. The upstream README describes a private app monorepository and a public engine/protocol subset. No complete iOS UI source is available here.

Verified source contracts:

| Source | Evidence and consequence |
| --- | --- |
| `Libraries/GRPC/Models/Sources/imageService/imageService.proto` | `Echo`, `FilesExist`, streaming `GenerateImage`; optional shared secret; metadata overrides; chunked responses. No persistent job lookup or server UI mirroring RPC. |
| `Libraries/DataModels/Sources/config.fbs` | Generation configuration travels as FlatBuffers bytes inside protobuf, not plain JSON. |
| `Libraries/RemoteImageGenerator/Sources/RemoteImageGenerator.swift` | Inputs are content-addressed SHA-256 tensor blobs using zip/fpzip; generated images and previews are decoded as tensor data. Do not assume PNG/JPEG from field names. Cancellation cancels the gRPC call. |
| `Libraries/MediaGenerationKit/Sources/MediaGenerationKit.docc/Articles/RemoteGeneration.md` | Self-hosted remote endpoint; TLS is the default; optional shared secret. |

The reference Swift client imports inference-related libraries. It must not be linked wholesale into Android. Protocol serialization, lossless tensor decoding, color conversion, bitmap rendering, and canvas drawing are non-AI client work. Tokenization, embeddings, segmentation, depth/pose extraction, diffusion, latent/TAE decoding, learned upscaling, restoration, and training must execute only on the Mac. If a preview requires a learned decoder, omit that preview or request a server-rendered alternative; never download a preview model.

## Goals / Non-Goals

**Goals:** Native Android control parity for applicable image workflows, a mandatory Mac connection, zero Android AI execution, and an encrypted private library. Prove compatibility against the running official Mac app, not just a standalone CLI or mock.

**Non-Goals:** Tailscale login/management, installing a server, local model downloads, cloud accounts/billing, hosting an inference server in the Android package, pixel streaming of the Mac desktop, or training on Android. Video/audio and remote training control require a separately specified extension; image control parity must be explicitly measured rather than asserted.

## Decisions

### Native Android boundary

Use Kotlin and Jetpack Compose for the Android interface; plan modules `app`, `connection`, `protocol`, `generation`, `editor`, and `vault` under `android/`. Android builds must have no dependency on upstream inference targets or model assets. A WebView/desktop-streaming wrapper would not provide the required native private storage and protocol control. The fork preserves upstream files as reference; only the Android dependency graph ships in the APK. No Android executable is implemented in this setup change.

### Connection before workspace

First launch shows one action: Connect to Mac. Accept hostname/IP, port, TLS trust, and an optional shared secret. Support LAN names/IPs and externally routed hostnames/IPs identically. Do not embed Tailscale or special-case its address range. Authenticate and perform Echo plus usable model/config checks before enabling generation; an open socket alone is insufficient. TLS defaults on; support a deliberately selected per-endpoint plaintext mode for compatible private servers with a clear transport disclosure, never automatic downgrade. Verify certificates or an explicitly confirmed pin. Store credentials encrypted, never in logs.

States: unconfigured -> connecting -> authenticating/validating -> ready; errors return to a recoverable connection screen. Once a profile has been verified, offline browsing of the encrypted library is allowed, while generation remains disabled. Endpoint changes invalidate readiness and require revalidation.

### Direct gRPC and wire compatibility first

Generate protobuf and FlatBuffers bindings from pinned upstream schemas. Establish a minimal end-to-end text-to-image round trip before building the full editor. Confirm the tensor format, fpzip support, dimensions, dtype, channel order, scaling and size bounds with recorded non-sensitive fixtures and the official Mac app. Implement only non-AI codecs on Android. If public protocol is insufficient, record the unsupported workflow and investigate an upstream-compatible server path; never quietly add local AI or substitute a different server product.

Use server metadata/files for available models, LoRAs, controls, embeddings, and upscalers; do not offer downloading their weights onto Android. Validate schema defaults and units against the Swift client. Unknown required capabilities block submission with an actionable message. Echo is not a general feature-negotiation API; maintained tested compatibility profiles and probes must cover gaps.

Consume streaming progress and chunked results with bounded buffers. Persist a result only after complete validated decoding and authenticated encryption. Mark a job complete only after its output is committed to the vault. Cancel the stream on Stop and accurately report uncertainty if server stop cannot be confirmed. On a dropped connection, retain the draft and mark the execution outcome unknown; no automatic resubmission or invented resume/job-query support.

### Full interface coverage is a measured contract

Build a parity inventory from the actual iOS app and Mac remote behavior, including settings, advanced panels, canvas, and history interactions. The public schema is evidence for wire capabilities, not proof that every UI operation is remotely supported. Map every image workflow to a screen, request fields, Mac-owned work, and an acceptance test. Unsupported server capabilities must be visible and tracked in Beads. Completing a prompt-only screen does not satisfy this change.

| Interface area | Planned Android controls | Execution boundary / verification |
| --- | --- | --- |
| Connection | One Mac profile, trust/auth, reconnect/change server | Verify official app handshake and available assets |
| Prompt and configuration | Positive/negative prompts, seed, size, batch, steps, sampler, guidance and advanced settings | FlatBuffers round-trip and actual Mac output settings |
| Model composition | Server model, LoRAs/weights, controls, embeddings, upscalers | Server inventory and validated metadata; no weight installation |
| Canvas and inputs | Pan/zoom, imported input, brush/mask, img2img, inpaint/outpaint, reference/control images | Android drawing and codecs only; all learned transforms on Mac |
| Run lifecycle | Generate, progress, optional non-AI preview, Stop, errors | Streaming gRPC; no silent replay after interruption |
| Private projects | Output viewer, thumbnails, prompt/config reuse, history, deletion | App-private encrypted client history, not Mac history synchronization |

### Encrypted app-only persistence

Use Android Keystore to hold a non-exportable AES-256 wrapping key. Generate a fresh random AES-256 data key per record, wrap it using Keystore AES-GCM, and encrypt objects with software AES-GCM, using separate fresh nonces and authenticated version/object identity, and opaque random filenames. Originals, thumbnails, input copies, masks, drafts, prompts, seeds, configuration, credentials, and indexes must not appear in plaintext files or databases. Store ciphertext in internal no-backup storage. Disable cloud backup and device transfer through manifest and extraction rules; keys and data stay device-local. No broad media/storage permission, MediaStore insert, media scanner request, public external directory, exported file provider, share action, or automatic export in this version.

Decode/display in memory; disable disk caches in image libraries. Encrypt temporary/spooled data as well. Bound image dimensions and stream sizes; process within a declared memory budget. Use atomic ciphertext writes and commit encrypted indexes only after durable object persistence. On disk-full/crash/authentication failure, never report a successful save or fall back to plaintext. Test orphan cleanup. Lost/invalidated keys cause an explicit unreadable-vault/reset flow, not silent key regeneration or deletion. Uninstall loses the vault. Deletion removes object references and ciphertext; do not promise physical secure erasure on flash. Use secure-window protection for sensitive screens and neutral notifications/recents to avoid incidental image disclosure. This does not prevent the Mac server from retaining its own outputs.

## Risks / Trade-offs

- Full Apple UI is private -> build native Android screens and audit parity using the actual app; no claim of copied UI source.
- Tensor codecs may be difficult on Android -> make wire compatibility the first blocking proof; do not mistake tensor bytes for encoded image files.
- Some iOS features might do learned preprocessing locally -> move those operations to the Mac when supported, otherwise expose the limitation and track server support.
- Public protocol lacks durable job identity -> report unknown interrupted jobs and require explicit retry.
- Self-hosted TLS configurations vary -> validate trust and test explicit plaintext only where deliberately configured; no automatic downgrade.
- Device-local keys preclude portable backups -> communicate uninstall/key-loss behavior and test restore failure safely.
- App-only encrypted storage protects this Android copy -> it does not control Mac retention or a compromised/unlocked Android process.

## Migration Plan

No existing Android data or users exist. Land this specification and Beads plan first. Implement and prove protocol compatibility, then shell/connection, vault, generation, and parity interface. Rollout requires an installable APK and an actual Mac-generated image visible after reopening the encrypted vault. Rollback removes the Android installation without modifying the server. Keep this OpenSpec change open until implementation and acceptance evidence are complete.

## Open Questions

- Which official Mac/iOS app versions and remotely available UI features define the first tested compatibility profile?
- Can every image/control input and output format be encoded/decoded by a non-AI Android codec at acceptable memory cost?
- Which advanced canvas and learned-preprocessing operations lack remote protocol support?
- Which host TLS/trust setup will be used for the initial device-to-Mac acceptance run?

## Implemented development slice (2026-10-26)

The Kotlin/Compose app and pure JVM protocol module are isolated under `android/`.
A read-only LLNL fpzip native codec decodes compressed tensors; it contains no learned
model or inference engine. Raw float16/float32 and fpzip responses have reached a real
Android tablet from official Draw Things Mac 1.20260716.0 over pinned TLS.

The DTR2 vault uses envelope encryption because direct image-sized hardware Keystore
AES-GCM operations failed authentication on the test API 28 tablet. The Keystore
protects only a short data key; software AES-GCM protects each record. Device tests
cover payloads around the 64 KiB boundary through 1 MiB, tampering, nonce freshness,
and interrupted atomic replacement. This is functional evidence, not a security audit.

Full iOS control parity, advanced canvas/reference inputs, key-loss reset UI, and broader
failure acceptance remain tracked in Beads. See `android/VERIFICATION.md` for measured
coverage. The OpenSpec change remains open.
