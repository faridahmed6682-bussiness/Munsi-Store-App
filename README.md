# Munsi Store — Android E-commerce App

**মুনসি স্টোর** — Rangpur, Bangladesh. A complete Android e-commerce app for the shop:
customer shopping + in-app admin panel, built with Kotlin + Jetpack Compose, Room
offline database, and Firebase (Spark free plan). Everything in this project runs
on **free plans only**: GitHub free public repo + free Actions minutes, Firebase
Spark (no card, no billing).

- **Repo:** https://github.com/faridahmed6682-bussiness/Munsi-Store-App
- **Download (APK):** https://github.com/faridahmed6682-bussiness/Munsi-Store-App/releases/tag/v1.1.0
  — download `app-debug.apk`, open it on an Android phone, allow "Install unknown apps" if asked.
- **Package:** `com.munsistore.app` • **minSdk 24** (Android 7.0+) • **version 1.1.0** (versionCode 2)

## What's new in v1.1.0 (Daraz-style)
- **Flash sale** on home with a live countdown and discounted prices (4 seeded sale products)
- **Vouchers / promo codes** — percent, fixed amount, free delivery; apply at checkout;
  admin creates, lists and disables them (seeded: `WELCOME10` 10% off, `FLAT50` ৳50 off over ৳300,
  `FREEDLV` free delivery over ৳200, `EID20` 20% off over ৳1000)
- **Payments** — COD, bKash / Nagad / **Rocket** (manual, shop send-money number + transaction-ID field),
  SSLCommerz shown as a gateway-ready placeholder (not live yet)
- **Reviews &amp; ratings** (stars + text, live average on cards and detail page) and **product Q&amp;A**
- **Filters + sorting** — price range, brand, minimum rating, category; sort by price, rating, newest, most popular
- **Address book** — multiple saved addresses; delivery zones Dhaka City ৳60 / Inside Dhaka ৳80 /
  Outside Dhaka ৳120 (free over ৳500), charged at checkout
- **Order tracking timeline** — Placed → Confirmed → Packed → Shipped → Out for delivery → Delivered,
  with courier name (Steadfast / Pathao / RedX / Paperfly); return / refund request from delivered orders
- **Referral &amp; coins** — every account gets a referral code (`MUNSI-XXXXXX`); 50 coins for signing up
  with a code, 1 coin per ৳10 spent on delivered orders, 50 coins to the referrer on the first delivery;
  redeem up to 10% of the subtotal at checkout (1 coin = ৳1)
- **Seller mode** — register a shop name, get a seller dashboard with your own products and orders
- **Recently viewed + "Recommended for you"** on home; **trending searches**
- **Local notifications** for order status changes (toggle in Profile; needs the notification permission
  on Android 13+)

## Features

### Customer app
- Product catalog with categories (razor, oral care, bath, grocery, cosmetics, handmade)
- Search (English + Bengali names), product details with price / old price / rating / stock
- Cart with quantity controls, delivery fee (free over ৳500, otherwise by delivery zone)
- Checkout with **Cash on Delivery**, **bKash**, **Nagad** and **Rocket** (manual — send money,
  enter the transaction ID), vouchers and coins
- Order tracking (Placed → Confirmed → Packed → Shipped → Out for delivery → Delivered, or Cancelled),
  return / refund requests
- Wishlist, order history, address book, seller mode
- Login with **Google Sign-In** + email/password (Firebase Auth when configured, local account fallback offline)
- Bengali + English UI (switch in Profile; Bengali is the default)

### Admin panel (inside the app)
- Login as admin, then Profile → Admin Panel
- Dashboard: total sales, total orders, pending orders, product count
- Manage orders: advance through the full pipeline (confirm / pack / ship / out for delivery / deliver / cancel)
- Manage products: add / edit / delete (name, Bengali name, price, old price, brand, category, stock, image URL, featured)
- **Vouchers tab:** create / list / enable-disable / delete promo codes
- **Returns tab:** approve / reject / mark-refunded on customer return requests

### Data & offline
- Room database is the source of truth — the app **works fully offline** with 12 seeded demo products
  (Gillette Guard ৳240/dozen, Lord razor, toothbrushes, soap, shampoo, groceries, crochet bag…).
- When Firebase is configured, orders/products also sync to Firestore (best-effort) and
  Auth uses Firebase; without a network the local copy still works.

## Firebase — already connected

Firebase project **Munsi Store** (`munsi-store-a72bb`, Spark free plan) is already
set up: Firestore (Production) + Auth (Email/Password and Google) enabled, Android
app `com.munsistore.app` registered. The real `app/google-services.json` is already
in this repo, so the release APK talks to the real Firebase project.

> The `apiKey` inside `google-services.json` is a public client identifier, not a
> private secret — Firebase security comes from Firestore rules, not from hiding
> that key. Still, **set Firestore rules** in the console (see below) so only the
> shop's admin can write products/orders.

### Firestore rules (recommended — set in Firebase console → Firestore → Rules)
```
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /products/{id} { allow read: if true; allow write: if request.auth != null; }
    match /orders/{id}   { allow read: if true; allow create: if true;
                           allow update: if request.auth != null; }
  }
}
```
Tighten further once real admin accounts exist in Firebase Auth.

### If you ever need to re-create the Firebase config
1. https://console.firebase.google.com → project **munsi-store-a72bb** (Spark plan — free, no billing).
2. Project settings → Your apps → Android app `com.munsistore.app` → download `google-services.json`.
3. Replace `app/google-services.json` in this repo and push — CI rebuilds the APK.

## Demo accounts (change these before real use)
- **Admin:** `admin@munsi.store` / `admin123` — seeded locally on first run.
  To change: edit `SeedData.adminUser` (or create a real admin in Firebase Auth and
  mark the matching local user `isAdmin = true`).
- Customers can register from the app (email + password).

## Build locally
```bash
# needs Android Studio or: JDK 17 + Android SDK 34
./gradlew assembleDebug        # APK at app/build/outputs/apk/debug/app-debug.apk
```

## CI — GitHub Actions (free)
`.github/workflows/android.yml` runs on every push to `main`:
1. Builds the debug APK (`assembleDebug`).
2. Uploads it as an Actions artifact.
3. Publishes/updates the **v1.1.0 GitHub Release** with `app-debug.apk` attached
   (public download link, no extra step needed).

## Project structure
```
app/src/main/java/com/munsistore/app/
  MainActivity.kt            Navigation + bottom bar (home, seller, addresses…)
  MunsiStoreApp.kt           Application: Room + Firebase init + seed data
  data/
    Models.kt                Room entities (Product, Cart, Order, User, Voucher, Review, Address…)
    AppDatabase.kt           Room database + DAOs (v2 schema)
    SeedData.kt              12 demo products + demo admin + vouchers + flash sale + reviews
    StoreRepository.kt       Data operations (Room first, Firestore best-effort)
    FirebaseManager.kt       Firebase init / Auth / Firestore / Storage helpers
    SessionManager.kt        Login + language + notification preferences
    ShopConfig.kt            Payment numbers, delivery fees, coin rules, couriers
    Notifications.kt         Local order-status notifications
  ui/
    StoreViewModel.kt        Shared view-model for all screens
    HomeScreen.kt            Catalog, search, categories, flash sale, trending, recommendations
    ProductDetailScreen.kt   Details, quantity, add to cart / buy now, reviews + Q&A
    ReviewsSection.kt        Reviews (stars + text) and product Q&A
    CartScreen.kt            Cart + checkout (COD / bKash / Nagad / Rocket, voucher, coins)
    OrdersScreen.kt          Order list + tracking timeline + return requests
    WishlistScreen.kt        Wishlist
    AuthScreen.kt            Email/password + Google sign-in (+ referral code)
    AdminScreen.kt           Dashboard, order + product management, vouchers, returns
    SellerScreen.kt          Seller registration + seller dashboard
    AddressBookScreen.kt     Saved delivery addresses + delivery zones
    ProfileScreen.kt         Profile, coins, referral code, notifications, language, shop info
    Components.kt            Shared product tile, price format, status chip, stars
    theme/Theme.kt           Munsi Store green Material 3 theme
app/src/main/res/values/strings.xml       English
app/src/main/res/values-bn/strings.xml    Bengali
```

## Limitations (honest notes)
- Debug APK: install it directly; it is **not** a Play Store release (no signing key yet).
- bKash/Nagad/Rocket are manual — the app does not call the bKash/Nagad payment APIs
  (those need a merchant account). The shop confirms payment by phone. SSLCommerz is a
  placeholder: no gateway keys are wired in yet.
- Product photos are category icons unless an image URL is pasted in the admin form;
  Firebase Storage upload helper exists in `FirebaseManager.uploadProductImage`.
- Demo admin password is in the seed data — change it before giving the APK to anyone else.
- The seeded flash sale ends 36 hours after the app's first run and is not re-seeded
  automatically — after that the home page simply shows no flash-sale section until an
  admin adds new flash products (or the app data is cleared).
- Notifications are local only: they fire when an order's status changes **on the same
  device** (e.g. the admin advancing an order). A customer on another phone gets no
  push — that needs Firebase Cloud Messaging, which is not wired in.
- Seller orders are matched by product ownership; an order mixing two sellers' products
  appears in both sellers' lists. Coins/referral balances live in the local Room DB
  (not synced to Firestore), so they don't carry across devices.
