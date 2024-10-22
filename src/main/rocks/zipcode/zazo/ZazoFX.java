package rocks.zipcode.zazo;

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URI;

public class ZazoFX extends Application {

    private TextArea editorPane;
    private TreeView<File> tree;
    private String currentFileName = "";
    private boolean hasChanges = false;

    public static void main(String[] args) {
        System.out.println("Launching ZazoFX");
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("ZazoFX - autosaving on Focus Change");

        BorderPane borderPane = new BorderPane();

        // Create the tree view
        TreeItem<File> rootItem = createNode(new File(System.getProperty("user.dir")));
        tree = new TreeView<>(rootItem);
        tree.setShowRoot(true);
        tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<File>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<File>> observable, TreeItem<File> oldValue, TreeItem<File> newValue) {
                if (newValue != null) {
                    displayFile(newValue.getValue().toURI().toString());
                }
            }
        });

        // Create the editor pane
        editorPane = new TextArea();
        editorPane.textProperty().addListener((observable, oldValue, newValue) -> hasChanges = true);

        // Layout
        VBox vbox = new VBox();
        VBox.setVgrow(tree, Priority.ALWAYS);
        VBox.setVgrow(editorPane, Priority.ALWAYS);
        vbox.getChildren().addAll(tree, editorPane);

        borderPane.setCenter(vbox);

        Scene scene = new Scene(borderPane, 1024, 600);
        primaryStage.setScene(scene);
        primaryStage.show();
    }

 
    // This method creates a TreeItem to represent the given File. It does this
    // by overriding the TreeItem.getChildren() and TreeItem.isLeaf() methods
    // anonymously, but this could be better abstracted by creating a
    // 'FileTreeItem' subclass of TreeItem. However, this is left as an exercise
    // for the reader.
    private TreeItem<File> createNode(final File f) {
        return new TreeItem<File>(f) {
            // We cache whether the File is a leaf or not. A File is a leaf if
            // it is not a directory and does not have any files contained within
            // it. We cache this as isLeaf() is called often, and doing the
            // actual check on File is expensive.
            private boolean isLeaf;

            // We do the children and leaf testing only once, and then set these
            // booleans to false so that we do not check again during this
            // run. A more complete implementation may need to handle more
            // dynamic file system situations (such as where a folder has files
            // added after the TreeView is shown). Again, this is left as an
            // exercise for the reader.
            private boolean isFirstTimeChildren = true;
            private boolean isFirstTimeLeaf = true;

            @Override public ObservableList<TreeItem<File>> getChildren() {
                if (isFirstTimeChildren) {
                    isFirstTimeChildren = false;

                    // First getChildren() call, so we actually go off and
                    // determine the children of the File contained in this TreeItem.
                    super.getChildren().setAll(buildChildren(this));
                }
                return super.getChildren();
            }

            @Override public boolean isLeaf() {
                if (isFirstTimeLeaf) {
                    isFirstTimeLeaf = false;
                    File f = (File) getValue();
                    isLeaf = f.isFile();
                }

                return isLeaf;
            }

            private ObservableList<TreeItem<File>> buildChildren(TreeItem<File> TreeItem) {
                File f = TreeItem.getValue();
                if (f != null && f.isDirectory()) {
                    File[] files = f.listFiles();
                    if (files != null) {
                        ObservableList<TreeItem<File>> children = FXCollections.observableArrayList();

                        for (File childFile : files) {
                            children.add(createNode(childFile));
                        }

                        return children;
                    }
                }

                return FXCollections.emptyObservableList();
            }
        };
    }

    private void displayFile(String url) {
        try {
            if (url != null) {
                saveFileIfChanged(url);
                String content = new String(Files.readAllBytes(Paths.get(new URI(url))));
                editorPane.setText(content);
            } else {
                editorPane.setText("File Not Found");
            }
        } catch (Exception e) {
            System.err.println("Attempted to read a bad URL: " + url);
        }
    }

    private void saveFileIfChanged(String url) {
        if (hasChanges) {
            try {
                Files.write(Paths.get(new URI(currentFileName)), editorPane.getText().getBytes());
                hasChanges = false;
            } catch (Exception ex) {
                new Alert(Alert.AlertType.ERROR, "Error saving file: " + ex.getMessage()).showAndWait();
            }
        }
        currentFileName = url;
    }
}
