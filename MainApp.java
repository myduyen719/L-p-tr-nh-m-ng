package SMTP_Email_Simulator;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.Vector;

/**
 * MainApp: giao diện Swing đẹp, tích hợp:
 * - Đăng ký / Đăng nhập (lưu users vào mail.db directly)
 * - Soạn & gửi email (gọi SMTPClient)
 * - Inbox: hiển thị JTable, View, Delete, Search
 * - Khi đọc message -> giải mã bằng CryptoUtil.decrypt(...)
 *
 * Trước khi chạy: đảm bảo SMTPServer đang chạy để gửi email thực sự lưu vào DB.
 */
public class MainApp extends JFrame {
    private static final String DB_URL = "jdbc:sqlite:mail.db";

    private String currentUser = null;

    // UI components
    private JTextField loginUserField;
    private JPasswordField loginPassField;

    private JTextField toField;
    private JTextField subjectField;
    private JTextArea bodyArea;

    private DefaultTableModel inboxModel;
    private JTable inboxTable;
    private JTextField searchField;

    public MainApp() {
        setTitle("📧 SMTP Email Simulator");
        setSize(920, 640);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        initUI();
    }

    private void initUI() {
        // Top panel: header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(20, 110, 190));
        header.setBorder(new EmptyBorder(10, 10, 10, 10));
        JLabel title = new JLabel("SMTP Email Simulator", JLabel.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 20));
        title.setForeground(Color.WHITE);
        header.add(title, BorderLayout.WEST);

        JPanel authPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        authPanel.setOpaque(false);
        loginUserField = new JTextField(10);
        loginPassField = new JPasswordField(10);
        JButton loginBtn = new JButton("Login");
        JButton registerBtn = new JButton("Register");
        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setVisible(false);

        authPanel.add(new JLabel("User:"));
        authPanel.add(loginUserField);
        authPanel.add(new JLabel("Pwd:"));
        authPanel.add(loginPassField);
        authPanel.add(loginBtn);
        authPanel.add(registerBtn);
        authPanel.add(logoutBtn);

        header.add(authPanel, BorderLayout.EAST);
        add(header, BorderLayout.NORTH);

        // Center: split left send / right inbox
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(420);

        // Left: compose panel
        JPanel left = new JPanel(new BorderLayout());
        left.setBackground(new Color(250, 250, 255));
        left.setBorder(new EmptyBorder(12, 12, 12, 12));

        JPanel composeHeader = new JPanel(new GridLayout(3, 1, 6, 6));
        composeHeader.setOpaque(false);
        toField = new JTextField();
        subjectField = new JTextField();
        composeHeader.add(labeledPanel("To:", toField));
        composeHeader.add(labeledPanel("Subject:", subjectField));
        left.add(composeHeader, BorderLayout.NORTH);

        bodyArea = new JTextArea(15, 30);
        bodyArea.setLineWrap(true);
        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        bodyScroll.setBorder(BorderFactory.createTitledBorder("Message"));
        left.add(bodyScroll, BorderLayout.CENTER);

        JPanel leftButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        leftButtons.setOpaque(false);
        JButton sendBtn = new JButton("Send");
        sendBtn.setBackground(new Color(30, 144, 255));
        sendBtn.setForeground(Color.WHITE);
        JButton clearBtn = new JButton("Clear");
        clearBtn.setBackground(new Color(220, 20, 60));
        clearBtn.setForeground(Color.WHITE);
        leftButtons.add(clearBtn);
        leftButtons.add(sendBtn);
        left.add(leftButtons, BorderLayout.SOUTH);

        // Right: inbox panel
        JPanel right = new JPanel(new BorderLayout());
        right.setBorder(new EmptyBorder(12, 12, 12, 12));
        right.setBackground(new Color(245, 255, 250));

        JPanel inboxTop = new JPanel(new BorderLayout(6, 6));
        inboxTop.setOpaque(false);
        JLabel inboxLabel = new JLabel("Inbox");
        inboxLabel.setFont(new Font("Segoe UI", Font.BOLD, 16));
        inboxTop.add(inboxLabel, BorderLayout.WEST);

        JPanel searchPane = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        searchPane.setOpaque(false);
        searchField = new JTextField(14);
        JButton searchBtn = new JButton("Search");
        JButton refreshBtn = new JButton("Refresh");
        searchPane.add(searchField);
        searchPane.add(searchBtn);
        searchPane.add(refreshBtn);
        inboxTop.add(searchPane, BorderLayout.EAST);

        right.add(inboxTop, BorderLayout.NORTH);

        inboxModel = new DefaultTableModel(new String[]{"ID", "From", "Subject", "Date"}, 0) {
            public boolean isCellEditable(int r, int c) { return false; }
        };
        inboxTable = new JTable(inboxModel);
        inboxTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        inboxTable.setRowHeight(24);
        JScrollPane tableScroll = new JScrollPane(inboxTable);
        right.add(tableScroll, BorderLayout.CENTER);

        JPanel inboxButtons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        inboxButtons.setOpaque(false);
        JButton viewBtn = new JButton("View");
        JButton deleteBtn = new JButton("Delete");
        inboxButtons.add(viewBtn);
        inboxButtons.add(deleteBtn);
        right.add(inboxButtons, BorderLayout.SOUTH);

        // assemble
        split.setLeftComponent(left);
        split.setRightComponent(right);
        add(split, BorderLayout.CENTER);

        // Footer
        JLabel footer = new JLabel("Tip: Start SMTPServer first (java SMTPServer) then use this app.", JLabel.CENTER);
        footer.setBorder(new EmptyBorder(8, 8, 8, 8));
        add(footer, BorderLayout.SOUTH);

        // Event handlers
        // Register
        registerBtn.addActionListener(e -> {
            String u = loginUserField.getText().trim();
            String p = new String(loginPassField.getPassword());
            if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter username & password"); return; }
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("INSERT INTO users(username, password) VALUES (?, ?)");
                ps.setString(1, u);
                ps.setString(2, p);
                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "Register successful. You can login now.");
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Register failed: " + ex.getMessage());
            }
        });

        // Login
        loginBtn.addActionListener(e -> {
            String u = loginUserField.getText().trim();
            String p = new String(loginPassField.getPassword());
            if (u.isEmpty() || p.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter username & password"); return; }
            try (Connection conn = DriverManager.getConnection(DB_URL)) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM users WHERE username=? AND password=?");
                ps.setString(1, u);
                ps.setString(2, p);
                ResultSet rs = ps.executeQuery();
                if (rs.next()) {
                    currentUser = u;
                    loginUserField.setEnabled(false);
                    loginPassField.setEnabled(false);
                    loginBtn.setEnabled(false);
                    registerBtn.setEnabled(false);
                    logoutBtn.setVisible(true);
                    JOptionPane.showMessageDialog(this, "Login successful. Welcome " + currentUser);
                    loadInbox(null);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid credentials");
                }
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Login error: " + ex.getMessage());
            }
        });

        logoutBtn.addActionListener(e -> {
            currentUser = null;
            loginUserField.setEnabled(true);
            loginPassField.setEnabled(true);
            loginUserField.setText("");
            loginPassField.setText("");
            loginBtn.setEnabled(true);
            registerBtn.setEnabled(true);
            logoutBtn.setVisible(false);
            inboxModel.setRowCount(0);
            JOptionPane.showMessageDialog(this, "Logged out.");
        });

        // Send email
        sendBtn.addActionListener(e -> {
            if (currentUser == null) { JOptionPane.showMessageDialog(this, "Please login first"); return; }
            String to = toField.getText().trim();
            String subj = subjectField.getText().trim();
            String body = bodyArea.getText();
            if (to.isEmpty()) { JOptionPane.showMessageDialog(this, "Enter recipient username (must be registered)"); return; }

            SMTPClient client = new SMTPClient("localhost", SMTPServer.PORT);
            boolean ok = client.sendMail(currentUser, to, subj, body);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Email sent.");
                toField.setText(""); subjectField.setText(""); bodyArea.setText("");
                loadInbox(null);
            } else {
                JOptionPane.showMessageDialog(this, "Failed to send. Ensure SMTPServer is running.");
            }
        });

        clearBtn.addActionListener(e -> {
            toField.setText(""); subjectField.setText(""); bodyArea.setText("");
        });

        // Inbox actions
        refreshBtn.addActionListener(e -> loadInbox(null));
        searchBtn.addActionListener(e -> loadInbox(searchField.getText().trim()));
        viewBtn.addActionListener(e -> {
            int row = inboxTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an email"); return; }
            String id = inboxModel.getValueAt(row, 0).toString();
            viewEmail(id);
        });
        deleteBtn.addActionListener(e -> {
            int row = inboxTable.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Select an email"); return; }
            String id = inboxModel.getValueAt(row, 0).toString();
            deleteEmail(id);
            loadInbox(null);
        });
    }

    private JPanel labeledPanel(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(6, 6));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setPreferredSize(new Dimension(70, 24));
        p.add(l, BorderLayout.WEST);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void loadInbox(String keyword) {
        inboxModel.setRowCount(0);
        if (currentUser == null) return;
        String sql = "SELECT id, sender, subject, date FROM emails WHERE receiver=? ";
        if (keyword != null && !keyword.isEmpty()) sql += "AND (sender LIKE ? OR subject LIKE ?) ";
        sql += "ORDER BY id DESC";
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUser);
            if (keyword != null && !keyword.isEmpty()) {
                ps.setString(2, "%" + keyword + "%");
                ps.setString(3, "%" + keyword + "%");
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(String.valueOf(rs.getInt("id")));
                row.add(rs.getString("sender"));
                row.add(rs.getString("subject"));
                row.add(rs.getString("date"));
                inboxModel.addRow(row);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Load inbox error: " + ex.getMessage());
        }
    }

    private void viewEmail(String id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT * FROM emails WHERE id=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String from = rs.getString("sender");
                String to = rs.getString("receiver");
                String subj = rs.getString("subject");
                String date = rs.getString("date");
                String encrypted = rs.getString("message");
                String message = CryptoUtil.decrypt(encrypted);

                JTextArea area = new JTextArea();
                area.setEditable(false);
                area.setText("From: " + from + "\nTo: " + to + "\nSubject: " + subj + "\nDate: " + date + "\n\n" + message);
                area.setCaretPosition(0);
                area.setLineWrap(true);

                JOptionPane.showMessageDialog(this, new JScrollPane(area), "Email Detail", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "View error: " + ex.getMessage());
        }
    }

    private void deleteEmail(String id) {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("DELETE FROM emails WHERE id=?")) {
            ps.setString(1, id);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Deleted.");
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Delete error: " + ex.getMessage());
        }
    }

    public static void main(String[] args) {
        // Ensure DB exists & tables created (in case server not run first)
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement st = conn.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS users (id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT UNIQUE, password TEXT)");
            st.execute("CREATE TABLE IF NOT EXISTS emails (id INTEGER PRIMARY KEY AUTOINCREMENT, sender TEXT, receiver TEXT, subject TEXT, message TEXT, date TEXT)");
        } catch (SQLException ignored) {}

        SwingUtilities.invokeLater(() -> {
            MainApp app = new MainApp();
            app.setVisible(true);
        });
    }
}
