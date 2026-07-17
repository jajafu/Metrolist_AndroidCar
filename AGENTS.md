# 以 AI 代理的身分操作 _AndroidCar

Metrolist_AndroidCar 是一個使用 Kotlin 開發的第三方 YouTube Music 客戶端，緊密遵循 Material 3 設計規範。

## 專案操作規則

1. 開始工作前，務必先從 `main` 分支拉取最新變更，以減少合併衝突。
2. 提交名稱應清晰明確，格式為：`type(scope): short description`。例如：`feat(ui): add dark mode support`。可選填 scope。
3. 所有字串編輯請修改 `Metrolist/app/src/main/res/values/metrolist_strings.xml` 檔案，**不要**修改 `Metrolist/app/src/main/res/values/strings.xml`。請勿碰觸專案中其他的 `strings.xml` 或 `metrolist_strings.xml` 檔案。**只**編輯預設（英文）的 `metrolist_strings.xml` 檔案，**不要**編輯其他語言版本。
4. 請遵循 Kotlin 與 Android 開發的最佳實務。
5. **不要**修改 APP 的資料庫結構。

## AI 專屬規範

1. 嚴禁對 readme/markdown 檔案（包含本檔案）進行任何修改，以確保文件內容準確且對所有貢獻者保持一致。
2. 除非明確要求，否則不得對任何分支進行 commit、push 或 merge。若經明確要求並獲授權 commit/push/merge，則你有權執行；相關責任由提出要求者承擔。
   - 絕對不可使用任何會修改 git 歷史紀錄的指令、強制推送（除了在你自己的分支上 rebase 外），或在未經人類指示下刪除分支。
3. 務必遵循人類貢獻者提供的指引和指示。
4. 確保所有貢獻具備最高程式碼品質，包含正確的排版、清晰的變數命名，以及必要時提供詳盡的註解。
5. 只有在程式邏輯複雜或非顯而易見時才加入註解。避免撰寫僅重複描述程式行為的多餘註解。
6. 在所有程式碼貢獻中，優先考量效能、電池續航力與可維護性。隨時考量變更對整體使用者體驗與應用程式效能的影響。
7. 如有任何疑問，請詢問人類貢獻者。切勿在未經釐清的情況下假設需求或實作細節。
8. 若未按照下一節的說明測試你的變更，將受到人類貢獻者的指正，並可能被要求重做。務必在要求最終審查前徹底測試你的變更。
9. 你絕對**不可**以任何方式提升應用程式的版本號。版本號提升僅由核心開發團隊在手動審查後執行。

## 建置與測試變更

1. 修改程式碼後，應建置應用程式以確保無編譯錯誤。請在專案根目錄執行以下指令：

```bash
./gradlew :app:assembleFossDebug
```

2. 若建置未成功，請檢視錯誤訊息、修正程式碼問題，然後再次嘗試建置。
3. 建置成功後，可在模擬器或實體裝置上測試變更。安裝位於 `app/build/outputs/apk/universalFoss/debug/app-universal-foss-debug.apk` 的 APK，並請人類協助測試你修改的特定功能。
