package com.life;

import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class life extends Application {

    private Stage primaryStage;

    private Thread simulationThread;
    private volatile boolean running = false;

    private static int rows = 40;
    private static int cols = 40;
    private static int cellSize = 20;
    private static int delay = 500;

    private static int[][] initial;
    private int[][] originalState = null;

    //survive min/ survive max/ birth
    private static int[] cell1rules = {2, 3, 3};

    private int[][] map = new int[rows][cols];
    private int[][] buffer = new int[rows][cols];

    private Canvas canvas;

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        openSetup();
    }

    private void openSetup() {
        Stage setupStage = new Stage();
        setupStage.setTitle("Setup");

        TextField rowsField = new TextField(String.valueOf(rows));
        TextField colsField = new TextField(String.valueOf(cols));
        TextField delayField = new TextField(String.valueOf(delay));

        Button startButton = new Button("Start");
        Button updateButton = new Button("Update grid");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(1);
        gridPane.setVgap(1);
        gridPane.setPadding(new Insets(10));


        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setPrefSize(865, 865);

        if (initial == null || initial.length != rows || initial[0].length != cols) {
            initial = new int[rows][cols];
        }

        generateGrid(gridPane);

        updateButton.setOnAction(event -> {
           try {
               int newRows = Integer.parseInt(rowsField.getText());
               int newCols = Integer.parseInt(colsField.getText());

               if(newRows <= 0 || newCols <= 0) {
                   throw new NumberFormatException();
               }

               int[][] newInitial = new int[newRows][newCols];

               for(int row = 0; row < Math.min(rows, newRows); row++) {
                   for(int col = 0; col < Math.min(cols, newCols); col++) {
                       newInitial[row][col] = initial[row][col];
                   }
               }

               rows = newRows;
               cols = newCols;
               initial = newInitial;

               generateGrid(gridPane);

           }catch (NumberFormatException e) {
               Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Input");
               alert.showAndWait();
           }
        });


        startButton.setOnAction(e -> {

            try {
                rows = Integer.parseInt(rowsField.getText());
                cols = Integer.parseInt(colsField.getText());
                delay = Integer.parseInt(delayField.getText());

                if(rows <= 0 || cols <= 0 || delay <= 0) {
                    throw new NumberFormatException();
                }

                if (initial.length != rows || initial[0].length != cols) {
                    initial = new int[rows][cols];
                }

                setupStage.close();

                if(simulationThread != null && simulationThread.isAlive()) {
                    running = false;
                    simulationThread.interrupt();
                }

                run(primaryStage);

            }catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input");
                alert.showAndWait();
            }

        });

        HBox patternBox = new HBox(5);
        patternBox.setPadding(new Insets(5));
        patternBox.setAlignment(Pos.CENTER_LEFT);

        ComboBox<String> patternComboBox = new ComboBox<>();
        patternComboBox.getItems().addAll("Glider", "Pentadecathlon", "Spaceship");
        patternComboBox.setValue("Glider");

        TextField xField = new TextField("0");
        xField.setPrefWidth(40);
        TextField yField = new TextField("0");
        yField.setPrefWidth(40);

        Button placeButton = new Button("Place");

        placeButton.setOnAction(event -> {
            try {
                int y = Integer.parseInt(xField.getText());
                int x = Integer.parseInt(yField.getText());

                String selectedPattern = patternComboBox.getValue();

                if(selectedPattern.equals("Glider")) {
                    placePattern(initial, x, y, 0);
                }
                else if(selectedPattern.equals("Pentadecathlon")) {
                    placePattern(initial, x, y, 1);
                }
                else if(selectedPattern.equals("Spaceship")) {
                    placePattern(initial, x, y, 2);
                }

                generateGrid(gridPane);
            }catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input");
                alert.showAndWait();
            }
        });

        patternBox.getChildren().addAll(new Label("Pattern"), patternComboBox, new Label("X:"), xField, new Label("Y:"), yField, placeButton);

        VBox root = new VBox(10,
                new HBox(10, new Label("Riadky:"), rowsField, new Label("Stĺpce:"), colsField),
                new HBox(10, new Label("Delay (ms):"), delayField),
                patternBox,
                scrollPane,
                new HBox(10, updateButton, startButton)
        );

        root.setPadding(new Insets(10));
        setupStage.setScene(new Scene(root));
        setupStage.show();

    }

    private void run(Stage stage) {
        if(originalState == null) {
            originalState = copy(initial);
        }

        map = copy(initial);
        buffer = new int[rows][cols];

        canvas = new Canvas(rows*cellSize, cols*cellSize);
        draw();

        Label timerLabel = new Label("Update in: " + (float)delay/1000 + "s");
        long[] remainingTime = {delay};

        Timeline countdown = new Timeline(new javafx.animation.KeyFrame(
                javafx.util.Duration.millis(50), e -> {
            if (running) {
                remainingTime[0] -= 50;
                if (remainingTime[0] < 0) remainingTime[0] = 0;
                timerLabel.setText("Update in: " + (float)remainingTime[0]/1000 + " s");
            }
        }
        ));
        countdown.setCycleCount(Timeline.INDEFINITE);
        countdown.play();

        Button pauseButton = new Button("Pause");
        pauseButton.setOnAction(e -> {
            running = !running;
            pauseButton.setText(running ? "Pause" : "Resume");
            if (!running) {
                timerLabel.setText("Paused");
            } else {
                remainingTime[0] = delay;

                if(simulationThread == null || !simulationThread.isAlive()){
                    simulationThread = new Thread(() -> {
                        while (running) {

                            try{
                                Thread.sleep(delay);
                            }catch(InterruptedException ex){
                                break;
                            }

                            if(running){
                                update();
                                remainingTime[0] = delay;
                                javafx.application.Platform.runLater(this::draw);
                            }

                        }
                    });
                    simulationThread.setDaemon(true);
                    simulationThread.start();
                }
            }
        });

        Button configure = new Button("Configure");
        configure.setOnAction(e -> {
            running = false;
            pauseButton.setText("Resume");
            timerLabel.setText("Paused");

            initial = copy(map);

            if (simulationThread != null && simulationThread.isAlive()) {
                simulationThread.interrupt();
            }
            openSetup();
        });

        Button resetButton = new Button("Reset");
        resetButton.setOnAction(e -> {
            running = false;
            delay = 500;
            pauseButton.setText("Resume");
            timerLabel.setText("Paused");

            if(originalState != null) {
                map = copy(originalState);
            }
            else{
                for (int i = 0; i < rows; i++) {
                    for (int j = 0; j < cols; j++) {
                        map[i][j] = 0;
                    }
                }
            }

            draw();
        });

        VBox controlPanel = new VBox(10, timerLabel, pauseButton, resetButton , configure);
        controlPanel.setPadding(new Insets(10));
        controlPanel.setStyle("-fx-background-color: white;");
        controlPanel.setPrefWidth(150);

        HBox mainPanel = new HBox(canvas, controlPanel);

        Scene scene = new Scene(mainPanel);
        stage.setTitle("Game of Life");
        stage.setScene(scene);
        stage.show();

        if (simulationThread != null && simulationThread.isAlive()) {
            running = false;
            simulationThread.interrupt();
        }

        running = true;
        Thread simulationThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ex) {
                    break;
                }

                if (running) {
                    update();
                    remainingTime[0] = delay;
                    javafx.application.Platform.runLater(this::draw);
                }
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private void randomGrid(){
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                map[i][j] = Math.random() < 0.2 ? 1 : 0;
            }
        }
    }

    private void generateGrid(GridPane gridPane){
        gridPane.getChildren().clear();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(cellSize, cellSize);
                cell.setStyle("-fx-border-color: black; -fx-background-color: " + (initial[i][j] == 1 ? "black" : "white") + ";");
                final int row = i, col = j;

                cell.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        initial[row][col] = 1 - initial[row][col];
                        cell.setStyle("-fx-border-color: black; -fx-background-color: " + (initial[row][col] == 1 ? "black" : "white") + ";");
                    }
                });

                gridPane.add(cell, j, i);
            }
        }
    }

    private void update(){
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int neighors = liveNeighbors1(i, j);
                if (map[i][j] > 0){
                    buffer[i][j] = (neighors >= cell1rules[0] && neighors <= cell1rules[1]) ? 1 : 0;
                }
                else{
                    buffer[i][j] = (neighors == cell1rules[2]) ? 1 : 0;
                }
            }
        }

        int[][] temp = map;
        map = buffer;
        buffer = temp;
    }

    private int liveNeighbors1(int row, int col){
        int count = 0;
        for (int dr = -1; dr <= 1; dr++){
            for (int dc = -1; dc <= 1; dc++){
                if (dr == 0 && dc == 0 ) continue;
                int newRow = (row + dr + rows) % rows;
                int newCol = (col + dc + cols) % cols;
                if (map[newRow][newCol] > 0 && map[newRow][newCol]%2==1) count++;
            }
        }
        return count;
    }

    private void draw(){
        GraphicsContext gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, cols*cellSize, rows*cellSize);

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                if (map[i][j] > 0){
                    gc.setFill(colorFromInt(map[i][j]));
                    gc.fillRect(j*cellSize, i*cellSize, cellSize, cellSize);
                }

                gc.setStroke(Color.BLACK);
                gc.strokeRect(j*cellSize, i*cellSize, cellSize, cellSize);
            }
        }
    }

    private Color colorFromInt(int color){
        if(color == 0) return Color.WHITE;
        if(color == -1) return Color.GREEN;
        return color%2 == 0 ? Color.RED : Color.BLACK;
    }

    private int[][] copy(int[][] original){
        int[][] copy = new int[original.length][original[0].length];

        for (int i = 0; i < original.length; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, original[i].length);
        }
        return copy;
    }



    private int[][] createPattern(int patternID){
        //glider
        if(patternID == 0){
            int[][] pattern = {
                    {0, 1, 0},
                    {0, 0, 1},
                    {1, 1, 1}
            };
            return pattern;
        }

        //spaceship
        else if(patternID == 2){
            int[][] pattern = {
                    {0,0,0,0,0},
                    {0,1,1,1,1},
                    {1,0,0,0,1},
                    {0,0,0,0,1},
                    {1,0,0,1,0}
            };
            return pattern;
        }

        //pentadecathlon
        else if(patternID == 1){
            int[][] pattern = new int[9][13];

            for(int i = 0; i < pattern.length; i++){
                if(i==4){
                    for(int j = 0; j < pattern[0].length; j++){
                        pattern[i][j] = (j < 3) ? 0 : 1;
                    }
                }
                else{
                    for (int j = 0; j < pattern[0].length; j++) {
                        pattern[i][j] = 0;
                    }
                }
            }
            return pattern;
        }

        return null;
    }

    private void placePattern(int[][] grid, int startX, int startY, int patternID){
        if(patternID < 0 || patternID > 2){
            return;
        }

        int[][] pattern = createPattern(patternID);

        for (int i = 0; i < pattern.length; i++) {
            for (int j = 0; j < pattern[0].length; j++) {
                int x = startX + i;
                int y = startY + j;

                if(x >= 0 && x < grid.length && y >= 0 && y < grid[0].length){
                    grid[x][y] = pattern[i][j];
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }

}
