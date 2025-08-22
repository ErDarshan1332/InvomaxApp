package org;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

public class LoginempController implements Initializable {

    @FXML private Label wrrongpass;
    @FXML private TextField textusername;
    @FXML private PasswordField textpassword;
    @FXML private Button login;
    @FXML private AnchorPane rootpane;

    // --- HikariCP Connection Pool ---
    private static HikariDataSource dataSource;

    static {
    HikariConfig config = new HikariConfig();
    config.setJdbcUrl("jdbc:mysql://127.0.0.1:3306/centralized_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
    config.setUsername("root");
    config.setPassword("Invomax@2025");
    config.setMaximumPoolSize(20); // 20 simultaneous connections
    config.setMinimumIdle(2);
    config.setIdleTimeout(60000);
    config.setMaxLifetime(1800000);
    config.setConnectionTimeout(30000);
    config.setPoolName("InvomaxPool");
    dataSource = new HikariDataSource(config);
}


    @Override
    public void initialize(URL url, ResourceBundle rb) {
        login.setOnAction(this::handleButtonAction);
    }

    @FXML
    private void handleButtonAction(ActionEvent event) {
        String username = textusername.getText();
        String password = textpassword.getText();

        if (username.isEmpty() || password.isEmpty()) {
            showErrorAlert("Error", "Please enter both username and password.");
            return;
        }

        try (Connection conn = dataSource.getConnection()) {
            // --- 1. Login verification ---
            String loginSql = "SELECT empid FROM employeelogin WHERE username = ? AND password = ?";
            try (PreparedStatement loginStmt = conn.prepareStatement(loginSql)) {
                loginStmt.setString(1, username);
                loginStmt.setString(2, password);

                ResultSet rsLogin = loginStmt.executeQuery();
                if (!rsLogin.next()) {
                    wrrongpass.setText("Invalid username or password.");
                    showErrorAlert("Login Failed", "Invalid username or password.");
                    return;
                }

                String empId = rsLogin.getString("empid");

                // --- 2. Fetch employee data ---
                String empDataSql = "SELECT fname, lname, jobrole FROM invomaxemp WHERE empid = ?";
                try (PreparedStatement empDataStmt = conn.prepareStatement(empDataSql)) {
                    empDataStmt.setString(1, empId);
                    ResultSet rsEmp = empDataStmt.executeQuery();

                    if (!rsEmp.next()) {
                        showErrorAlert("Error", "Employee data not found for this user.");
                        return;
                    }

                    String fName = rsEmp.getString("fname");
                    String lName = rsEmp.getString("lname");
                    String jobRole = rsEmp.getString("jobrole");
                    LoggedInUser user = new LoggedInUser(empId, fName, lName, jobRole);

                    // --- 3. Validate system date ---
                    if (!validateSystemDate(conn, empId)) return;

                    // --- 4. Insert check-in if required ---
                    handleLogin(conn, empId);

                    // --- 5. Redirect to dashboard ---
                    redirectToDashboard(user);
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Database Error", "Database connection failed: " + e.getMessage());
        }
    }

    private boolean validateSystemDate(Connection conn, String empId) {
        String tableName = "invomax_" + empId;
        LocalDate today = LocalDate.now();

        try {
            String query = "SELECT MAX(date) FROM `" + tableName + "`";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                ResultSet rs = stmt.executeQuery();
                if (rs.next() && rs.getDate(1) != null) {
                    LocalDate maxDate = rs.getDate(1).toLocalDate();
                    if (today.isBefore(maxDate)) {
                        showErrorAlert("Invalid System Date",
                                "Your PC date (" + today + ") is behind. Last recorded date is (" + maxDate + "). " +
                                        "Please correct your PC date.");
                        return false;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Database Error", "Could not validate date: " + e.getMessage());
            return false;
        }
        return true;
    }

    private void handleLogin(Connection conn, String empId) {
        String tableName = "invomax_" + empId;
        LocalDate today = LocalDate.now();
        LocalTime loginTime = LocalTime.now();
        String formattedDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String formattedTime = loginTime.format(DateTimeFormatter.ofPattern("HH:mm"));
        String weekDay = today.getDayOfWeek().toString();
        weekDay = weekDay.charAt(0) + weekDay.substring(1).toLowerCase();

        try {
            String checkQuery = "SELECT COUNT(*) FROM `" + tableName + "` WHERE date = ?";
            try (PreparedStatement stmt = conn.prepareStatement(checkQuery)) {
                stmt.setString(1, formattedDate);
                ResultSet rs = stmt.executeQuery();
                boolean rowExists = rs.next() && rs.getInt(1) > 0;

                if (!rowExists) {
                    String insertQuery = "INSERT INTO `" + tableName + "` (date, weekday, check_in) VALUES (?, ?, ?)";
                    try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                        insertStmt.setString(1, formattedDate);
                        insertStmt.setString(2, weekDay);
                        insertStmt.setString(3, formattedTime);
                        insertStmt.executeUpdate();
                    }
                    System.out.println("✅ Check-in inserted for " + empId);
                } else {
                    System.out.println("ℹ️ Today's row exists. Skipping insert for " + empId);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void redirectToDashboard(LoggedInUser user) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("logindone.fxml"));
            Parent successRoot = loader.load();

            Stage currentStage = (Stage) login.getScene().getWindow();
            currentStage.close();

            Stage successStage = new Stage();
            successStage.initStyle(StageStyle.UNDECORATED);
            successStage.setTitle("Invomax Engineering Solution LLP");
            successStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
            successStage.setResizable(false);
            successStage.setScene(new Scene(successRoot));
            successStage.show();

            PauseTransition delay = new PauseTransition(Duration.seconds(4));
            delay.setOnFinished(event -> {
                try {
                    FXMLLoader dashLoader = new FXMLLoader(getClass().getResource("dashboard_emp.fxml"));
                    Parent dashboardRoot = dashLoader.load();

                    Dashboard_empController dashboardController = dashLoader.getController();
                    dashboardController.setUserData(user);

                    Stage dashboardStage = new Stage();
                    dashboardStage.setTitle("Invomax Engineering Solution LLP");
                    dashboardStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
                    dashboardStage.setResizable(false);
                    dashboardStage.setOnCloseRequest(ev -> {
                        ev.consume();
                        Alert alert = new Alert(Alert.AlertType.INFORMATION);
                        alert.setHeaderText(null);
                        alert.setContentText("Close button is disabled!");
                        alert.showAndWait();
                    });

                    dashboardStage.setScene(new Scene(dashboardRoot));
                    dashboardStage.show();
                    successStage.close();

                } catch (IOException e) {
                    e.printStackTrace();
                    showErrorAlert("Error", "Could not load the dashboard.");
                }
            });
            delay.play();

        } catch (IOException e) {
            e.printStackTrace();
            showErrorAlert("Error", "Could not load the login success screen.");
        }
    }

    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.getDialogPane().setStyle("-fx-background-color: #8DBCC7;");

        Stage mainStage = null;
        if (rootpane != null && rootpane.getScene() != null) {
            mainStage = (Stage) rootpane.getScene().getWindow();
        }

        try {
            Image customIcon = new Image(getClass().getResourceAsStream("emp_logo.png"));
            if (!customIcon.isError()) {
                ImageView customIconView = new ImageView(customIcon);
                customIconView.setFitWidth(48);
                customIconView.setFitHeight(48);
                alert.setGraphic(customIconView);
            }
        } catch (Exception e) { }

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        try { alertStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png"))); }
        catch (Exception e) { }

        applyBlurEffect(mainStage);
        alert.showAndWait();
        removeBlurEffect(mainStage);
    }

    private void applyBlurEffect(Stage stage) {
        if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null)
            stage.getScene().getRoot().setEffect(new GaussianBlur(10));
    }

    private void removeBlurEffect(Stage stage) {
        if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null)
            stage.getScene().getRoot().setEffect(null);
    }
}
