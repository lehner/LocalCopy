/*
 * Copyright (C) 2011 Christoph Lehner
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

import java.io.*;
import java.util.*;

import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;


public class DownloadHttpSession {

    private Console console;
    private String authentication, referer;
    private static CookieManager cm = new CookieManager(); // keep cookies as long as LocalCopy runs

    public DownloadHttpSession(Console console) {
	this.console = console;
	this.authentication = "";
	this.referer = "";
	startupSSL();
    }

    private void startupSSL() {
	try {
	    System.setProperty( "java.protocol.handler.pkgs" , "javax.net.ssl" );
	    TrustManager[] trustAllCerts = new TrustManager[] {
		new X509TrustManager(){
		    public java.security.cert.X509Certificate[] getAcceptedIssuers(){
			return null;
		    }
		    public void checkClientTrusted( java.security.cert.X509Certificate[] certs, String authType ) { }
		    public void checkServerTrusted( java.security.cert.X509Certificate[] certs, String authType ) { }
		}
	    };
	    
	    SSLContext sc = SSLContext.getInstance( "SSL" );
	    sc.init( null, trustAllCerts, new java.security.SecureRandom() );
	    HttpsURLConnection.setDefaultSSLSocketFactory( sc.getSocketFactory() );

	    HostnameVerifier hv = new HostnameVerifier() {
		    public boolean verify(String urlHostName, SSLSession session) {
			return true;
		    }
		};
	    
	    HttpsURLConnection.setDefaultHostnameVerifier(hv);
	} catch (Exception e) {
	    console.output("! " + e.getMessage() + "\n",true);	    
	}
    }

    public File downloadFile(String dirFn, String urlFn) {

	File dir = new File(dirFn);
	File tmpFile;
	
	try {
	    tmpFile = File.createTempFile("article",".tmp",dir);
	} catch (Exception e) {
	    console.output("! could not create temporary file in directory '" + dirFn + 
			   "' for download of '" + urlFn + "'.\n",true);
	    console.setProgressBarEnabled(false);
	    return null;
	}

	try {
	    URL url = new URL(urlFn);
	    URLConnection con = openURL(url);

	    console.output("- Downloading " + urlFn + "...\n",false);

	    if (!con.getContentType().startsWith("application/pdf")) {

		console.output("! File is not of mime type application/pdf, but " + con.getContentType() + ".\n"
			       + "! Usually this happens if authentication with the journal failed.\n"
			       + "! Please follow the doi link with your web browser.\n",true);

	    } else {
		
		InputStream in = new BufferedInputStream(con.getInputStream());
		OutputStream out =  new BufferedOutputStream(new FileOutputStream(tmpFile));

		console.setProgressBarEnabled(true);
		if (con.getContentLength() == -1) {
		    console.setProgressBarIndeterminate(true);
		} else {
		    console.setProgressBarIndeterminate(false);
		    console.setProgressBarMaximum(con.getContentLength());
		    console.output("- File size is " + (con.getContentLength() / 1024) + " kB.\n",false);
		}

		byte[] buffer = new byte[512];
		int totalBytesRead = 0;
		while(!console.isCanceled()) {
		    int bytesRead = in.read(buffer);
		    if(bytesRead == -1) 
			break;
		    totalBytesRead += bytesRead;
		    console.setProgressBarValue(totalBytesRead);
		    out.write(buffer, 0, bytesRead);
		}
		
		out.close();
		
		if (!console.isCanceled()) {
		    console.output("- Download completed.\n",false);
		    console.setProgressBarValue(0);
		    console.setProgressBarEnabled(false);
		    return tmpFile;
		} else {
		    console.output("- Download canceled.\n",false);
		}		    
	    }
	    
	} catch (MalformedURLException e) {
	    console.output("! Download url '" + e.getLocalizedMessage() + "' is malformed.\n",true);
	    e.printStackTrace();
	} catch (SecurityException e) {
	    console.output("! Could not download file '" + urlFn + "' due to security problems.\n",true);
	    console.output("! " + e.getLocalizedMessage() + "\n",true);
	    e.printStackTrace();
	} catch (Exception e) {
	    console.output("! Could not download file '" + urlFn + "'.\n",true);
	    console.output("! " + e.getLocalizedMessage() + "\n",true);
	    e.printStackTrace();
	}

	tmpFile.delete();
	console.setProgressBarValue(0);
	console.setProgressBarEnabled(false);
	return null;
    }

    private String getHeaderHelper(HttpURLConnection con, String name) {
	int i;
	for (i=1;con.getHeaderField(i) != null;i++) {
	    if (name.equalsIgnoreCase(con.getHeaderFieldKey(i)))
		return con.getHeaderField(i);
	}
	return null;
    }

    public void setAuthentication(String auth) {
	authentication = auth;
    }

    public void setReferer(String ref) {
	referer = ref;
    }

    public HttpURLConnection openURL(URL url) throws IOException {
	return openURL(url, null);
    }

    public HttpURLConnection openURL(URL url, String postQuery) throws IOException {
	HttpURLConnection con;
	int n,r,i;

	if (postQuery == null)
	    postQuery = "";
	
	n=0;
	do {
	    con = (HttpURLConnection)url.openConnection();
	    con.setConnectTimeout(15000);
	    con.setReadTimeout(15000);
	    con.setInstanceFollowRedirects(false);
	    con.setRequestProperty("User-Agent", "Jabref");

	    if (postQuery.length() == 0) {
		console.output("- GET " + con.getURL().toString() + " with cookies {" + cm.getCookieList(url) + "}.\n",false);
	    } else {
		console.output("- POST " + con.getURL().toString() + " with cookies {" + cm.getCookieList(url) + "}.\n",false);
	    }

	    String cookies = cm.getCookieString(url);
	    if (cookies.length() > 0) {
		con.setRequestProperty("Cookie", cookies);
	    }
	    if (authentication.length() > 0) {
		con.setRequestProperty("Authorization" , authentication);
	    }
	    if (referer.length() > 0) {
		con.setRequestProperty("Referer", referer);
	    }
	    
	    if (postQuery.length() > 0) {
		con.setDoOutput(true);
		OutputStream outStream = con.getOutputStream();
		outStream.write(postQuery.getBytes());
		outStream.flush();
		outStream.close();
	    }

	    n++;

	    r = con.getResponseCode();

	    for (i=1;con.getHeaderField(i) != null;i++) {
		if (con.getHeaderFieldKey(i).equalsIgnoreCase("set-cookie")) {
		    //console.output("- cookie received: " + con.getHeaderField(i) + ".\n",false);
		    cm.setCookie(con.getURL(),con.getHeaderField(i));
		} else if (con.getHeaderFieldKey(i).equalsIgnoreCase("set-cookie2")) {
		    console.output("- set-cookie2 request not handled!\n",false);
		}
	    }

	    if (r == HttpURLConnection.HTTP_MOVED_TEMP || 
		r == HttpURLConnection.HTTP_MOVED_PERM ||
		r == HttpURLConnection.HTTP_SEE_OTHER) {
		String newURL = getHeaderHelper(con,"location");
		if (newURL == null)
		    throw new IOException("empty redirect");
		URL urlRedir = new URL(url,newURL);
		url = urlRedir;
		/*
		 * important: after redirect, don't post same values again
		 */
		postQuery = "";
	    } else if (r == HttpURLConnection.HTTP_OK) {
		break;
	    } else if (r == HttpURLConnection.HTTP_UNAUTHORIZED) {
		String realm = getHeaderHelper(con,"WWW-Authenticate");
		console.output("- authentication required: " + realm + "\n",false);

		IDItem m = IDManager.getID(con.getURL().getHost(),realm);
		if (m == null)
		    throw new IOException("authentication canceled");

		String userPassword = m.getUserID() + ":" + m.getPassword();
		String encoding = Base64.encode(userPassword.getBytes());
		String newAuth = "Basic " + encoding;
		if (newAuth.equals(authentication)) { 
		    // In case the suggested authentication is the same as just tried,
		    // invalidate login for this realm and ask id manager again.
		    IDManager.invalidate(con.getURL().getHost(),realm);
		    m = IDManager.getID(con.getURL().getHost(),realm);
		    if (m == null)
			throw new IOException("authentication canceled");

		    userPassword = m.getUserID() + ":" + m.getPassword();
		    encoding = Base64.encode(userPassword.getBytes());
		    authentication = "Basic " + encoding;
		} else {
		    authentication = newAuth;
		}		

	    } else {
		for (i=0;con.getHeaderField(i) != null;i++) {
		    console.output("! " + con.getHeaderFieldKey(i) + ": " + con.getHeaderField(i) + "\n",true);
		}
		throw new IOException("http request rejected");
	    }

	    if (console.isCanceled()) {
		throw new IOException("user canceled during resolution of doi");
	    }
	    
	} while (n<20);
	
	if (n==20) {
	    throw new IOException("redirected too often (>20 times)");
	}
	
	return con;
    }
    
    public void dumpStream(URLConnection con) {
	try {
	    InputStream input = new BufferedInputStream(con.getInputStream());
	    BufferedReader html = new BufferedReader(new InputStreamReader(input));    
	    String line;

	    System.err.println("------ STREAM DUMP ------");
	    while( (line = html.readLine()) != null ) {
		System.err.println(line);
	    }
	    System.err.println("-------------------------");

	} catch (IOException e) {
	    e.printStackTrace();
	    System.err.println(e.getMessage());
	}
    }
    
}
