# Third-party notices

Through Phase 2 task 1, no source code has been copied or adapted from ReFra, LibrePhotos, Immich, Fossify, or another gallery application. The items below are unmodified binary/build dependencies resolved by Gradle; project-authored production assets are tracked separately in `ASSET_LICENSES.md`.

| Component | Version | Source | License | Usage | Modified |
|---|---:|---|---|---|---|
| Android Gradle Plugin | 8.10.1 | https://android.googlesource.com/platform/tools/base | Apache-2.0 | Android build tooling | No |
| Kotlin Gradle plugins/runtime | 2.1.20 | https://github.com/JetBrains/kotlin | Apache-2.0 | Language and Compose compilation | No |
| Kotlin Symbol Processing | 2.1.20-1.0.32 | https://github.com/google/ksp | Apache-2.0 | Room code generation | No |
| Kotlin Coroutines Android | 1.10.2 | https://github.com/Kotlin/kotlinx.coroutines | Apache-2.0 | Asynchronous media work | No |
| AndroidX Core KTX | 1.16.0 | https://github.com/androidx/androidx | Apache-2.0 | Android framework extensions | No |
| AndroidX Activity Compose | 1.10.1 | https://github.com/androidx/androidx | Apache-2.0 | Compose activity host | No |
| AndroidX Lifecycle | 2.9.0 | https://github.com/androidx/androidx | Apache-2.0 | Runtime and ViewModel integration | No |
| AndroidX Compose BOM / UI / Material 3 | 2025.04.01 | https://github.com/androidx/androidx | Apache-2.0 | User interface | No |
| AndroidX Navigation Compose | 2.9.8 | https://github.com/androidx/androidx | Apache-2.0 | App navigation | No |
| AndroidX Paging | 3.3.6 | https://github.com/androidx/androidx | Apache-2.0 | Paged MediaStore presentation | No |
| AndroidX DataStore Preferences | 1.1.7 | https://github.com/androidx/androidx | Apache-2.0 | Main-realm state persistence | No |
| AndroidX Room | 2.7.1 | https://github.com/androidx/androidx | Apache-2.0 | Local metadata cache | No |
| AndroidX WorkManager | 2.10.1 | https://github.com/androidx/androidx | Apache-2.0 | Permission-gated background indexing | No |
| AndroidX ExifInterface | 1.4.1 | https://github.com/androidx/androidx | Apache-2.0 | Read photo GPS metadata | No |
| AndroidX Media3 | 1.7.1 | https://github.com/androidx/media | Apache-2.0 | Local video playback | No |
| Coil Compose | 3.2.0 | https://github.com/coil-kt/coil | Apache-2.0 | Image and thumbnail loading | No |
| Lottie Compose | 6.7.1 | https://github.com/airbnb/lottie-android | Apache-2.0 | Character feedback animation playback | No |
| Gradle License Report | 3.1.4 | https://github.com/jk1/Gradle-License-Report | Apache-2.0 | Dependency license gate and report | No |

Versions are locked in `gradle/libs.versions.toml` or the root build script. `generateLicenseReport` records resolved transitive dependencies and `checkLicense` enforces the allowlist.

Before any source is adapted from another project, this file must record its repository, exact commit SHA, original path, retained copyright/SPDX header, local path, and modification summary.


## Source reuse boundary

ReFra and LibrePhotos were previously inspected as design references. No source, model or asset from those repositories is included in this delivery. Before adapting third-party source, record the exact commit, original path, license, local path and modifications above.
