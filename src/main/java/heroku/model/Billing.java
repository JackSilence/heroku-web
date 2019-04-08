package heroku.model;

import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

public class Billing extends User {
	public static final String CSS_USAGE = "div.account-quota-usage > table.table";

	@FindBy( css = CSS_USAGE )
	private WebElement usage;

	public WebElement getUsage() {
		return usage;
	}
}