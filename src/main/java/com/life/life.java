package com.life;

import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.util.HashSet;
import java.util.Set;


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
    private static int[] cell2rules = {2, 3, 3};

    private static int cell1AgeLimit = 0;
    private static int cell2AgeLimit = 0;

    private static double lifeChance = 0d;
    private static double blackRandomPriority = 1d;

    private int[][] map = new int[rows][cols];
    private int[][] buffer = new int[rows][cols];

    private Canvas canvas;

    private Set<String> mapStates = new HashSet<>();
    private boolean[] hasCycle = {false};

    @Override
    public void start(Stage stage) throws Exception {
        this.primaryStage = stage;
        openSetup();
    }

    private void openSetup() {
        Stage setupStage = new Stage();
        setupStage.setTitle("Setup");

        TextField rowsField = new TextField(String.valueOf(rows));
        rowsField.setPrefWidth(60);
        TextField colsField = new TextField(String.valueOf(cols));
        colsField.setPrefWidth(60);
        TextField delayField = new TextField(String.valueOf(delay));
        delayField.setPrefWidth(60);

        Button startButton = new Button("Start");
        Button updateButton = new Button("Update grid");
        Button saveOriginalButton = new Button("Save as original");

        GridPane gridPane = new GridPane();
        gridPane.setHgap(1);
        gridPane.setVgap(1);
        gridPane.setPadding(new Insets(10));

        ComboBox<String> cellType = new ComboBox<>();
        cellType.getItems().addAll("cell", "red cell", "wall");
        cellType.setValue("cell");

        CheckBox ageLimit = new CheckBox("Enable age limit");
        if(cell1AgeLimit > 0 || cell2AgeLimit > 0) {
            ageLimit.setSelected(true);
        }

        TextField ageLimitField1 = new TextField(String.valueOf(cell1AgeLimit));
        ageLimitField1.setPrefWidth(50);
        TextField ageLimitField2 = new TextField(String.valueOf(cell2AgeLimit));
        ageLimitField2.setPrefWidth(50);

        Label cell1AgeLimitLabel = new Label("Standard cell age limit:");
        Label cell2AgeLimitLabel = new Label("Red cell age limit:");

        HBox ageBox = new HBox(5, cell1AgeLimitLabel, ageLimitField1, cell2AgeLimitLabel, ageLimitField2);
        ageBox.setAlignment(Pos.CENTER_LEFT);
        ageBox.setVisible(ageLimit.isSelected());
        ageBox.managedProperty().bind(ageLimit.visibleProperty());

        ageLimit.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
           ageBox.setVisible(isSelected);
        });

        CheckBox enableRandom = new CheckBox("Enable random cell revival");
        double maxRevivalChance = 0.05;
        double revivalChance = 0.001;

        Slider revivalChanceSlider = new Slider(0.0, maxRevivalChance, Math.min(revivalChance, maxRevivalChance));
        revivalChanceSlider.setBlockIncrement(revivalChance/50);
        revivalChanceSlider.setPrefWidth(200);

        Label revivalChanceLabel = new Label(String.format("Revival chance: %.2f%%", revivalChanceSlider.getValue() * 100));

        Label blackPriorityLabel = new Label("Probability that revived cell will be standard");

        Slider blackPrioritySlider = new Slider(0.0, 1.0, blackRandomPriority);
        blackPrioritySlider.setShowTickLabels(true);
        blackPrioritySlider.setShowTickMarks(true);
        blackPrioritySlider.setMajorTickUnit(0.2);
        blackPrioritySlider.setBlockIncrement(0.05);
        blackPrioritySlider.setPrefWidth(200);

        Label blackPriorityValueLabel = new Label(String.format("%.2f%%", blackPrioritySlider.getValue()*100));


        HBox revivalBox = new HBox(10, revivalChanceSlider, revivalChanceLabel);
        revivalBox.setAlignment(Pos.CENTER_LEFT);

        HBox priorityBox = new HBox(10, blackPriorityLabel, blackPrioritySlider, blackPriorityValueLabel);
        priorityBox.setAlignment(Pos.CENTER_LEFT);

        revivalBox.setVisible(false);
        priorityBox.setVisible(false);

        revivalBox.visibleProperty().bind(enableRandom.selectedProperty());
        priorityBox.visibleProperty().bind(enableRandom.selectedProperty());

        revivalChanceSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            lifeChance = newVal.doubleValue();
            revivalChanceLabel.setText(String.format("Revival chance: %.2f%%", lifeChance*100));
        });

        blackPrioritySlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            blackRandomPriority = newVal.doubleValue();
            blackPriorityValueLabel.setText(String.format("%.2f%%", blackRandomPriority*100));
        });

        enableRandom.setSelected(lifeChance > 0);

        enableRandom.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            if (!isSelected) {
                lifeChance = 0.0;
                revivalChanceSlider.setValue(0.0);
            } else {
                if (lifeChance == 0.0) {
                    lifeChance = maxRevivalChance / 10;
                    revivalChanceSlider.setValue(lifeChance);
                }
            }
        });

        VBox specialBox = new VBox(5, new Label("Cell type:"), cellType,
                ageLimit,
                ageBox,
                enableRandom,
                revivalBox,
                priorityBox
        );

        specialBox.setPadding(new Insets(5));
        specialBox.setAlignment(Pos.CENTER_LEFT);


        GridPane rulesGrid = new GridPane();
        rulesGrid.setHgap(10);
        rulesGrid.setVgap(10);

        rulesGrid.add(new Label("Survive Min"), 0, 0);
        rulesGrid.add(new Label("Survive Max"), 1, 0);
        rulesGrid.add(new Label("Birth"), 2, 0);

        TextField cell1SurviveMin = new TextField(String.valueOf(cell1rules[0]));
        TextField cell1SurviveMax = new TextField(String.valueOf(cell1rules[1]));
        TextField cell1Birth = new TextField(String.valueOf(cell1rules[2]));

        rulesGrid.add(cell1SurviveMin, 0, 1);
        rulesGrid.add(cell1SurviveMax, 1, 1);
        rulesGrid.add(cell1Birth, 2, 1);

        rulesGrid.add(new Label("Black cells"), 3, 1);

        TextField cell2SurviveMin = new TextField(String.valueOf(cell2rules[0]));
        TextField cell2SurviveMax = new TextField(String.valueOf(cell2rules[1]));
        TextField cell2Birth = new TextField(String.valueOf(cell2rules[2]));

        rulesGrid.add(cell2SurviveMin, 0, 2);
        rulesGrid.add(cell2SurviveMax, 1, 2);
        rulesGrid.add(cell2Birth, 2, 2);

        rulesGrid.add(new Label("Red cells"), 3, 2);


        ScrollPane scrollPane = new ScrollPane(gridPane);
        scrollPane.setPrefSize(865, 865);

        if (initial == null || initial.length != rows || initial[0].length != cols) {
            initial = new int[rows][cols];
        }

        generateGrid(gridPane, cellType);

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

               generateGrid(gridPane, cellType);

           }catch (NumberFormatException e) {
               Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Input");
               alert.showAndWait();
           }
        });

        saveOriginalButton.setOnAction(event -> {
            if (initial != null) {
                originalState = new int[initial.length][initial[0].length];
                for(int row = 0; row < rows; row++) {
                    System.arraycopy(initial[row], 0, originalState[row], 0, originalState[row].length);
                }
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Original state updated.");
                alert.showAndWait();
            }
        });

        startButton.setOnAction(e -> {

            try {
                rows = Integer.parseInt(rowsField.getText());
                cols = Integer.parseInt(colsField.getText());
                delay = Integer.parseInt(delayField.getText());

                cell1rules[0] = parseRule(cell1SurviveMin.getText());
                cell1rules[1] = parseRule(cell1SurviveMax.getText());
                cell1rules[2] = parseRule(cell1Birth.getText());

                cell2rules[0] = parseRule(cell2SurviveMin.getText());
                cell2rules[1] = parseRule(cell2SurviveMax.getText());
                cell2rules[2] = parseRule(cell2Birth.getText());

                if(rows <= 0 || cols <= 0 || delay <= 0) {
                    throw new NumberFormatException();
                }

                if (initial.length != rows || initial[0].length != cols) {
                    initial = new int[rows][cols];
                }

                if(ageLimit.isSelected()) {
                    try {
                        cell1AgeLimit = Integer.parseInt(ageLimitField1.getText());
                        cell2AgeLimit = Integer.parseInt(ageLimitField2.getText());

                        if(cell1AgeLimit < 0 || cell2AgeLimit < 0) {
                            throw new NumberFormatException();
                        }
                    }
                    catch (NumberFormatException ex) {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid Input");
                        alert.showAndWait();
                        return;
                    }
                }
                else{
                    cell1AgeLimit = cell2AgeLimit = 0;
                }

                setupStage.close();

                if(simulationThread != null && simulationThread.isAlive()) {
                    running = false;
                    simulationThread.interrupt();
                }

                run(primaryStage);

            }catch (Exception ex) {
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

                generateGrid(gridPane, cellType);
            }catch (NumberFormatException e) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input");
                alert.showAndWait();
            }
        });

        patternBox.getChildren().addAll(
                new Label("Pattern"), patternComboBox,
                new Label("X:"), xField,
                new Label("Y:"), yField,
                placeButton);

        HBox rowsBox = new HBox(5, new Label("Rows:"), rowsField);
        HBox colsBox = new HBox(5, new Label("Columns:"), colsField);
        HBox delayBox = new HBox(5, new Label("Delay (ms):"), delayField);

        HBox paramsBox = new HBox(15, rowsBox, colsBox, delayBox);
        paramsBox.setAlignment(Pos.CENTER_LEFT);

        HBox buttonsBox = new HBox(10, updateButton, saveOriginalButton, startButton);
        buttonsBox.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        VBox controls = new VBox(10,
                patternBox,
                paramsBox,
                specialBox,
                rulesGrid,
                spacer,
                buttonsBox);

        HBox mainBox = new HBox(15,
                controls,
                scrollPane);


        mainBox.setPadding(new Insets(10));
        setupStage.setScene(new Scene(mainBox));
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

                                int[][] pattern = smallestRectangle(map);
                                if(pattern != null){
                                    String patternKey = mapToString(pattern);

                                    if(mapStates.contains(patternKey)){
                                        if(!hasCycle[0]){
                                            hasCycle[0] = true;

                                            Platform.runLater(() -> {
                                                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cycle was detected");
                                                pauseButton.fire();
                                                alert.showAndWait();
                                            });
                                        }
                                    }
                                    else{
                                        mapStates.add(patternKey);
                                    }
                                }

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
            mapStates.clear();
            hasCycle[0] = false;

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
            mapStates.clear();
            hasCycle[0] = false;

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

                    int[][] pattern = smallestRectangle(map);
                    if(pattern != null){
                        String patternKey = mapToString(pattern);

                        if(mapStates.contains(patternKey)){
                            if(!hasCycle[0]){
                                hasCycle[0] = true;

                                Platform.runLater(() -> {
                                    Alert alert = new Alert(Alert.AlertType.INFORMATION, "Cycle was detected");
                                    pauseButton.fire();
                                    alert.showAndWait();
                                });
                            }
                        }
                        else{
                            mapStates.add(patternKey);
                        }
                    }

                    remainingTime[0] = delay;
                    javafx.application.Platform.runLater(this::draw);
                }
            }
        });

        simulationThread.setDaemon(true);
        simulationThread.start();
    }

    private void generateGrid(GridPane gridPane, ComboBox<String> cellType){
        gridPane.getChildren().clear();
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                StackPane cell = new StackPane();
                cell.setPrefSize(cellSize, cellSize);
                cell.setStyle("-fx-border-color: black; -fx-background-color: " + colorFromInt(initial[i][j]) + ";");
                final int row = i, col = j;

                cell.setOnMouseClicked(event -> {
                    if (event.getButton() == MouseButton.PRIMARY) {
                        String type = cellType.getValue();
                        switch (type) {
                            case "cell" -> initial[row][col] = 1;
                            case "red cell" -> initial[row][col] = 2;
                            case "wall" -> initial[row][col] = -1;
                        }

                        cell.setStyle("-fx-border-color: black; -fx-background-color: " + colorFromInt(initial[row][col]) + ";");
                    }

                    else if (event.getButton() == MouseButton.SECONDARY) {
                        initial[row][col] = 0;
                        cell.setStyle("-fx-border-color: black; -fx-background-color: white;");
                    }
                });

                gridPane.add(cell, j, i);
            }
        }
    }

    private void update(){
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int neighors1 = liveNeighbors1(i, j);
                int neighors2 = liveNeighbors2(i, j);

                //living cell
                if (map[i][j] > 0){
                    //normal cell
                    if(map[i][j]%2==1){
                        if(neighors1 < cell1rules[0] || neighors1 > cell1rules[1]){
                            buffer[i][j] = 0;
                        }
                        else if(cell1AgeLimit!=0){
                            //age
                            buffer[i][j] = map[i][j]+2;
                            if(buffer[i][j] > 2*cell1AgeLimit-1){
                                buffer[i][j] = 0;
                            }
                        }

                        else{
                            buffer[i][j] = 1;
                        }
                    }

                    //red cell
                    else{
                        if(neighors2 < cell2rules[0] || neighors2 > cell2rules[1]){
                            buffer[i][j] = 0;
                        }

                        else if(cell2AgeLimit!=0){
                            //age
                            buffer[i][j] = map[i][j]+2;
                            if(buffer[i][j]>2*cell2AgeLimit){
                                //die
                                buffer[i][j] = 0;
                            }
                        }

                        else{
                            buffer[i][j] = 2;
                        }
                    }

                }

                //wall
                else if (map[i][j] == -1) {
                    buffer[i][j] = -1;
                }

                //dead
                else{
                    if(neighors1==cell1rules[2] && neighors2==cell2rules[2]){
                        buffer[i][j] = (Math.random() < 0.5) ? 1 : 2;
                    }

                    else if(neighors1==cell1rules[2]){
                        buffer[i][j] = 1;
                    }

                    else if(neighors2==cell2rules[2]){
                        buffer[i][j] = 2;
                    }

                    else{
                        if(Math.random() > 1-lifeChance){
                            buffer[i][j] = (Math.random() < blackRandomPriority) ? 1 : 2;
                        }

                        else{
                            buffer[i][j] = 0;
                        }
                    }
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

    private int liveNeighbors2(int row, int col){
        int count = 0;
        for (int dr = -1; dr <= 1; dr++){
            for (int dc = -1; dc <= 1; dc++){
                if (dr == 0 && dc == 0 ) continue;
                int newRow = (row + dr + rows) % rows;
                int newCol = (col + dc + cols) % cols;
                if (map[newRow][newCol] > 0 && map[newRow][newCol]%2==0) count++;
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
                gc.setFill(Color.web(colorFromInt(map[i][j])));
                gc.fillRect(j*cellSize, i*cellSize, cellSize, cellSize);

                gc.setStroke(Color.BLACK);
                gc.strokeRect(j*cellSize, i*cellSize, cellSize, cellSize);
            }
        }
    }

    private String colorFromInt(int color){
        if(color == 0) return "FFFFFF";
        if(color == -1) return "#00FF00";
        return color%2 == 0 ? "#FF0000" : "#000000";
    }

    private int[][] copy(int[][] original){
        int[][] copy = new int[rows][cols];

        int minRows = Math.min(original.length, rows);
        int minCols = Math.min(original.length, cols);

        for (int i = 0; i < minRows; i++) {
            System.arraycopy(original[i], 0, copy[i], 0, minCols);
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

    private int[][] smallestRectangle(int[][] grid){

        int minRow = grid.length, maxRow = -1;
        int minCol = grid[0].length, maxCol = -1;

        for(int i = 0; i < grid.length; i++){
            for(int j = 0; j < grid[0].length; j++){
                if(grid[i][j] > 0){
                    minRow = Math.min(minRow, i);
                    minCol = Math.min(minCol, j);
                    maxRow = Math.max(maxRow, i);
                    maxCol = Math.max(maxCol, j);
                }
            }
        }

        if(maxRow < minRow || maxCol < minCol){
            return null;
        }

        int[][] minRectangle = new int[maxRow-minRow+1][maxCol-minCol+1];
        for(int i = minRow; i <= maxRow; i++){
            for(int j = minCol; j <= maxCol; j++){
                minRectangle[i-minRow][j-minCol] = grid[i][j];
            }
        }

        return minRectangle;
    }

    private String mapToString(int[][] map){
        StringBuilder sb = new StringBuilder();
        for(int[] row : map){
            for(int col : row){
                sb.append(col);
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private int parseRule(String rule){
        int value = Integer.parseInt(rule);
        if(value < 0 || value > 8){
            throw new IllegalArgumentException("Invalid input");
        }
        return value;
    }

    public static void main(String[] args) {
        launch(args);
    }

}
