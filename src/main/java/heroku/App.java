package heroku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.scheduling.annotation.EnableScheduling;

import magic.controller.ExecuteController;

@SpringBootApplication
@EnableScheduling
@Import( ExecuteController.class )
public class App {
	public static void main( String[] args ) {
		SpringApplication.run( App.class, args );
	}
}