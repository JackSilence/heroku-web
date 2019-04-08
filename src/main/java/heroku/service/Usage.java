package heroku.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.http.client.fluent.Form;
import org.apache.http.client.fluent.Request;
import org.openqa.selenium.Dimension;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.Point;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.PageFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.google.gson.Gson;

import heroku.model.Billing;
import heroku.util.Utils;
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class Usage implements IService {
	private final Logger log = LoggerFactory.getLogger( this.getClass() );

	private static final String SCRIPT = "/heroku/template/script.js";

	private static final String UPLOAD_URI = "https://api.imgur.com/3/upload", IMAGE = "<img src='%s'>";

	@Autowired
	private IMailService service;

	@Value( "${heroku.account}" )
	private String[] account;

	@Value( "${heroku.password}" )
	private String password;

	@Value( "${GOOGLE_CHROME_SHIM:}" )
	private String bin;

	@Value( "${imgur.id}" )
	private String id;

	@Override
	@Scheduled( cron = "0 0 12,19 * * *" )
	public void exec() {
		WebDriver driver = init();

		String script = String.format( Utils.getResourceAsString( SCRIPT ), Billing.CSS_USAGE );

		StringBuilder sb = new StringBuilder();

		Arrays.stream( account ).forEach( i -> {
			driver.get( "https://dashboard.heroku.com/account/billing" );

			Billing billing = PageFactory.initElements( driver, Billing.class );

			billing.getEmail().sendKeys( Utils.decode( i ) );
			billing.getPassword().sendKeys( Utils.decode( password ) );
			billing.getLogin().click();

			sleep();

			( ( JavascriptExecutor ) driver ).executeScript( script );

			WebElement element = billing.getUsage();

			File screenshot = ( ( TakesScreenshot ) driver ).getScreenshotAs( OutputType.FILE );

			Point point = element.getLocation();

			Dimension size = element.getSize();

			int x = point.getX(), y = point.getY(), width = size.getWidth(), height = size.getHeight();

			try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				ImageIO.write( ImageIO.read( screenshot ).getSubimage( x, y, width, height ), "png", stream );

				String base64 = DatatypeConverter.printBase64Binary( stream.toByteArray() );

				Request request = Request.Post( UPLOAD_URI ).setHeader( "Authorization", "Client-ID " + id );

				request.bodyForm( Form.form().add( "image", base64 ).add( "type", "base64" ).build() );

				Map<?, ?> result = new Gson().fromJson( Utils.getEntityAsString( request ), Map.class );

				sb.append( String.format( IMAGE, ( ( Map<?, ?> ) result.get( "data" ) ).get( "link" ) ) ).append( "<br>" );

			} catch ( IOException e ) {
				log.error( "", e );

			}

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

		options.addArguments( "--headless", "--disable-gpu", "--window-size=1920,1080" );

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