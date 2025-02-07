import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.sql.*;
import java.util.List;
import java.util.ArrayList;

// Cipher interface for modularity
interface CipherAlgorithm {
    String encrypt(String text, String key);
    String decrypt(String text, String key);
}

// CaesarCipher implementation
class CaesarCipher implements CipherAlgorithm {
    private static final int ALPHABET_SIZE = 26;

    @Override
    public String encrypt(String text, String key) {
        int shift = Integer.parseInt(key) % ALPHABET_SIZE;
        return shiftText(text, shift);
    }

    @Override
    public String decrypt(String text, String key) {
        int shift = Integer.parseInt(key) % ALPHABET_SIZE;
        return shiftText(text, -shift);
    }

    private String shiftText(String text, int shift) {
        StringBuilder result = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                c = (char) ((c - base + shift + ALPHABET_SIZE) % ALPHABET_SIZE + base);
            }
            result.append(c);
        }
        return result.toString();
    }
}

// VigenereCipher implementation
class VigenereCipher implements CipherAlgorithm {
    @Override
    public String encrypt(String text, String key) {
        return processText(text, key, true);
    }

    @Override
    public String decrypt(String text, String key) {
        return processText(text, key, false);
    }

    private String processText(String text, String key, boolean isEncrypt) {
        StringBuilder result = new StringBuilder();
        key = key.toLowerCase();
        int keyIndex = 0;
        for (char c : text.toCharArray()) {
            if (Character.isLetter(c)) {
                char base = Character.isUpperCase(c) ? 'A' : 'a';
                int shift = key.charAt(keyIndex) - 'a';
                shift = isEncrypt ? shift : -shift;
                c = (char) ((c - base + shift + 26) % 26 + base);
                keyIndex = (keyIndex + 1) % key.length();
            }
            result.append(c);
        }
        return result.toString();
    }
}

// JDBC helper class for interacting with the SQLite database
class DatabaseHelper {
    private static final String DATABASE_URL = "jdbc:sqlite:cipher_history.db";

    static {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL)) {
            Statement stmt = conn.createStatement();
            String createTableQuery = "CREATE TABLE IF NOT EXISTS cipher_history (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    "text TEXT NOT NULL, " +
                    "key TEXT NOT NULL, " +
                    "action TEXT NOT NULL, " +
                    "result TEXT NOT NULL, " +
                    "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)";
            stmt.execute(createTableQuery);
        } catch (SQLException e) {
            System.out.println("Database initialization error: " + e.getMessage());
        }
    }

    public static void saveHistory(String text, String key, String action, String result) {
        try (Connection conn = DriverManager.getConnection(DATABASE_URL)) {
            String insertQuery = "INSERT INTO cipher_history (text, key, action, result) VALUES (?, ?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, text);
                pstmt.setString(2, key);
                pstmt.setString(3, action);
                pstmt.setString(4, result);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.out.println("Error saving history: " + e.getMessage());
        }
    }

    public static List<String> getHistory() {
        List<String> history = new ArrayList<>();
        try (Connection conn = DriverManager.getConnection(DATABASE_URL)) {
            String selectQuery = "SELECT text, key, action, result, timestamp FROM cipher_history ORDER BY timestamp DESC";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(selectQuery)) {
                while (rs.next()) {
                    history.add(String.format("Text: %s, Key: %s, Action: %s, Result: %s, Time: %s",
                            rs.getString("text"),
                            rs.getString("key"),
                            rs.getString("action"),
                            rs.getString("result"),
                            rs.getString("timestamp")));
                }
            }
        } catch (SQLException e) {
            System.out.println("Error retrieving history: " + e.getMessage());
        }
        return history;
    }
}

// GUI for the Cipher Tool with JDBC
public class CipherToolGUI {
    private static JTextField textField;
    private static JTextField keyField;
    private static JTextArea resultArea;
    private static JTextArea historyArea;

    public static void main(String[] args) {
        // Create frame and layout
        JFrame frame = new JFrame("Cipher Tool");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 600);
        frame.setLayout(new BorderLayout(10, 10));

        // Create panel for input fields and buttons
        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new GridLayout(4, 2, 10, 10));

        // Text input field
        JLabel textLabel = new JLabel("Enter Text:");
        textField = new JTextField(20);
        inputPanel.add(textLabel);
        inputPanel.add(textField);

        // Key input field
        JLabel keyLabel = new JLabel("Enter Key:");
        keyField = new JTextField(20);
        inputPanel.add(keyLabel);
        inputPanel.add(keyField);

        // Encrypt and Decrypt Buttons
        JButton encryptButton = new JButton("Encrypt");
        JButton decryptButton = new JButton("Decrypt");
        inputPanel.add(encryptButton);
        inputPanel.add(decryptButton);

        // Add inputPanel to the frame
        frame.add(inputPanel, BorderLayout.NORTH);

        // Result output area
        resultArea = new JTextArea(5, 40);
        resultArea.setEditable(false);
        JScrollPane resultScroll = new JScrollPane(resultArea);
        frame.add(resultScroll, BorderLayout.CENTER);

        // History section with label
        JPanel historyPanel = new JPanel();
        historyPanel.setLayout(new BorderLayout(5, 5));
        
        JLabel historyLabel = new JLabel("History:");
        historyPanel.add(historyLabel, BorderLayout.NORTH);

        historyArea = new JTextArea(10, 40);
        historyArea.setEditable(false);
        JScrollPane historyScroll = new JScrollPane(historyArea);
        historyPanel.add(historyScroll, BorderLayout.CENTER);

        frame.add(historyPanel, BorderLayout.SOUTH);

        // Exit Button
        JButton exitButton = new JButton("Exit");
        frame.add(exitButton, BorderLayout.EAST);

        // Action listeners for Encrypt and Decrypt buttons
        encryptButton.addActionListener(e -> processAction(true));
        decryptButton.addActionListener(e -> processAction(false));

        // Action listener for Exit button
        exitButton.addActionListener(e -> System.exit(0));

        // Set frame visibility
        frame.setVisible(true);

        // Load history on startup
        loadHistory();
    }

    private static void processAction(boolean isEncrypt) {
        String text = textField.getText();
        String key = keyField.getText();
        if (text.isEmpty() || key.isEmpty()) {
            resultArea.setText("Error: Text and Key must not be empty.");
            return;
        }

        CipherAlgorithm cipher = new CaesarCipher(); // Default to Caesar Cipher
        String result;
        try {
            if (isEncrypt) {
                result = cipher.encrypt(text, key);
            } else {
                result = cipher.decrypt(text, key);
            }
            resultArea.setText("Result: " + result);

            // Save to database
            String action = isEncrypt ? "encrypt" : "decrypt";
            DatabaseHelper.saveHistory(text, key, action, result);

            // Load updated history
            loadHistory();
        } catch (IllegalArgumentException e) {
            resultArea.setText("Error: " + e.getMessage());
        }
    }

    private static void loadHistory() {
        List<String> history = DatabaseHelper.getHistory();
        historyArea.setText(String.join("\n", history));
    }
}
