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
 private static JComboBox<String> cipherSelector;

 public static void main(String[] args) {
     JFrame frame = new JFrame("Cipher Tool");
     frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
     frame.setSize(700, 600);
     frame.setLayout(new BorderLayout(10, 10));

     // Input Panel
     JPanel inputPanel = new JPanel(new GridLayout(5, 2, 10, 10));
     inputPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

     textField = new JTextField();
     keyField = new JTextField();

     cipherSelector = new JComboBox<>(new String[]{"Caesar Cipher", "Vigenere Cipher"});

     JButton encryptButton = new JButton("Encrypt");
     JButton decryptButton = new JButton("Decrypt");

     inputPanel.add(new JLabel("Enter Text:"));
     inputPanel.add(textField);
     inputPanel.add(new JLabel("Enter Key:"));
     inputPanel.add(keyField);
     inputPanel.add(new JLabel("Select Cipher:"));
     inputPanel.add(cipherSelector);
     inputPanel.add(encryptButton);
     inputPanel.add(decryptButton);

     frame.add(inputPanel, BorderLayout.NORTH);

     // Result Area
     resultArea = new JTextArea(5, 40);
     resultArea.setEditable(false);
     resultArea.setBorder(BorderFactory.createTitledBorder("Result"));
     frame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

     // History Panel
     JPanel historyPanel = new JPanel(new BorderLayout(5, 5));
     historyPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

     historyArea = new JTextArea(10, 40);
     historyArea.setEditable(false);
     JScrollPane historyScroll = new JScrollPane(historyArea);

     JButton clearHistoryBtn = new JButton("Clear History");
     clearHistoryBtn.addActionListener(e -> {
         try (Connection conn = DriverManager.getConnection("jdbc:sqlite:cipher_history.db")) {
             conn.createStatement().execute("DELETE FROM cipher_history");
             loadHistory();
         } catch (SQLException ex) {
             resultArea.setText("Error clearing history: " + ex.getMessage());
         }
     });

     historyPanel.add(new JLabel("History:"), BorderLayout.NORTH);
     historyPanel.add(historyScroll, BorderLayout.CENTER);
     historyPanel.add(clearHistoryBtn, BorderLayout.SOUTH);

     frame.add(historyPanel, BorderLayout.SOUTH);

     // Exit Button
     JButton exitButton = new JButton("Exit");
     exitButton.addActionListener(e -> System.exit(0));
     frame.add(exitButton, BorderLayout.EAST);

     // Action listeners
     encryptButton.addActionListener(e -> processAction(true));
     decryptButton.addActionListener(e -> processAction(false));

     frame.setVisible(true);
     loadHistory();
 }

 private static void processAction(boolean isEncrypt) {
     String text = textField.getText();
     String key = keyField.getText();
     String cipherType = (String) cipherSelector.getSelectedItem();

     if (text.isEmpty() || key.isEmpty()) {
         resultArea.setText("Error: Text and Key must not be empty.");
         return;
     }

     CipherAlgorithm cipher = switch (cipherType) {
         case "Vigenere Cipher" -> new VigenereCipher();
         default -> new CaesarCipher();
     };

     try {
         String result = isEncrypt ? cipher.encrypt(text, key) : cipher.decrypt(text, key);
         resultArea.setText("Result: " + result);

         DatabaseHelper.saveHistory(text, key, isEncrypt ? "encrypt" : "decrypt", result);
         loadHistory();
     } catch (Exception e) {
         resultArea.setText("Error: " + e.getMessage());
     }
 }

 private static void loadHistory() {
     List<String> history = DatabaseHelper.getHistory();
     historyArea.setText(String.join("\n", history));
 }
}
