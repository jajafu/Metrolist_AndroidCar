# Photo Frame 功能實作計畫

最後查證日期：2026-08-25

## 1. 文件目的

本文件是 Metrolist_AndroidCar「數位相框」功能的共同執行規格，供多個不同 Agent 分階段實作、審查與整合。

所有 Agent 開始工作前必須先閱讀：

- 根目錄 `AGENTS.md`
- 本文件 `photo_plan.md`
- 該階段會修改的既有程式碼

本文件定義的需求、Google API 限制、資料安全規則與驗收條件，不得由個別 Agent 自行放寬。若發現 API、政策或專案架構與本文件不一致，應停止該部分實作並回報，不得以未公開 API、網頁爬蟲或繞過 OAuth 的方式完成。

## 2. 已確認的產品需求

### 2.1 功能範圍

- 在設定頁新增「數位相框」設定入口。
- 支援「本機照片」與「Google 相簿」兩種照片來源。
- 可選擇只播放本機照片、只播放 Google 相簿照片，或混合兩種來源。
- 在完整音樂播放介面新增數位相框按鈕。
- 點擊按鈕後進入全螢幕沉浸式照片輪播，音樂持續播放。
- 照片以隨機順序播放，提供交叉淡入淡出效果。
- 畫面可選擇顯示時鐘、目前歌曲名稱與歌手。
- 使用者可設定換片間隔、縮放方式與照片來源。
- 使用者可明確退出相框模式、清除 Google 照片副本、移除本機照片選取內容及解除 Google 授權。

### 2.2 MVP 不包含

- 不實作 Android `DreamService`，本功能不是系統螢幕保護程式。
- 不在 Android Auto 投影介面或駕駛最佳化介面顯示照片。
- 不自動讀取使用者完整 Google 相簿庫。
- 不使用 Google Photos Library API 的舊版完整唯讀 scope。
- 不使用 Google Photos Ambient API，除非未來正式加入 Google Photos Partner Program。
- 不播放 Google Photos 影片；第一版只處理 `image/*`。Motion Photo 只使用靜態圖片部分。
- 不做臉孔辨識、地點分析、人物分類或 AI 訓練。
- 不新增或修改 App 的 Room 資料庫結構。
- 不新增後端伺服器、Firebase、Cloud Storage 或 CDN。

## 3. Google API 可行性與收費結論

### 3.1 收費結論

截至 2026-08-25，Google 官方 Google Photos API 文件對 Picker API 公布的是「配額限制」，沒有公布按請求、按照片或按流量計費的價格，也沒有要求在啟用 Photos Picker API 時綁定付費帳單。Google OAuth 2.0 建立 client ID、取得 token 與一般 OAuth 驗證流程也沒有按登入次數計費。

因此，本計畫使用的以下項目目前可視為沒有直接 Google API 使用費：

- 建立 Google Cloud 專案。
- 建立 OAuth 2.0 client ID。
- 使用 Google OAuth 2.0 登入及授權。
- 呼叫 Google Photos Picker API 建立、查詢與刪除 session。
- 列出使用者在 Picker 中明確選取的媒體。
- 在官方配額內下載選取照片的媒體 bytes。

這個結論代表「官方目前沒有列出可計費 SKU」，不代表 Google 永久保證免費。每次正式開發或發行前，負責 Agent 都應重新檢查官方配額與政策頁。

### 3.2 不屬於 Google API 費用，但可能產生成本的項目

- 使用者下載照片會消耗裝置網路流量與本機儲存空間。
- 若未來加入自有後端、Cloud Run、Cloud Storage、Firebase 或 CDN，相關雲端服務可能計費；本計畫明確不使用這些服務。
- 公開 App 的 OAuth verification 本身未在官方文件中列出審查費用，但若所用 scope 在 Google Cloud Console 被分類為 restricted scope，可能需要第三方安全評估，該評估可能產生成本。
- 若未來申請 Partner Program、Ambient API 或額外配額，條件與可能費用需另行向 Google 確認。
- Picker 方案只讀取並下載使用者已存在的照片，不會上傳照片回 Google Photos，因此不會因本功能增加使用者的 Google Photos 儲存用量。

### 3.3 Picker API 官方配額

目前官方配額如下：

| 類型 | 配額 |
|---|---:|
| Photos Picker API 一般請求 | 每個專案每分鐘 100,000 次 |
| 從 `baseUrl` 讀取媒體 bytes | 每個專案每分鐘 1,000,000 次 |

超過配額時 API 會回傳 HTTP 429。配額不是自動計費型額度，不得以高頻重試消耗 API；必須遵守伺服器提供的 polling interval，並對 429/5xx 使用 exponential backoff。

### 3.4 重要政策與技術限制

- 2025-03-31 後，第三方 App 不能再以舊版 Library API 讀取不是由該 App 建立的完整相簿庫。
- Picker API 只能取得使用者在 Google 提供的 Picker 畫面中明確選取的照片或影片。
- Picker session 有生命週期；使用者完成選取後，App 應盡快列出並取得所需媒體，再刪除 session。
- 每個 Picker session 最多可讓使用者選取 2,000 個項目；MVP 產品上限建議先設為 500 張，避免裝置儲存空間與匯入時間失控。
- `baseUrl` 最長只有效 60 分鐘，使用者撤銷權限時可能更早失效。
- 下載 `baseUrl` 必須附上仍有效的 OAuth bearer token。
- Google Photos API 不支援 service account，必須由實際 Google 使用者完成 OAuth 授權。
- OAuth consent screen 為 External 且 publishing status 為 Testing 時，非基本 scope 的 refresh token 通常 7 天到期。Testing 模式只能用於開發驗證，不適合作為長期正式方案。
- 公開發行前需要完成 Google OAuth verification；需準備公開首頁、隱私權政策、已驗證網域、應用內資料使用說明及完整功能示範影片。
- Google Cloud Console 會標示 Picker scope 屬於 non-sensitive、sensitive 或 restricted；實作 Agent 不得自行猜測分類。
- Google Photos 政策只允許適當且具明確使用者效益的用途，且禁止製作通用 Google Photos 替代品。
- Google 對「智慧電視／相框沉浸式輪播」另提供 Ambient API，且 Ambient API 只開放給 Partner Program 成員。因本功能與 Ambient use case 接近，Picker API 能否以此用途通過正式 OAuth／產品政策審查不得視為已保證。

### 3.5 Google 整合發布閘門

Google 來源功能必須先完成 Gate 0，才可投入完整產品化實作：

- 使用正式預計採用的 package name、SHA-1 與 OAuth client 類型完成授權測試。
- 在 `fossDebug` 裝置上證明不依賴 Google Play Services 也可完成 OAuth。
- 建立 Picker session，選取少量測試照片，列出項目並成功下載圖片。
- 確認 access token refresh、登出、撤銷授權及 7 天 Testing token 到期行為。
- 在 Google Cloud Console 確認 scope 分類並留下截圖或文字紀錄。
- 確認公開發布所需的 OAuth verification 材料與政策適用性。
- 若 Google 明確判定數位相框用途必須使用 Ambient API，Google 來源不得公開發布；本機照片來源與相框 UI 仍可獨立發布。

## 4. 官方參考資料

實作或審查時優先以以下官方文件為準：

- Google Photos API 概觀：<https://developers.google.com/photos/overview/about>
- Picker API 流程：<https://developers.google.com/photos/picker/guides/get-started-picker>
- Session 管理：<https://developers.google.com/photos/picker/guides/sessions>
- 媒體取得與 60 分鐘限制：<https://developers.google.com/photos/picker/guides/media-items>
- OAuth scope：<https://developers.google.com/photos/overview/authorization>
- API 配額：<https://developers.google.com/photos/overview/api-limits-quotas>
- App 設定：<https://developers.google.com/photos/overview/configure-your-app>
- Photos API 政策：<https://developers.google.com/photos/support/api-policy>
- Partner Program 與 Ambient API：<https://developers.google.com/photos/partner-program/overview>
- Google OAuth 2.0：<https://developers.google.com/identity/protocols/oauth2>
- OAuth verification：<https://support.google.com/cloud/answer/9110914>

## 5. 整體架構

```text
SettingsScreen
    -> PhotoFrameSettingsScreen
        -> 本機照片選取
        -> Google OAuth
        -> Google Photos Picker session
        -> 照片池、顯示及快取設定

Player.BottomSheetPlayer
    -> PhotoFrame 啟動按鈕
    -> PhotoFrameOverlay
        -> PhotoFrameViewModel / Controller
            -> PhotoCatalog
                -> LocalPhotoSource
                -> GooglePhotosSource
            -> PhotoFrameStorage
            -> PhotoFrameCache
```

### 5.1 模組邊界

建議新增以下 package：

```text
app/src/main/kotlin/com/metrolist/music/photo/
app/src/main/kotlin/com/metrolist/music/photo/google/
app/src/main/kotlin/com/metrolist/music/ui/player/PhotoFrameOverlay.kt
app/src/main/kotlin/com/metrolist/music/ui/screens/settings/PhotoFrameSettings.kt
app/src/main/kotlin/com/metrolist/music/viewmodels/PhotoFrameViewModel.kt
```

可依實際程式碼規模合併小型檔案，避免過度拆分，但下列責任不得混在 Compose UI：

- OAuth token 管理。
- Picker REST 呼叫。
- Google 媒體下載。
- 照片池 manifest 持久化。
- 快取與刪除策略。
- 隨機播放與下一張排程。

### 5.2 共用模型

共用模型至少需要表達：

```kotlin
enum class PhotoFrameSourceType {
    LOCAL,
    GOOGLE_PHOTOS,
}

data class FramePhoto(
    val stableId: String,
    val sourceType: PhotoFrameSourceType,
    val uri: String,
)
```

不得把短效 Google `baseUrl` 當成長期 `FramePhoto.uri`。Google 項目完成匯入後，`uri` 應指向 App 私有儲存中的本機檔案。

### 5.3 不修改資料庫

此功能不得新增 Room entity、DAO、migration 或 schema。持久化方式如下：

- 簡單布林值、數字、enum 使用既有 DataStore preference 架構。
- 本機 URI 清單與 Google 匯入檔案索引使用 App 私有目錄中的版本化 JSON manifest。
- manifest 寫入必須採 temporary file + atomic rename，避免程序中斷造成檔案毀損。
- manifest 格式必須包含 `schemaVersion`，但不是 App 資料庫 schema。

## 6. 照片來源設計

### 6.1 本機照片來源

MVP 優先使用 Android System Photo Picker 多選：

- 不要求 `READ_MEDIA_IMAGES` 或舊版儲存空間權限。
- 只取得使用者明確選擇的照片。
- 對返回 URI 呼叫 `takePersistableUriPermission`；若供應者不支援，必須偵測並提示使用者重新選取。
- 只接受圖片 MIME type。
- manifest 只保存 URI 與 stable ID，不複製本機照片。
- 使用者可新增、移除或清空已選照片。
- URI 失效時跳過該照片，設定頁顯示失效數量，不得造成輪播崩潰。

第一版不自動掃描整個 MediaStore。若未來增加「掃描整個本機相簿」選項，必須另行評估權限、Android 版本差異與 Play 政策。

### 6.2 Google Photos Picker 來源

標準流程：

1. 確認存在可用 OAuth access token。
2. 沒有 token 時，在使用者點擊 Google 來源操作後才請求 scope。
3. 呼叫 `POST https://photospicker.googleapis.com/v1/sessions`。
4. 以 Custom Tab 或可驗證的外部瀏覽器開啟 `pickerUri`。
5. 依 API 回傳的 `pollInterval` 與 `timeoutIn` 查詢 session。
6. `mediaItemsSet == true` 後，以 `sessionId` 分頁列出所有項目。
7. 只保留 `image/*`。
8. 依裝置實際顯示需求下載縮放後圖片，不下載不必要的原始解析度。
9. 所有檔案成功寫入 staging 目錄後，原子更新 manifest。
10. 成功或取消後清理 session；錯誤時也要安排可重試的清理。

下載規則：

- `baseUrl` 請求必須附 bearer token。
- 使用 `=w<width>-h<height>` 取得顯示用尺寸，不使用 `=d` 下載完整原圖。
- 尺寸依裝置螢幕長邊計算並設合理上限，例如 2560 px，避免大型圖片造成記憶體與空間浪費。
- 限制同時下載數量，建議 2 至 4 個 coroutine，避免搶占音樂串流頻寬。
- 支援取消；取消後刪除未完成 staging 檔案。
- 匯入前檢查可用空間，空間不足時不得留下半套 manifest。
- 401 先嘗試一次 token refresh；`invalid_grant` 要求重新連結帳號。
- 429 與可重試 5xx 使用 bounded exponential backoff。
- 不記錄 access token、refresh token、完整 `baseUrl` 或照片內容到 log。

### 6.3 照片池與隨機播放

- 每輪使用 Fisher-Yates 或等價方式洗牌。
- 同一輪不重複照片。
- 新一輪第一張不得與上一輪最後一張相同，照片池只有一張時除外。
- 混合來源模式將兩個來源合併後再洗牌，不要求固定比例。
- 照片清單變動時建立新的 immutable snapshot，不在目前顯示途中修改集合。
- 照片讀取失敗時跳過並嘗試下一張；連續全部失敗時顯示可理解的空狀態並退出計時循環。

## 7. OAuth 與憑證規格

### 7.1 OAuth 實作方向

- 優先評估 AppAuth Android，以保持 foss flavor 不依賴 Google Play Services。
- 必須使用 Authorization Code Flow with PKCE。
- 不可在 App 中保存 OAuth client secret；原生 Android client 不應依賴可保密的 secret。
- 只請求 `https://www.googleapis.com/auth/photospicker.mediaitems.readonly` 與 OAuth 流程真正需要的最小 scope。
- Google Photos 授權與現有 YouTube Music cookie 登入完全分離，不能共用或推導 token。
- OAuth client ID 可公開，不視為 secret；keystore、client secret、token、Google 帳號與密碼不得提交。
- debug 與 release 的 package name／SHA-1 可能不同，需分別建立對應憑證。
- Picker 資源綁定建立它的 OAuth client ID；更換 client ID 會失去舊 session 資源的存取權。

AppAuth 與 Google Android OAuth 是否在所有目標車機正常運作，必須在 Gate 0 實機驗證。若 Custom Tab 不可用：

- 顯示明確錯誤與需求說明。
- 不准使用內嵌 WebView 擷取 Google 帳號憑證。
- 可保留本機照片功能，不得自行改用未核准 OAuth flow。

### 7.2 憑證注入

- client ID 應透過 Android resource、BuildConfig 或 Gradle property 注入。
- 沒有設定 client ID 時，所有 flavor 仍必須可編譯。
- client ID 缺失時只停用 Google 來源並顯示設定未完成；本機來源與播放器不得受影響。
- CI 不應需要 OAuth secret。
- 若正式 client ID 決定提交到 repository，需在文件中說明它是公開識別碼，不得同時提交任何 secret 或 token。

### 7.3 Token 儲存

- access／refresh token 必須使用 Android Keystore 保護的加密儲存。
- 不得以明文 DataStore、SharedPreferences、JSON 或 log 儲存 token。
- 登出時清除 token、AuthState 與未完成 session 資訊。
- 使用者解除 Google 連結時，應嘗試撤銷授權並清除本機 Google 照片副本。
- 所有 refresh 失敗都必須可恢復，不可導致 App 啟動崩潰。

## 8. 本機儲存與隱私

### 8.1 儲存位置

- Google 匯入照片存放於 App 私有且不參與備份的目錄，例如 `noBackupFilesDir/photo_frame/google/`。
- 不使用公用 Pictures、Downloads 或媒體庫。
- 本機照片來源只保存 URI，不複製檔案。
- Google manifest 不保存短效 `baseUrl` 或 OAuth token。
- Android 備份規則必須明確排除照片副本、manifest 中的敏感欄位與 OAuth 狀態。

### 8.2 空間限制

- MVP Picker API 上限建議設為 500 張，官方 session 上限仍為 2,000 張。
- 設定頁顯示照片數量與實際磁碟用量。
- 匯入前檢查可用空間並保留安全餘量。
- 不採用會靜默淘汰照片的 LRU，因 session 刪除後無法用舊 `baseUrl` 重新下載。
- 達到容量限制時停止匯入並提示減少選取數量或先清除舊照片。
- 新匯入採 staging + commit；只有完整成功後才替換舊照片池。

### 8.3 使用者控制

設定頁必須提供：

- Google 帳號連結／解除連結。
- 重新選取 Google Photos。
- 刪除所有 Google 照片本機副本。
- 新增／移除／清空本機照片。
- 顯示各來源的照片數量與儲存用量。
- 隱私說明：照片只在裝置上用於相框顯示，不上傳到 Metrolist 或其他伺服器。

在 OAuth consent 前必須顯示應用內 disclosure，說明：

- 會讀取哪些資料。
- 只會讀取使用者在 Picker 明確選取的照片。
- 照片會下載到此裝置的 App 私有空間以供離線輪播。
- 使用者如何刪除資料及撤銷授權。
- 資料不會用於廣告、販售、第三方分享或 AI 訓練。

## 9. 設定頁設計

### 9.1 導航位置

- 在 `app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SettingsScreen.kt` 新增入口。
- 在 `app/src/main/kotlin/com/metrolist/music/ui/screens/NavigationBuilder.kt` 註冊 `settings/photo_frame`。
- 新畫面沿用 `Material3SettingsGroup` 與既有設定頁視覺語言。

### 9.2 設定項目

| 設定 | 建議預設值 |
|---|---|
| 照片來源 | 本機；Google 未設定時不可選 |
| 本機／Google 混合 | 關閉 |
| 換片間隔 | 10 秒 |
| 轉場 | 交叉淡入淡出 |
| 照片縮放 | Crop / 填滿 |
| 顯示時鐘 | 開啟 |
| 顯示歌曲資訊 | 開啟 |
| 相框期間保持螢幕常亮 | 開啟 |

允許的換片間隔可先提供 5、10、15、30、60 秒，不需要任意毫秒輸入。

### 9.3 狀態與錯誤

- 未連結 Google 時顯示「未連結」。
- Testing token 或授權失效時顯示「需要重新連結」。
- 匯入時顯示可取消的進度，不阻塞整個設定頁。
- 無瀏覽器、無網路、使用者取消、session timeout、配額、低儲存空間都要有不同訊息。
- Google 功能因未設定 client ID 而停用時，顯示開發者設定缺失，不可假裝是網路錯誤。

## 10. 播放器與相框 UI

### 10.1 整合位置

- 主要入口為 `app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt` 的完整播放器動作區。
- `Player.kt` 已是大型檔案，只加入必要按鈕與 Overlay 掛載點。
- 相框畫面實作放在獨立 `PhotoFrameOverlay.kt`。
- 迷你播放器不增加相框按鈕。
- 沒有可用照片時按鈕可顯示但點擊後應導向說明／設定，不進入空白全螢幕。

### 10.2 全螢幕行為

- 相框 Overlay 覆蓋完整播放器，但不停止或重建 Media3 播放。
- 支援橫向車機畫面及一般直向裝置。
- 進入時隱藏不必要的系統列；退出後完整還原原狀。
- 啟用時保持螢幕常亮；退出、App 進背景或 Lifecycle 停止時立即釋放。
- Back 鍵退出相框模式。
- 單擊顯示／隱藏最小控制層，控制層包含退出按鈕；不要讓單擊照片直接永久退出，以免誤觸。
- 顯示時鐘與歌曲資訊時需有足夠對比，可使用底部漸層，不長期覆蓋照片中央。
- 歌曲切換只更新文字，不重置照片輪播。
- App 進背景時暫停照片計時器，回前景後繼續，不在背景解碼圖片。

### 10.3 圖片載入與效能

- 使用專案既有 Coil 3。
- 只預載目前與下一張，不把整個照片池解碼進記憶體。
- 請求尺寸應接近實際顯示尺寸。
- 使用兩個穩定圖層或等價方式交叉淡入，不在每個 animation frame 建立新 request。
- 避免在 Compose recomposition 重新洗牌或重讀磁碟。
- 轉場與計時使用 Lifecycle-aware coroutine。
- 大圖解碼失敗、檔案被刪除或 URI 權限失效時跳過，不中止音樂播放。
- 相框模式不得持有 CPU wakelock；只使用畫面常亮旗標。

## 11. 預計修改的既有檔案

實作時以當時最新程式碼為準，可能涉及：

| 檔案 | 修改內容 |
|---|---|
| `gradle/libs.versions.toml` | AppAuth 或經 Gate 0 確認的 OAuth 依賴 |
| `app/build.gradle.kts` | 依賴、BuildConfig/resource 注入、版本號 |
| `app/src/main/AndroidManifest.xml` | OAuth redirect；必要時備份排除設定 |
| `app/src/main/kotlin/com/metrolist/music/constants/PreferenceKeys.kt` | 相框偏好設定 key |
| `app/src/main/kotlin/com/metrolist/music/di/AppModule.kt` | Repository/client provider；若合適也可新增獨立 module |
| `app/src/main/kotlin/com/metrolist/music/ui/screens/settings/SettingsScreen.kt` | 相框設定入口 |
| `app/src/main/kotlin/com/metrolist/music/ui/screens/NavigationBuilder.kt` | `settings/photo_frame` route |
| `app/src/main/kotlin/com/metrolist/music/ui/player/Player.kt` | 相框按鈕與 Overlay 掛載點 |
| `app/src/main/res/values/metrolist_strings.xml` | 只新增英文預設字串 |
| `README.md` | 使用者可見功能與限制 |
| `README.zh-TW.md` | 與英文 README 保持一致的繁中內容 |

禁止修改其他語言的 `strings.xml` 或 `metrolist_strings.xml`。禁止修改預設 `strings.xml`；所有新增字串只能放在 `app/src/main/res/values/metrolist_strings.xml`。

## 12. 多 Agent 分工計畫

### 12.1 執行原則

- 優先依賴順序執行，不要讓多個 Agent 同時修改衝突熱點。
- 若並行，使用獨立 git worktree 與獨立 branch；未提交變更不會自動出現在其他 worktree。
- 每個 Agent 只修改分配範圍，發現跨範圍需求時回報給整合 Agent。
- `Player.kt`、`SettingsScreen.kt`、`NavigationBuilder.kt`、`PreferenceKeys.kt`、Manifest、Gradle、字串與 README 由整合 Agent 或明確指定的單一 Agent 統一處理。
- 非整合 Agent 不要自行更新本文件的進度，以免造成文件衝突；完成後回報 commit hash、測試結果與已知限制。
- 每個階段都必須先在最新目標分支上建置，不能假設其他 Agent 的未合併程式碼存在。

### 12.2 建議工作包

| 工作包 | 依賴 | 可修改範圍 | 完成條件 |
|---|---|---|---|
| P0 Google 可行性 Spike | 無 | 隔離 prototype 或最小 `photo/google` 程式碼 | fossDebug OAuth、Picker、下載成功；scope 與政策結論有紀錄 |
| P1 共用模型與儲存 | P0 結論可並行 | `photo/` 共用模型、manifest、storage、tests | 原子 manifest、清除、空間檢查測試通過 |
| P2 本機照片來源 | P1 | `photo/LocalPhotoSource*`、tests | 選取、持久 URI、失效 URI 處理通過 |
| P3 相框播放引擎 | P1 | controller/viewmodel、tests | 隨機不重複、Lifecycle、失敗跳過通過 |
| P4 相框 Compose UI | P3 | `PhotoFrameOverlay.kt` | 橫／直向、轉場、常亮與退出行為完成 |
| P5 Google OAuth／Picker 正式化 | P0、P1 | `photo/google/`、tests | token 安全、session、分頁、下載、清理完成 |
| P6 設定頁 | P1、P2、P5 | `PhotoFrameSettings.kt` | 兩來源、混合、狀態、刪除與 disclosure 完成 |
| P7 整合 | P2、P4、P5、P6 | 衝突熱點既有檔案 | Player、Settings、DI、Manifest、Gradle、strings 接妥 |
| P8 文件與發行驗證 | P7 | README、版本、release 文件 | README 同步、版本提升、完整測試完成 |

### 12.3 P0 必須產出的紀錄

- 採用的 OAuth library 與版本。
- 使用的 OAuth endpoint、redirect URI 格式與 PKCE 證明。
- debug/release client ID 配置方式。
- fossDebug 是否完全不依賴 GMS。
- Picker scope 在 Cloud Console 的分類。
- Testing refresh token 的實際到期行為。
- Google 政策是否允許公開發布此數位相框 use case。
- GO、LOCAL-ONLY 或 NO-GO 決策。

P0 若為 LOCAL-ONLY，P1 至 P4、P6 至 P8 可繼續，但 P5 與所有 Google 使用者入口不得併入正式發行版。

## 13. Git 與專案規範

每個 Agent 開始前：

```bash
git status --short --branch
git fetch origin
git pull --rebase origin main
git status --short --branch
```

若第一次 `git status` 顯示未提交變更，不得 pull、覆寫或清除；必須先停止並請使用者決定。不得使用 `git reset --hard`、強制推送或會遺失其他工作的方法。

提交前至少執行：

```bash
git diff --check
git status --short
```

修改 App 程式碼、資源或依賴時，將當時 `versionName` 的第三段加 1；只修改文件時不提升版本。使用者可見功能完成時，`README.md` 與 `README.zh-TW.md` 必須同步更新。

建議 commit 切分：

```text
feat(photo-frame): add local photo source
feat(photo-frame): add slideshow engine
feat(photo-frame): add immersive player overlay
feat(google-photos): add picker authorization
feat(google-photos): import selected photos
feat(settings): configure photo frame
docs(photo-frame): document photo sources
```

實際 commit 仍需遵守 `type(scope): short description`，每個 commit 保持可建置或清楚標示為不進正式分支的 spike。

## 14. 測試計畫

### 14.1 單元測試

- manifest 正常讀寫、版本不支援、內容毀損及原子替換。
- Fisher-Yates 輪播每輪不重複。
- 新一輪首張不等於上一輪末張。
- 一張照片、零張照片、全部失效照片。
- 本機 URI 權限失效。
- Picker session create/get/delete JSON 解析。
- `mediaItems.list` 多頁分頁。
- 只接受 `image/*`。
- 401 refresh 一次、`invalid_grant`、429 backoff、5xx retry 上限。
- 下載取消、低儲存空間、staging rollback。
- 登出／清除資料會移除 token、manifest 與 Google 照片副本。
- Google API 測試使用 Ktor MockEngine 或 fake service，不在一般單元測試呼叫真實 Google API。

### 14.2 Compose／ViewModel 測試

- 無照片時顯示設定導引。
- 有照片時能啟動與退出 Overlay。
- App 進背景後停止換片，回前景恢復。
- 歌曲切換不重置照片順序。
- 設定變更在合理時機生效。
- 圖片載入錯誤會跳到下一張。

### 14.3 手動實機測試

- Android 8.0（minSdk 26）或可取得的最低版本環境。
- Android 最新目標版本。
- 沒有 Google Play Services 的 foss 裝置或模擬器。
- 有 Chrome／Custom Tab 與沒有可用瀏覽器的裝置。
- 橫向車機螢幕與一般手機直向畫面。
- 本機 Photo Picker 多選及重開 App 後 URI 仍有效。
- Google OAuth 同意、拒絕、取消、登出、撤銷授權。
- Google Picker 選取 1 張、數十張與產品上限。
- 網路中斷、token 過期、session timeout、API 429 模擬。
- 匯入中切到背景、旋轉畫面、終止 App 及重新啟動。
- 儲存空間不足與清除資料。
- 播放音樂期間進出相框，確認播放不中斷。
- 常亮旗標與系統列在退出後完整恢復。
- 連續輪播至少 30 分鐘，觀察記憶體、溫度與耗電。

### 14.4 必要建置

在專案根目錄執行：

```bash
./gradlew :app:assembleFossDebug
```

若新增可單獨執行的單元測試，亦應執行對應 test task。整合完成後不得只驗證 GMS variant，Foss Debug 是必要基準。

## 15. 驗收條件

### 15.1 本機來源

- 使用者可在設定頁選取多張本機照片。
- 重開 App 後仍可使用已授權 URI。
- 無廣泛儲存空間權限仍可運作。
- 可新增、移除與清空照片。

### 15.2 Google 來源

- Google client ID 未設定時 App 仍可建置及使用本機相框。
- 完成 OAuth 後只能存取 Picker 明確選取內容。
- Google 短效 URL 不會寫入長期照片池。
- session 完成後會清理。
- token 與照片資料符合安全儲存規範。
- 使用者可刪除照片副本及解除授權。
- 公開啟用前已有 OAuth verification／政策可發布證據；否則保持停用或只限開發版。

### 15.3 相框播放

- 音樂播放不中斷。
- 照片隨機播放且同輪不重複。
- 交叉淡入淡出流暢，無持續記憶體增長。
- 支援橫向及直向。
- Back 與退出按鈕都能離開。
- App 進背景不繼續解碼或換片。
- 系統列與常亮狀態離開後恢復。

### 15.4 專案品質

- 不修改 Room schema。
- 沒有 token、secret、個人照片、帳號或 keystore 進入 git。
- `git diff --check` 通過。
- `./gradlew :app:assembleFossDebug` 通過。
- 使用者可見變更已同步更新兩份 README。
- App 版本第三段只提升一次且符合當時基準版本。

## 16. 主要風險與停止條件

| 風險 | 等級 | 處理方式 |
|---|---|---|
| Picker 用途未通過 Google Photos 政策審核 | 高 | Google 來源停止公開發布，本機來源照常完成 |
| Testing refresh token 7 天到期 | 高 | 只用於開發；正式版需 Production 與 verification |
| foss 車機無法完成 Google OAuth | 高 | Gate 0 實機驗證；失敗則只提供本機來源 |
| 60 分鐘 baseUrl 過期 | 高 | session 完成後立即下載至 staging，不長期保存 URL |
| 大量照片耗盡儲存空間 | 中 | 產品上限、尺寸縮放、匯入前空間檢查、原子替換 |
| 大圖造成 OOM 或發熱 | 中 | 顯示尺寸下載、Coil size、只預載下一張 |
| 多 Agent 同時修改整合熱點 | 中 | 獨立 branch/worktree，整合 Agent 統一修改熱點 |
| 車輛行進中照片分散注意力 | 高 | 不整合 Android Auto；由使用者主動啟動，產品發行前再評估停車狀態限制 |

下列情況必須停止 Google 部分並回報：

- Google 文件或審查指出此用途必須使用 Ambient API。
- 需要提交 client secret、帳號密碼或繞過 OAuth 才能運作。
- 只能用未公開 Google Photos endpoint 或爬蟲取得照片。
- foss variant 無法在不加入 GMS 專有依賴下完成既定需求，且使用者未同意只支援特定 flavor。
- 必須修改 App Room 資料庫 schema 才能繼續。

## 17. 發行前最終檢查

- 重新檢查 Google Photos API 價格、配額、政策與 OAuth scope 分類。
- 確認 Production OAuth consent 與 verification 狀態。
- 確認首頁、隱私權政策、資料刪除說明及 verified domain。
- 確認應用內 disclosure 在 OAuth 前出現。
- 確認使用者登出與刪除流程完整。
- 確認 release client ID、package name 與 SHA-1 完全一致。
- 確認沒有把 debug OAuth client 用於 release。
- 確認 README 英文／繁中內容一致。
- 確認 Foss Debug 建置與實機測試完成。
- 確認 Git diff 只有本功能預期檔案且沒有敏感資料。
