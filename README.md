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
- The mini-player's add-to-playlist picker uses large adaptive playlist cards matching the library grid for easier in-car operation.
- The home screen is reduced to category chips, 12 local quick-access items, account playlists, and at most three official YouTube recommendation sections. Expensive duplicate discovery, community, similar-content, mood-and-genre, random-order, and infinite-pagination sections are not loaded, and quick-access items remain safe if a song is removed during synchronization.
- Branded the installed app as `Metrolist_AndroidCar` with a black music-car logo across the launcher, About screen, and playback notifications.
- New playlists default to YouTube Music sync when the account and sync setting are active. Failed playlist creation, song additions, and song removals are stored outside the App database, retried automatically, and shown as pending in the playlist library; duplicate-song removals preserve the exact YouTube occurrence, and new remote playlists receive a reconciliation grace period.
- Song like and unlike actions use one ordered, durable synchronization queue. Rapid opposite actions keep the latest state, failed YouTube updates remain pending across App restarts, and duplicate network requests are avoided.
- Automatic full sync starts its cooldown only after every required component and pending playlist edit succeeds. Partial failures remain visible and can be retried immediately.
- Library pull-to-refresh joins an already queued or running full sync, ignores repeated pull gestures, keeps the indicator active until the shared operation finishes, and reports partial failures instead of running duplicate back-to-back syncs.
- Rapid queue or radio selections keep only the latest request, preventing slower network responses from replacing the active playback queue.
- Podcast, UGC, and unknown media types validate WEB_REMIX streams before playback and automatically exclude a failed WEB_REMIX source when resolving a fallback client.
- Backup, restore, CSV preview, and M3U import file work runs in the background. File previews stream or cap input to avoid loading large documents into memory, and newer selections cancel stale preview work. Restores validate staged database and settings files before replacing live data, roll back all replacements on failure, and restart into a usable database state when required.
- Foreground-service startup handling and widget themes are compatible with Android 8.0 (API 26) through current Android releases.
- Wrapped safely supports listening histories with fewer than five eligible top songs.
- Automatic queue continuation retries temporary failures with bounded backoff, waits for network recovery without duplicate requests, and offers a manual retry after the final failure. When a YouTube continuation ends, enabled similar content starts a new radio from the current tail song so playback can keep extending beyond the first 99 tracks without duplicating queued songs. End-of-queue checks follow the actual shuffled playback order, and YouTube continuation state survives an App service restart.
- Parallel downloads share a thread-safe URL cache, preventing duplicate resolution and cache races.
- Security hardening limits cleartext Listen Together connections to local-network servers, isolates private widget actions from exported update receivers, and grants custom media commands only to trusted controllers.

## Features

- Independent volume control. Unlike standard YouTube Music which only follows system volume, this app allows separate music volume adjustment to reduce interference with navigation guidance. Music volume now reliably returns to its configured level after navigation guidance ducks or temporarily pauses playback.
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
- Car-focused defaults with large grids and a minimal set of generated playlists.

## Build and updates

Build the FOSS release variant locally with:

```bash
./gradlew :app:assembleFossRelease
```

The manually triggered GitHub Actions workflow builds only the FOSS release APK and publishes it to this repository's GitHub Releases. Release notes are generated automatically from the actual commits since the previous release and are refreshed when an existing release is rerun. The workflow requires the fixed Android signing secrets `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD`; do not commit the keystore or passwords.

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
