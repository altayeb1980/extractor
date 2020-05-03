package org.scrawls.extractor;

import javafx.application.Application;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Extractor extends Application {


    private static final Pattern PHONE_NUMBER_PATTERN = Pattern.compile("\\d{10}|(?:\\d{3}-){2}\\d{4}|\\(\\d{3}\\)\\d{3}-?\\d{4}");
    private final TreeTableView<ExtractorResult> treeTableView = new TreeTableView<>();
    private final TextField urlTextField = new TextField("https://seattle.craigslist.org/search/cta");
    private final AtomicInteger extractedPhoneNumbers = new AtomicInteger(0);
    private final Button buttonStart = new Button("Start");
    private final Button buttonStop = new Button("Stop");
    private final Button buttonSave = new Button("Save");
    private final Label timeRunningLabel = new Label("Time running (HH:MM:SS): ");
    private final Label timeRunningValue = new Label("00:00:00");
    private final Label totalPhonesLabel = new Label("Total Extracted phone numbers : ");
    private final Label totalPhones = new Label("0");
    private final StringBuilder phonesBuilder = new StringBuilder();
    private Instant start;

    public static void main(String[] args) {
        launch();
    }
    private final ExtractorService extractorService = new ExtractorService();
    private TreeItem<ExtractorResult> root;

    private final ProgressBar progressBar = new ProgressBar();


    @Override
    public void start(Stage stage) throws Exception {
        progressBar.prefWidthProperty().bind(treeTableView.widthProperty());
        setUpTreeTableViewColumns();
        buttonSave.setDisable(true);
        BorderPane border = new BorderPane();
        border.setTop(addVBox(stage));

        Scene scene = new Scene(border, 1000, 600);
        stage.setScene(scene);
        stage.show();

        progressBar.progressProperty().bind(extractorService.progressProperty());
        progressBar.visibleProperty().bind(extractorService.progressProperty().isNotEqualTo(new SimpleDoubleProperty(ProgressBar.INDETERMINATE_PROGRESS)));

        extractorService.setOnScheduled(workerStateEvent -> {
            progressBar.progressProperty().bind(extractorService.progressProperty());
        });

        extractorService.setOnCancelled(workerStateEvent -> {
            reset();
            buttonStart.setDisable(false);
        });

        extractorService.setOnSucceeded(workerStateEvent -> {
            treeTableView.setRoot(root);
            Instant end = Instant.now();
            Duration duration = Duration.between(start, end);
            timeRunningValue.setText(String.format("%02d:%02d:%02d", duration.toHoursPart(), duration.toMinutesPart(), duration.toSecondsPart()));
            totalPhones.setText(extractedPhoneNumbers.get() + "");
            buttonStart.setDisable(false);
            buttonSave.setDisable(false);
            progressBar.progressProperty().unbind();
            progressBar.setProgress(1);
        });
    }

    private void setUpTreeTableViewColumns() {
        TreeTableColumn<ExtractorResult, String> urlCol = new TreeTableColumn("URL Processed");
        urlCol.setMinWidth(750);
        urlCol.setCellValueFactory(
                param ->
                        new ReadOnlyStringWrapper(param.getValue().getValue().getUrl())
        );

        TreeTableColumn<ExtractorResult, String> phoneNumberCol = new TreeTableColumn("Phone Number(s)");
        phoneNumberCol.setMinWidth(200);
        phoneNumberCol.setCellValueFactory(
                param ->
                        new ReadOnlyStringWrapper(param.getValue().getValue().getPhoneNumber())
        );
        treeTableView.getColumns().addAll(urlCol, phoneNumberCol);
    }

    private VBox setUpSummaryData() {
        VBox vBox = new VBox();

        HBox hbox = new HBox();
        timeRunningLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        timeRunningValue.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        hbox.getChildren().add(timeRunningLabel);
        hbox.getChildren().add(timeRunningValue);
        vBox.getChildren().add(hbox);

        hbox = new HBox();
        totalPhonesLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        totalPhones.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        hbox.getChildren().add(totalPhonesLabel);
        hbox.getChildren().add(totalPhones);
        vBox.getChildren().add(hbox);
        return vBox;
    }

    private HBox getHBox() {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
        return hbox;
    }

    private VBox getVBox() {
        VBox vbox = new VBox();
        vbox.setPadding(new Insets(10));
        vbox.setSpacing(8);
        return vbox;
    }

    private VBox addVBox(Stage stage) {
        VBox vbox = getVBox();
        Label urlLabel = new Label("URL List: ");
        urlLabel.setAlignment(Pos.BOTTOM_LEFT);
        urlLabel.setFont(Font.font("Arial", FontWeight.BOLD, 12));
        HBox hbox = getHBox();
        hbox.getChildren().add(urlLabel);
        hbox.getChildren().add(urlTextField);
        HBox.setHgrow(urlTextField, Priority.ALWAYS);
        vbox.getChildren().add(hbox);


        buttonStart.setPrefSize(100, 20);
        buttonStop.setPrefSize(100, 20);
        buttonStop.setOnAction(event -> {
            if(extractorService.isRunning()){
                extractorService.cancel();
            }
        });

        buttonSave.setPrefSize(100, 20);
        buttonSave.setOnAction(event -> {

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Save extracted phones");
            fileChooser.setInitialFileName("phones" + Instant.now().toEpochMilli() + ".txt");
            fileChooser.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("All Files", "*.*"),
                    new FileChooser.ExtensionFilter("Text Documents", "*.txt")
            );
            File file = fileChooser.showSaveDialog(stage);
            if (file != null) {

                try {
                    Files.write(Paths.get(file.toURI()), phonesBuilder.toString().getBytes(), StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });

        buttonStart.setOnAction(event -> {
            try {
                root = new TreeItem<>(new ExtractorResult("/", ""));
                start = Instant.now();
                phonesBuilder.delete(0,phonesBuilder.length());
                extractedPhoneNumbers.set(0);
                timeRunningValue.setText("00:00:00");
                totalPhones.setText("0");
                buttonStart.setDisable(true);
                buttonSave.setDisable(true);
                handleStartAction(event);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        hbox = getHBox();
        hbox.getChildren().addAll(buttonStart, buttonStop, buttonSave);


        vbox.getChildren().add(hbox);
        vbox.getChildren().add(progressBar);
        vbox.getChildren().add(setUpSummaryData());
        vbox.getChildren().add(treeTableView);

        return vbox;
    }

    private void reset() {
        phonesBuilder.delete(0,phonesBuilder.length());
        extractedPhoneNumbers.set(0);
        timeRunningValue.setText("00:00:00");
        totalPhones.setText("0");
        treeTableView.setRoot(null);
        buttonSave.setDisable(true);
        progressBar.progressProperty().unbind();
        progressBar.setProgress(1);

    }


     class ExtractorService extends Service<Void>{
        @Override
        protected Task<Void> createTask() {
            return new Task<>() {
                @Override
                protected Void call() throws Exception {
                    System.out.println("start the service");
                    updateMessage("start extracting ......");
                    updateProgress(0,10);
                    String url = urlTextField.getText();
                    System.out.println(url);
                    Document doc = Jsoup.connect(url).get();
                    Elements elements = doc.select("#sortable-results > ul").select("li");

                    if (elements.size() == 0) {
                        Thread.sleep(300);
                        updateProgress(0,1);
                        processSubUrl(url, root);
                    }
                    for (int i = 0;i<elements.size();i++) {
                        Element element = elements.get(i);
                        final String hrefUrl = element.getElementsByTag("a").first().attr("href");
                        Thread.sleep(300);
                        updateProgress(i+1,elements.size());
                        processSubUrl(hrefUrl, root);
                    }
                    updateMessage("finish extracting ......");
                    return null;
                }
            };
        }
    }

    private void handleStartAction(ActionEvent event) throws IOException {

        if(!extractorService.isRunning()){
            extractorService.reset();
            extractorService.start();
        }
    }

    private  void  processSubUrl(String hrefUrl, final TreeItem<ExtractorResult> root) throws IOException {
        Set<ExtractorResult> results = getExtractedResults(hrefUrl);
        final TreeItem<ExtractorResult> subRoot =
                new TreeItem<>(new ExtractorResult(hrefUrl, ""));

        results.stream().filter(extractorResult -> !extractorResult.getPhoneNumber().isBlank()).forEach(extractorResult -> {
            subRoot.getChildren().add(new TreeItem<>(extractorResult));
            extractedPhoneNumbers.incrementAndGet();
            phonesBuilder.append(extractorResult.getPhoneNumber());
            phonesBuilder.append(System.lineSeparator());
        });
        root.getChildren().add(subRoot);
    }

    private Set<ExtractorResult> getExtractedResults(String url) throws IOException {
        Document doc = Jsoup.connect(url).get();
        Optional<Elements> optionalElements = Optional.ofNullable(doc.select("#postingbody"));

        return optionalElements.get().stream()
                .filter(element -> !element.text().isBlank())
                .map(element -> new Scanner(element.text().replaceAll("\\s+", "")).
                        findAll(PHONE_NUMBER_PATTERN)
                        .map(matcher ->
                                new ExtractorResult("", matcher.group())
                        )
                        .collect(Collectors.toSet())
                )
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
    }

}
