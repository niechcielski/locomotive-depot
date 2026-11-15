package parowozownia;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;

public class ParowozowniaApp extends Application {

    private int currentNumSpots;
    private int currentNumLocos;
    private volatile boolean running = true;
    private final List<Thread> simulationThreads = new ArrayList<>();

    private DepotManager depotManager;
    private final List<Locomotive> locomotives = new ArrayList<>();
    private Boss boss;

    private FlowPane baysContainer;
    private VBox accessTracksContainer;
    private VBox waitingContainer;
    private FlowPane activeTracksContainer;
    private Label bossStatusLabel;
    private HBox topPanel;

    @Override
    public void start(Stage primaryStage) {
        ConfigManager.loadConfig();
        currentNumSpots = ConfigManager.getDepotSpotsCount();
        currentNumLocos = ConfigManager.getLocomotivesCount();
        depotManager = new DepotManager(currentNumSpots);

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f0f0;");

        topPanel = new HBox(10);
        topPanel.setPadding(new Insets(8));
        topPanel.setStyle("-fx-background-color: #dddddd; -fx-border-color: #aaaaaa; -fx-border-width: 1;");
        topPanel.setAlignment(Pos.CENTER_LEFT);

        Label bossBadge = new Label("SZEF:");
        bossBadge.setStyle("-fx-font-weight: bold;");
        bossStatusLabel = new Label("Inicjalizacja...");
        bossStatusLabel.setStyle("-fx-font-size: 13px;");

        topPanel.getChildren().addAll(bossBadge, bossStatusLabel);
        root.setTop(topPanel);

        VBox leftPanel = new VBox(10);
        leftPanel.setPadding(new Insets(15, 15, 15, 0));
        leftPanel.setPrefWidth(180);

        Label configTitle = new Label("Konfiguracja:");
        configTitle.setStyle("-fx-font-weight: bold;");

        TextField spotsField = new TextField(String.valueOf(currentNumSpots));
        TextField locosField = new TextField(String.valueOf(currentNumLocos));

        Button applyBtn = new Button("Zastosuj");
        applyBtn.setMaxWidth(Double.MAX_VALUE);
        applyBtn.setOnAction(e -> {
            try {
                int nSpots = Integer.parseInt(spotsField.getText().trim());
                int nLocos = Integer.parseInt(locosField.getText().trim());
                if (nSpots > 0 && nLocos > 0) reconfigure(nSpots, nLocos);
            } catch (Exception ignored) {}
        });

        leftPanel.getChildren().addAll(configTitle, new Label("Boksy:"), spotsField, new Label("Lokomotywy:"), locosField, applyBtn);
        root.setLeft(leftPanel);

        VBox centerPanel = new VBox(15);
        centerPanel.setPadding(new Insets(15, 0, 0, 0));

        Label tracksTitle = new Label("Tory dojazdowe:");
        tracksTitle.setStyle("-fx-font-weight: bold;");

        accessTracksContainer = new VBox(5);
        accessTracksContainer.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #aaaaaa; -fx-padding: 8;");

        Label baysTitle = new Label("Boksy naprawcze:");
        baysTitle.setStyle("-fx-font-weight: bold;");

        baysContainer = new FlowPane();
        baysContainer.setHgap(10);
        baysContainer.setVgap(10);

        centerPanel.getChildren().addAll(tracksTitle, accessTracksContainer, baysTitle, baysContainer);
        root.setCenter(centerPanel);

        VBox rightPanel = new VBox(10);
        rightPanel.setPadding(new Insets(15, 0, 0, 15));
        rightPanel.setPrefWidth(220);

        Label waitingTitle = new Label("Czekające przed nastawnią:");
        waitingTitle.setStyle("-fx-font-weight: bold;");

        waitingContainer = new VBox(5);
        waitingContainer.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #aaaaaa; -fx-padding: 5; -fx-min-height: 200;");

        rightPanel.getChildren().addAll(waitingTitle, waitingContainer);
        root.setRight(rightPanel);
        VBox bottomPanel = new VBox(10);
        bottomPanel.setPadding(new Insets(15, 0, 0, 0));

        Label routeTitle = new Label("Lokomotywy na szlaku:");
        routeTitle.setStyle("-fx-font-weight: bold;");

        activeTracksContainer = new FlowPane();
        activeTracksContainer.setHgap(8);
        activeTracksContainer.setVgap(8);
        activeTracksContainer.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #aaaaaa; -fx-padding: 8;");

        bottomPanel.getChildren().addAll(routeTitle, activeTracksContainer);
        root.setBottom(bottomPanel);

        Scene scene = new Scene(root, 1150, 650);
        primaryStage.setTitle("Parowozownia");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(e -> shutdown());
        primaryStage.show();

        startSimulation();

        Thread uiUpdater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100);
                    if (running) Platform.runLater(this::updateUI);
                } catch (InterruptedException e) { break; }
            }
        });
        uiUpdater.setDaemon(true);
        uiUpdater.start();
    }

    private synchronized void reconfigure(int newSpots, int newLocos) {
        running = false;
        for (Thread t : simulationThreads) t.interrupt();
        simulationThreads.clear();
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        currentNumSpots = newSpots;
        currentNumLocos = newLocos;
        depotManager = new DepotManager(currentNumSpots);
        locomotives.clear();

        running = true;
        startSimulation();
    }

    private void startSimulation() {
        boss = new Boss(depotManager);
        Thread bossThread = new Thread(boss, "Kierownik");
        bossThread.setDaemon(true);
        bossThread.start();
        simulationThreads.add(bossThread);

        for (int i = 1; i <= currentNumLocos; i++) {
            Locomotive loco = new Locomotive(i, depotManager);
            locomotives.add(loco);
            Thread t = new Thread(loco, "Lokomotywa-" + i);
            t.setDaemon(true);
            t.start();
            simulationThreads.add(t);
        }
    }

    private void shutdown() {
        running = false;
        for (Thread t : simulationThreads) t.interrupt();
        Platform.exit();
        System.exit(0);
    }

    private void updateUI() {
        if (!running) return;

        String bossState = boss.getStatus();
        bossStatusLabel.setText(bossState);
        boolean isRemont = depotManager.isRenovationInProgress() || depotManager.isRenovationIntent();

        if (isRemont) {
            topPanel.setStyle("-fx-background-color: #ffcccc; -fx-border-color: #cc0000; -fx-border-width: 1;");
        } else {
            topPanel.setStyle("-fx-background-color: #dddddd; -fx-border-color: #aaaaaa; -fx-border-width: 1;");
        }

        accessTracksContainer.getChildren().clear();
        for (int i = 0; i < 2; i++) {
            Locomotive loc = depotManager.getLocomotiveOnTrack(i);
            Label trackLabel = new Label("Tor " + (i + 1) + ": " + (loc != null ? "Loko #" + loc.getLocoId() : "[wolny]"));
            trackLabel.setStyle("-fx-background-color: " + (loc != null ? "#cccccc" : "#f0f0f0") + "; -fx-padding: 6;");
            trackLabel.setMaxWidth(Double.MAX_VALUE);
            accessTracksContainer.getChildren().add(trackLabel);
        }

        baysContainer.getChildren().clear();
        for (int i = 0; i < currentNumSpots; i++) {
            Locomotive loco = depotManager.getLocomotiveAtSpot(i);
            VBox box = new VBox(5);
            box.setPadding(new Insets(10));
            box.setPrefSize(140, 80);
            box.setAlignment(Pos.CENTER);

            Label title = new Label("Boks " + (i + 1));
            title.setStyle("-fx-font-weight: bold;");
            box.getChildren().add(title);

            if (isRemont) {
                box.setStyle("-fx-background-color: #ffcccc; -fx-border-color: #cc0000; -fx-border-width: 1;");
                Label lockLabel = new Label("REMONT");
                lockLabel.setStyle("-fx-font-size: 10px;");
                box.getChildren().add(lockLabel);
            } else if (loco == null) {
                box.setStyle("-fx-background-color: #e8e8e8; -fx-border-color: #aaaaaa;");
                Label emptyLabel = new Label("[ pusty ]");
                emptyLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #888888;");
                box.getChildren().add(emptyLabel);
            } else {
                box.setStyle("-fx-background-color: #ccddcc; -fx-border-color: #555555; -fx-border-width: 1;");
                Label idLabel = new Label("Loko #" + loco.getLocoId());
                box.getChildren().add(idLabel);
            }
            baysContainer.getChildren().add(box);
        }

        waitingContainer.getChildren().clear();
        List<Locomotive> queue = depotManager.getWaitingQueue();
        for (Locomotive loco : queue) {
            Label qLabel = new Label(" Loko #" + loco.getLocoId() + " (czeka)");
            qLabel.setStyle("-fx-background-color: #dddddd; -fx-padding: 4;");
            qLabel.setMaxWidth(Double.MAX_VALUE);
            waitingContainer.getChildren().add(qLabel);
        }

        activeTracksContainer.getChildren().clear();
        synchronized (locomotives) {
            for (Locomotive loco : locomotives) {
                if (loco.getStateDescription().contains("szlaku")) {
                    Label trackLabel = new Label("Loko-" + loco.getLocoId());
                    trackLabel.setStyle("-fx-background-color: #dddddd; -fx-border-color: #aaaaaa; -fx-padding: 3 7;");
                    activeTracksContainer.getChildren().add(trackLabel);
                }
            }
        }
    }
}