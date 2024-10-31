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
import DatabaseConnection.config;

public class Login extends javax.swing.JFrame {

    public Login() {
        initComponents();
        jButtonLogin.addActionListener(e -> loginUser());
        jButtonClear.addActionListener(e -> clearLoginForm());
    }

    private void loginUser() {
        String username = jTextFieldUsername.getText().trim();
        String password = new String(jTextFieldPassword.getPassword()).trim();

        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter both username and password.");
            return;
        }

        String query = "SELECT user_id, role_name FROM users WHERE username = ? AND password = ?";

        try (Connection con = config.getConnection(); PreparedStatement ps = con.prepareStatement(query)) {
            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int userId = rs.getInt("user_id");
                    String roleName = rs.getString("role_name");

                    System.out.println("Login successful. User ID: " + userId + ", Role: " + roleName);

                    openDashboard(userId, roleName);
                } else {
                    JOptionPane.showMessageDialog(this, "Invalid username or password.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error logging in: " + e.getMessage());
        }
    }

    private void openDashboard(int userId, String roleName) {
        if ("TeamLeader".equals(roleName)) {
            String teamLeaderName = "Johnny"; 
            AttendanceTeamLeader attendanceTeamLeader = new AttendanceTeamLeader(teamLeaderName, userId);
            attendanceTeamLeader.setVisible(true);
        } else {
            Dashboard dashboard = new Dashboard(userId, roleName);
            dashboard.setVisible(true);
        }
        this.dispose();
    }

    private void clearLoginForm() {
        jTextFieldUsername.setText("");
        jTextFieldPassword.setText("");
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTextFieldUsername = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jButtonClear = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jButtonLogin = new javax.swing.JButton();
        jLabel4 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jTextFieldPassword = new javax.swing.JPasswordField();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setPreferredSize(new java.awt.Dimension(1300, 700));
        setResizable(false);

        jTextFieldUsername.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel1.setText("Login To System");

        jButtonClear.setBackground(new java.awt.Color(255, 51, 51));
        jButtonClear.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jButtonClear.setForeground(new java.awt.Color(255, 255, 255));
        jButtonClear.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/clear.png"))); // NOI18N
        jButtonClear.setText("Clear");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel2.setText("Password");

        jButtonLogin.setBackground(new java.awt.Color(102, 255, 102));
        jButtonLogin.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jButtonLogin.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/login.png"))); // NOI18N
        jButtonLogin.setText("Login");

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        jLabel4.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/logo007.png"))); // NOI18N

        jLabel3.setFont(new java.awt.Font("Segoe UI", 0, 24)); // NOI18N
        jLabel3.setText("Username");

        jLabel5.setIcon(new javax.swing.ImageIcon(getClass().getResource("/garmentfactory/image/login-image.jpg"))); // NOI18N

        jTextFieldPassword.setFont(new java.awt.Font("Segoe UI", 0, 20)); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel1))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(30, 30, 30)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 142, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonLogin, javax.swing.GroupLayout.PREFERRED_SIZE, 182, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jTextFieldPassword)
                            .addComponent(jTextFieldUsername))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 53, Short.MAX_VALUE)
                .addComponent(jLabel5)
                .addGap(47, 47, 47))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(32, 32, 32)
                        .addComponent(jLabel1))
                    .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 100, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(164, 164, 164)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jTextFieldUsername, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(32, 32, 32)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jTextFieldPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 38, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jButtonLogin, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jButtonClear, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addComponent(jLabel5, javax.swing.GroupLayout.DEFAULT_SIZE, 700, Short.MAX_VALUE)
        );

        pack();
        setLocationRelativeTo(null);
    }// </editor-fold>//GEN-END:initComponents

    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Login().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonClear;
    private javax.swing.JButton jButtonLogin;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JPasswordField jTextFieldPassword;
    private javax.swing.JTextField jTextFieldUsername;
    // End of variables declaration//GEN-END:variables
}
