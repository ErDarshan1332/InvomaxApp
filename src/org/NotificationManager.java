package org;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.time.LocalTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;


public class NotificationManager {

    private ScheduledExecutorService scheduler;
    private final StageProvider stageProvider; // dashboard stage मिळवण्यासाठी

    public NotificationManager(StageProvider stageProvider) {
        this.stageProvider = stageProvider;
    }

    // --- Slots define ---
    private final LocalTime[][] slots = {
            {LocalTime.of(10,30), LocalTime.of(10,45), LocalTime.of(11,00)},   // Morning Tea
            {LocalTime.of(12,30), LocalTime.of(12,45), LocalTime.of(13,00)},  // Lunch
            {LocalTime.of(15,30), LocalTime.of(15,45), LocalTime.of(16,00)},   // Evening Tea
            {LocalTime.of(17,30), LocalTime.of(17,45), LocalTime.of(18,00)}
    };

    private final String[] messages = {
            "☕ It's Morning Tea Time!",
            "🍽️ It's Lunch Time!",
            "☕ It's Evening Tea Time!",
            "☕ It's Time To Pack-Up On 6:00 PM !"
    };

    public void start() {
        scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(this::checkAndNotify, 0, 1, TimeUnit.MINUTES);
    }

    private void checkAndNotify() {
        LocalTime now = LocalTime.now().withSecond(0).withNano(0);

        for (int i = 0; i < slots.length; i++) {
            for (LocalTime notifyTime : slots[i]) {
                if (now.equals(notifyTime)) {
                    int index = i;
                    sendNotification(messages[index]);
                }
            }
        }
    }

    private void sendNotification(String message) {
        Platform.runLater(() -> {
            if (stageProvider.getStage().isIconified()) {
                showSystemTrayNotification(message);
            } else {
                showAlert(message);
            }
        });
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Break Reminder");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
//        try {
//            
//            Image customIcon = new Image(getClass().getResourceAsStream("emp_logo.png")); 
//            ImageView customIconView = new ImageView(customIcon);
//            customIconView.setFitWidth(48); 
//            customIconView.setFitHeight(48); 
//            alert.setGraphic(customIconView);
//        } catch (NullPointerException e) {
//            System.err.println("Error loading emp_logo.png for alert graphic. Make sure it's in the correct resource path.");
//            
//        }
    }

    private void showSystemTrayNotification(String message) {
    try {
        if (SystemTray.isSupported()) {
            SystemTray tray = SystemTray.getSystemTray();
            TrayIcon trayIcon;

            if (tray.getTrayIcons().length == 0) {
                java.awt.Image image = Toolkit.getDefaultToolkit().createImage(
                        getClass().getResource("emp_logo.png") // resources मधला icon
                );
                trayIcon = new TrayIcon(image, "App Notifications");
                trayIcon.setImageAutoSize(true);
                tray.add(trayIcon);
            } else {
                trayIcon = tray.getTrayIcons()[0];
            }

            trayIcon.displayMessage("Break Reminder", message, TrayIcon.MessageType.INFO);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
}

    public void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
    }

    // --- Stage मिळवण्यासाठी interface ---
    public interface StageProvider {
        javafx.stage.Stage getStage();
    }
}