# L-p-tr-nh-m-ng
<h2 align="center">
    <a href="https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin">
    🎓 Faculty of Information Technology (DaiNam University)
    </a>
</h2>
<h2 align="center">
   GỬI EMAIL MÔ PHỎNG SMTP QUA SOCKET
</h2>
<div align="center">
    <p align="center">
        <img src="docs/aiotlab_logo.png" alt="AIoTLab Logo" width="170"/>
        <img src="docs/fitdnu_logo.png" alt="AIoTLab Logo" width="180"/>
        <img src="docs/dnu_logo.png" alt="DaiNam University Logo" width="200"/>
    </p>

[![AIoTLab](https://img.shields.io/badge/AIoTLab-green?style=for-the-badge)](https://www.facebook.com/DNUAIoTLab)
[![Faculty of Information Technology](https://img.shields.io/badge/Faculty%20of%20Information%20Technology-blue?style=for-the-badge)](https://dainam.edu.vn/vi/khoa-cong-nghe-thong-tin)
[![DaiNam University](https://img.shields.io/badge/DaiNam%20University-orange?style=for-the-badge)](https://dainam.edu.vn)

</div>

---

## 📖 1. Giới thiệu  

- **Tên đề tài:** Gửi email mô phỏng SMTP qua Socket  

- **Mục tiêu:**  
  Hiểu cơ chế hoạt động cơ bản của giao thức **SMTP**.  

  Thực hành lập trình **Socket trong Java** để mô phỏng quá trình gửi/nhận email.  

  Xây dựng mô hình **Client – Server** đơn giản:  

  - **Client:** gửi lệnh SMTP và nội dung email.  
  - **Server:** phản hồi các mã trạng thái, lưu và hiển thị email.  

  - **Mục tiêu:** Giúp sinh viên nắm được cách thức hoạt động của các giao thức tầng ứng dụng.  

  Ứng dụng có thể mở rộng vào các bài toán lập trình mạng nâng cao:  
  - Xây dựng **mail relay**  
  - Bảo mật bằng **TLS/SSL**  
  - Xác thực tài khoản, quản lý hệ thống mail server  

---

## 🏗️ Thành phần hệ thống  

<p align="center">
  <img src="<img width="567" height="375" alt="image" src="https://github.com/user-attachments/assets/2f762ab2-e1a6-4e5c-a2ad-c082f0935ce4" />
"/>
</p>


---
## 2. ⚙️ Công nghệ sử dụng

| Công nghệ | Chi tiết |
| :--- | :--- |
| **Ngôn ngữ lập trình** | Java (JDK 21) |
| **Giao diện người dùng** | Java Swing |
| **Giao thức** | TCP Socket (Mô phỏng SMTP) |
| **Cơ sở dữ liệu** | SQLite (Sử dụng JDBC) |

---

## 🖼️ 3. Một số hình ảnh hệ thống 

<p align="center">
    <img width="1920" height="1013" alt="Screenshot (344)" src="<img width="1831" height="967" alt="image" src="https://github.com/user-attachments/assets/4e508601-8abd-465b-89bc-a192003c2ec9" />
" />
    <em>Giao diện đăng nhập, đăng ký</em><br/>
</p>



<p align="center">
    <img width="1920" height="1009" alt="Screenshot (345)" src="<img width="1572" height="980" alt="image" src="https://github.com/user-attachments/assets/c71dbb3d-aa5c-43af-8fc8-7d6b52fe774b" />
" />
    <em>Giao diện Gửi và Nhận Email</em><br/>
</p>
---
## 4. 🛠️ Trạng thái Hiện tại & Khắc phục Sự cố Socket (Quan trọng)

Trạng thái hiện tại tập trung vào việc khắc phục các sự cố triển khai Server (`SMTPServer.java`). Đây là vấn đề kỹ thuật dai dẳng và cách giải quyết triệt để:

* **Vấn đề Cốt lõi**: Lỗi **`java.net.BindException: Address already in use: bind`** liên tục xảy ra.
    * Lỗi này cho thấy `ServerSocket` không thể liên kết với cổng do một tiến trình cũ vẫn đang chiếm dụng cổng đó (hiện tại là **9090**).

* **Giải pháp Đã áp dụng**:
    1.  **Chuyển Cổng & Tái sử dụng Địa chỉ**: Cổng Server đã được chuyển cố định sang **9090** và thêm `serverSocket.setReuseAddress(true)` trong mã `SMTPServer.java`.
    2.  **Buộc Dừng Tiến trình (Giải pháp Hệ thống)**: Để khắc phục triệt để lỗi, đã sử dụng Command Prompt để xác định PID (ID tiến trình) đang chiếm dụng cổng và buộc dừng nó.
        * Lệnh kiểm tra: `netstat -ano | findstr :9090`
        * Lệnh buộc dừng: `taskkill /PID [PID] /F`

---
 ## 5. 🚀 Hướng dẫn Khởi động và Kiểm tra

Thực hiện các bước sau theo thứ tự để khởi động và kiểm tra đầy đủ ứng dụng:

1.  **Chạy Server**: Khởi động **`SMTPServer.java`** (đảm bảo cổng 9090 đã được giải phóng).
2.  **Khởi động Client**: Khởi động **`LoginFrame.java`**.
3.  **Kiểm tra Đăng ký & Đăng nhập**: Tạo tài khoản mới, sau đó Đăng nhập.
4.  **Kiểm tra Gửi/Nhận Mail**: Sử dụng giao diện chính để gửi thư và xác nhận Server xử lý đúng các lệnh SMTP.

  ## 💬 6. Liên hệ
📧 Email: myduyn71@gmail.com

---

<div align="center">

Thực hiện bởi Lê Thị Mỹ Duyên - CNTT 16-01, trường Đại học Đại Nam

Website • GitHub • Contact Me

</div>
 

### 🚀 Clone source code  

Mở terminal / cmd và chạy lệnh sau:  



