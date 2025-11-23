package smtpapp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.Vector;

public class MainApp extends JFrame {
    private static final String DB_URL = "jdbc:sqlite:mail.db";
    private String currentUser = null;

    private JTextField toField, subjectField, searchField;
    private JTextArea bodyArea;
    private DefaultTableModel inboxModel;
    private JTable inboxTable;
    private JLabel statusLabel;
    private static final int SMTP_PORT = 9090; // Cổng Server

    // Constructor nhận username từ LoginFrame
    public MainApp(String currentUser) {
        this.currentUser = currentUser;
        setupLookAndFeel();
        setTitle("SMTP Email Simulator - Chào " + currentUser);
        setSize(1250, 780);
        setMinimumSize(new Dimension(1000, 600));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        initUI();
        loadInbox(null); // Tự động load inbox khi mở
    }

    private void setupLookAndFeel() {
        try {
            UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }
        UIManager.put("nimbusBase", new Color(20, 100, 180));
        UIManager.put("nimbusSelectionBackground", new Color(0, 120, 215));
        UIManager.put("control", new Color(248, 250, 252));
    }

    private void initUI() {
        setLayout(new BorderLayout());

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0, 105, 192));
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JLabel title = new JLabel("GỬI MAIL", JLabel.LEFT);
        title.setFont(new Font("Segoe UI", Font.BOLD, 30));
        title.setForeground(Color.WHITE);
        title.setIcon(createMailIcon(32));
        header.add(title, BorderLayout.WEST);

        JLabel userLabel = new JLabel("Xin chào: " + currentUser);
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 18));
        userLabel.setForeground(Color.WHITE);
        header.add(userLabel, BorderLayout.EAST);

        add(header, BorderLayout.NORTH);

        // Main split
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        split.setDividerLocation(540);
        split.setContinuousLayout(true);
        split.setBorder(null);

        split.setLeftComponent(createComposePanel());
        split.setRightComponent(createInboxPanel());
        add(split, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(new Color(248, 249, 250));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 200, 200)));
        statusLabel = new JLabel(" Đã đăng nhập: " + currentUser + " • SMTPServer đang chạy");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(new Color(80, 80, 80));
        statusLabel.setBorder(new EmptyBorder(8, 16, 8, 16));
        footer.add(statusLabel, BorderLayout.WEST);
        add(footer, BorderLayout.SOUTH);
    }

    private JPanel createComposePanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(252, 254, 255));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new GridBagLayout());
        top.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 0, 10, 0);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;

        gbc.gridy = 0;
        top.add(createLabeledField("Người nhận:", toField = new JTextField(30)), gbc);
        gbc.gridy++;
        top.add(createLabeledField("Tiêu đề:", subjectField = new JTextField(30)), gbc);

        panel.add(top, BorderLayout.NORTH);

        bodyArea = new JTextArea(20, 50);
        bodyArea.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(bodyArea);
        scroll.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createLineBorder(new Color(180, 180, 180)),
                " Nội dung thư ", 0, 0, new Font("Segoe UI", Font.BOLD, 13), new Color(60, 60, 60)));
        panel.add(scroll, BorderLayout.CENTER);

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        buttons.setOpaque(false);
        JButton sendBtn = createModernButton("Gửi Email", new Color(0, 122, 255));
        sendBtn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        sendBtn.setPreferredSize(new Dimension(160, 46));
        JButton clearBtn = createModernButton("Xóa trắng", new Color(108, 117, 125));
        clearBtn.setPreferredSize(new Dimension(120, 46));

        buttons.add(clearBtn);
        buttons.add(sendBtn);
        panel.add(buttons, BorderLayout.SOUTH);

        sendBtn.addActionListener(e -> sendEmail());
        clearBtn.addActionListener(e -> {
            toField.setText("");
            subjectField.setText("");
            bodyArea.setText("");
        });

        return panel;
    }

    private JPanel createInboxPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(new Color(248, 250, 252));
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel title = new JLabel("Hộp thư đến");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(new Color(0, 105, 192));
        top.add(title, BorderLayout.WEST);

        JPanel searchBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        searchBar.setOpaque(false);
        searchField = new JTextField(22);
        searchField.setPreferredSize(new Dimension(220, 38));
        JButton searchBtn = createModernButton("Tìm kiếm", new Color(0, 122, 255));
        JButton refreshBtn = createModernButton("Làm mới", new Color(40, 167, 69));
        searchBar.add(new JLabel("Tìm:"));
        searchBar.add(searchField);
        searchBar.add(searchBtn);
        searchBar.add(refreshBtn);
        top.add(searchBar, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        inboxModel = new DefaultTableModel(new String[]{"ID", "Từ", "Tiêu đề", "Ngày gửi"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        inboxTable = new JTable(inboxModel);
        inboxTable.setRowHeight(38);
        inboxTable.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        inboxTable.getTableHeader().setBackground(new Color(0, 105, 192));
        inboxTable.getTableHeader().setForeground(Color.WHITE);
        inboxTable.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        inboxTable.setSelectionBackground(new Color(173, 216, 255));

        // Ẩn cột ID (cột 0)
        inboxTable.getColumnModel().getColumn(0).setPreferredWidth(0);
        inboxTable.getColumnModel().getColumn(0).setMinWidth(0);
        inboxTable.getColumnModel().getColumn(0).setMaxWidth(0);

        JScrollPane tableScroll = new JScrollPane(inboxTable);
        tableScroll.setBorder(BorderFactory.createLineBorder(new Color(200, 200, 200)));
        panel.add(tableScroll, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        actions.setOpaque(false);
        JButton viewBtn = createModernButton("Xem", new Color(0, 122, 255));
        JButton deleteBtn = createModernButton("Xóa", new Color(220, 53, 69));
        actions.add(viewBtn);
        actions.add(deleteBtn);
        panel.add(actions, BorderLayout.SOUTH);

        refreshBtn.addActionListener(e -> loadInbox(null));
        searchBtn.addActionListener(e -> loadInbox(searchField.getText().trim()));
        viewBtn.addActionListener(e -> viewSelectedEmail());
        deleteBtn.addActionListener(e -> deleteSelectedEmail());

        return panel;
    }

    private void sendEmail() {
        String to = toField.getText().trim();
        String subject = subjectField.getText();
        String body = bodyArea.getText();

        if (to.isEmpty() || !to.contains("@")) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập người nhận hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        SMTPClient client = new SMTPClient("localhost", SMTP_PORT);
        boolean success = client.sendMail(currentUser, to, subject, body);

        if (success) {
            JOptionPane.showMessageDialog(this, "Gửi email thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            toField.setText(""); subjectField.setText(""); bodyArea.setText("");
            loadInbox(null);
        } else {
            JOptionPane.showMessageDialog(this, "Gửi thất bại! Kiểm tra SMTPServer đã chạy chưa.", "Lỗi", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void loadInbox(String keyword) {
        inboxModel.setRowCount(0);
        String sql = keyword == null || keyword.isEmpty()
                ? "SELECT id, sender, subject, date FROM emails WHERE receiver = ? ORDER BY date DESC"
                : "SELECT id, sender, subject, date FROM emails WHERE receiver = ? AND (sender LIKE ? OR subject LIKE ?) ORDER BY date DESC";

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, currentUser);
            if (keyword != null && !keyword.isEmpty()) {
                String searchPattern = "%" + keyword + "%";
                ps.setString(2, searchPattern);
                ps.setString(3, searchPattern);
            }
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                inboxModel.addRow(new Object[]{
                    rs.getInt("id"),
                    rs.getString("sender"),
                    rs.getString("subject"),
                    rs.getString("date")
                });
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Lỗi tải Hộp thư đến: " + e.getMessage(), "Lỗi CSDL", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void viewSelectedEmail() {
        int row = inboxTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một email!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Lấy ID từ cột ẩn (index 0)
        int id = (int) inboxModel.getValueAt(row, 0); 
        String from = (String) inboxModel.getValueAt(row, 1);
        String subject = (String) inboxModel.getValueAt(row, 2);
        String date = (String) inboxModel.getValueAt(row, 3);

        try (Connection conn = DriverManager.getConnection(DB_URL);
             PreparedStatement ps = conn.prepareStatement("SELECT message FROM emails WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String encrypted = rs.getString("message");
                String decrypted = CryptoUtil.decrypt(encrypted);
                EmailFrame.showEmail(this, from, subject, date, decrypted);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Không thể xem email: Lỗi CSDL.", "Lỗi", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    private void deleteSelectedEmail() {
        int row = inboxTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn email để xóa!", "Thông báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int id = (int) inboxModel.getValueAt(row, 0);
        int confirm = JOptionPane.showConfirmDialog(this, "Xóa email này?", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            try (Connection conn = DriverManager.getConnection(DB_URL);
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM emails WHERE id = ?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                loadInbox(null);
                JOptionPane.showMessageDialog(this, "Đã xóa email!");
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Không thể xóa email: Lỗi CSDL.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    // Helper methods (createModernButton, createLabeledField, createMailIcon)
    private JButton createModernButton(String text, Color bg) {
        JButton btn = new JButton(text);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(110, 38));
        btn.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) { btn.setBackground(bg.brighter()); }
            public void mouseExited(MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private JPanel createLabeledField(String label, JTextField field) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.BOLD, 14));
        l.setPreferredSize(new Dimension(100, 36));
        p.add(l, BorderLayout.WEST);
        p.add(field, BorderLayout.CENTER);
        return p;
    }

    private ImageIcon createMailIcon(int s) {
        BufferedImage img = new BufferedImage(s, s, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(Color.WHITE);
        g.fillRoundRect(0, 0, s, s, 10, 10);
        g.setColor(new Color(0, 105, 192));
        g.setFont(new Font("Segoe UI", Font.BOLD, s * 7 / 10));
        g.drawString("M", s / 5, s - 6); // Dùng chữ M đơn giản thay vì chữ "Email" dài
        g.dispose();
        return new ImageIcon(img);
    }
}
