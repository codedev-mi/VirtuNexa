import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class CurrencyConverter extends JFrame {
    private JTextField amountField;
    private JButton convertToEuroButton;
    private JButton convertToDollarButton;
    private JLabel resultLabel;

    // Fixed exchange rate
    private static final double EXCHANGE_RATE = 0.85;

    public CurrencyConverter() {
        setTitle("Currency Converter");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new FlowLayout());

        // Input field for amount
        amountField = new JTextField(10);
        add(new JLabel("Amount:"));
        add(amountField);

        // Button to convert to Euro
        convertToEuroButton = new JButton("Convert to Euro");
        convertToEuroButton.addActionListener(new ConvertToEuroListener());
        add(convertToEuroButton);

        // Button to convert to Dollar
        convertToDollarButton = new JButton("Convert to Dollar");
        convertToDollarButton.addActionListener(new ConvertToDollarListener());
        add(convertToDollarButton);

        // Label to display result
        resultLabel = new JLabel("Result: ");
        add(resultLabel);
    }

    private class ConvertToEuroListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                double amount = Double.parseDouble(amountField.getText());
                double convertedAmount = amount * EXCHANGE_RATE;
                resultLabel.setText("Result: " + convertedAmount + " EUR");
            } catch (NumberFormatException ex) {
                resultLabel.setText("Invalid input!");
            }
        }
    }

    private class ConvertToDollarListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                double amount = Double.parseDouble(amountField.getText());
                double convertedAmount = amount / EXCHANGE_RATE;
                resultLabel.setText("Result: " + convertedAmount + " USD");
            } catch (NumberFormatException ex) {
                resultLabel.setText("Invalid input!");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CurrencyConverter converter = new CurrencyConverter();
            converter.setVisible(true);
        });
    }
}