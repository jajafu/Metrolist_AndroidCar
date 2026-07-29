# Metrolist AndroidCar 變更日誌 / Changelog

本檔案記錄 `Metrolist_AndroidCar` 專案自 `13.6.0` 起的客製功能、修正與建置變更。上游 Metrolist 的同步內容未在此重複列出。

This file records project-specific features, fixes, and build changes in `Metrolist_AndroidCar` from `13.6.0` onward. Upstream Metrolist synchronization changes are not repeated here.

## 13.6.47

### 中文

- Android Auto 搜尋與語音播放改用 YouTube 搜尋摘要中的相關性歌曲排序，相同回應會穩定選擇相同的最佳歌曲。
- 線上結果會直接建立播放器項目，背景寫入 Room 不再立即查回，避免冷資料庫競態丟失歌曲；語音播放會建立可續頁的相關歌曲 Radio queue，沿用既有自動延伸與重試機制。

### English

- Use relevance-ranked songs from YouTube search summaries for Android Auto search and voice playback, making selection deterministic for the same response.
- Build playable items directly from online results instead of racing an asynchronous Room insert/read, and seed an extendable related-song radio queue that uses the existing continuation and retry system.

## 13.6.46

### 中文

- 連續使用「播放下一首」時改為依請求先後順序插入，不再讓較新的歌曲插到較舊請求前面。
- 開啟隨機播放時會將目前歌曲後的完整手動優先區塊排在自動歌曲前；切歌、重複播放、刪除、移動、換新或清空佇列後會同步縮減或重置追蹤狀態。

### English

- Keep repeated Play Next requests in first-in-first-out insertion order instead of placing newer requests before earlier ones.
- Keep the complete manual-priority block ahead of automatic songs during shuffle, reconciling or resetting its state after transitions, repeats, removals, moves, queue replacement, and clearing.

## 13.6.45

### 中文

- Coil 解碼或載入的封面會先複製成獨立、不可變的 ARGB_8888 軟體 Bitmap，再交給 Media3 使用。
- 來源圖片已回收或複製失敗時改用安全的小型替代圖，避免 Android 15 在通知、鎖定畫面或車機媒體控制縮放封面時崩潰。

### English

- Copy artwork decoded or loaded by Coil into an independently owned, immutable ARGB_8888 software bitmap before handing it to Media3.
- Return a small safe fallback when the source is recycled or copying fails, preventing Android 15 crashes while notifications, lock-screen metadata, or car controls scale artwork.

## 13.6.44

### 中文

- 遠端喜歡歌曲清單核對時，會保留尚未成功送出的最新本機按讚或取消按讚，避免離線操作被遠端舊狀態覆蓋。
- 沒有待處理本機操作時仍接受遠端取消按讚；裝置本機歌曲不會送往 YouTube，待處理按讚尚未清空時也不會把完整同步誤記為成功。

### English

- Preserve the latest pending local like or unlike while reconciling the remote liked-songs playlist so stale remote state cannot overwrite offline actions.
- Continue accepting remote unlikes when no local action is pending, never send device-local songs to YouTube, and do not mark a full sync successful while song-like updates remain pending.

## 13.6.43

### 中文

- 切換歌曲時會取消仍在等待的即時靜音跳轉、清除服務狀態並重置目前播放器的靜音偵測器。
- 延遲跳轉及每次連續跳轉前都會核對播放器、歌曲 ID 與佇列索引，避免上一首的靜音工作跳轉下一首。

### English

- Cancel pending instant-silence seek work, clear service state, and reset the active player's silence detector whenever the track changes.
- Verify the player, media ID, and queue index after the debounce and before every follow-up seek so stale work from the previous track cannot move the next one.

## 13.6.42

### 中文

- 遠端播放列表同步會先依 browse ID 去重，並在同一輪同步中記住新建立的本機項目，避免產生重複播放列表。
- 清理本機重複播放列表時改用輕量的播放列表資料；待處理編輯、正在修改與寬限期內項目不會被刪除，其他重複項目的已下載歌曲會先合併至保留項目。

### English

- Deduplicate remote playlists by browse ID and track newly inserted local records during the same sync pass to prevent duplicate playlist creation.
- Use lightweight playlist entities for local duplicate cleanup; preserve pending, actively modified, and grace-period records, and merge downloaded songs before removing other duplicates.

## 13.6.41

### 中文

- 歌曲批次讀取與播放列表重複歌曲檢查改為每 900 個 ID 分批查詢，避免大型播放列表超過 SQLite 綁定參數上限而崩潰。
- 空清單不再送入 Room 查詢；重複 ID 只回傳一次，跨批次結果會依輸入 ID 第一次出現的順序排列。

### English

- Split bulk song reads and playlist duplicate checks into queries of 900 IDs to prevent large playlists from exceeding SQLite bind-variable limits.
- Empty lists bypass Room queries, duplicate IDs produce one result, and combined results follow each input ID's first occurrence.

## 13.6.40

### 中文

- Podcast、UGC 與未知媒體型態不再略過 WEB_REMIX 串流驗證，驗證失敗時會繼續嘗試其他播放來源。
- WEB_REMIX 實際播放發生 `IO_UNSPECIFIED` 時，會在有限重試內排除該來源並重新解析；其他來源的相同錯誤不再無意義地重試同一網址。

### English

- Validate WEB_REMIX streams for podcast, UGC, and unknown media types, continuing through other playback sources when validation fails.
- When WEB_REMIX playback returns `IO_UNSPECIFIED`, exclude that source and resolve again within the existing retry limit; the same error from other clients no longer retries the same unsuitable URL.

## 13.6.39

### 中文

- 修正首頁快速存取歌曲在同步或刪除期間從資料庫消失時，畫面仍以非空值存取歌曲而造成的崩潰。
- 播放與歌曲選單會使用最新資料；資料列剛移除時則暫時使用畫面原有項目，直到首頁清單完成更新。

### English

- Fix a Home quick-access crash when a song disappears from the database during synchronization or deletion while the composed item still accesses it as non-null.
- Playback and song menus use the latest data, with the original displayed item as a temporary fallback until the Home list refreshes.

## 13.6.38

### 中文

- 音樂庫下拉重新整理現在會加入已排隊或執行中的完整同步，避免自動同步後立刻重複執行第二次完整同步。
- 重複下拉會共用同一個同步工作，旋轉指示會持續到實際工作完成；部分同步失敗時會顯示重試提示。

### English

- Make library pull-to-refresh join a queued or running full sync, preventing a second complete sync from running immediately after auto-sync.
- Repeated pulls now share one sync operation, keep the refresh indicator active until the actual work finishes, and show a retry message after partial failure.

## 13.6.37

### 中文

- 將車機首頁精簡為分類按鈕、12 個本機快速存取項目、帳號播放列表及最多 3 個 YouTube 官方推薦區塊。
- 停止載入重複且耗費資源的每日探索、社區歌單、自訂相似推薦、情境與類型、隨機首頁排序及無限分頁，並移除對應的內容設定選項。

### English

- Streamline the car home screen to category chips, 12 local quick-access items, account playlists, and at most three official YouTube recommendation sections.
- Stop loading duplicate and resource-intensive daily discovery, community playlist, custom similar recommendation, mood-and-genre, randomized home ordering, and infinite pagination sections, and remove the related content settings.

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
