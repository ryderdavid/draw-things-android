## ADDED Requirements

### Requirement: Mac-only AI execution

The Android product MUST execute all learned AI operations exclusively on the selected Mac. The APK MUST contain no inference runtime, model weights, local tokenizer/embedding pipeline, learned preview decoder, training engine, or local fallback.

#### Scenario: Server unavailable
- **WHEN** the user attempts generation without a ready connection
- **THEN** the app blocks submission and performs no local AI operation or alternate backend request

#### Scenario: APK audit
- **WHEN** the release dependency graph and package contents are inspected
- **THEN** they contain only client UI, transport, storage, and non-AI codecs and no model assets or AI engines

### Requirement: Wire-compatible generation

The app SHALL encode the pinned upstream protobuf/FlatBuffers contract and content-addressed inputs and SHALL decode complete tensor results using non-AI codecs. It SHALL validate dimensions, types, bounds, chunks and metadata and reject unsupported formats explicitly.

#### Scenario: Real Mac round trip
- **WHEN** a valid text-to-image request is submitted to the running official Mac app
- **THEN** the Mac performs inference and Android displays the correctly decoded result

#### Scenario: Invalid or oversized result
- **WHEN** a response has invalid dimensions, corrupt bytes or exceeds the configured resource limits
- **THEN** the app rejects it without exposing a partial image as a successful output

### Requirement: Progress cancellation and interruptions

The app SHALL consume progress and bounded result streams, support Stop by cancelling the RPC, and report uncertain server outcomes honestly. It SHALL NOT resubmit interrupted requests automatically or claim durable server resume support absent protocol evidence.

#### Scenario: User stops generation
- **WHEN** the user taps Stop during a live request
- **THEN** the client cancels the stream and distinguishes client cancellation from confirmed server termination

#### Scenario: Network drops during generation
- **WHEN** the request stream fails before a complete result arrives
- **THEN** the draft remains, the outcome is marked unknown and no duplicate request is issued

#### Scenario: Completed transport without saved result
- **WHEN** a stream ends but vault persistence fails
- **THEN** the app reports a save failure and does not mark the job successfully saved
