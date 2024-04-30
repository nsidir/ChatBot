package com.example.demo;

import de.kherud.llama.*;
import javafx.application.*;
import javafx.concurrent.*;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.*;

import java.io.*;

public class MainApp extends Application {

    private LlamaModel model;
    private BufferedWriter writer;

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Set up chatbot model params and writer
        ModelParameters modelParams = new ModelParameters()
                .setF16Kv(true)
                .setEmbedding(true)
                .setNGpuLayers(23);
        String modelPath = GlobalConstants.path;
        model = new LlamaModel(modelPath, modelParams);

        writer = new BufferedWriter(new FileWriter("output.txt"));

        // Create UI components
        TextField userInput = new TextField();
        TextArea chatDisplay = new TextArea();
        chatDisplay.setEditable(false);
        Button sendButton = new Button("Send");

        // Event handler for sendButton
        sendButton.setOnAction(event -> {
            String input = userInput.getText();
            chatDisplay.appendText("User: " + input + "\n");

            // Create a Task to generate response asynchronously
            Task<String> responseTask = new Task<>() {
                @Override
                protected String call() throws Exception {
                    return generateResponse(input);
                }
            };

            // Update UI when the response task completes
            responseTask.setOnSucceeded(e -> {
                try {
                    chatDisplay.appendText("Llama: " + responseTask.get() + "\n");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            // Start the task
            new Thread(responseTask).start();
        });

        // Layout setup
        VBox root = new VBox(10);
        root.setPadding(new Insets(20));
        root.getChildren().addAll(userInput, sendButton, chatDisplay);

        // Scene setup
        Scene scene = new Scene(root, 400, 400);

        // Stage setup
        primaryStage.setTitle("Llama Chatbot");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private String generateResponse(String input) throws IOException {
        InferenceParameters inferParams = new InferenceParameters()
                .setTemperature(0.85f)
                .setTopP(0.15f)
                .setBeamSearch(true)
                .setNBeams(4)
                .setPenalizeNl(true)
                .setNProbs(10)
                .setMirostat(InferenceParameters.MiroStat.V2)
                .setAntiPrompt("User:");

        StringBuilder response = new StringBuilder();
        for (LlamaModel.Output output : model.generate(input, inferParams)) {
            response.append(output);
            try {
                writer.write(String.valueOf(output));
            } catch (IOException e) {
                System.err.println("Error writing to file: " + e.getMessage());
            }
        }
        writer.flush();
        return response.toString();
    }

    @Override
    public void stop() throws Exception {
        // Clean up resources when the application stops
        model.close();
        writer.close();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
