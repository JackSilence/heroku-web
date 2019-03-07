package heroku.model;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class Billing extends User {
	@FindBy( css = "div.account-quota-usage > table.table" )
	private WebElement usage;

	public WebElement getUsage() {
		return usage;
	}
}