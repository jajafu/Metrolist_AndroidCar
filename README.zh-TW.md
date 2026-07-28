[English](README.md) | [繁體中文](README.zh-TW.md)

# Metrolist Android Car

Metrolist Android Car 是 [Metrolist](https://github.com/MetrolistGroup/Metrolist) 的 Android車機導向客製分支。Metrolist 是一個開源的 Android YouTube Music 用戶端。

本分支由 [jajafu](https://github.com/jajafu) 維護，主要改善車載使用時的播放介面、可讀性與操作體驗。

## 目前的客製功能

- 將播放島放大 2 倍，提升資訊可見度。
- 修正橫式畫面下 Theme 與 Color 設定頁無法滑動的問題。
- 暗黑模式下，將調整按鈕外框改為純白色，增加對比度。
- 將快取播放列表增加為 3 首歌曲。
- 撥放封面下刪除睡眠按鈕，放大其他按鈕。
- 將已安裝的 App 品牌化為 `Metrolist_AndroidCar`，在啟動器、關於頁面與播放通知中使用黑色音樂車 Logo。
- 帳號已登入且同步功能開啟時，新播放列表預設同步至 YouTube Music。播放列表建立、歌曲加入或移除失敗時，待處理操作會儲存在 App 資料庫以外、自動重試，並在播放列表音樂庫顯示待同步數量；移除重複歌曲時會保留正確的 YouTube 項目識別碼，新建立的遠端播放列表也有同步寬限時間，避免暫時從清單消失。
- 自動完整同步只有在所有必要項目與待處理播放列表操作都成功後，才會開始計算冷卻時間。部分失敗會保留錯誤狀態，並可立即重試。
- 快速切換播放佇列或電台時只保留最新請求，避免較慢的網路回應覆蓋目前播放內容。
- 備份與還原檔案操作改於背景執行；還原會先驗證暫存的資料庫與設定檔，失敗時回復所有替換內容，並在必要時重新啟動至可正常使用的資料庫狀態。

## 功能

- 獨立調整播放音量。一般 YouTube Music 僅跟隨系統音量，無法獨立控制。本軟體可獨立設定音樂音量，降低對導航語音的干擾；導航壓低音量或短暫暫停播放後，音樂音量會可靠地恢復至原本設定。
- 播放 YouTube Music 音樂。
- 背景播放與離線下載。
- 跳過靜音、睡眠計時、音量正常化、速度與音調調整。
- 同步歌詞與歌詞翻譯。
- 搜尋歌曲、專輯、藝人與播放列表。
- 音樂庫、本機播放列表與帳號同步。
- 與其他使用者一起聆聽。
- Material 3 介面，支援亮色、暗色、全黑、動態與預設配色主題。
- 針對 Android Auto 調整版面與播放控制。
- 支援 YouTube 目前圖片 CDN 格式的高解析度圖片網址處理。
- 車機導向的預設設定，包含大型網格與精簡的自動生成播放列表。

## 建置與更新

在本機建置 FOSS Release 版本：

```bash
./gradlew :app:assembleFossRelease
```

手動觸發的 GitHub Actions workflow 只會建置 FOSS Release APK，並發布到本專案的 GitHub Releases。Release notes 會依上一版 Release 之後的實際提交內容自動產生；重新執行既有版本時也會更新日誌。Workflow 需要固定的 Android 簽章 Secrets：`RELEASE_KEYSTORE_BASE64`、`RELEASE_STORE_PASSWORD`、`RELEASE_KEY_ALIAS` 與 `RELEASE_KEY_PASSWORD`；請勿提交 keystore 或密碼。

App 內更新器會檢查[本專案的 Releases](https://github.com/jajafu/Metrolist_AndroidCar/releases)，並開啟符合版本的 APK 下載頁供確認。Android 仍會要求使用者核准安裝。

Release 名稱可能包含 `-car` 後綴；更新器會比較版本中的數字部分，因此目前版本不會被誤判為有新更新。

首次從舊版 Debug APK 安裝時，因為使用不同的 application ID，必須先解除安裝 Debug 版本。之後的 FOSS Release APK 使用相同簽章金鑰，可以直接覆蓋更新。

## 原始專案與致謝

本專案是 [Metrolist](https://github.com/MetrolistGroup/Metrolist) 的修改版本。原作者、貢獻者與版權聲明仍保留於程式碼與 [`LICENSE`](LICENSE) 中。

Metrolist 也使用了 [InnerTune](https://github.com/z-huang/InnerTune)、[OuterTune](https://github.com/DD3Boh/OuterTune)、[Better Lyrics](https://better-lyrics.boidu.dev)、[metroserver](https://github.com/MetrolistGroup/metroserver)、[MusicRecognizer](https://github.com/aleksey-saenko/MusicRecognizer) 及 [zemer-cipher](https://github.com/ZemerTeam/zemer-cipher) 等開源專案的成果。

## GPLv3 修改發布要求

本專案採用 [GNU General Public License v3.0](LICENSE) 授權。

發布修改後的程式或基於本專案產生的 APK 時，請遵守以下要求：

- 保留原作者、版權、來源、授權與免責聲明。
- 清楚標示這是修改版本，並說明修改內容。
- 提供對應原始碼，以及建置該發布版本所需的腳本或指示。
- 衍生作品必須依 GPLv3 授權發布，不得加入與授權條款衝突的限制。
- 隨發布內容提供 GPLv3 授權全文。

原始程式的版權仍屬於原作者；新增程式碼的版權則由各貢獻者保有。

## 免責聲明

本專案與 YouTube、Google LLC、Metrolist Group LLC 或其關係企業沒有任何隸屬、出資、授權、背書或其他關聯關係。

本專案中提及的商標、服務標章及其他智慧財產權均屬於各自權利人所有。
