package heroku.service;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import heroku.model.Billing;
import heroku.util.Utils;
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class Usage implements IService {
	@Autowired
	private IMailService service;

	@Value( "${heroku.account}" )
	private String[] account;

	@Value( "${heroku.password}" )
	private String password;

	@Value( "${GOOGLE_CHROME_SHIM:}" )
	private String bin;

	@Override
	public void exec() {
		WebDriver driver = init();

		StringBuilder sb = new StringBuilder();

		Arrays.stream( account ).forEach( i -> {
			driver.get( "https://dashboard.heroku.com/account/billing" );

			Billing billing = PageFactory.initElements( driver, Billing.class );

			String email = Utils.decode( i ), text;

			sb.append( email ).append( ":<br>" );

			billing.getEmail().sendKeys( email );
			billing.getPassword().sendKeys( Utils.decode( password ) );
			billing.getLogin().click();

			sleep();

			text = billing.getUsage().getText().replace( "Find out more", "--" );

			sb.append( text.replaceAll( "(\r\n|\n)", "<br>" ) ).append( "<br><br>" );

			billing.getMenu().click();
			billing.getLogout().click();

			sleep();
		} );

		driver.quit();

		service.send( "Heroku Usage_" + new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() ), sb.toString() );
	}

	private WebDriver init() {
		ChromeOptions options = new ChromeOptions();

		if ( bin.isEmpty() ) {
			WebDriverManager.chromedriver().setup();

		} else {
			System.setProperty( "webdriver.chrome.driver", "/app/.chromedriver/bin/chromedriver" );

			options.setBinary( bin );

		}

		options.addArguments( "--headless", "--disable-gpu" );

		return new ChromeDriver( options );
	}

	private void sleep() {
		try {
			Thread.sleep( 5000 );

		} catch ( InterruptedException e ) {
			throw new RuntimeException( e );

		}
	}
}