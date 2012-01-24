/*
 * Copyright (C) 2009 Christoph Lehner
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

class FormInputItem {
    public enum Type { UNKNOWN, HIDDEN, TEXT, PASSWORD };
    public String typeSZ, name, value;
    public Type type = Type.UNKNOWN;
    FormInputItem(String typeSZ,String name,String value) {
	this.typeSZ = typeSZ;
	this.name = name;
	this.value = value;
	if (typeSZ == null) {
	    type = Type.TEXT;
	} else {
	    if (typeSZ.equalsIgnoreCase("text"))
		type = Type.TEXT;
	    else if (typeSZ.equalsIgnoreCase("password"))
		type = Type.PASSWORD;
	    else if (typeSZ.equalsIgnoreCase("hidden"))
		type = Type.HIDDEN;
	}
    }
}

class FrameStatus {
    public ArrayList<String> checkedPages;
    public Stack<String> pagesToCheck;
	
    FrameStatus() {
	checkedPages = new ArrayList<String>();
	pagesToCheck = new Stack<String>();
    }

    private boolean hasChecked(String page) {
	if (checkedPages.contains(page))
	    return true;
	return false;
    }
    
    public void checkPage(String page) {
	if (!hasChecked(page))
	    pagesToCheck.push(page);
    }
    
    public String nextPageToCheck() {
	if (pagesToCheck.empty())
	    return null;
	return pagesToCheck.pop();
    }
}

public class HtmlPage {

    private ArrayList<String> pdfLinks = new ArrayList<String>();
    private ArrayList<String> pdfCaptions = new ArrayList<String>();
    private ArrayList<String> allLinks = new ArrayList<String>();
    private ArrayList<String> pdfTitles = new ArrayList<String>();
    private ArrayList<String> pdfClasses = new ArrayList<String>();

    private HtmlPageLoginForm lf = null;
    private URL baseURL = null;
    private String logOutLink = null;
    private String title = null;
    private boolean hasCaptcha = false;
    
    public HtmlPage(DownloadHttpSession session, HttpURLConnection con, Console console) {

	String urlFn;
	FrameStatus state = new FrameStatus();
	baseURL = con.getURL();
	try {
	    processConnection(session,con,console,state);
	} catch (IOException e) {
	    console.output("! Error processConnection: " + e.getMessage() + "\n",true);
	}

	while ((urlFn = state.nextPageToCheck()) != null && !console.isCanceled()) {
	    try {
		con = session.openURL(new URL(urlFn));
		processConnection(session,con,console,state);
	    } catch (IOException e) {
		console.output("! Error accessing " + urlFn + ": " + e.getMessage() + "\n",true);
	    }
	}

    }

    public HtmlPage(DownloadHttpSession session, String pageURL, Console console) {
	
	FrameStatus state = new FrameStatus();
	HttpURLConnection con;
	String urlFn;

	state.checkPage(pageURL);
	while ((urlFn = state.nextPageToCheck()) != null && !console.isCanceled()) {
	    try {
		con = session.openURL(new URL(urlFn));
		if (baseURL == null)
		    baseURL = con.getURL();
		processConnection(session,con,console,state);
	    } catch (IOException e) {
		console.output("! Error accessing " + urlFn + ": " + e.getMessage() + "\n",true);
	    }
	}

    }

    public URL getBaseURL() {
	return baseURL;
    }

    public String getLogOutLink() {
	return logOutLink;
    }

    public String getTitle() {
	return title;
    }

    public boolean hasCaptcha() {
	return hasCaptcha;
    }

    private static int nr = 0;

    private void processConnection(DownloadHttpSession session, HttpURLConnection con, Console console,
				   FrameStatus state) throws IOException, MalformedURLException {
	
	InputStream input = new BufferedInputStream(con.getInputStream());
	BufferedReader html = new BufferedReader(new InputStreamReader(input));    
	String fullHTML = "", line, urlFn;
	URL url = con.getURL();
	urlFn = url.toString();

	console.output("- Process " + urlFn + " ...\n",false);

	/*
	 * if this link is a pdf file, add a pdf link
	 */
	if (con.getContentType().startsWith("application/pdf")) {
	    addPdfLink(urlFn,"<n/a>","<n/a>","<n/a>");
	    return;
	} else if (con.getContentType().startsWith("text/html") ||
	    con.getContentType().startsWith("application/xhtml")) {
	    // proceed with html parsing
	} else {
	    console.output("- Unknown content type: " + con.getContentType() + "\n",false);
	    return;
	}
	
	/*
	 * We have a text/html page, parse links and forms
	 */
	while( (line = html.readLine()) != null ) {
	    fullHTML += " " + line;
	    
	    if (console.isCanceled()) {
		console.output("- Download canceled by user.\n",false);
		return;
	    }
	}

	parseLinks(url,fullHTML);
	if (lf == null)
	    lf = findLoginForm(url, fullHTML);
	else if (findLoginForm(url, fullHTML) != null) {
	    console.output("- Warning: multiple login forms detected.\n",false);
	}

	/*
	 * Gather further information about site
	 */
	title = findTitle(fullHTML);
	console.output("- Title: " + title + "\n",false);
	if (!hasCaptcha)
	    hasCaptcha = siteHasCaptcha(fullHTML);
	
	ArrayList<String> frames = parseFrames(url, fullHTML);
	Iterator<String> it = frames.iterator();
	while (it.hasNext()) {
	    urlFn = it.next();
	    state.checkPage(urlFn);
	    console.output("- Frame " + urlFn + " detected.\n",false);
	}
    }

    public String[] getPdfLinks() {
	return pdfLinks.toArray(new String[0]);
    }

    public String[] getPdfCaptions() {
	return pdfCaptions.toArray(new String[0]);
    }

    public String[] getPdfTitles() {
	return pdfTitles.toArray(new String[0]);
    }

    public String[] getPdfClasses() {
	return pdfClasses.toArray(new String[0]);
    }

    public String[] getLinks() {
	return allLinks.toArray(new String[0]);
    }

    public HtmlPageLoginForm getLoginForm() {
	return lf;
    }

    private String formatHref(String a) {
	return a.replaceAll("&amp;","&");
    }

    private void addPdfLink(String href,String caption,String a_title,String a_class) {
	href = formatHref(href);
	if (!pdfLinks.contains(href)) {
	    pdfLinks.add(href);
	    pdfCaptions.add(caption);
	    pdfTitles.add(a_title);
	    pdfClasses.add(a_class);
	}
    }

    private void addLink(String href) {
	href = formatHref(href);
	if (!allLinks.contains(href))
	    allLinks.add(href);
    }

    private boolean looksLikePDF(String href, String content) {
	Pattern pd = Pattern.compile("pdf",Pattern.CASE_INSENSITIVE);
	Matcher md = pd.matcher(href);
	if (md.find())
	    return true;
	md = pd.matcher(content);
	if (md.find())
	    return true;
	return false;
    }

    private boolean looksLikeLogout(String href, String content) {
	Pattern pd = Pattern.compile("logout",Pattern.CASE_INSENSITIVE);
	Matcher md = pd.matcher(href);
	if (md.find())
	    return true;
	md = pd.matcher(content);
	if (md.find())
	    return true;
	return false;
    }

    private String decodeHTML(String h) {
	return h;
    }

    private String parseProp(String ln, String prop) {

	Matcher m;

	m = Pattern.compile(prop + "[ ]*=[ ]*\"([^\"]*)\"",Pattern.CASE_INSENSITIVE).matcher(ln);
	if (m.find()) {
	    return m.group(1);
	}

	m = Pattern.compile(prop + "[ ]*=[ ]*'([^']*)'",Pattern.CASE_INSENSITIVE).matcher(ln);
	if (m.find()) {
	    return m.group(1);
	}

	m = Pattern.compile(prop + "[ ]*=[ ]*([^ >]*)",Pattern.CASE_INSENSITIVE).matcher(ln);
	if (m.find()) {
	    return m.group(1);
	}

	return null;

    }

    private void parseLinks(URL baseURL, String fullHTML) throws MalformedURLException {
	Pattern p = Pattern.compile("<a ([^>]*?)>(.*?)</a>",Pattern.CASE_INSENSITIVE);
	Matcher m = p.matcher(fullHTML);

	/*try {
	    OutputStream out =  new BufferedOutputStream(new FileOutputStream(new File("test.html")));
	    out.write(fullHTML.getBytes());
	    out.close();
	} catch (Exception e) {
	}*/

	while (m.find()) {
	    String href = decodeHTML(parseProp(m.group(1),"href"));
	    String a_title = decodeHTML(parseProp(m.group(1),"title"));
	    String a_class = decodeHTML(parseProp(m.group(1),"class"));
	    String content = m.group(2);
	    
	    if (href != null) {
		try {
		    URL newHref = new URL(baseURL,href);
		    String nh = newHref.toString();
		    
		    addLink(nh);
		    if (looksLikePDF(href,content)) {
			addPdfLink(nh,content,a_title,a_class);
		    } else if (looksLikeLogout(href,content)) {
			if (logOutLink != null && !logOutLink.equals(nh)) {
			    System.err.println("Multiple logout links detected: " + logOutLink + ", " + nh);
			}
			logOutLink = nh;
		    }
		} catch (MalformedURLException e) {
		    // a malformed link encountered
		}
	    }
	}
    }

    private ArrayList<String> parseFrames(URL baseURL, String fullHTML) throws MalformedURLException {
	ArrayList<String> frames = new ArrayList<String>();
	Pattern p = Pattern.compile("<frame ([^>]*?)>",Pattern.CASE_INSENSITIVE);
	Matcher m = p.matcher(fullHTML);
	while (m.find()) {
	    String src = decodeHTML(parseProp(m.group(1),"src"));
	    if (src != null) {
		URL newHref = new URL(baseURL,src);
		src = newHref.toString();
		if (!frames.contains(src))
		    frames.add(src);
	    }
	}
	return frames;
    }

    // the following is a very naive test for a captcha, improve in the future!
    private boolean siteHasCaptcha(String fullHTML) { 
	Pattern pf = Pattern.compile("captcha",Pattern.CASE_INSENSITIVE);
	Matcher mf = pf.matcher(fullHTML);
	return mf.find();
    }

    private String findTitle(String fullHTML) {
	Pattern pf = Pattern.compile("<title>(.*?)</title>",Pattern.CASE_INSENSITIVE);
	Matcher mf = pf.matcher(fullHTML);
	if (mf.find()) {
	    return mf.group(1).trim();
	}
	return null;
    }

    private HtmlPageLoginForm findLoginForm(URL baseUrl, String fullHTML) {
	Pattern pf = Pattern.compile("<form ([^>]*?)>(.*?)</form>",Pattern.CASE_INSENSITIVE);
	Matcher mf = pf.matcher(fullHTML);
	String method = "",action = "",query = "",userFieldName = "",passwordFieldName = "";
	boolean hasUserField = false, hasPasswordField = false;
		
	while (mf.find()) {
	    method = parseProp(mf.group(1),"method");
	    action = parseProp(mf.group(1),"action");
	    query = "";

	    if (action != null) {
		hasUserField = false;
		hasPasswordField = false;
		userFieldName = "";
		passwordFieldName = "";
			
		Pattern pi = Pattern.compile("<input ([^>]*?)>",Pattern.CASE_INSENSITIVE);
		Matcher mi = pi.matcher(mf.group(2));
		ArrayList<FormInputItem> in = new ArrayList<FormInputItem>();
		while (mi.find()) {
		    String type = parseProp(mi.group(1),"type");	
		    String name = parseProp(mi.group(1),"name");
		    String value = parseProp(mi.group(1),"value");

		    if (name != null) {
			in.add(new FormInputItem(type,name,value));
		    }
		}

		/*
		 * test if form has user / password fields
		 */
		FormInputItem[] fields = (FormInputItem[])in.toArray(new FormInputItem[0]);
		int i;
		for (i=0;i<fields.length;i++) {
		    if (fields[i].type == FormInputItem.Type.TEXT) {
			if (fields[i].name.toLowerCase().indexOf("user") != -1)
			    break;
		    }
		}
		if (i == fields.length) {
		    for (i=0;i<fields.length;i++) {
			if (fields[i].type == FormInputItem.Type.TEXT) {
			    if (fields[i].name.toLowerCase().indexOf("id") != -1)
				break;
			}
		    }
		}
		if (i < fields.length) {
		    hasUserField = true;
		    userFieldName = fields[i].name;
		}

		for (i=0;i<fields.length;i++) {
		    if (fields[i].type == FormInputItem.Type.PASSWORD) {
			hasPasswordField = true;
			passwordFieldName = fields[i].name;
			break;
		    }
		}

		/*
		 * compose query
		 */
		for (i=0;i<fields.length;i++) {
		    if (fields[i].value != null &&
			!fields[i].name.equalsIgnoreCase(userFieldName) &&
			!fields[i].name.equalsIgnoreCase(passwordFieldName)) {
		    	
			if (query.length() > 0)
			    query += "&";
			
			try {
			    query += URLEncoder.encode(fields[i].name,"UTF-8") + "=" + URLEncoder.encode(fields[i].value,"UTF-8");
			} catch (UnsupportedEncodingException e) {
			    // utf-8 should always be supported
			}
		    }
		}
		    
		if (hasUserField && hasPasswordField)
		    return new HtmlPageLoginForm(baseUrl,method,action,query,userFieldName,passwordFieldName);
	    }
	}

	return null;
    }
}
