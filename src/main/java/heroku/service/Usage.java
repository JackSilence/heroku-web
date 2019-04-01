package heroku.service;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.Dimension;
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

import heroku.model.Billing;
import heroku.util.Utils;
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class Usage implements IService {
	private final Logger log = LoggerFactory.getLogger( this.getClass() );

	private static final String TEMPLATE = "/heroku/template/template.html", ROW = "/heroku/template/row.html";

	private static final String IMAGE = "<img src='data:image/png;base64,%s'>";

	private static final String USAGE = "(.+?) free dyno hours (.+?) used this month";

	@Autowired
	private IMailService service;

	@Value( "${heroku.account}" )
	private String[] account;

	@Value( "${heroku.password}" )
	private String password;

	@Value( "${GOOGLE_CHROME_SHIM:}" )
	private String bin;

	@Override
	@Scheduled( cron = "0 0 12,19 * * *" )
	public void exec() {
		WebDriver driver = init();

		String row = Utils.getResourceAsString( ROW );

		StringBuilder sb1 = new StringBuilder(), sb2 = new StringBuilder();

		Arrays.stream( account ).forEach( i -> {
			driver.get( "https://dashboard.heroku.com/account/billing" );

			Billing billing = PageFactory.initElements( driver, Billing.class );

			String email = Utils.decode( i ), usage = StringUtils.EMPTY;

			billing.getEmail().sendKeys( email );
			billing.getPassword().sendKeys( Utils.decode( password ) );
			billing.getLogin().click();

			sleep();

			WebElement element = billing.getUsage();

			Matcher matcher = Pattern.compile( USAGE ).matcher( element.getText() );

			usage = matcher.find() ? matcher.group( 1 ) + StringUtils.SPACE + matcher.group( 2 ) : usage;

			sb1.append( String.format( row, StringUtils.substringBefore( email, "@" ), usage ) );

			File screenshot = ( ( TakesScreenshot ) driver ).getScreenshotAs( OutputType.FILE );

			Point point = element.getLocation();

			Dimension size = element.getSize();

			int x = point.getX(), y = point.getY(), width = size.getWidth(), height = size.getHeight();

			try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				ImageIO.write( ImageIO.read( screenshot ).getSubimage( x, y, width, height ), "png", stream );

				sb2.append( String.format( IMAGE, DatatypeConverter.printBase64Binary( stream.toByteArray() ) ) ).append( "<br>" );

			} catch ( IOException e ) {
				log.error( "", e );

			}

			billing.getMenu().click();
			billing.getLogout().click();

			sleep();
		} );

		driver.quit();

		String content = String.format( Utils.getResourceAsString( TEMPLATE ), sb1.toString(), sb2.toString() );

		service.send( "Heroku Usage_" + new SimpleDateFormat( "yyyy-MM-dd" ).format( new Date() ), content );
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