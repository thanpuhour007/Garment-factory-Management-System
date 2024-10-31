package garmentfactory.admin;

import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import DatabaseConnection.config;
import java.awt.Component;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.sql.Statement;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.RowFilter;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.table.TableRowSorter;

public class Employees extends javax.swing.JFrame {

    private DefaultTableModel model;
    private TableRowSorter<DefaultTableModel> sorter;
    private String lastSearchTerm, lastOrderBy, lastFilterByDepartment;

    public Employees() {
        initComponents();
        model = (DefaultTableModel) jTableEmployees.getModel();
        sorter = new TableRowSorter<>(model);
        jTableEmployees.setRowSorter(sorter);  // Set the sorter to the table
        populateComboBoxForFiltering(jComboBoxSortByDepartment, "SELECT department_name FROM departments");
        populateComboBoxForEmployee(jComboBoxDepartment, "SELECT department_name FROM departments");

        setupSearchAndSortListeners();
        initTableSelectionListener();
        loadData(null, null, null);
    }

    private void setupSearchAndSortListeners() {
        KeyAdapter searchListener = new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                performSearchAndSort();
            }
        };

        jTextFieldSearchEmployeeName.addKeyListener(searchListener);

        jComboBoxPosition.setModel(new DefaultComboBoxModel<>(new String[]{
            "Manager", "Supervisor", "Operator", "Technician", "Assistant",
            "Quality Control", "Cutting Master", "Pattern Maker", "Sewing Operator",
            "Maintenance Technician", "Warehouse Manager", "Packing Supervisor",
            "Production Planner", "HR Officer", "Safety Officer", "Finishing Supervisor"
        }));
    }

    private void performSearchAndSort() {
        String searchTermEmployee = jTextFieldSearchEmployeeName.getText().trim();

        // Check if an item is selected in the combo box (non-null)
        String filterByDepartment = jComboBoxSortByDepartment.getSelectedItem() != null ? jComboBoxSortByDepartment.getSelectedItem().toString() : null;

        sortAndFilterTable(searchTermEmployee, filterByDepartment);
    }

    private void sortAndFilterTable(String searchTermEmployee, String filterByDepartment) {
        List<RowFilter<Object, Object>> filters = new ArrayList<>();

        // Create a filter based on the employee name search term
        if (searchTermEmployee != null && !searchTermEmployee.isEmpty()) {
            filters.add(RowFilter.regexFilter("(?i)" + searchTermEmployee, 1)); // Assuming employee name is in column 1
        }

        // Only filter by department if it's not "All Departments"
        if (filterByDepartment != null && !"All Departments".equals(filterByDepartment)) {
            filters.add(RowFilter.regexFilter(filterByDepartment, 5)); // Assuming department name is in column 5
        }

        // Combine the filters if both are present
        if (!filters.isEmpty()) {
            RowFilter<Object, Object> combinedFilter = RowFilter.andFilter(filters);
            sorter.setRowFilter(combinedFilter);
        } else {
            sorter.setRowFilter(null); // Remove filters if none is applied
        }
    }

    private boolean shouldLoadData(String orderByColumn, String filterByDepartment, String searchTermEmployee) {
        if (Objects.equals(orderByColumn, lastOrderBy)
                && Objects.equals(filterByDepartment, lastFilterByDepartment)
                && Objects.equals(searchTermEmployee, lastSearchTerm)) {
            return false;
        }

        lastOrderBy = orderByColumn;
        lastFilterByDepartment = filterByDepartment;
        lastSearchTerm = searchTermEmployee;

        return true;
    }
    
//    private String roleName;

    private void loadData(String orderByColumn, String filterByDepartment, String searchTermEmployee) {
        model.setRowCount(0);

        List<String> validColumns = Arrays.asList("employee_name", "team_leader_name", "department_name", "position");

        String orderBy = validColumns.contains(orderByColumn) ? orderByColumn : "employee_name";

        String query = "SELECT employees.employee_id, employees.employee_name, employees.phone, employees.basic_salary, "
                + "CASE "
                + "    WHEN team_leader.user_id = 1 THEN 'Not Team' "
                + "    ELSE tl_users.username "
                + "END AS team_leader_name, "
                + "departments.department_name, employees.position, employees.created_at "
                + "FROM employees "
                + "LEFT JOIN team_leader ON employees.employee_id = team_leader.employee_id "
                + "LEFT JOIN users tl_users ON team_leader.user_id = tl_users.user_id "
                + "LEFT JOIN departments ON employees.department_id = departments.department_id";

        boolean hasSearchEmployee = searchTermEmployee != null && !searchTermEmployee.isEmpty();
        boolean hasFilterByDepartment = filterByDepartment != null && !filterByDepartment.isEmpty();

        // Build query conditions based on the filters.
        StringBuilder whereClause = new StringBuilder();
        if (hasSearchEmployee || hasFilterByDepartment) {
            whereClause.append(" WHERE ");
            if (hasSearchEmployee) {
                whereClause.append("(employees.employee_name LIKE ? OR employees.phone LIKE ? OR tl_users.username LIKE ? OR employees.position LIKE ?)");
            }
            if (hasFilterByDepartment) {
                if (hasSearchEmployee) {
                    whereClause.append(" AND ");
                }
                whereClause.append("departments.department_name = ?");
            }
        }

        query += whereClause + " ORDER BY " + orderBy;

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            int paramIndex = 1;
            if (hasSearchEmployee) {
                String searchPattern = "%" + searchTermEmployee + "%";
                for (int i = 0; i < 4; i++) {
                    ps.setString(paramIndex++, searchPattern);
                }
            }
            if (hasFilterByDepartment) {
                ps.setString(paramIndex++, filterByDepartment);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    model.addRow(new Object[]{
                        rs.getObject("employee_id"),
                        rs.getObject("employee_name"),
                        rs.getObject("phone"),
                        rs.getObject("basic_salary"),
                        rs.getObject("team_leader_name"),
                        rs.getObject("department_name"),
                        rs.getObject("position"),
                        rs.getObject("created_at")
                    });
                }
            }
        } catch (SQLException e) {
            showError("Error loading data", e);
        }
    }

    private void clearAllFields() {
        jTextFieldEmployeeName.setText("");
        jTextFieldPhone.setText("");
        jTextFieldSearchEmployeeName.setText("");
        jTextFieldBasicSalary.setText("300");
        jComboBoxDepartment.setSelectedIndex(0);
        loadData(null, null, null);
    }

    private void populateComboBoxForFiltering(JComboBox<String> comboBox, String query) {
        comboBox.removeAllItems();  // Clear existing items

        // Add a default option "All Departments" for filtering
        comboBox.addItem("All Departments");

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String departmentName = rs.getString(1);  // Assuming `department_name` is the first column in your result set
                comboBox.addItem(departmentName);
            }
        } catch (SQLException e) {
            e.printStackTrace();  // Print full stack trace for debugging
            showError("Error populating combo box", e);
        }
    }

    private void populateComboBoxForEmployee(JComboBox<String> comboBox, String query) {
        comboBox.removeAllItems();  // Clear existing items

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String departmentName = rs.getString(1);  // Assuming `department_name` is the first column in your result set
                comboBox.addItem(departmentName);
            }
        } catch (SQLException e) {
            e.printStackTrace();  // Print full stack trace for debugging
            showError("Error populating combo box", e);
        }
    }

    private void showError(String message, Exception e) {
        JOptionPane.showMessageDialog(this, message + ": " + e.getMessage());
    }

    private void initTableSelectionListener() {
        jTableEmployees.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = jTableEmployees.getSelectedRow();
                if (selectedRow != -1) {
                    updateFormFieldsWithSelectedRow(selectedRow);
                }
            }
        });
    }

    private int getDepartmentIdByName(String departmentName) throws SQLException {
        String query = "SELECT department_id FROM departments WHERE department_name = ?";
        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, departmentName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("department_id");
                }
            }
        }
        throw new SQLException("Department not found: " + departmentName);
    }

    private void updateFormFieldsWithSelectedRow(int selectedRow) {
        try {
            String currentEmployeeName = (String) jTableEmployees.getValueAt(selectedRow, 1);
            String currentPhone = (String) jTableEmployees.getValueAt(selectedRow, 2);
            String currentBasicSalary = jTableEmployees.getValueAt(selectedRow, 3).toString();
//            String currentTeamLeaderName = (String) jTableEmployees.getValueAt(selectedRow, 4);
            String currentDepartmentId = (String) jTableEmployees.getValueAt(selectedRow, 5);

            jTextFieldEmployeeName.setText(currentEmployeeName != null ? currentEmployeeName : "");
            jTextFieldPhone.setText(currentPhone != null ? currentPhone : "");
            jTextFieldBasicSalary.setText(currentBasicSalary != null ? currentBasicSalary : "");

            if (currentDepartmentId != null) {
                jComboBoxDepartment.setSelectedItem(currentDepartmentId);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error updating form fields: " + e.getMessage());
        }
    }

    private boolean validateInputFields() {
        String employeeName = jTextFieldEmployeeName.getText().trim();
        String phone = jTextFieldPhone.getText().trim();
        String basicSalaryStr = jTextFieldBasicSalary.getText().trim();
        String departmentName = (String) jComboBoxDepartment.getSelectedItem();
        String position = (String) jComboBoxPosition.getSelectedItem(); // Validate position

        if (employeeName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Employee name is required.");
            return false;
        }

        if (phone.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Phone number is required.");
            return false;
        }

        if (basicSalaryStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Basic salary is required.");
            return false;
        }

        try {
            new BigDecimal(basicSalaryStr); // Validate decimal format
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Invalid basic salary format. Please enter a valid number.");
            return false;
        }

        if (departmentName == null || departmentName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a valid department.");
            return false;
        }
        if (position == null || position.isEmpty()) { // Check for position
            JOptionPane.showMessageDialog(this, "Please select a valid position.");
            return false;
        }
        return true;
    }

    private void allActionPerformed(java.awt.event.ActionEvent evt, String actionType) {
        int selectedRow = jTableEmployees.getSelectedRow();

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

    private void handleAddAction() {
        if (!validateInputFields()) {
            return;
        }

        String employeeName = jTextFieldEmployeeName.getText().trim();
        String phone = jTextFieldPhone.getText().trim();
        String basicSalaryStr = jTextFieldBasicSalary.getText().trim();
        String departmentName = (String) jComboBoxDepartment.getSelectedItem();
        String position = (String) jComboBoxPosition.getSelectedItem();

        try (Connection con = config.getConnection()) {
            con.setAutoCommit(false); // Start transaction

            int departmentId = getDepartmentIdByName(departmentName);
            BigDecimal basicSalary = new BigDecimal(basicSalaryStr);

            // Insert a new employee
            String insertEmployeeQuery = "INSERT INTO employees (employee_name, phone, basic_salary, department_id, position) VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertEmployeeQuery, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, employeeName);
                ps.setString(2, phone);
                ps.setBigDecimal(3, basicSalary);
                ps.setInt(4, departmentId);
                ps.setString(5, position);
                ps.executeUpdate();

                // Get the generated employee_id
                try (ResultSet generatedKeys = ps.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int employeeId = generatedKeys.getInt(1); // Get the generated employee ID

                        // Insert into team_leader table with default user_id = 1
                        String insertTeamLeaderQuery = "INSERT INTO team_leader (user_id, employee_id) VALUES (?, ?)";
                        try (PreparedStatement teamLeaderPs = con.prepareStatement(insertTeamLeaderQuery)) {
                            teamLeaderPs.setInt(1, 1); // Default user_id = 1
                            teamLeaderPs.setInt(2, employeeId); // Insert employee_id
                            teamLeaderPs.executeUpdate();
                        }
                    } else {
                        throw new SQLException("Failed to retrieve the employee_id.");
                    }
                }

                con.commit(); // Commit transaction
                loadData(null, null, null);
                JOptionPane.showMessageDialog(this, "Employee added successfully.");
                clearAllFields();
            } catch (SQLException e) {
                con.rollback(); // Rollback transaction if any error occurs
                showError("Error adding Employee", e);
            }
        } catch (SQLException e) {
            showError("Error establishing connection", e);
        }
    }

    private void handleEditAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an Employee to edit.");
            return;
        }
        if (!validateInputFields()) {
            return;
        }

        String employeeName = jTextFieldEmployeeName.getText().trim();
        String phone = jTextFieldPhone.getText().trim();
        String basicSalaryStr = jTextFieldBasicSalary.getText().trim();
        String departmentName = (String) jComboBoxDepartment.getSelectedItem();
        String position = (String) jComboBoxPosition.getSelectedItem();

        int employeeId = (int) jTableEmployees.getValueAt(selectedRow, 0); // Get employee_id from table

        try (Connection con = config.getConnection()) {
            con.setAutoCommit(false); // Start transaction

            int departmentId = getDepartmentIdByName(departmentName);
            BigDecimal basicSalary = new BigDecimal(basicSalaryStr);

            // Update employee information
            String query = "UPDATE employees SET employee_name = ?, phone = ?, basic_salary = ?, department_id = ?, position = ? WHERE employee_id = ?";
            try (PreparedStatement ps = con.prepareStatement(query)) {
                ps.setString(1, employeeName);
                ps.setString(2, phone);
                ps.setBigDecimal(3, basicSalary);
                ps.setInt(4, departmentId);
                ps.setString(5, position);
                ps.setInt(6, employeeId);
                ps.executeUpdate();

                con.commit(); // Commit transaction
                loadData(null, null, null);
                JOptionPane.showMessageDialog(this, "Employee updated successfully.");
                clearAllFields();
            } catch (SQLException e) {
                con.rollback(); // Rollback if there's an error
                showError("Error updating Employee", e);
            }
        } catch (SQLException e) {
            showError("Error establishing connection", e);
        }
    }

    private void handleDeleteAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an Employee to delete.");
            return;
        }

        int employeeId = (int) jTableEmployees.getValueAt(selectedRow, 0);
        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this Employee?", "Delete Employee", JOptionPane.YES_NO_OPTION);

        if (option == JOptionPane.YES_OPTION) {
            try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement("DELETE FROM employees WHERE employee_id = ?")) {
                ps.setInt(1, employeeId);
                ps.executeUpdate();
                loadData(null, null, null);

                JOptionPane.showMessageDialog(this, "Employee deleted successfully.");
                clearAllFields();
            } catch (SQLException e) {
                showError("Error deleting Employee", e);
            }
        }
    }

    private void handleViewAction(int selectedRow) {
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select an Employee to view.");
            return;
        }

        String employeeDetails = String.format(
                "Employee ID: %s\nName: %s\nPhone: %s\nBasic Salary: %s\nTeam Leader: %s\nDepartment: %s\n",
                Objects.toString(jTableEmployees.getValueAt(selectedRow, 0), "N/A"),
                Objects.toString(jTableEmployees.getValueAt(selectedRow, 1), "N/A"),
                Objects.toString(jTableEmployees.getValueAt(selectedRow, 2), "N/A"),
                Objects.toString(jTableEmployees.getValueAt(selectedRow, 3), "N/A"),
                Objects.toString(jTableEmployees.getValueAt(selectedRow, 4), "N/A"),
                Objects.toString(jTableEmployees.getValueAt(selectedRow, 5), "N/A")
        );

        JOptionPane.showMessageDialog(this, employeeDetails, "Employee Details", JOptionPane.INFORMATION_MESSAGE);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jTableEmployees = new javax.swing.JTable();
        jPanel1 = new javax.swing.JPanel();
        jButtonAdd = new javax.swing.JButton();
        jButtonEdit = new javax.swing.JButton();
        jButtonDelete = new javax.swing.JButton();
        jButtonView = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jButtonClose = new javax.swing.JButton();
        jTextFieldEmployeeName = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jTextFieldPhone = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        jComboBoxDepartment = new javax.swing.JComboBox<>();
        jLabel8 = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jLabel9 = new javax.swing.JLabel();
        jTextFieldBasicSalary = new javax.swing.JTextField();
        jComboBoxPosition = new javax.swing.JComboBox<>();
        jLabel12 = new javax.swing.JLabel();
        jButtonTeamManagement = new javax.swing.JButton();
        jTextFieldSearchEmployeeName = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jComboBoxSortByDepartment = new javax.swing.JComboBox<>();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setAutoRequestFocus(false);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1880, 1000));
        setResizable(false);

        jTableEmployees.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jTableEmployees.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "ID", "name", "phone ", "salary ", "TeamLeader", "department", "position", "created_at"
            }
        ));
        jTableEmployees.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        jTableEmployees.setRowHeight(40);
        jTableEmployees.setShowGrid(false);
        jScrollPane1.setViewportView(jTableEmployees);

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
        jLabel1.setText("Manage Employees");

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

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel4.setForeground(new java.awt.Color(223, 249, 251));
        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel4.setText("Employee Name");

        jTextFieldPhone.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel6.setForeground(new java.awt.Color(223, 249, 251));
        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel6.setText("Phone");

        jComboBoxDepartment.setEditable(true);
        jComboBoxDepartment.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jComboBoxDepartment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxDepartment.setAutoscrolls(true);
        jComboBoxDepartment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxDepartmentActionPerformed(evt);
            }
        });

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(223, 249, 251));
        jLabel8.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel8.setText("Department");

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

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel9.setForeground(new java.awt.Color(223, 249, 251));
        jLabel9.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel9.setText("Salary ($)");

        jTextFieldBasicSalary.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldBasicSalary.setText("300");

        jComboBoxPosition.setEditable(true);
        jComboBoxPosition.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jComboBoxPosition.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxPosition.setAutoscrolls(true);
        jComboBoxPosition.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxPositionActionPerformed(evt);
            }
        });

        jLabel12.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel12.setForeground(new java.awt.Color(223, 249, 251));
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        jLabel12.setText("Position\t");

        jButtonTeamManagement.setBackground(new java.awt.Color(255, 255, 51));
        jButtonTeamManagement.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jButtonTeamManagement.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/alone.png"))); // NOI18N
        jButtonTeamManagement.setText("Join Team");
        jButtonTeamManagement.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonTeamManagementActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 648, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel8, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(6, 6, 6)
                                .addComponent(jComboBoxDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel12, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(9, 9, 9)
                                .addComponent(jComboBoxPosition, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jButtonTeamManagement, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(27, 27, 27)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(20, 20, 20)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jButtonClose, javax.swing.GroupLayout.PREFERRED_SIZE, 604, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addGap(6, 6, 6)
                                .addComponent(jTextFieldEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, 252, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(30, 30, 30)
                                .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.PREFERRED_SIZE, 151, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(6, 6, 6)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jTextFieldPhone, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jTextFieldBasicSalary, javax.swing.GroupLayout.PREFERRED_SIZE, 280, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addGap(27, 27, 27)
                                .addComponent(jButtonView, javax.swing.GroupLayout.PREFERRED_SIZE, 140, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addGap(2, 2, 2))
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
                        .addGap(13, 13, 13)
                        .addComponent(jLabel4))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jTextFieldEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jButtonAdd, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(1, 1, 1)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(3, 3, 3)
                        .addComponent(jLabel6)
                        .addGap(24, 24, 24)
                        .addComponent(jLabel9))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jTextFieldPhone, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jTextFieldBasicSalary, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(17, 17, 17)
                        .addComponent(jButtonView, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(6, 6, 6)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(15, 15, 15)
                                .addComponent(jLabel8))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(12, 12, 12)
                                .addComponent(jComboBoxDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel12)
                            .addComponent(jComboBoxPosition, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jButtonTeamManagement))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(jButtonEdit, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jButtonDelete, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addGap(449, 449, 449))
        );

        jTextFieldSearchEmployeeName.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldSearchEmployeeName.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jTextFieldSearchEmployeeNameActionPerformed(evt);
            }
        });
        jTextFieldSearchEmployeeName.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyPressed(java.awt.event.KeyEvent evt) {
                jTextFieldSearchEmployeeNameKeyPressed(evt);
            }
        });

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setText("Search Employee Name");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel10.setText("Sort By Department");

        jComboBoxSortByDepartment.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jComboBoxSortByDepartment.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        jComboBoxSortByDepartment.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jComboBoxSortByDepartmentActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 1399, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel10)
                            .addComponent(jLabel2))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jComboBoxSortByDepartment, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jTextFieldSearchEmployeeName))
                        .addContainerGap())))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldSearchEmployeeName, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jComboBoxSortByDepartment, javax.swing.GroupLayout.PREFERRED_SIZE, 44, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 858, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18))
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, 1000, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jTextFieldSearchEmployeeNameActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jTextFieldSearchEmployeeNameActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jTextFieldSearchEmployeeNameActionPerformed

    private void jTextFieldSearchEmployeeNameKeyPressed(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_jTextFieldSearchEmployeeNameKeyPressed
        if (evt.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER) {
            String searchTerm = jTextFieldSearchEmployeeName.getText();
            if (searchTerm == null || searchTerm.trim().isEmpty()) {
                searchTerm = null;
            }
            loadData(null, null, searchTerm);
        }
    }//GEN-LAST:event_jTextFieldSearchEmployeeNameKeyPressed

    private void jComboBoxDepartmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxDepartmentActionPerformed

    }//GEN-LAST:event_jComboBoxDepartmentActionPerformed

    private void jButtonCloseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonCloseActionPerformed
        setVisible(false);
    }//GEN-LAST:event_jButtonCloseActionPerformed

    private void jButtonViewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonViewActionPerformed
        allActionPerformed(evt, "VIEW");
    }//GEN-LAST:event_jButtonViewActionPerformed

    private void jButtonDeleteActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDeleteActionPerformed
        allActionPerformed(evt, "DELETE");
    }//GEN-LAST:event_jButtonDeleteActionPerformed

    private void jButtonEditActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEditActionPerformed
        allActionPerformed(evt, "EDIT");
    }//GEN-LAST:event_jButtonEditActionPerformed

    private void jButtonAddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAddActionPerformed
        allActionPerformed(evt, "ADD");
    }//GEN-LAST:event_jButtonAddActionPerformed

    private void jButtonClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonClearActionPerformed
        clearAllFields();
    }//GEN-LAST:event_jButtonClearActionPerformed

    private void jComboBoxPositionActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxPositionActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_jComboBoxPositionActionPerformed

    private void jComboBoxSortByDepartmentActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jComboBoxSortByDepartmentActionPerformed
        Object selectedItem = jComboBoxSortByDepartment.getSelectedItem();

        // Check if a valid department is selected (non-null)
        String selectedDepartment = (selectedItem != null) ? selectedItem.toString() : null;

        performSearchAndSort();
    }//GEN-LAST:event_jComboBoxSortByDepartmentActionPerformed

    private void jButtonTeamManagementActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonTeamManagementActionPerformed
        new TeamManagement((JFrame) SwingUtilities.getWindowAncestor((Component) evt.getSource())).setVisible(true);
    }//GEN-LAST:event_jButtonTeamManagementActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Employees().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAdd;
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonClose;
    private javax.swing.JButton jButtonDelete;
    private javax.swing.JButton jButtonEdit;
    private javax.swing.JButton jButtonTeamManagement;
    private javax.swing.JButton jButtonView;
    private javax.swing.JComboBox<String> jComboBoxDepartment;
    private javax.swing.JComboBox<String> jComboBoxPosition;
    private javax.swing.JComboBox<String> jComboBoxSortByDepartment;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable jTableEmployees;
    private javax.swing.JTextField jTextFieldBasicSalary;
    private javax.swing.JTextField jTextFieldEmployeeName;
    private javax.swing.JTextField jTextFieldPhone;
    private javax.swing.JTextField jTextFieldSearchEmployeeName;
    // End of variables declaration//GEN-END:variables
}
