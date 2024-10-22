

import javafx.application.Application;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Priority;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;
import javafx.collections.ObservableList;
import javafx.collections.FXCollections;
// some changes
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URI;

class PathTreeItem extends TreeItem<FileItem> {
    private boolean isLeaf = false;
    private boolean isFirstTimeChildren = true;
    private boolean isFirstTimeLeft = true;

    private PathTreeItem(FileItem fileItem) {
        super(fileItem);
    }

    public static TreeItem<FileItem> createNode(File file) {
        return new PathTreeItem(new FileItem(file));
    }

    @Override
    public ObservableList<TreeItem<FileItem>> getChildren() {
        if (isFirstTimeChildren) {
            isFirstTimeChildren = false;
            super.getChildren().setAll(buildChildren(this));
        }
        return super.getChildren();
    }

    @Override
    public boolean isLeaf() {
        if (isFirstTimeLeft) {
            isFirstTimeLeft = false;
            File f = (File) getValue().getValue();
            isLeaf = f.isFile();
        }
        return isLeaf;
    }

    private ObservableList<TreeItem<FileItem>> buildChildren(TreeItem<FileItem> treeItem) {
        File f = treeItem.getValue().getValue();
        if (f != null && f.isDirectory()) {
            File[] files = f.listFiles();
            if (files != null) {
                ObservableList<TreeItem<FileItem>> children = FXCollections.observableArrayList();

                for (File childFile : files) {
                    children.add(createNode(childFile));
                }

                return children;
            }
        }

        return FXCollections.emptyObservableList();
    }
}

class FileItem {
    private File file;
    public FileItem(File file) {
        this.file = file;
    }
    public File getValue() {
        return file;
    }
    @Override
    public String toString() {
        if (file.getName() == null) {
            return file.toString();
        } else {
            return file.getName().toString(); // showing file name on the TreeView
        }
    }        
}
public class ZazoFX extends Application {

    private TextArea editorPane;
    private TreeView<FileItem> tree;
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
        TreeItem<FileItem> rootItem = createNode(new File(System.getProperty("user.dir")));
        tree = new TreeView<>(rootItem);
        tree.setShowRoot(true);
        tree.getSelectionModel().setSelectionMode(SelectionMode.SINGLE);
        tree.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<TreeItem<FileItem>>() {
            @Override
            public void changed(ObservableValue<? extends TreeItem<FileItem>> observable, TreeItem<FileItem> oldValue, TreeItem<FileItem> newValue) {
                if (newValue != null) {
                    displayFile(newValue.getValue().getValue().toURI().toString());
                }
            }
        });
        tree.setMinWidth(100.0);
        tree.setStyle("-fx-font-family: Monaco; -fx-font-size: 16;");
        
        // Create the editor pane
        editorPane = new TextArea();
        editorPane.textProperty().addListener((observable, oldValue, newValue) -> hasChanges = true);
        editorPane.setFont(Font.font("Monaco", FontWeight.NORMAL, 16));
        // Layout
        // HBox vbox = new HBox();
        // HBox.setHgrow(tree, Priority.ALWAYS);
        // HBox.setHgrow(editorPane, Priority.ALWAYS);
        // vbox.getChildren().addAll(tree, editorPane);
        SplitPane vbox = new SplitPane();
        vbox.getItems().addAll(tree, editorPane);
        vbox.setDividerPositions(0.2f, 0.8f);

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
    private TreeItem<FileItem> createNode(final File f) {
        return PathTreeItem.createNode(f);
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
