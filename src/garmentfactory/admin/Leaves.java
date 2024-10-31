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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class Leaves extends javax.swing.JFrame {

    private DefaultTableModel modelEmployees;
    private DefaultTableModel modelLeaves;

    public Leaves() {
        initComponents();
        modelEmployees = (DefaultTableModel) jTableEmployees.getModel();
        modelLeaves = (DefaultTableModel) jTableLeaves.getModel();
        loadEmployeeData(null);  // Load employees into jTableEmployees
        loadLeaveData(null, null);  // Load leave data into jTableLeaves
        initTableSelectionListeners(); // Initialize all table selection listeners

        // Set up search functionality for employees
        jTextFieldSearchEmployees.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String searchTerm = jTextFieldSearchEmployees.getText();
                    loadEmployeeData(searchTerm);
                }
            }
        });
        jButtonToday.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                setTodayDate();
            }
        });

        // Set up sort functionality for leave records
        jComboBoxSortByLeaves.addActionListener(e -> {
            String searchTerm = jTextFieldSearchEmployees.getText();
            String orderByColumn = (String) jComboBoxSortByLeaves.getSelectedItem();
            loadLeaveData(orderByColumn, searchTerm);
        });

        // Populate the jComboBoxSortByLeaves with sorting options
        jComboBoxSortByLeaves.removeAllItems();
        jComboBoxSortByLeaves.addItem("employee_name");
        jComboBoxSortByLeaves.addItem("reason");
        jComboBoxSortByLeaves.addItem("start_date");
        jComboBoxSortByLeaves.addItem("end_date");
    }

    private void setTodayDate() {
        LocalDate today = LocalDate.now();  // Get the current date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        String formattedDate = today.format(formatter);
        jTextFieldStartDate.setText(formattedDate);
        jTextFieldEndDate.setText(formattedDate);
    }

    // Method to load Employee Data from the database into jTableEmployees
    private void loadEmployeeData(String searchTerm) {
        modelEmployees.setRowCount(0);

        String query = "SELECT employee_id, employee_name FROM employees";
        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();

        if (hasSearch) {
            query += " WHERE employee_name LIKE ?";
        }

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            if (hasSearch) {
                String searchPattern = "%" + searchTerm.trim() + "%";
                ps.setString(1, searchPattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelEmployees.addRow(new Object[]{
                        rs.getObject("employee_id"),
                        rs.getObject("employee_name")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading employees: " + e.getMessage());
        }
    }

    // Load Leave Data from the database into jTableLeaves
    private void loadLeaveData(String orderByColumn, String searchTerm) {
        modelLeaves.setRowCount(0);

        String query = "SELECT leaves.leave_id, employees.employee_name, leaves.reason, leaves.start_date, leaves.end_date "
                + "FROM leaves "
                + "INNER JOIN employees ON leaves.employee_id = employees.employee_id ";

        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        boolean hasSort = orderByColumn != null && !orderByColumn.trim().isEmpty();

        if (hasSearch) {
            query += " WHERE (employees.employee_name LIKE ? OR leaves.reason LIKE ?)";
        }

        if (hasSort) {
            query += " ORDER BY " + orderByColumn;
        }

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            if (hasSearch) {
                String searchPattern = "%" + searchTerm.trim() + "%";
                ps.setString(1, searchPattern);
                ps.setString(2, searchPattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    modelLeaves.addRow(new Object[]{
                        rs.getObject("leave_id"),
                        rs.getObject("employee_name"),
                        rs.getObject("reason"),
                        rs.getObject("start_date"),
                        rs.getObject("end_date")
                    });
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading leaves: " + e.getMessage());
        }
    }

    // Initialize table selection listeners for jTableEmployees and jTableLeaves
    private void initTableSelectionListeners() {
        // Listener for jTableEmployees
        jTableEmployees.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = jTableEmployees.getSelectedRow();
                if (selectedRow != -1) {
                    String selectedEmployeeName = (String) jTableEmployees.getValueAt(selectedRow, 1);  // Index 1: employee_name
                    jTextFieldEmployeeName.setText(selectedEmployeeName);
                }
            }
        });

        // Listener for jTableLeaves
        jTableLeaves.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = jTableLeaves.getSelectedRow();
                if (selectedRow != -1) {
                    updateLeaveFieldsWithSelectedRow(selectedRow);
                }
            }
        });
    }

    private void updateLeaveFieldsWithSelectedRow(int selectedRow) {
        try {
            String employeeName = (String) jTableLeaves.getValueAt(selectedRow, 1); // Index 1: employee_name
            String reason = (String) jTableLeaves.getValueAt(selectedRow, 2); // Index 2: reason
            String startDate = jTableLeaves.getValueAt(selectedRow, 3).toString(); // Index 3: start_date
            String endDate = jTableLeaves.getValueAt(selectedRow, 4).toString(); // Index 4: end_date

            jTextFieldEmployeeName.setText(employeeName);
            jTextAreaLeaveReason.setText(reason);
            jTextFieldStartDate.setText(startDate);
            jTextFieldEndDate.setText(endDate);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Error updating leave fields: " + e.getMessage());
        }
    }

    private void allActionPerformed(String actionType) {
        int selectedRow = jTableLeaves.getSelectedRow(); // Get the selected row for Edit/Delete/View actions

        switch (actionType) {
            case "ADD":
                handleAddLeaveAction();
                break;
            case "EDIT":
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a Leave to edit.");
                } else {
                    handleEditLeaveAction(selectedRow);  // Pass the selected row for editing
                }
                break;
            case "DELETE":
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a Leave to delete.");
                } else {
                    handleDeleteLeaveAction(selectedRow);  // Pass the selected row for deletion
                }
                break;
            case "VIEW":
                if (selectedRow == -1) {
                    JOptionPane.showMessageDialog(this, "Please select a Leave to view.");
                } else {
                    handleViewLeaveAction(selectedRow);  // Pass the selected row for viewing
                }
                break;
            default:
                JOptionPane.showMessageDialog(this, "Unknown action type: " + actionType);
        }
    }

    private void handleAddLeaveAction() {
        if (!validateInputFields()) {
            return;
        }

        String employeeName = jTextFieldEmployeeName.getText().trim();
        String leaveReason = jTextAreaLeaveReason.getText().trim();
        String startDate = jTextFieldStartDate.getText().trim();
        String endDate = jTextFieldEndDate.getText().trim();

        try (Connection con = config.getConnection()) {
            // First, check if a record with the same employee_id, start_date, and end_date exists
            String checkQuery = "SELECT COUNT(*) FROM leaves "
                    + "WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?) "
                    + "AND start_date = ? AND end_date = ?";
            PreparedStatement checkPs = con.prepareStatement(checkQuery);
            checkPs.setString(1, employeeName);
            checkPs.setString(2, startDate);
            checkPs.setString(3, endDate);

            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Record exists, show error message
                    JOptionPane.showMessageDialog(this, "A leave record for this employee with the same start and end date already exists.");
                    return;
                }
            }

            // If no duplicate found, proceed with inserting the new leave record
            String insertQuery = "INSERT INTO leaves (employee_id, reason, start_date, end_date) "
                    + "VALUES ((SELECT employee_id FROM employees WHERE employee_name = ?), ?, ?, ?)";
            PreparedStatement insertPs = con.prepareStatement(insertQuery);
            insertPs.setString(1, employeeName);
            insertPs.setString(2, leaveReason);
            insertPs.setString(3, startDate);
            insertPs.setString(4, endDate);

            insertPs.executeUpdate();
            loadLeaveData(null, null);  // Reload table
            JOptionPane.showMessageDialog(this, "Leave added successfully.");
            clearAllFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error adding leave: " + e.getMessage());
        }
    }

    private void handleEditLeaveAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a Leave to edit.");
            return;
        }
        if (!validateInputFields()) {
            return;
        }

        String employeeName = jTextFieldEmployeeName.getText().trim();
        String leaveReason = jTextAreaLeaveReason.getText().trim();
        String startDate = jTextFieldStartDate.getText().trim();
        String endDate = jTextFieldEndDate.getText().trim();

        int leaveId = (int) jTableLeaves.getValueAt(selectedRow, 0);  // Get leave_id from table

        try (Connection con = config.getConnection()) {
            // First, check if a record with the same employee_id, start_date, and end_date exists, excluding the current record
            String checkQuery = "SELECT COUNT(*) FROM leaves "
                    + "WHERE employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?) "
                    + "AND start_date = ? AND end_date = ? AND leave_id != ?";
            PreparedStatement checkPs = con.prepareStatement(checkQuery);
            checkPs.setString(1, employeeName);
            checkPs.setString(2, startDate);
            checkPs.setString(3, endDate);
            checkPs.setInt(4, leaveId);

            try (ResultSet rs = checkPs.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    // Record exists, show error message
                    JOptionPane.showMessageDialog(this, "A leave record for this employee with the same start and end date already exists.");
                    return;
                }
            }

            // If no duplicate found, proceed with updating the leave record
            String updateQuery = "UPDATE leaves SET employee_id = (SELECT employee_id FROM employees WHERE employee_name = ?), "
                    + "reason = ?, start_date = ?, end_date = ? WHERE leave_id = ?";
            PreparedStatement ps = con.prepareStatement(updateQuery);
            ps.setString(1, employeeName);
            ps.setString(2, leaveReason);
            ps.setString(3, startDate);
            ps.setString(4, endDate);
            ps.setInt(5, leaveId);

            ps.executeUpdate();
            loadLeaveData(null, null);  // Reload table
            JOptionPane.showMessageDialog(this, "Leave updated successfully.");
            clearAllFields();
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error updating leave: " + e.getMessage());
        }
    }

    private void handleViewLeaveAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a Leave to view.");
            return;
        }

        String leaveDetails = String.format(
                "Leave ID: %s\nEmployee Name: %s\nLeave Reason: %s\nStart Date: %s\nEnd Date: %s",
                jTableLeaves.getValueAt(selectedRow, 0), // leave_id
                jTableLeaves.getValueAt(selectedRow, 1), // employee_name
                jTableLeaves.getValueAt(selectedRow, 2),
                jTableLeaves.getValueAt(selectedRow, 3), // start_date
                jTableLeaves.getValueAt(selectedRow, 4) // end_date
        );

        JOptionPane.showMessageDialog(this, leaveDetails, "Leave Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // Method to delete a leave record
    private void handleDeleteLeaveAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a Leave to delete.");
            return;
        }

        int leaveId = (int) jTableLeaves.getValueAt(selectedRow, 0);  // Get leave_id from table
        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this Leave?", "Delete Leave", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM leaves WHERE leave_id = ?")) {
                ps.setInt(1, leaveId);
                ps.executeUpdate();
                loadLeaveData(null, null);  // Reload table
                JOptionPane.showMessageDialog(this, "Leave deleted successfully.");
                clearAllFields();
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Error deleting leave: " + e.getMessage());
            }
        }
    }

    // Validate input fields for adding/editing leave
    private boolean validateInputFields() {
        String employeeName = jTextFieldEmployeeName.getText().trim();
        String leaveReason = jTextAreaLeaveReason.getText().trim();
        String startDate = jTextFieldStartDate.getText().trim();
        String endDate = jTextFieldEndDate.getText().trim();

        if (employeeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Employee name is required.");
            return false;
        }
        if (leaveReason.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Leave Reason is required.");
            return false;
        }
        if (startDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Start date is required.");
            return false;
        }
        if (endDate.isEmpty()) {
            JOptionPane.showMessageDialog(this, "End date is required.");
            return false;
        }
        return true;
    }

    // Clear all form fields
    private void clearAllFields() {
        jTextFieldEmployeeName.setText("");
        jTextAreaLeaveReason.setText("");
        jTextFieldStartDate.setText("");
        jTextFieldEndDate.setText("");
        loadEmployeeData(null);  // Load employees into jTableEmployees
        loadLeaveData(null, null);  // Load leave data into jTableLeaves
    }

    private void handleSearchEmployees() {
        String searchTerm = jTextFieldSearchEmployees.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            searchTerm = null;  // Treat empty or null input as no search term
        }
        loadEmployeeData(searchTerm);  // Assuming loadEmployeeData loads employees based on the search term
    }

// Method for handling the search functionality for leaves
    private void handleSearchLeaves() {
        String searchTerm = jTextFieldSearchLeaves.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            searchTerm = null;  // Treat empty or null input as no search term
        }
        loadLeaveData(null, searchTerm);  // Assuming loadData loads leaves based on the search term
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableLeaves = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jButtonAdd = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonView = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButtonClose = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldStartDate = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jTextFieldEmployeeName = new javax.swing.JTextField();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldEndDate = new javax.swing.JTextField();
        jButtonToday = new javax.swing.JButton();
        jScrollPane3 = new javax.swing.JScrollPane();
        jTextAreaLeaveReason = new javax.swing.JTextArea();
        jComboBoxSortByLeaves = new javax.swing.JComboBox<>();
        jTextFieldSearchLeaves = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jTableEmployees = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldSearchEmployees = new javax.swing.JTextField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1880, 1000));
        setResizable(false);

        jTableLeaves.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jTableLeaves.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "employee", "leave_reason", "start_date ", "end_date "
            }
        ));
        jTableLeaves.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTableLeaves.setRowHeight(40);
        jTableLeaves.setShowGrid(false);
        jScrollPane1.setViewportView(jTableLeaves);

        jPanel1.setBackground(new java.awt.Color(19, 15, 64));
        jPanel1.setPreferredSize(new java.awt.Dimension(650, 486));

        jButtonAdd.setBackground(new java.awt.Color(34, 166, 179));
        jButtonAdd.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonAdd.setForeground(new java.awt.Color(255, 255, 255));
        jButtonAdd.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/add.png"))); // NOI18N
        jButtonAdd.setText("Add");
        jButtonAdd.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAddActionPerformed(evt);
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

        jButtonDelete.setBackground(new java.awt.Color(255, 121, 121));
        jButtonDelete.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonDelete.setForeground(new java.awt.Color(255, 255, 255));
        jButtonDelete.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/delete.png"))); // NOI18N
        jButtonDelete.setText("Delete");
        jButtonDelete.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDeleteActionPerformed(evt);
            }
        });

        jButtonView.setBackground(new java.awt.Color(249, 202, 36));
        jButtonView.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jButtonView.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/view.png"))); // NOI18N
        jButtonView.setText("View");
        jButtonView.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonViewActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(246, 229, 141));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Manage Leaves");

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

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(223, 249, 251));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("Leave Reason");

        jTextFieldStartDate.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(223, 249, 251));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel6.setText("Start date");

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel7.setForeground(new java.awt.Color(223, 249, 251));
        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel7.setText("Employee");

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

        jTextFieldEmployeeName.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldEmployeeName.setForeground(new java.awt.Color(255, 255, 255));
        jTextFieldEmployeeName.setDisabledTextColor(new java.awt.Color(0, 0, 153));
        jTextFieldEmployeeName.setEnabled(false);

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(223, 249, 251));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel9.setText("End date");

        jTextFieldEndDate.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

        jButtonToday.setBackground(new java.awt.Color(153, 255, 153));
        jButtonToday.setFont(new java.awt.Font("Segoe UI", 0, 16)); // NOI18N
        jButtonToday.setText("Today");
        jButtonToday.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTodayActionPerformed(evt);
            }
        });

        jTextAreaLeaveReason.setColumns(20);
        jTextAreaLeaveReason.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jTextAreaLeaveReason.setRows(5);
        jScrollPane3.setViewportView(jTextAreaLeaveReason);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGap(6, 6, 6)
                            .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                .addComponent(jTextFieldEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGroup(jPanel1Layout.createSequentialGroup()
                                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jTextFieldStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(jTextFieldEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, 190, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                    .addComponent(jButtonToday, javax.swing.GroupLayout.PREFERRED_SIZE, 78, javax.swing.GroupLayout.PREFERRED_SIZE))))
                        .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(27, 27, 27)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(26, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 648, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(484, 484, 484)
                        .addComponent(jButtonView, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(484, 484, 484)
                        .addComponent(jButtonEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 604, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(0, 0, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(12, 12, 12)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(13, 13, 13)
                                .addComponent(jLabel4))
                            .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jButtonView, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(15, 15, 15)
                        .addComponent(jButtonEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 222, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel7)
                        .addGap(24, 24, 24)
                        .addComponent(jLabel6)
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel9)
                            .addComponent(jTextFieldEndDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jTextFieldEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldStartDate, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(56, 56, 56)
                        .addComponent(jButtonToday, javax.swing.GroupLayout.PREFERRED_SIZE, 94, javax.swing.GroupLayout.PREFERRED_SIZE))))
        );

        jComboBoxSortByLeaves.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jComboBoxSortByLeaves.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxSortByLeaves.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSortByLeavesActionPerformed(evt);
            }
        });

        jTextFieldSearchLeaves.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldSearchLeaves.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSearchLeavesActionPerformed(evt);
            }
        });
        jTextFieldSearchLeaves.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldSearchLeavesKeyPressed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setText("Search");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel3.setText("Sort By");

        jTableEmployees.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jTableEmployees.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "name"
            }
        ));
        jTableEmployees.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTableEmployees.setRowHeight(40);
        jTableEmployees.setShowGrid(false);
        jScrollPane2.setViewportView(jTableEmployees);

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel5.setText("Search");

        jTextFieldSearchEmployees.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldSearchEmployees.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSearchEmployeesActionPerformed(evt);
            }
        });
        jTextFieldSearchEmployees.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldSearchEmployeesKeyPressed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldSearchEmployees, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane1)
                        .addContainerGap())
                    .addGroup(layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldSearchLeaves, javax.swing.GroupLayout.PREFERRED_SIZE, 256, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(29, 29, 29)
                        .addComponent(jLabel3)
                        .addGap(18, 18, 18)
                        .addComponent(jComboBoxSortByLeaves, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap(168, Short.MAX_VALUE))))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(jLabel3)
                                .addComponent(jTextFieldSearchEmployees, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5)
                                .addComponent(jTextFieldSearchLeaves, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel2))
                            .addComponent(jComboBoxSortByLeaves, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 823, Short.MAX_VALUE)
                            .addComponent(jScrollPane2)))
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 879, Short.MAX_VALUE))
                .addContainerGap())
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldSearchLeavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSearchLeavesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldSearchLeavesActionPerformed

    private void jComboBoxSortByLeavesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSortByLeavesActionPerformed

    }//GEN-LAST:event_jComboBoxSortByLeavesActionPerformed

    private void jTextFieldSearchLeavesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldSearchLeavesKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            handleSearchLeaves();  // Trigger search when Enter is pressed for leaves
        }
    }//GEN-LAST:event_jTextFieldSearchLeavesKeyPressed

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jButtonViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewActionPerformed
        allActionPerformed("VIEW");
    }//GEN-LAST:event_jButtonViewActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        allActionPerformed("DELETE");
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jButtonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEditActionPerformed
        allActionPerformed("EDIT");
    }//GEN-LAST:event_jButtonEditActionPerformed

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        allActionPerformed("ADD");
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAllFields();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private void jTextFieldSearchEmployeesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSearchEmployeesActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldSearchEmployeesActionPerformed

    private void jTextFieldSearchEmployeesKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldSearchEmployeesKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            handleSearchEmployees();  // Trigger search when Enter is pressed for employees
        }
    }//GEN-LAST:event_jTextFieldSearchEmployeesKeyPressed

    private void jButtonTodayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTodayActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jButtonTodayActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Leaves().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonToday;
    private javax.swing.JButton jButtonView;
    private javax.swing.JComboBox<String> jComboBoxSortByLeaves;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTable jTableEmployees;
    private javax.swing.JTable jTableLeaves;
    private javax.swing.JTextArea jTextAreaLeaveReason;
    private javax.swing.JTextField jTextFieldEmployeeName;
    private javax.swing.JTextField jTextFieldEndDate;
    private javax.swing.JTextField jTextFieldSearchEmployees;
    private javax.swing.JTextField jTextFieldSearchLeaves;
    private javax.swing.JTextField jTextFieldStartDate;
    // End of variables declaration//GEN-END:variables
}
