package heroku.service;

import java.util.concurrent.TimeUnit;

import org.openqa.selenium.WebDriver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import magic.service.Selenium;
import magic.util.Utils;
import net.gpedro.integrations.slack.SlackMessage;

@Service
public class Logz extends Selenium {
    @Value( "${logz.account}" )
    private String account;

    @Value( "${logz.password}" )
    private String password;

    @Override
    @Scheduled( cron = "0 0 16 * * *" )
    public void exec() {
        run();
    }

    @Override
    protected void run( WebDriver driver ) {
        driver.manage().timeouts().implicitlyWait( 10, TimeUnit.SECONDS );

        driver.get( "https://app.logz.io/#/login" );

        find( driver, "input[name='email']" ).sendKeys( Utils.decode( account ) );
        find( driver, "input[name='password']" ).sendKeys( Utils.decode( password ) );

        find( driver, "div.block-button.login" ).click();

        slack.call( new SlackMessage( "Logz login successfully" ) );
    }
}