/**
 * 
 */
package de.unirostock.sems.cbarchive.web;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.xml.bind.DatatypeConverter;

import com.fasterxml.jackson.core.JsonProcessingException;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.web.dataholder.UserData;


// TODO: Auto-generated Javadoc
/**
 * The Class Tools.
 *
 * @author Martin Scharm
 */
public class Tools
{

	/** The Constant DATE_FORMATTER. */
	public static final SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss.SSS");

	public static UserManager doLogin( HttpServletRequest request, HttpServletResponse response ) throws CombineArchiveWebException, CombineArchiveWebCriticalException {
		// find Cookies
//		HttpSession session = request.getSession (true);
		CookieManager cookieManagement = new CookieManager (request, response);

		// gets the user class
		UserManager user = null;
		try {
			user = getUser(cookieManagement);
			if( user == null ) {
				user = new UserManager();
				storeUserCookies(cookieManagement, user);
			}
		}
		catch (IOException e) {
			throw new CombineArchiveWebCriticalException("Cannot find and/or obtain working directory", e);
		}

		return user;
	}

	/**
	 * Gets the user.
	 *
	 * @param cookies the cookies
	 * @return the user
	 * @throws IOException 
	 */
	public static UserManager getUser (CookieManager cookies) throws IOException
	{
		Cookie pathCookie = cookies.getCookie (Fields.COOKIE_PATH);
		if (pathCookie == null)
			return null;

		cookies.setCookie (pathCookie);

		Cookie userInfo		= cookies.getCookie( Fields.COOKIE_USER );

		UserManager user = new UserManager(pathCookie.getValue());
		if( userInfo != null && !userInfo.getValue().isEmpty() )
			user.setData( UserData.fromJson( userInfo.getValue() ) ); 

		storeUserCookies(cookies, user);

		return user;

	}

	public static void storeUserCookies(CookieManager cookies, UserManager user) {

		cookies.setCookie( new Cookie(Fields.COOKIE_PATH, user.getWorkspaceId()) );

		if( user.getData() != null && user.getData().hasInformation() ) {
			UserData userData = user.getData();
			try {
				cookies.setCookie(new Cookie( Fields.COOKIE_USER, userData.toJson() ));
			} catch (JsonProcessingException e) {
				LOGGER.error(e, "Cannot store cookies, due to json errors");
			}
		}

	}

	/**
	 * Extract file name.
	 *
	 * @param part the part
	 * @return the string
	 */
	public static final String extractFileName (Part part)
	{
		if (part != null)
		{
			String header = part.getHeader ("content-disposition");
			if (header != null)
			{
				LOGGER.debug ("content-disposition not null: ", header);
				String[] items = header.split (";");
				for (String s : items)
				{
					LOGGER.debug ("current disposition: ", s);
					if (s.trim ().startsWith ("filename"))
						return s.substring (s.indexOf ("=") + 2, s.length () - 1);
				}
			}
		}
		else
			LOGGER.debug ("file part seems to be null -> cannot extract file name.");
		return "UploadedFile-" + DATE_FORMATTER.format (new Date ());
	}

	public static String generateHashId( String input ) {
		try {
			byte[] hash = MessageDigest.getInstance(Fields.HASH_ALGO).digest( input.getBytes() );
			return DatatypeConverter.printHexBinary(hash);
		} catch (NoSuchAlgorithmException e) {
			// As fallback send the complete String
			return input;
		}
	}
	
	/**
	 * Returns false, if a quota is exceeded. Otherwise true
	 * 
	 * @param currentValue
	 * @param quota
	 * @return
	 */
	public static boolean checkQuota( long currentValue, long quota ) {
		
		// Quota is set to unlimited
		if( quota == Fields.QUOTA_UNLIMITED )
			return true;
		
		LOGGER.info(currentValue, " vs ", quota);
		
		// check if quota is exceeded
		if( currentValue >= quota )
			return false;
		else
			return true;
	}
	
}
