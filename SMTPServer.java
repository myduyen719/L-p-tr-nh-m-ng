package smtpapp;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.Statement;

public class SMTPServer {
    
    public static final int PORT = 9090; 
    public static final String DB_URL = "jdbc:sqlite:mail.db";

    public static void main(String[] args) {
        
        System.out.println("SMTP Server đang khởi tạo...");
        initDB(); 

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            serverSocket.setReuseAddress(true); 
            
            System.out.println("✅ SMTP Server đang chạy trên PORT " + PORT + ". Đang lắng nghe...");
            
            while (true) {
                Socket client = serverSocket.accept();
                System.out.println("🔗 Client mới đã kết nối: " + client.getInetAddress().getHostAddress());
                
                Thread clientHandler = new Thread(new ClientHandler(client));
                clientHandler.start();
            }

        } catch (IOException e) {
            System.err.println("❌ Lỗi Server không thể khởi động. Vui lòng kiểm tra cổng " + PORT + " có bị chiếm dụng không.");
            e.printStackTrace();
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException closeE) {
                    System.err.println("Lỗi khi đóng ServerSocket: " + closeE.getMessage());
                }
            }
        }
    }

    private static void initDB() {
        try {
            Class.forName("org.sqlite.JDBC");
        } catch (ClassNotFoundException e) {
            System.err.println("Lỗi: Không tìm thấy driver SQLite.");
            e.printStackTrace();
        }

        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            
            // 1. Tạo bảng emails
            String sqlEmails = "CREATE TABLE IF NOT EXISTS emails (\n"
                   + " id INTEGER PRIMARY KEY AUTOINCREMENT,\n"
                   + " sender TEXT NOT NULL,\n"
                   + " receiver TEXT NOT NULL,\n"
                   + " subject TEXT,\n"
                   + " message TEXT,\n"
                   + " date TEXT\n"
                   + ");";
            stmt.execute(sqlEmails);
            
            // 2. Tạo bảng users (quan trọng cho chức năng đăng nhập/đăng ký)
            String sqlUsers = "CREATE TABLE IF NOT EXISTS users (\n"
                + " username TEXT PRIMARY KEY NOT NULL,\n"
                + " password TEXT NOT NULL\n"
                + ");";
            stmt.execute(sqlUsers);
            
            System.out.println("📦 CSDL đã sẵn sàng.");
        } catch (SQLException e) {
            System.err.println("Lỗi CSDL khi tạo bảng: " + e.getMessage());
        }
    }
}


// LỚP CLIENTHANDLER ĐƯỢC ĐẶT NGAY BÊN DƯỚI LỚP SMTPSERVER
class ClientHandler implements Runnable {
    private final Socket clientSocket;
    private BufferedReader in;
    private PrintWriter out;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    private void sendResponse(String response) {
        out.println(response);
    }

    @Override
    public void run() {
        try {
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            out = new PrintWriter(clientSocket.getOutputStream(), true);

            sendResponse("220 SMTPApp Service ready");

            String clientCommand;
            while ((clientCommand = in.readLine()) != null) {
                
                // Khắc phục lỗi tiềm ẩn: Xử lý lệnh rỗng/chỉ có khoảng trắng
                String trimmedCommand = clientCommand.trim();
                if (trimmedCommand.isEmpty()) {
                    continue; 
                }
                
                System.out.println("C: " + trimmedCommand);
                String commandType = trimmedCommand.toUpperCase().split(" ")[0];

                switch (commandType) {
                    case "EHLO":
                    case "HELO":
                        sendResponse("250 Hello");
                        break;
                    case "MAIL": 
                    case "RCPT": 
                        sendResponse("250 OK");
                        break;
                    case "DATA":
                        sendResponse("354 Start mail input; end with <CRLF>.<CRLF>");
                        receiveData(); 
                        break;
                    case "QUIT":
                        sendResponse("221 Bye");
                        return;
                    default:
                        sendResponse("500 Command not recognized");
                        break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client disconnected unexpectedly: " + e.getMessage());
        } finally {
            try {
                if (clientSocket != null) clientSocket.close();
            } catch (IOException e) { /* ignored */ }
        }
    }

    private void receiveData() throws IOException {
        String line = null;
        // Sửa lỗi logic: Kiểm tra null trước, sau đó mới kiểm tra dấu chấm.
        while ((line = in.readLine()) != null && !line.equals(".")) { 
            // Nội dung thư đã được đọc
        }
        
        if (line != null && line.equals(".")) { 
             sendResponse("250 OK: Message accepted for delivery");
        } else {
             throw new IOException("Client disconnected during DATA phase.");
        }
    }
}
