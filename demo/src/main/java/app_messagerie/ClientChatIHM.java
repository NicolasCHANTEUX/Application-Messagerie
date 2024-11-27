package app_messagerie;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

import java.io.*;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class ClientChatIHM extends Application
{
	private TextArea messageArea; // Zone d'affichage des messages
	private TextField inputField; // Champ d'entrée pour les messages
	private PrintWriter out; // Pour envoyer des messages au serveur1
	private String pseudo; // Pseudo de l'utilisateur
	private BufferedReader in;

	public static void main(String[] args)
	{
		launch(args); // Démarre l'application JavaFX
	}

	@Override
	public void start(Stage primaryStage)
	{
		// Fenêtre pour demander le pseudo
		pseudo = askPseudo();

		// Créer l'interface principale
		primaryStage.setTitle("Chat - " + pseudo);

		// Zone d'affichage des messages
		messageArea = new TextArea();
		messageArea.setEditable(false); // Les messages ne peuvent pas être modifiés

		// Champ de texte pour écrire un message
		inputField = new TextField();
		inputField.setPromptText("Tapez votre message ici...");
		inputField.setOnAction(event -> sendMessage()); // Envoyer le message à la pression d'Entrée

		// Mise en page
		VBox root = new VBox(10, messageArea, inputField);
		root.setPrefSize(400, 300);

		// Créer la scène
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();

		// Connecter au serveur
		new Thread(this::connectToServer).start();
	}

	private String askPseudo()
	{
		TextInputDialog dialog = new TextInputDialog();
		dialog.setTitle("Connexion");
		dialog.setHeaderText("Bienvenue dans le chat");
		dialog.setContentText("Entrez votre pseudo :");

		return dialog.showAndWait().orElse("Anonyme");
	}

	private void connectToServer()
	{
		try
		{
			Socket socket = new Socket("localhost", 6000); // Connexion au serveur
			in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new PrintWriter(socket.getOutputStream(), true);

			// Envoyer le pseudo au serveur
			out.println(pseudo);

			// Écouter les messages entrants
			String message;
			while ((message = in.readLine()) != null)
			{
				String finalMessage = message; // Final pour usage dans Platform.runLater
				Platform.runLater(() -> messageArea.appendText(finalMessage + "\n")); // Mise  à jour de l'IHM
			}
		} catch (IOException e)
		{
			Platform.runLater(() -> messageArea.appendText("Erreur : Impossible de se connecter au serveur.\n"));
		}
	}

	private void sendMessage()
	{
		String message = inputField.getText().trim();
		if (!message.isEmpty())
		{
			String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm")); // Ajouter l'heure
			out.println(message); // Envoyer au serveur
			messageArea.appendText(pseudo + " : " + message + " (" + time + ")\n"); // Afficher localement
			inputField.clear();
		}
	}

	@Override
	public void stop() throws Exception
	{
		super.stop();
		if (out != null)
		{
			out.println("/quit"); // Informer le serveur de la déconnexion
			out.close();
		}
		if (in != null)
		{
			in.close();
		}
	}
}
