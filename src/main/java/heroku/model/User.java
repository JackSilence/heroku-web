package heroku.model;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class User extends Login {
	@FindBy( xpath = "/html/body/nav/div[3]/a[2]" )
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