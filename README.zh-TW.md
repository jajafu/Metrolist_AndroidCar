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
- 迷你播放器的新增至播放清單選擇器改用與音樂庫一致的大型自適應格狀卡片，方便車機操作。
- 首頁精簡為分類按鈕、12 個本機快速存取項目、帳號播放列表，以及最多 3 個 YouTube 官方推薦區塊；不再載入重複且耗費資源的每日探索、社區、相似內容、情境與類型、隨機排序及無限分頁區塊，且同步期間歌曲遭移除時，快速存取項目仍可安全顯示。
- 將已安裝的 App 品牌化為 `Metrolist_AndroidCar`，在啟動器、關於頁面與播放通知中使用黑色音樂車 Logo。
- 帳號已登入且同步功能開啟時，新播放列表預設同步至 YouTube Music。播放列表建立、歌曲加入或移除失敗時，待處理操作會儲存在 App 資料庫以外、自動重試，並在播放列表音樂庫顯示待同步數量；移除重複歌曲時會保留正確的 YouTube 項目識別碼，新建立的遠端播放列表也有同步寬限時間，重複遠端播放列表會整併為一個受保護的本機記錄且不遺失已下載歌曲，大量歌曲 ID 操作則會分批執行以低於 SQLite 限制。
- 歌曲按讚與取消按讚會使用單一、有順序且可持久保存的同步佇列。快速反向操作只保留最後狀態，YouTube 更新失敗時會跨 App 重啟保留待處理項目，並避免重複網路請求。遠端核對會保留最新的本機待處理選擇，沒有本機操作時則接受遠端變更；裝置本機歌曲的按讚不會送往 YouTube。
- 自動完整同步只有在所有必要項目、待處理歌曲按讚與播放列表操作都成功後，才會開始計算冷卻時間。部分失敗會保留錯誤狀態，並可立即重試。
- 音樂庫下拉重新整理會加入已排隊或執行中的完整同步、忽略重複下拉，並持續顯示旋轉指示直到共用工作結束；部分項目失敗時會提示使用者，不再連續執行兩次完整同步。
- 快速切換播放佇列或電台時只保留最新請求，避免較慢的網路回應覆蓋目前播放內容。
- Podcast、UGC 與未知媒體型態會在播放前驗證 WEB_REMIX 串流；WEB_REMIX 實際播放失敗時，重新解析會自動排除該來源並改用備援來源。
- 備份、還原、CSV 預覽與 M3U 匯入檔案操作改於背景執行；檔案預覽會採串流或限制讀取量，避免將大型文件整份載入記憶體，較新的選檔也會取消過期預覽。還原會先驗證暫存的資料庫與設定檔，失敗時回復所有替換內容，並在必要時重新啟動至可正常使用的資料庫狀態。
- 前景服務啟動處理與小工具主題支援 Android 8.0（API 26）至目前的 Android 版本。
- 年度回顧可安全處理符合條件的熱門歌曲少於 5 首的播放紀錄。
- 自動延伸播放佇列遇到暫時性錯誤時會以有限退避重試，斷線時等待網路恢復且不重複請求，最終失敗後可手動重試。YouTube 的續頁結束後，若已啟用類似內容，會以目前佇列尾端歌曲建立新電台，讓播放可持續延伸超過前 99 首，並排除佇列中已有的歌曲；佇列尾端判斷會依照隨機播放的實際順序，App 播放服務重啟後也會保留 YouTube 續頁狀態。
- 即時略過靜音會在每次切歌時重置偵測器並取消延遲跳轉工作，避免上一首累積的靜音誤跳過下一首。
- 平行下載共用執行緒安全的網址快取，避免重複解析與快取競態。
- 安全性強化會將 Listen Together 明文連線限制於區域網路伺服器、把私人小工具操作與匯出的更新接收器分離，並只允許受信任的控制器使用自訂媒體命令。

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
