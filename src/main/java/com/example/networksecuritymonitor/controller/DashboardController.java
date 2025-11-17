package com.example.networksecuritymonitor.controller;

import com.example.networksecuritymonitor.config.SpringContext;
import com.example.networksecuritymonitor.model.TrafficLog;
import com.example.networksecuritymonitor.service.IPBlockerService;
import com.example.networksecuritymonitor.service.LogExportService;
import com.example.networksecuritymonitor.service.MongoDBService;
import org.springframework.beans.factory.annotation.Autowired;

import javafx.animation.Interpolator;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Bounds;
import javafx.scene.chart.PieChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.TableColumn;
import javafx.scene.chart.BarChart;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;

import java.io.*;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import javafx.scene.control.*;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class DashboardController {
    @FXML private TableView<TrafficLog> logsTable;
    @FXML private TableColumn<TrafficLog, String> ipColumn;
    @FXML private TableColumn<TrafficLog, String> urlColumn;
    @FXML private TableColumn<TrafficLog, Integer> statusColumn;
    @FXML private TableColumn<TrafficLog, String> attackColumn;
    @FXML private TableColumn<TrafficLog, String> timestampColumn;
    @FXML private PieChart attackPieChart;
    @FXML private BarChart<String, Number> trafficBarChart;
    @FXML private VBox menuVBox;
    @FXML private Button refreshButton;
    @FXML private Button showChartsButton;
    @FXML private Button exitButton;
    @FXML private StackPane contentPane;
    @FXML private Button blockIpButton;
    @FXML private Button unblockIpButton;
    @FXML private Button updateDurationButton;
    @FXML private ListView<String> blockedIpListView;
    @FXML private Label notificationLabel;
    @FXML private Button refreshBlockedIpsButton;


    @FXML private Label activeThreatsLabel;
    @FXML private Label totalAttacksLabel;
    @FXML private Label blockedIpsLabel;

    @FXML private ComboBox<String> chartSelectorComboBox;
    @FXML private BarChart<String, Number> trafficPerMinuteChart;
    @FXML private BarChart<String, Number> statusCodeChart;
    @FXML private StackPane chartStackPane;

    @FXML private Label csvExportStatus;

    @FXML private VBox tipsPopupContainer;
    @FXML private Label tipsHeaderLabel;
    @FXML private ListView<String> tipsListView;
    @FXML private Button closeTipsButton;
    @FXML private Button saveTipsButton;
    @FXML private HBox tipsButtonContainer;
    @FXML private Label tipsContentLabel;

    @FXML private TextArea tipsContentArea;

    @FXML private Label attackTypeLabel;
    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    private ObservableList<String> blockedIps = FXCollections.observableArrayList();

    private final MongoDBService mongoDBService;
    private final IPBlockerService ipBlockerService;

    public DashboardController(MongoDBService mongoDBService, IPBlockerService ipBlockerService) {
        this.mongoDBService = mongoDBService;
        this.ipBlockerService = ipBlockerService;
    }

    @FXML
    public void initialize() {
        setupTableColumns();
        loadLogs();
        showCharts();
        blockedIpListView.setItems(blockedIps);

        refreshBlockedIpsUI();
        startPeriodicCheck();

        scheduler.scheduleAtFixedRate(() -> Platform.runLater(this::refreshBlockedIpsUI), 0, 20, TimeUnit.SECONDS);

        logsTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        timestampColumn.setSortType(TableColumn.SortType.DESCENDING);
        logsTable.getSortOrder().add(timestampColumn);

        refreshButton.setOnAction(e -> refreshLogs());
        showChartsButton.setOnAction(e -> showCharts());
        exitButton.setOnAction(e -> exitApplication());
        blockIpButton.setOnAction(event -> blockManualIP());
        unblockIpButton.setOnAction(event -> unblockManualIP());
        refreshBlockedIpsButton.setOnAction(e -> refreshBlockedIpsUI());


        trafficPerMinuteChart.setVisible(false);
        trafficPerMinuteChart.setManaged(false);
        statusCodeChart.setVisible(false);
        statusCodeChart.setManaged(false);

        chartSelectorComboBox.setOnAction(event -> {
            String selected = chartSelectorComboBox.getValue();
            if (selected.equals("Trafic")) {
                trafficPerMinuteChart.setVisible(true);
                trafficPerMinuteChart.setManaged(true);
                statusCodeChart.setVisible(false);
                statusCodeChart.setManaged(false);
            } else {
                statusCodeChart.setVisible(true);
                statusCodeChart.setManaged(true);
                trafficPerMinuteChart.setVisible(false);
                trafficPerMinuteChart.setManaged(false);
            }
        });

        setupBlockedIpClickListener();

        tipsPopupContainer.setVisible(false);
        tipsPopupContainer.setManaged(false);
    }

    private void refreshBlockedIpsUI() {
        List<String> ips = this.ipBlockerService.getBlockedIPsFromDB();
        Platform.runLater(() -> {
            blockedIps.setAll(ips); // blockedIps e ObservableList<String>
        });
    }

    private void setupTableColumns() {
        ipColumn.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        urlColumn.setCellValueFactory(new PropertyValueFactory<>("url"));
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("statusCode"));
        attackColumn.setCellValueFactory(new PropertyValueFactory<>("attackType"));
        timestampColumn.setCellValueFactory(new PropertyValueFactory<>("timestamp"));
    }
    @FXML
    private void refreshLogs() {
        System.out.println("Refresh date... <3");
        loadLogs();
    }

    private String getPastelColor(int index) {
        String[] pastelColors = {"#FF9AA2", "#FFB7B2", "#FFDAC1", "#E2F0CB", "#B5EAD7", "#C7CEEA"};

        return pastelColors[index % pastelColors.length];
    }
    @FXML
    public void showCharts() {
        List<TrafficLog> logs = mongoDBService.getAllLogs();

        //pie
        Map<String, Long> attackCounts = logs.stream()
                .collect(Collectors.groupingBy(TrafficLog::getAttackType, Collectors.counting()));

        ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
        int colorIndex = 0;
        for (Map.Entry<String, Long> entry : attackCounts.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            int finalColorIndex = colorIndex;
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.setStyle("-fx-pie-color: " + getPastelColor(finalColorIndex) + ";");
            });
            pieChartData.add(data);
            colorIndex++;
        }
        attackPieChart.setData(pieChartData);
        attackPieChart.setTitle("Distributia atacurilor detectate");
        attackPieChart.setLegendVisible(false);

        // Chart 1: Traffic
        trafficPerMinuteChart.getData().clear();
        XYChart.Series<String, Number> trafficSeries = new XYChart.Series<>();
        trafficSeries.setName("Trafic / ora");

        Map<String, Long> trafficByHour = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> DateTimeFormatter.ofPattern("HH")
                                .format(LocalDateTime.ofInstant(
                                        log.getTimestamp(), java.time.ZoneId.systemDefault())),
                        TreeMap::new,
                        Collectors.counting()
                ));

        colorIndex = 0;
        for (Map.Entry<String, Long> entry : trafficByHour.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey(), entry.getValue());
            int finalColorIndex = colorIndex;
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.setStyle("-fx-bar-fill: " + getPastelColor(finalColorIndex) + ";");
            });
            trafficSeries.getData().add(data);
            colorIndex++;
        }
        trafficPerMinuteChart.getData().add(trafficSeries);
        trafficPerMinuteChart.setTitle("Trafic");

        // Chart 2: Status codes
        statusCodeChart.getData().clear();
        XYChart.Series<String, Number> statusSeries = new XYChart.Series<>();
        statusSeries.setName("Coduri HTTP");

        Map<Integer, Long> statusCounts = logs.stream()
                .collect(Collectors.groupingBy(TrafficLog::getStatusCode, Collectors.counting()));

        colorIndex = 0;
        for (Map.Entry<Integer, Long> entry : statusCounts.entrySet()) {
            XYChart.Data<String, Number> data = new XYChart.Data<>(entry.getKey().toString(), entry.getValue());
            int finalColorIndex = colorIndex;
            data.nodeProperty().addListener((obs, oldNode, newNode) -> {
                if (newNode != null) newNode.setStyle("-fx-bar-fill: " + getPastelColor(finalColorIndex) + ";");
            });
            statusSeries.getData().add(data);
            colorIndex++;
        }
        statusCodeChart.getData().add(statusSeries);
        statusCodeChart.setTitle("Frecvență Coduri HTTP");
    }

    @FXML
    private void exitApplication() {
        System.out.println("Exiting application... ;( ");
        IPBlockerService.shutdownScheduler();
        Platform.exit();
    }
    private void loadLogs() {
        List<TrafficLog> logs = mongoDBService.getAllLogs();
        ObservableList<TrafficLog> observableLogs = FXCollections.observableArrayList(logs);
        logsTable.setItems(observableLogs);

        long activeThreats = logs.stream().filter(l -> !l.getAttackType().equalsIgnoreCase("Normal")).count();
        long blockedAttacks = logs.stream().filter(l -> l.getStatusCode() >= 400).count();

        activeThreatsLabel.setText(String.valueOf(activeThreats));
        totalAttacksLabel.setText(String.valueOf(blockedAttacks));
        blockedIpsLabel.setText(String.valueOf(blockedIps.size()));
    }

    public void stopScheduler() {
        if (scheduler != null) {
            scheduler.shutdown(); // optreste scheduler fortat sau nu
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
            }
        }
    }

    private boolean sendPostRequestToBlocker(String endpoint, String ip) {
        try {
            URL url = new URL("http://localhost:8081/api/" + endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/json");

            String credentials = "bianca:parola";
            String encodedCredentials = Base64.getEncoder().encodeToString(credentials.getBytes(StandardCharsets.UTF_8));
            conn.setRequestProperty("Authorization", "Basic " + encodedCredentials);

            conn.setDoOutput(true);
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);

            String jsonInputString = "{\"ip\":\"" + ip + "\"}";
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            System.out.println("Response Code pentru " + endpoint + ": " + responseCode);

            InputStream inputStream = (responseCode >= 200 && responseCode < 300)
                    ? conn.getInputStream()
                    : conn.getErrorStream();

            if (inputStream != null) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    System.out.println("Response pentru " + endpoint + ": " + response);
                }
            } else {
                System.out.println("No response stream available for " + endpoint);
            }

            if (responseCode == 401) {
                Platform.runLater(() -> showAlert("Eroare de autentificare",
                        "Serverul necesită autentificare. Verifică username/parola."));
                return false;
            }

            return responseCode == 200;

        } catch (ConnectException e) {
            System.err.println("Nu se poate conecta la Spring Boot server (8081). Verifică dacă rulează.");
            Platform.runLater(() -> showAlert("Eroare de conexiune",
                    "Nu se poate conecta la serverul de management (port 8081). Verifică dacă aplicația Spring Boot rulează."));
            return false;

        } catch (IOException e) {
            System.err.println("Cererea HTTP a eșuat: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    @FXML
    private void blockManualIP() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Blocare IP");
        dialog.setHeaderText("Introduceți IP-ul pe care doriți să îl blocați:");
        dialog.setContentText("IP:");

        Optional<String> result = dialog.showAndWait();
        result.ifPresent(ip -> {
            if (!isValidIP(ip)) {
                showAlert("Eroare", "IP-ul introdus nu este valid");
                return;
            }

            boolean success = sendPostRequestToBlocker("block-ip", ip);
            if (success) {
                refreshBlockedIpsUI();
                showAlert("Succes", "IP-ul " + ip + " a fost blocat.");
            } else {
                showAlert("Eroare", "Nu s-a putut bloca IP-ul.");
            }
        });
    }

    @FXML
    private void unblockManualIP() {
        String selectedIp = blockedIpListView.getSelectionModel().getSelectedItem();
        if (selectedIp != null) {
            boolean success = sendPostRequestToBlocker("unblock-ip", selectedIp);
            if (success) {
                refreshBlockedIpsUI();
                showNotification("IP DEBLOCAT: " + selectedIp + " a fost deblocat.");
            } else {
                showAlert("Eroare", "Nu s-a putut debloca IP-ul.");
            }
        } else {
            showAlert("Eroare", "Selectați un IP pentru deblocare.");
        }
    }

    @FXML
    private void updateBlockDurationForIP() {
        String selectedIp = blockedIpListView.getSelectionModel().getSelectedItem();
        if (selectedIp != null) {
            TextInputDialog dialog = new TextInputDialog("");
            dialog.setTitle("Actualizează durata blocării");
            dialog.setHeaderText("Introduceți noua durată (în secunde):");
            dialog.setContentText("Durată:");

            Optional<String> result = dialog.showAndWait();
            result.ifPresent(value -> {
                try {
                    int newDuration = Integer.parseInt(value);
                    this.ipBlockerService.updateBlockDuration(selectedIp, newDuration);
                    showAlert("Succes", "Durata blocării a fost actualizată.");
                } catch (NumberFormatException e) {
                    showAlert("Eroare", "Introduceți o durată validă (număr întreg).");
                }
            });
        } else {
            showAlert("Eroare", "Selectați un IP pentru actualizare.");
        }
    }

    @FXML
    private void exportCSVClicked() {
        List<TrafficLog> logs = mongoDBService.getAllLogs();

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvează ca CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(null);

        if (file != null) {
            try (PrintWriter writer = new PrintWriter(file)) {
                writer.println("IP Address,URL,Attack Type,Timestamp,Status");

                for (TrafficLog log : logs) {
                    writer.printf("%s,%s,%s,%s,%s%n",
                            log.getIpAddress(),
                            log.getUrl(),
                            log.getAttackType(),
                            log.getTimestamp().toString(),
                            log.getStatus());
                }

                showAlert("Export reușit", "Fișierul CSV a fost salvat cu succes.");
            } catch (IOException e) {
                showAlert("Eroare", "A apărut o eroare la export: " + e.getMessage());
            }
        }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }


    private void startPeriodicCheck() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.minutes(2), event -> {
            int count = blockedIps.size();
            if (count > 0) {
                showNotification("IP-uri blocate: " + count);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    private void showNotification(String message) {
        notificationLabel.setText(message);
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(5), event -> notificationLabel.setText("")));
        timeline.setCycleCount(1);
        timeline.play();
    }

    private boolean isValidIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) {
            return false;
        }

        String[] parts = ip.trim().split("\\.");
        if (parts.length != 4) {
            return false;
        }

        try {
            for (String part : parts) {
                int num = Integer.parseInt(part);
                if (num < 0 || num > 255) {
                    return false;
                }
            }
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    private boolean verifyIPIsBlocked(String ip) {
        try {
            ProcessBuilder pb = new ProcessBuilder("sudo", "iptables", "-L", "INPUT", "-v", "-n");
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream())
            );

            String output = reader.lines()
                    .collect(Collectors.joining("\n"));

            int exitCode = process.waitFor();
            reader.close();

            if (exitCode == 0) {
                boolean found = output.contains(ip + " ");
                System.out.println("Verificare iptables pentru IP " + ip + ": " + (found ? "GĂSIT" : "NU GĂSIT"));
                if (!found) {
                    System.out.println("Output iptables:\n" + output);
                }
                return found;
            } else {
                System.err.println("Eroare la verificarea iptables, exit code: " + exitCode);
                return false;
            }

        } catch (IOException | InterruptedException e) {
            System.err.println("Eroare la verificarea regulii iptables: " + e.getMessage());
            return false;
        }
    }

    private Map<String, List<String>> securityTips = new HashMap<>() {{
        put("SQL Injection", Arrays.asList(
                "Folosește prepared statements și parametri bound pentru toate query-urile",
                "Validează rigoroas toate input-urile utilizatorilor",
                "Limitează privilegiile bazei de date",
                "Implementează logging detaliat pentru detectarea tentativelor SQLi",
                "Activează WAF cu reguli specifice anti-SQL injection"
        ));

        put("XSS", Arrays.asList(
                "Implementează Content Security Policy (CSP) headers stricte",
                "Validează și filtrează toate input-urile pe server-side",
                "Folosește librării de templating cu auto-escaping activat",
                "Implementează whitelist strict pentru tag-urile HTML permise"
        ));

        put("Brute Force", Arrays.asList(
                "Implementează rate limiting agresiv per IP și per cont utilizator",
                "Obligă utilizarea autentificării cu doi factori (2FA/MFA)",
                "Blochează automat IP-urile după 3-5 tentative eșuate consecutive",
                "Monitorizează în timp real pattern-urile de login suspicioase",
                "mplementează CAPTCHA după prima tentativă eșuată"
        ));

        put("DDoS", Arrays.asList(
                "Implementează rate limiting la nivel de aplicație și rețea",
                "Instalează sisteme de monitorizare trafic în timp real (ELK Stack)",
                "Optimizează firewall-ul cu reguli specifice anti-DDoS"
        ));

        put("Directory Traversal", Arrays.asList(
                "Validează strict și sanitizează toate path-urile de fișiere",
                "Blochează complet caracterele speciale (../, \\, %2e%2e%2f)",
                "Creează whitelist explicit pentru fișierele și directoarele accesibile",
                "Verifică că path-ul final este în zona sigură",
                "Rulează aplicația cu privilegii minime (non-root user)"
        ));

        put("Normal", Arrays.asList(
                "Implementează monitorizare continuă a traficului pentru detectarea anomaliilor",
                "Configurează alerting în timp real pentru comportamente suspicioase",
                "Efectuează backup-uri automate și testează procedurile de restore",
                "Verifica zilnic log-urile de securitate și trafic"
        ));
    }};

    private Random random = new Random();

    private void setupBlockedIpClickListener() {
        blockedIpListView.setOnMouseClicked(event -> {
            String selectedIp = blockedIpListView.getSelectionModel().getSelectedItem();
            if (selectedIp != null) {
                String attackType = findAttackTypeForIP(selectedIp);
                showRandomTipPopup(selectedIp, attackType);
            }
        });

        blockedIpListView.sceneProperty().addListener((observable, oldValue, newScene) -> {
            if (newScene != null) {
                newScene.setOnMouseClicked(event -> {
                    if (tipsPopupContainer.isVisible() && !isClickInsidePopup(event)) {
                        hideSimpleTipsPopup();
                    }
                });
            }
        });
    }

    private boolean isClickInsidePopup(MouseEvent event) {
        if (!tipsPopupContainer.isVisible()) return false;

        Bounds popupBounds = tipsPopupContainer.localToScene(tipsPopupContainer.getBoundsInLocal());
        return popupBounds.contains(event.getSceneX(), event.getSceneY());
    }

    private void showRandomTipPopup(String ip, String attackType) {
        List<String> tips = getSecurityTipsForAttack(attackType);
        String randomTip = tips.get(random.nextInt(tips.size()));

        attackTypeLabel.setText(attackType);
        tipsContentLabel.setText(randomTip);

        Text text = new Text(randomTip);
        text.setFont(Font.font("System", 12));
        text.setWrappingWidth(250);

        Text attackText = new Text(attackType);
        attackText.setFont(Font.font("System", FontWeight.BOLD, 11));

        double contentHeight = text.getLayoutBounds().getHeight();
        double attackHeight = attackText.getLayoutBounds().getHeight();
        double totalHeight = contentHeight + attackHeight + 25;

        tipsPopupContainer.setPrefSize(270, totalHeight);

        animateSimplePopupShow();
        tipsPopupContainer.setVisible(true);
        tipsPopupContainer.setManaged(true);
    }

    private void hideSimpleTipsPopup() {
        animateSimplePopupHide();

        Timeline hideTimeline = new Timeline(new KeyFrame(Duration.millis(400), e -> {
            tipsPopupContainer.setVisible(false);
            tipsPopupContainer.setManaged(false);
        }));
        hideTimeline.play();
    }

    private void animateSimplePopupShow() {
        tipsPopupContainer.setScaleX(0.7);
        tipsPopupContainer.setScaleY(0.7);
        tipsPopupContainer.setOpacity(0);

        Timeline showAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(tipsPopupContainer.scaleXProperty(), 0.7),
                        new KeyValue(tipsPopupContainer.scaleYProperty(), 0.7),
                        new KeyValue(tipsPopupContainer.opacityProperty(), 0)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(tipsPopupContainer.scaleXProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(tipsPopupContainer.scaleYProperty(), 1.0, Interpolator.EASE_OUT),
                        new KeyValue(tipsPopupContainer.opacityProperty(), 1.0)
                )
        );
        showAnimation.play();
    }

    private void animateSimplePopupHide() {
        Timeline hideAnimation = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(tipsPopupContainer.scaleXProperty(), 1.0),
                        new KeyValue(tipsPopupContainer.scaleYProperty(), 1.0),
                        new KeyValue(tipsPopupContainer.opacityProperty(), 1.0)
                ),
                new KeyFrame(Duration.millis(300),
                        new KeyValue(tipsPopupContainer.scaleXProperty(), 0.7, Interpolator.EASE_IN),
                        new KeyValue(tipsPopupContainer.scaleYProperty(), 0.7, Interpolator.EASE_IN),
                        new KeyValue(tipsPopupContainer.opacityProperty(), 0)
                )
        );
        hideAnimation.play();
    }

    private List<String> getSecurityTipsForAttack(String attackType) {
        return securityTips.getOrDefault(attackType, securityTips.get("Normal"));
    }

    private String findAttackTypeForIP(String ip) {
        List<TrafficLog> logs = mongoDBService.getAllLogs();

        Optional<TrafficLog> latestLog = logs.stream()
                .filter(log -> log.getIpAddress().equals(ip))
                .filter(log -> !log.getAttackType().equalsIgnoreCase("Normal"))
                .max(Comparator.comparing(TrafficLog::getTimestamp));

        return latestLog.map(TrafficLog::getAttackType).orElse("Normal");
    }
}
