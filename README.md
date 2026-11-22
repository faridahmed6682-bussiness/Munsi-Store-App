# Munsi Store — Android E-commerce App

**মুনসি স্টোর** — Rangpur, Bangladesh. A complete Android e-commerce app for the shop:
customer shopping + in-app admin panel, built with Kotlin + Jetpack Compose, Room
offline database, and Firebase (Spark free plan). Everything in this project runs
on **free plans only**: GitHub free public repo + free Actions minutes, Firebase
Spark (no card, no billing).

- **Repo:** https://github.com/faridahmed6682-bussiness/Munsi-Store-App
- **Download (APK):** https://github.com/faridahmed6682-bussiness/Munsi-Store-App/releases/tag/v1.0.0
  — download `app-debug.apk`, open it on an Android phone, allow "Install unknown apps" if asked.
- **Package:** `com.munsistore.app` • **minSdk 24** (Android 7.0+) • **version 1.0.0**

## Features

### Customer app
- Product catalog with categories (razor, oral care, bath, grocery, cosmetics, handmade)
- Search (English + Bengali names), product details with price / old price / rating / stock
- Cart with quantity controls, delivery fee (free over ৳500, otherwise ৳60)
- Checkout with **Cash on Delivery**, **bKash** and **Nagad** (manual — send money, mention order ID)
- Order tracking (Placed → Confirmed → Shipped → Delivered, or Cancelled)
- Wishlist, order history
- Login with **Google Sign-In** + email/password (Firebase Auth when configured, local account fallback offline)
- Bengali + English UI (switch in Profile; Bengali is the default)

### Admin panel (inside the app)
- Login as admin, then Profile → Admin Panel
- Dashboard: total sales, total orders, pending orders, product count
- Manage orders: confirm / ship / deliver / cancel
- Manage products: add / edit / delete (name, Bengali name, price, old price, category, stock, image URL, featured)

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
3. Publishes/updates the **v1.0.0 GitHub Release** with `app-debug.apk` attached
   (public download link, no extra step needed).

## Project structure
```
app/src/main/java/com/munsistore/app/
  MainActivity.kt            Navigation + bottom bar
  MunsiStoreApp.kt           Application: Room + Firebase init + seed data
  data/
    Models.kt                Room entities (Product, Cart, Order, User…)
    AppDatabase.kt           Room database + DAOs
    SeedData.kt              12 demo products + demo admin
    StoreRepository.kt       Data operations (Room first, Firestore best-effort)
    FirebaseManager.kt       Firebase init / Auth / Firestore / Storage helpers
    SessionManager.kt        Login + language preferences
  ui/
    StoreViewModel.kt        Shared view-model for all screens
    HomeScreen.kt            Catalog, search, categories
    ProductDetailScreen.kt   Details, quantity, add to cart / buy now
    CartScreen.kt            Cart + checkout (COD / bKash / Nagad)
    OrdersScreen.kt          Order list + tracking timeline
    WishlistScreen.kt        Wishlist
    AuthScreen.kt            Email/password + Google sign-in
    AdminScreen.kt           Dashboard, order + product management
    ProfileScreen.kt         Profile, language, shop info
    Components.kt            Shared product tile, price format, status chip
    theme/Theme.kt           Munsi Store green Material 3 theme
app/src/main/res/values/strings.xml       English
app/src/main/res/values-bn/strings.xml    Bengali
```

## Limitations (honest notes)
- Debug APK: install it directly; it is **not** a Play Store release (no signing key yet).
- bKash/Nagad are manual — the app does not call the bKash/Nagad payment APIs
  (those need a merchant account). The shop confirms payment by phone.
- Product photos are category icons unless an image URL is pasted in the admin form;
  Firebase Storage upload helper exists in `FirebaseManager.uploadProductImage`.
- Demo admin password is in the seed data — change it before giving the APK to anyone else.
