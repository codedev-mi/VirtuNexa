import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;
import javax.swing.*;

public class SortingApplicationGUI {

    private static JTextField inputField;
    private static JTextArea resultArea;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SortingApplicationGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Sorting Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 500);

        // Panel for inputs
        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1, 10, 10));

        // Input for numbers
        JLabel inputLabel = new JLabel("Enter numbers separated by spaces:");
        inputField = new JTextField();
        panel.add(inputLabel);
        panel.add(inputField);

        // Dropdown for sorting order
        JLabel orderLabel = new JLabel("Choose sorting order:");
        String[] options = {"Ascending", "Descending"};
        JComboBox<String> orderComboBox = new JComboBox<>(options);
        panel.add(orderLabel);
        panel.add(orderComboBox);

        // Panel for buttons
        JPanel buttonPanel = new JPanel();
        JButton sortButton = new JButton("Sort and Save");
        JButton clearButton = new JButton("Clear");
        JButton viewHistoryButton = new JButton("View History");
        buttonPanel.add(sortButton);
        buttonPanel.add(clearButton);
        buttonPanel.add(viewHistoryButton);
        panel.add(buttonPanel);

        // Output area
        resultArea = new JTextArea();
        resultArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultArea);

        // Layout
        frame.add(panel, BorderLayout.NORTH);
        frame.add(scrollPane, BorderLayout.CENTER);

        // Action: Sort and Save
        sortButton.addActionListener(e -> {
            String input = inputField.getText().trim();
            String order = (String) orderComboBox.getSelectedItem();
            List<Integer> numbers = parseInput(input);

            if (numbers.isEmpty()) {
                resultArea.setText("⚠️ Invalid input: Please enter numbers separated by spaces.");
                return;
            }

            if ("Ascending".equals(order)) {
                numbers = sortAscending(numbers);
                resultArea.setText("Sorted in Ascending Order: " + numbers);
            } else {
                numbers = sortDescending(numbers);
                resultArea.setText("Sorted in Descending Order: " + numbers);
            }

            saveToDatabase(numbers, "Ascending".equals(order) ? 1 : 2);
            resultArea.append("\n✅ Result saved to database.");
        });

        // Action: Clear
        clearButton.addActionListener(e -> {
            inputField.setText("");
            resultArea.setText("");
        });

        // Action: View History
        viewHistoryButton.addActionListener(e -> {
            List<String> history = getHistoryFromDatabase();
            if (history.isEmpty()) {
                resultArea.setText("No history found.");
            } else {
                resultArea.setText("📜 History:\n" + String.join("\n", history));
            }
        });

        frame.setVisible(true);
    }

    private static List<Integer> parseInput(String input) {
        List<Integer> numbers = new ArrayList<>();
        try {
            numbers = Arrays.stream(input.split(" "))
                    .map(String::trim)
                    .map(Integer::parseInt)
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            System.out.println("Error: Non-numeric input detected.");
        }
        return numbers;
    }

    private static List<Integer> sortAscending(List<Integer> numbers) {
        Collections.sort(numbers);
        return numbers;
    }

    private static List<Integer> sortDescending(List<Integer> numbers) {
        Collections.sort(numbers, Collections.reverseOrder());
        return numbers;
    }

    private static void saveToDatabase(List<Integer> numbers, int order) {
        String dbUrl = "jdbc:sqlite:sorting_app.db";
        String createTableQuery = "CREATE TABLE IF NOT EXISTS sorted_numbers (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "numbers TEXT NOT NULL, " +
                "order_type TEXT NOT NULL)";
        String insertQuery = "INSERT INTO sorted_numbers (numbers, order_type) VALUES (?, ?)";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableQuery);

            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, numbers.toString());
                pstmt.setString(2, order == 1 ? "Ascending" : "Descending");
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            resultArea.setText("❌ Database error: " + e.getMessage());
        }
    }

    private static List<String> getHistoryFromDatabase() {
        List<String> history = new ArrayList<>();
        String dbUrl = "jdbc:sqlite:sorting_app.db";
        String selectQuery = "SELECT numbers, order_type FROM sorted_numbers ORDER BY id DESC";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(selectQuery)) {

            while (rs.next()) {
                String numbers = rs.getString("numbers");
                String order = rs.getString("order_type");
                history.add("Order: " + order + ", Numbers: " + numbers);
            }

        } catch (SQLException e) {
            history.add("❌ Error retrieving history: " + e.getMessage());
        }

        return history;
    }
}
