# FixAdvisor — Build Brief for the Builder

Working name only; Shahab has not picked a final name. Do NOT ask him about
the name until the MVP compiles — it ships as a placeholder until he chooses.

Plan: `/home/hatch/workspace/apps/fixadvisor/plan.md` (read it first — it is the
source of truth for features; this file is the source of truth for *how to build
without repeating the Warranty Tracker 8-build grind*).

Target: `~/workspace/apps/fixadvisor/android/` — complete Kotlin/Compose source,
same deliverable shape as the other two apps (README + BUG-REVIEW self-review).

## Proven build recipe (copied from Warranty Tracker's green config — do NOT improvise)

- `compileSdk = 36`, `targetSdk = 36` (Play rejects new apps below API 36)
- Root plugins: `com.android.application` 8.10.0, `org.jetbrains.kotlin.android`
  2.0.21, `org.jetbrains.kotlin.kapt` 2.0.21, **`org.jetbrains.kotlin.plugin.compose`
  2.0.21` (REQUIRED with Kotlin 2.0+ — the old
  `composeOptions { kotlinCompilerExtensionVersion = "1.5.15" }` block is INVALID
  with Kotlin 2.0 and will fail the build), `com.google.gms.google-services` 4.4.2
- Workflow must use **Gradle 8.11.1+** (AGP 8.10 refuses 8.10.2) and the
  `packages:` line must be `'platform-tools platforms;android-36 build-tools;36.0.0'`
  — single quotes only, NO literal double quotes (sdkmanager chokes on them)
- `gradle.properties` MUST contain `android.useAndroidX=true` (the
  `mapReleaseSourceSetPaths` task fails without it) plus the low-memory settings:
  `org.gradle.jvmargs=-Xmx1280m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8`,
  `org.gradle.workers.max=1`, `kotlin.compiler.execution.strategy=in-process`
- Compose via `compose-bom:2024.09.00`; Java 17 (`jvmTarget = "17"`)
- XML drawables: check for DUPLICATE attributes before committing — build #7 died
  on a duplicated `fillColor` in `ic_launcher.xml`
- `google-services.json`: ship a PLACEHOLDER; Shahab creates the real Firebase
  project himself later (new separate project — see Memory) and uploads the real
  file via GitHub web himself. The app must still compile and run without Firebase.

## FixAdvisor tech deltas vs the other two apps

- Online BY DESIGN: Firebase Auth (Google sign-in) + Firestore + Storage are
  required, not optional. Offline caching is a nice-to-have, not the architecture.
- Map view: use the Maps SDK for Android with a **placeholder API key** in the
  manifest (`com.google.android.geo.API_KEY`) and a clear README note telling
  Shahab to paste his key from Google Cloud Console. List view must work fully
  without the key so the app is usable while he gets one.
- Ads: AdMob banner slot at the bottom of list/profile screens from day one
  (Shahab's decision). Use a TEST ad unit ID (`ca-app-pub-3940256099942544/6300978111`)
  so it compiles and runs; README notes where to swap in his real ID.
- `minSdk = 26`, namespace/applicationId `com.shahab.fixadvisor` (placeholder).

## Suggested Firestore schema (keep it flat, per-user-style rules do NOT apply here)

- `shops/{shopId}` — public read; write: owner only
  (name, address, lat/lng, services[], priceList[{repair, price}], hours, photos[],
  websiteUrl, googleMapsUrl, badges{genericParts, warrantyDays, dataSafe},
  qrSecret, viewCount, ratingAvg, reviewCount, ownerUid, claimed)
- `shops/{shopId}/reviews/{reviewId}` — public read; create: signed-in users
  (repairType, phoneModel, pricePaid, durationDays, partsQuality, wouldReturn,
  stars, text, verified bool, repairCode, photos[] (parked — no Storage upload yet),
  authorUid, createdAt)
- `repairCodes/{code}` — read: shop owner only (shopId, issuedAt, used bool).
  Flow: shop generates codes in-dashboard → prints on receipts → customer enters
  code when reviewing → code marked used → review gets "verified" tick.
- `views/{shopId}/days/{yyyy-MM-dd}` — public read, owner-only aggregated write
  for the dashboard (counters; simplest correct first cut).

Security rules sketch for README: shops/reviews public read; shop doc write only
when `request.auth.uid == resource.data.ownerUid`; review create requires auth
and `authorUid == request.auth.uid`; owners may write `replies` subcollection on
their own shop's reviews. Owners can NEVER delete reviews (rules enforce this).

## Parked — build WITHOUT these (Shahab said "as-is")

1. "Get a quote" (customer posts repair need → shops reply with prices)
2. Drop-off booking (day/time picker + shop confirmation)
3. Photo proof on reviews
4. Fair-price guide (phase 2 in plan)

## Done checklist before handoff

- [ ] Full Android source in `~/workspace/apps/fixadvisor/android/` (Kotlin/Compose)
- [ ] README.md + BUG-REVIEW.md (self-reviewed, same bar as the other apps)
- [ ] Phase 1 MVP: sign-in, shop register/claim, search + profiles, reviews with
      repair details, QR-code verified reviews, owner replies, counter QR code
- [ ] Compiles against the recipe above (CI only proves compilation — same honest
      caveat as always: real tap-through testing happens on his emulator)
