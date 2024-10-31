/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package garmentfactory.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.sql.*;
import java.util.Vector;

public class Users extends JFrame {

    private JTextField txtUsername, txtPassword, txtSearchEmployee;
    private JComboBox<String> comboRole, comboEmployee;
    private JTable userTable;
    private DefaultTableModel tableModel;
    private JButton btnAdd, btnUpdate, btnDelete, btnClear;

    public Users() {
        initComponents();
        loadData();
        populateEmployeeComboBox("");
    }

    private void initComponents() {
        setTitle("User Management");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setUndecorated(true);
        setLocationRelativeTo(null);

        setLayout(new BorderLayout());

        // Top Panel with Back Button and Title
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));  // Adds padding

        // Back Button (on the left)
        JButton btnBack = new JButton("Back");
        btnBack.setFont(new Font("Arial", Font.PLAIN, 20));

        // Set button width and height
        btnBack.setPreferredSize(new Dimension(300, 50));  // Width = 150, Height = 50

        // Set background and text color
        btnBack.setBackground(Color.RED);  // Background color
        btnBack.setForeground(Color.WHITE);  // Text color

        btnBack.addActionListener(e -> handleBack());  // Back button action

        topPanel.add(btnBack, BorderLayout.WEST);  // Add button to the left side of topPanel

        // Top Label for the Title (centered)
        JLabel titleLabel = new JLabel("User Management", JLabel.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 30));  // Larger font for title
        topPanel.add(titleLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);

        // Left Panel (Insert fields + Buttons) combined into one panel
        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BorderLayout());

        // Panel for Insert Fields (Username, Password, Role, Search Employee, Employee)
        JPanel fieldsPanel = new JPanel(new GridLayout(7, 1, 10, 10));
        fieldsPanel.setBorder(BorderFactory.createTitledBorder("Insert Fields"));

        Font labelFont = new Font("Arial", Font.PLAIN, 18);  // Font for labels
        Font textFieldFont = new Font("Arial", Font.PLAIN, 18);  // Font for text fields

        // Username
        JLabel lblUsername = new JLabel("Username:");
        lblUsername.setFont(labelFont);
        txtUsername = new JTextField();
        txtUsername.setFont(textFieldFont);
        fieldsPanel.add(lblUsername);
        fieldsPanel.add(txtUsername);

        // Password
        JLabel lblPassword = new JLabel("Password:");
        lblPassword.setFont(labelFont);
        txtPassword = new JTextField();
        txtPassword.setFont(textFieldFont);
        fieldsPanel.add(lblPassword);
        fieldsPanel.add(txtPassword);

        // Role
        JLabel lblRole = new JLabel("Role:");
        lblRole.setFont(labelFont);
        comboRole = new JComboBox<>();
        comboRole.setFont(textFieldFont);
        fieldsPanel.add(lblRole);
        fieldsPanel.add(comboRole);

        comboRole.addItem("Admin");
        comboRole.addItem("HR");
        comboRole.addItem("TeamLeader");

        // Search Employee
        JLabel lblSearchEmployee = new JLabel("Search Employee:");
        lblSearchEmployee.setFont(labelFont);
        txtSearchEmployee = new JTextField();
        txtSearchEmployee.setFont(textFieldFont);
        fieldsPanel.add(lblSearchEmployee);
        fieldsPanel.add(txtSearchEmployee);

        txtSearchEmployee.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                String searchText = txtSearchEmployee.getText().trim();
                populateEmployeeComboBox(searchText); // Search employees by name
            }
        });

        // Employee
        JLabel lblEmployee = new JLabel("Employee:");
        lblEmployee.setFont(labelFont);
        comboEmployee = new JComboBox<>();
        comboEmployee.setFont(textFieldFont);
        fieldsPanel.add(lblEmployee);
        fieldsPanel.add(comboEmployee);

        // Panel for Buttons (Add, Update, Delete, Clear)
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 20));
        buttonsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));

        Font buttonFont = new Font("Arial", Font.PLAIN, 18);  // Font for buttons

        btnAdd = new JButton("Add");
        btnAdd.setFont(buttonFont);
        btnAdd.addActionListener(this::handleAdd);
        buttonsPanel.add(btnAdd);

        btnUpdate = new JButton("Update");
        btnUpdate.setFont(buttonFont);
        btnUpdate.addActionListener(this::handleUpdate);
        buttonsPanel.add(btnUpdate);

        btnDelete = new JButton("Delete");
        btnDelete.setFont(buttonFont);
        btnDelete.addActionListener(this::handleDelete);
        buttonsPanel.add(btnDelete);

        btnClear = new JButton("Clear");
        btnClear.setFont(buttonFont);
        btnClear.addActionListener(this::handleClear);
        buttonsPanel.add(btnClear);

        // Create a JSplitPane between fieldsPanel and buttonsPanel
        JSplitPane fieldsButtonsSplitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, fieldsPanel, buttonsPanel);
        fieldsButtonsSplitPane.setDividerLocation(350);  // Adjust divider location based on your preference

        // Add the JSplitPane to the leftPanel
        leftPanel.add(fieldsButtonsSplitPane, BorderLayout.CENTER);

        // Right Panel (Table to Display Users)
        JPanel rightPanel = new JPanel();
        rightPanel.setLayout(new BorderLayout());
        rightPanel.setBorder(BorderFactory.createTitledBorder("User Table"));

        tableModel = new DefaultTableModel(new String[]{"Username", "Password", "Role", "Employee"}, 0);
        userTable = new JTable(tableModel);
        userTable.setFont(new Font("Arial", Font.PLAIN, 18));  // Larger font for table
        userTable.setRowHeight(30);  // Increase row height for better readability
        JScrollPane tableScrollPane = new JScrollPane(userTable);
        rightPanel.add(tableScrollPane, BorderLayout.CENTER);

        // Add table selection listener to get the selected row and populate fields
        userTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting()) {
                int selectedRow = userTable.getSelectedRow();
                if (selectedRow != -1) {
                    populateFieldsFromSelectedRow(selectedRow);
                }
            }
        });

        // Split Pane to divide left and right panels
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftPanel, rightPanel);
        splitPane.setDividerLocation(500);  // Adjust the divider location for appropriate layout
        add(splitPane, BorderLayout.CENTER);

        // Set frame size and visibility
        setSize(1880, 1000);
        setLocationRelativeTo(null);
        setVisible(true);
    }

    private void handleBack() {
        setVisible(false);

    }

    // Populate employee combo box with an optional search filter
    private void populateEmployeeComboBox(String searchTerm) {
        comboEmployee.removeAllItems();
        try (Connection con = getConnection()) {
            String query = "SELECT employee_id, employee_name FROM employees WHERE employee_name LIKE ?";
            PreparedStatement ps = con.prepareStatement(query);
            ps.setString(1, "%" + searchTerm + "%");
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                comboEmployee.addItem(rs.getInt("employee_id") + " - " + rs.getString("employee_name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading employees: " + e.getMessage());
        }
    }

    // Function to validate all fields
    private boolean validateFields() {
        if (txtUsername.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Username is required.");
            return false;
        }

        if (txtPassword.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Password is required.");
            return false;
        }

        if (comboRole.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select a role.");
            return false;
        }

        if (comboEmployee.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select an employee.");
            return false;
        }

        return true;  // All fields are valid
    }

    private void populateRoleComboBox() {
        comboRole.removeAllItems();  // Clear existing items
        try (Connection con = getConnection()) {
            String query = "SELECT role_name FROM users";  // Assuming roles table exists
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                comboRole.addItem(rs.getString("role_name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading roles: " + e.getMessage());
        }
    }

// Function to populate fields when a row is selected
    private void populateFieldsFromSelectedRow(int selectedRow) {
        String username = (String) tableModel.getValueAt(selectedRow, 0);
        String password = (String) tableModel.getValueAt(selectedRow, 1);
        String role = (String) tableModel.getValueAt(selectedRow, 2);
        String employee = (String) tableModel.getValueAt(selectedRow, 3);

        // Set the fields
        txtUsername.setText(username);
        txtPassword.setText(password);
        comboRole.setSelectedItem(role);
        comboEmployee.setSelectedItem(employee);
    }

// Load data from the database and populate the table
    private void loadData() {
        tableModel.setRowCount(0);
        try (Connection con = getConnection()) {
            String query = "SELECT u.username, u.password, u.role_name, e.employee_name FROM users u "
                    + "JOIN employees e ON u.employee_id = e.employee_id";
            PreparedStatement ps = con.prepareStatement(query);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                Vector<String> row = new Vector<>();
                row.add(rs.getString("username"));
                row.add(rs.getString("password"));  // Include password in the table
                row.add(rs.getString("role_name"));
                row.add(rs.getString("employee_name"));
                tableModel.addRow(row);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading data: " + e.getMessage());
        }
    }

// Function to handle Add operation
    private void handleAdd(ActionEvent e) {
        if (!validateFields()) {
            return;
        }

        try (Connection con = getConnection()) {
            String query = "INSERT INTO users (employee_id, username, password, role_name) VALUES (?, ?, ?, ?)";
            PreparedStatement ps = con.prepareStatement(query);

            int employeeId = getSelectedEmployeeId();
            ps.setInt(1, employeeId);
            ps.setString(2, txtUsername.getText());
            ps.setString(3, txtPassword.getText());
            ps.setString(4, comboRole.getSelectedItem().toString());

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "User created successfully.");
            loadData();
            clearFields();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error creating user: " + ex.getMessage());
        }
    }

// Function to handle Update operation
    private void handleUpdate(ActionEvent e) {
        if (!validateFields()) {
            return;
        }

        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to update.");
            return;
        }

        String originalUsername = (String) tableModel.getValueAt(selectedRow, 0);  // Get the original username

        try (Connection con = getConnection()) {
            String query = "UPDATE users SET employee_id = ?, username = ?, password = ?, role_name = ? WHERE username = ?";
            PreparedStatement ps = con.prepareStatement(query);

            int employeeId = getSelectedEmployeeId();
            ps.setInt(1, employeeId);
            ps.setString(2, txtUsername.getText());
            ps.setString(3, txtPassword.getText());
            ps.setString(4, comboRole.getSelectedItem().toString());
            ps.setString(5, originalUsername);  // Use the original username to identify the row

            ps.executeUpdate();
            JOptionPane.showMessageDialog(this, "User updated successfully.");
            loadData();
            clearFields();
        } catch (SQLException ex) {
            JOptionPane.showMessageDialog(this, "Error updating user: " + ex.getMessage());
        }
    }

// Function to handle Delete operation
    private void handleDelete(ActionEvent e) {
        int selectedRow = userTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a user to delete.");
            return;
        }

        String username = (String) tableModel.getValueAt(selectedRow, 0);  // Get the username from the selected row

        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this user?", "Delete User", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            try (Connection con = getConnection()) {
                String query = "DELETE FROM users WHERE username = ?";
                PreparedStatement ps = con.prepareStatement(query);
                ps.setString(1, username);

                ps.executeUpdate();
                JOptionPane.showMessageDialog(this, "User deleted successfully.");
                loadData();
                clearFields();
            } catch (SQLException ex) {
                JOptionPane.showMessageDialog(this, "Error deleting user: " + ex.getMessage());
            }
        }
    }

    private void handleClear(ActionEvent e) {
        clearFields();
    }

    private void clearFields() {
        txtUsername.setText("");
        txtPassword.setText("");
        txtSearchEmployee.setText("");
        comboRole.setSelectedIndex(0);
        comboEmployee.setSelectedIndex(0);
        loadData();
    }

    private int getSelectedEmployeeId() {
        String selectedItem = (String) comboEmployee.getSelectedItem();
        return Integer.parseInt(selectedItem.split(" - ")[0]);
    }

    // Helper method to get database connection
    private Connection getConnection() throws SQLException {
        // Adjust this to your actual database connection settings
        String url = "jdbc:mysql://localhost:3306/garmentfactory";
        String user = "root";
        String password = "";
        return DriverManager.getConnection(url, user, password);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new Users().setVisible(true));
    }
}
