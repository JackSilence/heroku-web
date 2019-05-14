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

import javax.imageio.ImageIO;
import javax.xml.bind.DatatypeConverter;

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

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import heroku.model.Billing;
import heroku.util.Utils;
import io.github.bonigarcia.wdm.WebDriverManager;

@Service
public class Usage implements IService {
	private final Logger log = LoggerFactory.getLogger( this.getClass() );

	private static final String SCRIPT = "/heroku/template/script.js";

	private static final String DATA_URI = "data:image/png;base64,%s", IMAGE = "<img src='%s'>";

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

		String script = String.format( Utils.getResourceAsString( SCRIPT ), Billing.CSS_USAGE );

		List<BufferedImage> images = new ArrayList<>();

		Arrays.stream( account ).forEach( i -> {
			driver.get( "https://dashboard.heroku.com/account/billing" );

			Billing billing = PageFactory.initElements( driver, Billing.class );

			billing.getEmail().sendKeys( Utils.decode( i ) );
			billing.getPassword().sendKeys( Utils.decode( password ) );
			billing.getLogin().click();

			sleep();

			( ( JavascriptExecutor ) driver ).executeScript( script );

			sleep();

			WebElement element = billing.getUsage();

			File screenshot = ( ( TakesScreenshot ) driver ).getScreenshotAs( OutputType.FILE );

			Point point = element.getLocation();

			Dimension size = element.getSize();

			int x = point.getX(), y = point.getY(), width = size.getWidth(), height = size.getHeight();

			try {
				images.add( ImageIO.read( screenshot ).getSubimage( x, y, width, height ) );

			} catch ( IOException e ) {
				log.error( "", e );

			}

			billing.getMenu().click();
			billing.getLogout().click();

			sleep();
		} );

		driver.quit();

		if ( images.isEmpty() ) {
			return;

		}

		int height = images.stream().mapToInt( BufferedImage::getHeight ).sum();

		BufferedImage image = new BufferedImage( images.get( 0 ).getWidth(), height, BufferedImage.TYPE_INT_RGB );

		Graphics g = image.getGraphics();

		for ( int i = 0; i < images.size(); i++ ) {
			g.drawImage( images.get( i ), 0, i == 0 ? height = 0 : ( height += images.get( i - 1 ).getHeight() ), null );

		}

		g.dispose();

		String time = new SimpleDateFormat( "yyyyMMddHH" ).format( new Date() );

		try (ByteArrayOutputStream stream = new ByteArrayOutputStream()) {
			ImageIO.write( image, "png", stream );

			String file = String.format( DATA_URI, DatatypeConverter.printBase64Binary( stream.toByteArray() ) );

			Map<?, ?> result = new Cloudinary().uploader().upload( file, ObjectUtils.asMap( "public_id", time ) );

			service.send( "Heroku Usage_" + time, String.format( IMAGE, result.get( "secure_url" ) ).toString() );

		} catch ( IOException e ) {
			log.error( "", e );

		}
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