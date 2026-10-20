# FixAdvisor — Phone Repair Shop Review App (working name)

Shahab's idea (2026-10-20): a review app dedicated to mobile phone repair shops.
Shops register their name, address and website/Google link; customers who got a
repair leave a review with repair-specific details; anyone searching sees ratings,
prices, warranty and trust badges before choosing a shop.

Model proof: RepairPal did this for car repair in the US and became the biggest
trusted shop network there. Nobody owns the phone-repair version yet — today
those reviews sit scattered inside Google Maps with no repair details.

## For customers (free)
- Search by area, phone model + repair type (e.g. "iPhone 13 screen"), or shop name
- Map view + list view; filters: nearest, highest rated, cheapest, fastest, longest warranty
- Shop profile: services, price list per repair, opening hours, photos, address,
  call / WhatsApp / directions buttons, website + Google Maps link
- Reviews with repair details (the part Google reviews don't have):
  repair type, phone model, price paid, time taken, parts quality, would-return yes/no
- Verified reviews: after a repair the shop gives a QR code / repair code; only
  customers with a code can mark a review "verified" — this fights fake reviews
- Fair-price guide: typical price range for common repairs (screen, battery,
  charging port, water damage) in that area, so customers know if a quote is fair
- Follow / save favourite shops; report a review

## For shop owners (free to join)
- Register / claim their shop: name, address, services, price list, photos,
  website or Google link
- Reply to reviews publicly
- Trust badges (earned, not bought):
  - Genuine Parts — uses original/OEM-quality parts
  - Warranty — states the warranty period given on repairs (30/90/180 days)
  - Data Safe — promises not to access customer data
- Counter QR code sticker: "Got a repair? Rate us" — printed for the shop,
  this is how reviews (and users) actually arrive
- Simple dashboard: rating, review count, profile views, recent reviews

## Trust & safety rules
- One review per repair; verified badge only with a shop-issued code
- Owners cannot delete bad reviews — they can only reply
- Moderation queue for abusive/fake reviews; phone-number or Google sign-in to review

## Money (later, Phase 2 — Shahab's decisions 2026-10-20/21)
- Two streams:
  1. Small banner ads for users (AdMob) — banner only at the bottom of screens,
     never pop-ups. Pocket money, not the main income: ads pay cents per click,
     so real money needs thousands of active users.
  2. One simple FLAT ANNUAL FEE for shops, no tiered plans — small shops hate complexity.
     The fee for a shop starts ONLY once that shop actually gets customers through the app.
- NEW 2026-10-21: **Review dispute fee** — a shop that believes a review is fake or
  baseless can open a dispute for a small fee (~RM5) covering human moderation.
  The shop must attach screenshot evidence (e.g. no visit record, chat proof that
  the reviewer was never a customer); disputes land in an admin moderation queue
  where Shahab reviews the evidence and decides; the fee is REFUNDED if the review
  is found unfair and removed. **Never sell guaranteed deletion** — letting shops
  pay to delete bad reviews would destroy customer trust in the whole app (trust
  is the one thing FixAdvisor sells).
- NEW 2026-10-21: **All payments via Google Play Billing** — multi-currency is
  automatic (PKR in Pakistan, RM in Malaysia, etc.), no extra code needed.
  Note: Google Play takes a 15–30% commission on in-app purchases.
- Free while the app is new; founding shops could get year one free, or a RM49 lifetime rate.
- Target price once there's traction: ~RM60–99/year (think in RM, not $ — shops are Malaysian).
- What the fee unlocks: Verified badge, top placement in their area, full dashboard,
  QR sticker pack.
- Rule: never charge before the app visibly brings the shop customers.

## Tech (same stack as Shahab's other apps)
- Android app (Kotlin/Compose), built like Where Did I Put It / Warranty Tracker
- Backend REQUIRED and online (reviews are public): Firebase — Google sign-in,
  Firestore (shops, reviews, users), Storage for shop photos
- Package: com.shahab.fixadvisor (placeholder until he picks a final name)

## Build order
- Phase 1 (MVP): sign-in, shop registration, search + profiles, reviews with
  repair details, QR verified reviews, owner replies, counter QR code
- Phase 2: fair-price guide, badge verification, dashboard, premium listings
