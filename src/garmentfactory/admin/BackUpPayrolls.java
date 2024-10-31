//package garmentfactory.admin;
//
//import javax.swing.*;
//import javax.swing.table.DefaultTableModel;
//import javax.swing.table.JTableHeader;
//import java.awt.*;
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.IOException;
//import java.sql.*;
//import java.time.YearMonth;
//import java.time.format.DateTimeFormatter;
//import java.time.format.DateTimeParseException;
//import com.itextpdf.text.Document;
//import com.itextpdf.text.DocumentException;
//import com.itextpdf.text.Font;
//import com.itextpdf.text.Paragraph;
//import com.itextpdf.text.Phrase;
//import com.itextpdf.text.pdf.PdfPCell;
//import com.itextpdf.text.pdf.PdfPTable;
//import com.itextpdf.text.pdf.PdfWriter;
//
//public class Payrolls extends javax.swing.JFrame {
//
//    private JTextField jTextFieldCurrentMonth;
//    private JButton jButtonCurrentMonth, jButtonGeneratePayroll, jButtonBack;
//    private JTable jTablePayrolls;
//    private JScrollPane jScrollPanePayrolls;
//    private Connection connection;
//
//    public Payrolls() {
//        initComponents();
//        initDatabase();
//        setLookAndFeel();
//    }
//
//    private void setCurrentMonth() {
//        YearMonth currentYearMonth = YearMonth.now();
//        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM:yyyy");
//        jTextFieldCurrentMonth.setText(currentYearMonth.format(formatter));
//    }
//
//    private void setBack() {
//        setVisible(false);
//    }
//
//    private void initComponents() {
//        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        setUndecorated(true);
//        setTitle("Modern Payroll Generation");
//        setLocationRelativeTo(null);
//        jButtonBack = new JButton("Back");
//        jTextFieldCurrentMonth = new JTextField(7);
//        jButtonCurrentMonth = new JButton("Set Current Month");
//        jButtonGeneratePayroll = new JButton("Generate Payroll");
//        jTablePayrolls = new JTable();
//        jScrollPanePayrolls = new JScrollPane(jTablePayrolls);
//
//        JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 10));
//        topPanel.add(jButtonBack);
//        topPanel.add(new JLabel("Current Month (MM:YYYY):"));
//        topPanel.add(jTextFieldCurrentMonth);
//        topPanel.add(jButtonCurrentMonth);
//        topPanel.add(jButtonGeneratePayroll);
//
//        setLayout(new BorderLayout());
//        add(topPanel, BorderLayout.NORTH);
//        add(jScrollPanePayrolls, BorderLayout.CENTER);
//
//        jButtonBack.addActionListener(e -> setBack());
//        jButtonCurrentMonth.addActionListener(e -> setCurrentMonth());
//        jButtonGeneratePayroll.addActionListener(e -> generatePayrolls());
//
//        setPreferredSize(new Dimension(1880, 1000)); // Set preferred size
//        pack(); // Pack the components to respect preferred size
//        setResizable(false); // Disable resizability
//        setLocationRelativeTo(null); // Center window on screen
//
//        styleComponents();
//    }
//
//    private void styleComponents() {
//        Color primaryColor = new Color(41, 128, 185);
//        Color secondaryColor = new Color(52, 152, 219);
//        Color successColor = new Color(34, 187, 51);
//        Color textColor = new Color(236, 240, 241);
//        Color dangerColor = new Color(187, 33, 36);
//
//        JPanel topPanel = (JPanel) getContentPane().getComponent(0);
//        topPanel.setBackground(primaryColor);
//        topPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
//
//        for (Component comp : topPanel.getComponents()) {
//            if (comp instanceof JLabel) {
//                JLabel label = (JLabel) comp;
//                label.setForeground(textColor);
//                label.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));
//
//            }
//        }
//
//        jTextFieldCurrentMonth.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 30));
//
//        jTextFieldCurrentMonth.setBorder(BorderFactory.createCompoundBorder(
//                BorderFactory.createLineBorder(secondaryColor),
//                BorderFactory.createEmptyBorder(5, 5, 5, 5)));
//
//        styleButton(jButtonBack, dangerColor, textColor);
//        styleButton(jButtonCurrentMonth, secondaryColor, textColor);
//        styleButton(jButtonGeneratePayroll, successColor, textColor);
//
//        jTablePayrolls.setFont(new java.awt.Font("Arial", java.awt.Font.PLAIN, 14));
//
//        jTablePayrolls.setRowHeight(25);
//        jTablePayrolls.setIntercellSpacing(new Dimension(10, 10));
//        jTablePayrolls.setGridColor(new Color(189, 195, 199));
//
//        JTableHeader header = jTablePayrolls.getTableHeader();
//        header.setBackground(primaryColor);
//        header.setForeground(textColor);
//        header.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 14));
//
//        jScrollPanePayrolls.setBorder(BorderFactory.createEmptyBorder());
//    }
//
//    private void styleButton(JButton button, Color bgColor, Color fgColor) {
//        button.setBackground(bgColor);
//        button.setForeground(fgColor);
//        button.setFocusPainted(false);
//        button.setBorderPainted(false);
//        button.setFont(new java.awt.Font("Arial", java.awt.Font.BOLD, 30));
//
//        button.setPreferredSize(new Dimension(300, 40));
//        button.setCursor(new Cursor(Cursor.HAND_CURSOR));
//    }
//
//    private void setLookAndFeel() {
//        try {
//            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }
//
//    private void initDatabase() {
//        try {
//            Class.forName("com.mysql.cj.jdbc.Driver");
//            String url = "jdbc:mysql://localhost:3306/garmentfactory";
//            connection = DriverManager.getConnection(url, "root", "");
//        } catch (ClassNotFoundException | SQLException e) {
//            JOptionPane.showMessageDialog(this, "Failed to connect to the database: " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
//        }
//    }
//
//    private void generatePayrolls() {
//        String currentMonth = jTextFieldCurrentMonth.getText();
//        if (!isValidMonthFormat(currentMonth)) {
//            JOptionPane.showMessageDialog(this, "Invalid month format. Please use MM:YYYY", "Input Error", JOptionPane.ERROR_MESSAGE);
//            return;
//        }
//
//        YearMonth yearMonth = YearMonth.parse(currentMonth, DateTimeFormatter.ofPattern("MM:yyyy"));
//        int month = yearMonth.getMonthValue();
//        int year = yearMonth.getYear();
//
//        String sql = "WITH employee_attendance AS ("
//                + "    SELECT"
//                + "        e.employee_name," // Keep employee_name
//                + "        e.basic_salary,"
//                + "        SUM(a.late_time) AS total_late_time,"
//                + "        SUM(a.overtime) AS total_overtime,"
//                + "        SUM(a.work_time) AS total_work_time,"
//                + "        ROUND(SUM(a.late_time) * (e.basic_salary / 26 / 8), 2) AS late_deduction,"
//                + "        ROUND(SUM(a.overtime) * (e.basic_salary / 26 / 8) * 2, 2) AS overtime_payment,"
//                + "        ROUND(SUM(a.work_time) * (e.basic_salary / 26 / 8), 2) AS salary_earned"
//                + "    FROM"
//                + "        attendance a"
//                + "    JOIN employees e ON a.employee_id = e.employee_id"
//                + "    WHERE MONTH(a.date) = ? AND YEAR(a.date) = ?"
//                + "    GROUP BY e.employee_name, e.basic_salary" // Group by employee_name and basic_salary
//                + "),"
//                + "employee_leaves AS ("
//                + "    SELECT"
//                + "        e.employee_name," // Use employee_name instead of employee_id
//                + "        e.basic_salary,"
//                + "        COALESCE(SUM(DATEDIFF(l.end_date, l.start_date) + 1), 0) AS leave_days,"
//                + "        ROUND(COALESCE(SUM((DATEDIFF(l.end_date, l.start_date) + 1) * (e.basic_salary / 26)), 0), 2) AS leave_salary"
//                + "    FROM"
//                + "        leaves l"
//                + "    RIGHT JOIN employees e ON l.employee_id = e.employee_id"
//                + "    WHERE (MONTH(l.start_date) = ? AND YEAR(l.start_date) = ?)"
//                + "        OR (MONTH(l.end_date) = ? AND YEAR(l.end_date) = ?)"
//                + "    GROUP BY e.employee_name, e.basic_salary"
//                + "),"
//                + "payroll_summary AS ("
//                + "    SELECT"
//                + "        ea.employee_name," // Select employee_name
//                + "        ea.basic_salary,"
//                + "        ea.total_late_time,"
//                + "        ea.total_overtime,"
//                + "        ea.total_work_time,"
//                + "        ea.salary_earned,"
//                + "        ea.late_deduction,"
//                + "        ea.overtime_payment,"
//                + "        COALESCE(el.leave_days, 0) AS leave_days,"
//                + "        COALESCE(el.leave_salary, 0) AS leave_salary,"
//                + "        ROUND(CASE "
//                + "            WHEN ea.total_late_time = 0 AND el.leave_days = 0 THEN ea.basic_salary * 0.10"
//                + "            ELSE 0 "
//                + "        END, 2) AS bonus"
//                + "    FROM"
//                + "        employee_attendance ea"
//                + "    LEFT JOIN employee_leaves el ON ea.employee_name = el.employee_name"
//                + ")"
//                + "SELECT"
//                + "    ps.employee_name," // Select employee_name from payroll_summary
//                + "    ROUND(ps.basic_salary, 2) AS basic_salary,"
//                + "    ? AS on_months,"
//                + "    d.department_name AS department,"
//                + "    u.username AS team_leader,"
//                + "    ps.total_overtime,"
//                + "    ROUND(ps.overtime_payment, 2) AS overtime_amount,"
//                + "    ps.total_late_time,"
//                + "    ROUND(ps.late_deduction, 2) AS late_time_amount,"
//                + "    ps.total_work_time,"
//                + "    ROUND(ps.salary_earned, 2) AS work_time_amount,"
//                + "    ps.leave_days,"
//                + "    ROUND(ps.leave_salary, 2) AS leave_days_amount,"
//                + "    ps.bonus,"
//                + "    ROUND( ps.late_deduction + ps.leave_salary, 2 ) AS salary_deduction,"
//                + "    ROUND( ps.basic_salary + ps.overtime_payment + ps.salary_earned + ps.bonus - ( ps.late_deduction + ps.leave_salary ), 2 ) AS total_salary "
//                + "FROM"
//                + "    payroll_summary ps "
//                + "JOIN employees e ON ps.employee_name = e.employee_name "
//                + "JOIN departments d ON e.department_id = d.department_id "
//                + "JOIN team_leader tl ON e.employee_id = tl.employee_id "
//                + "JOIN users u ON tl.user_id = u.user_id";
//
//        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
//            pstmt.setInt(1, month);
//            pstmt.setInt(2, year);
//            pstmt.setInt(3, month);
//            pstmt.setInt(4, year);
//            pstmt.setInt(5, month);
//            pstmt.setInt(6, year);
//            pstmt.setString(7, String.format("%02d-%04d", month, year));
//
//            try (ResultSet rs = pstmt.executeQuery()) {
//                DefaultTableModel model = new DefaultTableModel(new String[]{
//                    "Employee Name", "Basic Salary", "On Months", "Department", "Team Leader",
//                    "Total Overtime", "Overtime Amount", "Total Late Time", "Late Time Amount",
//                    "Total Work Time", "Work Time Amount", "Leave Days", "Leave Days Amount",
//                    "Bonus", "Salary Deduction", "Total Salary"
//                }, 0);
//
//                while (rs.next()) {
//                    Object[] row = {
//                        rs.getString("employee_name"),
//                        rs.getDouble("basic_salary"),
//                        rs.getString("on_months"),
//                        rs.getString("department"),
//                        rs.getString("team_leader"),
//                        rs.getInt("total_overtime"),
//                        rs.getDouble("overtime_amount"),
//                        rs.getInt("total_late_time"),
//                        rs.getDouble("late_time_amount"),
//                        rs.getInt("total_work_time"),
//                        rs.getDouble("work_time_amount"),
//                        rs.getInt("leave_days"),
//                        rs.getDouble("leave_days_amount"),
//                        rs.getDouble("bonus"),
//                        rs.getDouble("salary_deduction"),
//                        rs.getDouble("total_salary")
//                    };
//                    model.addRow(row);
//
//                    generatePDFReceipt(
//                            rs.getString("employee_name"),
//                            rs.getDouble("basic_salary"),
//                            rs.getString("on_months"),
//                            rs.getString("department"),
//                            rs.getString("team_leader"),
//                            rs.getInt("total_overtime"),
//                            rs.getDouble("overtime_amount"),
//                            rs.getInt("total_late_time"),
//                            rs.getDouble("late_time_amount"),
//                            rs.getInt("total_work_time"),
//                            rs.getDouble("work_time_amount"),
//                            rs.getInt("leave_days"),
//                            rs.getDouble("leave_days_amount"),
//                            rs.getDouble("bonus"),
//                            rs.getDouble("salary_deduction"),
//                            rs.getDouble("total_salary")
//                    );
//                }
//
//                jTablePayrolls.setModel(model);
//                JOptionPane.showMessageDialog(this, "Payroll generated successfully!", "Success", JOptionPane.INFORMATION_MESSAGE);
//            }
//        } catch (SQLException | IOException | DocumentException e) {
//            JOptionPane.showMessageDialog(this, "Error generating payrolls: " + e.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
//        }
//    }
//
//    public void generatePDFReceipt(String employeeName, double basicSalary, String onMonths, String department,
//            String teamLeader, int totalOvertime, double overtimeAmount, int totalLateTime,
//            double lateTimeAmount, int totalWorkTime, double workTimeAmount, int leaveDays,
//            double leaveDaysAmount, double bonus, double salaryDeduction, double totalSalary)
//            throws DocumentException, IOException {
//
//        String directory = "D:\\PuHour_DATA\\Java-Garment-Factory\\Payroll_Receipt";
//        File dir = new File(directory);
//        if (!dir.exists()) {
//            dir.mkdirs();
//        }
//
//        String fileName = employeeName.replaceAll("\\s+", "_") + ".pdf";
//        String fullPath = directory + "\\" + fileName;
//
//        // Create a new document
//        Document document = new Document();
//        PdfWriter.getInstance(document, new FileOutputStream(fullPath));
//
//        try {
//            // Open the document
//            document.open();
//
//            // Header
//            PdfPTable headerTable = new PdfPTable(2);
//            headerTable.setWidthPercentage(100);
//            headerTable.setSpacingBefore(10f);
//            headerTable.setSpacingAfter(10f);
//
//            PdfPCell logoCell = new PdfPCell(new Phrase("Than PuHour"));
//            logoCell.setBorder(PdfPCell.NO_BORDER);
//            logoCell.setPadding(10); // Increase padding for logo
//            headerTable.addCell(logoCell);
//
//            document.add(headerTable);
//
//            // Recipient Info
//            document.add(new Paragraph("Your Name: " + employeeName));
//            document.add(new Paragraph("Department: " + department));
//            document.add(new Paragraph("Team Leader: " + teamLeader));
//            document.add(new Paragraph("Payroll On Month(s): " + onMonths));
//            document.add(new Paragraph(" ")); // blank line for spacing
//
//            // Table with payroll details
//            PdfPTable table = new PdfPTable(3);
//            table.setWidthPercentage(100);
//            table.setSpacingBefore(10f);
//            table.setSpacingAfter(10f);
//
//            float[] columnWidths = {0.1f, 0.6f, 0.3f};
//            table.setWidths(columnWidths);
//
//            // Set table headers
//            PdfPCell cell;
//
//            cell = new PdfPCell(new Phrase("No", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
//            cell.setPadding(10); // Increase padding for header
//            cell.setMinimumHeight(30); // Set minimum height for header cells
//            table.addCell(cell);
//
//            cell = new PdfPCell(new Phrase("Payment Details", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
//            cell.setPadding(10);
//            cell.setMinimumHeight(30);
//            table.addCell(cell);
//
//            cell = new PdfPCell(new Phrase("Amount", new Font(Font.FontFamily.HELVETICA, 12, Font.BOLD)));
//            cell.setPadding(10);
//            cell.setMinimumHeight(30);
//            table.addCell(cell);
//
//            // Add details rows with bigger cell sizes
//            table.addCell(createCell("1"));
//            table.addCell(createCell("Basic Salary"));
//            table.addCell(createCell(String.format("$%.2f", basicSalary)));
//
//            table.addCell(createCell("2"));
//            table.addCell(createCell("Total Overtime (" + totalOvertime + " hours)"));
//            table.addCell(createCell(String.format("$%.2f", overtimeAmount)));
//
//            table.addCell(createCell("3"));
//            table.addCell(createCell("Total Late Time (" + totalLateTime + " hours)"));
//            table.addCell(createCell(String.format("-$%.2f", lateTimeAmount)));
//
//            table.addCell(createCell("4"));
//            table.addCell(createCell("Total Work Time (" + totalWorkTime + " hours)"));
//            table.addCell(createCell(String.format("$%.2f", workTimeAmount)));
//
//            table.addCell(createCell("5"));
//            table.addCell(createCell("Leave Days (" + leaveDays + " days)"));
//            table.addCell(createCell(String.format("-$%.2f", leaveDaysAmount)));
//
//            table.addCell(createCell("6"));
//            table.addCell(createCell("Bonus"));
//            table.addCell(createCell(String.format("$%.2f", bonus)));
//
//            table.addCell(createCell("7"));
//            table.addCell(createCell("Salary Deduction"));
//            table.addCell(createCell(String.format("-$%.2f", salaryDeduction)));
//
//            document.add(table);
//
//            // Summary
//            PdfPTable summaryTable = new PdfPTable(2);
//            summaryTable.setWidthPercentage(100);
//            summaryTable.setSpacingBefore(10f);
//
//            PdfPCell totalLabelCell = new PdfPCell(new Phrase("Total Salary", new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD)));
//            totalLabelCell.setPadding(16);
//            totalLabelCell.setMinimumHeight(40);
//            summaryTable.addCell(totalLabelCell);
//
//            PdfPCell totalAmountCell = new PdfPCell(new Phrase(String.format("$%.2f", totalSalary), new Font(Font.FontFamily.HELVETICA, 20, Font.BOLD)));
//            totalAmountCell.setPadding(16);
//            totalAmountCell.setMinimumHeight(40);
//            summaryTable.addCell(totalAmountCell);
//
//            document.add(summaryTable);
//
//            // Footer
//            document.add(new Paragraph("Thank you for your working"));
//            document.add(new Paragraph("Garment Factory"));
//            document.add(new Paragraph("CEO : Than PuHour"));
//            document.add(new Paragraph("Location : Kampong Cham (city)"));
//            document.add(new Paragraph("Month(s): " + onMonths));
//
//        } catch (DocumentException e) {
//            e.printStackTrace();
//        } finally {
//            // Close the document
//            document.close();
//        }
//
//        System.out.println("Payroll receipt generated for: " + employeeName);
//    }
//
//// Helper method to create a cell with bigger padding and minimum height
//    private PdfPCell createCell(String content) {
//        PdfPCell cell = new PdfPCell(new Phrase(content));
//        cell.setPadding(10); // Increase padding for all cells
//        cell.setMinimumHeight(25); // Set minimum height for all cells
//        return cell;
//    }
//
//    private boolean isValidMonthFormat(String input) {
//        try {
//            YearMonth.parse(input, DateTimeFormatter.ofPattern("MM:yyyy"));
//            return true;
//        } catch (DateTimeParseException e) {
//            return false;
//        }
//    }
//
//    public static void main(String args[]) {
//        // Using a lambda expression for a cleaner approach
//        java.awt.EventQueue.invokeLater(() -> {
//            new Payrolls().setVisible(true);
//        });
//    }
//}
