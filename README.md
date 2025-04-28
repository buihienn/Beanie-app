# Beanie-app

## Overview
**Beanie-app** is a loyalty points and coffee shop management application developed for Android.  
It enables customers to place coffee orders, earn and accumulate loyalty points, apply discount vouchers, track their membership tiers, and review their past orders.

## Notes
This project was developed using **Android Studio** with **Kotlin** as the main programming language.  
It integrates **Firebase** services and **Cloudinary** for image storage and management.

## Some Basic Features
- User registration and login with Firebase Authentication.
- Place coffee and beverage orders.
- Earn loyalty points and unlock membership tiers (New, Loyal, VIP).
- Apply discount vouchers at checkout.
- Manage and review past orders.
- Admin features for managing vouchers, categories, and products.

## Requirements
- Android Studio (Arctic Fox or later)
- Kotlin 1.6 or higher
- Firebase project (Firestore, Authentication, Storage)
- Cloudinary account (for image uploads)

## Installation
1. **Clone the repository:**
   ```bash
   git clone https://github.com/buihienn/Beanie-app.git
   cd Beanie-app

2.  **Open the project in Android Studio:**
  - Launch **Android Studio**.
  - Select **Open an existing project** and navigate to the `Beanie-app` directory.

3. **Configure Firebase:**

  - Create a Firebase project.
  - Add an Android app to Firebase and download the `google-services.json` file.
  - Place `google-services.json` inside the `app/` directory.

4. **Configure Cloudinary (optional for image uploads):**

  - Create a free [Cloudinary](https://cloudinary.com/) account.
  - Obtain your **Cloud Name**, **API Key**, and **API Secret**.
  - Configure in your code:

```kotlin
val config = MediaManager.get().config
config.cloudName = "your_cloud_name"
config.apiKey = "your_api_key"
config.apiSecret = "your_api_secret"
