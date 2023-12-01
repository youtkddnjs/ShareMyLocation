plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
}

android {
    signingConfigs{
        create("myDebug"){
            storeFile = file("sharemylocation.jks")
            storePassword = "sharemylocation"
            keyAlias = "key0"
            keyPassword = "sharemylocation"
        }
        buildTypes {
            getByName("debug") {
                signingConfig = signingConfigs.getByName("myDebug")
            }
        }
    }
    namespace = "mhha.sample.sharemylocation"
    compileSdk = 34

    defaultConfig {
        applicationId = "mhha.sample.sharemylocation"
        minSdk = 24
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    viewBinding{
        enable = true
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    implementation ("com.kakao.sdk:v2-all:2.18.0") // 전체 모듈 설치, 2.11.0 버전부터 지원
    implementation ("com.kakao.sdk:v2-user:2.18.0") // 카카오 로그인

    implementation(platform("com.google.firebase:firebase-bom:32.6.0"))

    //google map
    implementation("com.google.android.gms:play-services-maps:18.1.0")
}