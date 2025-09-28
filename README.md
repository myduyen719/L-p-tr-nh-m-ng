# L-p-tr-nh-m-ng
LapTrinhMang
<h1 align="center">📧 GỬI EMAIL MÔ PHỎNG SMTP QUA SOCKET</h1>

<p align="center">
  <img src="https://upload.wikimedia.org/wikipedia/commons/9/99/Socket-Icon.png" alt="SMTP Socket" width="120"/>
</p>

<p align="center">
  <img src="<img width="1508" height="1687" alt="image" src="https://github.com/user-attachments/assets/c0290238-8d45-481b-9c3b-0bc5acfb95a5" />
" alt="AIoT Lab" width="100"/>
  <img src="<img width="3693" height="3693" alt="image" src="https://github.com/user-attachments/assets/393e0772-ab21-4ce2-9987-9061b8b45e2f" />
" alt="DNU - Khoa Công nghệ thông tin" width="200"/>
  <img src="<img width="1128" height="1024" alt="image" src="https://github.com/user-attachments/assets/94fe3669-f49d-422f-b1a7-2716225e1cde" />
" />
" alt="Dai Nam University" width="200"/>
</p>

---

<h1 align="center">📧 GỬI EMAIL MÔ PHỎNG SMTP QUA SOCKET</h1>

---

## 📖 1. Giới thiệu  

- **Tên đề tài:** Gửi email mô phỏng SMTP qua Socket  

- **Mục tiêu:**  
  Hiểu cơ chế hoạt động cơ bản của giao thức **SMTP**.  

  Thực hành lập trình **Socket trong Java** để mô phỏng quá trình gửi/nhận email.  

  Xây dựng mô hình **Client – Server** đơn giản:  

  - **Client:** gửi lệnh SMTP và nội dung email.  
  - **Server:** phản hồi các mã trạng thái, lưu và hiển thị email.  

  - **Ý nghĩa:** Sinh viên nắm được cách thức hoạt động của các giao thức tầng ứng dụng.  

  Ứng dụng có thể mở rộng vào các bài toán lập trình mạng nâng cao:  
  - Xây dựng **mail relay**  
  - Bảo mật bằng **TLS/SSL**  
  - Xác thực tài khoản, quản lý hệ thống mail server  

---

## 🏗️ Thành phần hệ thống  

<p align="center">
  <img src="docs/images/system-architecture.png" alt="SMTP Socket Architecture" width="500"/>
</p>


## ⚙️ 2. Công nghệ sử dụng
- Ngôn ngữ: **Java / Python**  
- Giao thức: **TCP/IP, SMTP**  
- Công cụ hỗ trợ:  
  - `Socket Programming`  
  - `Swing / Tkinter` cho giao diện (nếu có)  

---

## 🏗️ 3. Cấu trúc dự án  

```bash
📂 smtp-socket-demo
 ┣ 📂 src
 ┃ ┣ 📜 Server.java
 ┃ ┣ 📜 Client.java
 ┃ ┗ 📜 MailHandler.java
 ┣ 📜 README.md
 ┣ 📜 LICENSE
 ┗ 📜 .gitignore
