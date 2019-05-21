package heroku.service;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.PageFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import heroku.model.Billing;
import magic.service.IMailService;
import magic.service.Selenium;
import magic.util.Utils;

@Service
public class Usage extends Selenium {
	private static final String SCRIPT = "/heroku/template/script.js", IMAGE = "<img src='%s'>";

	@Autowired
	private IMailService service;

	@Value( "${heroku.account}" )
	private String[] account;

	@Value( "${heroku.password}" )
	private String password;

	@Override
	@Scheduled( cron = "0 0 12,19 * * *" )
	public void exec() {
		run( "--window-size=1920,1080" );
	}

	@Override
	protected void run( WebDriver driver ) {
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

			images.add( screenshot( driver, billing.getUsage() ) );

			billing.getMenu().click();
			billing.getLogout().click();

			sleep();
		} );

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

		service.send( "Heroku Usage_" + time, String.format( IMAGE, Utils.upload( base64( image ), time ) ) );
	}
}