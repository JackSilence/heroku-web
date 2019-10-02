package heroku.model;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class Login {
	@FindBy( xpath = "//*[@id=\"email\"]" )
	private WebElement email;

	@FindBy( xpath = "//*[@id=\"password\"]" )
	private WebElement password;

	@FindBy( xpath = "//*[@id=\"code\"]" )
	private WebElement code;

	@FindBy( xpath = "//*[@id=\"login\"]/form/button" )
	private WebElement login;

	public WebElement getEmail() {
		return email;
	}

	public WebElement getPassword() {
		return password;
	}

	public WebElement getCode() {
		return code;
	}

	public WebElement getLogin() {
		return login;
	}
}