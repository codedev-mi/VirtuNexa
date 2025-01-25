import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class CurrencyConverterApp {

    static final String DB_URL = "jdbc:sqlite:expenses.db";

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            setupDatabase();
            new MainFrame();
        });
    }

    private static void setupDatabase() {
        try (Connection conn = DriverManager.getConnection(DB_URL);
             Statement stmt = conn.createStatement()) {
            // Create the table if it doesn't exist
            String createTableSQL = "CREATE TABLE IF NOT EXISTS expenses ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "amount REAL,"
                    + "currency TEXT,"
                    + "category TEXT,"
                    + "description TEXT,"
                    + "date TEXT"
                    + ");";
            stmt.execute(createTableSQL);

            // Alter table to add missing columns
            String alterTableSQL = "ALTER TABLE expenses ADD COLUMN description TEXT;";
            try {
                stmt.execute(alterTableSQL);
            } catch (SQLException e) {
                // Ignore error if column already exists
                if (!e.getMessage().contains("duplicate column name")) {
                    throw e;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }


}

class MainFrame extends JFrame {

    public MainFrame() {
        setTitle("Currency Converter & Expense Manager");
        setSize(800, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JTabbedPane tabbedPane = new JTabbedPane();

        tabbedPane.addTab("Currency Converter", new CurrencyConverterPanel());
        tabbedPane.addTab("Expense Management", new ExpenseManagerPanel());
        tabbedPane.addTab("Reports", new ReportsPanel());

        add(tabbedPane);

        setLocationRelativeTo(null);
        setVisible(true);
    }
}

class CurrencyConverterPanel extends JPanel {

    public CurrencyConverterPanel() {
        setLayout(new GridLayout(5, 2, 10, 10));

        JLabel amountLabel = new JLabel("Amount:");
        JTextField amountField = new JTextField();

        JLabel fromCurrencyLabel = new JLabel("From Currency:");
        JTextField fromCurrencyField = new JTextField("USD");

        JLabel toCurrencyLabel = new JLabel("To Currency:");
        JTextField toCurrencyField = new JTextField("EUR");

        JButton convertButton = new JButton("Convert");
        JLabel resultLabel = new JLabel("Result: ");

        convertButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String fromCurrency = fromCurrencyField.getText().toUpperCase();
                String toCurrency = toCurrencyField.getText().toUpperCase();
                double conversionRate = getConversionRate(fromCurrency, toCurrency);
                double result = amount * conversionRate;
                resultLabel.setText("Result: " + result + " " + toCurrency);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Error: Please enter a valid number for amount.");
            } catch (Exception ex) {
                resultLabel.setText("Error: Conversion failed.");
            }
        });

        add(amountLabel);
        add(amountField);
        add(fromCurrencyLabel);
        add(fromCurrencyField);
        add(toCurrencyLabel);
        add(toCurrencyField);
        add(convertButton);
        add(resultLabel);
    }

    private double getConversionRate(String fromCurrency, String toCurrency) {
        if (fromCurrency.equals("USD") && toCurrency.equals("EUR")) {
            return 0.85;
        } else if (fromCurrency.equals("EUR") && toCurrency.equals("USD")) {
            return 1.18;
        } else {
            return 1.0;
        }
    }
}

class ExpenseManagerPanel extends JPanel {

    private DefaultListModel<String> expenseListModel;

    public ExpenseManagerPanel() {
        setLayout(new BorderLayout(10, 10));

        expenseListModel = new DefaultListModel<>();
        JList<String> expenseList = new JList<>(expenseListModel);

        JPanel formPanel = new JPanel(new GridLayout(5, 2, 5, 5));

        JTextField amountField = new JTextField();
        JTextField currencyField = new JTextField("USD");
        JTextField categoryField = new JTextField();
        JTextField descriptionField = new JTextField();

        JButton addButton = new JButton("Add Expense");
        addButton.addActionListener(e -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                String currency = currencyField.getText();
                String category = categoryField.getText();
                String description = descriptionField.getText();
                addExpense(amount, currency, category, description);
                expenseListModel.addElement(amount + " " + currency + " - " + category + " - " + description);
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(this, "Error: Please enter a valid number for amount.", "Input Error", JOptionPane.ERROR_MESSAGE);
            }
        });

        formPanel.add(new JLabel("Amount:"));
        formPanel.add(amountField);
        formPanel.add(new JLabel("Currency:"));
        formPanel.add(currencyField);
        formPanel.add(new JLabel("Category:"));
        formPanel.add(categoryField);
        formPanel.add(new JLabel("Description:"));
        formPanel.add(descriptionField);
        formPanel.add(new JLabel());
        formPanel.add(addButton);

        add(new JScrollPane(expenseList), BorderLayout.CENTER);
        add(formPanel, BorderLayout.SOUTH);
    }

    private void addExpense(double amount, String currency, String category, String description) {
        try (Connection conn = DriverManager.getConnection(CurrencyConverterApp.DB_URL);
             PreparedStatement pstmt = conn.prepareStatement(
                     "INSERT INTO expenses (amount, currency, category, description, date) VALUES (?, ?, ?, ?, date('now'))")) {
            pstmt.setDouble(1, amount);
            pstmt.setString(2, currency);
            pstmt.setString(3, category);
            pstmt.setString(4, description);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}

class ReportsPanel extends JPanel {

    public ReportsPanel() {
        setLayout(new BorderLayout(10, 10));

        JTextArea reportArea = new JTextArea();
        reportArea.setEditable(false);

        JButton generateReportButton = new JButton("Generate Report");
        generateReportButton.addActionListener(e -> {
            reportArea.setText(generateReport());
        });

        add(new JScrollPane(reportArea), BorderLayout.CENTER);
        add(generateReportButton, BorderLayout.SOUTH);
    }

    private String generateReport() {
        StringBuilder report = new StringBuilder();
        try (Connection conn = DriverManager.getConnection(CurrencyConverterApp.DB_URL);
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM expenses")) {

            while (rs.next()) {
                report.append("ID: ").append(rs.getInt("id"))
                        .append(", Amount: ").append(rs.getDouble("amount"))
                        .append(", Currency: ").append(rs.getString("currency"))
                        .append(", Category: ").append(rs.getString("category"))
                        .append(", Description: ").append(rs.getString("description"))
                        .append(", Date: ").append(rs.getString("date"))
                        .append("\n");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return report.toString();
    }
}
