package com.project.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;

import com.project.dao.ZadanieDAO;
import com.project.model.Projekt;
import com.project.model.Zadanie;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.Callback;
import javafx.util.StringConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ZadanieController {
    private static final Logger logger = LoggerFactory.getLogger(ZadanieController.class);
    private static final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @FXML
    private Label lblProjektNazwa;
    @FXML
    private Button btnPowrot;
    @FXML
    private Button btnDodaj;
    @FXML
    private TableView<Zadanie> tblZadanie;
    @FXML
    private TableColumn<Zadanie, Integer> colId;
    @FXML
    private TableColumn<Zadanie, String> colNazwa;
    @FXML
    private TableColumn<Zadanie, String> colStatus;
    @FXML
    private TableColumn<Zadanie, LocalDate> colDataRozpoczecia;
    @FXML
    private TableColumn<Zadanie, LocalDate> colDataZakonczenia;
    @FXML
    private TableColumn<Zadanie, String> colOpis;
    private final ExecutorService wykonawca;
    private final ZadanieDAO zadanieDAO;
    private final Projekt projekt;
    private ObservableList<Zadanie> zadania;

    public ZadanieController(Projekt projekt, ZadanieDAO zadanieDAO, ExecutorService wykonawca) {
        this.projekt = projekt;
        this.zadanieDAO = zadanieDAO;
        this.wykonawca = wykonawca;
    }

    @FXML
    public void initialize() {
        lblProjektNazwa.setText("Zadania dla projektu: " + projekt.getNazwa());

        colId.setCellValueFactory(new PropertyValueFactory<>("zadanieId"));
        colNazwa.setCellValueFactory(new PropertyValueFactory<>("nazwa"));
        colStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colOpis.setCellValueFactory(new PropertyValueFactory<>("opis"));

        colDataRozpoczecia.setCellValueFactory(new PropertyValueFactory<>("dataRozpoczecia"));
        colDataZakonczenia.setCellValueFactory(new PropertyValueFactory<>("dataZakonczenia"));
        colDataRozpoczecia.setCellFactory(column -> new TableCell<Zadanie, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(item));
                }
            }
        });

        colDataZakonczenia.setCellFactory(column -> new TableCell<Zadanie, LocalDate>() {
            @Override
            protected void updateItem(LocalDate item, boolean empty) {
                super.updateItem(item, empty);
                if (item == null || empty) {
                    setText(null);
                } else {
                    setText(dateFormatter.format(item));
                }
            }
        });

        TableColumn<Zadanie, Void> colActions = new TableColumn<>("Akcje");
        colActions.setCellFactory(column -> new TableCell<Zadanie, Void>() {
            private final Button btnEdit = new Button("Edytuj");
            private final Button btnDelete = new Button("Usuń");
            private final GridPane pane = new GridPane();

            {
                btnEdit.setMaxWidth(Double.MAX_VALUE);
                btnDelete.setMaxWidth(Double.MAX_VALUE);

                btnEdit.setOnAction(event -> edytujZadanie(getCurrentZadanie()));
                btnDelete.setOnAction(event -> usunZadanie(getCurrentZadanie()));

                pane.setAlignment(Pos.CENTER);
                pane.setHgap(5);
                pane.setVgap(5);
                pane.setPadding(new Insets(5));
                pane.add(btnEdit, 0, 0);
                pane.add(btnDelete, 0, 1);
            }

            private Zadanie getCurrentZadanie() {
                int index = getTableRow().getIndex();
                return getTableView().getItems().get(index);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        tblZadanie.getColumns().add(colActions);

        zadania = FXCollections.observableArrayList();
        tblZadanie.setItems(zadania);

        loadZadania();
    }

    private void loadZadania() {
        wykonawca.execute(() -> {
            try {
                List<Zadanie> zadaniaList = zadanieDAO.getZadaniaByProjekt(projekt.getProjektId());
                Platform.runLater(() -> {
                    zadania.clear();
                    zadania.addAll(zadaniaList);
                });
            } catch (RuntimeException e) {
                String errMsg = "Błąd podczas wczytywania zadań projektu";
                logger.error(errMsg, e);
                Platform.runLater(() -> showError(errMsg, e.getMessage()));
            }
        });
    }

    @FXML
    private void onActionBtnDodaj(ActionEvent event) {
        Zadanie zadanie = new Zadanie();
        zadanie.setProjektId(projekt.getProjektId());
        edytujZadanie(zadanie);
    }

    @FXML
    private void onActionBtnPowrot(ActionEvent event) {
        Stage stage = (Stage) btnPowrot.getScene().getWindow();
        stage.fireEvent(new WindowEvent(stage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    private void edytujZadanie(Zadanie zadanie) {
        Dialog<Zadanie> dialog = new Dialog<>();
        dialog.setTitle("Edycja zadania");

        if (zadanie.getZadanieId() != null) {
            dialog.setHeaderText("Edycja zadania");
        } else {
            dialog.setHeaderText("Dodawanie nowego zadania");
        }

        dialog.setResizable(true);

        Label lblId = new Label("Id:");
        Label lblNazwa = new Label("Nazwa:");
        Label lblOpis = new Label("Opis:");
        Label lblStatus = new Label("Status:");
        Label lblDataRozpoczecia = new Label("Data rozpoczęcia:");
        Label lblDataZakonczenia = new Label("Data zakończenia:");

        lblId.setAlignment(Pos.CENTER_RIGHT);
        lblNazwa.setAlignment(Pos.CENTER_RIGHT);
        lblOpis.setAlignment(Pos.CENTER_RIGHT);
        lblStatus.setAlignment(Pos.CENTER_RIGHT);
        lblDataRozpoczecia.setAlignment(Pos.CENTER_RIGHT);
        lblDataZakonczenia.setAlignment(Pos.CENTER_RIGHT);

        Label txtId = new Label();
        if (zadanie.getZadanieId() != null) {
            txtId.setText(zadanie.getZadanieId().toString());
        }

        TextField txtNazwa = new TextField();
        if (zadanie.getNazwa() != null) {
            txtNazwa.setText(zadanie.getNazwa());
        }

        TextArea txtOpis = new TextArea();
        txtOpis.setPrefRowCount(4);
        txtOpis.setPrefColumnCount(30);
        txtOpis.setWrapText(true);
        if (zadanie.getOpis() != null) {
            txtOpis.setText(zadanie.getOpis());
        }

        ComboBox<String> cbStatus = new ComboBox<>();
        cbStatus.getItems().addAll("Nowe", "W trakcie", "Wstrzymane", "Zakończone", "Anulowane");
        if (zadanie.getStatus() != null) {
            cbStatus.setValue(zadanie.getStatus());
        } else {
            cbStatus.setValue("Nowe");
        }

        DatePicker dpDataRozpoczecia = createDatePicker();
        if (zadanie.getDataRozpoczecia() != null) {
            dpDataRozpoczecia.setValue(zadanie.getDataRozpoczecia());
        } else {
            dpDataRozpoczecia.setValue(LocalDate.now());
        }

        DatePicker dpDataZakonczenia = createDatePicker();
        if (zadanie.getDataZakonczenia() != null) {
            dpDataZakonczenia.setValue(zadanie.getDataZakonczenia());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(10));

        int row = 0;
        grid.add(lblId, 0, row);
        grid.add(txtId, 1, row++);
        grid.add(lblNazwa, 0, row);
        grid.add(txtNazwa, 1, row++);
        grid.add(lblOpis, 0, row);
        grid.add(txtOpis, 1, row++);
        grid.add(lblStatus, 0, row);
        grid.add(cbStatus, 1, row++);
        grid.add(lblDataRozpoczecia, 0, row);
        grid.add(dpDataRozpoczecia, 1, row++);
        grid.add(lblDataZakonczenia, 0, row);
        grid.add(dpDataZakonczenia, 1, row++);

        dialog.getDialogPane().setContent(grid);

        ButtonType buttonTypeOk = new ButtonType("Zapisz", ButtonBar.ButtonData.OK_DONE);
        ButtonType buttonTypeCancel = new ButtonType("Anuluj", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeOk);
        dialog.getDialogPane().getButtonTypes().add(buttonTypeCancel);

        dialog.setResultConverter(new Callback<ButtonType, Zadanie>() {
            @Override
            public Zadanie call(ButtonType buttonType) {
                if (buttonType == buttonTypeOk) {
                    zadanie.setNazwa(txtNazwa.getText().trim());
                    zadanie.setOpis(txtOpis.getText().trim());
                    zadanie.setStatus(cbStatus.getValue());
                    zadanie.setDataRozpoczecia(dpDataRozpoczecia.getValue());
                    zadanie.setDataZakonczenia(dpDataZakonczenia.getValue());

                    if (zadanie.getKolejnosc() == null) {
                        try {
                            // Get count of current tasks to determine next sequence number
                            List<Zadanie> existingZadania = zadanieDAO.getZadaniaByProjekt(projekt.getProjektId());
                            zadanie.setKolejnosc(existingZadania.size() + 1);
                        } catch (Exception e) {
                            zadanie.setKolejnosc(1);
                        }
                    }

                    return zadanie;
                }
                return null;
            }
        });

        Optional<Zadanie> result = dialog.showAndWait();
        if (result.isPresent()) {
            wykonawca.execute(() -> {
                try {
                    zadanieDAO.setZadanie(zadanie);
                    Platform.runLater(() -> {
                        loadZadania();
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas zapisywania zadania";
                    logger.error(errMsg, e);
                    Platform.runLater(() -> showError(errMsg, e.getMessage()));
                }
            });
        }
    }

    private DatePicker createDatePicker() {
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("RRRR-MM-DD");
        datePicker.setConverter(new StringConverter<LocalDate>() {
            @Override
            public String toString(LocalDate date) {
                return date != null ? dateFormatter.format(date) : null;
            }

            @Override
            public LocalDate fromString(String text) {
                return text == null || text.trim().isEmpty() ? null : LocalDate.parse(text, dateFormatter);
            }
        });

        datePicker.getEditor().focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    datePicker.setValue(datePicker.getConverter().fromString(datePicker.getEditor().getText()));
                } catch (DateTimeParseException e) {
                    datePicker.getEditor().setText(datePicker.getConverter().toString(datePicker.getValue()));
                }
            }
        });

        return datePicker;
    }

    private void usunZadanie(Zadanie zadanie) {
        if (zadanie == null || zadanie.getZadanieId() == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Potwierdzenie");
        alert.setHeaderText("Usuwanie zadania");
        alert.setContentText("Czy na pewno chcesz usunąć zadanie: " + zadanie.getNazwa() + "?");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            wykonawca.execute(() -> {
                try {
                    zadanieDAO.deleteZadanie(zadanie.getZadanieId());
                    Platform.runLater(() -> {
                        zadania.remove(zadanie);
                    });
                } catch (RuntimeException e) {
                    String errMsg = "Błąd podczas usuwania zadania";
                    logger.error(errMsg, e);
                    Platform.runLater(() -> showError(errMsg, e.getMessage()));
                }
            });
        }
    }

    private void showError(String header, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Błąd");
        alert.setHeaderText(header);
        alert.setContentText(content);
        alert.showAndWait();
    }
}