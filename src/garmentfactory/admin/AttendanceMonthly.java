package garmentfactory.admin;

import javax.swing.*;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import javax.swing.table.DefaultTableModel;

public class AttendanceMonthly extends JFrame {

    private static final String URL = "jdbc:mysql://localhost:3306/garmentfactory";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    public AttendanceMonthly() {
        setTitle("Attendance Monthly");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1880, 1000);
        setLocationRelativeTo(null);
        setUndecorated(true);

        AttendanceMonthlyPanel attendanceMonthlyPanel = new AttendanceMonthlyPanel();
        add(attendanceMonthlyPanel);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new AttendanceMonthly().setVisible(true));
    }

    private static class AttendanceMonthlyPanel extends JPanel {

        private JTextField searchField;
        private JCheckBox lateTimeFilter;
        private JCheckBox overtimeFilter;
        private JCheckBox workTimeFilter;
        private JCheckBox leavesFilter;
        private JTable attendanceTable;
        private JLabel monthLabel;

        public AttendanceMonthlyPanel() {
            initComponents();
        }

        private void initComponents() {
            setLayout(new BorderLayout());
            Font font = new Font("Arial", Font.PLAIN, 20);

            add(createTopPanel(font), BorderLayout.NORTH);
            add(createSearchPanel(font), BorderLayout.CENTER);
            loadMonthlyAttendanceData();
        }

        private JPanel createTopPanel(Font font) {
            JPanel topPanel = new JPanel(new BorderLayout());

            JButton dailyButton = createButton("Back To Daily", font, new Color(255, 215, 71), Color.BLACK);
            dailyButton.setPreferredSize(new Dimension(250, 60));
            dailyButton.addActionListener(e -> backToDaily());
            topPanel.add(dailyButton, BorderLayout.WEST);

            LocalDate currentDate = LocalDate.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM-yyyy");
            monthLabel = new JLabel("Month: " + currentDate.format(formatter), JLabel.CENTER);
            monthLabel.setFont(font);
            topPanel.add(monthLabel, BorderLayout.CENTER);

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
            searchButton.addActionListener(e -> searchMonthlyAttendance());
            gbc.gridx = 2;
            searchPanel.add(searchButton, gbc);

            gbc.gridx = 3;
            searchPanel.add(createLabel("Filter:", font), gbc);

            JPanel filtersPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
            lateTimeFilter = createFilterCheckbox("Late Time", font, filtersPanel);
            overtimeFilter = createFilterCheckbox("Overtime", font, filtersPanel);
            workTimeFilter = createFilterCheckbox("Work Time", font, filtersPanel);
            leavesFilter = createFilterCheckbox("Leaves", font, filtersPanel);

            gbc.gridx = 4;
            searchPanel.add(filtersPanel, gbc);

            String[] columns = {"Employee Name", "Position", "Total Late Time", "Total Overtime", "Total Work Time", "Total Leaves"};
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
                searchMonthlyAttendance();
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
            if (selectedCheckBox != leavesFilter) {
                leavesFilter.setSelected(false);
            }
        }

        private void loadMonthlyAttendanceData() {
            loadAttendanceData(null);
        }

        private void searchMonthlyAttendance() {
            loadAttendanceData(searchField.getText());
        }

        private void loadAttendanceData(String searchQuery) {
            DefaultTableModel model = (DefaultTableModel) attendanceTable.getModel();
            model.setRowCount(0);

            LocalDate currentDate = LocalDate.now();
            LocalDate firstDayOfMonth = currentDate.withDayOfMonth(1);
            LocalDate lastDayOfMonth = currentDate.withDayOfMonth(currentDate.lengthOfMonth());

            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("SELECT e.employee_name, e.position, ")
                    .append("SUM(a.late_time) AS total_late_time, ")
                    .append("SUM(a.overtime) AS total_overtime, ")
                    .append("SUM(a.work_time) AS total_work_time, ")
                    .append("(SELECT COUNT(*) FROM leaves l WHERE l.employee_id = e.employee_id AND ")
                    .append("l.start_date BETWEEN ? AND ?) AS total_leaves ")
                    .append("FROM attendance a JOIN employees e ON a.employee_id = e.employee_id ")
                    .append("WHERE a.date BETWEEN ? AND ? ");

            if (searchQuery != null && !searchQuery.isEmpty()) {
                queryBuilder.append("AND e.employee_name LIKE ? ");
            }

            queryBuilder.append("GROUP BY e.employee_name, e.position");

            if (lateTimeFilter.isSelected()) {
                queryBuilder.append(" ORDER BY total_late_time DESC");
            } else if (overtimeFilter.isSelected()) {
                queryBuilder.append(" ORDER BY total_overtime DESC");
            } else if (workTimeFilter.isSelected()) {
                queryBuilder.append(" ORDER BY total_work_time DESC");
            } else if (leavesFilter.isSelected()) {
                queryBuilder.append(" ORDER BY total_leaves DESC");
            }

            try (Connection conn = DriverManager.getConnection(URL, USER, PASSWORD); PreparedStatement stmt = conn.prepareStatement(queryBuilder.toString())) {

                stmt.setDate(1, Date.valueOf(firstDayOfMonth));
                stmt.setDate(2, Date.valueOf(lastDayOfMonth));
                stmt.setDate(3, Date.valueOf(firstDayOfMonth));
                stmt.setDate(4, Date.valueOf(lastDayOfMonth));

                if (searchQuery != null && !searchQuery.isEmpty()) {
                    stmt.setString(5, "%" + searchQuery + "%");
                }

                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        model.addRow(new Object[]{
                            rs.getString("employee_name"),
                            rs.getString("position"),
                            rs.getObject("total_late_time"),
                            rs.getObject("total_overtime"),
                            rs.getObject("total_work_time"),
                            rs.getInt("total_leaves")
                        });
                    }
                }
            } catch (SQLException e) {
                JOptionPane.showMessageDialog(this, "Connection unsuccessful! " + e.getMessage(), "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }

        private void backToDaily() {
               AttendanceDaily monthlyFrame = new AttendanceDaily();
            monthlyFrame.setVisible(true);
            ((JFrame) SwingUtilities.getWindowAncestor(this)).dispose();
        }
    }
}