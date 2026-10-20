# Bug Review — FixAdvisor

Self-review pass over every file, done after writing the complete project.
Each item below was found during the review and fixed (or verified correct).

## Issues found and fixed

1. **Invalid state-flow declarations** (`WriteReviewViewModel`,
   `RegisterShopViewModel`): the first draft declared form fields as
   `var x = MutableStateFlow(...); private set` — the semicolon ends the
   declaration, making `private set` a syntax error. Rewrote both files to
   private `…Flow` MutableStateFlows with public read-only `StateFlow`
   views, and updated every internal reference (setters and `submit()`).

2. **Missing flow import** (`WriteReviewViewModel`): `shop` uses
   `Flow.map {}` but `kotlinx.coroutines.flow.map` wasn't imported —
   compile error. Added. Audited the other ViewModels: ShopDetail,
   ShopQr and Profile all import `map` correctly.

3. **Unclosed call** (`RegisterShopScreen`): the photo-thumbnail
   `AsyncImage` in the picker row was missing its closing parenthesis,
   which would have swallowed the `IconButton` as an argument. Fixed and
   re-read the whole row.

4. **Dead placeholder line** (`ProfileScreen`): "My reviews" referenced a
   leftover `val allShops = favoriteShops // placeholder…`. Replaced with a
   real `shopNames: StateFlow<Map<String, String>>` in `ProfileViewModel`
   so each of my reviews shows which shop it's for.

5. **Unused imports**: removed `Flow`/`combine` (ProfileViewModel),
   `FontWeight`/`layout.width` (RegisterShopScreen). Also removed a stray
   zero-width `Spacer` left in RegisterShopScreen.

## Design decisions (verified intentional)

- **Manual Firestore mapping** (`mapShop`/`mapReview`/`mapUserProfile` with
  safe casts) instead of `toObject()`: no Firestore no-arg-constructor
  pitfalls on Kotlin data classes, and a malformed document yields null
  (skipped) instead of a crash.
- **Single-field queries + client-side sort/filter** (`whereEqualTo` on
  `shopId`/`authorUid` only; shops filtered in `ShopRepository.filterAndSort`):
  no composite Firestore indexes need creating — Shahab just pastes the
  README rules and it works. Fine at Phase 1 scale; revisit if the shop
  count grows into the thousands.
- **Rating aggregates in a transaction**: review write/delete updates the
  shop's `averageRating`/`reviewCount` in the same `runTransaction`, so the
  shop list never aggregates client-side and can't show stale ratings.
  The README rules allow any signed-in user to update *only* those three
  keys — that's what makes the transaction pass the rules.
- **Deterministic verification code**: SHA-256 of the shop id → stable
  6-digit code (`QrCodeUtils.verificationCodeFor`). The owner always sees
  the same code; nothing to store or sync. It is a trust signal, not a
  secret — a determined faker could brute-force 6 digits, which is why the
  plan's "one review per user per shop" rule also applies.
- **One review per user per shop** (Phase 1 approximation of the plan's
  "one review per repair"): checked in `WriteReviewViewModel` both at
  init (to explain the rule up front) and again inside `submit()` before
  the write. Note: it is enforced client-side only — the README rules don't
  (and can't cheaply) prevent a hand-crafted second review document.
- **No CALL_PHONE permission**: the Call button uses `ACTION_DIAL`.
  WhatsApp uses `https://wa.me/…` (number stored digits-only at
  registration — the repository strips non-digits). Directions use a
  `geo:` intent with the Google Maps link as fallback.
- **Photo Picker** (`PickVisualMedia`) for shop photos: no storage or
  camera permission anywhere in the manifest. Only `INTERNET` +
  `ACCESS_NETWORK_STATE`, which this app genuinely needs (online by design).
- **Firestore persistence ordering**: `firestoreSettings` is set in
  `FixAdvisorApp.onCreate()` *before* `AppContainer` is constructed, so no
  repository touches Firestore before persistence is configured (same
  pattern as the sibling apps).

## Verified correct (checked, no change needed)

- **Manifest**: `android:name=".FixAdvisorApp"` matches the Application class;
  package/namespace/applicationId all `com.shahab.fixadvisor`; launcher icon
  is a self-contained vector (green star), no binary files anywhere.
- **Gradle**: AGP 8.10.0, Kotlin 2.0.21 + Compose compiler plugin,
  `compileSdk`/`targetSdk` 36, minSdk 26, `android.useAndroidX=true` —
  identical conventions to the sibling apps whose builds are proven.
- **Navigation**: shop ids are Firestore ids (URL-safe), code arg is
  `Uri.encode`d; tab navigation uses `popUpTo(HOME) { saveState = true }`
  + `launchSingleTop` + `restoreState`; ViewModels use stable keys
  (`detail_…`, `review_…`, `qr_…`) so per-shop state never collides.
- **Sign-in gating**: WriteReview and RegisterShop render a sign-in prompt
  when signed out and create the user profile via
  `UserRepository.ensureProfile` on success (required before
  `users/{uid}` writes used by registration and favourites).
- **Null/edge handling**: empty photo list, blank links (buttons hidden),
  invalid price text (registration rejects with a message; review price
  falls back to null), shop deleted mid-read (transaction throws a clear
  error), rating average clamped ≥ 0 on review delete.

## Known gaps / untested (honest list)

- **NOT compiled or run.** There is no working Android build environment on
  this machine; the code is complete and self-reviewed only. The first
  Gradle sync/build in Antigravity is the real test. Most likely friction
  points if anything surfaces: exact dependency versions resolved from
  Maven Central (`io.coil-kt:coil-compose:2.7.0`, `com.google.zxing:core:3.5.3`)
  and any Compose API drift in BOM 2024.09.00 (dropdowns were deliberately
  avoided in favour of `FlowRow` + `FilterChip` to minimise this risk).
- **QR deep link is a placeholder.** The QR encodes
  `https://fixadvisor.app/r/{shopId}`; there is no website or App Links
  config yet, so scanning it today opens nothing useful — in Phase 1 the
  *printed code* is the working part of the sticker (customers type it in
  the review form; the form also accepts a prefilled code via the
  `write_review/{shopId}?code=` route for when links exist).
- **Storage rules draft is permissive** (any signed-in user can write
  under `shops/…`). Tightening needs a Firestore lookup inside the Storage
  rules; flagged in the README as a Phase 2 hardening step.
- **`ProfileViewModel.removeFavorite` is not wired to a UI button** —
  favourites are un-saved via the heart on the shop detail screen. The
  method is kept as the repository-level capability for a future
  edit-favourites UI.
- **Rating float drift**: averages are recomputed incrementally from the
  stored average, so over hundreds of reviews tiny float error can
  accumulate. Acceptable at MVP scale; a periodic recompute (Cloud
  Function) is the Phase 2 fix.
- **No moderation queue yet** (the plan lists one under trust & safety):
  Phase 1 relies on sign-in + one-review-per-shop + the verified code.
  Abusive reviews can only be removed manually in the Firebase console.
- Declined-by-user features are intentionally absent (quote requests,
  booking, review photo proof) — parked in `plan.md` for later.
