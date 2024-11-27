package app_messagerie;
import java.io.*;
import java.net.*;
import java.util.*;

public class ServeurChat
{
	private static final int PORT = 6000;
	private static final Set<GerantDeClient> clients = new HashSet<>();

	public static void main(String[] args)
	{
		System.out.println("Serveur de chat démarré...");
		try (ServerSocket serverSocket = new ServerSocket(PORT))
		{
			while (true)
			{
				Socket clientSocket = serverSocket.accept();
				GerantDeClient client = new GerantDeClient(clientSocket);
				clients.add(client);
				new Thread(client).start();
			}
		} catch (IOException e)
		{
			System.err.println("Erreur du serveur : " + e.getMessage());
		}
	}

	public static void broadcast(String message, GerantDeClient sender)
	{
		for (GerantDeClient client : clients)
		{
			if (client != sender)
			{ // Ne pas envoyer le message à l'expéditeur
				client.sendMessage(message);
			}
		}
	}

	public static void removeClient(GerantDeClient client)
	{
		clients.remove(client);
	}

	static class GerantDeClient implements Runnable
	{
		private final Socket socket;
		private PrintWriter out;
		private String pseudo;

		public GerantDeClient(Socket socket)
		{
			this.socket = socket;
		}

		@Override
		public void run()
		{
			try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream())))
			{
				// Configuration de la sortie pour l'envoi des messages
				out = new PrintWriter(socket.getOutputStream(), true);

				// Récupération du pseudo
				pseudo = in.readLine();
				System.out.println("Nouveau client connecté : " + pseudo);

				// Annonce aux autres clients
				broadcast(pseudo + " a rejoint le chat.", this);

				// Lecture des messages des clients
				String message;
				while ((message = in.readLine()) != null)
				{
					if ("/quit".equalsIgnoreCase(message.trim()))
					{
						// Déconnexion propre
						System.out.println(pseudo + " s'est déconnecté.");
						break;
					}
					System.out.println(pseudo + " : " + message);
					broadcast(pseudo + " : " + message, this);
				}
			} catch (IOException e)
			{
				System.out.println("Erreur de connexion avec " + pseudo);
			} finally
			{
				// Nettoyage après la déconnexion
				ServeurChat.removeClient(this);
				broadcast(pseudo + " a quitté le chat.", this);
				try
				{
					socket.close();
				} catch (IOException e)
				{
					System.err.println("Erreur lors de la fermeture du socket : " + e.getMessage());
				}
			}
		}

		public void sendMessage(String message)
		{
			if (out != null)
			{
				out.println(message);
			}
		}
	}
}
