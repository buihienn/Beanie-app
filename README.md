Beanie-app
Beanie-app is a loyalty points and coffee shop management application developed for Android.
It allows customers to place orders, accumulate membership points, use vouchers, track their membership tier, and review their orders.

Features
User registration and login

Place orders for coffee and beverages

Accumulate loyalty points and unlock membership tiers (Silver, Gold, Platinum)

Apply discount vouchers to orders

Manage and review past orders

Admin management for vouchers, categories, and products

Technologies Used
Kotlin (Android development)

Firebase Firestore (Database)

Firebase Authentication (User login and registration)

Firebase Storage (Image storage)

Cloudinary (Image management)

Kotlin Coroutines (Asynchronous programming)

Glide (Image loading)

Getting Started
1. Clone the Repository
git clone https://github.com/buihienn/Beanie-app.git

2. Open the Project
Open the project with Android Studio.

3. Firebase Setup
Create your own Firebase project.

Download the google-services.json file and place it under the app/ directory.

4. Cloudinary Setup (for Image Uploads)
Go to Cloudinary and create a free account.

After signing up, retrieve your Cloud Name, API Key, and API Secret.

You can configure Cloudinary in your Android project like this:
val config = MediaManager.get().config
config.cloudName = "your_cloud_name"
config.apiKey = "your_api_key"
config.apiSecret = "your_api_secret"

Alternatively, you can define your credentials securely in a local properties file.

Example local.properties entry:
CLOUDINARY_URL=cloudinary://your_api_key:your_api_secret@your_cloud_name

5. Build and Run
Connect a real device or start an emulator.

Build and run the application from Android Studio.

Screenshots
![image](https://github.com/user-attachments/assets/6e04d6e5-9014-4c77-9b9e-79c2d3abf526)
![image](https://github.com/user-attachments/assets/3d6c3188-2f7e-4346-9654-2257de835e45)
![image](https://github.com/user-attachments/assets/7de7fb9b-5eda-4c07-bf88-586f19188268)



