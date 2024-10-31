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
import java.time.YearMonth;

public class BlackLists extends javax.swing.JFrame {

    private DefaultTableModel model;

    public BlackLists() {
        initComponents();
        model = (DefaultTableModel) jTableBlackLists.getModel();
        loadData(null, null);
        setupEventListeners();
        populateSortOptions();
    }

    // Method to initialize event listeners
    private void setupEventListeners() {
        // Search action when pressing Enter in the search text field
        jTextFieldSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    loadDataFromUI();
                }
            }
        });

        // Sorting action when a new sort option is selected
        jComboBoxSortBy.addActionListener(e -> loadDataFromUI());

        // Button action to view selected employee details
        jButtonView.addActionListener(e -> {
            int selectedRow = jTableBlackLists.getSelectedRow();
            handleViewAction(selectedRow);
        });
    }

    // Method to populate sorting options
    private void populateSortOptions() {
        jComboBoxSortBy.removeAllItems();
        jComboBoxSortBy.addItem("employee_name");
        jComboBoxSortBy.addItem("department_name");
    }

    // Load employee data with optional search and sorting
    private void loadData(String orderByColumn, String searchTerm) {
        model.setRowCount(0);
        String query = createBlacklistQuery(orderByColumn, searchTerm);
        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            setQueryParameters(ps, searchTerm);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getObject("employee_id"),
                        rs.getObject("employee_name"),
                        rs.getObject("phone"),
                        rs.getObject("department_name"),
                        rs.getObject("team_leader_name"),
                    });
                }
            }
        } catch (SQLException e) {
            showError("Error loading data", e);
        }
    }

    // Create the SQL query for loading blacklisted employees
    private String createBlacklistQuery(String orderByColumn, String searchTerm) {
        String query = "SELECT e.employee_id, e.employee_name, e.phone, d.department_name, u.username AS team_leader_name, "
                + "(SELECT COUNT(*) FROM leaves l WHERE l.employee_id = e.employee_id AND l.start_date >= ? AND l.end_date <= ?) AS leave_count, "
                + "(SELECT COUNT(*) FROM attendance a WHERE a.employee_id = e.employee_id AND a.late_time > 0 AND a.date >= ? AND a.date <= ?) AS late_count, "
                + "(SELECT COUNT(*) FROM attendance a WHERE a.employee_id = e.employee_id AND a.work_time = 0 AND a.date >= ? AND a.date <= ?) AS no_work_count "
                + "FROM employees e "
                + "JOIN departments d ON e.department_id = d.department_id "
                + "LEFT JOIN team_leader tl ON e.employee_id = tl.employee_id "
                + "LEFT JOIN users u ON tl.user_id = u.user_id "
                + "WHERE e.employee_id IN ("
                + "    SELECT employee_id FROM leaves WHERE start_date >= ? AND end_date <= ? GROUP BY employee_id HAVING COUNT(*) > 3 "
                + "    UNION "
                + "    SELECT employee_id FROM attendance WHERE date >= ? AND date <= ? AND check_in IS NULL GROUP BY employee_id HAVING COUNT(*) > 3 "
                + "    UNION "
                + "    SELECT employee_id FROM attendance WHERE date >= ? AND date <= ? AND (late_time > 0 OR work_time = 0) GROUP BY employee_id HAVING COUNT(*) >= 4"
                + ")";

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            query += " AND (e.employee_name LIKE ? OR d.department_name LIKE ? OR u.username LIKE ?)";
        }

        if (orderByColumn != null && !orderByColumn.trim().isEmpty()) {
            query += " ORDER BY " + orderByColumn;
        }

        return query;
    }

    // Set parameters for the prepared statement
    private void setQueryParameters(PreparedStatement ps, String searchTerm) throws SQLException {
        YearMonth currentMonth = YearMonth.now();
        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();

        // Set parameters for leave and attendance counts
        for (int i = 1; i <= 6; i++) {
            if (i % 2 == 1) {
                ps.setDate(i, java.sql.Date.valueOf(firstDayOfMonth));
            } else {
                ps.setDate(i, java.sql.Date.valueOf(lastDayOfMonth));
            }
        }

        // Set parameters for subquery
        for (int i = 7; i <= 12; i++) {
            if (i % 2 == 1) {
                ps.setDate(i, java.sql.Date.valueOf(firstDayOfMonth));
            } else {
                ps.setDate(i, java.sql.Date.valueOf(lastDayOfMonth));
            }
        }

        // Set search parameters if available
        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            String searchPattern = "%" + searchTerm.trim() + "%";
            ps.setString(13, searchPattern);
            ps.setString(14, searchPattern);
            ps.setString(15, searchPattern);
        }
    }
// Handle viewing the details of the selected employee
private void handleViewAction(int selectedRow) {
    if (selectedRow == -1) {
        JOptionPane.showMessageDialog(this, "Please select an employee to view.");
        return;
    }

    int employeeId = (int) jTableBlackLists.getValueAt(selectedRow, 0);
    StringBuilder details = new StringBuilder("<html><body style='font-size:20px;'>Employee Details<br>");
    appendEmployeeDetails(selectedRow, details);
    appendLeaveDetails(employeeId, details);
    appendLateTimeDetails(employeeId, details);
    details.append("</body></html>");

    JOptionPane.showMessageDialog(this, details.toString(), "Employee Details", JOptionPane.INFORMATION_MESSAGE);
}

private void appendEmployeeDetails(int selectedRow, StringBuilder details) {
    details.append(String.format("%-20s: %s<br>", "Employee ID", jTableBlackLists.getValueAt(selectedRow, 0)))
           .append(String.format("%-20s: %s<br>", "Employee Name", jTableBlackLists.getValueAt(selectedRow, 1)))
           .append(String.format("%-20s: %s<br>", "Phone", jTableBlackLists.getValueAt(selectedRow, 2)))
           .append(String.format("%-20s: %s<br>", "Department", jTableBlackLists.getValueAt(selectedRow, 3)))
           .append(String.format("%-20s: %s<br><br>", "Team Leader", jTableBlackLists.getValueAt(selectedRow, 4)));
}

// Append leave details to the StringBuilder
private void appendLeaveDetails(int employeeId, StringBuilder details) {
    details.append("Leave Details:<br>");
    String leaveQuery = "SELECT start_date, end_date FROM leaves WHERE employee_id = ?";
    try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(leaveQuery)) {
        ps.setInt(1, employeeId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                details.append(String.format("%-20s: Start Date = %-12s, End Date = %-12s<br>", "Leave", rs.getDate("start_date"), rs.getDate("end_date")));
            }
        }
    } catch (SQLException e) {
        showError("Error loading leave details", e);
    }
}

// Append late time details to the StringBuilder
private void appendLateTimeDetails(int employeeId, StringBuilder details) {
    details.append("<br>Late Time Details:<br>");
    String lateQuery = "SELECT date, late_time FROM attendance WHERE employee_id = ? AND late_time > 0";
    try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(lateQuery)) {
        ps.setInt(1, employeeId);
        try (ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                details.append(String.format("%-20s: Date = %-12s, Late Time = %d hours<br>", "Late Time", rs.getDate("date"), rs.getInt("late_time")));
            }
        }
    } catch (SQLException e) {
        showError("Error loading late time details", e);
    }
}

    // Show error message
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage());
    }

    // Clear all form fields
    private void clearAllFields() {
        jTextFieldSearch.setText("");
        jComboBoxSortBy.setSelectedIndex(0);
    }

    // Load data based on user input
    private void loadDataFromUI() {
        String searchTerm = jTextFieldSearch.getText();
        String orderByColumn = (String) jComboBoxSortBy.getSelectedItem();
        loadData(orderByColumn, searchTerm);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableBlackLists = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jButtonClose = new javax.swing.JButton();
        jButtonClear = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jTextFieldSearch = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jComboBoxSortBy = new javax.swing.JComboBox<>();
        jButtonView = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1880, 1000));
        setResizable(false);

        jTableBlackLists.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jTableBlackLists.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "employee name", "phone", "department name", "team leader name"
            }
        ));
        jTableBlackLists.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTableBlackLists.setRowHeight(40);
        jTableBlackLists.setShowGrid(false);
        jScrollPane1.setViewportView(jTableBlackLists);

        jPanel1.setBackground(new java.awt.Color(19, 15, 64));
        jPanel1.setPreferredSize(new java.awt.Dimension(650, 486));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(246, 229, 141));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Black lists");

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

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(255, 255, 0));
        jLabel2.setText("Search");

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

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel3.setForeground(new java.awt.Color(255, 255, 0));
        jLabel3.setText("Sort By");

        jComboBoxSortBy.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jComboBoxSortBy.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxSortBy.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSortByActionPerformed(evt);
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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 365, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonView, javax.swing.GroupLayout.PREFERRED_SIZE, 262, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel2)
                .addGap(18, 18, 18)
                .addComponent(jTextFieldSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 342, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jLabel3)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jComboBoxSortBy, javax.swing.GroupLayout.PREFERRED_SIZE, 193, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(272, Short.MAX_VALUE))
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addComponent(jLabel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jTextFieldSearch, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel2)
                        .addComponent(jComboBoxSortBy, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel3)
                        .addComponent(jButtonClear))
                    .addComponent(jButtonView, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(22, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jScrollPane1)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1831, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, 170, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 715, Short.MAX_VALUE))
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

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAllFields();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private void jButtonViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewActionPerformed

    }//GEN-LAST:event_jButtonViewActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new BlackLists().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonView;
    private javax.swing.JComboBox<String> jComboBoxSortBy;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableBlackLists;
    private javax.swing.JTextField jTextFieldSearch;
    // End of variables declaration//GEN-END:variables

    private void handleViewAction() {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }
}
