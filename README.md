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
- Stored favorite music records in the Download directory for direct access after reinstall, no import/export needed.

## Features

- Independent volume control. Unlike standard YouTube Music which only follows system volume, this app allows separate music volume adjustment to reduce interference with navigation guidance.
- Stream music from YouTube Music.
- Background playback and offline downloads.
- Skip silence, sleep timer, audio normalization, tempo and pitch control.
- Synced lyrics and lyrics translation.
- Search for songs, albums, artists and playlists.
- Library, local playlist and account synchronization.
- Listen together with other users.
- Material 3 interface with light, dark, black, dynamic and preset color themes.
- Android Auto-focused layout and playback controls.
- High-resolution image URL handling for YouTube's current image CDN formats.

## Build and updates

Build the FOSS release variant locally with:

```bash
./gradlew :app:assembleFossRelease
```

The manually triggered GitHub Actions workflow builds only the FOSS release APK and publishes it to this repository's GitHub Releases. It requires the fixed Android signing secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`; do not commit the keystore or passwords.

The in-app updater checks [this repository's releases](https://github.com/jajafu/Metrolist_AndroidCar/releases) and opens the matching APK download for confirmation. Android still requires the user to approve installation.

Release names may include the `-car` suffix; the updater compares their numeric version components with the installed app version, so the current release is not reported as a new update.

The first installation from an earlier Debug APK requires uninstalling that Debug package because it uses a different application ID. Subsequent FOSS Release APKs use the same signing key and can update in place.

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
