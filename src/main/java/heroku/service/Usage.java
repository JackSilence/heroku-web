package heroku.service;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

import org.apache.commons.lang3.StringUtils;
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

	private static final String SCRIPT = "$('%s').css('width', '500px').find('> thead > th.pull-right').text('')";

	private static final String TEMPLATE = "/heroku/template/template.html", ROW = "/heroku/template/row.html";

	private static final String USAGE = "(.+?) free dyno hours (.+?) used this month";

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

		String row = Utils.getResourceAsString( ROW );

		StringBuilder sb = new StringBuilder();

		List<BufferedImage> images = new ArrayList<>();

		Arrays.stream( account ).forEach( i -> {
			driver.get( "https://dashboard.heroku.com/account/billing" );

			Billing billing = PageFactory.initElements( driver, Billing.class );

			String email = Utils.decode( i ), usage = StringUtils.EMPTY;

			billing.getEmail().sendKeys( email );
			billing.getPassword().sendKeys( Utils.decode( password ) );
			billing.getLogin().click();

			sleep();

			( ( JavascriptExecutor ) driver ).executeScript( String.format( SCRIPT, Billing.CSS_USAGE ) );

			WebElement element = billing.getUsage();

			Matcher matcher = Pattern.compile( USAGE ).matcher( element.getText() );

			usage = matcher.find() ? matcher.group( 1 ) + StringUtils.SPACE + matcher.group( 2 ) : usage;

			sb.append( String.format( row, StringUtils.substringBefore( email, "@" ), usage ) );

			File screenshot = ( ( TakesScreenshot ) driver ).getScreenshotAs( OutputType.FILE );

			Point point = element.getLocation();

			Dimension size = element.getSize();

			try {
				images.add( ImageIO.read( screenshot ).getSubimage( point.getX(), point.getY(), size.getWidth(), size.getHeight() ) );

			} catch ( IOException e ) {
			}

			billing.getMenu().click();
			billing.getLogout().click();

			sleep();
		} );

		driver.quit();

		String str = StringUtils.EMPTY;

		if ( !images.isEmpty() ) {
			int height = images.stream().mapToInt( BufferedImage::getHeight ).sum();

			BufferedImage image = new BufferedImage( images.get( 0 ).getWidth(), height, BufferedImage.TYPE_INT_RGB );

			Graphics g = image.getGraphics();

			for ( int i = 0; i < images.size(); i++ ) {
				if ( i == 0 ) {
					g.drawImage( images.get( 0 ), 0, 0, null );

				} else {
					g.drawImage( images.get( 1 ), 0, images.get( 0 ).getHeight(), null );
				}
			}

			g.dispose();

			try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
				ImageIO.write( image, "png", stream );

				String base64 = DatatypeConverter.printBase64Binary( stream.toByteArray() );

				Request request = Request.Post( UPLOAD_URI ).setHeader( "Authorization", "Client-ID " + id );

				request.bodyForm( Form.form().add( "image", base64 ).add( "type", "base64" ).build() );

				Map<?, ?> result = new Gson().fromJson( Utils.getEntityAsString( request ), Map.class );

				str = String.format( IMAGE, ( ( Map<?, ?> ) result.get( "data" ) ).get( "link" ) );

			} catch ( IOException e ) {
				log.error( "", e );

			}
		}

		String content = String.format( Utils.getResourceAsString( TEMPLATE ), sb.toString(), str );

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