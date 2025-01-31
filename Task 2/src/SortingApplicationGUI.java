import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class SortingApplicationGUI {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(SortingApplicationGUI::createAndShowGUI);
    }

    private static void createAndShowGUI() {
        JFrame frame = new JFrame("Sorting Application");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(550, 450);

        JPanel panel = new JPanel();
        panel.setLayout(new GridLayout(5, 1));

        JLabel inputLabel = new JLabel("Enter numbers separated by spaces:");
        JTextField inputField = new JTextField();
        panel.add(inputLabel);
        panel.add(inputField);

        JLabel orderLabel = new JLabel("Choose sorting order:");
        String[] options = {"Ascending", "Descending"};
        JComboBox<String> orderComboBox = new JComboBox<>(options);
        panel.add(orderLabel);
        panel.add(orderComboBox);

        JButton sortButton = new JButton("Sort and Save");
        panel.add(sortButton);

        JTextArea resultArea = new JTextArea();
        resultArea.setEditable(false);
        frame.add(panel, BorderLayout.NORTH);
        frame.add(new JScrollPane(resultArea), BorderLayout.CENTER);

        sortButton.addActionListener(e -> {
            String input = inputField.getText();
            String order = (String) orderComboBox.getSelectedItem();

            List<Integer> numbers = parseInput(input);
            if (numbers.isEmpty()) {
                resultArea.setText("Invalid input. Please enter numbers separated by spaces.");
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
        String createTableQuery = "CREATE TABLE IF NOT EXISTS sorted_numbers (id INTEGER PRIMARY KEY AUTOINCREMENT, numbers TEXT, order_type TEXT);";
        String insertQuery = "INSERT INTO sorted_numbers (numbers, order_type) VALUES (?, ?);";

        try (Connection conn = DriverManager.getConnection(dbUrl);
             Statement stmt = conn.createStatement()) {

            stmt.execute(createTableQuery);

            try (PreparedStatement pstmt = conn.prepareStatement(insertQuery)) {
                pstmt.setString(1, numbers.toString());
                pstmt.setString(2, order == 1 ? "Ascending" : "Descending");
                pstmt.executeUpdate();
            }

            System.out.println("Sorted data saved to the database.");
        } catch (SQLException e) {
            System.out.println("Database error: " + e.getMessage());
        }
    }
}
