# FixAdvisor

A review app dedicated to **mobile phone repair shops**. Shops register their
name, address, website/Google Maps link, services and prices; customers who got
a repair leave a review with the details Google reviews never show — repair
type, phone model, price paid, time taken, parts quality and whether they'd
go back. Anyone can search shops and read real repair reviews before choosing.

Inspired by RepairPal (which did exactly this for car repair in the US).
Nobody owns the phone-repair version yet.

- Package: `com.shahab.fixadvisor`
- Min SDK 26 · Compile/Target SDK 36
- Built with Kotlin, Jetpack Compose (Material 3), Navigation Compose,
  ViewModel + StateFlow
- **Online by design** — reviews are public, so the backend (Firebase) is
  required, not optional like in the sibling apps. Browsing works anonymously;
  writing reviews and registering shops require Google sign-in.

## Features (Phase 1)

1. **Home / search** — live shop list; search by shop name, area or service;
   filter by repair type; sort by top rated / most reviewed / newest.
2. **Shop profiles** — photos, average rating + review count, address,
   opening hours, services with a price list (RM), and Call / WhatsApp /
   Directions / Website / Google Maps action buttons.
3. **Reviews with repair details** — star rating plus repair type, phone
   model, price paid, time taken, parts quality, would-return, and free text.
   The review form shows repair-type and parts-quality chips for fast entry.
4. **Verified reviews (QR code)** — every shop has a stable 6-digit
   verification code (derived from its id) and a **counter QR code** screen:
   the owner prints it, customers scan it after a repair and enter the code,
   and that review gets a green "Verified repair" tick.
5. **Shop registration** — name, address, phone, WhatsApp, website and
   Google Maps links, opening hours, services + per-service prices, and up to
   6 photos (Photo Picker, no storage permission needed; uploaded to Firebase
   Storage). One shop per account in Phase 1.
6. **Owner replies** — the shop owner can reply publicly to any review on
   their shop. Owners can never edit or delete a customer's review.
7. **Profile** — Google sign-in/out, "My reviews" (with delete; the shop's
   rating updates automatically), "Favourite shops" (heart on any shop),
   "My shop" with a shortcut to the counter QR screen.
8. **Rating aggregates** — a shop's average rating and review count are
   maintained by a Firestore transaction on every review write/delete, so
   list sorting is always accurate.

Not in Phase 1 (parked for later): quote requests, booking, photo proof on
reviews, the fair-price guide, trust-badge verification, ads and the shop
annual fee (see `plan.md`).

### Data model (Firestore)

- `shops/{shopId}` — name, address, phone, whatsapp, websiteUrl,
  googleMapsUrl, services[], prices{repairType: RM}, openingHours,
  photoUrls[], ownerUid, ownerEmail, verificationCode, averageRating,
  reviewCount, createdAt, updatedAt
- `reviews/{reviewId}` — shopId, authorUid, authorName, stars, repairType,
  phoneModel, pricePaid, timeTaken, partsQuality, wouldReturn, text,
  verified, ownerReply, ownerReplyAt, createdAt
- `users/{uid}` — displayName, email, favoriteShopIds[], ownedShopId,
  createdAt (private — only the owner can read/write)

Queries deliberately use single-field filters (`shopId`, `authorUid`) with
client-side sorting, so **no composite Firestore indexes need to be created**.

## Firebase setup (REQUIRED — do this before sign-in/reviews can work)

`app/google-services.json` is currently a clearly-marked **placeholder** —
the app builds with it, but Firebase will fail at runtime until you replace it.

⚠️ **Create a NEW Firebase project for FixAdvisor.** Do NOT reuse the Warranty
Tracker project — a different app (different package name) needs its own
project and its own `google-services.json`, or sign-in breaks.

a. Go to [console.firebase.google.com](https://console.firebase.google.com)
   and create a new project (any name, e.g. "FixAdvisor"). Analytics/Gemini can
   stay off.
b. In the project, add an **Android app** with package name
   `com.shahab.fixadvisor`. Then register the SHA-1 fingerprint of **every**
   certificate that will sign this app — skip one and Google Sign-In fails
   on those builds (`ApiException: 10`, "developer error"):
   - **Debug builds:** the debug certificate SHA-1 (Android Studio shows it
     under the Gradle `signingReport` task).
   - **Your signed release `.aab`:** the SHA-1 of the keystore you create
     for the release build: `keytool -list -v -keystore <file>.jks`.
   - **After enrolling in Play App Signing:** copy the **App signing key
     certificate** SHA-1 from Play Console > Setup > App integrity and add
     it here too.
   Add each fingerprint under Project settings > Your apps > SHA certificate
   fingerprints. Every time you add one, **re-download** `google-services.json`
   (step c) so it picks up the new entry.
c. Download the generated `google-services.json` and copy it over
   `app/google-services.json` (replace the placeholder).
d. In the Firebase console go to **Authentication > Sign-in method** and
   **enable the Google provider**.
e. Go to **Firestore Database** and **Create database** (production mode is
   fine — the rules below lock it down; choose any region).
f. Go to **Storage** and **Get started** to create the default bucket.
g. Paste these security rules.

**Firestore rules** (Firestore Database > Rules) — draft, review before
publishing to real users:

```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {

    function signedIn() { return request.auth != null; }

    // Shops: public read.
    // Create: signed in, and ownerUid must be yourself.
    // Update: the owner — OR any signed-in user updating ONLY the rating
    // aggregates (that write is part of the review transaction).
    match /shops/{shopId} {
      allow read: if true;
      allow create: if signedIn()
        && request.resource.data.ownerUid == request.auth.uid;
      allow update: if signedIn() && (
        resource.data.ownerUid == request.auth.uid ||
        request.resource.data.diff(resource.data).affectedKeys()
          .hasOnly(['averageRating', 'reviewCount', 'updatedAt'])
      );
      allow delete: if false;
    }

    // Reviews: public read.
    // Create: signed in as the author.
    // Update: the author (own review) — or the shop's owner changing ONLY
    // the reply fields (owners can reply, never edit/delete reviews).
    // Delete: the author only.
    match /reviews/{reviewId} {
      allow read: if true;
      allow create: if signedIn()
        && request.resource.data.authorUid == request.auth.uid;
      allow update: if signedIn() && (
        request.auth.uid == resource.data.authorUid ||
        (
          request.auth.uid == get(/databases/$(database)/documents/shops/$(resource.data.shopId)).data.ownerUid &&
          request.resource.data.diff(resource.data).affectedKeys()
            .hasOnly(['ownerReply', 'ownerReplyAt'])
        )
      );
      allow delete: if signedIn() && request.auth.uid == resource.data.authorUid;
    }

    // Users: fully private — profiles, favourites, owned shop id.
    match /users/{uid} {
      allow read, write: if signedIn() && request.auth.uid == uid;
    }
  }
}
```

**Storage rules** (Storage > Rules) — draft. Shop photos are public (they're
shown to everyone). Writes are limited to signed-in users under `shops/…`;
tightening this to the shop's owner needs a Firestore lookup in the rules
and is a Phase 2 hardening step:

```
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /shops/{shopId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null;
    }
  }
}
```

## Build & run

1. Open the `fixadvisor` folder in **Antigravity** (open the whole folder, not
   individual files) or **Android Studio** (Koala or newer).
2. Let Gradle sync (first sync downloads dependencies; needs internet once).
   `local.properties` (SDK path) is created automatically by the IDE —
   it's git-ignored and machine-specific.
3. Run on an emulator or device with **API 26+**:
   - `Run > Run 'app'` (recommended). For terminal builds you need the
     Gradle wrapper: in Android Studio open View > Tool Windows > Gradle,
     find the `wrapper` task under the root project, double-click it to
     generate `gradlew`, then `./gradlew assembleDebug`.
4. The debug APK lands in `app/build/outputs/apk/debug/`.

Notes:
- Kotlin 2.0.21 with the Compose compiler plugin (same pairing as the
  sibling apps); AGP 8.10.0; compileSdk/targetSdk 36 (Play requirement).
- Firestore offline persistence is enabled: reads work offline from cache,
  writes queue and sync when the connection returns.
- The QR code encodes the placeholder web route `https://fixadvisor.app/r/{shopId}`.
  There is no website yet — when one exists (or App Links are set up), the
  same route can be adopted without reprinting counter stickers.

## Project structure

```
app/src/main/java/com/shahab/fixadvisor/
├── MainActivity.kt            # Entry point (edge-to-edge, Compose)
├── FixAdvisorApp.kt              # Application + manual DI container
├── data/
│   ├── models/                # Shop, Review, UserProfile, repair-type lists
│   ├── repo/                  # AuthManager, Shop/Review/User repositories
│   └── util/                  # QrCodeUtils (zxing), price/date formatting
└── ui/
    ├── theme/                 # Material 3 theme (green brand)
    ├── navigation/NavGraph.kt # Routes + NavHost, bottom-bar tabs
    ├── components/            # Stars, shop/review cards, sign-in, intents
    ├── screens/               # Home, ShopDetail, WriteReview,
    │                          # RegisterShop, ShopQr, Profile
    └── viewmodel/             # One ViewModel per screen + VmFactory
```
