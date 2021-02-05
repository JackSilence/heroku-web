package heroku.model;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class Login {
	@FindBy( xpath = "//*[@id=\"email\"]" )
	private WebElement email;

	@FindBy( xpath = "//*[@id=\"password\"]" )
	private WebElement password;

	@FindBy( xpath = "//*[@id=\"input-9\"]" )
	private WebElement code;

	@FindBy( xpath = "//*[@id=\"login\"]/form/button" )
	private WebElement login;

	@FindBy( xpath = "//*[@id=\"root\"]/vaasdist-verify/div/vaas-verify/div/vaas-verify-totp/vaas-container/div/div/slot/div/form/div/vaas-button-brand/button" )
	private WebElement verify;

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

	public WebElement getVerify() {
		return verify;
	}
}