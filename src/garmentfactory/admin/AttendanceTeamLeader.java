package garmentfactory.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class AttendanceTeamLeader extends JFrame {

    private JTextField employeeField, startDateField, endDateField, searchField;
    private JTextArea leaveReasonArea;
    private JButton todayButton, removeLeaveButton, clearButton, saveButton, logoutButton;
    private JTable presentTable, absentTable;
    private JLabel totalPresentLabel, totalAbsentLabel, jLabelUserName;
    private DefaultTableModel presentModel, absentModel;
    private final LocalDate currentDate = LocalDate.now();
    private final String teamLeaderName;
    private final int loggedInUserId;
    private Connection conn;

    public AttendanceTeamLeader(String teamLeaderName, int loggedInUserId) {
        this.teamLeaderName = teamLeaderName;
        this.loggedInUserId = loggedInUserId;
        initializeConnection();
        initializeUI();
        loadUserName();
        loadAttendanceData();
    }

    private void initializeConnection() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/garmentfactory", "root", "");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error establishing database connection", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void initializeUI() {
        setTitle("Garment Factory Management System");
        setSize(1880, 1000);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // Creating a JSplitPane to divide the left and right components
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(530);
        splitPane.setOneTouchExpandable(true);
        add(splitPane, BorderLayout.CENTER);

        // Left panel for employee actions
        JPanel leftPanel = new JPanel(new GridBagLayout());
        setupLeftPanel(leftPanel);
        splitPane.setLeftComponent(leftPanel);

        // Right panel for tables
        JPanel rightPanel = new JPanel(new BorderLayout());
        setupRightPanel(rightPanel);
        splitPane.setRightComponent(rightPanel);
    }

    private void setupLeftPanel(JPanel leftPanel) {
        Font largerFont = new Font("Arial", Font.PLAIN, 23); // Define a larger font

        // Creating the first part of the split pane for date and team leader information
        JPanel dateTeamLeaderPanel = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 2;

        JLabel dateLabel = new JLabel("Date Time: " + currentDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        dateLabel.setFont(largerFont);
        dateTeamLeaderPanel.add(dateLabel, gbc);

        gbc.gridy++;
        JLabel teamLeaderLabel = new JLabel("Team Leader: " + loadUserName());
        teamLeaderLabel.setFont(largerFont);
        dateTeamLeaderPanel.add(teamLeaderLabel, gbc);

        // Creating the second part of the split pane for employee and leave information
        JPanel employeeLeavePanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 1;

        JLabel employeeLabel = new JLabel("Employee:");
        employeeLabel.setFont(largerFont);
        employeeLeavePanel.add(employeeLabel, gbc);

        gbc.gridx = 1;
        employeeField = new JTextField();
        employeeField.setFont(largerFont);
        employeeField.setEnabled(false);
        employeeField.setDisabledTextColor(Color.MAGENTA);
        employeeLeavePanel.add(employeeField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel leaveReasonLabel = new JLabel("Leave Reason:");
        leaveReasonLabel.setFont(largerFont);
        employeeLeavePanel.add(leaveReasonLabel, gbc);

        gbc.gridx = 1;
        leaveReasonArea = new JTextArea(3, 10);
        leaveReasonArea.setFont(largerFont);
        employeeLeavePanel.add(new JScrollPane(leaveReasonArea), gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel startDateLabel = new JLabel("Start Date:");
        startDateLabel.setFont(largerFont);
        employeeLeavePanel.add(startDateLabel, gbc);

        gbc.gridx = 1;
        startDateField = new JTextField();
        startDateField.setFont(largerFont);
        employeeLeavePanel.add(startDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        JLabel endDateLabel = new JLabel("End Date:");
        endDateLabel.setFont(largerFont);
        employeeLeavePanel.add(endDateLabel, gbc);

        gbc.gridx = 1;
        endDateField = new JTextField();
        endDateField.setFont(largerFont);
        employeeLeavePanel.add(endDateField, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        todayButton = new JButton("Today");
        todayButton.setFont(largerFont);
        todayButton.addActionListener(e -> setTodayDate());
        employeeLeavePanel.add(todayButton, gbc);

        gbc.gridy++;
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        removeLeaveButton = new JButton("Remove Leave");

        removeLeaveButton.setBackground(Color.ORANGE);
        removeLeaveButton.setForeground(Color.RED);
        removeLeaveButton.setFont(largerFont);
        removeLeaveButton.addActionListener(e -> removeLeave());
        buttonPanel.add(removeLeaveButton);

        clearButton = new JButton("Clear");
        clearButton.setBackground(Color.RED);
        clearButton.setForeground(Color.WHITE);
        clearButton.setFont(largerFont);
        clearButton.addActionListener(e -> clearLeaveFields());
        buttonPanel.add(clearButton);

        saveButton = new JButton("Save");
        saveButton.setBackground(Color.GREEN);
        saveButton.setForeground(Color.WHITE);
        saveButton.setFont(largerFont);
        saveButton.addActionListener(e -> saveLeave());
        buttonPanel.add(saveButton);

        employeeLeavePanel.add(buttonPanel, gbc);

        // Creating the third part of the split pane for logout button
        JPanel logoutPanel = new JPanel(new GridBagLayout());
        gbc.gridx = 0;
        gbc.gridy = 0;
        logoutButton = new JButton("Logout");
        logoutButton.setBackground(Color.CYAN);
        logoutButton.setForeground(Color.BLACK);
        logoutButton.setFont(largerFont);
        logoutButton.addActionListener(e -> logoutButton());

        logoutPanel.add(logoutButton, gbc);

        // Create JSplitPanes to divide the panels
        JSplitPane splitPane1 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, dateTeamLeaderPanel, employeeLeavePanel);
        splitPane1.setDividerLocation(200); // Adjust the divider location as needed
        splitPane1.setOneTouchExpandable(true);

        JSplitPane splitPane2 = new JSplitPane(JSplitPane.VERTICAL_SPLIT, splitPane1, logoutPanel);
        splitPane2.setDividerLocation(800); // Adjust the divider location as needed
        splitPane2.setOneTouchExpandable(true);

        leftPanel.setLayout(new BorderLayout());
        leftPanel.add(splitPane2, BorderLayout.CENTER);
    }

    private void setupRightPanel(JPanel rightPanel) {
        Font largerFont = new Font("Arial", Font.PLAIN, 20); // Define a larger font
        JTabbedPane tabbedPane = new JTabbedPane();

        // Present Table Panel
        presentModel = new DefaultTableModel(new String[]{"Employee Name", "Date", "Check In", "Check Out", "Late Time", "Overtime", "Work Time"}, 0);
        presentTable = new JTable(presentModel);
        presentTable.setFont(largerFont);
        presentTable.setRowHeight(25); // Increase row height to make it more readable
        presentTable.setRowSorter(new TableRowSorter<>(presentModel));
        JScrollPane presentScrollPane = new JScrollPane(presentTable);
        tabbedPane.addTab("Present Employees", presentScrollPane);

        // Absent Table Panel
        JPanel absentPanel = new JPanel(new BorderLayout());
        absentModel = new DefaultTableModel(new String[]{"Employee Name", "Phone", "Position", "Reason", "Start Date", "End Date"}, 0);
        absentTable = new JTable(absentModel);
        absentTable.setFont(largerFont);
        absentTable.setRowHeight(25);
        absentTable.setRowSorter(new TableRowSorter<>(absentModel));
        absentTable.getSelectionModel().addListSelectionListener(e -> setSelectedEmployee());
        JScrollPane absentScrollPane = new JScrollPane(absentTable);
        absentPanel.add(absentScrollPane, BorderLayout.CENTER);

        // Search Panel inside Absent Table Panel
        JPanel searchPanel = new JPanel(new BorderLayout(5, 5));
        searchPanel.setBorder(BorderFactory.createTitledBorder("Search"));
        searchPanel.add(new JLabel("Search Employee Name:"), BorderLayout.WEST);
        searchField = new JTextField();
        searchField.setFont(largerFont);
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                filterAbsentTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                filterAbsentTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                filterAbsentTable();
            }
        });
        searchPanel.add(searchField, BorderLayout.CENTER);
        absentPanel.add(searchPanel, BorderLayout.NORTH);

        tabbedPane.addTab("Absent Employees", absentPanel);

        rightPanel.add(tabbedPane, BorderLayout.CENTER);

        // Footer Panel for Totals
        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 50, 5));
        totalPresentLabel = new JLabel("Total Present: 0");
        totalPresentLabel.setFont(largerFont);
        totalAbsentLabel = new JLabel("Total Absent: 0");
        totalAbsentLabel.setFont(largerFont);
        footerPanel.add(totalPresentLabel);
        footerPanel.add(totalAbsentLabel);
        rightPanel.add(footerPanel, BorderLayout.SOUTH);
    }

    private void setTodayDate() {
        String today = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        startDateField.setText(today);
        endDateField.setText(today);
    }

    private void logoutButton() {
        this.dispose();
        new Login().setVisible(true);
    }

    private void setSelectedEmployee() {
        if (!absentTable.getSelectionModel().getValueIsAdjusting() && absentTable.getSelectedRow() != -1) {
            String employeeName = absentModel.getValueAt(absentTable.getSelectedRow(), 0).toString();
            employeeField.setText(employeeName);
        }
    }

    private void loadAttendanceData() {
        try {
            loadPresentData();
            loadAbsentData();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading attendance data", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadPresentData() throws SQLException {
        String presentQuery = "SELECT e.employee_name, a.date, a.check_in, a.check_out, a.late_time, a.overtime, a.work_time "
                + "FROM attendance a JOIN employees e ON a.employee_id = e.employee_id "
                + "JOIN team_leader tl ON e.employee_id = tl.employee_id "
                + "WHERE tl.user_id = ? AND a.date = ?";
        try (PreparedStatement presentStmt = conn.prepareStatement(presentQuery)) {
            presentStmt.setInt(1, loggedInUserId);
            presentStmt.setDate(2, java.sql.Date.valueOf(currentDate));
            try (ResultSet presentRs = presentStmt.executeQuery()) {
                presentModel.setRowCount(0); // Clear previous data
                while (presentRs.next()) {
                    presentModel.addRow(new Object[]{
                        presentRs.getString("employee_name"),
                        presentRs.getDate("date"),
                        presentRs.getTime("check_in"),
                        presentRs.getTime("check_out"),
                        presentRs.getInt("late_time"),
                        presentRs.getInt("overtime"),
                        presentRs.getInt("work_time")
                    });
                }
            }
        }
        totalPresentLabel.setText("Total Present: " + presentModel.getRowCount());
    }

    private void loadAbsentData() throws SQLException {
        String absentQuery = "SELECT e.employee_name, e.phone, e.position, l.reason, l.start_date, l.end_date "
                + "FROM employees e "
                + "JOIN team_leader tl ON e.employee_id = tl.employee_id "
                + "LEFT JOIN attendance a ON e.employee_id = a.employee_id AND a.date = ? "
                + "LEFT JOIN leaves l ON e.employee_id = l.employee_id "
                + "WHERE tl.user_id = ? AND a.employee_id IS NULL";
        try (PreparedStatement absentStmt = conn.prepareStatement(absentQuery)) {
            absentStmt.setDate(1, java.sql.Date.valueOf(currentDate));
            absentStmt.setInt(2, loggedInUserId);
            try (ResultSet absentRs = absentStmt.executeQuery()) {
                absentModel.setRowCount(0); // Clear previous data
                while (absentRs.next()) {
                    absentModel.addRow(new Object[]{
                        absentRs.getString("employee_name"),
                        absentRs.getString("phone"),
                        absentRs.getString("position"),
                        absentRs.getString("reason"),
                        absentRs.getDate("start_date"),
                        absentRs.getDate("end_date")
                    });
                }
            }
        }
        totalAbsentLabel.setText("Total Absent: " + absentModel.getRowCount());
    }

    private void filterAbsentTable() {
        String searchText = searchField.getText().toLowerCase();
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(absentModel);
        absentTable.setRowSorter(sorter);
        sorter.setRowFilter(RowFilter.regexFilter("(?i)" + searchText));
    }

    private String loadUserName() {
        String query = "SELECT username FROM users WHERE user_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setInt(1, loggedInUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("username");
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading username", "Error", JOptionPane.ERROR_MESSAGE);
        }
        return "Unknown";
    }

    private void saveLeave() {
        String employeeName = employeeField.getText();
        String reason = leaveReasonArea.getText();
        String startDate = startDateField.getText();
        String endDate = endDateField.getText();

        if (employeeName.isEmpty() || reason.isEmpty() || startDate.isEmpty() || endDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please fill in all fields", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "INSERT INTO leaves (employee_id, reason, start_date, end_date) "
                + "VALUES ((SELECT employee_id FROM employees WHERE employee_name = ?), ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, employeeName);
            ps.setString(2, reason);
            ps.setString(3, startDate);
            ps.setString(4, endDate);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Leave saved successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAttendanceData();
            clearLeaveFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error saving leave", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void removeLeave() {
        String employeeName = employeeField.getText();
        if (employeeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an employee", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        String query = "DELETE FROM leaves WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?)";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, employeeName);
            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "Leave removed successfully", "Success", JOptionPane.INFORMATION_MESSAGE);
            loadAttendanceData();
            clearLeaveFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error removing leave", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void clearLeaveFields() {
        employeeField.setText("");
        leaveReasonArea.setText("");
        startDateField.setText("");
        endDateField.setText("");
        loadUserName();
        loadAttendanceData();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            String teamLeaderName = "Johnny";
            int loggedInUserId = 6;
            new AttendanceTeamLeader(teamLeaderName, loggedInUserId).setVisible(true);
        });
    }
}
