/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package garmentfactory.admin;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import DatabaseConnection.config;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.sql.Time;

public class AttendanceView extends javax.swing.JFrame {

    private DefaultTableModel model;
    private int loggedInUserId;

    public AttendanceView(int userId) {
        initComponents();
        this.loggedInUserId = userId;
        model = (DefaultTableModel) jTableAttendanceView.getModel();
        loadUserName();
        loadData(null, null);
        initListeners();
        populateSortOptions();
    }

    // Load and display the logged-in user's username
    private void loadUserName() {
        String query = "SELECT username FROM users WHERE user_id = ?";
        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, loggedInUserId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    jLabelUserName.setText(rs.getString("username"));
                }
            }
        } catch (SQLException e) {
            showError("Error loading username", e);
        }
    }

    private void loadData(String orderByColumn, String searchTerm) {
        model.setRowCount(0);

        String query = "SELECT a.attendance_id, e.employee_name, a.date, a.check_in, a.check_out, "
                + "a.late_time, a.overtime, a.work_time "
                + "FROM attendance a "
                + "JOIN employees e ON a.employee_id = e.employee_id "
                + "JOIN users u ON u.user_id "
                + "WHERE u.user_id = ? AND a.date = CURRENT_DATE()";

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query += " AND LOWER(e.employee_name) LIKE ?";
        }

        if (orderByColumn != null && !orderByColumn.trim().isEmpty()) {
            query += " ORDER BY " + getOrderByColumn(orderByColumn);
        }

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setInt(1, loggedInUserId);
            if (searchTerm != null && !searchTerm.trim().isEmpty()) {
                ps.setString(2, "%" + searchTerm.trim().toLowerCase() + "%");
            }
            populateTable(ps);
        } catch (SQLException e) {
            showError("Error loading data", e);
        }
    }

    private void populateTable(PreparedStatement ps) throws SQLException {
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                model.addRow(new Object[]{
                    rs.getObject("attendance_id"),
                    rs.getObject("employee_name"),
                    rs.getObject("date"),
                    rs.getObject("check_in"),
                    rs.getObject("check_out"),
                    rs.getObject("late_time"),
                    rs.getObject("overtime"),
                    rs.getObject("work_time")
                });
            }
        }
    }

    private String getOrderByColumn(String selectedItem) {
        switch (selectedItem) {
            case "employee_name":
                return "e.employee_name";
            case "date":
                return "a.date";
            default:
                return "";
        }
    }

    private boolean validateInputFields() {
        String employeeName = jTextFieldEmployeeName.getText().trim();
        String leaveTime = jTextFieldLeaveTime.getText().trim();

        if (employeeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Employee name is required.");
            return false;
        }

        if (leaveTime.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Leave time is required.");
            return false;
        }

        if (!isValidTimeFormat(leaveTime)) {
            JOptionPane.showMessageDialog(this, "Invalid leave time format. Please use the format HH:mm.");
            return false;
        }

        return true;
    }

    private boolean isValidTimeFormat(String time) {
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
        timeFormat.setLenient(false);
        try {
            timeFormat.parse(time);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }

    // Initialize listeners for search, sorting, and table selection
    private void initListeners() {
        jButtonTimeNow.addActionListener(e -> setTimeNow());

        jTextFieldSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loadData((String) jComboBoxSortBy.getSelectedItem(), jTextFieldSearch.getText());
                }
            }
        });

        jComboBoxSortBy.addActionListener(e -> loadData((String) jComboBoxSortBy.getSelectedItem(), jTextFieldSearch.getText()));

        jTableAttendanceView.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = jTableAttendanceView.getSelectedRow();
                if (selectedRow != -1) {
                    updateFormFieldsWithSelectedRow(selectedRow);
                }
            }
        });
    }

    private void updateFormFieldsWithSelectedRow(int selectedRow) {
        try {
            // Assuming column 1 is employee_name and column 6 is reason
            jTextFieldEmployeeName.setText((String) jTableAttendanceView.getValueAt(selectedRow, 1));
            jTextAreaReason.setText((String) jTableAttendanceView.getValueAt(selectedRow, 6));

            // Extract and format leave_time (Assuming column 7 is leave_time)
            Object leaveTimeObj = jTableAttendanceView.getValueAt(selectedRow, 7);
            if (leaveTimeObj instanceof Time) {
                // Leave time is a TIME object, format it to HH:mm
                Time leaveTime = (Time) leaveTimeObj;
                SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm");
                jTextFieldLeaveTime.setText(timeFormat.format(leaveTime));
            } else {
                jTextFieldLeaveTime.setText(""); // Clear if no leave time
            }
        } catch (Exception e) {
            showError("Error updating form fields", e);
        }
    }

    // Handle all CRUD actions (save, edit, reset, view)
    private void allActionPerformed(java.awt.event.ActionEvent evt, String actionType) {
        int selectedRow = jTableAttendanceView.getSelectedRow();

        switch (actionType) {
            case "SAVE":
                handleSaveAction();
                break;
            case "EDIT":
                handleEditAction(selectedRow);
                break;
            case "RESET":
                handleResetAction(selectedRow);
                break;
            case "VIEW":
                handleViewAction(selectedRow);
                break;
            default:
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }

    private void handleSaveAction() {
        if (validateInputFields()) {
            updateAttendanceRecord(true);
        }
    }

    private void handleEditAction(int selectedRow) {
        if (selectedRow != -1 && validateInputFields()) {
            updateAttendanceRecord(false);
        } else {
            JOptionPane.showMessageDialog(this, "Please select an attendance record to edit.");
        }
    }

    private void updateAttendanceRecord(boolean isSaveAction) {
        int selectedRow = jTableAttendanceView.getSelectedRow();
        String reason = jTextAreaReason.getText().trim();
        String leaveTimeStr = jTextFieldLeaveTime.getText().trim();
        java.sql.Time leaveTime = parseLeaveTime(leaveTimeStr);

        if (leaveTime == null) {
            return; // Error has already been shown in parseLeaveTime method
        }

        int attendanceId = (int) jTableAttendanceView.getValueAt(selectedRow, 0);

        try (Connection con = config.getConnection()) {
            String query;
            if (isSaveAction) {
                query = "UPDATE attendance SET reason = ?, leave_time = ?, check_out = ? WHERE attendance_id = ?";
            } else {
                query = "UPDATE attendance SET reason = ?, leave_time = ? WHERE attendance_id = ?";
            }
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, reason);
            ps.setTime(2, leaveTime);
            if (isSaveAction) {
                ps.setTime(3, java.sql.Time.valueOf("16:00:00"));
                ps.setInt(4, attendanceId);
            } else {
                ps.setInt(3, attendanceId);
            }

            ps.executeUpdate();
            loadData(null, null);
            JOptionPane.showMessageDialog(this, "Attendance updated successfully.");
            clearAllFields();
        } catch (SQLException e) {
            showError("Error saving attendance", e);
        }
    }

    private java.sql.Time parseLeaveTime(String leaveTimeStr) {
        try {
            return java.sql.Time.valueOf(leaveTimeStr + ":00"); // Assuming input is in hh:mm format
        } catch (IllegalArgumentException e) {
            JOptionPane.showMessageDialog(this, "Leave time must be in the format HH:mm.");
            return null;
        }
    }

    // Reset the reason and leave_time to default values
    private void handleResetAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an attendance record to reset.");
            return;
        }

        int attendanceId = (int) jTableAttendanceView.getValueAt(selectedRow, 0);

        try (Connection con = config.getConnection()) {
            String query = "UPDATE attendance SET check_out = NULL, reason = NULL, leave_time = '00:00:00' WHERE attendance_id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, attendanceId);

            ps.executeUpdate();
            loadData(null, null);
            JOptionPane.showMessageDialog(this, "Attendance reset successfully.");
            clearAllFields();
        } catch (SQLException e) {
            showError("Error resetting attendance", e);
        }
    }

    // View attendance details for the selected record
    private void handleViewAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an attendance record to view.");
            return;
        }

        StringBuilder attendanceDetails = new StringBuilder();
        attendanceDetails.append("Attendance ID: ").append(jTableAttendanceView.getValueAt(selectedRow, 0)).append("\n")
                .append("Employee Name: ").append(jTableAttendanceView.getValueAt(selectedRow, 1)).append("\n")
                .append("Date: ").append(jTableAttendanceView.getValueAt(selectedRow, 2)).append("\n")
                .append("Check In: ").append(jTableAttendanceView.getValueAt(selectedRow, 3)).append("\n")
                .append("Check Out: ").append(jTableAttendanceView.getValueAt(selectedRow, 4)).append("\n")
                .append("Attendance Status: ").append(jTableAttendanceView.getValueAt(selectedRow, 5)).append("\n")
                .append("Reason: ").append(jTableAttendanceView.getValueAt(selectedRow, 6)).append("\n")
                .append("Leave Time: ").append(jTableAttendanceView.getValueAt(selectedRow, 7)).append("\n");

        JOptionPane.showMessageDialog(this, attendanceDetails.toString(), "Attendance Details", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setTimeNow() {
        // Get the current time
        java.util.Calendar calendar = java.util.Calendar.getInstance();

        // Extract the hour component
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);

        // Create a formatted string with hour rounded down, minutes and seconds set to zero
        String roundedTime = String.format("%02d:00", hour);

        // Set the formatted time to jTextFieldLeaveTime
        jTextFieldLeaveTime.setText(roundedTime);
    }

    // Populate sorting options
    private void populateSortOptions() {
        jComboBoxSortBy.removeAllItems();
        jComboBoxSortBy.addItem("employee_name");
        jComboBoxSortBy.addItem("date");
    }

    // Clear all form fields
    private void clearAllFields() {
        jTextFieldEmployeeName.setText("");
        jTextFieldSearch.setText("");
        jComboBoxSortBy.setSelectedIndex(0);
        jTextAreaReason.setText("");
        jTextFieldLeaveTime.setText("");
    }

    // Show error message
    private void showError(String message, Exception e) {
        e.printStackTrace();
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage());
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableAttendanceView = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jButtonSave = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonReset = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButtonClose = new javax.swing.JButton();
        jTextFieldEmployeeName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldLeaveTime = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTextAreaReason = new javax.swing.JTextArea();
        jButtonTimeNow = new javax.swing.JButton();
        jButtonView = new javax.swing.JButton();
        jLabelUserName = new javax.swing.JLabel();
        jComboBoxSortBy = new javax.swing.JComboBox<>();
        jTextFieldSearch = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1880, 1000));

        jTableAttendanceView.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jTableAttendanceView.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "employee_name", "date", "check_in", "check_out", "late_time", "overtime", "work_time"
            }
        ));
        jTableAttendanceView.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTableAttendanceView.setRowHeight(40);
        jTableAttendanceView.setShowGrid(false);
        jScrollPane1.setViewportView(jTableAttendanceView);

        jPanel1.setBackground(new java.awt.Color(19, 15, 64));
        jPanel1.setPreferredSize(new java.awt.Dimension(650, 486));

        jButtonSave.setBackground(new java.awt.Color(34, 166, 179));
        jButtonSave.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonSave.setForeground(new java.awt.Color(255, 255, 255));
        jButtonSave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/save.png"))); // NOI18N
        jButtonSave.setText("Save");
        jButtonSave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonSaveActionPerformed(evt);
            }
        });

        jButtonEdit.setBackground(new java.awt.Color(104, 109, 224));
        jButtonEdit.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonEdit.setForeground(new java.awt.Color(255, 255, 255));
        jButtonEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/edit.png"))); // NOI18N
        jButtonEdit.setText("Edit");
        jButtonEdit.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEditActionPerformed(evt);
            }
        });

        jButtonReset.setBackground(new java.awt.Color(255, 121, 121));
        jButtonReset.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonReset.setForeground(new java.awt.Color(255, 255, 255));
        jButtonReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/reset.png"))); // NOI18N
        jButtonReset.setText("Reset");
        jButtonReset.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonResetActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(246, 229, 141));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Manage AttendanceView");

        jButtonClose.setBackground(new java.awt.Color(235, 77, 75));
        jButtonClose.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jButtonClose.setForeground(new java.awt.Color(255, 255, 255));
        jButtonClose.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/back.png"))); // NOI18N
        jButtonClose.setText("Back");
        jButtonClose.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonCloseActionPerformed(evt);
            }
        });

        jTextFieldEmployeeName.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldEmployeeName.setDisabledTextColor(new java.awt.Color(0, 0, 153));
        jTextFieldEmployeeName.setEnabled(false);

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(223, 249, 251));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("Name");

        jButtonClear.setBackground(new java.awt.Color(235, 77, 75));
        jButtonClear.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonClear.setForeground(new java.awt.Color(255, 255, 255));
        jButtonClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/clear.png"))); // NOI18N
        jButtonClear.setText("Clear");
        jButtonClear.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonClearActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel5.setForeground(new java.awt.Color(223, 249, 251));
        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel5.setText("Leave Time ");

        jTextFieldLeaveTime.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldLeaveTime.setDisabledTextColor(new java.awt.Color(0, 0, 153));
        jTextFieldLeaveTime.setEnabled(false);

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(223, 249, 251));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel6.setText("Reason ");

        jTextAreaReason.setColumns(20);
        jTextAreaReason.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jTextAreaReason.setRows(5);
        jScrollPane2.setViewportView(jTextAreaReason);

        jButtonTimeNow.setBackground(new java.awt.Color(153, 255, 153));
        jButtonTimeNow.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButtonTimeNow.setText("Now");

        jButtonView.setBackground(new java.awt.Color(249, 202, 36));
        jButtonView.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonView.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/view.png"))); // NOI18N
        jButtonView.setText("View");
        jButtonView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonViewActionPerformed(evt);
            }
        });

        jLabelUserName.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabelUserName.setForeground(new java.awt.Color(246, 229, 141));
        jLabelUserName.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabelUserName.setText("Lording ...");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jTextFieldEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jTextFieldLeaveTime, javax.swing.GroupLayout.PREFERRED_SIZE, 180, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                    .addComponent(jButtonTimeNow, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 27, Short.MAX_VALUE)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonSave, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonEdit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonReset, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonClear, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonView, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 318, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabelUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 314, javax.swing.GroupLayout.PREFERRED_SIZE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelUserName, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldLeaveTime, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5)
                            .addComponent(jButtonTimeNow, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(jScrollPane2)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jButtonSave, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonView, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonReset, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(336, Short.MAX_VALUE))
        );

        jComboBoxSortBy.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jComboBoxSortBy.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxSortBy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSortByActionPerformed(evt);
            }
        });

        jTextFieldSearch.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldSearch.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSearchActionPerformed(evt);
            }
        });
        jTextFieldSearch.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldSearchKeyPressed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setText("Search");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel3.setText("Sort By");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(32, 32, 32)
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBoxSortBy, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(386, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jScrollPane1))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)
                        .addComponent(jLabel3))
                    .addComponent(jComboBoxSortBy, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1)
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 893, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldSearchActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSearchActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldSearchActionPerformed

    private void jComboBoxSortByActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSortByActionPerformed

    }//GEN-LAST:event_jComboBoxSortByActionPerformed

    private void jTextFieldSearchKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldSearchKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            String searchTerm = jTextFieldSearch.getText();
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                searchTerm = null;
            }
            loadData(null, searchTerm);
        }
    }//GEN-LAST:event_jTextFieldSearchKeyPressed

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jButtonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEditActionPerformed
        allActionPerformed(evt, "EDIT");
    }//GEN-LAST:event_jButtonEditActionPerformed

    private void jButtonSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonSaveActionPerformed
        allActionPerformed(evt, "SAVE");
    }//GEN-LAST:event_jButtonSaveActionPerformed

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAllFields();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private void jButtonResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonResetActionPerformed
        allActionPerformed(evt, "RESET");
    }//GEN-LAST:event_jButtonResetActionPerformed

    private void jButtonViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewActionPerformed
        allActionPerformed(evt, "VIEW");
    }//GEN-LAST:event_jButtonViewActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                int loggedInUserId = 2;
                new AttendanceView(loggedInUserId).setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonReset;
    private javax.swing.JButton jButtonSave;
    private javax.swing.JButton jButtonTimeNow;
    private javax.swing.JButton jButtonView;
    private javax.swing.JComboBox<String> jComboBoxSortBy;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabelUserName;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JTable jTableAttendanceView;
    private javax.swing.JTextArea jTextAreaReason;
    private javax.swing.JTextField jTextFieldEmployeeName;
    private javax.swing.JTextField jTextFieldLeaveTime;
    private javax.swing.JTextField jTextFieldSearch;
    // End of variables declaration//GEN-END:variables
}
