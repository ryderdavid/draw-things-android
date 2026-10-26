## ADDED Requirements

### Requirement: Mandatory first connection

The app SHALL require a successful connection to a user-selected Draw Things Mac app before first access to the generation workspace. It SHALL offer no skip, demo generation, local backend, or hosted service signup.

#### Scenario: Fresh install
- **WHEN** the user launches a fresh installation
- **THEN** the app shows Connect to Mac and generation remains unavailable until the endpoint is authenticated and validated

### Requirement: Endpoint validation and trust

The app SHALL accept a hostname or IP and port independent of network routing. TLS SHALL default on and validate trust; any plaintext mode SHALL require explicit per-endpoint selection and SHALL never be an automatic fallback. Optional server shared secrets SHALL be protected in storage and redacted from logs.

#### Scenario: Untrusted endpoint
- **WHEN** certificate validation fails
- **THEN** the app blocks readiness until trust is explicitly resolved and does not downgrade transport

#### Scenario: Authentication required
- **WHEN** Echo reports a missing shared secret or authentication fails
- **THEN** the app requests corrected credentials without opening generation controls

#### Scenario: External network route
- **WHEN** a configured hostname is reachable through a device-managed VPN
- **THEN** the app uses the same connection flow without embedding VPN management

### Requirement: Readiness and reconnection

The app SHALL validate Echo, usable server assets and the tested protocol/configuration profile before readiness. Changing endpoint or losing connectivity SHALL disable generation. After an earlier successful setup, encrypted library browsing SHALL remain available offline.

#### Scenario: Socket without usable service
- **WHEN** TCP connects but the protocol or server inventory is incompatible
- **THEN** generation stays disabled with an actionable connection error

#### Scenario: Connection loss
- **WHEN** a previously ready server becomes unreachable
- **THEN** the app offers reconnect while allowing private saved-image browsing and preserving the draft
