## ADDED Requirements

### Requirement: Complete applicable image interface

The app SHALL provide native Android screens for prompts, seed, resolution, batches, sampler, steps, guidance, advanced generation settings, server models, weighted LoRAs, controls, embeddings, upscalers, canvas, image inputs, masks, img2img, inpainting/outpainting, generation lifecycle and encrypted project history. Every workflow SHALL map to a verified server capability and acceptance evidence; unavailable operations SHALL be visibly unsupported and tracked rather than simulated locally.

#### Scenario: Parity review
- **WHEN** the Android image workflows are compared with the agreed actual iOS remote interface
- **THEN** each workflow has a field/control mapping, execution owner and passing acceptance evidence or an explicit unresolved capability gap; unresolved required gaps prevent a full-parity completion claim

#### Scenario: Unsupported learned preprocessing
- **WHEN** an input workflow requires an AI operation not exposed by the connected Mac
- **THEN** the app identifies the missing server capability and never runs a local model

### Requirement: Single execution destination

After setup the app SHALL present one Mac-backed workspace without cloud credits, subscriptions, hosted generation, local model download/install controls, local GPU settings or execution-mode selectors. Connection status and change-server access SHALL remain available.

#### Scenario: Workspace inspection
- **WHEN** the user opens all workspace and settings panels
- **THEN** all generation controls target the selected Mac and no alternate compute or account flow is offered

### Requirement: Private project interaction

The app SHALL support private result viewing, zooming, thumbnails, draft/configuration reuse, history and deletion in its own encrypted library. Its history SHALL be client-owned and SHALL NOT imply synchronization with the Mac app project database.

#### Scenario: Reuse while offline
- **WHEN** a previously connected user opens a saved image offline
- **THEN** the app can display it and reuse its configuration as a draft while Generate remains disabled
