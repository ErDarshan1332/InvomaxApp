package org;

import com.sun.javafx.application.LauncherImpl; // Needed for preloader
import invomax_v0.preloader.Invomax_v0_Preloader;
import javafx.application.Application;
import javafx.application.Preloader;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

public class invomax_v0 extends Application {

    @Override
    public void init() throws Exception {
        // Simulate loading and notify preloader
        for (int i = 1; i <= 100; i++) {
            Thread.sleep(35); // simulate loading
            notifyPreloader(new Preloader.ProgressNotification(i / 100.0));
        }
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("loginemp.fxml"));
            
            Scene scene = new Scene(root);
            primaryStage.setTitle("Invomax Engineering Solution LLP");
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("emp_logo.png")));
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (IOException ex) {
            Logger.getLogger(invomax_v0.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public static void main(String[] args) {
        // Launch application with preloader
        LauncherImpl.launchApplication(invomax_v0.class, Invomax_v0_Preloader.class, args);
    }
}
