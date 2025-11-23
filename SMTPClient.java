package smtpapp;

import java.io.*;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.*;

public class SMTPClient {
    private static final String DB_URL = "jdbc:sqlite:mail.db";
    
    // Cổng đã sửa thành 9090 để kết nối đúng với SMTPServer đang chạy
    private static final int DEFAULT_PORT = 9090; 

    private String serverHost;
    private int serverPort;
    private Socket socket;
    private BufferedReader in;
    private PrintWriter out;

    // Constructor đã được thay đổi để sử dụng cổng mặc định 9090 nếu chỉ truyền host
    public SMTPClient(String host) {
        this(host, DEFAULT_PORT);
    }
    
    public SMTPClient(String host, int port) {
        this.serverHost = host;
        this.serverPort = port;
    }

    private boolean connect() {
        try {
            socket = new Socket(serverHost, serverPort);
            // Thiết lập InputStream và OutputStream với mã hóa UTF-8
            in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            
            // Đọc phản hồi 220 ban đầu từ server
            String initialResponse = readResponse();
            if (!initialResponse.startsWith("220")) {
                System.err.println("Server không sẵn sàng: " + initialResponse);
                return false;
            }
            return true;
        } catch (IOException e) {
            System.err.println("Lỗi kết nối tới SMTP Server (kiểm tra SMTPServer đã chạy chưa): " + e.getMessage());
            return false;
        }
    }

    private void disconnect() {
        try {
            if (socket != null && !socket.isClosed()) {
                // Gửi QUIT và đóng socket
                sendCommand("QUIT");
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khi đóng kết nối client: " + e.getMessage());
        }
    }

    private String sendCommand(String command) throws IOException {
        System.out.println("C: " + command);
        out.println(command);
        return readResponse();
    }

    private String readResponse() throws IOException {
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null) {
            response.append(line).append("\n");
            // Logic đọc phản hồi đa dòng (chính xác theo giao thức SMTP)
            if (line.length() > 3 && (line.charAt(3) != '-')) {
                break;
            }
        }
        String responseStr = response.toString().trim();
        System.out.println("S: " + responseStr);
        return responseStr;
    }

    public boolean sendMail(String from, String to, String subject, String body) {
        if (!connect()) return false;

        try {
            // Chuỗi giao thức SMTP tiêu chuẩn: EHLO -> MAIL FROM -> RCPT TO -> DATA
            if (!sendCommand("EHLO localhost").startsWith("250")) return false;
            if (!sendCommand("MAIL FROM:<" + from + ">").startsWith("250")) return false;
            if (!sendCommand("RCPT TO:<" + to + ">").startsWith("250")) return false;
            
            if (!sendCommand("DATA").startsWith("354")) return false;

            // Gửi Headers
            out.println("From: " + from);
            out.println("To: " + to);
            out.println("Subject: " + subject);
            out.println("Date: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            out.println("Content-Type: text/plain; charset=UTF-8");
            out.println(); // Dòng trống ngăn cách header và body
            
            // Gửi Body (Mã hóa nội dung)
            // YÊU CẦU: Cần đảm bảo lớp CryptoUtil.java đã tồn tại trong package smtpapp
            String encryptedBody = CryptoUtil.encrypt(body); 
            out.println(encryptedBody);
            
            // Kết thúc DATA bằng dấu chấm
            if (!sendCommand(".").startsWith("250")) return false;

            // ** QUAN TRỌNG: Lưu mail vào DB (Mô phỏng việc Server nhận và lưu mail) **
            saveToDatabase(to, from, subject, encryptedBody);

            return true;
        } catch (IOException e) {
            System.err.println("Lỗi giao tiếp SMTP: " + e.getMessage());
            return false;
        } finally {
            disconnect();
        }
    }

    private void saveToDatabase(String receiver, String sender, String subject, String encryptedMessage) {
         String sql = "INSERT INTO emails (sender, receiver, subject, message, date) VALUES (?, ?, ?, ?, ?)";
         try (Connection conn = DriverManager.getConnection(DB_URL);
              PreparedStatement ps = conn.prepareStatement(sql)) {

             ps.setString(1, sender);
             ps.setString(2, receiver);
             ps.setString(3, subject);
             ps.setString(4, encryptedMessage);
             ps.setString(5, LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
             ps.executeUpdate();
             
             System.out.println("Mail đã được lưu vào DB (Mô phỏng Server đã nhận).");

         } catch (SQLException e) {
             System.err.println("Lỗi CSDL khi lưu email: " + e.getMessage());
             e.printStackTrace();
         }
    }
}
