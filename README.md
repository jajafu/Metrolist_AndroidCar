[English](README.md) | [繁體中文](README.zh-TW.md)

# Metrolist Android Car

Metrolist Android Car is a customized Android Car-oriented fork of [Metrolist](https://github.com/MetrolistGroup/Metrolist), an open-source YouTube Music client for Android.

This fork is maintained by [jajafu](https://github.com/jajafu) and focuses on a more readable and practical in-car playback experience.

## Current customizations

- Enlarged the playback island by 2× for better visibility.
- Fixed scrolling for the Theme and Color settings in landscape orientation.
- Increased dark-mode contrast by changing adjustment-button outlines to pure white.
- Increased the cached playback queue to three tracks.
- Removed the sleep button from the playback cover and enlarged the other buttons.

## Features

- Independent volume control. Unlike standard YouTube Music which only follows system volume, this app allows separate music volume adjustment to reduce interference with navigation guidance.
- Stream music and videos from YouTube Music.
- Background playback and offline downloads.
- Skip silence, sleep timer, audio normalization, tempo and pitch control.
- Synced lyrics and lyrics translation.
- Search for songs, albums, artists, videos and playlists.
- Library, local playlist and account synchronization.
- Listen together with other users.
- Material 3 interface with light, dark, black, dynamic and preset color themes.
- Android Auto-focused layout and playback controls.

## Screenshots

Screenshots are available in the repository's [`fastlane/metadata/android`](fastlane/metadata/android) directory.

## Build from source

1. Clone this repository, including its submodules:

   ```bash
   git clone --recurse-submodules https://github.com/jajafu/Metrolist_AndroidCar.git
   ```

2. Build the FOSS debug variant:

   ```bash
   ./gradlew :app:assembleFossDebug
   ```

The generated APK is `app/build/outputs/apk/foss/debug/app-foss-debug.apk`.

The GitHub Actions workflow is run manually and builds and lints only this FOSS debug variant. A successful manual run from the main branch uploads the APK as a workflow artifact and publishes it to a versioned GitHub Release. The workflow does not run automatically on pushes or pull requests, does not build GMS, Izzy or release variants, and does not require release-signing secrets. CI uses a temporary debug key, so an APK from a newer run may require uninstalling an APK installed from an older run.

## Original project and acknowledgements

This project is a modified version of [Metrolist](https://github.com/MetrolistGroup/Metrolist). The original authors, contributors and copyright notices remain acknowledged in the source tree and [`LICENSE`](LICENSE).

Metrolist also builds on work from projects including [InnerTune](https://github.com/z-huang/InnerTune), [OuterTune](https://github.com/DD3Boh/OuterTune), [Better Lyrics](https://better-lyrics.boidu.dev), [metroserver](https://github.com/MetrolistGroup/metroserver), [MusicRecognizer](https://github.com/aleksey-saenko/MusicRecognizer), and [zemer-cipher](https://github.com/ZemerTeam/zemer-cipher).

## GPLv3 notices for modified distributions

This project is licensed under the [GNU General Public License v3.0](LICENSE).

When distributing this modified project or an APK based on it:

- Keep the original copyright, attribution, license and disclaimer notices.
- Clearly identify that this is a modified version and describe the changes.
- Provide the corresponding source code and the scripts or instructions needed to build the distributed version.
- Distribute covered derivative works under GPLv3 and do not add restrictions that conflict with the license.
- Include a copy of the GPLv3 license with the distribution.

Copyright in the original work remains with its original authors. Copyright in new contributions remains with their respective contributors.

## Disclaimer

This project is not affiliated with, funded, authorized, endorsed by, or associated with YouTube, Google LLC, Metrolist Group LLC, or their affiliates and subsidiaries.

All trademarks, service marks and other intellectual property referenced in this project belong to their respective owners.
