package heroku.model;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class User extends Login {
	@FindBy( css = "body > div.glostick > nav > div.glostick__user > a.glostick__menu-icon.glostick__menu-icon--account" )
	private WebElement menu;

	@FindBy( xpath = "//*[@id=\"glostick__menu--account\"]/li[4]/a" )
	private WebElement logout;

	public WebElement getMenu() {
		return menu;
	}

	public WebElement getLogout() {
		return logout;
	}
}