# 面試解說筆記

## 為什麼選 Vue、SQLite 與 JdbcTemplate？

Vue 讓兩個頁面的狀態與表單互動保持精簡，Composition API 可把每個操作的資料、送出狀態和錯誤放在一起。SQLite 不需要安裝伺服器，clone 後即可由應用程式建表，適合本機面試展示。JdbcTemplate 直接呈現 SQL、參數綁定與 CRUD 流程，比 ORM 更容易在短時間解說。

取捨是 SQLite 的寫入併發能力有限，所以連線池固定為一條連線；JdbcTemplate 需要自行撰寫 SQL 與 row mapping。本設計沒有宣稱適合高併發正式服務。

## 後端分層

- Controller：接收 HTTP 請求、取得登入 principal、決定狀態碼及是否使目前 Session 失效。
- Service：帳號正規化、欄位規則、查重、BCrypt、本人操作與交易邊界。
- Repository：只負責 JdbcTemplate、參數化 SQL、資料列轉成 `Member`。
- Security：form login、Session、CSRF、401／403 JSON handler，以及刪除會員後舊 Session 的檢查。
- DTO：限制每支 API 的輸入輸出；資料庫模型不直接序列化。

沒有替只有一個實作的 Service／Repository 建介面，也沒有 BaseController、GenericRepository、ORM 或 MapStruct，讓程式碼保持直白。

## 註冊流程

1. 前端先取得 CSRF token，驗證基本格式後送出 JSON。
2. `AuthController` 以 Jakarta Validation 檢查請求。
3. `MemberService` 將帳號 trim、轉小寫，檢查暱稱 code point、確認密碼及帳號重複。
4. 密碼經 BCrypt 後，Repository 用參數化 INSERT 寫入 SQLite。
5. 資料庫 UNIQUE constraint 是競爭條件的最後防線；只有實際的 `members.username` 唯一鍵錯誤會轉成 409。
6. 回應只包含 username、nickname；確認密碼及 password hash 都不回傳。

## 登入、Session 與前端路由

登入使用 Spring Security `formLogin` filter，前端以 `application/x-www-form-urlencoded` 傳送 username、password。`MemberUserDetailsService` 從資料庫載入會員，Spring Security 用 BCrypt 驗證並建立 Session。principal 保存固定的 memberId，所以日後即使帳號改名，後端仍能辨認同一筆會員。

Vue Router guard 只是改善畫面導向；真正的授權在後端。直接呼叫 `/api/members/**` 沒有 Session 時仍會得到 401。

## CSRF 設計

瀏覽器會自動帶上 Session cookie，惡意網站可能誘使瀏覽器送出寫入請求，因此不能因為前後端分離就關閉 CSRF。前端從 `/api/auth/csrf` 取得 token，只保存在 JavaScript 記憶體，POST／PUT／DELETE 放入指定 header。登入與登出後 Security 會清除舊 token，前端會重新取得。

403 不會自動重送寫入操作，避免在不確定伺服器是否已執行時重複修改。

## 查詢與防止 SQL 注入

查詢 selector 只接受 `username` 或 `nickname`，Service 用固定 if 分支選擇 Repository 方法，不把使用者輸入拼成欄位名稱。值全部用 JdbcTemplate 參數綁定。LIKE 查詢會把反斜線、`%`、`_` 跳脫成字面值；固定抓 21 筆，用第 21 筆判斷 `hasNext`，不額外 COUNT。

## 防止修改或刪除其他會員

更新、改密碼、刪除 API 都是 `/api/members/me`。目標 id 只取自登入 principal，不接受 request body 或 URL 傳入其他會員 id。整合測試刻意在更新 JSON 夾帶另一會員 id，確認另一會員資料不變。

修改帳號與密碼後使目前 Session 失效，避免 principal 內的登入資訊與資料庫不同。多裝置 Session 沒有做集中管理，因此其他裝置可能維持到逾時；刪除比較嚴格，`ExistingMemberFilter` 會在每次會員 API 請求確認 memberId 仍存在，不存在就清除認證、失效 Session 並回 401。

## 一次完整操作如何走過程式

- 註冊：`LoginView` → `api.js` → `AuthController` → `MemberService` → `MemberRepository` → SQLite。
- 登入：`LoginView` → `api.js` → Spring Security filter → `MemberUserDetailsService` → Session → success handler。
- 修改暱稱：`MemberView` → `api.js` → `MemberController` → `MemberService` → `MemberRepository`；回傳最新資料並維持登入。
- 改密碼／刪除：同樣只以 principal memberId 操作，成功後 Controller 使目前 Session 失效。

## 實際驗證

- Maven 整合測試：7 個測試，使用真實 SQLite 暫存檔，0 failure、0 error。
- Vite production build：成功。
- 本機端到端：實際同時啟動 8080 與 5173，透過 Vite proxy 完成註冊、登入、本人資料、修改暱稱、改密碼、重新登入、刪除及刪除後 401。

## AI 協作說明

本專案由使用者提供完整規格與需求決策，AI 協助檢查環境、建立專案、撰寫程式與測試、執行建置及整理文件。實作過程依真實測試結果修正了 Spring Boot 版本座標與 Windows SQLite 測試檔鎖問題。沒有宣稱所有程式皆由使用者親手輸入，也沒有編造未執行的人工驗證。
