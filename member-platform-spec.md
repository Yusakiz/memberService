# 簡易會員平台實作規格白皮書

版本：1.0｜日期：2026-10-10｜用途：交由 Antigravity／Gemini 開發面試實作題

## 1. 給開發 AI 的指令

請依本文件實作可在本機執行的完整專案。目標是讓熟悉 Java 的工程師能閱讀、修改並在面試時解說程式。功能正確、程式直白、容易啟動，優先於視覺效果與架構擴充。

先檢查現有工作目錄與工具版本，再逐階段開發。若已有程式，先理解再修改，不覆蓋使用者未提交的工作。一般實作細節可自行決定，遇到真正的需求衝突再詢問。不可自行增加管理員、Email、手機、OAuth、JWT、忘記密碼、信箱驗證、頭像、微服務、Redis、Docker、複雜權限或部署平台。

每階段完成後，簡短說明修改檔案、功能流程及實際驗證結果。不要把尚未執行的測試寫成通過。使用真實且分階段的 Git 提交，不偽造開發歷程。最後提供繁體中文 README 與面試解說文件。

## 2. 需求解讀與範圍

原題要求 Java 8 以上、Spring Boot 或 Spring MVC、Vue／React／Angular、資料庫，以及公開 Git repository，並包含註冊、登入、修改、查詢、刪除會員。

本專案採以下明確解讀：

- 訪客可公開註冊、登入；註冊即為會員資料的 Create。
- 查詢會員、修改個人資料、刪除帳號及會員頁，皆需要登入。
- 原題「新增也必須登入」與公開註冊存在矛盾。依委託者確認，本作業將其視為文字疏漏，以公開註冊作為例外；這不是出題方已確認的解釋，README 應揭露此假設。
- 不增加另一個登入後新增會員功能，不做管理員與角色管理。
- 題目的姓名以「暱稱」實作，可用中文或英文，不要求真實姓名。
- 修改及刪除僅限本人；查詢可以看到其他人的帳號、暱稱。
- 本次交付目標是本機可執行及 GitHub 公開原始碼，不包含公開網站部署。

## 3. 技術選擇

| 項目 | 規格 |
| --- | --- |
| Java | Java 17 |
| 後端 | Spring Boot、Spring MVC、Maven |
| 資料存取 | Spring JDBC／JdbcTemplate，參數化 SQL |
| 資料庫 | SQLite，Xerial SQLite JDBC driver |
| 驗證 | Jakarta Bean Validation，必要時用簡單 Service 判斷 |
| 安全 | Spring Security、Session、BCrypt、CSRF |
| 前端 | Vue 3、JavaScript、Vite、Vue Router |
| 請求 | 原生 fetch，一個小型 API 共用模組 |
| 版面 | 原生 HTML 與簡單 CSS，不使用 UI 框架 |
| 測試 | Spring Boot Test、Spring Security Test、JUnit；真實 SQLite 暫存檔 |
| 版本控制 | Git，單一 repository 包含 backend 與 frontend |

新專案使用 Java 17 相容的 Spring Boot 4.x 正式穩定版本；開工時確認可取得的版本與對應 starter，不使用 SNAPSHOT，也不要混用不同大版本的 Security 範例。若現有專案已採相容且仍受維護的版本，不為了版本號而升級。將實際版本固定在 pom.xml，Spring 元件版本交由 Boot 管理。

Vue／Vite／SQLite JDBC 使用相容的正式版本，將實際 Node.js 需求與版本記入 README，提交 package-lock.json。不要在文件中只寫「最新版」而未記錄實際使用版本。

此處採 JdbcTemplate 是為了讓 SQL 和 CRUD 流程容易解說，不引入 JPA、Hibernate dialect 或額外 ORM。小型本機示範適合 SQLite；不宣稱本設計適合高併發正式服務。

## 4. 頁面與操作

只設計兩個主要路由，不另外建立註冊頁。

### 4.1 /login：登入／註冊頁

- 同頁提供「登入」「註冊」兩個切換按鈕。
- 登入表單：帳號、密碼、登入按鈕。
- 註冊表單：帳號、暱稱、密碼、確認密碼、註冊按鈕。
- 清楚標註暱稱可自行取名，不需要本名。
- 註冊成功後切換回登入表單，保留新帳號、清空密碼，顯示「註冊成功，請登入」。不自動登入。
- 切換表單時清空密碼與舊錯誤，不必清空已輸入的帳號。
- 登入成功導向 /members。
- 已登入者進入 /login 時導向 /members。
- 密碼欄位使用 type=password；依登入／新密碼用途設定 autocomplete。

### 4.2 /members：會員頁（兼歡迎頁）

同頁依序顯示以下區塊，無需側邊欄或 dashboard。

1. 歡迎區：顯示「歡迎，{暱稱}」與登出按鈕。
2. 會員查詢：查詢類型（帳號／暱稱）、關鍵字、查詢按鈕，以及結果表格。
3. 我的資料：帳號與暱稱編輯表單、儲存按鈕。
4. 修改密碼：目前密碼、新密碼、確認新密碼、修改按鈕。
5. 刪除帳號：獨立顯示紅色按鈕，點擊後以原生 confirm 明確提示資料永久刪除。

初次進入不列出全部會員；輸入條件後才查詢。查詢結果只顯示帳號與暱稱，不提供操作其他會員的按鈕。

修改帳號或密碼成功後登出，提示重新登入；修改暱稱則維持登入並更新歡迎文字。刪除成功後清除登入狀態、導回 /login 並顯示成功訊息。取消刪除不得送出刪除請求。

### 4.3 視覺與互動

白底、置中內容、簡單邊框與一致按鈕樣式即可；不使用動畫、插圖、漸層或裝飾性卡片。手機窄畫面不應有明顯溢出。

送出時停用該表單按鈕，防止重複提交；完成後恢復。錯誤顯示在表單附近，避免只有 console 訊息。搜尋無結果顯示「查無符合的會員」，與查詢失敗分開處理。前端先做基本檢查，後端仍必須獨立驗證。

## 5. 欄位及驗證規則

下列長度為本專案預設，目的是把題目未指定的細節固定下來，不宣稱為出題方要求。

| 欄位 | 規則 |
| --- | --- |
| 帳號 username | 必填，4～20 字元，半形英文字母、數字、底線；以完整字串匹配 [A-Za-z0-9_]{4,20} |
| 暱稱 nickname | 必填，去除頭尾空白後 1～30 個 Unicode code point；允許中英文、數字及一般可顯示字元；拒絕換行與控制字元，不要求唯一 |
| 密碼 password | 必填，8～64 字元，僅 ASCII 0x21～0x7E，即半形英文字母、數字、標點符號；完整匹配 [!-~]{8,64} |
| 確認密碼 confirmPassword | 與密碼完全一致，不寫入資料庫 |

帳號去除頭尾空白後轉成小寫，再進行驗證、查重、儲存和登入比對；例如 Yu_01 與 yu_01 視為同一帳號。前端提示帳號不區分大小寫。

密碼不得 trim 或轉換大小寫；空格、Tab、換行、中文及全形字元皆不接受。不強制同時含大小寫、數字和符號，也不增加連號或重複字元規則。64 個 ASCII 字元的上限亦避免超過 BCrypt 的 72-byte 輸入限制。

暱稱不使用 v-html 顯示，使用 Vue 一般文字插值。前端暱稱長度計算需與後端 code point 計算一致，例如 JavaScript 使用 Array.from(value).length。

修改資料使用同一套帳號與暱稱規則；保留自己的原帳號不算重複。密碼修改另用表單，必須驗證目前密碼、新密碼規則及兩次新密碼一致。修改密碼不是原題額外硬性要求，是本規格對個人帳號資料修改的基本涵蓋。

## 6. 資料表與 SQLite

只建立一張 members 表，最少欄位如下：

```sql
CREATE TABLE IF NOT EXISTS members (
    id INTEGER PRIMARY KEY,
    username TEXT NOT NULL UNIQUE,
    nickname TEXT NOT NULL,
    password_hash TEXT NOT NULL
);
```

- id 是後端識別本人用的固定主鍵；帳號變更不影響 id。
- password_hash 僅儲存 BCrypt 結果，絕不儲存明碼或確認密碼。
- 啟動時建立資料目錄與資料表，不使用每次啟動刪表重建。
- 預設資料檔 backend/data/members.db，以從 backend 啟動為準；README 說明工作目錄及實際解析路徑。
- 可透過設定覆寫資料檔位置；重啟後保留既有資料。
- 對 SQLite 明確設定 DataSource，必要時使用小型連線池且 maximumPoolSize=1；不要照抄 MySQL 專用參數。
- schema.sql 使用冪等建表；SQLite 屬非 Boot 預設內嵌資料庫時，明確啟用初始化，例如 spring.sql.init.mode=always，並驗證首次啟動確實執行。
- Service 必要的寫入交易保持短小，不在交易裡做外部呼叫。
- 先查重以提供友善訊息，並由 UNIQUE constraint 最終防止競爭新增。只把實際唯一鍵衝突轉成帳號重複；其他 SQL 錯誤不可一律冒充重複帳號。
- 測試使用獨立暫存 SQLite 檔案，不碰示範資料庫，也不用 H2 代替 SQLite 驗證相容性。

## 7. 分層與程式風格

### 後端責任

| 類別／套件 | 責任 |
| --- | --- |
| AuthController | CSRF token、註冊入口；登入與登出由 Security filter 處理 |
| MemberController | 目前會員、查詢、修改、改密碼、刪除入口 |
| MemberService | 業務驗證、查重、BCrypt、本人資料操作 |
| MemberRepository | JdbcTemplate 與 SQL |
| Member | 資料列模型，不直接序列化回應 |
| dto | 實際 API 所需的請求與回應物件 |
| SecurityConfig | 公開／受保護路徑、登入、登出、Session、CSRF |
| MemberUserDetailsService／MemberPrincipal | 載入登入資料；principal 帶固定 memberId |
| GlobalExceptionHandler | MVC 業務與驗證錯誤轉 JSON |

Security filter 的登入失敗、未授權及 CSRF 錯誤，用對應 handler 回傳 JSON；不能假設 ControllerAdvice 會攔到 filter 的錯誤。

以建構式注入、一般 class、直白 if/for 與明確命名為主。不要為每個 Service／Repository 建立只有一個實作的介面，也不要 BaseController、BaseService、GenericRepository、MapStruct、AOP 或過度泛型。預設不使用 Lombok，避免隱藏基本結構；允許簡單重複，不為少量程式建立複雜工具層。註解解釋原因，不逐行翻譯程式。

### 前端檔案

保留 main.js、App.vue、router/index.js、api.js、views/LoginView.vue、views/MemberView.vue 與簡單樣式即可。使用 Composition API／script setup，但不加 Pinia 或通用表單框架。只有明確重複或檔案過長才拆元件。

## 8. 登入、Session 與授權

### 8.1 登入方式

使用 Spring Security 標準 formLogin filter，loginProcessingUrl 設為 /api/auth/login，欄位名 username、password。前端登入請求使用 application/x-www-form-urlencoded；其他有內容的業務 API 使用 JSON。成功／失敗 handler 回傳 JSON 或約定的空回應，不做伺服器 HTML redirect。

沿用 Security 的認證與 session fixation 防護，不自行在 Controller 比對密碼後只塞入一個 session attribute。使用含 memberId 的 principal，登入帳號需與註冊相同正規化。

### 8.2 登入後保護

- 不只靠 Vue Router 守衛；後端保護會員 API。
- 更新、改密碼、刪除都使用 /me，由 principal 的 memberId 決定目標，不接受前端傳來的目標會員 id。
- 存取 /me 與執行受保護操作時確認該 memberId 仍存在；帳號已刪除時不得讓舊 Session 繼續操作，清除其認證並回 401。可用一個小型請求檢查 filter 統一處理，避免到處重複。
- 帳號與密碼變更後使本次 Session 失效，重新登入使用新資料；暱稱以最新 DB 資料顯示，不長期沿用 principal 的舊值。
- 本題不另做所有裝置的 Session 管理。帳號／密碼修改後，其他既有 Session 可能直到過期才結束，README 說明此簡化限制；刪除後的所有舊 Session 則必須在下一次受保護請求被拒絕。
- Session 閒置逾時預設 30 分鐘；停用 Remember Me；不把密碼或登入 token 放 localStorage。
- JSESSIONID 使用 HttpOnly、SameSite=Lax；本機 HTTP 不強制 Secure，未來 HTTPS 環境才啟用 Secure。
- 登出使用 POST，使 Session 失效並清除 Cookie。

### 8.3 CSRF 與同來源

保留 CSRF 防護，禁止為了讓 Vue 能送請求直接全域停用 CSRF。

使用 HttpSessionCsrfTokenRepository 與公開的 GET /api/auth/csrf，回應 token、headerName。前端只保存在記憶體，送 POST／PUT／DELETE 時附對應 header。呼叫該端點需實際取得 token 值，以確保延遲 token 被建立。

初始化、登入成功、登出後及 Session 失效後，重新取得 token；需考量 Security 登入／登出會清除舊 token。註冊與登入即使 permitAll，也仍需 CSRF token。遇到 403 時顯示重新整理／重試提示；不要自動重送可能已執行的寫入。

開發環境由 Vite 把 /api 代理到 http://localhost:8080，瀏覽器全程使用 http://localhost:5173，fetch 使用相對路徑與 same-origin credentials。不要混用 localhost 與 127.0.0.1，也不要為此設置萬用 CORS。Vue 路由選擇 hash history，降低將來靜態部署路由回退設定的負擔。

## 9. API 契約

所有會員回應只包含必要公開欄位，不回傳 password_hash、Security principal 或整個資料模型。

| Method | 路徑 | 身分 | 輸入／用途 | 成功 |
| --- | --- | --- | --- | --- |
| GET | /api/auth/csrf | 公開 | 取得 token 與 headerName | 200 JSON |
| POST | /api/auth/register | 公開 | username, nickname, password, confirmPassword | 201 公開會員資料 |
| POST | /api/auth/login | 公開 | 表單編碼 username, password | 200 公開會員資料 |
| POST | /api/auth/logout | 登出操作 | 無內容，有 CSRF | 204 |
| GET | /api/members/me | 登入 | 取得本人最新資料 | 200 公開會員資料 |
| GET | /api/members | 登入 | field=username 或 nickname；keyword；page 預設 0 | 200 查詢結果 |
| PUT | /api/members/me | 登入 | username, nickname | 200，含 member 與 reLoginRequired |
| PUT | /api/members/me/password | 登入 | currentPassword, newPassword, confirmPassword | 204，並使本次 Session 失效 |
| DELETE | /api/members/me | 登入 | 無內容；前端先確認 | 204，並登出 |

公開會員資料格式為 {"username":"yu_01","nickname":"慕語"}。不強制通用成功包裝，204 不應被前端強制 JSON.parse。

修改資料成功範例：

```json
{
  "member": { "username": "yu_02", "nickname": "慕語" },
  "reLoginRequired": true
}
```

查詢規則：

- 欄位 selector 只接受 username／nickname；後端以 if 或 enum 選擇固定 SQL，不能直接拼接傳入欄位名。
- 關鍵字 trim 後 1～30 個 code point，空白查詢回 400。
- 以包含字串做模糊查詢；帳號關鍵字轉小寫，暱稱支援中文子字串與 ASCII 英文大小寫不敏感查詢；不承諾所有語系的 Unicode case folding。
- %、_ 與跳脫字元視為一般文字，使用一致的 LIKE ESCAPE 規則及參數綁定，不允許任意萬用字元。
- 固定每頁 20 筆，以 id 升冪排序。page 必須為非負且可安全計算 offset 的整數。
- 用 LIMIT 21／OFFSET 取得當頁 20 筆與 hasNext，不必額外 COUNT。前端提供上一頁／下一頁，改查詢條件時回第 0 頁；避免只顯示前 20 筆導致找不到其他會員。
- 結果包含自己也可以，無結果回 200 空陣列。

```json
{
  "items": [{ "username": "yu_01", "nickname": "慕語" }],
  "page": 0,
  "hasNext": false
}
```

錯誤最少包含 message，欄位錯誤可加 fieldErrors，不加無用途的 code 體系：

```json
{
  "message": "輸入資料有誤",
  "fieldErrors": { "password": "密碼限 8～64 個半形英數或符號，不含空白" }
}
```

| 狀態 | 使用情境 |
| --- | --- |
| 400 | 資料格式、必填、確認密碼不符、目前密碼錯誤、查詢參數錯誤 |
| 401 | 帳號或密碼錯誤；缺少／過期登入狀態；會員已不存在 |
| 403 | CSRF 缺少或失效 |
| 409 | 帳號重複 |
| 500 | 非預期錯誤，顯示一般訊息，不洩漏 SQL 或 stack trace |

登入失敗統一「帳號或密碼錯誤」，不區分不存在帳號與錯誤密碼。前端僅對會員受保護請求的 401 清除登入並導回登入頁；登入失敗的 401 留在表單呈現錯誤。CSRF filter 可能在授權前回 403，驗證未登入 API 的 401 時需先取得有效 CSRF token，避免測試誤判。

## 10. Git 與 GitHub

Git CLI 或 Antigravity Git 介面皆可，兩者操作同一份 .git。先檢查電腦是否已安裝 Git，及目錄是否已是 repository；不可覆蓋既有 remote、任意修改全域使用者設定，或把 Token 寫入 remote URL。

建議真實提交節點：

1. chore: initialize backend and frontend
2. feat: add member registration and session login
3. feat: add member search and profile update
4. feat: add password change and account deletion
5. test: verify authentication and member permissions
6. docs: add setup and interview notes

可依實際完成順序調整，不要求為了湊提交拆出不可運作的版本。提交前檢查 diff。

.gitignore 至少排除 target/、node_modules/、dist/、IDE 個人設定、log、.env、data/ 內執行資料，以及 *.db、*.db-wal、*.db-shm、*.db-journal。保留 pom.xml、Maven Wrapper、package-lock.json、schema.sql、可公開的 application 設定與必要範例設定。不要放真實會員資訊或密碼。

請協助使用者透過既有 GitHub 登入建立 public repository，或由使用者建立後提供 HTTPS／SSH URL，再設定 origin 推送。不得猜測帳號或 URL，也不要要求使用者把 PAT 貼在聊天。遇到未登入就提供 GitHub 官方登入流程。不得 force push 或覆寫遠端既有內容。

推送後提供實際 repository URL，驗證原始碼已推送且公開；若因憑證或網路阻擋，明確交代未完成部分，不宣稱已上傳。此白皮書本身不代表 repository 已建立。

## 11. 執行方式與交付檔案

預設兩個程序即可，無需打包成單一 JAR、容器或外部網站。

- Windows：在 backend 執行 mvnw.cmd spring-boot:run；可另說明在 IDE 直接 Run 啟動類別。
- macOS／Linux：在 backend 執行 ./mvnw spring-boot:run。
- 前端：在 frontend 執行 npm ci，再 npm run dev。
- 瀏覽器入口：http://localhost:5173。
- 測試：backend 的 mvnw.cmd test／./mvnw test。
- 前端建置檢查：npm run build。

README 必須包含實際版本、安裝前提、啟動路徑、資料庫位置、API 摘要、欄位規則、需求例外、測試方式與已知限制。提供 docs/interview-notes.md，說明各層、一次註冊／登入／修改資料如何走過程式，以及重要設計理由。

demo 帳號由操作註冊建立，不提交含 demo 密碼的資料庫。交付 schema 與啟動初始化流程，確保 clone 後可產生新資料庫。公開展示時只用虛構帳號。

## 12. 開發階段

1. 建立最小專案、固定版本、初始化 SQLite 與 Git，驗證啟動。
2. 完成註冊、BCrypt、Session、CSRF、登入／登出與本人 API；先確保認證流程正常。
3. 完成查詢、個人資料修改、密碼修改與刪除。
4. 完成兩個 Vue 頁面，接真實後端，不使用 mock 資料冒充完成。
5. 執行關鍵測試、手動走完整流程，補上 README 與解說文件，再上傳 GitHub。

每階段用短段落解釋，不必每完成一個檔案就停下要求確認。需要做需求以外的大變更，才先詢問。

## 13. 驗收與必要測試

測試聚焦資料安全與功能契約，不為 getter、setter 或單純畫面樣式寫測試。以下可合併為少數整合測試，前端以手動流程與 build 驗證為主。

| 情境 | 預期 |
| --- | --- |
| 全新 clone／沒有資料庫 | 可按 README 啟動並建表，重啟不遺失資料 |
| 註冊 A、B；中文／英文暱稱 | 成功，確認密碼不存入 DB，密碼為 BCrypt |
| 大小寫不同的重複帳號 | 409；修改為自己的原帳號成功 |
| 密碼含中文、空白、換行、少於 8 或多於 64 | 前後端拒絕；合規 ASCII 符號可正常登入 |
| 密碼確認不符 | 註冊與改密碼皆拒絕 |
| 正確／錯誤登入 | 成功進入歡迎頁；失敗訊息不洩漏帳號是否存在 |
| 未登入直接進會員頁／呼叫會員 API | 頁面回登入；持有效 CSRF 的未登入 API 請求回 401 |
| 已登入但寫入缺少 CSRF | 403，資料不變 |
| 用帳號或暱稱查詢 B | 顯示帳號與暱稱，無密碼雜湊或內部資料 |
| 查詢含 SQL 特殊字元、% 或 _ | 不發生 SQL 注入，LIKE 特殊字元依字面處理 |
| 查無結果／多於 20 筆 | 空狀態正確；可翻頁找到後續會員 |
| A 修改資料並夾帶 B 的 id | 不可修改 B；目標始終由登入 principal 決定 |
| 修改暱稱 | 畫面更新；重新整理仍正確且保持登入 |
| 修改帳號 | 本次登出；新帳號登入成功、舊帳號登入失敗 |
| 密碼修改 | 目前密碼錯誤不更新；成功後登出，舊密碼失敗、新密碼成功 |
| 取消／確認刪除 | 取消不送出；確認後 DB 刪除本人並登出，另一會員不受影響 |
| 刪除會員後使用另一個舊 Session | 下一次受保護請求被拒絕 |
| 登出、重新整理、Session 逾時 | 不殘留可用會員畫面，登入後重取 CSRF 能再操作 |
| 後端測試與前端 build | 實際執行成功，記錄尚未驗證的環境限制 |
| GitHub public repository | 可取得原始碼，不含執行 DB、真實資料或敏感憑證 |

## 14. 面試解說準備

請在 interview-notes.md 用與實際程式一致的簡短說明回答：

- 為什麼選擇 Vue、SQLite、JdbcTemplate？有哪些取捨？
- Controller、Service、Repository 各處理什麼？
- 註冊資料如何驗證、查重、雜湊並寫入？
- Session 如何讓後端辨認登入者，和前端路由守衛有何差別？
- 為什麼需要 CSRF token，為什麼不是關閉 CSRF 解決整合問題？
- 如何避免修改或刪除其他會員，以及 SQL 注入？
- 為什麼確認密碼不進資料庫、API 不回傳密碼雜湊？
- 修改帳號後為什麼重新登入？多裝置 Session 有何限制？
- AI 協助了哪些工作？自己確認或修正了哪些內容？

只寫實際完成的協作與驗證；不要編造人工修改或聲稱所有程式皆親手撰寫。

## 15. 官方參考

下列資料用於查核技術行為，不取代本專案需求。實作時查閱與所選版本一致的文件。

- Spring Boot 系統需求：https://docs.spring.io/spring-boot/system-requirements.html
- Spring Security Form Login：https://docs.spring.io/spring-security/reference/servlet/authentication/passwords/form.html
- Spring Security CSRF：https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- SQLite 適用情境：https://www.sqlite.org/whentouse.html

本文件是實作規格，尚未代表程式已完成、測試通過或 GitHub 已上傳。
