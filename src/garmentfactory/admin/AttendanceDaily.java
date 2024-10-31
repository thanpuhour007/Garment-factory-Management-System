package garmentfactory.admin;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import javax.swing.table.DefaultTableModel;

public class AttendanceDaily extends JFrame {

    private static final String URL = "jdbc:mysql://localhost:3306/garmentfactory";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public AttendanceDaily() {
        setTitle("Attendance Daily");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1880, 1000);
        setLocationRelativeTo(null);
        setUndecorated(true);

        AttendancePanel attendancePanel = new AttendancePanel();
        add(attendancePanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceDaily().setVisible(true));
    }

    private static class AttendancePanel extends JPanel {

        private JTextField searchField;
        private JCheckBox lateTimeFilter;
        private JCheckBox overtimeFilter;
        private JCheckBox workTimeFilter;
        private JTable attendanceTable;

        public AttendancePanel() {
            initComponents();
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            Font font = new Font("Arial", Font.PLAIN, 20);

            add(createTopPanel(font), BorderLayout.NORTH);
            add(createSearchPanel(font), BorderLayout.CENTER);
            loadAttendanceData();
        }

        private JPanel createTopPanel(Font font) {
            JPanel topPanel = new JPanel(new BorderLayout());
            JButton homeButton = createButton("Back To Home", font, new Color(235, 77, 75), Color.WHITE);
            homeButton.setPreferredSize(new Dimension(250, 60));
            homeButton.addActionListener(e -> navigateHome());
            topPanel.add(homeButton, BorderLayout.WEST);

            JButton viewMonthlyButton = createButton("View Monthly", font, new Color(111, 71, 255), Color.WHITE);
            viewMonthlyButton.setPreferredSize(new Dimension(250, 60));
            viewMonthlyButton.addActionListener(e -> viewMonthlyAttendance());
            topPanel.add(viewMonthlyButton, BorderLayout.EAST);

            return topPanel;
        }

        private JSplitPane createSearchPanel(Font font) {
            JPanel searchPanel = new JPanel(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = new Insets(5, 5, 5, 5);

            gbc.gridx = 0;
            gbc.gridy = 0;
            searchPanel.add(createLabel("Search:", font), gbc);

            searchField = new JTextField(20);
            searchField.setFont(font);
            gbc.gridx = 1;
            searchPanel.add(searchField, gbc);

            JButton searchButton = createButton("Search", font, null, null);
            searchButton.addActionListener(e -> searchAttendance());
            gbc.gridx = 2;
            searchPanel.add(searchButton, gbc);

            gbc.gridx = 3;
            searchPanel.add(createLabel("Filter:", font), gbc);

            JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            lateTimeFilter = createFilterCheckbox("Late Time", font, filtersPanel);
            overtimeFilter = createFilterCheckbox("Overtime", font, filtersPanel);
            workTimeFilter = createFilterCheckbox("Work Time", font, filtersPanel);

            gbc.gridx = 4;
            searchPanel.add(filtersPanel, gbc);

            String[] columns = {"Employee Name", "Position", "Date", "Check In", "Check Out", "Late Time", "Overtime", "Work Time"};
            DefaultTableModel tableModel = new DefaultTableModel(columns, 0);
            attendanceTable = new JTable(tableModel);
            attendanceTable.setFont(font);
            attendanceTable.setRowHeight(25);
            attendanceTable.getTableHeader().setBackground(new Color(204, 255, 204));

            JScrollPane scrollPane = new JScrollPane(attendanceTable);

            JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, searchPanel, scrollPane);
            splitPane.setResizeWeight(0.1);
            splitPane.setContinuousLayout(true);

            return splitPane;
        }

        private JButton createButton(String text, Font font, Color background, Color foreground) {
            JButton button = new JButton(text);
            button.setFont(font);
            if (background != null) {
                button.setBackground(background);
            }
            if (foreground != null) {
                button.setForeground(foreground);
            }
            return button;
        }

        private JLabel createLabel(String text, Font font) {
            JLabel label = new JLabel(text);
            label.setFont(font);
            return label;
        }

        private JCheckBox createFilterCheckbox(String text, Font font, JPanel panel) {
            JCheckBox checkBox = new JCheckBox(text);
            checkBox.setFont(font);
            checkBox.setBackground(Color.GREEN);
            checkBox.addActionListener(e -> {
                uncheckOtherFilters(checkBox);
                searchAttendance();
            });
            panel.add(checkBox);
            return checkBox;
        }

        private void uncheckOtherFilters(JCheckBox selectedCheckBox) {
            if (selectedCheckBox != lateTimeFilter) {
                lateTimeFilter.setSelected(false);
            }
            if (selectedCheckBox != overtimeFilter) {
                overtimeFilter.setSelected(false);
            }
            if (selectedCheckBox != workTimeFilter) {
                workTimeFilter.setSelected(false);
            }
        }

        private void loadAttendanceData() {
            executeQuery("SELECT e.employee_name, e.position, a.date, a.check_in, a.check_out, a.late_time, a.overtime, a.work_time "
                    + "FROM attendance a JOIN employees e ON a.employee_id = e.employee_id WHERE a.date = ?", "%");
        }

        private void searchAttendance() {
            String query = "SELECT e.employee_name, e.position, a.date, a.check_in, a.check_out, a.late_time, a.overtime, a.work_time "
                    + "FROM attendance a JOIN employees e ON a.employee_id = e.employee_id WHERE a.date = ? AND e.employee_name LIKE ?";

            if (lateTimeFilter.isSelected()) {
                query += " ORDER BY a.late_time DESC";
            } else if (overtimeFilter.isSelected()) {
                query += " ORDER BY a.overtime DESC";
            } else if (workTimeFilter.isSelected()) {
                query += " ORDER BY a.work_time DESC";
            }

            executeQuery(query, "%" + searchField.getText() + "%");
        }

        private void executeQuery(String query, String searchText) {
            DefaultTableModel model = (DefaultTableModel) attendanceTable.getModel();
            model.setRowCount(0);

            LocalDate currentDate = LocalDate.now();

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD); PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setDate(1, Date.valueOf(currentDate));
                if (searchText != null && !searchText.equals("%")) {
                    stmt.setString(2, searchText);
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("employee_name"),
                            rs.getString("position"),
                            rs.getDate("date"),
                            rs.getTime("check_in"),
                            rs.getTime("check_out"),
                            rs.getObject("late_time"),
                            rs.getObject("overtime"),
                            rs.getObject("work_time")
                        });
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Connection unsuccessful! " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void navigateHome() {
            setVisible(false);
            ((JFrame) SwingUtilities.getWindowAncestor(this)).dispose();

        }

        private void viewMonthlyAttendance() {
            AttendanceMonthly monthlyFrame = new AttendanceMonthly();
            monthlyFrame.setVisible(true);
            ((JFrame) SwingUtilities.getWindowAncestor(this)).dispose();
        }
    }
}
