package garmentfactory.admin;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.*;

public class TeamManagement extends JDialog {

    private JLabel employeeNameLabel;
    private JLabel searchNameLabel;
    private JLabel seletedLabel;
    private JLabel searchNameemployeeLabel;

    private JComboBox<String> teamLeaderDropdown;
    private JButton joinTeamButton;
    private JTextField searchEmployeeField;
    private JTextField searchTeamLeaderField;
    private JTable employeeTable;
    private DefaultTableModel employeeTableModel;
    private Connection conn;

    public TeamManagement(JFrame parent) {
        super(parent, "Team Management", true);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        setSize(900, 650);
        setLocationRelativeTo(parent);

        initializeConnection();

        JSplitPane splitPane = new JSplitPane();
        splitPane.setDividerLocation(350);

        // Left Panel
        JPanel leftPanel = new JPanel(new GridBagLayout());
        leftPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Team Leader Management", 0, 0, new Font("Arial", Font.BOLD, 14)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy = 0;

        employeeNameLabel = new JLabel("Selected Employee: ");
        employeeNameLabel.setFont(new Font("Arial", Font.PLAIN, 20));
        leftPanel.add(employeeNameLabel, gbc);

        gbc.gridy++;
        searchNameLabel = new JLabel("Search Team Leader Name: ");
        searchNameLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        leftPanel.add(searchNameLabel, gbc);

        gbc.gridy++;

        searchTeamLeaderField = new JTextField(15);
        searchTeamLeaderField.setToolTipText("Search Team Leader Name");
        searchTeamLeaderField.setFont(new Font("Arial", Font.PLAIN, 20));
        searchTeamLeaderField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                filterTeamLeaders(searchTeamLeaderField.getText().trim());
            }
        });
        leftPanel.add(searchTeamLeaderField, gbc);
        gbc.gridy++;
        seletedLabel = new JLabel("Seleted Team Leader Name: ");
        seletedLabel.setFont(new Font("Arial", Font.PLAIN, 14));
        leftPanel.add(seletedLabel, gbc);
        gbc.gridy++;
        teamLeaderDropdown = new JComboBox<>();
        teamLeaderDropdown.setFont(new Font("Arial", Font.PLAIN, 20));
        loadTeamLeaders();
        leftPanel.add(teamLeaderDropdown, gbc);

        gbc.gridy++;
        joinTeamButton = new JButton("Join Team");
        joinTeamButton.setFont(new Font("Arial", Font.BOLD, 30));
        joinTeamButton.setBackground(new Color(173, 216, 230)); // Light blue background color
        leftPanel.add(joinTeamButton, gbc);
        joinTeamButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                joinTeam();
            }
        });

        // Right Panel
        JPanel rightPanel = new JPanel(new BorderLayout(10, 10));
        rightPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Search Employee Name", 0, 0, new Font("Arial", Font.BOLD, 14)));
        searchEmployeeField = new JTextField();
        searchEmployeeField.setToolTipText("Search Employee Name");
        searchEmployeeField.setFont(new Font("Arial", Font.PLAIN, 20));
        searchEmployeeField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                searchEmployees();
            }
        });
        rightPanel.add(searchEmployeeField, BorderLayout.NORTH);

        employeeTableModel = new DefaultTableModel();
        employeeTableModel.addColumn("Employee Name");
        employeeTableModel.addColumn("Created At");
        employeeTable = new JTable(employeeTableModel);
        employeeTable.setFont(new Font("Arial", Font.PLAIN, 14));
        employeeTable.setRowHeight(25);
        employeeTable.getTableHeader().setFont(new Font("Arial", Font.BOLD, 20));
        employeeTable.getSelectionModel().addListSelectionListener(event -> {
            if (!event.getValueIsAdjusting() && employeeTable.getSelectedRow() != -1) {
                String employeeName = (String) employeeTableModel.getValueAt(employeeTable.getSelectedRow(), 0);
                employeeNameLabel.setText("Selected Employee: " + employeeName);
            }
        });
        rightPanel.add(new JScrollPane(employeeTable), BorderLayout.CENTER);

        loadEmployees();

        // Add panels to split pane
        splitPane.setLeftComponent(leftPanel);
        splitPane.setRightComponent(rightPanel);

        add(splitPane);
    }

    private void initializeConnection() {
        try {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/garmentfactory", "root", "");
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error establishing database connection", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void filterTeamLeaders(String searchText) {
        teamLeaderDropdown.removeAllItems();
        if (searchText.isEmpty()) {
            loadTeamLeaders();
            return;
        }
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT users.user_id, employees.employee_name FROM users JOIN employees ON users.employee_id = employees.employee_id WHERE users.role_name = 'TeamLeader' AND employees.employee_name LIKE ?")) {
            pstmt.setString(1, "%" + searchText + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                String employeeName = rs.getString("employee_name");
                teamLeaderDropdown.addItem(employeeName);
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error filtering team leaders", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadTeamLeaders() {
        teamLeaderDropdown.removeAllItems();
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT users.user_id, employees.employee_name FROM users JOIN employees ON users.employee_id = employees.employee_id WHERE users.role_name = 'TeamLeader'")) {
            while (rs.next()) {
                teamLeaderDropdown.addItem(rs.getString("employee_name"));
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading team leaders", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void joinTeam() {
    String selectedEmployee = employeeNameLabel.getText().replace("Selected Employee: ", "");
    String selectedTeamLeader = (String) teamLeaderDropdown.getSelectedItem();

    if (selectedEmployee.isEmpty() || selectedTeamLeader == null) {
        JOptionPane.showMessageDialog(this, "Please select an employee and a team leader", "Warning", JOptionPane.WARNING_MESSAGE);
        return;
    }

    try {
        // Assuming team name is retrieved from the selected team leader's name
        String teamName = selectedTeamLeader;  // This is the team leader's name or team name
        int teamLeaderId = getTeamLeaderIdByName(selectedTeamLeader); // Retrieve the team leader ID from name
        int employeeId = getEmployeeIdByName(selectedEmployee);

        try (PreparedStatement pstmt = conn.prepareStatement("UPDATE team_leader SET user_id = ? WHERE employee_id = ?")) {
            pstmt.setInt(1, teamLeaderId);
            pstmt.setInt(2, employeeId);

            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0) {
                // Custom message to display team name and employee name
                String message = String.format("<html><div style='font-size:20px;'>Employee successfully joined \"%s\" Team!<br>\"%s\" >>> joined \"%s\" Team.</div></html>", 
                                               teamName, selectedEmployee, teamName);

                // Create a JLabel with custom font size
                JLabel messageLabel = new JLabel(message);
                messageLabel.setFont(new Font("Arial", Font.PLAIN, 20));  // Set font and size

                // Show the message dialog with custom font size
                JOptionPane.showMessageDialog(this, messageLabel);
            } else {
                JOptionPane.showMessageDialog(this, "No matching employee found to update!", "Warning", JOptionPane.WARNING_MESSAGE);
            }
        }
    } catch (SQLException e) {
        JOptionPane.showMessageDialog(this, "Error joining team", "Error", JOptionPane.ERROR_MESSAGE);
    }
}


    private int getTeamLeaderIdByName(String teamLeaderName) throws SQLException {
        String query = "SELECT user_id FROM users WHERE username = ?";
        try (PreparedStatement ps = conn.prepareStatement(query)) {
            ps.setString(1, teamLeaderName);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("user_id");
                } else {
                    throw new SQLException("Team leader not found");
                }
            }
        }
    }

    private void loadEmployees() {
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT employee_name, created_at FROM employees WHERE employee_id IN (SELECT employee_id FROM team_leader WHERE user_id = 1)")) {
            while (rs.next()) {
                employeeTableModel.addRow(new Object[]{rs.getString("employee_name"), rs.getTimestamp("created_at")});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error loading employees", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void searchEmployees() {
        String searchText = searchEmployeeField.getText().trim();
        if (searchText.isEmpty()) {
            return;
        }

        employeeTableModel.setRowCount(0);
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT employee_name, created_at FROM employees WHERE employee_name LIKE ?")) {
            pstmt.setString(1, "%" + searchText + "%");
            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                employeeTableModel.addRow(new Object[]{rs.getString("employee_name"), rs.getTimestamp("created_at")});
            }
        } catch (SQLException e) {
            JOptionPane.showMessageDialog(this, "Error searching employees", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private int getEmployeeIdByName(String employeeName) throws SQLException {
        try (PreparedStatement pstmt = conn.prepareStatement("SELECT employee_id FROM employees WHERE employee_name = ?")) {
            pstmt.setString(1, employeeName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("employee_id");
            } else {
                throw new SQLException("Employee not found");
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame parentFrame = new JFrame();
            TeamManagement dialog = new TeamManagement(parentFrame);
            dialog.setVisible(true);
        });
    }
}
