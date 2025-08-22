package org;
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/javafx/FXMLController.java to edit this template
 */
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.StackPane;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.animation.Animation;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;

import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.effect.GaussianBlur; 
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage; 
import javafx.stage.StageStyle;
import javafx.stage.WindowEvent;

/**
 * FXML Controller class
 *
 * @author Invomax
 */

public class Dashboard_empController implements Initializable {

    @FXML
    private Label timeText;
    @FXML
    private Label dateText;
    @FXML
    private ImageView weatherImage;
    @FXML
    private Text tempText;
    @FXML
    private Text seasonText;
    
    // 🔁 Replace with your actual OpenWeatherMap API key
    private static final String API_KEY = "43862b98e915ac1053fe5d846539ef54"; 
    
    private static final String CITY = "Nashik";
    @FXML
    private AnchorPane rootPane;
    @FXML
    private Label typingLabel;
    
    private final String HEADLINE_TEXT = "Upcoming Soon.....";
    
    @FXML
    private Label activetime;
    @FXML
    private Label extratime;
    
    private int mainSeconds = 0;
    
    private int overtimeSeconds = 0;
    
    private static final int EIGHT_HOURS_IN_SECONDS = 8 * 60 * 60; // 8 घंटे = 28800 सेकंड
    
    private boolean isTimerRunning = false;
    
    @FXML
    private Label empIdLabel;
    @FXML
    private Label uani;
    @FXML
    private Label jobRoleLabel;
    @FXML
    private Button btnMarkAttendance;
    
    private Label datememo = dateText;
       
    private String fname;
    
    private String empId;
    @FXML
    private LineChart<String, Number> lineChart;
    
    private static final String DB_URL = "jdbc:mysql://127.0.0.1:3306/centralized_db";
    private static final String DB_USER = "root";
    private static final String DB_PASS = "Invomax@2025";
    @FXML
    private ImageView present;
    @FXML
    private ImageView profileImageView;
    @FXML
    private Button updatebtn;
    
    /**
     * Initializes the controller class.
     * @param url
     * @param rb
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Wather animation here
        new Thread(this::fetchAndDisplayWeather).start();
        
        // Local Time Code here
        Timeline timeline = new Timeline(
                new KeyFrame(Duration.ZERO, e -> updateClock()),
                new KeyFrame(Duration.seconds(1))
        );
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
        
        //updateProgressAnimated(10, 10);
      
        playTypingAnimations();
        
        extratime.setVisible(false);
        
        present.setVisible(false);
        
        startTimers();
        
        if (empId != null && !empId.isEmpty()) {
            String gender = getEmployeeGender(empId);   
            System.out.println("Employee Gender: " + gender); 
            setProfileImage(gender);                   
        } else {
            System.err.println("empId is null or empty, cannot load profile image.");
        }
        
        updatebtn.setOnAction(this::onCheckUpdate);
        
        Image staticImage = new Image(getClass().getResource("update.png").toExternalForm());
            profileImageView.setImage(staticImage);
            profileImageView.setFitWidth(20);
            profileImageView.setFitHeight(20);

            profileImageView.setOnMouseEntered(e -> {
                Image gifImage = new Image(getClass().getResource("update.gif").toExternalForm());
                profileImageView.setImage(gifImage);
            });

            profileImageView.setOnMouseExited(e -> {
                profileImageView.setImage(staticImage);
            });
            
            
            
        Platform.runLater(() -> {
            Stage stage = (Stage) rootPane.getScene().getWindow();
            NotificationManager notificationManager = new NotificationManager(() -> stage);
            notificationManager.start();
        });
    }    
    
    private void checkAttendanceStatus(String empId) {   
        String tableName = "invomax_" + empId; // current empId
        LocalDate today = LocalDate.now();
        
        String query = "SELECT attendance FROM `" + tableName + "` WHERE date = ?";
        
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
                
                PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setString(1, today.toString());
                    ResultSet rs = pstmt.executeQuery();
                    
                    if (rs.next()) {
                        
                        String attendance = rs.getString("attendance");
                        if ("P".equalsIgnoreCase(attendance)) {
                            present.setVisible(true); 
                        } else {
                            present.setVisible(false); 
                        }
                    } else {
                        present.setVisible(false); 
                    }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setEmpId(String empId) {
        this.empId = empId;
        
    }
    
    @FXML
    private void handleViewAttendanceButton(ActionEvent event) {
       
        Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        showAttendanceTableInAlert(mainStage);
    }

    public void showAttendanceTableInAlert(Stage mainStage) {
        if (empId == null || empId.isEmpty()) {
            showErrorAlert("Invalid Employee ID", "Employee ID is not set properly."); // showErrorAlert आता mainStage घेत नाही
            return;
        }
        String tableName = "invomax_" + empId;
        String query = "SELECT id, date, weekday, check_in, check_out, active_time, overtime, work_status , attendance FROM `" + tableName + "`ORDER BY date ASC";
               
        TableView<AttendanceRecord> tableView = new TableView<>();
                
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        
        TableColumn<AttendanceRecord, String> srnoColumn = new TableColumn<>("Sr No");
        TableColumn<AttendanceRecord, String> dateColumn = new TableColumn<>("Date");
        TableColumn<AttendanceRecord, String> weekdayColumn = new TableColumn<>("Weekday");
        TableColumn<AttendanceRecord, String> checkinColumn = new TableColumn<>("Check-in");
        TableColumn<AttendanceRecord, String> checkoutColumn = new TableColumn<>("Check-out");
        TableColumn<AttendanceRecord, String> activetimeColumn = new TableColumn<>("Active Time");
        TableColumn<AttendanceRecord, String> overtimeColumn = new TableColumn<>("Overtime");
        TableColumn<AttendanceRecord, String> statusColumn = new TableColumn<>("Status");
        TableColumn<AttendanceRecord, String> attendanceColumn = new TableColumn<>("Attend");

        tableView.getColumns().addAll(srnoColumn, dateColumn, weekdayColumn, checkinColumn, checkoutColumn, activetimeColumn, overtimeColumn, statusColumn, attendanceColumn);
        
        srnoColumn.setCellValueFactory(cellData -> cellData.getValue().srnoProperty());
        dateColumn.setCellValueFactory(cellData -> cellData.getValue().dateProperty());
        weekdayColumn.setCellValueFactory(cellData -> cellData.getValue().weekdayProperty());
        checkinColumn.setCellValueFactory(cellData -> cellData.getValue().checkinProperty());
        checkoutColumn.setCellValueFactory(cellData -> cellData.getValue().checkoutProperty());
        activetimeColumn.setCellValueFactory(cellData -> cellData.getValue().activetimeProperty());
        overtimeColumn.setCellValueFactory(cellData -> cellData.getValue().overtimeProperty());
        statusColumn.setCellValueFactory(cellData -> cellData.getValue().statusProperty());
        attendanceColumn.setCellValueFactory(cellData -> cellData.getValue().attendaceProperty());

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                String srno = rs.getString("id");
                String date = rs.getString("date");
                String weekday = rs.getString("weekday");
                String checkin = rs.getString("check_in");
                String checkout = rs.getString("check_out");
                String activetime = rs.getString("active_time");
                String overtime = rs.getString("overtime");
                String status = rs.getString("work_status");
                String attendance = rs.getString("attendance");

                
                AttendanceRecord record = new AttendanceRecord(srno, date, weekday, checkin, checkout, activetime, overtime, status, attendance);
                tableView.getItems().add(record);
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorAlert("Database Error", "Failed to load data from the database."); // showErrorAlert आता mainStage घेत नाही
            return;
        }

       
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Invomax Engineering Solution LLP");
        alert.setHeaderText(fname + " Your Attendance");
        alert.getDialogPane().setStyle("-fx-background-color: #8DBCC7;"); 
        
        try {
            Image customIcon = new Image(getClass().getResourceAsStream("emp_logo.png")); 
            ImageView customIconView = new ImageView(customIcon);
            customIconView.setFitWidth(48); 
            customIconView.setFitHeight(48); 
            alert.setGraphic(customIconView);
        } catch (NullPointerException e) {
            System.err.println("Error loading emp_logo.png for alert graphic. Make sure it's in the correct resource path.");
            
        }

        
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        try {
            alertStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
        } catch (NullPointerException e) {
            System.err.println("Error loading emp_logo.png for stage icon.");
        }

        VBox vbox = new VBox(tableView);
        
        
        alert.getDialogPane().setContent(vbox);

        applyBlurEffect(mainStage);
       
        alert.showAndWait();

        removeBlurEffect(mainStage);
    }
 
    private void showErrorAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        alert.getDialogPane().setStyle("-fx-background-color: #8DBCC7;"); 
        
        Stage mainStage = null;
        if (rootPane != null && rootPane.getScene() != null) {
            mainStage = (Stage) rootPane.getScene().getWindow();
        }

        
//        try {
//            Image customIcon = new Image(getClass().getResourceAsStream("emp_logo.png"));
//            if (!customIcon.isError()) {
//                ImageView customIconView = new ImageView(customIcon);
//                customIconView.setFitWidth(48);
//                customIconView.setFitHeight(48);
//                alert.setGraphic(customIconView);
//            }
//        } catch (NullPointerException e) {
//            System.err.println("Error loading emp_logo.png for error alert graphic.");
//        }

        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        try {
            alertStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png"))); 
        } catch (NullPointerException e) {
            System.err.println("Error loading emp_logo.png for error alert stage icon.");
        }

        
        applyBlurEffect(mainStage);
        alert.showAndWait();
        removeBlurEffect(mainStage);
    }

    @FXML
    private void handleDailysheetButton(ActionEvent event) {
        Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

        dailysheetrecord(mainStage);
}

    // Method to either insert or update the daily sheet data based on the current date
    private void handleDatabaseUpdate(LocalDate currentDate, String projectName, String projectTakenBy, String todaysWork) {
    String todayDate = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE);  // Get current date as string (YYYY-MM-DD)
    String tableName = "invomax_" + empId; // Assuming you use employee's table name like invomax_[empId]

    // Check if data for today exists in the employee's table
    String checkQuery = "SELECT * FROM `" + tableName + "` WHERE date = ?";
    
    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement checkStmt = conn.prepareStatement(checkQuery)) {

        checkStmt.setString(1, todayDate);
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            // If the row exists for today, update the row
            String updateQuery = "UPDATE `" + tableName + "` SET project_name = ?, project_taken = ?, daily_sheet = ? WHERE date = ?";
            try (PreparedStatement updateStmt = conn.prepareStatement(updateQuery)) {
                updateStmt.setString(1, projectName);
                updateStmt.setString(2, projectTakenBy);
                updateStmt.setString(3, todaysWork);
                updateStmt.setString(4, todayDate);
                
                int rowsUpdated = updateStmt.executeUpdate();
                if (rowsUpdated > 0) {
                    System.out.println("Daily sheet updated successfully for " + todayDate);
                }
            }

        } else {
            // If the row does not exist, insert a new row
            String insertQuery = "INSERT INTO `" + tableName + "` (date, project_name, project_taken_by, todays_work) VALUES (?, ?, ?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                insertStmt.setString(1, todayDate);
                insertStmt.setString(2, projectName);
                insertStmt.setString(3, projectTakenBy);
                insertStmt.setString(4, todaysWork);
                
                int rowsInserted = insertStmt.executeUpdate();
                if (rowsInserted > 0) {
                    System.out.println("Daily sheet submitted successfully for " + todayDate);
                }
            }
        }

    } catch (SQLException e) {
        e.printStackTrace();
        System.out.println("Error while handling daily sheet for " + todayDate);
    }
}

    @FXML
    private void handleButtonAction(ActionEvent event) {
    }

    @FXML
    private void handleTODOButton(ActionEvent event) {
    }

    @FXML
    private void handleRequestLeaveButton(ActionEvent event) {
    }

    @FXML
    private void handleProjectButton(ActionEvent event) {
    }

    @FXML
    private void handleTaskButton(ActionEvent event) {
    }

    @FXML
    private void handleLogoutButton(ActionEvent event) {
       Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
       Stage logoutStage = (Stage) ((Node) event.getSource()).getScene().getWindow();

       logoyAction(mainStage,logoutStage);
    }

    private boolean checkDailySheetStatus(String empId) {
        String tableName = "invomax_" + empId;
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        // daily_sheet, project_name, project_taken हे तुमच्या डेटाबेस कॉलमची नावे आहेत असे गृहीत धरले आहे.
        String query = "SELECT daily_sheet, project_name, project_taken FROM `" + tableName + "` WHERE date = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, todayDate);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                String dailySheet = rs.getString("daily_sheet");
                String projectName = rs.getString("project_name");
                String projectTaken = rs.getString("project_taken");

              
                if (dailySheet == null || dailySheet.trim().isEmpty() ||
                    projectName == null || projectName.trim().isEmpty() ||
                    projectTaken == null || projectTaken.trim().isEmpty()) {
                    return false; 
                } else {
                    return true; 
                }
            } else {
                
                return false;
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("डेली शीट स्टेटस तपासताना डेटाबेस त्रुटी: " + e.getMessage());
            showErrorAlert("डेटाबेस त्रुटी", "डेली शीट स्थिती तपासण्यात अयशस्वी. कृपया पुन्हा प्रयत्न करा.");
            return false; // डेटाबेस त्रुटी झाल्यास false समजा.
        }
    }
    
    private KeyFrame updateClock() {
        LocalDateTime now = LocalDateTime.now();

        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("hh:mm:ss a");
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd - MMMM - yyyy", Locale.ENGLISH);

        timeText.setText(now.format(timeFormatter));
        dateText.setText(now.format(dateFormatter));
        return null;
    }
      
    private void fetchAndDisplayWeather() {
    try {
        String urlStr = "https://api.openweathermap.org/data/2.5/weather?q=" + CITY +
                "&appid=" + API_KEY + "&units=metric";

        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        conn.setRequestMethod("GET");

        int responseCode = conn.getResponseCode();

        if (responseCode != 200) {
            System.out.println("HTTP ERROR: " + responseCode);
            BufferedReader errorReader = new BufferedReader(new InputStreamReader(conn.getErrorStream()));
            StringBuilder errorResponse = new StringBuilder();
            String line;
            while ((line = errorReader.readLine()) != null) {
                errorResponse.append(line);
            }
            errorReader.close();
            System.out.println("Error response: " + errorResponse);
            return;
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = reader.readLine()) != null) {
            response.append(line);
        }

        reader.close();

        System.out.println("API Response: " + response); // 🔍 Print for debug

        JSONObject json = new JSONObject(response.toString());

        // Safe access
        if (json.has("main") && json.getJSONObject("main").has("temp")) {
            double temp = json.getJSONObject("main").getDouble("temp");

            String condition = "Unknown";
            if (json.has("weather") && json.getJSONArray("weather").length() > 0) {
                condition = json.getJSONArray("weather").getJSONObject(0).getString("main");
            }

            String finalCondition = condition;
            Platform.runLater(() -> {
                tempText.setText(Math.round(temp) + "°C");
                seasonText.setText(finalCondition);
                weatherImage.setImage(new Image(getClass().getResourceAsStream(getImagePath(finalCondition))));
            });

        } else {
            Platform.runLater(() -> {
                tempText.setText("Invalid data");
                seasonText.setText("Missing 'main.temp'");
            });
        }

    } catch (Exception e) {
        e.printStackTrace();
        Platform.runLater(() -> {
            tempText.setText("Error");
            seasonText.setText("Check network or API key");
        });
    }
}

    private String getSeason(int month) {
        if (month >= 3 && month <= 5)
            return "Summer";
        else if (month >= 6 && month <= 9)
            return "Monsoon";
        else if (month >= 10 && month <= 11)
            return "Autumn";
        else
            return "Winter";
    }    
    
    // Weather logic
    private String getImagePath(String condition) { 
        
        switch (condition.toLowerCase()) {
            case "clear":
                return "sun.png"; // Changed to absolute path
            case "clouds":
                return "cloud.png"; // Changed to absolute path
            case "rain":
                return "rain.png"; // Changed to absolute path
            case "mist":
                return "mist.png"; // Changed to absolute path
            default:
                return "default.png"; // Changed to absolute path
        }
    }

    private void playTypingAnimations() {
        
        Timeline headlineTimeline = new Timeline();
        StringBuilder headlineDisplayed = new StringBuilder();

        for (int i = 0; i < HEADLINE_TEXT.length(); i++) {
            final int index = i;
            KeyFrame kf = new KeyFrame(Duration.millis(150 * (i + 1)), e -> {
                headlineDisplayed.append(HEADLINE_TEXT.charAt(index));
                typingLabel.setText(headlineDisplayed.toString());
            });
            headlineTimeline.getKeyFrames().add(kf);
        }
        // After typing is done, wait, then clear and repeat
        KeyFrame pauseAndRestart = new KeyFrame(Duration.millis(150 * (HEADLINE_TEXT.length() + 5)), e -> {
            typingLabel.setText("");
            playTypingAnimations(); // 🔁 Recursively call itself to loop
        });
        headlineTimeline.getKeyFrames().add(pauseAndRestart);

        headlineTimeline.play();
    }

    private void startTimers() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            mainSeconds++;

            if (mainSeconds > EIGHT_HOURS_IN_SECONDS) {
                overtimeSeconds++;
                extratime.setVisible(true);
            }

            updateLabels();
        }));
        timeline.setCycleCount(Animation.INDEFINITE);
        timeline.play();
    }
    
    private void updateLabels() {
        int mainHrs = mainSeconds / 3600;
        int mainMins = (mainSeconds % 3600) / 60;
        int mainSecs = mainSeconds % 60;
        activetime.setText(String.format("%02d:%02d:%02d", mainHrs, mainMins, mainSecs));

        int overtimeHrs = overtimeSeconds / 3600;
        int overtimeMins = (overtimeSeconds % 3600) / 60;
        int overtimeSecs = overtimeSeconds % 60;
        extratime.setText(String.format("%02d:%02d:%02d", overtimeHrs, overtimeMins, overtimeSecs));
    }    
    
    public void setUserData(LoggedInUser user) {
            
        empIdLabel.setText(user.getEmpId());
        uani.setText(user.getFirstName() + " " + user.getLastName());
        jobRoleLabel.setText(user.getJobRole());
        this.empId = user.getEmpId(); 
        this.fname = user.getFirstName()+ " "+ user.getLastName();
        loadWeeklyAttendance(user.getEmpId());
        checkAttendanceStatus(user.getEmpId());
        getEmployeeGender(user.getEmpId());
      }
          
    @FXML
    private void handleMarkAttendance(ActionEvent event) { 
        showAttendancePopup(event);     
    }

    // showAttendancePopup 
    private void showAttendancePopup(ActionEvent event) {
        // Create radio buttons for attendance status
        RadioButton workFromOffice = new RadioButton("Work from Office");
        RadioButton workFromClientSide = new RadioButton("Work from Client Side");
        RadioButton workFromHome = new RadioButton("Work from Home");

        // Group them into a toggle group
        ToggleGroup group = new ToggleGroup();
        workFromOffice.setToggleGroup(group);
        workFromClientSide.setToggleGroup(group);
        workFromHome.setToggleGroup(group);

        // Create a VBox to hold the radio buttons
        VBox vbox = new VBox(10);
        vbox.getChildren().addAll(workFromOffice, workFromClientSide, workFromHome);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Mark Attendance");
        alert.setHeaderText("Select your work status:");

        // ब्लर इफेक्टसाठी सध्याची स्टेज मिळवा
        Stage mainStage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        
        try {
            
            Image customIcon = new Image(getClass().getResourceAsStream("emp_logo.png")); 
            ImageView customIconView = new ImageView(customIcon);
            customIconView.setFitWidth(48); 
            customIconView.setFitHeight(48); 
            alert.setGraphic(customIconView);
        } catch (NullPointerException e) {
            System.err.println("Error loading emp_logo.png for alert graphic. Make sure it's in the correct resource path.");
            
        }
        // सेट custom icon
        Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
        try {
            alertStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png"))); // Changed to absolute path
        } catch (NullPointerException e) {
            System.err.println("Error loading emp_logo.png for alert stage icon.");
        }
        alert.getDialogPane().setContent(vbox);

        // ब्लर लावा
        applyBlurEffect(mainStage);

        // शो the alert and wait for the result
        alert.showAndWait().ifPresent(response -> {
            // ब्लर काढा
            removeBlurEffect(mainStage);

            // Get selected radio button
            RadioButton selectedRadioButton = (RadioButton) group.getSelectedToggle();
            if (selectedRadioButton != null) {
                // Handle the database saving logic here
                String selectedStatus = selectedRadioButton.getText();
                String empidata= empIdLabel.getText();
                saveAttendanceToDatabase(empidata, "P", selectedStatus);
                
                if (response == ButtonType.OK) {                   
                    showGifPopup(); // GIF दाखवेल 3 sec                    
                }                
            }
        });
    }
    
    private void saveAttendanceToDatabase(String empidata, String p, String selectedStatus) {
        
        String tableName = "invomax_" + empidata;
        String todayDate = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE);
        String weekDay = LocalDate.now().getDayOfWeek().toString();  
        weekDay = weekDay.charAt(0) + weekDay.substring(1).toLowerCase();  

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {
            // आजची attendance update करायचा प्रयत्न
            String updateQuery = "UPDATE `" + tableName + "` SET attendance = ?, work_status = ?, weekday = ? WHERE date = ?";
            PreparedStatement pst = conn.prepareStatement(updateQuery);
            pst.setString(1, p);               // attendance (P)
            pst.setString(2, selectedStatus);  // work_status
            pst.setString(3, weekDay);         // weekday
            pst.setString(4, todayDate);       // date

            int updatedRows = pst.executeUpdate();

            if (updatedRows > 0) {
                System.out.println("Attendance updated for " + empidata + " on " + todayDate + " (" + weekDay + ")");
            } else {
                // जर आजचा record नसेल तर insert करायचा
                String insertQuery = "INSERT INTO `" + tableName + "` (date, weekday, attendance, work_status) VALUES (?, ?, ?, ?)";
                PreparedStatement insertPst = conn.prepareStatement(insertQuery);
                insertPst.setString(1, todayDate);
                insertPst.setString(2, weekDay);
                insertPst.setString(3, p);
                insertPst.setString(4, selectedStatus);
                insertPst.executeUpdate();
                System.out.println("Attendance inserted for " + empidata + " on " + todayDate + " (" + weekDay + ")");
            }
            // ✅ Attendance update/insert झाल्यावर लगेच chart refresh करा
            Platform.runLater(() -> loadWeeklyAttendance(empidata));
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    private void loadWeeklyAttendance(String empId) {
        
        String tableName = "invomax_" + empId;

        // Mapping attendance to numerical values for plotting
        Map<String, Double> attendanceMap = new HashMap<>();
        attendanceMap.put("A", 0.0);
        attendanceMap.put("H", 0.5);
        attendanceMap.put("P", 1.0);

        // X-axis will be weekday (Monday to Sunday)
        List<String> daysOfWeek = Arrays.asList(
            "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
        );

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Attendance");
        
        // **बदल 2**: लाइन चार्टचे ॲनिमेशन बंद केले
        lineChart.setAnimated(false);

        // **बदल 3**: चालू आठवड्याच्या सोमवारची तारीख काढली
        LocalDate today = LocalDate.now();
        LocalDate mondayOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        String mondayDateStr = mondayOfWeek.format(DateTimeFormatter.ISO_LOCAL_DATE);

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS)) {

            // **बदल 4**: क्वेरी बदलून सोमवारपासूनचा डेटा निवडला
            String query = "SELECT weekday, attendance FROM `" + tableName + "` WHERE date >= ? ORDER BY date ASC";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, mondayDateStr);
            ResultSet rs = stmt.executeQuery();

            Map<String, Double> attendanceDataForWeek = new HashMap<>();
            while (rs.next()) {
                String weekday = rs.getString("weekday");
                String attendanceStatus = rs.getString("attendance");
                double value = attendanceMap.getOrDefault(attendanceStatus, 0.0);
                attendanceDataForWeek.put(weekday, value);
            }

            // **बदल 5**: सर्व ७ दिवसांसाठी डेटा पॉइंट्स जोडले (माहिती नसलेल्या दिवसांसाठी शून्य सेट केला)
            for (String day : daysOfWeek) {
                double value = attendanceDataForWeek.getOrDefault(day, 0.0); // डेटा नसेल तर 0.0 (Absent) समजा.
                series.getData().add(new XYChart.Data<>(day, value));
            }

            Platform.runLater(() -> {
                lineChart.getData().clear();
                lineChart.getData().add(series);
            });
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error loading weekly attendance data.");
        }
    }

    // Helper method to apply blur effect to a stage
    private void applyBlurEffect(Stage stage) {
        if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null) {
            GaussianBlur blurEffect = new GaussianBlur(10); 
            stage.getScene().getRoot().setEffect(blurEffect);
        }
    }

    // Helper method to remove blur effect from a stage
    private void removeBlurEffect(Stage stage) {
        
        if (stage != null && stage.getScene() != null && stage.getScene().getRoot() != null) {
            stage.getScene().getRoot().setEffect(null); 
        }
    }
    
    private void dailysheetrecord(Stage mainStage) {
        
        // Create a dialog for the daily sheet entry
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Invomax Engineering Solution LLP");
        dialog.setHeaderText("Please enter the daily sheet details !");

        // Create dialog pane and set its content
        DialogPane dialogPane = dialog.getDialogPane();
    
        Stage dialogStage = (Stage) dialog.getDialogPane().getScene().getWindow();
        
            try {
                dialogStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
            } catch (NullPointerException e) {
                System.err.println("Error loading emp_logo.png for stage icon.");
            }
        
        try {
            Image customIcon = new Image(getClass().getResourceAsStream("emp_logo.png")); 
            ImageView customIconView = new ImageView(customIcon);
            customIconView.setFitWidth(48); 
            customIconView.setFitHeight(48); 
            dialog.setGraphic(customIconView);
        } catch (NullPointerException e) {
            System.err.println("Error loading emp_logo.png for alert graphic. Make sure it's in the correct resource path.");
            
        }
        
        // Create labels and fields
        TextField projectNameField = new TextField();
        projectNameField.setPromptText("Working On ...");

        TextField projectTakenByField = new TextField();
        projectTakenByField.setPromptText("Who is giving you this project...");

        TextArea todaysWorkArea = new TextArea();
        todaysWorkArea.setPromptText("Today's Work... (If you are working on two projects, mention point-wise)");

        // Layout for the dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Project Name:"), 0, 0);
        grid.add(projectNameField, 1, 0);
        grid.add(new Label("Project Taken By:"), 0, 1);
        grid.add(projectTakenByField, 1, 1);
        grid.add(new Label("Today's Work:"), 0, 2);
        grid.add(todaysWorkArea, 1, 2);

        dialogPane.setContent(grid);

        // Create "Submit" and "Cancel" buttons
        ButtonType submitButtonType = new ButtonType("Submit", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, cancelButtonType);

        // Handle submit button action
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                String projectName = projectNameField.getText();
                String projectTakenBy = projectTakenByField.getText();
                String todaysWork = todaysWorkArea.getText();

                // Get current date
                LocalDate currentDate = LocalDate.now();
                // Call method to either update or insert data in the employee's table
                handleDatabaseUpdate(currentDate, projectName, projectTakenBy, todaysWork);  // Handle database logic
            }
            return null;
        });

        // Show the dialog
        applyBlurEffect(mainStage);
        dialog.showAndWait();
        removeBlurEffect(mainStage);
    }
          
    private void logoyAction(Stage mainStage , Stage logoutStage) {
        
        if (empId == null || empId.isEmpty()) {
            showErrorAlert("Logout Error", "Employee ID is not set. Cannot verify daily sheet.");
            return;
        }

        if (checkDailySheetStatus(this.empId)) { 
            // ✅ Save Logout Time + Active Time + Extra Time
            saveLogoutData(this.empId);
            // ✅ Then redirect to logout screen
            redirectToLogoutscreen(logoutStage);
        } else {            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Invomax Engineering Solution LLP");
            alert.setHeaderText(fname + " Your Daily Sheet Is Pending !");
            alert.getDialogPane().setStyle("-fx-background-color: #8DBCC7;");       
            
            
            
            try {
                Image customIcon = new Image(getClass().getResourceAsStream("logout.gif")); 
                ImageView customIconView = new ImageView(customIcon);
                customIconView.setFitWidth(48); 
                customIconView.setFitHeight(48); 
                alert.setGraphic(customIconView);
            } catch (NullPointerException e) {
                System.err.println("Error loading logout.gif for alert graphic.");
            }

            Stage alertStage = (Stage) alert.getDialogPane().getScene().getWindow();
            try {
                alertStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
            } catch (NullPointerException e) {
                System.err.println("Error loading emp_logo.png for stage icon.");
            }

            VBox vbox = new VBox();    
            alert.getDialogPane().setContent(vbox);

            applyBlurEffect(mainStage);
            alert.showAndWait();
            removeBlurEffect(mainStage);
        }
    }

    private void redirectToLogoutscreen(Stage logoutStage) {
    try {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("logoutsuccess.fxml"));
        Parent root = loader.load();

        // ✅ आधीची screen बंद
        logoutStage.close();
        

        // ✅ नवीन Stage
        Stage newStage = new Stage();
        newStage.initStyle(StageStyle.UNDECORATED);
//        newStage.setTitle("Invomax Engineering Solution LLP");
//        newStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
//        newStage.setResizable(false);
//        // ❌ Close disable
//        newStage.setOnCloseRequest(new javafx.event.EventHandler<WindowEvent>() {
//        @Override
//        public void handle(WindowEvent event) {
//            event.consume(); 
//        }
//        });
        newStage.setScene(new Scene(root));
        newStage.show();

        // ✅ 4 सेकंदांनी Stage बंद + App पूर्ण बंद
        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(event -> {
            newStage.close();
            Platform.exit();   // JavaFX बंद
            System.exit(0);    // JVM process बंद
        });
        delay.play();

    } catch (IOException e) {
        e.printStackTrace();
        showErrorAlert("Logout Error", "Could not load logoutsuccess screen.");
    }
}

       
    private void showGifPopup() {
        
        Stage gifStage = new Stage();
        gifStage.setTitle("Processing...");
        gifStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
        // GIF 
        Image gifImage = new Image(getClass().getResourceAsStream("AttendanceLoader.gif"));
        ImageView gifView = new ImageView(gifImage);
        gifView.setFitWidth(200); // size adjust कर
        gifView.setFitHeight(200);

        StackPane root = new StackPane(gifView);
        Scene scene = new Scene(root, 250, 50);

        gifStage.setScene(scene);
        gifStage.show();

        PauseTransition delay = new PauseTransition(Duration.seconds(3));
        delay.setOnFinished(e -> gifStage.close());
        delay.play();
        
        
    }

    private void saveLogoutData(String empId) {
        
        String tableName = "invomax_" + empId;
        LocalDate currentDate = LocalDate.now();
        LocalTime checkOutTime = LocalTime.now();
        
        
        // Active Time
        int mainHrs = mainSeconds / 3600;
        int mainMins = (mainSeconds % 3600) / 60;
        int mainSecs = mainSeconds % 60;
        String activeTimeStr = String.format("%02d:%02d:%02d", mainHrs, mainMins, mainSecs);

        // Extra Time
        int overtimeHrs = overtimeSeconds / 3600;
        int overtimeMins = (overtimeSeconds % 3600) / 60;
        int overtimeSecs = overtimeSeconds % 60;
        String extraTimeStr = String.format("%02d:%02d:%02d", overtimeHrs, overtimeMins, overtimeSecs);

     
        if (overtimeSeconds <= 0) {
            extraTimeStr = "00:00:00";
        }

        String updateQuery = "UPDATE " + tableName + " SET check_out = ?, active_time = ?, overtime = ? WHERE date = ?";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
            PreparedStatement pstmt = conn.prepareStatement(updateQuery)) {

            pstmt.setTime(1, Time.valueOf(checkOutTime));
            pstmt.setString(2, activeTimeStr);
            pstmt.setString(3, extraTimeStr);
            pstmt.setDate(4, java.sql.Date.valueOf(currentDate));

            int rows = pstmt.executeUpdate();
            if (rows > 0) {
                System.out.println("Logout Data saved for " + empId + " on " + currentDate);
            } else {
                System.out.println("No attendance record found for " + empId + " on " + currentDate);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

private void setProfileImage(String gender) {
    String imagePath;

    if ("Male".equalsIgnoreCase(gender)) {
        imagePath = "/male.gif";
    } else if ("Female".equalsIgnoreCase(gender)) {
        imagePath = "/female.gif";
    } else {
        imagePath = "/default.png";
    }
    try {
        URL url = getClass().getResource(imagePath);
        if (url == null) {
            throw new IllegalArgumentException("Image not found: " + imagePath);
        }
        Image image = new Image(url.toExternalForm());
        profileImageView.setImage(image);
    } catch (Exception e) {
        e.printStackTrace();
    }
}




    private String getEmployeeGender(String empId) {
        
        this.empId= empId ;
    String gender = "";
    String query = "SELECT gender FROM invomaxemp WHERE empid = ?";

    try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASS);
         PreparedStatement pstmt = conn.prepareStatement(query)) {

        pstmt.setString(1, empId);
        ResultSet rs = pstmt.executeQuery();

        if (rs.next()) {
            gender = rs.getString("gender");
            System.out.println("DB Gender for " + empId + ": " + gender);
        }
    } catch (SQLException e) {
        e.printStackTrace();
    }
    return gender;
    }



@FXML
    private void onCheckUpdate(ActionEvent event) {
        String versionUrl = "https://api.github.com/repos/ErDarshan1332/InvomaxApp/releases/latest";

        try {
            URL url = new URL(versionUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");

            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = in.readLine()) != null) response.append(line);
            in.close();

            JSONObject json = new JSONObject(response.toString());
            String latestVersion = json.optString("tag_name", "v0.0.0");

            JSONArray assets = json.getJSONArray("assets");
            if (assets.length() == 0) {
                showErrorAlert("Error", "No downloadable files found in the latest release.");
                return;
            }

            String downloadUrl = assets.getJSONObject(0).getString("browser_download_url");

            if (isLatestVersion(AppVersion.CURRENT_VERSION, latestVersion)) {
                showInfoAlert("No Update Available", "You already have the latest version!");
            } else {
                showUpdateDialog(latestVersion, downloadUrl);
            }

        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Could not check for updates.");
        }
    }

    private boolean isLatestVersion(String current, String latest) {
        String[] cur = current.replace("v", "").split("\\.");
        String[] lat = latest.replace("v", "").split("\\.");

        for (int i = 0; i < Math.max(cur.length, lat.length); i++) {
            int c = (i < cur.length) ? Integer.parseInt(cur[i]) : 0;
            int l = (i < lat.length) ? Integer.parseInt(lat[i]) : 0;
            if (c < l) return false;
            if (c > l) return true;
        }
        return true;
    }

    private void showUpdateDialog(String latestVersion, String downloadUrl) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Update Available");
        alert.setHeaderText("Version " + latestVersion + " is available!");
        alert.setContentText("Do you want to download and install it?");

        ButtonType downloadBtn = new ButtonType("Download");
        ButtonType cancelBtn = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(downloadBtn, cancelBtn);

        alert.showAndWait().ifPresent(response -> {
            if (response == downloadBtn) showDownloadProgress(downloadUrl, latestVersion);
        });
    }

    private void showDownloadProgress(String fileUrl, String latestVersion) {
        Stage stage = new Stage();
        stage.setTitle("Downloading Update...");

        ProgressBar progressBar = new ProgressBar(0);
        progressBar.setPrefWidth(300);
        Label lblProgress = new Label("Starting download...");

        VBox vbox = new VBox(10, lblProgress, progressBar);
        vbox.setPadding(new Insets(20));
        vbox.setAlignment(Pos.CENTER);

        stage.setScene(new Scene(vbox));
        stage.show();

        Task<Void> task = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                URL url = new URL(fileUrl);
                URLConnection conn = url.openConnection();
                int fileSize = conn.getContentLength();

                File saveFile = new File(System.getProperty("user.home"), "InvomaxApp-" + latestVersion + ".jar");

                try (InputStream in = conn.getInputStream();
                     FileOutputStream out = new FileOutputStream(saveFile)) {

                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    long totalRead = 0;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, bytesRead);
                        totalRead += bytesRead;

                        updateProgress(totalRead, fileSize);
                        updateMessage(String.format("Downloaded %d%%", (int)((totalRead * 100) / fileSize)));
                    }
                }
                return null;
            }
        };

        progressBar.progressProperty().bind(task.progressProperty());
        lblProgress.textProperty().bind(task.messageProperty());

        task.setOnSucceeded(e -> {
            stage.close();
            installUpdate(latestVersion);
        });

        task.setOnFailed(e -> {
            stage.close();
            showErrorAlert("Download Error", "Failed to download the update.");
        });

        new Thread(task).start();
    }

    private void installUpdate(String latestVersion) {
        try {
            File newFile = new File(System.getProperty("user.home"), "InvomaxApp-" + latestVersion + ".jar");
            if (newFile.exists()) {
                Runtime.getRuntime().exec("java -jar \"" + newFile.getAbsolutePath() + "\"");
                System.exit(0);
            } else showErrorAlert("Error", "Downloaded file not found.");
        } catch (Exception e) {
            e.printStackTrace();
            showErrorAlert("Error", "Could not install the update.");
        }
    }

   


private void showInfoAlert(String title, String msg) {
    Alert alert = new Alert(Alert.AlertType.INFORMATION);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(msg);
    alert.showAndWait();
}

    


}
