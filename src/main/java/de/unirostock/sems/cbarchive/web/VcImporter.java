package de.unirostock.sems.cbarchive.web;
import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.jdom2.JDOMException;

import com.aragost.javahg.Changeset;
import com.aragost.javahg.Repository;
import com.aragost.javahg.commands.LogCommand;
import com.google.common.io.Files;

import de.binfalse.bflog.LOGGER;
import de.unirostock.sems.cbarchive.ArchiveEntry;
import de.unirostock.sems.cbarchive.CombineArchive;
import de.unirostock.sems.cbarchive.CombineArchiveException;
import de.unirostock.sems.cbarchive.meta.OmexMetaDataObject;
import de.unirostock.sems.cbarchive.meta.omex.OmexDescription;
import de.unirostock.sems.cbarchive.meta.omex.VCard;
import de.unirostock.sems.cbarchive.web.dataholder.Archive;
import de.unirostock.sems.cbarchive.web.dataholder.ArchiveFromCellMl;


/**
 * 
 */

/**
 * @author Martin Scharm
 *
 */
public class VcImporter
{
	/**
	 * @param archive
	 * @param id 
	 * @param user 
	 * @return true or false.
	 * @throws IOException 
	 * @throws TransformerException 
	 * @throws CombineArchiveException 
	 * @throws ParseException 
	 * @throws JDOMException 
	 * @throws CombineArchiveWebException 
	 */
	public static boolean importRepo (ArchiveFromCellMl archive, String id, UserManager user) throws IOException, TransformerException, JDOMException, ParseException, CombineArchiveException, CombineArchiveWebException
	{
		String link = archive.getCellmlLink ();
		
		File archiveFile = user.getArchiveFile (id);
		archive.setArchiveFile (archiveFile);
		CombineArchive ca = archive.getArchive ();
		// archive.getArchive ();
		
		File f = Files.createTempDir ();
		Repository repo = Repository.clone(f, link);
		if( repo == null )
		{
			LOGGER.error ("Cannot clone Mercurial Repository " + link + " into " + f);
			return false;
		}
		
		List<File> relevantFiles = scanRepository(f, repo);
		System.out.println ("before LogCommand");
		LogCommand logCmd = new LogCommand(repo);
		System.out.println ("after LogCommand");
		for (File cur : relevantFiles)
		{
			List<Changeset> relevantVersions = logCmd.execute(cur.getAbsolutePath ());
			
			ArchiveEntry caFile = ca.addEntry (
			   f,
			   cur, 
			   // TODO
			   "stuff");
			
			// lets create meta!
			List<Date> modified = new ArrayList<Date> ();
			List<VCard> creators = new ArrayList<VCard> ();
			
			HashMap<String, VCard> users = new HashMap<String, VCard> ();
			for (Changeset cs : relevantVersions)
			{
				LOGGER.debug ("cs: " + cs.getTimestamp ().getDate () + " -- " + cs.getUser ());
				modified.add (cs.getTimestamp ().getDate ());
			
				String vcuser = cs.getUser ();
				String firstName = "";
				String lastName = "";
				String mail = "";
				
				String[] tokens = vcuser.split (" ");
				int lastNameToken = tokens.length - 1;
				// is there a mail address?
				if (tokens[lastNameToken].contains ("@"))
				{
					mail = tokens[lastNameToken];
					if (mail.startsWith ("<") && mail.endsWith (">"))
						mail = mail.substring (1, mail.length () - 1);
					lastNameToken--;
				}
				
				// search for a non-empty last name
				while (lastNameToken >= 0)
				{
					if (tokens[lastNameToken].length () > 0)
					{
						lastName = tokens[lastNameToken];
						break;
					}
					lastNameToken--;
				}
				
				// and first name of course...
				for (int i = 0; i < lastNameToken; i++)
					if (tokens[i].length () > 0)
						firstName += tokens[i] + " ";
				firstName = firstName.trim ();
				
				String userid = "[" + firstName + "] -- [" + lastName + "] -- [" + mail + "]";
				LOGGER.debug ("this is user: " + userid);
				if (users.get (userid) == null)
				{
					users.put (userid, new VCard (lastName, firstName, mail, null));
				}
			}
			
			for (VCard vc : users.values ())
				creators.add (vc);

			caFile.addDescription (new OmexMetaDataObject (new OmexDescription (
        creators, modified, modified.get (modified.size () - 1))));
			
		}
		ca.pack ();
		ca.close ();
		repo.close ();
		return true;
	}
	
	
	
	private static List<File> scanRepository ( File location, Repository repo )
	{
		List<File> relevantFiles = new ArrayList<File>();

		// scans the directory recursively
		scanRepositoryDir( location, location, relevantFiles );

		return relevantFiles;
	}

	private static void scanRepositoryDir( File base, File dir, List<File> relevantFiles ) {
		
		String[] entries = dir.list();
		// nothing to scan in this dir
		if( entries == null )
			return;

		// looping throw all directory elements
		for( int index = 0; index < entries.length; index++ ) {
			File entry = new File( dir, entries[index] );

			if( entry.isDirectory() && entry.exists() && !entry.getName().startsWith(".") ) {
				// Entry is a directory and not hidden (begins with a dot) -> recursive
				scanRepositoryDir(base, entry, relevantFiles);
			}
			else if( entry.isFile() && entry.exists() ) {
				// Entry is a file -> check if it is relevant
				
				relevantFiles.add (entry);
			}

		}

	}
}