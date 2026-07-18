# 以 AI 代理的身分操作 _AndroidCar

Metrolist_AndroidCar 是一個使用 Kotlin 開發的第三方 YouTube Music 客戶端，緊密遵循 Material 3 設計規範。

## 專案操作規則

1. 開始工作前，務必先從 `main` 分支拉取最新變更，以減少合併衝突。
2. 提交名稱應清晰明確，格式為：`type(scope): short description`。例如：`feat(ui): add dark mode support`。可選填 scope。
3. 所有字串編輯請修改 `Metrolist/app/src/main/res/values/metrolist_strings.xml` 檔案，**不要**修改 `Metrolist/app/src/main/res/values/strings.xml`。請勿碰觸專案中其他的 `strings.xml` 或 `metrolist_strings.xml` 檔案。**只**編輯預設（英文）的 `metrolist_strings.xml` 檔案，**不要**編輯其他語言版本。
4. 請遵循 Kotlin 與 Android 開發的最佳實務。
5. **不要**修改 APP 的資料庫結構。

## AI 專屬規範

1. 修改 App 相關檔案後更新 README.md，若沒有太大改變，不要隨意更新 README，不要一直膨脹內容。
2. 先執行 `git status --short --branch`；若工作樹有未提交變更，不得直接拉取或覆寫，應先提交／推送既有變更，或停下來請使用者決定如何處理。
- 工作樹乾淨時，執行 `git fetch origin`，再執行 `git pull --rebase origin main`（或目前任務指定的目標分支）。
- 若同步發生衝突、遠端未設定、認證失敗或無法確認已是最新版本，必須先停止修改並回報，不得在未同步狀態下繼續更新。
- 同步完成後再次確認分支與工作樹狀態，才可開始修改；不得使用 `git reset --hard`、強制推送或其他會遺失另一台電腦變更的作法，除非使用者明確授權。
- 修正完成後，進行git commit，並立即推送至目前同步的 GitHub 遠端與目標分支（通常為 `origin/main`）。提交前確認沒有敏感資料、非本次變更或未預期的檔案，並至少執行 `git diff --check` 與 `git status --short`；
3. 務必遵循人類貢獻者提供的指引和指示。
4. 確保所有貢獻具備最高程式碼品質，包含正確的排版、清晰的變數命名，以及必要時提供詳盡的註解。
5. 只有在程式邏輯複雜或非顯而易見時才加入註解。避免撰寫僅重複描述程式行為的多餘註解。
6. 在所有程式碼貢獻中，優先考量效能、電池續航力與可維護性。隨時考量變更對整體使用者體驗與應用程式效能的影響。
7. 如有任何疑問，請詢問人類貢獻者。切勿在未經釐清的情況下假設需求或實作細節。
8. 若未按照下一節的說明測試你的變更，將受到人類貢獻者的指正，並可能被要求重做。務必在要求最終審查前徹底測試你的變更。
9. 只有修改 App 程式碼、資源或依賴時，才將 App 軟體版本號的第三段加 1，如 `0.1.0` 要變成 `0.1.1`。單純修改文件或 GitHub Actions workflow 時不要提升版本號。

## 建置與測試變更

0. 初次建置前，連同 submodule 複製本 repository：

   ```bash
   git clone --recurse-submodules https://github.com/jajafu/Metrolist_AndroidCar.git
   ```

1. 修改程式碼後，應建置應用程式以確保無編譯錯誤。請在專案根目錄執行以下指令：

```bash
./gradlew :app:assembleFossDebug
```

2. 若建置未成功，請檢視錯誤訊息、修正程式碼問題，然後再次嘗試建置。
3. 建置成功後，可在模擬器或實體裝置上測試變更。安裝位於 `app/build/outputs/apk/foss/debug/app-foss-debug.apk` 的 APK，並請人類協助測試你修改的特定功能。
4. GitHub Actions workflow 只允許手動執行，且只建置 Foss Debug APK，不得在 push 或 pull request 時自動建置，也不建置 GMS、Izzy 或 Release build variant。從 main 分支手動執行並建置成功後，可將 Foss Debug APK 上傳為 workflow artifact 並發布至 GitHub Release，但不得加入正式簽章金鑰或簽章用 GitHub Secrets。CI 使用臨時 debug 金鑰，因此安裝不同次執行產生的 APK 時，可能需要先移除舊版。
