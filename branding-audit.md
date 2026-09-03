# MusicCabin branding audit

Version: 13.6.75 (versionCode 224).

## Updated surfaces

| Surface | Result |
| --- | --- |
| GitHub repository and origin | `jajafu/MusicCabin`; repository description identifies MusicCabin and acknowledges Metrolist |
| App, launcher aliases, system notification app label | One non-translatable `app_name` resource: `MusicCabin` |
| About | MusicCabin title, project repository and license links; upstream developer and community links explicitly identified |
| Widgets, recognition actions, Wrapped, Last.fm and Discord information | New default English brand resources prevent inherited translations from restoring the old brand |
| Launcher search/library shortcuts | Target the existing release/debug application IDs |
| Updater | MusicCabin Releases page and API; new and historical APK names accepted |
| Player configuration and dates | Both Base64-encoded GitHub raw URLs now target MusicCabin |
| OpenRouter and Discord defaults | MusicCabin client title, project link and default activity button |
| New exports | `MusicCabinExports`, `Pictures/MusicCabin`, `musiccabin_crash_…`; FileProvider accepts both export directories |
| Images | Existing music-car mark across adaptive, static and legacy launcher icons, notification icon, About and store artwork; both Wrapped covers renamed |
| Documentation and metadata | Both READMEs, developer guide, security policy, agent guide, changelog heading, issue templates, Gradle project name and store titles |
| GitHub Releases | 13 historical release titles and descriptions updated; version-specific notes preserved |
| Release workflow | Manual FOSS Release only; `MusicCabin-v<version>-car.apk`, MusicCabin title and version-specific changelog notes |
| Other workflows | Player configuration sync changed to manual-only to match the project rules |

## Intentionally retained identifiers and history

- `com.jajafu.metrolist.androidcar` and its debug suffix remain unchanged. There is no database schema change or migration.
- Kotlin namespaces, component names, intent actions, theme names, preference keys, encryption key aliases and `METROLIST_*` build overrides remain technical compatibility identifiers.
- Original Metrolist copyrights, authorship, licenses, upstream dependency coordinates, the metroproto submodule and contributor links retain their real names and URLs.
- `metrolist.cc` Listen Together links and `metrolistdiscord` OAuth callbacks belong to existing upstream services. The external Discord OAuth application remains the upstream registration; changing that registration requires its owner's account or a separately provisioned application.
- Translated `strings.xml` and `metrolist_strings.xml` files are untouched. Only the default English `app/src/main/res/values/metrolist_strings.xml` was edited; unused legacy translation keys and lint-baseline snapshots may still contain the old name.
- Historical changelog entries, release tags and published historical APK filenames retain their original meaning. Only new APKs contain the MusicCabin app branding. Existing user-created playlists, exports and saved files are not renamed.
- The local checkout directory name is independent of the GitHub repository name.

## Image edits

The existing music-car assets were reused without redesign. Both Wrapped covers were edited with the built-in ImageGen tool and saved in `app/src/main/res/drawable/wrapped_playlistv1.png` and `wrapped_playlistv2.png`.

Prompt used for each cover:

> Use case: text-localization. Asset type: square Android music app Wrapped playlist cover. Edit the provided target image. Replace BOTH occurrences of the old brand METROLIST (large title and tiny bottom-right signature) with exact mixed-case text MusicCabin (M-u-s-i-c-C-a-b-i-n). Keep WRAPPED and 2025 unchanged. Preserve original square composition, monochrome palette, flat background, geometric border marks, glitch styling, dashed circle, tiny binary decorations and clear margins. Keep all text fully inside the canvas. Match the bold angular pixel typography while making MusicCabin easy to read. Change only the brand lettering; no extra words or logo, no gradients, no mockup.

## Verification

- Resource reference audit: active old-brand text is limited to explicit upstream credits.
- Reviewed launcher, store and Wrapped images and the six existing store screenshots for old brand lettering.
- FOSS Debug assembly and both `UpdaterBrandingTest` cases passed. The tests cover new/historical APK selection and numeric version comparison with branded release titles.
- APK manifest inspection confirms version 13.6.75 / 224, the unchanged debug application ID and MusicCabin labels for every packaged locale. The Releases API and both raw configuration endpoints return HTTP 200 and valid JSON.
- Device checks remain for launcher aliases/shortcuts, About, a playback notification and the updater download action; the available emulators were offline during this audit.
