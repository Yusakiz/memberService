# 簡易會員平台

這是 Java／Vue 面試實作專案，提供公開註冊、Session 登入、會員查詢、修改個人資料、修改密碼及刪除帳號功能。後端使用 Spring Boot 與 SQLite，前端使用 Vue 3 與 Vite。

GitHub repository：<https://github.com/Yusakiz/memberService>

## 技術版本

| 項目 | 版本 |
| --- | --- |
| Java | 17 |
| Spring Boot | 4.1.1 |
| Maven Wrapper | 3.3.4（下載 Maven 3.10.0） |
| SQLite JDBC | 3.53.4.0 |
| Vue | 3.5.43 |
| Vue Router | 5.4.0 |
| Vite | 8.3.4 |
| Node.js | 22.23.3（開發驗證版本） |
| npm | 10.9.9（開發驗證版本） |

Node.js 請使用 `^22.18.0` 或 `>=24.12.0`。Maven 由 Wrapper 提供，不必另外安裝全域 Maven；SQLite 透過 JDBC 使用，不必安裝資料庫伺服器。

## 專案結構

```text
backend/                  Spring Boot API、SQLite schema、Maven Wrapper、整合測試
frontend/                 Vue 3、Vite、Vue Router
docs/interview-notes.md   面試解說重點
member-platform-spec.md   本專案需求白皮書
```

## 本機啟動

請確認 8080 與 5173 沒有被其他程式使用。開兩個終端機，且不要混用 `localhost` 與 `127.0.0.1`。

Windows 後端：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

macOS／Linux 後端：

```bash
cd backend
./mvnw spring-boot:run
```

前端：

```powershell
cd frontend
npm ci
npm run dev
```

瀏覽器開啟 <http://localhost:5173>。Vite 會把相對路徑 `/api` 代理到 `http://localhost:8080`，不需要額外 CORS 設定。

## SQLite 資料庫

從 `backend` 目錄啟動時，預設資料檔為：

```text
backend/data/members.db
```

啟動時會自動建立 `data` 目錄並以冪等的 `schema.sql` 建立 `members` 表，重新啟動不會刪除資料。可用環境變數覆寫路徑：

```powershell
$env:MEMBER_DB_PATH = 'D:\data\member-platform.db'
.\mvnw.cmd spring-boot:run
```

執行中的 `.db`、WAL、journal 等檔案都被 Git 忽略，不提交示範帳號或密碼。

## 功能與操作

- `/login`：同頁切換登入與註冊；註冊成功後回到登入，不自動登入。
- `/members`：歡迎資訊、會員查詢、本人資料、修改密碼、刪除帳號。
- 初次進入不列出所有會員；搜尋固定每頁 20 筆，可上一頁／下一頁。
- 修改暱稱保持登入；修改帳號或密碼後須重新登入。
- 刪除帳號前使用瀏覽器原生確認，成功後回登入頁。

### 欄位規則

- 帳號：trim 後轉小寫，4～20 個半形英文字母、數字或底線，不區分大小寫。
- 暱稱：trim 後 1～30 個 Unicode code point，不接受換行與控制字元，不要求唯一。
- 密碼：8～64 個 ASCII `0x21`～`0x7E` 字元，不接受空白、中文或全形字元。
- 密碼使用 BCrypt；確認密碼不寫入資料庫。

## API 摘要

| Method | 路徑 | 用途 |
| --- | --- | --- |
| GET | `/api/auth/csrf` | 取得 CSRF token 與 header 名稱 |
| POST | `/api/auth/register` | 公開註冊 |
| POST | `/api/auth/login` | Spring Security form login |
| POST | `/api/auth/logout` | 登出並清除 Session |
| GET | `/api/members/me` | 取得本人最新資料 |
| GET | `/api/members` | 依帳號或暱稱搜尋會員 |
| PUT | `/api/members/me` | 修改本人帳號與暱稱 |
| PUT | `/api/members/me/password` | 修改本人密碼 |
| DELETE | `/api/members/me` | 刪除本人帳號 |

除 CSRF、註冊與登入外，會員 API 都需要有效 Session。POST、PUT、DELETE 都需要 CSRF header。API 只回傳帳號與暱稱，不回傳 id、密碼雜湊或 Security principal。

## 測試與建置

後端：

```powershell
cd backend
.\mvnw.cmd test
```

前端：

```powershell
cd frontend
npm ci
npm run build
```

後端測試使用 `backend/target/test-data` 下的獨立 SQLite 檔案，不使用 H2，也不會碰到示範資料庫。目前整合測試涵蓋註冊、BCrypt、登入／登出、CSRF、查詢特殊字元、翻頁、本人資料、改密碼、刪除及舊 Session。

## 需求解讀與已知限制

- 原題同時要求公開註冊與「新增必須登入」。本專案把公開註冊視為唯一例外，不另做登入後替他人新增會員的功能；這是本作業的需求解讀，並非出題方已確認。
- 修改及刪除只允許本人；查詢可看到其他會員的帳號與暱稱。
- 修改帳號或密碼只使目前 Session 失效，其他裝置的既有 Session 可能持續到過期；刪除會員後，所有舊 Session 下一次存取受保護 API 都會被拒絕。
- Session 閒置逾時為 30 分鐘；JSESSIONID 使用 HttpOnly 與 SameSite=Lax。本機 HTTP 不設定 Secure，HTTPS 環境應啟用 Secure。
- SQLite 與單連線池適合這個本機、小型面試示範，不宣稱適合高併發正式服務。
- 本專案只交付本機可執行原始碼，沒有 Docker、雲端部署、JWT、OAuth、管理員或角色系統。
