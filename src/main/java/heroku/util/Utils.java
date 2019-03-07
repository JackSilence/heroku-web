package heroku.util;

import java.util.Base64;

public class Utils {
	public static String decode( String str ) {
		return new String( Base64.getDecoder().decode( str ) );
	}
}