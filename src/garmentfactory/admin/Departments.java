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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class Departments extends javax.swing.JFrame {

    private DefaultTableModel model;

    public Departments() {
        initComponents();
        model = (DefaultTableModel) jTableDepartments.getModel();
        loadData(null, null);

        jTextFieldSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String searchTerm = jTextFieldSearch.getText();
                    String orderByColumn = (String) jComboBoxSortBy.getSelectedItem();
                    loadData(orderByColumn, searchTerm);
                }
            }
        });

        jComboBoxSortBy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String searchTerm = jTextFieldSearch.getText();
                String orderByColumn = (String) jComboBoxSortBy.getSelectedItem();
                loadData(orderByColumn, searchTerm);
            }
        });

        jComboBoxSortBy.removeAllItems();
        jComboBoxSortBy.addItem("department_name");

        initTableSelectionListener();
    }

    private void loadData(String orderByColumn, String searchTerm) {
        model.setRowCount(0);

        String query = "SELECT d.department_id, d.department_name, COUNT(e.employee_id) AS total_employees "
                + "FROM departments d "
                + "LEFT JOIN employees e ON d.department_id = e.department_id ";

        boolean hasSearch = searchTerm != null && !searchTerm.trim().isEmpty();
        boolean hasSort = orderByColumn != null && !orderByColumn.trim().isEmpty();

        // Add search condition
        if (hasSearch) {
            query += "WHERE d.department_name LIKE ? ";
        }

        // Add grouping
        query += "GROUP BY d.department_id, d.department_name ";

        // Add sorting condition
        if (hasSort) {
            query += "ORDER BY " + orderByColumn;
        }

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            if (hasSearch) {
                String searchPattern = "%" + searchTerm.trim() + "%";
                ps.setString(1, searchPattern);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getObject("department_id"),
                        rs.getObject("department_name"),
                        rs.getObject("total_employees"),});
                }
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }

    // Validate form fields before any operation
    private boolean validateInputFields() {
        String departmentName = jTextFieldDepartmentName.getText().trim();

        if (departmentName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Department name is required.");
            return false;
        }
        return true;
    }

    // Initialize table selection listener to populate the form when a row is selected
    private void initTableSelectionListener() {
        jTableDepartments.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = jTableDepartments.getSelectedRow();
                if (selectedRow != -1) {
                    updateFormFieldsWithSelectedRow(selectedRow);
                }
            }
        });
    }

    // Populate form fields based on selected row
    private void updateFormFieldsWithSelectedRow(int selectedRow) {
        try {
            String currentDepartmentName = (String) jTableDepartments.getValueAt(selectedRow, 1);  // Index 1: department_name
            jTextFieldDepartmentName.setText(currentDepartmentName != null ? currentDepartmentName : "");
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating form fields: " + e.getMessage());
        }
    }

    // Handle all CRUD actions (add, edit, delete, view)
    private void allActionPerformed(java.awt.event.ActionEvent evt, String actionType) {
        int selectedRow = jTableDepartments.getSelectedRow();

        switch (actionType) {
            case "ADD" ->
                handleAddAction();
            case "EDIT" ->
                handleEditAction(selectedRow);
            case "DELETE" ->
                handleDeleteAction(selectedRow);
            case "VIEW" ->
                handleViewAction(selectedRow);
            default ->
                throw new IllegalArgumentException("Unknown action type: " + actionType);
        }
    }

    // Add a new department
    private void handleAddAction() {
        if (!validateInputFields()) {
            return;
        }

        String departmentName = jTextFieldDepartmentName.getText().trim();

        try (Connection con = config.getConnection()) {
            String query = "INSERT INTO departments (department_name) VALUES (?)";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, departmentName);

            ps.executeUpdate();
            loadData(null, null);  // Reload table
            JOptionPane.showMessageDialog(this, "Department added successfully.");
            clearAllFields();
        } catch (SQLException e) {
            showError("Error adding department", e);
        }
    }

    // Edit an existing department
    private void handleEditAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a department to edit.");
            return;
        }
        if (!validateInputFields()) {
            return;
        }

        String departmentName = jTextFieldDepartmentName.getText().trim();
        int departmentId = (int) jTableDepartments.getValueAt(selectedRow, 0);  // Get department_id from table

        try (Connection con = config.getConnection()) {
            String query = "UPDATE departments SET department_name = ? WHERE department_id = ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, departmentName);
            ps.setInt(2, departmentId);

            ps.executeUpdate();
            loadData(null, null);  // Reload table
            JOptionPane.showMessageDialog(this, "Department updated successfully.");
            clearAllFields();
        } catch (SQLException e) {
            showError("Error updating department", e);
        }
    }

    // Delete a department
    private void handleDeleteAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a department to delete.");
            return;
        }

        int departmentId = (int) jTableDepartments.getValueAt(selectedRow, 0);  // Get department_id from table
        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this department?", "Delete Department", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM departments WHERE department_id = ?")) {
                ps.setInt(1, departmentId);
                ps.executeUpdate();
                loadData(null, null);  // Reload table
                JOptionPane.showMessageDialog(this, "Department deleted successfully.");
                clearAllFields();
            } catch (SQLException e) {
                showError("Error deleting department", e);
            }
        }
    }

    // View department details
    private void handleViewAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a department to view.");
            return;
        }

        String departmentDetails = String.format(
                "Department ID: %s\nDepartment Name: %s\nCreated At: %s\n",
                jTableDepartments.getValueAt(selectedRow, 0),
                jTableDepartments.getValueAt(selectedRow, 1),
                jTableDepartments.getValueAt(selectedRow, 2)
        );

        JOptionPane.showMessageDialog(this, departmentDetails, "Department Details", JOptionPane.INFORMATION_MESSAGE);
    }

    // Show error message
    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage());
    }

    // Clear all form fields
    private void clearAllFields() {
        jTextFieldDepartmentName.setText("");
        jTextFieldSearch.setText("");
        jComboBoxSortBy.setSelectedIndex(0);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableDepartments = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jButtonAdd = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButtonClose = new javax.swing.JButton();
        jTextFieldDepartmentName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jComboBoxSortBy = new javax.swing.JComboBox<>();
        jTextFieldSearch = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1880, 1000));
        setResizable(false);

        jTableDepartments.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jTableDepartments.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "Department", "Total Employees"
            }
        ));
        jTableDepartments.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTableDepartments.setRowHeight(40);
        jTableDepartments.setShowGrid(false);
        jScrollPane1.setViewportView(jTableDepartments);
        if (jTableDepartments.getColumnModel().getColumnCount() > 0) {
            jTableDepartments.getColumnModel().getColumn(0).setMinWidth(100);
            jTableDepartments.getColumnModel().getColumn(0).setPreferredWidth(100);
            jTableDepartments.getColumnModel().getColumn(0).setMaxWidth(100);
        }

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

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(246, 229, 141));
        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel1.setText("Manage Departments");

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

        jTextFieldDepartmentName.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

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

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(0, 0, 0)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(jButtonClose, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jTextFieldDepartmentName, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(27, 27, 27)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jButtonAdd, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonClear, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonDelete, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addComponent(jButtonEdit, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 648, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(43, 43, 43))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 71, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jTextFieldDepartmentName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(18, 18, 18)
                .addComponent(jButtonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(417, Short.MAX_VALUE))
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
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 823, Short.MAX_VALUE)
                .addContainerGap())
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 891, Short.MAX_VALUE)
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

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        allActionPerformed(evt, "ADD");
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEditActionPerformed
        allActionPerformed(evt, "EDIT");
    }//GEN-LAST:event_jButtonEditActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        allActionPerformed(evt, "DELETE");
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Departments().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JComboBox<String> jComboBoxSortBy;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableDepartments;
    private javax.swing.JTextField jTextFieldDepartmentName;
    private javax.swing.JTextField jTextFieldSearch;
    // End of variables declaration//GEN-END:variables
}
