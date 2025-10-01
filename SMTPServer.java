package SMTP_Email_Simulator;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * SMTPServer: mô phỏng server SMTP đơn giản.
 * - Lắng nghe port 2525
 * - Nhận MAIL FROM, RCPT TO, SUBJECT, DATA ... .
 * - Mã hóa nội dung bằng CryptoUtil.encrypt(...) trước khi lưu vào SQLite.
 * - Tạo DB (users, emails) tự động nếu chưa có.
 * - Hỗ trợ FETCH <username> để client tải inbox.
 */
public class SMTPServer {
    public static final int PORT = 2525;
    public static final String DB_URL = "jdbc:sqlite:mail.db";

    public static void main(String[] args) {
        initDatabase();
        System.out.println("📡 SMTP Server starting on port " + PORT + " ...");
        try (ServerSocket ss = new ServerSocket(PORT)) {
            while (true) {
                Socket s = ss.accept();
                new Thread(new Handler(s)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void initDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement()) {
            st.execute("PRAGMA foreign_keys = ON;");
            st.execute("CREATE TABLE IF NOT EXISTS users (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS emails (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT, receiver TEXT, subject TEXT, message TEXT, date TEXT)");
            System.out.println("✅ Database ready (mail.db)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    static class Handler implements Runnable {
        private Socket socket;

        Handler(Socket socket) { this.socket = socket; }

        public void run() {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                 Connection conn = DriverManager.getConnection(DB_URL)) {

                out.println("220 SMTP Simulator Ready"); // greeting

                String line;
                String sender = null, receiver = null, subject = null;
                StringBuilder dataBuilder = new StringBuilder();
                boolean dataMode = false;

                while ((line = in.readLine()) != null) {
                    if (dataMode) {
                        if (line.equals(".")) { // end of data
                            dataMode = false;
                            String encrypted = CryptoUtil.encrypt(dataBuilder.toString());
                            saveEmail(conn, sender, receiver, subject, encrypted);
                            dataBuilder.setLength(0);
                            out.println("250 Message accepted");
                        } else {
                            if (line.startsWith("..")) line = line.substring(1); // dot-stuffing
                            dataBuilder.append(line).append("\n");
                        }
                        continue;
                    }

                    String cmd = line.toUpperCase();
                    if (cmd.startsWith("HELO") || cmd.startsWith("EHLO")) {
                        out.println("250 Hello");
                    } else if (cmd.startsWith("MAIL FROM:")) {
                        sender = line.substring(10).trim();
                        out.println("250 OK");
                    } else if (cmd.startsWith("RCPT TO:")) {
                        receiver = line.substring(8).trim();
                        out.println("250 OK");
                    } else if (cmd.startsWith("SUBJECT:")) {
                        subject = line.substring(8).trim();
                        out.println("250 OK");
                    } else if (cmd.equals("DATA")) {
                        out.println("354 End data with <CR><LF>.<CR><LF>");
                        dataMode = true;
                    } else if (cmd.startsWith("FETCH")) {
                        String user = line.substring(5).trim();
                        fetchEmails(conn, user, out);
                    } else if (cmd.equals("QUIT")) {
                        out.println("221 Bye");
                        break;
                    } else {
                        out.println("502 Command not implemented");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try { socket.close(); } catch (IOException ignored) {}
            }
        }

        private void saveEmail(Connection conn, String sender, String receiver, String subject, String encryptedMessage) {
            if (sender == null || receiver == null) return;
            String sql = "INSERT INTO emails(sender, receiver, subject, message, date) VALUES (?,?,?,?,?)";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, sender);
                ps.setString(2, receiver);
                ps.setString(3, subject == null ? "" : subject);
                ps.setString(4, encryptedMessage);
                ps.setString(5, new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()));
                ps.executeUpdate();
                System.out.println("📩 Saved email from=" + sender + " to=" + receiver + " subj=" + subject);
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }

        private void fetchEmails(Connection conn, String user, PrintWriter out) {
            String sql = "SELECT sender, subject, message, date FROM emails WHERE receiver = ?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setString(1, user);
                ResultSet rs = ps.executeQuery();
                boolean hasMsg = false;
                while (rs.next()) {
                    hasMsg = true;
                    String sender = rs.getString("sender");
                    String subject = rs.getString("subject");
                    String encrypted = rs.getString("message");
                    String message = CryptoUtil.decrypt(encrypted);
                    String date = rs.getString("date");

                    out.println("FROM: " + sender);
                    out.println("SUBJECT: " + subject);
                    out.println("DATE: " + date);
                    out.println("BODY:\n" + message);
                    out.println("---");
                }
                if (!hasMsg) {
                    out.println("No messages for " + user);
                }
                out.println("250 End of messages");
            } catch (Exception e) {
                out.println("550 Error fetching messages");
                e.printStackTrace();
            }
        }
    }
}
