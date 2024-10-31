package garmentfactory.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.*;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import java.awt.Font;
import javax.swing.table.JTableHeader;

public class Payrolls extends javax.swing.JFrame {

    private JTextField jTextFieldCurrentMonth, jTextFieldSearch;
    private JButton jButtonCurrentMonth, jButtonGeneratePayroll, jButtonBack, jButtonPrint, jButtonPrintAll;
    private JTable jTablePayrolls;
    private JScrollPane jScrollPanePayrolls;
    private Connection connection;
    private TableRowSorter<DefaultTableModel> sorter;

    public Payrolls() {
        initComponents();
        initDatabase();
        setSize(1880, 1000);
        setLocationRelativeTo(null);
// Set the window size
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setTitle("Payroll Generation");

        setUndecorated(true);

        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        // Create and configure the title label
        JLabel titleLabel = new JLabel("Payrolls", JLabel.CENTER); // Center the title
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30)); // Set the font and size
        titleLabel.setForeground(Color.BLACK); // Set the font color

        titleLabel.setBorder(BorderFactory.createEmptyBorder(20, 0, 20, 0)); // Top, Left, Bottom, Right padding
        
        
        Font customFont = new Font("Arial", Font.PLAIN, 25);

        // Set the default font for all components
        UIManager.put("Button.font", customFont);
        UIManager.put("Label.font", customFont);
        UIManager.put("TextField.font", customFont);
        UIManager.put("Table.font", customFont);
        UIManager.put("TableHeader.font", customFont);

        jButtonBack = new JButton("Back");
        jButtonBack.setForeground(Color.WHITE);
        jButtonBack.setBackground(Color.RED);
        jButtonBack.setBorderPainted(false);

        jTextFieldCurrentMonth = new JTextField(7);
        jButtonCurrentMonth = new JButton("Set Current Month");
        jButtonCurrentMonth.setForeground(Color.WHITE);
        jButtonCurrentMonth.setBackground(Color.GRAY);
        jButtonCurrentMonth.setBorderPainted(false);

        jButtonGeneratePayroll = new JButton("Generate Payroll");
        jButtonGeneratePayroll.setForeground(Color.BLACK);
        jButtonGeneratePayroll.setBackground(Color.ORANGE);
        jButtonGeneratePayroll.setBorderPainted(false);

        jButtonPrint = new JButton("Print Selected");
        jButtonPrint.setForeground(Color.WHITE);
        jButtonPrint.setBackground(Color.GREEN);
        jButtonPrint.setBorderPainted(false);

        jButtonPrintAll = new JButton("Print All");
        jButtonPrintAll.setForeground(Color.BLACK);
        jButtonPrintAll.setBackground(Color.CYAN);
        jButtonPrintAll.setBorderPainted(false);

        jTextFieldSearch = new JTextField(15);

        // Create the table and set its font and row height
        jTablePayrolls = new JTable();
        jTablePayrolls.setFont(new Font("Arial", Font.PLAIN, 14)); // Set font for the table rows
        jTablePayrolls.setRowHeight(20); // Adjust row height for readability

        // Set the font for the table header
        JTableHeader tableHeader = jTablePayrolls.getTableHeader();
        tableHeader.setFont(new Font("Arial", Font.BOLD, 16)); // Set font for the header

        jScrollPanePayrolls = new JScrollPane(jTablePayrolls); // No double initialization

        // Create the first row panel
        JPanel firstRowPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcFirstRow = new GridBagConstraints();
        gbcFirstRow.insets = new Insets(5, 5, 5, 5);

        gbcFirstRow.gridx = 0;
        gbcFirstRow.gridy = 0;
        firstRowPanel.add(jButtonBack, gbcFirstRow);

        gbcFirstRow.gridx = 1;
        firstRowPanel.add(new JLabel("Current Month (MM:YYYY):"), gbcFirstRow);

        gbcFirstRow.gridx = 2;
        firstRowPanel.add(jTextFieldCurrentMonth, gbcFirstRow);

        gbcFirstRow.gridx = 3;
        firstRowPanel.add(jButtonCurrentMonth, gbcFirstRow);

        gbcFirstRow.gridx = 4;
        firstRowPanel.add(jButtonGeneratePayroll, gbcFirstRow);

        // Create the second row panel
        JPanel secondRowPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcSecondRow = new GridBagConstraints();
        gbcSecondRow.insets = new Insets(5, 5, 5, 5);

        gbcSecondRow.gridx = 0;
        gbcSecondRow.gridy = 0;
        secondRowPanel.add(new JLabel("Search:"), gbcSecondRow);

        gbcSecondRow.gridx = 1;
        secondRowPanel.add(jTextFieldSearch, gbcSecondRow);

        gbcSecondRow.gridx = 2;
        secondRowPanel.add(jButtonPrint, gbcSecondRow);

        gbcSecondRow.gridx = 3;
        secondRowPanel.add(jButtonPrintAll, gbcSecondRow);

        // Add both row panels to the top panel
        JPanel topPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbcTopPanel = new GridBagConstraints();
        gbcTopPanel.insets = new Insets(5, 5, 5, 5);

        gbcTopPanel.gridx = 0;
        gbcTopPanel.gridy = 0;
        topPanel.add(firstRowPanel, gbcTopPanel);

        gbcTopPanel.gridy = 1;
        topPanel.add(secondRowPanel, gbcTopPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, topPanel, jScrollPanePayrolls);
        splitPane.setResizeWeight(0.1);
        splitPane.setDividerSize(5);

        add(splitPane, BorderLayout.CENTER);

        // Add action listeners
        jButtonBack.addActionListener(e -> setBack());
        jButtonCurrentMonth.addActionListener(e -> setCurrentMonth());
        jButtonGeneratePayroll.addActionListener(e -> generatePayrolls());
        jButtonPrint.addActionListener(e -> printSelectedEmployees());
        jButtonPrintAll.addActionListener(e -> printAllEmployees());

        // Add an ActionListener to the search field
        jTextFieldSearch.addActionListener(e -> filterTable());

        // Add tooltips
        jButtonBack.setToolTipText("Go back to the previous screen");
        jButtonCurrentMonth.setToolTipText("Set the current month for payroll generation");
        jButtonGeneratePayroll.setToolTipText("Generate payroll for the selected month");
        jButtonPrint.setToolTipText("Print the selected payroll records");
        jButtonPrintAll.setToolTipText("Print all payroll records");
        jTextFieldSearch.setToolTipText("Search for specific payroll records");
        add(titleLabel, BorderLayout.NORTH);
        pack();
    }

    private void setCurrentMonth() {
        YearMonth currentYearMonth = YearMonth.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM:yyyy");
        jTextFieldCurrentMonth.setText(currentYearMonth.format(formatter));
    }

    private void setBack() {
        dispose();
    }

    private void initDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String url = "jdbc:mysql://localhost:3306/garmentfactory";
            connection = DriverManager.getConnection(url, "root", "");
        } catch (ClassNotFoundException | SQLException e) {
            JOptionPane.showMessageDialog(this, "Failed to connect to the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generatePayrolls() {
        String currentMonth = jTextFieldCurrentMonth.getText();
        if (!isValidMonthFormat(currentMonth)) {
            JOptionPane.showMessageDialog(this, "Invalid month format. Please use MM:YYYY", "Input Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        YearMonth yearMonth = YearMonth.parse(currentMonth, DateTimeFormatter.ofPattern("MM:yyyy"));
        int month = yearMonth.getMonthValue();
        int year = yearMonth.getYear();

        String sql = "WITH employee_attendance AS ("
                + "    SELECT"
                + "        e.employee_name," // Keep employee_name
                + "        e.basic_salary,"
                + "        SUM(a.late_time) AS total_late_time,"
                + "        SUM(a.overtime) AS total_overtime,"
                + "        SUM(a.work_time) AS total_work_time,"
                + "        ROUND(SUM(a.late_time) * (e.basic_salary / 26 / 8), 2) AS late_deduction,"
                + "        ROUND(SUM(a.overtime) * (e.basic_salary / 26 / 8) * 2, 2) AS overtime_payment,"
                + "        ROUND(SUM(a.work_time) * (e.basic_salary / 26 / 8), 2) AS salary_earned"
                + "    FROM"
                + "        attendance a"
                + "    JOIN employees e ON a.employee_id = e.employee_id"
                + "    WHERE MONTH(a.date) = ? AND YEAR(a.date) = ?"
                + "    GROUP BY e.employee_name, e.basic_salary"
                + "),"
                + "employee_leaves AS ("
                + "    SELECT"
                + "        e.employee_name,"
                + "        e.basic_salary,"
                + "        COALESCE(SUM(DATEDIFF(l.end_date, l.start_date) + 1), 0) AS leave_days,"
                + "        ROUND(COALESCE(SUM((DATEDIFF(l.end_date, l.start_date) + 1) * (e.basic_salary / 26)), 0), 2) AS leave_salary"
                + "    FROM"
                + "        leaves l"
                + "    RIGHT JOIN employees e ON l.employee_id = e.employee_id"
                + "    WHERE (MONTH(l.start_date) = ? AND YEAR(l.start_date) = ?)"
                + "        OR (MONTH(l.end_date) = ? AND YEAR(l.end_date) = ?)"
                + "    GROUP BY e.employee_name, e.basic_salary"
                + "),"
                + "payroll_summary AS ("
                + "    SELECT"
                + "        ea.employee_name,"
                + "        ea.basic_salary,"
                + "        ea.total_late_time,"
                + "        ea.total_overtime,"
                + "        ea.total_work_time,"
                + "        ea.salary_earned,"
                + "        ea.late_deduction,"
                + "        ea.overtime_payment,"
                + "        COALESCE(el.leave_days, 0) AS leave_days,"
                + "        COALESCE(el.leave_salary, 0) AS leave_salary,"
                + "        ROUND(CASE "
                + "            WHEN ea.total_late_time = 0 AND el.leave_days = 0 THEN ea.basic_salary * 0.10"
                + "            ELSE 0 "
                + "        END, 2) AS bonus"
                + "    FROM"
                + "        employee_attendance ea"
                + "    LEFT JOIN employee_leaves el ON ea.employee_name = el.employee_name"
                + ")"
                + "SELECT"
                + "    ps.employee_name,"
                + "    ROUND(ps.basic_salary, 2) AS basic_salary,"
                + "    ? AS on_months,"
                + "    d.department_name AS department,"
                + "    u.username AS team_leader,"
                + "    ps.total_overtime,"
                + "    ROUND(ps.overtime_payment, 2) AS overtime_amount,"
                + "    ps.total_late_time,"
                + "    ROUND(ps.late_deduction, 2) AS late_time_amount,"
                + "    ps.total_work_time,"
                + "    ROUND(ps.salary_earned, 2) AS work_time_amount,"
                + "    ps.leave_days,"
                + "    ROUND(ps.leave_salary, 2) AS leave_days_amount,"
                + "    ps.bonus,"
                + "    ROUND( ps.late_deduction + ps.leave_salary, 2 ) AS salary_deduction,"
                + "    ROUND( ps.basic_salary + ps.overtime_payment + ps.salary_earned + ps.bonus - ( ps.late_deduction + ps.leave_salary ), 2 ) AS total_salary "
                + "FROM"
                + "    payroll_summary ps "
                + "JOIN employees e ON ps.employee_name = e.employee_name "
                + "JOIN departments d ON e.department_id = d.department_id "
                + "JOIN team_leader tl ON e.employee_id = tl.employee_id "
                + "JOIN users u ON tl.user_id = u.user_id";

        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setInt(1, month);
            pstmt.setInt(2, year);
            pstmt.setInt(3, month);
            pstmt.setInt(4, year);
            pstmt.setInt(5, month);
            pstmt.setInt(6, year);
            pstmt.setString(7, String.format("%02d-%04d", month, year));

            try (ResultSet rs = pstmt.executeQuery()) {
                DefaultTableModel model = new DefaultTableModel(new String[]{
                    "Employee Name", "Basic Salary", "On Months", "Department", "Team Leader",
                    "Total Overtime", "Overtime Amount", "Total Late Time", "Late Time Amount",
                    "Total Work Time", "Work Time Amount", "Leave Days", "Leave Days Amount",
                    "Bonus", "Salary Deduction", "Total Salary"
                }, 0);

                while (rs.next()) {
                    Object[] row = {
                        rs.getString("employee_name"),
                        rs.getDouble("basic_salary"),
                        rs.getString("on_months"),
                        rs.getString("department"),
                        rs.getString("team_leader"),
                        rs.getInt("total_overtime"),
                        rs.getDouble("overtime_amount"),
                        rs.getInt("total_late_time"),
                        rs.getDouble("late_time_amount"),
                        rs.getInt("total_work_time"),
                        rs.getDouble("work_time_amount"),
                        rs.getInt("leave_days"),
                        rs.getDouble("leave_days_amount"),
                        rs.getDouble("bonus"),
                        rs.getDouble("salary_deduction"),
                        rs.getDouble("total_salary")
                    };
                    model.addRow(row);
                }

                jTablePayrolls.setModel(model);
                sorter = new TableRowSorter<>(model);
                jTablePayrolls.setRowSorter(sorter);
                JOptionPane.showMessageDialog(this, "Payroll generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error generating payrolls: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void generatePDFReceipt(String employeeName, double basicSalary, String onMonths, String department,
            String teamLeader, int totalOvertime, double overtimeAmount, int totalLateTime,
            double lateTimeAmount, int totalWorkTime, double workTimeAmount, int leaveDays,
            double leaveDaysAmount, double bonus, double salaryDeduction, double totalSalary)
            throws DocumentException, IOException {

        String directory = "D:\\PuHour_DATA\\Java-Garment-Factory\\Payroll_Receipt";
        File dir = new File(directory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String fileName = employeeName.replaceAll("\\s+", "_") + "_" + onMonths.replace(":", "_") + ".pdf";
        String fullPath = directory + File.separator + fileName;

        Document document = new Document();
        PdfWriter.getInstance(document, new FileOutputStream(fullPath));

        try {
            document.open();

            // Header
            PdfPTable headerTable = new PdfPTable(2);
            headerTable.setWidthPercentage(100);
            headerTable.setSpacingBefore(10f);
            headerTable.setSpacingAfter(10f);

            PdfPCell logoCell = new PdfPCell(new Phrase("Than PuHour", new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 18, com.itextpdf.text.Font.BOLD)));
            logoCell.setBorder(PdfPCell.NO_BORDER);
            logoCell.setPadding(10);
            headerTable.addCell(logoCell);

            PdfPCell dateCell = new PdfPCell(new Phrase("Date: " + java.time.LocalDate.now().toString(), new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12)));
            dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            dateCell.setBorder(PdfPCell.NO_BORDER);
            dateCell.setPadding(10);
            headerTable.addCell(dateCell);

            document.add(headerTable);

            // Recipient Info
            com.itextpdf.text.Font infoFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12);
            document.add(new Paragraph("Employee Name: " + employeeName, infoFont));
            document.add(new Paragraph("Department: " + department, infoFont));
            document.add(new Paragraph("Team Leader: " + teamLeader, infoFont));
            document.add(new Paragraph("Payroll On Month(s): " + onMonths, infoFont));
            document.add(new Paragraph(" "));

            // Table with payroll details
            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            float[] columnWidths = {0.1f, 0.6f, 0.3f};
            table.setWidths(columnWidths);

            // Set table headers
            com.itextpdf.text.Font headerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 12, com.itextpdf.text.Font.BOLD);
            table.addCell(createCell("No", headerFont));
            table.addCell(createCell("Payment Details", headerFont));
            table.addCell(createCell("Amount", headerFont));

            // Add details rows
            com.itextpdf.text.Font cellFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10);
            addRow(table, "1", "Basic Salary", String.format("$%.2f", basicSalary), cellFont);
            addRow(table, "2", "Total Overtime (" + totalOvertime + " hours)", String.format("$%.2f", overtimeAmount), cellFont);
            addRow(table, "3", "Total Late Time (" + totalLateTime + " hours)", String.format("-$%.2f", lateTimeAmount), cellFont);
            addRow(table, "4", "Total Work Time (" + totalWorkTime + " hours)", String.format("$%.2f", workTimeAmount), cellFont);
            addRow(table, "5", "Leave Days (" + leaveDays + " days)", String.format("-$%.2f", leaveDaysAmount), cellFont);
            addRow(table, "6", "Bonus", String.format("$%.2f", bonus), cellFont);
            addRow(table, "7", "Salary Deduction", String.format("-$%.2f", salaryDeduction), cellFont);

            document.add(table);

            // Summary
            PdfPTable summaryTable = new PdfPTable(2);
            summaryTable.setWidthPercentage(100);
            summaryTable.setSpacingBefore(10f);

            com.itextpdf.text.Font totalFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 14, com.itextpdf.text.Font.BOLD);
            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total Salary", totalFont));
            totalLabelCell.setPadding(10);
            totalLabelCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            summaryTable.addCell(totalLabelCell);

            PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.format("$%.2f", totalSalary), totalFont));
            totalAmountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
            totalAmountCell.setPadding(10);
            totalAmountCell.setBackgroundColor(BaseColor.LIGHT_GRAY);
            summaryTable.addCell(totalAmountCell);

            document.add(summaryTable);

            // Footer
            com.itextpdf.text.Font footerFont = new com.itextpdf.text.Font(com.itextpdf.text.Font.FontFamily.HELVETICA, 10, com.itextpdf.text.Font.ITALIC);
            document.add(new Paragraph("Thank you for your work", footerFont));
            document.add(new Paragraph("Garment Factory", footerFont));
            document.add(new Paragraph("CEO: Than PuHour", footerFont));
            document.add(new Paragraph("Location: Kampong Cham (city)", footerFont));
            document.add(new Paragraph("Month(s): " + onMonths, footerFont));

        } finally {
            document.close();
        }

        System.out.println("Payroll receipt generated for: " + employeeName);
    }

    private PdfPCell createCell(String content, com.itextpdf.text.Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(content, font));
        cell.setPadding(5);
        cell.setMinimumHeight(25);
        return cell;
    }

    private void addRow(PdfPTable table, String col1, String col2, String col3, com.itextpdf.text.Font font) {
        table.addCell(createCell(col1, font));
        table.addCell(createCell(col2, font));
        table.addCell(createCell(col3, font));
    }

    private boolean isValidMonthFormat(String input) {
        try {
            YearMonth.parse(input, DateTimeFormatter.ofPattern("MM:yyyy"));
            return true;
        } catch (DateTimeParseException e) {
            return false;
        }
    }

    private void printSelectedEmployees() {
        int[] selectedRows = jTablePayrolls.getSelectedRows();
        if (selectedRows.length == 0) {
            JOptionPane.showMessageDialog(this, "No employees selected. Please select at least one row.", "Print Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        DefaultTableModel model = (DefaultTableModel) jTablePayrolls.getModel();
        List<String> printedEmployees = new ArrayList<>();

        for (int selectedRow : selectedRows) {
            int modelRow = jTablePayrolls.convertRowIndexToModel(selectedRow);
            String employeeName = (String) model.getValueAt(modelRow, 0);
            double basicSalary = (double) model.getValueAt(modelRow, 1);
            String onMonths = (String) model.getValueAt(modelRow, 2);
            String department = (String) model.getValueAt(modelRow, 3);
            String teamLeader = (String) model.getValueAt(modelRow, 4);
            int totalOvertime = (int) model.getValueAt(modelRow, 5);
            double overtimeAmount = (double) model.getValueAt(modelRow, 6);
            int totalLateTime = (int) model.getValueAt(modelRow, 7);
            double lateTimeAmount = (double) model.getValueAt(modelRow, 8);
            int totalWorkTime = (int) model.getValueAt(modelRow, 9);
            double workTimeAmount = (double) model.getValueAt(modelRow, 10);
            int leaveDays = (int) model.getValueAt(modelRow, 11);
            double leaveDaysAmount = (double) model.getValueAt(modelRow, 12);
            double bonus = (double) model.getValueAt(modelRow, 13);
            double salaryDeduction = (double) model.getValueAt(modelRow, 14);
            double totalSalary = (double) model.getValueAt(modelRow, 15);

            try {
                generatePDFReceipt(employeeName, basicSalary, onMonths, department, teamLeader, totalOvertime,
                        overtimeAmount, totalLateTime, lateTimeAmount, totalWorkTime, workTimeAmount, leaveDays,
                        leaveDaysAmount, bonus, salaryDeduction, totalSalary);
                printedEmployees.add(employeeName);
            } catch (DocumentException | IOException e) {
                JOptionPane.showMessageDialog(this, "Error printing employee: " + employeeName + "\n" + e.getMessage(), "Print Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        if (!printedEmployees.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Payroll printed successfully for:\n" + String.join(", ", printedEmployees), "Success", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void printAllEmployees() {
        DefaultTableModel model = (DefaultTableModel) jTablePayrolls.getModel();
        int rowCount = model.getRowCount();
        List<String> printedEmployees = new ArrayList<>();
        List<String> failedEmployees = new ArrayList<>();

        for (int i = 0; i < rowCount; i++) {
            String employeeName = (String) model.getValueAt(i, 0);
            double basicSalary = (double) model.getValueAt(i, 1);
            String onMonths = (String) model.getValueAt(i, 2);
            String department = (String) model.getValueAt(i, 3);
            String teamLeader = (String) model.getValueAt(i, 4);
            int totalOvertime = (int) model.getValueAt(i, 5);
            double overtimeAmount = (double) model.getValueAt(i, 6);
            int totalLateTime = (int) model.getValueAt(i, 7);
            double lateTimeAmount = (double) model.getValueAt(i, 8);
            int totalWorkTime = (int) model.getValueAt(i, 9);
            double workTimeAmount = (double) model.getValueAt(i, 10);
            int leaveDays = (int) model.getValueAt(i, 11);
            double leaveDaysAmount = (double) model.getValueAt(i, 12);
            double bonus = (double) model.getValueAt(i, 13);
            double salaryDeduction = (double) model.getValueAt(i, 14);
            double totalSalary = (double) model.getValueAt(i, 15);

            try {
                generatePDFReceipt(employeeName, basicSalary, onMonths, department, teamLeader, totalOvertime,
                        overtimeAmount, totalLateTime, lateTimeAmount, totalWorkTime, workTimeAmount, leaveDays,
                        leaveDaysAmount, bonus, salaryDeduction, totalSalary);
                printedEmployees.add(employeeName);
            } catch (DocumentException | IOException e) {
                failedEmployees.add(employeeName);
            }
        }

        StringBuilder message = new StringBuilder();
        if (!printedEmployees.isEmpty()) {
            message.append("Payroll printed successfully for:\n").append(String.join(", ", printedEmployees)).append("\n\n");
        }
        if (!failedEmployees.isEmpty()) {
            message.append("Failed to print payroll for:\n").append(String.join(", ", failedEmployees));
        }

        JOptionPane.showMessageDialog(this, message.toString(), "Print All Results", JOptionPane.INFORMATION_MESSAGE);
    }

    private void filterTable() {
        String text = jTextFieldSearch.getText();
        if (text.trim().length() == 0) {
            sorter.setRowFilter(null);
        } else {
            sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0)); // Filter by employee name (column 0)
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
            new Payrolls().setVisible(true);
        });
    }
}
