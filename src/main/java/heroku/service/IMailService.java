package heroku.service;

public interface IMailService {
	void send( String subject, String content );
}