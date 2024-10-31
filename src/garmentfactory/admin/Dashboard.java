/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/GUIForms/JFrame.java to edit this template
 */
package garmentfactory.admin;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import DatabaseConnection.config;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.sql.PreparedStatement;
import javax.swing.SwingUtilities;
import javax.swing.border.AbstractBorder;
import javax.swing.Timer;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.time.LocalDate;
import java.time.YearMonth;
import javax.swing.JFrame;

public class Dashboard extends javax.swing.JFrame {

    private int userId;
    private String roleName;
    private Timer timer;

    public Dashboard(int userId, String roleName) {
        this.userId = userId;
        this.roleName = roleName;
        initComponents();
        setupRoleBasedUI();
        updateCounts();
        startRealTimeReload();
        startDisplayCurrentDateAndTime();
        getRootPane().setBorder(new DropShadowBorder(5, Color.GREEN));
        generateDailyReport();
        updateBlackListedEmployeeCount();
    }

    private void updateBlackListedEmployeeCount() {
        int totalBlackListedEmployees = countBlackListEmployees();
        jLabelEmployeeBlackLists.setText(String.valueOf(totalBlackListedEmployees));
    }

    public void generateDailyReport() {
        LocalDate today = LocalDate.now();
        String sql = """
            SELECT
                COUNT(DISTINCT CASE WHEN a.check_in IS NOT NULL THEN a.employee_id END) AS EmployeePresent,
                COUNT(DISTINCT CASE WHEN a.late_time > 0 THEN a.employee_id END) AS EmployeeLate,
                COUNT(DISTINCT CASE WHEN a.overtime > 0 THEN a.employee_id END) AS EmployeeOverTime,
                COUNT(DISTINCT CASE WHEN a.work_time > 0 THEN a.employee_id END) AS EmployeeWorkTime,
                (SELECT COUNT(DISTINCT e.employee_id) FROM attendance e WHERE e.date = ?) -
                COUNT(DISTINCT CASE WHEN a.check_in IS NOT NULL THEN a.employee_id END) AS EmployeeAbsent
            FROM attendance a WHERE a.date = ?
        """;

        try (Connection conn = config.getConnection(); PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDate(1, java.sql.Date.valueOf(today));
            stmt.setDate(2, java.sql.Date.valueOf(today));

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    // Update JLabels with the result values
                    jLabelEmployeePresent.setText(String.valueOf(rs.getInt("EmployeePresent")));
                    jLabelEmployeeLate.setText(String.valueOf(rs.getInt("EmployeeLate")));
                    jLabelEmployeeOverTime.setText(String.valueOf(rs.getInt("EmployeeOverTime")));
                    jLabelEmployeeWorkTime.setText(String.valueOf(rs.getInt("EmployeeWorkTime")));
                    jLabelEmployeeAbsent.setText(String.valueOf(rs.getInt("EmployeeAbsent")));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static int countBlackListEmployees() {
        YearMonth currentMonth = YearMonth.now();
        LocalDate firstDayOfMonth = currentMonth.atDay(1);
        LocalDate lastDayOfMonth = currentMonth.atEndOfMonth();

        String[] queries = {
            "SELECT employee_id, COUNT(*) AS leave_days FROM leaves WHERE start_date >= ? AND end_date <= ? GROUP BY employee_id HAVING leave_days > 3",
            "SELECT employee_id, COUNT(*) AS absent_days FROM attendance WHERE date >= ? AND date <= ? AND check_in IS NULL GROUP BY employee_id HAVING absent_days > 3",
            "SELECT employee_id, COUNT(*) AS issue_count FROM attendance WHERE date >= ? AND date <= ? AND (late_time > 0 OR work_time = 0) GROUP BY employee_id HAVING issue_count >= 4"
        };

        int totalBlackListedEmployees = 0;

        try (Connection conn = config.getConnection()) {
            for (String query : queries) {
                totalBlackListedEmployees += countBlackListedEmployeesFromQuery(conn, query, firstDayOfMonth, lastDayOfMonth);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return totalBlackListedEmployees;
    }

    private void setupRoleBasedUI() {
        switch (roleName) {
//            case "TeamLeader":
//                jPanelAttendanceView.setVisible(true);
//                jPanelAttendance.setVisible(false);
//                jPanelEmployees.setVisible(false);
//                jPanelLeaves.setVisible(false);
//                jPanelPayroll.setVisible(false);
//                jPanelUsers.setVisible(false);
//                jPanelDepartments.setVisible(false);
//                jPanelDailyReport.setVisible(false);
//
//                break;
            case "HR":
                jPanelAttendance.setVisible(true);
                jPanelEmployees.setVisible(true);
                jPanelLeaves.setVisible(true);
                jPanelPayroll.setVisible(true);
//                jPanelAttendanceView.setVisible(false);
                jPanelUsers.setVisible(false);
                jPanelDepartments.setVisible(false);
                break;
            case "Admin":
                jPanelAttendance.setVisible(true);
                jPanelEmployees.setVisible(true);
                jPanelLeaves.setVisible(true);
                jPanelPayroll.setVisible(true);
//                jPanelAttendanceView.setVisible(false);
                jPanelUsers.setVisible(true);
                jPanelDepartments.setVisible(true);
                break;
            default:
//                jPanelAttendanceView.setVisible(false);
                jPanelAttendance.setVisible(false);
                jPanelEmployees.setVisible(false);
                jPanelLeaves.setVisible(false);
                jPanelPayroll.setVisible(false);
                jPanelUsers.setVisible(false);
                jPanelDepartments.setVisible(false);
                jPanelDailyReport.setVisible(false);

                break;
        }
    }

    private void updateCounts() {
        jTextFieldCountAttendance.setText(getCountAttendanceDaily("attendance"));
        jTextFieldCountDepartments.setText(getCountFromTable("departments"));
        jTextFieldCountEmployees.setText(getCountFromTable("employees"));
        jTextFieldCountLeaves.setText(getCountFromTable("leaves"));
        jTextFieldCountUsers.setText(getCountFromTable("users"));
    }

    // Helper method to execute the queries and return the count of blacklisted employees
    private static int countBlackListedEmployeesFromQuery(Connection conn, String query, LocalDate startDate, LocalDate endDate) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setDate(1, java.sql.Date.valueOf(startDate));
            stmt.setDate(2, java.sql.Date.valueOf(endDate));
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                count++;
            }
            return count;
        }
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 32767));
        jLabel9 = new javax.swing.JLabel();
        jSplitPane2 = new javax.swing.JSplitPane();
        jPanel1 = new javax.swing.JPanel();
        jPanelDailyReport = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabelEmployeePresent = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jLabelEmployeeAbsent = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabelEmployeeLate = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jLabelEmployeeOverTime = new javax.swing.JLabel();
        jLabel17 = new javax.swing.JLabel();
        jLabelEmployeeWorkTime = new javax.swing.JLabel();
        jButtonPreviewBlackLists = new javax.swing.JButton();
        jLabel18 = new javax.swing.JLabel();
        jLabelEmployeeBlackLists = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabelDisplayCurrentDateAndTimer = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        jPanelDepartments = new javax.swing.JPanel();
        jButtonDepartments = new javax.swing.JButton();
        jLabel7 = new javax.swing.JLabel();
        jTextFieldCountDepartments = new javax.swing.JTextField();
        jPanelLeaves = new javax.swing.JPanel();
        jButtonLeave = new javax.swing.JButton();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldCountLeaves = new javax.swing.JTextField();
        jPanelPayroll = new javax.swing.JPanel();
        jButtonPayroll = new javax.swing.JButton();
        jPanelUsers = new javax.swing.JPanel();
        jButtonUsers = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        jTextFieldCountUsers = new javax.swing.JTextField();
        jPanelAttendance = new javax.swing.JPanel();
        jButtonAttendance = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        jTextFieldCountAttendance = new javax.swing.JTextField();
        jPanelEmployees = new javax.swing.JPanel();
        jButtonEmployees = new javax.swing.JButton();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldCountEmployees = new javax.swing.JTextField();
        jButtonLogout = new javax.swing.JButton();
        jPanel3 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        jLabelCross = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        setUndecorated(true);
        setPreferredSize(new java.awt.Dimension(1880, 1000));
        setResizable(false);
        setSize(new java.awt.Dimension(1880, 1000));

        jLabel9.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N

        jPanel1.setBackground(new java.awt.Color(204, 255, 204));

        jPanelDailyReport.setBackground(new java.awt.Color(204, 255, 255));

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel2.setForeground(new java.awt.Color(0, 102, 102));
        jLabel2.setText("Daily Report");

        jLabel8.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabel8.setForeground(new java.awt.Color(0, 0, 204));
        jLabel8.setText("Employee Present :");

        jLabelEmployeePresent.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabelEmployeePresent.setForeground(new java.awt.Color(0, 51, 51));
        jLabelEmployeePresent.setText("0");

        jLabel11.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabel11.setForeground(new java.awt.Color(0, 0, 204));
        jLabel11.setText("Employee Absent :");

        jLabelEmployeeAbsent.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabelEmployeeAbsent.setForeground(new java.awt.Color(0, 51, 51));
        jLabelEmployeeAbsent.setText("0");

        jLabel13.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabel13.setForeground(new java.awt.Color(0, 0, 204));
        jLabel13.setText("Employee Late :");

        jLabelEmployeeLate.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabelEmployeeLate.setForeground(new java.awt.Color(0, 51, 51));
        jLabelEmployeeLate.setText("0");

        jLabel15.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabel15.setForeground(new java.awt.Color(0, 0, 204));
        jLabel15.setText("Employee OverTime :");

        jLabelEmployeeOverTime.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabelEmployeeOverTime.setForeground(new java.awt.Color(0, 51, 51));
        jLabelEmployeeOverTime.setText("0");

        jLabel17.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabel17.setForeground(new java.awt.Color(0, 0, 204));
        jLabel17.setText("Employee WorkTime :");

        jLabelEmployeeWorkTime.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabelEmployeeWorkTime.setForeground(new java.awt.Color(0, 51, 51));
        jLabelEmployeeWorkTime.setText("0");

        jButtonPreviewBlackLists.setBackground(new java.awt.Color(51, 51, 51));
        jButtonPreviewBlackLists.setFont(new java.awt.Font("Segoe UI", 0, 18)); // NOI18N
        jButtonPreviewBlackLists.setForeground(new java.awt.Color(255, 255, 255));
        jButtonPreviewBlackLists.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/PreviewBlackLists.png"))); // NOI18N
        jButtonPreviewBlackLists.setText("Preview Black Lists");
        jButtonPreviewBlackLists.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPreviewBlackListsActionPerformed(evt);
            }
        });

        jLabel18.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N
        jLabel18.setForeground(new java.awt.Color(0, 0, 204));
        jLabel18.setText("Employee Black Lists :");

        jLabelEmployeeBlackLists.setBackground(new java.awt.Color(255, 204, 255));
        jLabelEmployeeBlackLists.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabelEmployeeBlackLists.setForeground(new java.awt.Color(255, 0, 0));
        jLabelEmployeeBlackLists.setText("0");

        jLabel10.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabel10.setForeground(new java.awt.Color(0, 102, 102));
        jLabel10.setText("Black Lists Report");

        jLabelDisplayCurrentDateAndTimer.setFont(new java.awt.Font("Segoe UI", 0, 36)); // NOI18N
        jLabelDisplayCurrentDateAndTimer.setText("Lording ....");

        javax.swing.GroupLayout jPanelDailyReportLayout = new javax.swing.GroupLayout(jPanelDailyReport);
        jPanelDailyReport.setLayout(jPanelDailyReportLayout);
        jPanelDailyReportLayout.setHorizontalGroup(
            jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                        .addGap(120, 120, 120)
                        .addComponent(jLabel18)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelEmployeeBlackLists)
                        .addGap(26, 26, 26)
                        .addComponent(jButtonPreviewBlackLists, javax.swing.GroupLayout.PREFERRED_SIZE, 251, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                        .addGap(6, 6, 6)
                        .addComponent(jLabel2)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelDisplayCurrentDateAndTimer, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel10))
                    .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                        .addGap(119, 119, 119)
                        .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addGap(36, 36, 36)
                                .addComponent(jLabelEmployeePresent))
                            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                                .addComponent(jLabel11)
                                .addGap(39, 39, 39)
                                .addComponent(jLabelEmployeeAbsent))
                            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                                .addComponent(jLabel13)
                                .addGap(64, 64, 64)
                                .addComponent(jLabelEmployeeLate))
                            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                                .addComponent(jLabel15)
                                .addGap(16, 16, 16)
                                .addComponent(jLabelEmployeeOverTime))
                            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                                .addComponent(jLabel17)
                                .addGap(10, 10, 10)
                                .addComponent(jLabelEmployeeWorkTime)))))
                .addContainerGap(533, Short.MAX_VALUE))
        );
        jPanelDailyReportLayout.setVerticalGroup(
            jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDailyReportLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabelDisplayCurrentDateAndTimer))
                .addGap(18, 18, 18)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel8)
                    .addComponent(jLabelEmployeePresent))
                .addGap(13, 13, 13)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel11)
                    .addComponent(jLabelEmployeeAbsent))
                .addGap(18, 18, 18)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel13)
                    .addComponent(jLabelEmployeeLate))
                .addGap(18, 18, 18)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel15)
                    .addComponent(jLabelEmployeeOverTime))
                .addGap(18, 18, 18)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel17)
                    .addComponent(jLabelEmployeeWorkTime))
                .addGap(18, 18, 18)
                .addComponent(jLabel10)
                .addGap(18, 18, 18)
                .addGroup(jPanelDailyReportLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel18)
                    .addComponent(jLabelEmployeeBlackLists)
                    .addComponent(jButtonPreviewBlackLists))
                .addContainerGap(491, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelDailyReport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanelDailyReport, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        jSplitPane2.setRightComponent(jPanel1);

        jPanel2.setBackground(new java.awt.Color(153, 255, 255));

        jLabel12.setFont(new java.awt.Font("Segoe UI", 0, 48)); // NOI18N
        jLabel12.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel12.setText("Quick Assets");
        jLabel12.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);

        jPanelDepartments.setPreferredSize(new java.awt.Dimension(400, 200));

        jButtonDepartments.setBackground(new java.awt.Color(204, 255, 204));
        jButtonDepartments.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonDepartments.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/departments.png"))); // NOI18N
        jButtonDepartments.setText("Departments ");
        jButtonDepartments.setBorder(null);
        jButtonDepartments.setPreferredSize(new java.awt.Dimension(1920, 1080));
        jButtonDepartments.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonDepartmentsActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel7.setText("Count");

        jTextFieldCountDepartments.setEditable(false);
        jTextFieldCountDepartments.setBackground(new java.awt.Color(204, 204, 204));
        jTextFieldCountDepartments.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldCountDepartments.setForeground(new java.awt.Color(0, 102, 51));

        javax.swing.GroupLayout jPanelDepartmentsLayout = new javax.swing.GroupLayout(jPanelDepartments);
        jPanelDepartments.setLayout(jPanelDepartmentsLayout);
        jPanelDepartmentsLayout.setHorizontalGroup(
            jPanelDepartmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDepartmentsLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelDepartmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonDepartments, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelDepartmentsLayout.createSequentialGroup()
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCountDepartments, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelDepartmentsLayout.setVerticalGroup(
            jPanelDepartmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelDepartmentsLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonDepartments, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(5, 5, 5)
                .addGroup(jPanelDepartmentsLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel7)
                    .addComponent(jTextFieldCountDepartments, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(10, Short.MAX_VALUE))
        );

        jPanelLeaves.setPreferredSize(new java.awt.Dimension(400, 200));
        jPanelLeaves.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentHidden(java.awt.event.ComponentEvent evt) {
                jPanelLeavesComponentHidden(evt);
            }
        });

        jButtonLeave.setBackground(new java.awt.Color(204, 255, 204));
        jButtonLeave.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonLeave.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/alone.png"))); // NOI18N
        jButtonLeave.setText("Leaves ");
        jButtonLeave.setBorder(null);
        jButtonLeave.setPreferredSize(new java.awt.Dimension(1920, 1080));
        jButtonLeave.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLeaveActionPerformed(evt);
            }
        });

        jLabel5.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel5.setText("Count");

        jTextFieldCountLeaves.setEditable(false);
        jTextFieldCountLeaves.setBackground(new java.awt.Color(204, 204, 204));
        jTextFieldCountLeaves.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldCountLeaves.setForeground(new java.awt.Color(0, 102, 51));

        javax.swing.GroupLayout jPanelLeavesLayout = new javax.swing.GroupLayout(jPanelLeaves);
        jPanelLeaves.setLayout(jPanelLeavesLayout);
        jPanelLeavesLayout.setHorizontalGroup(
            jPanelLeavesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLeavesLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelLeavesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonLeave, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelLeavesLayout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCountLeaves)))
                .addContainerGap())
        );
        jPanelLeavesLayout.setVerticalGroup(
            jPanelLeavesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelLeavesLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jButtonLeave, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(jPanelLeavesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldCountLeaves, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel5)))
        );

        jPanelPayroll.setPreferredSize(new java.awt.Dimension(400, 200));

        jButtonPayroll.setBackground(new java.awt.Color(204, 255, 204));
        jButtonPayroll.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonPayroll.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/Payroll.png"))); // NOI18N
        jButtonPayroll.setText("Payroll ");
        jButtonPayroll.setBorder(null);
        jButtonPayroll.setPreferredSize(new java.awt.Dimension(1920, 1080));
        jButtonPayroll.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonPayrollActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanelPayrollLayout = new javax.swing.GroupLayout(jPanelPayroll);
        jPanelPayroll.setLayout(jPanelPayrollLayout);
        jPanelPayrollLayout.setHorizontalGroup(
            jPanelPayrollLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPayrollLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jButtonPayroll, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanelPayrollLayout.setVerticalGroup(
            jPanelPayrollLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelPayrollLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jButtonPayroll, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(53, Short.MAX_VALUE))
        );

        jPanelUsers.setPreferredSize(new java.awt.Dimension(400, 200));

        jButtonUsers.setBackground(new java.awt.Color(204, 255, 204));
        jButtonUsers.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonUsers.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/user.png"))); // NOI18N
        jButtonUsers.setText("Users ");
        jButtonUsers.setBorder(null);
        jButtonUsers.setPreferredSize(new java.awt.Dimension(1920, 1080));
        jButtonUsers.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonUsersActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel6.setText("Count");

        jTextFieldCountUsers.setEditable(false);
        jTextFieldCountUsers.setBackground(new java.awt.Color(204, 204, 204));
        jTextFieldCountUsers.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldCountUsers.setForeground(new java.awt.Color(0, 102, 51));

        javax.swing.GroupLayout jPanelUsersLayout = new javax.swing.GroupLayout(jPanelUsers);
        jPanelUsers.setLayout(jPanelUsersLayout);
        jPanelUsersLayout.setHorizontalGroup(
            jPanelUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelUsersLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelUsersLayout.createSequentialGroup()
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCountUsers, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelUsersLayout.setVerticalGroup(
            jPanelUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelUsersLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jButtonUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(3, 3, 3)
                .addGroup(jPanelUsersLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel6)
                    .addComponent(jTextFieldCountUsers, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(9, 9, 9))
        );

        jPanelAttendance.setPreferredSize(new java.awt.Dimension(400, 200));

        jButtonAttendance.setBackground(new java.awt.Color(204, 255, 204));
        jButtonAttendance.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonAttendance.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/attendance.png"))); // NOI18N
        jButtonAttendance.setText("Attendance ");
        jButtonAttendance.setBorder(null);
        jButtonAttendance.setPreferredSize(new java.awt.Dimension(1920, 1080));
        jButtonAttendance.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonAttendanceActionPerformed(evt);
            }
        });

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel3.setText("Count");

        jTextFieldCountAttendance.setEditable(false);
        jTextFieldCountAttendance.setBackground(new java.awt.Color(204, 204, 204));
        jTextFieldCountAttendance.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldCountAttendance.setForeground(new java.awt.Color(0, 102, 51));

        javax.swing.GroupLayout jPanelAttendanceLayout = new javax.swing.GroupLayout(jPanelAttendance);
        jPanelAttendance.setLayout(jPanelAttendanceLayout);
        jPanelAttendanceLayout.setHorizontalGroup(
            jPanelAttendanceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAttendanceLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelAttendanceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonAttendance, javax.swing.GroupLayout.PREFERRED_SIZE, 287, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(jPanelAttendanceLayout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCountAttendance, javax.swing.GroupLayout.DEFAULT_SIZE, 217, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanelAttendanceLayout.setVerticalGroup(
            jPanelAttendanceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelAttendanceLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jButtonAttendance, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(jPanelAttendanceLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldCountAttendance, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addContainerGap(9, Short.MAX_VALUE))
        );

        jPanelEmployees.setRequestFocusEnabled(false);

        jButtonEmployees.setBackground(new java.awt.Color(204, 255, 204));
        jButtonEmployees.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonEmployees.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/employees.png"))); // NOI18N
        jButtonEmployees.setText("Employees");
        jButtonEmployees.setBorder(null);
        jButtonEmployees.setMaximumSize(null);
        jButtonEmployees.setMinimumSize(null);
        jButtonEmployees.setPreferredSize(new java.awt.Dimension(1920, 1080));
        jButtonEmployees.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonEmployeesActionPerformed(evt);
            }
        });

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel1.setText("Count");

        jTextFieldCountEmployees.setEditable(false);
        jTextFieldCountEmployees.setBackground(new java.awt.Color(204, 204, 204));
        jTextFieldCountEmployees.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jTextFieldCountEmployees.setForeground(new java.awt.Color(0, 102, 51));

        javax.swing.GroupLayout jPanelEmployeesLayout = new javax.swing.GroupLayout(jPanelEmployees);
        jPanelEmployees.setLayout(jPanelEmployeesLayout);
        jPanelEmployeesLayout.setHorizontalGroup(
            jPanelEmployeesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEmployeesLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addGroup(jPanelEmployeesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonEmployees, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addGroup(jPanelEmployeesLayout.createSequentialGroup()
                        .addComponent(jLabel1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jTextFieldCountEmployees)))
                .addContainerGap())
        );
        jPanelEmployeesLayout.setVerticalGroup(
            jPanelEmployeesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanelEmployeesLayout.createSequentialGroup()
                .addGap(6, 6, 6)
                .addComponent(jButtonEmployees, javax.swing.GroupLayout.PREFERRED_SIZE, 91, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addGroup(jPanelEmployeesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jTextFieldCountEmployees, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addContainerGap(9, Short.MAX_VALUE))
        );

        jButtonLogout.setBackground(new java.awt.Color(255, 51, 102));
        jButtonLogout.setFont(new java.awt.Font("Bahnschrift", 0, 30)); // NOI18N
        jButtonLogout.setForeground(new java.awt.Color(153, 255, 255));
        jButtonLogout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/logout.png"))); // NOI18N
        jButtonLogout.setText("Logout");
        jButtonLogout.setBorder(null);
        jButtonLogout.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButtonLogoutActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(jPanelEmployees, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(jPanelAttendance, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE)
                            .addComponent(jPanelLeaves, javax.swing.GroupLayout.DEFAULT_SIZE, 300, Short.MAX_VALUE))
                        .addGap(12, 12, 12)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelDepartments, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanelUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 300, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addComponent(jButtonLogout, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanelPayroll, javax.swing.GroupLayout.DEFAULT_SIZE, 612, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGap(18, 18, 18)
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addComponent(jLabel12)
                        .addGap(28, 28, 28)
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jPanelAttendance, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jPanelDepartments, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(18, 18, 18)
                        .addComponent(jPanelLeaves, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(jPanelUsers, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addComponent(jPanelEmployees, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(jPanelPayroll, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 46, Short.MAX_VALUE)
                .addComponent(jButtonLogout, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(52, 52, 52))
        );

        jSplitPane2.setLeftComponent(jPanel2);

        jPanel3.setBackground(new java.awt.Color(204, 255, 255));

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/logo007.png"))); // NOI18N

        jLabelCross.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/cross.png"))); // NOI18N
        jLabelCross.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                jLabelCrossMouseClicked(evt);
            }
        });

        jLabel16.setFont(new java.awt.Font("Bahnschrift", 0, 36)); // NOI18N
        jLabel16.setForeground(new java.awt.Color(102, 102, 255));
        jLabel16.setText("Garment Factory Management System");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel4)
                .addGap(18, 18, 18)
                .addComponent(jLabel16)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jLabelCross)
                .addContainerGap())
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jLabel4, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(jPanel3Layout.createSequentialGroup()
                        .addGap(12, 12, 12)
                        .addComponent(jLabel16))
                    .addComponent(jLabelCross, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jSplitPane2)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(filler1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(6, 6, 6)
                .addComponent(jSplitPane2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    private void jButtonLogoutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLogoutActionPerformed
        this.dispose();
        new Login().setVisible(true);
    }//GEN-LAST:event_jButtonLogoutActionPerformed

    private void jButtonEmployeesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonEmployeesActionPerformed
        new Employees().setVisible(true);
    }//GEN-LAST:event_jButtonEmployeesActionPerformed

    private void jLabelCrossMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_jLabelCrossMouseClicked
        System.exit(0);
    }//GEN-LAST:event_jLabelCrossMouseClicked

    private void jButtonPayrollActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPayrollActionPerformed

        new Payrolls().setVisible(true);

    }//GEN-LAST:event_jButtonPayrollActionPerformed

    private void jButtonAttendanceActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonAttendanceActionPerformed
        new AttendanceDaily().setVisible(true); // Call setVisible on the instance
    }//GEN-LAST:event_jButtonAttendanceActionPerformed

    private void jButtonLeaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonLeaveActionPerformed
        new Leaves().setVisible(true);
    }//GEN-LAST:event_jButtonLeaveActionPerformed

    private void jButtonUsersActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonUsersActionPerformed
        new Users().setVisible(true);
    }//GEN-LAST:event_jButtonUsersActionPerformed

    private void jButtonDepartmentsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonDepartmentsActionPerformed
        new Departments().setVisible(true);
    }//GEN-LAST:event_jButtonDepartmentsActionPerformed

    private void jPanelLeavesComponentHidden(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_jPanelLeavesComponentHidden
        // TODO add your handling code here:
    }//GEN-LAST:event_jPanelLeavesComponentHidden

    private void jButtonPreviewBlackListsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButtonPreviewBlackListsActionPerformed
        new BlackLists().setVisible(true);
    }//GEN-LAST:event_jButtonPreviewBlackListsActionPerformed

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                int testUserId = 1;
                String testRoleName = "Admin";
                new Dashboard(testUserId, testRoleName).setVisible(true);
            }
        });
    }

    private void startDisplayCurrentDateAndTime() {
        // Format for date and time (dd-MM-yyyy : hh-mm-ss a)
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy : hh-mm-ss a");

        // Create a Timer to update the label every second
        timer = new Timer(1000, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Get the current date and time
                Date now = new Date();
                // Format the date and time
                String dateTimeString = formatter.format(now);
                // Update the label with the formatted date and time
                jLabelDisplayCurrentDateAndTimer.setText(dateTimeString);
            }
        });

        // Start the timer
        timer.start();
    }

    private String getCountFromTable(String tableName) {
        String query = "SELECT COUNT(*) FROM " + tableName;
        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query); ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getString(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "0";
    }

    private String getCountAttendanceDaily(String tableName) {
        String currentDate = LocalDate.now().toString(); 
        String query = "SELECT COUNT(*) FROM " + tableName + " WHERE date = ?";
        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, currentDate);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return "0";
    }

    private void startRealTimeReload() {
        int delay = 2000;

        // Create a Timer to reload counts periodically
        timer = new Timer(delay, new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Update counts in the text fields
                SwingUtilities.invokeLater(() -> updateCounts());
            }
        });

        // Start the timer
        timer.start();
    }

    // Method to stop the timer when it's no longer needed
    private void stopRealTimeReload() {
        if (timer != null) {
            timer.stop();
        }
    }

    class DropShadowBorder extends AbstractBorder {

        private final int shadowSize;
        private final Color shadowColor;

        public DropShadowBorder(int shadowSize, Color shadowColor) {
            this.shadowSize = shadowSize;
            this.shadowColor = shadowColor;
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(shadowSize, shadowSize, shadowSize, shadowSize);
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int width, int height) {
            Graphics2D g2d = (Graphics2D) g.create();

            // Enable antialiasing for smoother shadows
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // Draw the shadow
            g2d.setColor(shadowColor);
            for (int i = 0; i < shadowSize; i++) {
                g2d.drawRoundRect(x + i, y + i, width - i - 1, height - i - 1, shadowSize, shadowSize);
            }

            g2d.dispose();
        }
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.Box.Filler filler1;
    private javax.swing.JButton jButtonAttendance;
    private javax.swing.JButton jButtonDepartments;
    private javax.swing.JButton jButtonEmployees;
    private javax.swing.JButton jButtonLeave;
    private javax.swing.JButton jButtonLogout;
    private javax.swing.JButton jButtonPayroll;
    private javax.swing.JButton jButtonPreviewBlackLists;
    private javax.swing.JButton jButtonUsers;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelCross;
    private javax.swing.JLabel jLabelDisplayCurrentDateAndTimer;
    private javax.swing.JLabel jLabelEmployeeAbsent;
    private javax.swing.JLabel jLabelEmployeeBlackLists;
    private javax.swing.JLabel jLabelEmployeeLate;
    private javax.swing.JLabel jLabelEmployeeOverTime;
    private javax.swing.JLabel jLabelEmployeePresent;
    private javax.swing.JLabel jLabelEmployeeWorkTime;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanelAttendance;
    private javax.swing.JPanel jPanelDailyReport;
    private javax.swing.JPanel jPanelDepartments;
    private javax.swing.JPanel jPanelEmployees;
    private javax.swing.JPanel jPanelLeaves;
    private javax.swing.JPanel jPanelPayroll;
    private javax.swing.JPanel jPanelUsers;
    private javax.swing.JSplitPane jSplitPane2;
    private javax.swing.JTextField jTextFieldCountAttendance;
    private javax.swing.JTextField jTextFieldCountDepartments;
    private javax.swing.JTextField jTextFieldCountEmployees;
    private javax.swing.JTextField jTextFieldCountLeaves;
    private javax.swing.JTextField jTextFieldCountUsers;
    // End of variables declaration//GEN-END:variables
}
