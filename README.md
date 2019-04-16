目的: 每日定時排程發送Heroku多個帳號的Free Dyno Usage資訊, 避免使用量超過限額

機制: 將此應用程式部署在Heroku上, 透過UptimeRobot定時監控避免使用Heroku免費帳號的休眠問題

Usage - 透過Selenium + Headless Chrome自動登入帳號後獲取Billing頁面資料,

執行客製化js腳本調整目標表格大小與內容, 擷取畫面合併為同一張圖片上傳至Cloudinary, 用SendGrid寄出通知信

ExecuteController - 手動執行Task使用. 將Spring ApplicationContext中符合名稱的Bean取出呼叫execute方法執行
