# Tyler Durden Vault (Android Phone-as-HSM)

## Overview
The Vault feature turns the Android device into a hardware-backed decryption oracle:

1. Vault data is encrypted with an AES-256-GCM key in Android Keystore (`tyler_vault_key`).
2. The key is non-exportable and marked user-auth-required.
3. Server requests (`vault.decrypt`) send encrypted vault blob + target field + browser RSA public key.
4. Phone decrypts locally, extracts only requested field, re-encrypts with RSA-OAEP for browser, then wipes plaintext buffers.

## Components

### `VaultKeystore.kt`
- Manages Keystore AES key lifecycle.
- Provides `encrypt()` and `decrypt()` against `EncryptedBlob(iv, ciphertext)`.
- Uses GCM tag embedded in ciphertext (platform default).

### `VaultStore.kt`
- Defines serializable vault models (`VaultField`, `VaultData`).
- Stores encrypted blob in app-internal storage (`filesDir/vault_local_blob.bin`).
- Supports:
  - `loadLocal()`
  - `saveLocal()`
  - `exportBlob()`
  - `maskValue()`
- Includes tier lookup for fields (Tier 1/2/3).

### `VaultSyncManager.kt`
- Upload helper to send encrypted blob to gateway using caller-supplied request function.
- Payload: `{ "vault_blob": "<base64>" }`.

### `VaultDecryptHandler.kt`
- Handles `vault.decrypt` + `vault.sync` invoke routes.
- Flow:
  - Parse + validate request
  - Approval gate callback
  - Biometric gate callback for Tier 2/3
  - Keystore decrypt
  - Field extraction
  - RSA-OAEP encryption using browser SPKI public key
  - Return `{ encrypted_value, field, masked }`
- Wipes plaintext byte arrays after use.

## Security Notes
- Plaintext bytes are zero-filled (`ByteArray.fill(0)`) immediately after use.
- Key material never leaves Android Keystore.
- Only field-level response returned (not full vault).
- Tier policy:
  - Tier 1: first_name, last_name, dob, phone, email, address
  - Tier 2: credit_card, card_expiry, passport, membership_number
  - Tier 3: cvv

## UI integration
Vault UI composables are under `ui/vault/` and use Tyler copy throughout.

## Font placeholders (AIR typography)
AIR font resources are currently placeholders backed by existing Manrope files.
The following resources were added as XML placeholders and should be replaced with real `.ttf` assets:

- `res/font/poppins_400_regular.xml`
- `res/font/poppins_500_medium.xml`
- `res/font/poppins_600_semibold.xml`
- `res/font/dm_sans_400_regular.xml`
- `res/font/dm_sans_600_semibold.xml`

When ready, replace with true TTF assets named:
- `poppins_400_regular.ttf`
- `poppins_500_medium.ttf`
- `poppins_600_semibold.ttf`
- `dm_sans_400_regular.ttf`
- `dm_sans_600_semibold.ttf`
