package com.project.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.project.dao.ProjektDAO;
import com.project.dao.ZadanieDAO;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.project.model.Projekt;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;

public class ProjectController {
    private static final Logger logger = LoggerFactory.getLogger(ProjectController.class);
    private String search4;
    private Integer pageNo;
    private Integer pageSize;
    private ObservableList<Projekt> projekty;
    private ExecutorService wykonawca;
    private ProjektDAO projektDAO;
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter dateTimeFormater = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @FXML
    private Label lblStrona;
    @FXML
    private ChoiceBox<Integer> cbPageSizes;
    @FXML
    private TableView<Projekt> tblProjekt;
    @FXML
    private TableColumn<Projekt, Integer> colId;
    @FXML
    private TableColumn<Projekt, String> colNazwa;
    @FXML
    private TableColumn<Projekt, String> colOpis;
    @FXML
    private TableColumn<Projekt, LocalDateTime> colDataCzasUtworzenia;
    @FXML
    private TableColumn<Projekt, LocalDate> colDataOddania;
    @FXML
    private TextField txtSzukaj;
    @FXML
    private Button btnDalej;
    @FXML
    private Button btnWstecz;
    @FXML
    private Button btnPierwsza;
    @FXML
    private Button btnOstatnia;

    public ProjectController() {
    }

    private ZadanieDAO zadanieDAO;

    public ProjectController(ProjektDAO projektDAO, ZadanieDAO zadanieDAO) {
        this.projektDAO = projektDAO;
        this.zadanieDAO = zadanieDAO;
        wykonawca = Executors.newFixedThreadPool(1);
    }


    @FXML
    public void initialize() {
        search4 = "";
        pageNo = 0;
        pageSize = 10;
        cbPageSizes.getItems().addAll(5, 10, 20, 50, 100);
        cbPageSizes.setValue(pageSize);


        cbPageSizes.valueProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null && !newValue.equals(oldValue)) {
                pageSize = newValue;
                wykonawca.execute(() -> {
                    loadPage(search4, pageNo, pageSize);
                    updatePageLabel();
                });
            }
        });


        colId.setCellValueFactory(new PropertyValueFactory<Projekt, Integer>("projektId"));
        colNazwa.setCellValueFactory(new PropertyValueFactory<Projekt, String>("nazwa"));
        colOpis.setCellValueFactory(new PropertyValueFactory<Projekt, String>("opis"));
        colDataCzasUtworzenia.setCellValueFactory(new PropertyValueFactory<Projekt, LocalDateTime>
                ("dataCzasUtworzenia"));
        colDataOddania.setCellValueFactory(new PropertyValueFactory<Projekt, LocalDate>("dataOddania"));


        TableColumn<Projekt, Void> colEdit = new TableColumn<>("Edycja");
        colEdit.setCellFactory(column -> new TableCell<Projekt, Void>() {
            private final GridPane pane;

            {
                Button btnEdit = new Button("Edycja");
                Button btnRemove = new Button("Usuń");
                Button btnTask = new Button("Zadania");
                btnEdit.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnRemove.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnTask.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
                btnEdit.setOnAction(event -> {
                    edytujProjekt(getCurrentProjekt());
                });
                btnRemove.setOnAction(event -> {
                    usunProjekt(getCurrentProjekt());
                });
                btnTask.setOnAction(event -> {
                    openZadanieFrame(getCurrentProjekt());
                });
                pane = new GridPane();
                pane.setAlignment(Pos.CENTER);
                pane.setHgap(10);
                pane.setVgap(10);
                pane.setPadding(new Insets(5, 5, 5, 5));
                pane.add(btnTask, 0, 0);
                pane.add(btnEdit, 0, 1);
                pane.add(btnRemove, 0, 2);
            }

            private Projekt getCurrentProjekt() {
                int index = this.getTableRow().getIndex();
                return this.getTableView().getItems().get(index);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        tblProjekt.getColumns().add(colEdit);
        colId.setMaxWidth(5000);
        colNazwa.setMaxWidth(10000);
        colOpis.setMaxWidth(10000);
        colDataCzasUtworzenia.setMaxWidth(9000);
        colDataOddania.setMaxWidth(7000);
        colEdit.setMaxWidth(7000);


        projekty = FXCollections.observableArrayList();
        tblProjekt.setItems(projekty);
        wykonawca.execute(() -> loadPage(search4, pageNo, pageSize));
        colDataCzasUtworzenia.setCellFactory(column -> new TableCell<Projekt, LocalDateTime>() {
            @Override
            protected void updateItem(LocalDateTime item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(dateTimeFormater.format(item));
                }
            }
        });
        updatePageLabel();
    }


    private List<Projekt> getPageData(String search4, Integer pageNo, Integer pageSize) {
        List<Projekt> projektList = new ArrayList<>();
        try {
            if (search4 != null && !search4.isEmpty()) {
                if (search4.matches("[0-9]+")) {
                    Integer id = Integer.parseInt(search4);
                    Projekt projekt = projektDAO.getProjekt(id);
                    if (projekt != null) {
                        projektList.add(projekt);
                    }
                } else if (search4.matches("^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) {
                    LocalDate date = LocalDate.parse(search4, dateFormatter);
                    projektList.addAll(projektDAO.getProjektyWhereDataOddaniaIs(date, pageNo * pageSize, pageSize));
                } else {
                    projektList.addAll(projektDAO.getProjektyWhereNazwaLike(search4, pageNo * pageSize, pageSize));
                }
            } else {
                projektList.addAll(projektDAO.getProjekty(pageNo * pageSize, pageSize));
            }
        } catch (RuntimeException e) {
            String errMsg = "Błąd podczas pobierania listy projektów.";
            logger.error(errMsg, e);
            String errDetails = e.getCause() != null ?
                    e.getMessage() + "\n" + e.getCause().getMessage()
                    : e.getMessage();
            Platform.runLater(() -> showError(errMsg, errDetails));
        }
        return projektList;
    }

    private void loadPage(String search4, Integer pageNo, Integer pageSize) {
        try {
            final List<Projekt> projektList = getPageData(search4, pageNo, pageSize);
            Platform.runLater(() -> {
                projekty.clear();
                projekty.addAll(projektList);
            });
        } catch (RuntimeException e) {
            String errMsg = "Błąd podczas pobierania listy projektów.";
            logger.error(errMsg, e);
            String errDetails = e.getCause() != null ?
                    e.getMessage() + "\n" + e.getCause().getMessage()
                    : e.getMessage();
            Platform.runLater(() -> showError(errMsg, errDetails));
        }
    }


    @FXML
    private void onActionBtnSzukaj(ActionEvent event) {
        search4 = txtSzukaj.getText().trim();
        pageNo = 0; 
        pageSize = cbPageSizes.getValue();
        wykonawca.execute(() -> {
            loadPage(search4, pageNo, pageSize);
            updatePageLabel();
        });
    }

    @FXML
    private void onActionBtnDalej(ActionEvent event) {
        int oldPageNo = pageNo;
        pageNo++;
        pageSize = cbPageSizes.getValue();
        wykonawca.execute(() -> {
            List<Projekt> nextPage = getPageData(search4, pageNo, pageSize);
            if (nextPage.isEmpty()) {
                pageNo = oldPageNo;
                updatePageLabel();
                return;
            }
            Platform.runLater(() -> {
                projekty.clear();
                projekty.addAll(nextPage);
            });
            updatePageLabel();
        });
    }

    @FXML
    private void onActionBtnWstecz(ActionEvent event) {
        if (pageNo > 0) {
            pageNo--;
            pageSize = cbPageSizes.getValue();
            wykonawca.execute(() -> {
                loadPage(search4, pageNo, pageSize);
                updatePageLabel();
            });
        }
    }

    @FXML
    private void onActionBtnPierwsza(ActionEvent event) {
        pageNo = 0;
        pageSize = cbPageSizes.getValue();
        wykonawca.execute(() -> {
            loadPage(search4, pageNo, pageSize);
            updatePageLabel();
        });
    }

    @FXML
    private void onActionBtnOstatnia(ActionEvent event) {
        wykonawca.execute(() -> {
            int totalCount;
            if (search4 != null && !search4.isEmpty()) {
                if (search4.matches("[0-9]+")) {
                    totalCount = 1; 
                } else if (search4.matches("^[0-9]{4}-(0[1-9]|1[0-2])-(0[1-9]|[12][0-9]|3[01])$")) {
                    LocalDate date = LocalDate.parse(search4, dateFormatter);
                    totalCount = projektDAO.getRowsNumberWhereDataOddaniaIs(date);
                } else {
                    totalCount = projektDAO.getRowsNumberWhereNazwaLike(search4);
                }
            } else {
                totalCount = projektDAO.getRowsNumber();
            }

            pageSize = cbPageSizes.getValue();
            int lastPage = (totalCount - 1) / pageSize;
            if (lastPage < 0) lastPage = 0;

            pageNo = lastPage;
            loadPage(search4, pageNo, pageSize);
            updatePageLabel();
        });
    }


    @FXML
    public void onActionBtnDodaj(ActionEvent event) {
        edytujProjekt(new Projekt());
    }


    private void updatePageLabel() {
        Platform.runLater(() -> {
            lblStrona.setText("Strona " + (pageNo + 1));
        });
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }

    public void shutdown() {
        try {
            if (wykonawca != null && !wykonawca.isShutdown()) {
                wykonawca.shutdown();
                if (!wykonawca.awaitTermination(3, TimeUnit.SECONDS)) {
                    wykonawca.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Przerwano zamykanie wątków", e);
        }
    }

    private void edytujProjekt(Projekt projekt) {
        Dialog<Projekt> dialog = new Dialog<>();
        dialog.setTitle("Edycja");
        if (projekt.getProjektId() != null) {
            dialog.setHeaderText("Edycja danych projektu");
        } else {
            dialog.setHeaderText("Dodawanie projektu");
        }
        dialog.setResizable(true);
        Label lblId = getRightLabel("Id: ");
        Label lblNazwa = getRightLabel("Nazwa: ");
        Label lblOpis = getRightLabel("Opis: ");
        Label lblDataCzasUtworzenia = getRightLabel("Data utworzenia: ");
        Label lblDataOddania = getRightLabel("Data oddania: ");
        Label txtId = new Label();
        if (projekt.getProjektId() != null)
            txtId.setText(projekt.getProjektId().toString());
        TextField txtNazwa = new TextField();
        if (projekt.getNazwa() != null)
            txtNazwa.setText(projekt.getNazwa());
        TextArea txtOpis = new TextArea();
        txtOpis.setPrefRowCount(6);
        txtOpis.setPrefColumnCount(40);
        txtOpis.setWrapText(true);
        if (projekt.getOpis() != null)
            txtOpis.setText(projekt.getOpis());
        Label txtDataUtworzenia = new Label();
        if (projekt.getDataCzasUtworzenia() != null)
            txtDataUtworzenia.setText(dateTimeFormater.format(projekt.getDataCzasUtworzenia()));
        DatePicker dtDataOddania = new DatePicker();
        dtDataOddania.setPromptText("RRRR-MM-DD");
        dtDataOddania.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? dateFormatter.format(date) : null;
            }

            @Override
            public LocalDate fromString(String text) {
                return text == null || text.trim().isEmpty() ? null : LocalDate.parse(text, dateFormatter);
            }
        });
        dtDataOddania.getEditor().focusedProperty().addListener((obsValue, oldFocus, newFocus) -> {
            if (!newFocus) {
                try {
                    dtDataOddania.setValue(dtDataOddania.getConverter().fromString(
                            dtDataOddania.getEditor().getText()));
                } catch (DateTimeParseException e) {
                    dtDataOddania.getEditor().setText(dtDataOddania.getConverter()
                            .toString(dtDataOddania.getValue()));
                }
            }
        });
        if (projekt.getDataOddania() != null) {
            dtDataOddania.setValue(projekt.getDataOddania());
        }
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(5, 5, 5, 5));
        grid.add(lblId, 0, 0);
        grid.add(txtId, 1, 0);
        grid.add(lblDataCzasUtworzenia, 0, 1);
        grid.add(txtDataUtworzenia, 1, 1);
        grid.add(lblNazwa, 0, 2);
        grid.add(txtNazwa, 1, 2);
        grid.add(lblOpis, 0, 3);
        grid.add(txtOpis, 1, 3);
        grid.add(lblDataOddania, 0, 4);
        grid.add(dtDataOddania, 1, 4);
        dialog.getDialogPane().setContent(grid);
        ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);
        dialog.setResultConverter(new Callback<ButtonType, Projekt>() {
            @Override
            public Projekt call(ButtonType butonType) {
                if (butonType == buttonTypeOk) {
                    projekt.setNazwa(txtNazwa.getText().trim());
                    projekt.setOpis(txtOpis.getText().trim());
                    projekt.setDataOddania(dtDataOddania.getValue());
                    return projekt;
                }
                return null;
            }
        });
        Optional<Projekt> result = dialog.showAndWait();
        if (result.isPresent()) {
            wykonawca.execute(() -> {
                try {
                    projektDAO.setProjekt(projekt);
                    Platform.runLater(() -> {
                        if (tblProjekt.getItems().contains(projekt)) {
                            tblProjekt.refresh();
                        } else {
                            tblProjekt.getItems().add(0, projekt);
                        }
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas zapisywania danych projektu!";
                    logger.error(errMsg, e);
                    String errDetails = e.getCause() != null ?
                            e.getMessage() + "\n" + e.getCause().getMessage()
                            : e.getMessage();
                    Platform.runLater(() -> showError(errMsg, errDetails));
                }
            });
        }
    }

    private Label getRightLabel(String text) {
        Label lbl = new Label(text);
        lbl.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        lbl.setAlignment(Pos.CENTER_RIGHT);
        return lbl;
    }


    private void usunProjekt(Projekt projekt) {
        if (projekt == null || projekt.getProjektId() == null) return;

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Potwierdzenie");
        confirmDialog.setHeaderText("Usuwanie projektu");
        confirmDialog.setContentText("Czy na pewno chcesz usunąć projekt: " + projekt.getNazwa() + "?");

        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            wykonawca.execute(() -> {
                try {
                    projektDAO.deleteProjekt(projekt.getProjektId());
                    Platform.runLater(() -> {
                        projekty.remove(projekt);
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas usuwania projektu!";
                    logger.error(errMsg, e);
                    String errDetails = e.getCause() != null ?
                            e.getMessage() + "\n" + e.getCause().getMessage()
                            : e.getMessage();
                    Platform.runLater(() -> showError(errMsg, errDetails));
                }
            });
        }
    }

	private void openZadanieFrame(Projekt projekt) {
		try {
			FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ZadanieFrame.fxml"));
			loader.setControllerFactory(param -> {
				if (param == ZadanieController.class) {
					return new ZadanieController(projekt, zadanieDAO, wykonawca);
				}
				return null;
			});
			Parent root = loader.load();
			Stage stage = new Stage();
			stage.setTitle("Zadania projektu: " + projekt.getNazwa());
			Scene scene = new Scene(root);
			stage.setScene(scene);
			stage.show();
		} catch (IOException e) {
			logger.error("Error loading ZadanieFrame.fxml", e);
			showError("Błąd", e.getMessage());
		}
	}


}
