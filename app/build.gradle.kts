plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.netshield.vpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.netshield.vpn"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        debug {
            isMinifyEnabled = false
        }
    }

    // Two separate apps built from one codebase:
    //  - "admin"  : full app, includes the manual/panel/subscription config-management UI.
    //               This is the build YOU install to add/curate servers.
    //  - "user"   : the app your customers install. Config-adding UI is compiled out
    //               entirely (BuildConfig.IS_ADMIN gates it in LocationsScreen.kt), and on
    //               first launch it silently pulls its server list from
    //               DEFAULT_SUBSCRIPTION_URL below — just like Windscribe/ExpressVPN,
    //               there's no "add config" screen for end users at all.
    flavorDimensions += "audience"
    productFlavors {
        create("user") {
            dimension = "audience"
            buildConfigField("boolean", "IS_ADMIN", "false")
            // Set this to the subscription link your panel (Marzban/X-UI) generates for
            // the plan you want every end user to share, e.g.
            // "https://panel.example.com/sub/abc123". Left blank = user sees an empty
            // list until you set this.
            buildConfigField("String", "DEFAULT_SUBSCRIPTION_URL", "\"\"")
        }
        create("admin") {
            dimension = "audience"
            applicationIdSuffix = ".admin"
            versionNameSuffix = "-admin"
            buildConfigField("boolean", "IS_ADMIN", "true")
            buildConfigField("String", "DEFAULT_SUBSCRIPTION_URL", "\"\"")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    // Core / Compose
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")
    implementation("androidx.activity:activity-compose:1.9.1")
    implementation(platform("androidx.compose:compose-bom:2024.06.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material:material-ripple")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking (panel API: Marzban / X-UI style REST)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Local storage of configs
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // QR scan for importing configs from panel
    implementation("com.google.zxing:core:3.5.3")
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // Billing (subscription purchase)
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    // Used by VpnTunnelService to broadcast real connect/disconnect state to the UI.
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")

    // ------------------------------------------------------------------
    // V2Ray/Xray core: the actual proxy core (tun2socks + xray-core) ships
    // as a prebuilt AAR, e.g. AndroidLibXrayLite
    // (https://github.com/2dust/AndroidLibXrayLite). Grab `libv2ray.aar`
    // from that project's Releases (or build it yourself with their
    // `make` target, which needs Go + gomobile), then:
    //   1. copy it to app/libs/libv2ray.aar
    //   2. uncomment the line below
    // XrayCoreBridge.kt / VpnTunnelService.kt are already wired to call it.
    implementation(files("libs/libv2ray.aar"))
    // ------------------------------------------------------------------

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
