package com.project.app;

import com.project.controller.ProjectController;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.dao.ZadanieDAO;
import com.project.dao.ZadanieDAOImpl;
import com.project.datasource.DbInitializer;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProjectClientApplication extends Application {
    private static final Logger logger = LoggerFactory.getLogger(ProjectClientApplication.class);
    private FXMLLoader loader;
    private Parent root;

    public static void main(String[] args) {
        DbInitializer.init();
		launch(ProjectClientApplication.class, args);
    }

	@Override
	public void start(Stage primaryStage) throws Exception {
		ProjektDAO projektDAO = new ProjektDAOImpl();
		ZadanieDAO zadanieDAO = new ZadanieDAOImpl();

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectFrame.fxml"));

		loader.setControllerFactory(param -> {
			if (param == ProjectController.class) {
				return new ProjectController(projektDAO, zadanieDAO);
			}
			return null;
		});

		root = loader.load();
		primaryStage.setTitle("Projekty");
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();
		primaryStage.show();
	}
	
}