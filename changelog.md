# Metrolist AndroidCar 變更日誌 / Changelog

本檔案記錄 `Metrolist_AndroidCar` 專案自 `13.6.0` 起的客製功能、修正與建置變更。上游 Metrolist 的同步內容未在此重複列出。

This file records project-specific features, fixes, and build changes in `Metrolist_AndroidCar` from `13.6.0` onward. Upstream Metrolist synchronization changes are not repeated here.

## 13.6.36

### 中文

- 將 App、播放器服務、一起聆聽、歌詞與 ViewModel 的 DataStore 設定讀取改為非同步讀取或記憶體快取，避免同步磁碟存取阻塞主執行緒。
- 播放佇列、電台、自動混音與 Podcast 播放位置的協程失敗現在會記錄具體操作來源與完整錯誤，不再靜默吞掉例外。

### English

- Move DataStore preference reads in the app, playback service, Listen Together, lyrics, and ViewModels to asynchronous reads or in-memory snapshots to prevent synchronous disk access from blocking the main thread.
- Record the specific operation and full error when queue, radio, automix, or podcast-position coroutines fail instead of silently swallowing exceptions.

## 13.6.35

### 中文

- Return YouTube Dislike 服務無法使用或回傳異常時，仍會保留 YouTube 已成功取得的歌曲媒體資訊，只有觀看、按讚與倒讚數暫時留空。

### English

- Preserve successfully retrieved YouTube media details when Return YouTube Dislike is unavailable or returns an invalid response; only view, like, and dislike counts remain unavailable.

## 13.6.34

### 中文

- 將 App 執行時的播放器設定與日期更新來源改為本專案的 GitHub 儲存庫，降低原始 Metrolist 遠端檔案遺失所造成的風險。

### English

- Point runtime player configuration and date updates to this project's GitHub repository, reducing reliance on the original Metrolist remote files.

## 13.6.33

### 中文

- 關閉串流網址驗證使用的 HTTP response，避免資源未釋放與連線累積。

### English

- Close HTTP responses used for stream URL validation to prevent leaked resources and accumulating connections.

## 13.6.32

### 中文

- 將備份檔案預覽與串流驗證移至背景執行緒，避免大型檔案操作阻塞介面。

### English

- Move backup file preview and streaming validation work off the main thread so large files do not block the UI.

## 13.6.31

### 中文

- 將喜歡歌曲的同步更新排入持久化、依序處理的佇列，避免快速操作時遺失或覆蓋更新。

### English

- Serialize durable liked-song synchronization updates so rapid actions do not lose or overwrite changes.

## 13.6.30

### 中文

- 服務重新啟動後恢復可延伸的 YouTube 播放佇列，讓自動載入更多歌曲可以繼續運作。

### English

- Restore extendable YouTube playback queues after service restarts so automatic loading can continue.

## 13.6.29

### 中文

- 修正隨機播放接近佇列尾端時的判斷，改為依照實際隨機播放順序載入後續歌曲。

### English

- Fix queue-end detection during shuffle playback by following the actual shuffled order when loading more songs.

## 13.6.28

### 中文

- 修正電台分頁結束後無法繼續產生推薦歌曲的問題。

### English

- Fix radio playback stopping when the current continuation page is exhausted.

## 13.6.27

### 中文

- 新增適合車機操作的播放清單格狀選擇器，放大項目與操作區域。
- CI 增加 lint 錯誤回歸檢查。
- 限制外部控制入口，避免未授權的控制路徑被使用。
- 修正下載網址快取的執行緒安全問題。
- 自動載入更多歌曲失敗時加入重試機制。
- 修正小型歌曲資料庫的播放處理、Android 版本相容性例外、備份替換復原，以及過期播放佇列請求。
- 修正音訊 ducking 後音量未恢復的問題。
- 修正同步完成狀態，使背景同步結果能被正確記錄。

### English

- Add a car-friendly grid playlist picker with larger items and touch targets.
- Add a lint-error regression gate to CI.
- Restrict external control entry points to prevent unauthorized control paths.
- Fix thread safety in the download URL cache.
- Retry failed automatic load-more requests.
- Fix playback for small song libraries, Android-version-specific service exceptions, recoverable backup replacement, and stale queue requests.
- Restore volume correctly after audio-focus ducking.
- Record background synchronization completion accurately.

## 13.6.26

### 中文

- CI 增加 lint 錯誤回歸檢查，防止新的編譯或靜態分析錯誤被忽略。

### English

- Add a lint-error regression gate to CI so new build or static-analysis errors are not missed.

## 13.6.25

### 中文

- 限制外部控制入口，改善應用程式的安全性。

### English

- Restrict external control entry points to improve application security.

## 13.6.24

### 中文

- 修正下載網址快取的執行緒安全問題，降低並行下載時的錯誤風險。

### English

- Fix thread safety in the download URL cache to reduce failures during concurrent downloads.

## 13.6.23

### 中文

- 自動載入更多歌曲失敗時加入重試機制，改善播放清單接近尾端時的連續播放。

### English

- Retry failed automatic load-more requests to improve continuous playback near the end of a playlist.

## 13.6.22

### 中文

- 修正小型歌曲資料庫的播放處理，避免歌曲數量較少時的錯誤。

### English

- Fix playback handling for small song libraries and avoid errors when only a few songs are available.

## 13.6.21

### 中文

- 增加 Android 特定版本服務例外的防護，改善背景播放穩定性。

### English

- Guard against Android-version-specific service exceptions to improve background playback stability.

## 13.6.20

### 中文

- 讓備份還原的檔案替換流程可復原，降低替換中斷造成資料無法使用的風險。

### English

- Make backup replacement during restore recoverable, reducing the risk of unusable data after an interrupted replacement.

## 13.6.19

### 中文

- 防止過期的播放佇列請求覆蓋較新的播放狀態。

### English

- Prevent stale queue requests from overwriting newer playback state.

## 13.6.18

### 中文

- 修正導航或其他音訊焦點事件暫時降低音量後，播放音量沒有恢復的問題。

### English

- Fix playback volume not being restored after navigation or other audio-focus ducking events.

## 13.6.17

### 中文

- 修正同步完成狀態的記錄，讓背景同步結果能被正確判定。

### English

- Fix synchronization completion tracking so background sync results are reported accurately.

## 13.6.16

### 中文

- 持久化播放清單歌曲移除操作，確保登出、重新登入或同步延遲後仍能套用刪除結果。

### English

- Persist playlist song removals so deletions are retained across logout, login, or delayed synchronization.

## 13.6.15

### 中文

- 持久化尚未送出的播放清單編輯，避免網路或背景同步中斷時遺失變更。

### English

- Persist pending playlist edits so changes are not lost when network or background synchronization is interrupted.

## 13.6.14

### 中文

- 改善 YouTube 播放清單同步可靠性，修正手機端與 YouTube 端更新延遲或不一致的情況。
- 更新專案文件與代理規範，要求 Release notes 依實際變更完整填寫。
- 移除已停用的 Download 收藏歌曲 JSON 備份說明與相關流程。
- 穩定自動電台佇列的延續播放。

### English

- Improve YouTube playlist synchronization reliability and fix delayed or inconsistent updates between the app and YouTube.
- Update project documentation and agent rules to require complete release notes based on actual changes.
- Remove the obsolete Download-folder liked-song JSON backup documentation and workflow.
- Stabilize automatic radio queue continuation.

## 13.6.13

### 中文

- 穩定自動電台佇列延續播放，改善播放清單播到尾端後沒有新歌曲的情況。

### English

- Stabilize automatic radio queue continuation when playback reaches the end of the current list.

## 13.6.12

### 中文

- 修正應用程式標籤與關於頁顯示不一致，確保品牌名稱顯示為 `Metrolist_AndroidCar`。

### English

- Fix inconsistent application labels and About-screen branding so the project name is shown as `Metrolist_AndroidCar`.

## 13.6.11

### 中文

- 將新的黑色專案標誌套用到應用程式、通知與待機播放控制等顯示位置。
- 修正收藏歌曲檔案重複建立的問題。

### English

- Apply the new black project logo across the app, notifications, and playback controls shown while idle.
- Prevent duplicate liked-song files from being created.

## 13.6.10

### 中文

- 修正收藏歌曲 JSON 檔案因同名檔案而不斷產生副本的問題。

### English

- Fix liked-song JSON exports repeatedly creating duplicate files when a file with the same name already exists.

## 13.6.9

### 中文

- 更新應用程式圖示與通知圖示，改用 AndroidCar 專案品牌圖案。

### English

- Update the application and notification icons with the AndroidCar project branding.

## 13.6.8

### 中文

- 在關於頁新增專案維護者 `jajafu` 與 GitHub 連結。
- 調整羅馬化、外觀，以及播放與音訊設定的預設值，使車機使用更簡潔。

### English

- Add project maintainer `jajafu` and a GitHub link to the About screen.
- Adjust default Romanization, appearance, and playback/audio settings for a simpler car-focused experience.

## 13.6.7

### 中文

- 調整預設設定：關閉羅馬化內容、頂部欄「一起聆聽」與不必要的自動播放清單選項；網格大小預設為大，並保留喜歡歌曲與已下載歌曲的自動播放清單。

### English

- Adjust defaults: disable Romanization content, top-bar Listen Together, and unnecessary automatic playlists; use a large grid by default while keeping liked and downloaded songs enabled.

## 13.6.6

### 中文

- 修正 YouTube 縮圖尺寸處理，恢復正常的圖片載入與顯示。

### English

- Fix YouTube thumbnail resizing and restore correct image loading and display.

## 13.6.5

### 中文

- 更新程式內更新檢查的版本比較方式，正確處理多位數版本號，避免錯誤判斷更新狀態。

### English

- Use semantic version comparison for update checks so multi-digit version numbers do not produce incorrect update states.

## 13.6.4

### 中文

- 新增使用固定 Android 簽章金鑰的 Foss Release 更新流程。
- 支援從 GitHub Release 取得並套用簽章一致的更新，改善重新安裝時的資料保留。

### English

- Add a signed Foss Release update flow using a persistent Android signing key.
- Support updates from GitHub Releases with consistent signing so reinstalling can preserve app data.

## 13.6.3

### 中文

- 將 Material 3 的棄用 `rememberModalBottomSheetState` 遷移至新版 bottom sheet API，保留隱藏與半展開行為。
- GitHub Actions 改為僅建置 Foss 版本，並維持手動執行流程。

### English

- Migrate the deprecated Material 3 `rememberModalBottomSheetState` calls to the new bottom-sheet API while preserving hidden and half-expanded behavior.
- Configure GitHub Actions to build only the Foss variant and remain manually triggered.

## 13.6.2

### 中文

- 更新 Room 破壞性遷移 fallback，明確使用 `dropAllTables` 參數以符合 Room 2.7 新版 API。

### English

- Update the Room destructive-migration fallback to use the explicit `dropAllTables` parameter required by the Room 2.7 API.

## 13.6.1

### 中文

- 修正 MusicService 與安全相關的 Kotlin 編譯警告，降低背景播放與外部控制的風險。

### English

- Resolve Kotlin compiler warnings in MusicService and security-related code, reducing risks around background playback and external control.

## 13.6.0

### 中文

- 建立 Metrolist AndroidCar 客製版本，加入車機導向的播放與橫向設定介面。
- 建立中英文雙語專案文件與客製化品牌資訊。
- 將 GitHub Actions 簡化為手動執行的 Foss APK 建置與 Release 流程。

### English

- Establish the customized Metrolist AndroidCar build with car-focused playback and landscape settings UI.
- Add bilingual project documentation and customized branding information.
- Simplify GitHub Actions to a manually triggered Foss APK build and release flow.
