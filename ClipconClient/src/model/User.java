package model;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class User {
	private String name;
	private Group group;
	private String session;

	private StringProperty nameProperty;

	public User() {
	}

	public User(String name) {
		this.name = name;
		group = null;
		this.nameProperty = new SimpleStringProperty(name);
	}
}
