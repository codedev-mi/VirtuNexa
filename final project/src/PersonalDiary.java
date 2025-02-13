import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.security.spec.KeySpec;
import java.sql.*;
import java.util.Base64;
import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class PersonalDiary {
    private JFrame frame;
    private JTable table;
    private DefaultTableModel model;
    private JTextArea textArea;
    private Connection conn;
    private SecretKey secretKey;
    private static final String SALT = "12345678";  // Simplified salt for key derivation

    public PersonalDiary(String password) {
        secretKey = deriveKey(password);
        connectDB();
        setupUI();
        loadEntries();
    }

    private SecretKey deriveKey(String password) {
        try {
            KeySpec spec = new PBEKeySpec(password.toCharArray(), SALT.getBytes(), 65536, 128);
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
            return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void connectDB() {
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:diary.db");
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS entries (id INTEGER PRIMARY KEY, content TEXT)");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void setupUI() {
        frame = new JFrame("Personal Diary");
        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLayout(new BorderLayout());

        model = new DefaultTableModel(new String[]{"ID", "Content"}, 0);
        table = new JTable(model);
        frame.add(new JScrollPane(table), BorderLayout.CENTER);

        textArea = new JTextArea(3, 40);
        frame.add(new JScrollPane(textArea), BorderLayout.NORTH);

        JButton addBtn = new JButton("Add Entry"), deleteBtn = new JButton("Delete"), logoutBtn = new JButton("Logout");
        addBtn.addActionListener(e -> addEntry());
        deleteBtn.addActionListener(e -> deleteEntry());
        logoutBtn.addActionListener(e -> { frame.dispose(); showLoginScreen(); });

        JPanel panel = new JPanel();
        panel.add(addBtn);
        panel.add(deleteBtn);
        panel.add(logoutBtn);
        frame.add(panel, BorderLayout.SOUTH);

        frame.setVisible(true);
    }

    private void loadEntries() {
        model.setRowCount(0);
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM entries")) {
            while (rs.next()) model.addRow(new Object[]{rs.getInt("id"), decrypt(rs.getString("content"))});
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void addEntry() {
        String content = textArea.getText().trim();
        if (content.isEmpty()) return;
        try (PreparedStatement stmt = conn.prepareStatement("INSERT INTO entries (content) VALUES (?)")) {
            stmt.setString(1, encrypt(content));
            stmt.executeUpdate();
            textArea.setText("");
            loadEntries();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void deleteEntry() {
        int row = table.getSelectedRow();
        if (row == -1) return;
        try (PreparedStatement stmt = conn.prepareStatement("DELETE FROM entries WHERE id = ?")) {
            stmt.setInt(1, (int) model.getValueAt(row, 0));
            stmt.executeUpdate();
            loadEntries();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            byte[] iv = new byte[16];
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(data.getBytes()));
        } catch (Exception e) {
            return null;
        }
    }

    private String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(new byte[16]));
            return new String(cipher.doFinal(Base64.getDecoder().decode(encryptedData)));
        } catch (Exception e) {
            return "Error: Decryption Failed";
        }
    }

    private static void showLoginScreen() {
        JFrame loginFrame = new JFrame("Diary Login");
        loginFrame.setSize(300, 150);
        loginFrame.setLayout(new FlowLayout());

        JLabel label = new JLabel("Enter Password:");
        JPasswordField passwordField = new JPasswordField(15);
        JButton loginButton = new JButton("Login");

        loginButton.addActionListener(e -> {
            loginFrame.dispose();
            new PersonalDiary(new String(passwordField.getPassword()));
        });

        loginFrame.add(label);
        loginFrame.add(passwordField);
        loginFrame.add(loginButton);
        loginFrame.setVisible(true);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(PersonalDiary::showLoginScreen);
    }
}
