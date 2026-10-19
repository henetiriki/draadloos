import kotlinx.kover.gradle.plugin.dsl.CoverageUnit

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kover)
}

// Release signing values come from Gradle properties (~/.gradle/gradle.properties) or environment
// variables (CI), never from the repo. The keystore defaults to the git-ignored one in the repo root.
fun signingValue(name: String): String? =
    providers.gradleProperty(name).orElse(providers.environmentVariable(name)).orNull
        ?.takeIf { it.isNotBlank() }

val releaseKeystore: File =
    signingValue("DRAADLOOS_KEYSTORE")?.let(::file) ?: rootProject.file("draadloos-release.jks")
val releaseStorePassword: String? = signingValue("DRAADLOOS_KEYSTORE_PASSWORD")

android {
    namespace = "house.ouwl.draadloos"
    compileSdk = 37

    defaultConfig {
        applicationId = "house.ouwl.draadloos"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
    }

    signingConfigs {
        // Only defined when the keystore and its password are available, so debug builds, tests and
        // CI's other jobs work without them. Without it, assembleRelease makes an unsigned APK.
        if (releaseKeystore.exists() && releaseStorePassword != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = releaseStorePassword
                keyAlias = signingValue("DRAADLOOS_KEY_ALIAS") ?: "draadloos"
                keyPassword = signingValue("DRAADLOOS_KEY_PASSWORD") ?: releaseStorePassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        // Robolectric needs the merged resources (strings, drawables) on the test classpath.
        unitTests.isIncludeAndroidResources = true
        unitTests.all {
            // Robolectric reaches into FileDescriptor internals and loads a native runtime,
            // both of which newer JDKs (25+) lock down by default.
            it.jvmArgs(
                "--add-exports", "java.base/jdk.internal.access=ALL-UNNAMED",
                "--enable-native-access=ALL-UNNAMED",
            )
        }
    }
}

kover {
    reports {
        filters {
            excludes {
                classes("*.R", "*.R$*", "*.BuildConfig", "*ComposableSingletons*")
                annotatedBy("house.ouwl.draadloos.widget.SessionOnly")
            }
        }
        // Coverage minimums for koverVerify, set to what the tests reach. Only ever raise them.
        // The uncovered branches are defensive null checks and a stale-connection guard
        // that Robolectric can't reach.
        verify {
            rule {
                minBound(100, CoverageUnit.LINE)
                minBound(90, CoverageUnit.BRANCH)
            }
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.glance.appwidget)

    implementation(libs.media3.exoplayer)
    implementation(libs.media3.exoplayer.hls)
    implementation(libs.media3.session)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.truth)
    testImplementation(libs.androidx.test.core.ktx)
    testImplementation(libs.androidx.test.ext.junit.ktx)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    testImplementation(libs.glance.appwidget.testing)
    testImplementation(libs.media3.test.utils)
    testImplementation(libs.media3.test.utils.robolectric)
}
