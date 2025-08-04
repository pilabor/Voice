plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.data)

  implementation(libs.androidxCore)
  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.coroutines.guava)
  implementation(libs.serialization.json)

  // implementation(libs.media3.exoplayer)
  // implementation(libs.media3.session)
  implementation(project(":media3-lib-exoplayer"))
  implementation(project(":media3-lib-session"))


  testImplementation(libs.bundles.testing.jvm)
  testImplementation(libs.media3.testUtils.core)
  testImplementation(libs.media3.testUtils.robolectric)

  // testImplementation(project(":media3-lib-testUtils-core"))
  // testImplementation(project(":media3-lib-testUtils-robolectric"))
}
