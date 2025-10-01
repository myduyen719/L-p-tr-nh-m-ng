package SMTP_Email_Simulator;
import java.io.*;
import java.net.Socket;

/**
 * SMTPClient: Gửi email hoặc tải inbox từ SMTPServer.
 * - sendMail: gửi email theo SMTP mô phỏng
 * - fetchMail: tải inbox (FETCH <username>)
 */
public class SMTPClient {
    private final String host;
    private final int port;

    public SMTPClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public boolean sendMail(String from, String to, String subject, String body) {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine(); // greeting

            out.println("HELO localhost");
            in.readLine();

            out.println("MAIL FROM: " + from);
            in.readLine();

            out.println("RCPT TO: " + to);
            in.readLine();

            out.println("SUBJECT: " + (subject == null ? "" : subject));
            in.readLine();

            out.println("DATA");
            in.readLine(); // 354

            BufferedReader br = new BufferedReader(new StringReader(body == null ? "" : body));
            String ln;
            while ((ln = br.readLine()) != null) {
                if (ln.startsWith(".")) ln = "." + ln;
                out.println(ln);
            }
            out.println(".");
            in.readLine(); // 250

            out.println("QUIT");
            in.readLine();

            return true;
        } catch (Exception ex) {
            ex.printStackTrace();
            return false;
        }
    }

    public void fetchMail(String user) {
        try (Socket socket = new Socket(host, port);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true)) {

            in.readLine(); // greeting

            out.println("HELO localhost");
            in.readLine();

            out.println("FETCH " + user);

            String line;
            while ((line = in.readLine()) != null) {
                if (line.equals("250 End of messages")) break;
                System.out.println(line);
            }

            out.println("QUIT");
            in.readLine();

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
