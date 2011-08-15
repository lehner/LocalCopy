/*
 * Copyright (C) 2011 Christoph Lehner
 * 
 * Modifications by:
 * - Julien Rioux (2010):  Download all arXiv versions (adapted from Julien's code),
 *                         add descriptions to files.
 *
 * All programs in this directory and subdirectories are published under the GNU
 * General Public License as described below.
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place, Suite 330, Boston, MA 02111-1307 USA
 *
 * Further information about the GNU GPL is available at:
 * http://www.gnu.org/copyleft/gpl.ja.html
 *
 */
package localcopy;

import net.sf.jabref.gui.*;
import net.sf.jabref.*;
import net.sf.jabref.labelPattern.LabelPatternUtil;

import java.lang.reflect.*;
import java.util.*;
import java.util.regex.*;
import java.io.*;
import javax.swing.*;
import java.awt.Component;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;

public class Download {

    private BasePanel panel;
    private Console console;
    private Component dialog;
    private DownloadHttpSession session;

    public Download(BasePanel panel, Component dialog, Console console) {
	this.panel = panel;
	this.console = console;
	this.dialog = dialog;
	this.session = new DownloadHttpSession(console);
    }

    private boolean hasFile(BibtexEntry b, String fn) {
	FileListTableModel m = new FileListTableModel();
	m.setContent(b.getField("file"));
	for (int j=0;j<m.getRowCount();j++) {
	    FileListEntry f = m.getEntry(j);
	    if (f.getLink().replace("\\","/").equals(fn.replace("\\","/")))
		return true;
	}
	return false;
    }

    private void addFile(BibtexEntry b, String fn, String desc) {
	if (!hasFile(b,fn)) {
	    FileListTableModel m = new FileListTableModel();
	    m.setContent(b.getField("file"));
	    FileListEntry e = new FileListEntry(desc,fn,Globals.prefs.getExternalFileTypeByExt("pdf"));
	    m.addEntry(0,e);
	    b.setField("file",m.getStringRepresentation());
	    panel.markBaseChanged();
	}
    }

    private String getDirFn() {

	String dirFn = null;

	if (panel != null) {
	    MetaData md = panel.metaData();
	    if (md != null) {
		dirFn = md.getFileDirectory("file");
		if (dirFn != null && dirFn.length() != 0)
		    return dirFn;
		dirFn = md.getFileDirectory("pdf");
		if (dirFn != null && dirFn.length() != 0)
		    return dirFn;
	    }
	}
	
	return null;
    }

    private void downloadFile(BibtexEntry b, String fn, String urlFn, String desc) {
	
	String dirFn = getDirFn();
	if (dirFn == null) {
	    console.output("! please set the main file directory or the pdf file directory in the preferences menu first.\n",true);
	    console.setProgressBarEnabled(false);
	    return;
	} else {	

	    File dir = new File(dirFn);
	    File tmpFile = session.downloadFile(dirFn, urlFn);

	    if (tmpFile != null) {

		/*
		 * Temporary file created, download successful.
		 * Next: Rename to proper filename.
		 */

		while (true) {
		    File file = new File(dir,fn);		

		    if (file.exists()) {
			Object[] options = {"Overwrite file.", "Use a different filename.", "Cancel download."};
			int n = JOptionPane.showOptionDialog(dialog,"A file named '" + fn + "' already exists.  What shall I do?",
							     "File already exists",
							     JOptionPane.YES_NO_OPTION,
							     JOptionPane.QUESTION_MESSAGE,
							     null,options,options[1]);
			if (n == 2)
			    return;
			else if (n == 1) {
			    String k = (String)JOptionPane.showInputDialog(dialog,"Please specify the filename to be used.",
									   "New filename",
									   JOptionPane.PLAIN_MESSAGE,
									   null,
									   null,
									   fn);
			    if (k == null)
				return;
			    
			    fn = k;
			    continue;
			}
		    }

		    /*
		     * Filename fn does not exist (or should be overwritten), try to rename temporary file.
		     */
		    if (file.exists()) {
			file.delete();
		    }
		    if (!tmpFile.renameTo(file)) {
			String k = (String)JOptionPane.showInputDialog(dialog,"The filename " + fn + 
								       " seems to be invalid on your filesystem.\n" +
								       "Please specify a different filename to be used.",
								       "New filename",
								       JOptionPane.PLAIN_MESSAGE,
								       null,
								       null,
								       fn);
			if (k == null)
			    return;
			fn = k;
			continue;
		    }

		    /*
		     * Filename fn now exists, temporary file no more
		     */
		    console.output("* file " + fn + " downloaded successfully\n",false);
		    addFile(b,fn,desc);
		    break;
		}
	    }	    
	}
    }

    private String osFormatFn(String s) {
	String os = System.getProperty("os.name");
	// request by john kehayias: format files so they can be used in windows
	//if (os.toLowerCase().indexOf("windows")!=-1) {
	s=s.replace(':','_');
	//}
	return s;
    }

    public void downloadEprintFile(BibtexEntry b, String fn, String rem, String desc) {
	int n=0;
	if (hasFile(b,fn)) {
	    Object[] options = {"Overwrite old file.", "Keep old file."};
	    n = JOptionPane.showOptionDialog(dialog,"A file named '" + fn + "' is already linked.  What shall I do?",
					     "File already linked",
					     JOptionPane.YES_NO_OPTION,
					     JOptionPane.QUESTION_MESSAGE,
					     null,options,options[1]);
	}
	if (n == 0) {
	    downloadFile(b,fn,rem,desc);
	}
    }

    public void downloadEprint(BibtexEntry b, String id) {
	String key = FNTemplate.getFN("arXiv",panel,b);
	if (key == null) {
	    console.output("! download canceled by user.\n",true);
	    return;
	}

	id = id.replace("arXiv:","");

	// first get list of available versions
	int maxver;
	for (maxver=2;maxver<9;maxver++) {
	    try {
		String ct = (new URL("http://arxiv.org/pdf/" + id + "v" + maxver)).openConnection().getContentType();
		if (ct==null || !ct.toLowerCase().startsWith("application/pdf"))
		    break;		
	    } catch (IOException e) {
		break;
	    }
	}
	maxver--;


	boolean downloadAll = false;
	if (maxver>1) {
	    // offer to download all versions
	    Object[] options = {"Download latest version.", 
				"Download all versions."};
	    String s = (String)JOptionPane.showInputDialog(dialog,"More than one arXiv version is available (" + maxver + ").",
							   "Download arXiv version",
							   JOptionPane.QUESTION_MESSAGE,
							   null,options,options[0]);
	    if (s != null) {
		if (s.equals(options[1])) {
		    downloadAll = true;
		}
	    } else
		return;
	}

	if (!downloadAll) {
	    downloadEprintFile(b, osFormatFn(key + "v" + maxver + ".pdf"),
			       "http://arxiv.org/pdf/" + id + "v" + maxver, "arXiv v" + maxver);
	} else {
	    for (int vid=1;vid<=maxver;vid++) {
		downloadEprintFile(b, osFormatFn(key + "v" + vid + ".pdf"),
				   "http://arxiv.org/pdf/" + id + "v" + vid, "arXiv v" + vid);
	    }
	}
    }
    
    boolean isPDFLink(String urlFn) {

	try {
	    URL url = new URL(urlFn);
	    URLConnection con = session.openURL(url);
	    if (con.getContentType().toLowerCase().startsWith("application/pdf")) {
		return true;
	    }

	} catch (MalformedURLException e) {
	    console.output("! test for pdf url '" + e.getLocalizedMessage() + "' is malformed.\n",true);
	} catch (SecurityException e) {
	    console.output("! could not test format of file '" + urlFn + "' due to security problems.\n",true);
	    console.output("! " + e.getLocalizedMessage() + "\n",true);
	} catch (Exception e) {
	    console.output("! could not test format of file '" + urlFn + "'.\n",true);
	    console.output("! " + e.getLocalizedMessage() + "\n",true);
	}
	
	return false;
    }
    
    private HttpURLConnection logIn(HtmlPageLoginForm lf) throws IOException {
	IDItem it = IDManager.getID(lf.baseUrl.getHost(),lf.userFieldName + ":" + lf.passwordFieldName);
	if (it == null)
	    throw new IOException("operation canceled by user");
	
	String query = lf.query;
	if (query.length() > 0)
	    query += "&";
	query+= URLEncoder.encode(lf.userFieldName,"UTF-8") + "=" + URLEncoder.encode(it.getUserID(),"UTF-8");
	query+= "&" + URLEncoder.encode(lf.passwordFieldName,"UTF-8") + "=" + URLEncoder.encode(it.getPassword(),"UTF-8");
	
	URL u = new URL(lf.baseUrl,lf.action);
	if (lf.method.equalsIgnoreCase("get")) {
	    if (u.getQuery() != null && u.getQuery().length() > 0) {
		u = new URL(lf.baseUrl,lf.action + "&" + query);
	    } else {
		u = new URL(lf.baseUrl,lf.action + "?" + query);
	    }
	} else if (lf.method.equalsIgnoreCase("post")) {
	    return session.openURL(u,query);
	} else {
	    throw new IOException("unknown form method: " + lf.method);
	}

	return session.openURL(u);
    }

    private String selectPDFFile(URL baseUrl, String[] links, String[] captions, String[] titles, String[] classes) throws IOException {
	String list = "";
	String[] opts = { "Present list of pdf files to user",
			  "Select n-th pdf file",
			  "Select n-th pdf file from inverse list",
			  "Select pdf file that contains a certain text",
			  "Select pdf link whose caption contains a certain text",
			  "Select pdf link whose title contains a certain text",
			  "Select pdf link whose class contains a certain text"
	};

	int k;
	for (k=0;k<links.length;k++)
	    list += "\n" + (k+1) + ": " + links[k] + "\n  \"" + captions[k] + "\"\n";
	PolicyItem po = PolicyManager.getPolicy(baseUrl.getHost(),"On multiple pdf files",
						"The page contains more than one link to a pdf file.\n" +
						"How shall the proper pdf file be selected?\n" + list, opts);
	if (po == null)
	    throw new IOException("operation canceled by user");
	
	if (po.getSel().equals(opts[0])) {
	    String s = (String)JOptionPane.showInputDialog(dialog,
							   "More than one pdf file is linked on the journal page " +
							   "corresponding to the selected article.\nPlease select " +
							   "the file that I should download:",
							   "Link selection is not unique",
							   JOptionPane.QUESTION_MESSAGE,
							   null,links,links[0]);
	    if (s != null) {
		return s;
	    } else
		throw new IOException("operation canceled by user");
	    
	} else if (po.getSel().equals(opts[1])) {
	    po = PolicyManager.getPolicy(baseUrl.getHost(),"Index of pdf file",
					 "Enter the index of the pdf file to be selected.\n" +
					 "The first pdf files is 1.", "#|Index: |1|1|1000");
	    if (po == null)
		throw new IOException("operation canceled by user");
	    
	    k = po.getVal() - 1;
	    if (k < 0)
		k = 0;
	    else if (k >= links.length) {
		console.output("- warning: policy states to select " + (k+1) + "-th pdf file, but only " + links.length 
			       + " are available!\n",false);
		k = links.length - 1;
	    }
	    return links[k];
		    
	} else if (po.getSel().equals(opts[2])) {
	    po = PolicyManager.getPolicy(baseUrl.getHost(),"Index of pdf file",
					 "Enter the index of the pdf file to be selected.\n" +
					 "The count starts with the last pdf file, i.e.,\n" +
					 "the last pdf files is 1.", "#|Index: |1|1|1000");
	    if (po == null)
		throw new IOException("operation canceled by user");
	    
	    k = po.getVal() - 1;
	    if (k < 0)
		k = 0;
	    else if (k >= links.length) {
		console.output("- warning: policy states to select " + (k+1) + "-th pdf file (inverse list), but only " + links.length 
			       + " are available!\n",false);
		k = links.length - 1;
	    }
	    return links[links.length - 1 - k];
	    
	} else if (po.getSel().equals(opts[3])) {
	    po = PolicyManager.getPolicy(baseUrl.getHost(),"Text within pdf file",
					 "Enter the text that has to appear within a pdf file name.\n" +
					 "The test is not case sensitive.", "$|Text: |article|50");
	    if (po == null)
		throw new IOException("operation canceled by user");
	    
	    for (k=0;k<links.length;k++) {
		if (links[k].toLowerCase().contains(po.getSel().toLowerCase()))
		    break;
	    }
	    
	    if (k<links.length)
		return links[k];
	    else {
		console.output("! warning: policy states to select pdf link that contains '" + po.getSel() + "' but no such link exists!\n",true);
	    }
	} else if (po.getSel().equals(opts[4])) {
	    po = PolicyManager.getPolicy(baseUrl.getHost(),"Text within pdf link caption",
					 "Enter the text that has to appear within a pdf link caption.\n" +
					 "The test is not case sensitive.", "$|Text: |PDF|50");
	    if (po == null)
		throw new IOException("operation canceled by user");
	    
	    for (k=0;k<captions.length;k++) {
		if (captions[k].toLowerCase().contains(po.getSel().toLowerCase()))
		    break;
	    }
	    
	    if (k<links.length)
		return links[k];
	    else {
		console.output("! warning: policy states to select pdf link whose caption contains '" + po.getSel() + "' but no such link exists!\n",true);
	    }
	} else if (po.getSel().equals(opts[5])) {
	    po = PolicyManager.getPolicy(baseUrl.getHost(),"Text within pdf link title",
					 "Enter the text that has to appear within a pdf link title.\n" +
					 "The test is not case sensitive.", "$|Text: |PDF|50");
	    if (po == null)
		throw new IOException("operation canceled by user");
	    
	    for (k=0;k<titles.length;k++) {
		if (titles[k]!=null && titles[k].toLowerCase().contains(po.getSel().toLowerCase()))
		    break;
	    }
	    
	    if (k<links.length)
		return links[k];
	    else {
		console.output("! warning: policy states to select pdf link whose title contains '" + po.getSel() + "' but no such link exists!\n",true);
	    }
	} else if (po.getSel().equals(opts[6])) {
	    po = PolicyManager.getPolicy(baseUrl.getHost(),"Text within pdf link class",
					 "Enter the text that has to appear within a pdf link class.\n" +
					 "The test is not case sensitive.", "$|Text: |PDF|50");
	    if (po == null)
		throw new IOException("operation canceled by user");
	    
	    for (k=0;k<classes.length;k++) {
		if (classes[k] != null && classes[k].toLowerCase().contains(po.getSel().toLowerCase()))
		    break;
	    }
	    
	    if (k<links.length)
		return links[k];
	    else {
		console.output("! warning: policy states to select pdf link whose class contains '" + po.getSel() + "' but no such link exists!\n",true);
	    }
	}


	return null;
    }

    private void downloadFromPageFile(BibtexEntry b, String fn, String urlFn) {
	try {
	    HtmlPage page = null;
	    String[] pdfLinks = null;
	    String[] pdfCaptions = null;
	    String[] pdfTitles = null;
	    String[] pdfClasses = null;
	    String dlFile = null;
	    String logOutLink = null;
	    HtmlPageLoginForm lf = null;

	    boolean stateLoggedIn = false;
	    /*
	     * In some cases login form only appears after 'click' on pdf.
	     * Also there might be a need of multiple 'clicks' on pdf links
	     * due to confirmation dialogs (e.g. JSTOR).
	     * A maximum number of redirects is set below.
	     */
	    int stateFollowedPDFLink = 3; 

	    page = new HtmlPage(session,urlFn,console);
	    while (!console.isCanceled()) {

		pdfLinks = page.getPdfLinks();
		pdfCaptions = page.getPdfCaptions();
		pdfTitles = page.getPdfTitles();
		pdfClasses = page.getPdfClasses();
		lf = page.getLoginForm();
		if (logOutLink == null)
		    logOutLink = page.getLogOutLink();
		dlFile = null;

		if (pdfLinks.length == 0) {
		    /*
		     * no pdf link on current page -> if possible: login
		     */
		    if (lf != null && !stateLoggedIn) {
			stateLoggedIn = true;
			console.output("- No PDF link found but log in form available.  Try to log in.\n",false);
			page = new HtmlPage(session,logIn(lf),console);
			continue;
		    }
		} else if (pdfLinks.length>1) {
		    dlFile = selectPDFFile(page.getBaseURL(),pdfLinks,pdfCaptions,pdfTitles,pdfClasses);
		} else if (pdfLinks.length == 1) {
		    dlFile = pdfLinks[0];
		}
	    
		/*
		 * If we found a pdf file
		 */
		if (dlFile != null) {
		    //		    console.output("DBG: Found a PDF link: " + dlFile + "\n" ,false);
		    if (!isPDFLink(dlFile)) {
			if (lf != null && !stateLoggedIn) {
			    stateLoggedIn = true;
			    console.output("- Link is not a PDF file.  Try to log in.  " + dlFile + ".\n",false);
			    page = new HtmlPage(session,logIn(lf),console);
			    continue;
			} else if (stateFollowedPDFLink > 0) {
			    stateFollowedPDFLink--;
			    console.output("- Link is not a PDF file.  Follow link to HTML page and look for log in form.  " + dlFile + ".\n",false);
			    page = new HtmlPage(session,dlFile,console);
			    continue;
			} else {
			    console.output("! No more options for current page.\n",true);
			}		    
		    } else {
			downloadFile(b,fn,dlFile,"Published version");
		    }		
		} else {

		    console.output("! No PDF files passed test on current page.\n",true);

		}	    
		
		if (stateLoggedIn && logOutLink != null) {
		    console.output("- Performing log-out with " + logOutLink + ".\n",false);
		    page = new HtmlPage(session,logOutLink,console);
		}
		break;
	    }
	    
	} catch (MalformedURLException e) {
	    console.output("! parse url '" + e.getLocalizedMessage() + "' is malformed.\n",true);
	    e.printStackTrace();
	} catch (IOException e) {
	    console.output("! could not download file '" + urlFn + "'.\n",true);
	    console.output("! " + e.getLocalizedMessage() + "\n",true);
	    e.printStackTrace();
	}
    }
    
    public void downloadFromPage(BibtexEntry b, String url) {
	String key = FNTemplate.getFN("Journal",panel,b);
	if (key == null) {
	    console.output("! download canceled by user.\n",true);
	    return;
	}
	String fn = osFormatFn(key + ".pdf");
	
	if (hasFile(b,fn)) {
	    Object[] options = {"Overwrite old file.", "Keep old file."};
	    int n = JOptionPane.showOptionDialog(dialog,"A file named '" + fn + "' is already linked.  What shall I do?",
						 "File already linked",
						 JOptionPane.YES_NO_OPTION,
						 JOptionPane.QUESTION_MESSAGE,
						 null,options,options[1]);
	    if (n == 0) {
		downloadFromPageFile(b,fn,url);
	    }
	} else {
	    downloadFromPageFile(b,fn,url);
	}
    }
}
