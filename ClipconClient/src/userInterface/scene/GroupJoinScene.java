package userInterface.scene;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

import javax.websocket.EncodeException;

import application.Main;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;
import model.message.Message;
import server.Endpoint;
import userInterface.UserInterface;
import userInterface.dialog.Dialog;
import userInterface.dialog.PlainDialog;

public class GroupJoinScene implements Initializable {

	private UserInterface ui = UserInterface.getInstance();

	@FXML
	private TextField groupKey;
	@FXML
	private Button confirmBtn, XBtn;

	private Dialog dialog;

	private Endpoint endpoint = Endpoint.getInstance();

	@Override public void initialize(URL location, ResourceBundle resources) {
		ui.setGroupJoinScene(this);

		groupKey.setOnKeyPressed(new EventHandler<KeyEvent>() {
			@Override public void handle(KeyEvent event) {
				if (event.getCode().equals(KeyCode.ENTER)) {
					if (groupKey.getText().length() == 0) {
						notInputGroupKey();
					}
					else {
						sendGroupJoinMessage();
					}
				}
			}
		});

		confirmBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent event) {
				if (groupKey.getText().length() == 0) {
					notInputGroupKey();
				}
				else {
					sendGroupJoinMessage();
				}
			}
		});

		XBtn.setOnAction(new EventHandler<ActionEvent>() {
			@Override public void handle(ActionEvent event) {
				try {
					Parent goBack = FXMLLoader.load(getClass().getResource("/view/StartingView.fxml"));
					Scene scene = new Scene(goBack);
					Stage backStage = Main.getPrimaryStage();

					backStage.setScene(scene);
					backStage.show();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});
	}

	public void notInputGroupKey() {
		dialog = new PlainDialog("Group key �� �Է��ϼ���.", false);
		dialog.showAndWait();
	}

	// send REQUEST_JOIN_GROUP Messgae to server
	public void sendGroupJoinMessage() {
		if (groupKey.getText().length() != 0) {

			Message signUpMsg = new Message().setType(Message.REQUEST_JOIN_GROUP);
			signUpMsg.add(Message.GROUP_PK, groupKey.getText());
			endpoint.sendMessage(signUpMsg);
		}
	}

	public void failGroupJoin() {
		Platform.runLater(() -> {
			dialog = new PlainDialog("��ȿ���� �ʴ� Group Key �Դϴ�. �ٽ� �Է��ϼ���.", false);
			dialog.showAndWait();
			groupKey.setText("");
		});
	}

	public void showMainView() {
		Platform.runLater(() -> {
			try {
				Parent toMain = FXMLLoader.load(getClass().getResource("/view/MainView.fxml"));
				Scene mainScene = new Scene(toMain);
				Stage primaryStage = Main.getPrimaryStage();

				primaryStage.setScene(mainScene);
				primaryStage.show();

			} catch (Exception e) {
				e.printStackTrace();
			}
		});
	}

}
