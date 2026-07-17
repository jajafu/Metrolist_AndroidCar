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

## 功能
- 獨立調整播放音量。一般 YouTube Music 僅跟隨系統音量，無法獨立控制。本軟體可獨立設定音樂音量，降低對導航語音的干擾。
- 播放 YouTube Music 音樂與影片。
- 背景播放與離線下載。
- 跳過靜音、睡眠計時、音量正常化、速度與音調調整。
- 同步歌詞與歌詞翻譯。
- 搜尋歌曲、專輯、藝人、影片與播放列表。
- 音樂庫、私人播放列表與帳號同步。
- 與其他使用者一起聆聽。
- Material 3 介面，支援亮色、暗色、全黑、動態與預設配色主題。
- 針對 Android Auto 調整版面與播放控制。

## 截圖

截圖位於 repository 的 [`fastlane/metadata/android`](fastlane/metadata/android) 目錄。

## 從原始碼建置

1. 連同 submodule 複製本 repository：

   ```bash
   git clone --recurse-submodules https://github.com/jajafu/Metrolist_AndroidCar.git
   ```

2. 建置 FOSS debug 版本：

   ```bash
   ./gradlew :app:assembleFossDebug
   ```

產生的 APK 位於 `app/build/outputs/apk/foss/debug/app-foss-debug.apk`。

GitHub Actions workflow 由使用者手動執行，且只建置及檢查這個 FOSS debug 版本。從 main 分支手動執行並建置成功後，會將 APK 上傳為 workflow artifact，並發布至依版本命名的 GitHub Release。它不會在 push 或 pull request 時自動執行，不會建置 GMS、Izzy 或 release build variant，也不需要正式簽章 Secrets。CI 使用臨時 debug 金鑰，因此安裝不同次執行產生的 APK 時，可能需要先移除舊版。

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
