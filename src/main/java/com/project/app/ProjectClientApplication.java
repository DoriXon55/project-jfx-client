package com.project.app;

import com.project.controller.ProjectController;
import com.project.dao.ProjektDAO;
import com.project.dao.ProjektDAOImpl;
import com.project.dao.ZadanieDAO;
import com.project.dao.ZadanieDAOImpl;
import com.project.datasource.DbInitializer;
import javafx.application.Application;
import javafx.application.Platform;
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
	private ProjectController controller;

	public static void main(String[] args) {
		DbInitializer.init();
		DbInitializer.loadTestData();
		launch(ProjectClientApplication.class, args);
	}

	@Override
	public void start(Stage primaryStage) throws Exception {
		ProjektDAO projektDAO = new ProjektDAOImpl();
		ZadanieDAO zadanieDAO = new ZadanieDAOImpl();

		FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ProjectFrame.fxml"));

		loader.setControllerFactory(param -> {
			if (param == ProjectController.class) {
				controller = new ProjectController(projektDAO, zadanieDAO);
				return controller;
			}
			return null;
		});

		root = loader.load();
		primaryStage.setTitle("Projekty");
		Scene scene = new Scene(root);
		scene.getStylesheets().add(getClass().getResource("/css/application.css").toExternalForm());
		primaryStage.setScene(scene);
		primaryStage.sizeToScene();

		primaryStage.setOnCloseRequest(event -> {
			if (controller != null) {
				controller.shutdown();
			}
			Platform.exit();
			System.exit(0);
		});

		primaryStage.show();
	}

	@Override
	public void stop() {
		if (controller != null) {
			controller.shutdown();
		}
		Platform.exit();
		System.exit(0);
	}
}