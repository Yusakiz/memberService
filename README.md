# 簡易會員平台

這是會員平台面試實作的精簡版本，重點是讓程式流程容易閱讀與解說，同時完成註冊、登入及會員資料 CRUD。

## 技術與結構

- 後端：Java 17、Spring Boot、Spring MVC、Spring Security、JdbcTemplate
- 前端：Vue 3、JavaScript、Vite
- 資料庫：SQLite（透過 JDBC，自動建立於 `backend/data/members.db`）
- 驗證：Session 登入、BCrypt 密碼雜湊、CSRF 防護

後端刻意只保留 8 個正式 Java 類別：Controller 接收請求、Service 處理規則、DAO 執行 SQL、Model 表示會員資料；其餘只有啟動、安全設定與統一錯誤處理。前端不使用 Router，由 `App.vue` 切換登入與會員頁面。

## 功能

- 註冊會員並檢查帳號、姓名、密碼
- 登入／登出與歡迎頁
- 依帳號或姓名查詢會員
- 修改自己的帳號與姓名
- 刪除自己的帳號
- 未登入時不能查詢、修改或刪除會員資料

註冊必須允許尚未登入的使用者使用，因此視為題目「新增必須登入」的合理例外；既有會員資料的修改與刪除都有登入保護。

## 啟動方式（PowerShell）

先在專案根目錄設定本專案自帶的工具：

```powershell
$env:JAVA_HOME = "$PWD\.tools\jdk17\jdk-17.0.20.1+1"
$env:Path = "$env:JAVA_HOME\bin;$PWD\.tools\node22\node-v22.23.3-win-x64;$env:Path"
```

終端機一（後端）：

```powershell
cd backend
.\mvnw.cmd spring-boot:run
```

終端機二（前端）：

```powershell
cd frontend
npm.cmd run dev
```

開啟 <http://localhost:5173>。後端 API 位於 <http://localhost:8080>，開發期間由 Vite 代理 `/api`。

## 驗證指令

```powershell
cd backend
.\mvnw.cmd test

cd ..\frontend
npm.cmd run build
```

整合測試涵蓋完整會員流程、重複帳號、未登入阻擋、CSRF 與 BCrypt 密碼雜湊。
