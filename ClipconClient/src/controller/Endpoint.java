package controller;

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

import model.Group;
import model.Message;
import model.MessageDecoder;
import model.MessageEncoder;
import model.MessageParser;
import model.User;
import userInterface.UserInterface;

@ClientEndpoint(decoders = { MessageDecoder.class }, encoders = { MessageEncoder.class })
public class Endpoint {
	public final static String CONFIRM = "confirm";
	public final static String REJECT = "reject";
	
	private String uri = "ws://182.172.16.118:8080/websocketServerModule/ServerEndpoint";
	private Session session = null;

	private static Endpoint uniqueEndpoint;
	private static UserInterface ui;
	
	private User user;
	private Group group;

	public static Endpoint getIntance() {
		System.out.println("Endpoint getIntance()");
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
		System.out.println("Endpoint 생성자");
		URI uRI = new URI(uri);
		ContainerProvider.getWebSocketContainer().connectToServer(this, uRI);
		ui = UserInterface.getIntance();
	}

	@OnOpen
	public void onOpen(Session session) {
		this.session = session;
	}

	@OnMessage
	public void onMessage(Message message) {
		System.out.println("message type: " + message.getType());
		switch (message.get(Message.TYPE)) {
		case Message.REQUEST_SIGN_IN: // 로그인 요청에 대한 응답
			switch (message.get("result")) {
			case CONFIRM:
				System.out.println("sign in confirm");
				ui.getStartingScene().setFlag(true); // EntryView 보여줌
				user = MessageParser.getUserByMessage(message); // 서버에서 이메일, 닉네임, 주소록 받아 User 객체 생성
				break;
			case REJECT:
				System.out.println("sign in reject");
				ui.getStartingScene().showLoginFailPopup();
				break;
			}
			
			break;

		case Message.REQUEST_SIGN_UP:
			
			switch (message.get("response")) {
			case CONFIRM:
				System.out.println("sign up confirm");
				ui.getSignupScene().setFlag(true); // signUpView 닫음
				break;
			case REJECT:
				System.out.println("sign up reject");
				System.out.println("이메일/닉네임 중복");
				break;
			}
			
			break;

		case Message.REQUEST_CREATE_GROUP:
			
			switch (message.get("response")) {
			case CONFIRM:
				System.out.println("create group confirm");
				ui.getEntryScene().setFlag(true); // MainView 보여줌
				group = MessageParser.getGroupByMessage(message); // 서버에서 primaryKey, name 받아 Group 객체 생성 후
				user.setGroup(group); // User에 할당
				// 이후에 다른 User 들어올 때 마다 respond 받고 UI 갱신
				break;
			case REJECT:
				System.out.println("create group reject");
				break;
			}
			
			break;

		case Message.REQUEST_JOIN_GROUP:
			
			switch (message.get("response")) {
			case CONFIRM:
				System.out.println("join group confirm");
				ui.getEntryScene().setFlag(true); // MainView 보여줌
				// 서버에서 (primaryKey), name, 참여자 명단, 히스토리 받아 Group 객체 생성 후 User에 할당
				// 이후에 다른 User 들어올 때 마다 respond 받고 UI 갱신
				break;
			case REJECT:
				System.out.println("join group reject");
				break;
			}
			
			break;

		default:
			System.out.println("default");
			break;
		}
	}

	public void sendMessage(Message message) throws IOException, EncodeException {
		session.getBasicRemote().sendObject(message);
	}

	@OnClose
	public void onClose() {
		// 세션이 끊겼을 때 어떻게 할지 처리
	}
}