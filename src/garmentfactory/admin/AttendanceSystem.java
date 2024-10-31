package garmentfactory.admin;

// AttendanceSystem.java
import javax.swing.*;
import java.awt.*;
import java.sql.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class AttendanceSystem extends JFrame {

    private static final LocalTime EARLY_CHECK_IN_START = LocalTime.of(7, 0);
    private static final LocalTime WORK_START_TIME = LocalTime.of(8, 0);
    private static final LocalTime LATE_START_TIME = LocalTime.of(8, 20);
    private static final LocalTime OVERTIME_START_TIME = LocalTime.of(19, 0);
    private static final LocalTime CHECK_OUT_ALLOWED_END = LocalTime.of(22, 0);

    private static final String SQL_SELECT_EMPLOYEES = "SELECT employee_name FROM employees";
    private static final String SQL_INSERT_ATTENDANCE = "INSERT INTO attendance (employee_id, date, check_in, check_out, late_time, overtime, work_time) VALUES (?, ?, ?, ?, ?, ?, ?)";
    private static final String SQL_SELECT_TODAY_ATTENDANCE = "SELECT e.employee_name, a.check_in, a.check_out, a.late_time, a.overtime, a.work_time FROM attendance a JOIN employees e ON a.employee_id = e.employee_id WHERE a.date = ?";
    private static final String SQL_UPDATE_ATTENDANCE = "UPDATE attendance SET check_out = ?, overtime = ?, work_time = ? WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?) AND date = ?";
    private static final String SQL_SELECT_EMPLOYEE_ID = "SELECT employee_id FROM employees WHERE employee_name = ? LIMIT 1";
    private static final String SQL_SELECT_CHECK_IN = "SELECT check_in FROM attendance WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?) AND date = ?";
    private static final String SQL_SELECT_CHECK_OUT = "SELECT check_out FROM attendance WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?) AND date = ?";
    private static final String SQL_COUNT_ATTENDANCE = "SELECT COUNT(*) FROM attendance WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?) AND date = ?";

    private JComboBox<String> employeeComboBox;
    private JComboBox<String> timeComboBox;
    private JButton checkInButton, checkOutButton;
    private DefaultTableModel tableModel;

    public AttendanceSystem() {
        initializeUI();
        loadEmployees();
        loadTodayAttendance();
        setActionListeners();
    }

    private void initializeUI() {
        setTitle("Attendance System");
        setSize(1920, 1080);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        Font defaultFont = new Font("SansSerif", Font.PLAIN, 25);
        UIManager.put("Label.font", defaultFont);
        UIManager.put("Button.font", defaultFont);
        UIManager.put("ComboBox.font", defaultFont);
        UIManager.put("Table.font", defaultFont);
        UIManager.put("TableHeader.font", new Font("SansSerif", Font.BOLD, 30));

        // Menu bar setup
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        JMenuItem exportItem = new JMenuItem("Export to CSV");
        exportItem.addActionListener(e -> exportToCSV());
        fileMenu.add(exportItem);
        menuBar.add(fileMenu);
        setJMenuBar(menuBar);

        JPanel inputPanel = new JPanel(new GridBagLayout());
        inputPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.anchor = GridBagConstraints.WEST;

        employeeComboBox = new JComboBox<>();
        employeeComboBox.setPreferredSize(new Dimension(200, 40));
        timeComboBox = new JComboBox<>(new String[]{"06:00", "07:00", "08:00",
            "09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00",
            "17:00", "18:00", "19:00", "20:00", "21:00", "22:00", "23:00"});
        timeComboBox.setPreferredSize(new Dimension(200, 40));

        checkInButton = new JButton("Check In");
        checkInButton.setPreferredSize(new Dimension(200, 80));
        checkInButton.setBackground(new Color(60, 179, 113));
        checkInButton.setForeground(Color.WHITE);
        checkInButton.setToolTipText("Check in the selected employee.");

        checkOutButton = new JButton("Check Out");
        checkOutButton.setPreferredSize(new Dimension(200, 80));
        checkOutButton.setBackground(new Color(220, 20, 60));
        checkOutButton.setForeground(Color.WHITE);
        checkOutButton.setToolTipText("Check out the selected employee.");

        gbc.gridx = 0;
        gbc.gridy = 0;
        inputPanel.add(new JLabel("Employee:"), gbc);

        gbc.gridx = 1;
        inputPanel.add(employeeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        inputPanel.add(new JLabel("Time:"), gbc);

        gbc.gridx = 1;
        inputPanel.add(timeComboBox, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        inputPanel.add(checkInButton, gbc);

        gbc.gridx = 1;
        inputPanel.add(checkOutButton, gbc);

        String[] columnNames = {"Employee", "Date", "Check In", "Check Out", "Late Time (Hours)", "Overtime (Hours)", "Work Time (Hours)"};
        tableModel = new DefaultTableModel(columnNames, 0);
        JTable attendanceTable = new JTable(tableModel);
        attendanceTable.setRowHeight(40);
        attendanceTable.setFont(new Font("SansSerif", Font.PLAIN, 16));

        JTableHeader header = attendanceTable.getTableHeader();
        header.setFont(new Font("SansSerif", Font.BOLD, 20));
        header.setBackground(new Color(70, 130, 180));
        header.setForeground(Color.WHITE);

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        attendanceTable.setDefaultRenderer(Object.class, centerRenderer);
        attendanceTable.setShowGrid(true);
        attendanceTable.setGridColor(new Color(220, 220, 220));

        attendanceTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                final Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                c.setBackground(row % 2 == 0 ? new Color(245, 245, 245) : Color.WHITE);
                return c;
            }
        });

        JScrollPane tableScrollPane = new JScrollPane(attendanceTable);

        add(inputPanel, BorderLayout.NORTH);
        add(tableScrollPane, BorderLayout.CENTER);
    }

    private void loadEmployees() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(SQL_SELECT_EMPLOYEES)) {
            while (rs.next()) {
                employeeComboBox.addItem(rs.getString("employee_name"));
            }
        } catch (SQLException e) {
            showError("Error loading employees", e);
        }
    }

    private void loadTodayAttendance() {
        LocalDate today = LocalDate.now();
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_TODAY_ATTENDANCE)) {
            pstmt.setDate(1, Date.valueOf(today));
            try (ResultSet rs = pstmt.executeQuery()) {
                tableModel.setRowCount(0);
                while (rs.next()) {
                    String employeeName = rs.getString("employee_name");
                    Time checkInTime = rs.getTime("check_in");
                    Time checkOutTime = rs.getTime("check_out");
                    int lateTime = rs.getInt("late_time");
                    int overtime = rs.getInt("overtime");
                    int workTime = rs.getInt("work_time");

                    tableModel.addRow(new Object[]{
                        employeeName,
                        today,
                        checkInTime,
                        checkOutTime,
                        lateTime,
                        overtime,
                        workTime
                    });
                }
            }
        } catch (SQLException e) {
            showError("Error loading today's attendance", e);
        }
    }

    private void setActionListeners() {
        checkInButton.addActionListener(e -> handleCheckIn());
        checkOutButton.addActionListener(e -> handleCheckOut());
    }

    private void handleCheckIn() {
        String employeeName = (String) employeeComboBox.getSelectedItem();
        String selectedTime = (String) timeComboBox.getSelectedItem();

        if (employeeName == null || selectedTime == null || selectedTime.isEmpty()) {
            showMessage("Please select an employee and a time.");
            return;
        }

        try (Connection conn = getConnection()) {
            if (isAlreadyCheckedIn(conn, employeeName)) {
                showMessage(employeeName + " has already checked in today.");
                return;
            }

            LocalTime checkInTime = LocalTime.parse(selectedTime);

            if (!isValidCheckInTime(checkInTime)) {
                showMessage("Check-in time must be between 07:00 and 14:00.");
                return;
            }

            int lateHours = calculateLateHours(checkInTime);
            insertAttendanceRecord(conn, employeeName, checkInTime, null, lateHours, 0, 0); // Initial workTime is 0
            updateTable(employeeName, LocalDate.now(), checkInTime, null, lateHours, 0);
            showMessage("Check-in recorded successfully for " + employeeName);
        } catch (SQLException e) {
            showError("Error processing check-in", e);
        }
    }

    private boolean isValidCheckInTime(LocalTime checkInTime) {
        return checkInTime.isAfter(EARLY_CHECK_IN_START.minusSeconds(1)) && checkInTime.isBefore(LocalTime.of(14, 0));
    }

    private int calculateLateHours(LocalTime checkInTime) {
        if (checkInTime.isAfter(LATE_START_TIME)) {
            if (checkInTime.equals(LATE_START_TIME)) {
                return 1;
            } else {
                long minutesLate = ChronoUnit.MINUTES.between(WORK_START_TIME, checkInTime);
                return (int) Math.ceil(minutesLate / 60.0);
            }
        }
        return 0;
    }

    private void handleCheckOut() {
        String employeeName = (String) employeeComboBox.getSelectedItem();
        String selectedTime = (String) timeComboBox.getSelectedItem();

        if (employeeName == null || selectedTime == null || selectedTime.isEmpty()) {
            showMessage("Please select an employee and a time.");
            return;
        }

        try (Connection conn = getConnection()) {
            if (isAlreadyCheckedOut(conn, employeeName)) {
                showMessage(employeeName + " has already checked out today.");
                return;
            }

            LocalTime checkOutTime = LocalTime.parse(selectedTime);

            if (!isValidCheckOutTime(checkOutTime)) {
                showMessage("Check-out time must be between " + LATE_START_TIME + " and " + CHECK_OUT_ALLOWED_END + ".");
                return;
            }

            int overtimeHours = calculateOvertimeHours(checkOutTime);

            // Fetch check-in time to calculate work time
            LocalTime checkInTime = getCheckInTime(conn, employeeName);
            if (checkInTime == null) {
                showMessage("Check-in record not found for " + employeeName);
                return;
            }
            int workTime = calculateWorkTimeWithBreak(checkInTime, checkOutTime);

            updateAttendanceRecord(conn, employeeName, checkOutTime, overtimeHours, workTime);
            updateTableForCheckOut(employeeName, checkOutTime, overtimeHours, workTime);
            showMessage("Check-out recorded successfully for " + employeeName);
        } catch (SQLException e) {
            showError("Error processing check-out", e);
        }
    }

    private boolean isValidCheckOutTime(LocalTime checkOutTime) {
        return (checkOutTime.isAfter(LATE_START_TIME.minusSeconds(1)) && checkOutTime.isBefore(CHECK_OUT_ALLOWED_END.plusSeconds(1)));
    }

    private int calculateWorkTimeWithBreak(LocalTime checkInTime, LocalTime checkOutTime) {
        LocalTime workStart = LocalTime.of(8, 0);
        LocalTime morningEnd = LocalTime.of(11, 0);
        LocalTime breakStart = LocalTime.of(11, 0);
        LocalTime breakEnd = LocalTime.of(12, 0);
        LocalTime workEnd = LocalTime.of(17, 0);  // End of work day

        // If the checkout time is 17:00 or later, work time is 0 hours
        if (!checkOutTime.isBefore(workEnd)) {
            return 0;
        }

        // Adjust the check-in and check-out times based on work periods
        LocalTime effectiveCheckIn = checkInTime.isBefore(workStart) ? workStart : checkInTime;
        LocalTime effectiveCheckOut = checkOutTime.isAfter(workEnd) ? workEnd : checkOutTime;

        // Calculate morning work time (from 08:00 to 11:00)
        long morningMinutes = 0;
        if (effectiveCheckIn.isBefore(morningEnd)) {
            morningMinutes = ChronoUnit.MINUTES.between(effectiveCheckIn, effectiveCheckOut.isBefore(morningEnd) ? effectiveCheckOut : morningEnd);
        }

        // Calculate afternoon work time (from 12:00 to 17:00)
        long afternoonMinutes = 0;
        if (effectiveCheckOut.isAfter(breakEnd)) {
            LocalTime afternoonStart = effectiveCheckIn.isAfter(breakEnd) ? effectiveCheckIn : breakEnd;
            afternoonMinutes = ChronoUnit.MINUTES.between(afternoonStart, effectiveCheckOut);
        }

        // Total work minutes excluding the break
        long totalMinutes = morningMinutes + afternoonMinutes;

        // Return work time in hours, rounding down
        return (int) (totalMinutes / 60);
    }

    private LocalTime getCheckInTime(Connection conn, String employeeName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_CHECK_IN)) {
            pstmt.setString(1, employeeName);
            pstmt.setDate(2, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getTime("check_in").toLocalTime();
                }
            }
        }
        return null;
    }

    private int calculateOvertimeHours(LocalTime checkOutTime) {
        LocalTime breakStart = LocalTime.of(17, 0);
        LocalTime breakEnd = LocalTime.of(18, 0);
        LocalTime overtimeStart = breakEnd; // Overtime starts after the break
        LocalTime overtimeEnd = LocalTime.of(22, 0); // End of the overtime period

        // If the checkout time is before the overtime starts, no overtime
        if (checkOutTime.isBefore(overtimeStart)) {
            return 0;
        }

        // Calculate overtime minutes starting from 18:00
        LocalTime effectiveOvertimeEnd = checkOutTime.isAfter(overtimeEnd) ? overtimeEnd : checkOutTime;
        long overtimeMinutes = ChronoUnit.MINUTES.between(overtimeStart, effectiveOvertimeEnd);

        // Return overtime in hours, rounding up
        return (int) Math.ceil(overtimeMinutes / 60.0);
    }

    private void insertAttendanceRecord(Connection conn, String employeeName, LocalTime checkInTime, LocalTime checkOutTime, int lateHours, int overtimeHours, int workTime) throws SQLException {
        String employeeId = getEmployeeId(conn, employeeName);
        if (employeeId == null) {
            throw new SQLException("Employee not found.");
        }

        try (PreparedStatement pstmt = conn.prepareStatement(SQL_INSERT_ATTENDANCE)) {
            pstmt.setString(1, employeeId);
            pstmt.setDate(2, Date.valueOf(LocalDate.now()));
            pstmt.setTime(3, Time.valueOf(checkInTime));
            pstmt.setTime(4, checkOutTime != null ? Time.valueOf(checkOutTime) : null);
            pstmt.setInt(5, lateHours);
            pstmt.setInt(6, overtimeHours);
            pstmt.setInt(7, workTime); // Insert work time
            pstmt.executeUpdate();
        }
    }

    private String getEmployeeId(Connection conn, String employeeName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_EMPLOYEE_ID)) {
            pstmt.setString(1, employeeName);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("employee_id");
                }
            }
        }
        return null;
    }

    private void updateAttendanceRecord(Connection conn, String employeeName, LocalTime checkOutTime, int overtimeHours, int workTime) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_UPDATE_ATTENDANCE)) {
            pstmt.setTime(1, Time.valueOf(checkOutTime));
            pstmt.setInt(2, overtimeHours);
            pstmt.setInt(3, workTime); // Update work time
            pstmt.setString(4, employeeName);
            pstmt.setDate(5, Date.valueOf(LocalDate.now()));
            pstmt.executeUpdate();
        }
    }

    private boolean isAlreadyCheckedIn(Connection conn, String employeeName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_COUNT_ATTENDANCE)) {
            pstmt.setString(1, employeeName);
            pstmt.setDate(2, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        }
        return false;
    }

    private boolean isAlreadyCheckedOut(Connection conn, String employeeName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement(SQL_SELECT_CHECK_OUT)) {
            pstmt.setString(1, employeeName);
            pstmt.setDate(2, Date.valueOf(LocalDate.now()));
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Time clockOutTime = rs.getTime("check_out");
                    return clockOutTime != null;
                }
            }
        }
        return false;
    }

    private void updateTable(String employeeName, LocalDate date, LocalTime checkInTime, LocalTime checkOutTime, int lateMinutes, int overtimeMinutes) {
        tableModel.addRow(new Object[]{employeeName, date, checkInTime, checkOutTime, lateMinutes, overtimeMinutes});
    }

    private void updateTableForCheckOut(String employeeName, LocalTime checkOutTime, int overtimeHours, int workTime) {
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            if (Objects.equals(tableModel.getValueAt(i, 0), employeeName) && tableModel.getValueAt(i, 1).equals(LocalDate.now())) {
                tableModel.setValueAt(checkOutTime, i, 3);
                tableModel.setValueAt(overtimeHours, i, 5);
                tableModel.setValueAt(workTime, i, 6); // Update work time in the table
                break;
            }
        }
    }

    private Connection getConnection() throws SQLException {
        String url = "jdbc:mysql://localhost:3306/garmentfactory";
        String username = "root";
        String password = "";
        return DriverManager.getConnection(url, username, password);
    }

    private void showMessage(String message) {
        JOptionPane.showMessageDialog(this, message);
    }

    private void showError(String message, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage());
    }

    private void exportToCSV() {
        // Implementation for exporting table data to CSV file
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceSystem().setVisible(true));
    }
}
