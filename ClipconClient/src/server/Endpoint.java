package server;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.websocket.ClientEndpoint;
import javax.websocket.ContainerProvider;
import javax.websocket.DeploymentException;
import javax.websocket.EncodeException;
import javax.websocket.OnClose;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;

import application.Main;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import model.Contents;
import model.User;
import model.message.Message;
import model.message.MessageDecoder;
import model.message.MessageEncoder;
import model.message.MessageParser;
import userInterface.UserInterface;
import userInterface.dialog.Dialog;
import userInterface.dialog.PlainDialog;

@ClientEndpoint(decoders = { MessageDecoder.class }, encoders = { MessageEncoder.class })
public class Endpoint {

	private final String PROTOCOL = "ws://";
	private final String CONTEXT_ROOT = "globalclipboard/ServerEndpoint";
	private final String uri = PROTOCOL + Main.SERVER_URI_PART + CONTEXT_ROOT;

	private Session session = null;
	private static Endpoint uniqueEndpoint;
	private static UserInterface ui;
	private Dialog dialog;

	public static User user;

	public static Endpoint getInstance() {
		try {
			if (uniqueEndpoint == null) {
				uniqueEndpoint = new Endpoint();
			}
		} catch (DeploymentException | IOException | URISyntaxException e) {
			e.printStackTrace();
		}

		return uniqueEndpoint;
	}

	public Endpoint() throws DeploymentException, IOException, URISyntaxException {
		URI uRI = new URI(uri);
		ContainerProvider.getWebSocketContainer().connectToServer(this, uRI);
		ui = UserInterface.getInstance();
		new PingPong().start();
	}

	@OnOpen public void onOpen(Session session) {
		this.session = session;

		// send hello message: sesion id information
		Message hello = new Message().setType(Message.HELLO);
		hello.add(Message.SERVERMSG, session.getId());
		sendMessage(hello);
	}

	@OnMessage public void onMessage(Message message) {
		System.out.println("message type: " + message.get(Message.TYPE) + " - " + Main.getTime());
		switch (message.get(Message.TYPE)) {

		case Message.HELLO:
			String serverSssion = message.get(Message.SESSION);
			user.setSession(serverSssion);
			break;

		case Message.RESPONSE_CONFIRM_VERSION:
			switch (message.get(Message.RESULT)) {
			case Message.REJECT:
				Platform.runLater(() -> {
					dialog = new PlainDialog("You have to download update version http://113.198.84.53/globalclipboard/download", false);
					dialog.showAndWait();
				});
				break;
			}
			break;

		case Message.RESPONSE_CREATE_GROUP:
			switch (message.get(Message.RESULT)) {
			case Message.CONFIRM:
				ui.getStartingScene().showMainView(); // Show MainView
				user = MessageParser.getUserAndGroupByMessage(message); // create Group Object using primaryKey, name(get from server) and set to user

				while (true) {
					if (ui.getMainScene() != null && user != null) {
						break;
					}
					System.out.print(""); // [TODO] UI refresh
				}

				ui.getMainScene().initGroupParticipantList(); // UI list initialization
				break;
			}
			break;

		case Message.RESPONSE_CHANGE_NAME:
			switch (message.get(Message.RESULT)) {
			case Message.CONFIRM:
				String changeName = message.get(Message.CHANGE_NAME);

				System.out.println("changeName : " + changeName);

				user.setName(changeName);
				user.getGroup().getUserList().get(0).setName(changeName);
				user.getGroup().getUserList().get(0).setNameProperty(new SimpleStringProperty(changeName));

				ui.getMainScene().closeNicknameChangeStage();
				ui.getMainScene().initGroupParticipantList(); // UI list initialization
				break;
			}
			break;

		case Message.RESPONSE_JOIN_GROUP:
			switch (message.get(Message.RESULT)) {
			case Message.CONFIRM:
				ui.getGroupJoinScene().showMainView(); // close group join and show MainView
				user = MessageParser.getUserAndGroupByMessage(message); // create Group Object using primaryKey, name(get from server) and set to user

				while (true) {
					if (ui.getMainScene() != null && user != null) {
						break;
					}
					System.out.print(""); // [TODO] UI refresh
				}

				ui.getMainScene().initGroupParticipantList(); // UI list initialization
				break;

			case Message.REJECT:
				ui.getGroupJoinScene().failGroupJoin(); // UI list initialization
				break;
			}
			break;

		case Message.RESPONSE_EXIT_GROUP:
			while (true) {
				if (ui.getMainScene() != null) {
					break;
				}
			}

			ui.getMainScene().showStartingView(); // show StartingView
			user = null;
			break;

		case Message.NOTI_ADD_PARTICIPANT: // receive a message when another user enters the group and updates the UI
			User newParticipant = new User(message.get(Message.PARTICIPANT_NAME));

			user.getGroup().getUserList().add(newParticipant);
			ui.getMainScene().getGroupParticipantList().add(newParticipant);
			ui.getMainScene().addGroupParticipantList(); // update UI list
			break;

		case Message.NOTI_CHANGE_NAME:
			String name = message.get(Message.NAME);
			String changeName = message.get(Message.CHANGE_NAME);

			for (int i = 0; i < user.getGroup().getUserList().size(); i++) {
				if (user.getGroup().getUserList().get(i).getName().equals(name)) {
					user.getGroup().getUserList().remove(i);
					user.getGroup().getUserList().add(i, new User(changeName));
				}
			}

			ui.getMainScene().initGroupParticipantList(); // UI list initialization
			break;

		case Message.NOTI_EXIT_PARTICIPANT:
			for (int i = 0; i < user.getGroup().getUserList().size(); i++) {
				if (message.get(Message.PARTICIPANT_NAME).equals(user.getGroup().getUserList().get(i).getName())) {
					int removeIndex = i;
					user.getGroup().getUserList().remove(removeIndex);
					break;
				}
			}

			ui.getMainScene().initGroupParticipantList(); // update UI list
			break;

		case Message.NOTI_UPLOAD_DATA:
			Contents contents = MessageParser.getContentsbyMessage(message);

			user.getGroup().addContents(contents);

			ui.getMainScene().getHistoryList().add(0, contents);
			ui.getMainScene().addContentsInHistory(); // update UI list

			break;

		case Message.PONG:
			break;

		case Message.SERVERMSG:
			String msg = message.get(Message.CONTENTS);
			Platform.runLater(() -> {
				Dialog plainDialog = new PlainDialog(msg, false);
				plainDialog.showAndWait();
			});
			break;

		default:
			break;
		}
	}

	public void sendMessage(Message message) {
		try {
			session.getBasicRemote().sendObject(message);
		} catch (IOException e) {
			System.err.println(getClass().getName() + ". error at sending message. " + e.getMessage());
		} catch (EncodeException e) {
			System.err.println(getClass().getName() + ". error at sending message. " + e.getMessage());
		}
	}

	@OnClose public void onClose() {
		System.out.println("[on Close]");
		Platform.runLater(() -> {
			dialog = new PlainDialog("�������� ������ ������ϴ�.", true);
			dialog.showAndWait();
		});
	}

	class PingPong extends Thread {
		@Override public void run() {
			while (true) {
				try {
					Thread.sleep(3 * 60 * 1000);
					sendMessage(new Message().setType(Message.PING));

				} catch (InterruptedException e) {
					System.out.println("[ERROR] Pingping thread - InterruptedException");
				}
			}
		}
	}
}