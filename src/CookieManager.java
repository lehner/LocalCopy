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

import java.io.*;
import java.util.*;
import java.text.*;
import java.net.URL;

class Cookie {

    private final static String[] COOKIE_DATE_FORMATS = {
	"EEE',' dd-MMM-yyyy HH:mm:ss 'GMT'",
	"EEE',' dd MMM yyyy HH:mm:ss 'GMT'",
	"EEE MMM dd yyyy HH:mm:ss 'GMT'Z"
    };

    private static SimpleDateFormat[] cDateFormats = null;
    static {
	cDateFormats = new SimpleDateFormat[COOKIE_DATE_FORMATS.length];
	for (int i = 0; i < COOKIE_DATE_FORMATS.length; i++) {
	    cDateFormats[i] = new SimpleDateFormat(COOKIE_DATE_FORMATS[i], Locale.US);
	    cDateFormats[i].setTimeZone(TimeZone.getTimeZone("GMT"));
	}
    }

    class CookieMap {
	public String key,value;
	public CookieMap(String k,String v) {
	    key=k;value=v;
	}
    }

    ArrayList<CookieMap> map = new ArrayList<CookieMap>();

    public Cookie(String cookie) {
	String[] cv = cookie.split(";");
	for (int k=0;k<cv.length;k++) {
	    int equ = cv[k].indexOf("=");
	    if (equ == -1) {
		map.add(new CookieMap(cv[k],null));
	    } else {
		String name = cv[k].substring(0,equ).trim();
		String value = cv[k].substring(equ+1).trim();
		map.add(new CookieMap(name,value));
	    }
	}
    }

    public boolean appliesTo(URL url) {
	String domain = url.getHost().toString();
	String path = url.getPath().toString();
	String tDomain = getKey("domain","");
	String tPath = getKey("path","/");
	int pos = domain.length()-tDomain.length();
	if (path.length() == 0) // for some reason url.getPath() returns empty string
	    path = "/";
	if (pos<0)
	    pos=0;
	int len = tPath.length();
	if (len>path.length())
	    len=path.length();
	String cDomain = domain.substring(pos);
	String cPath = path.substring(0,len);
	//System.err.println(url.toString() + "\n" + domain + ":" + path + "|" + tDomain + ":" + tPath + "\n" +
	//		   (cDomain.equalsIgnoreCase(tDomain) && cPath.equals(tPath)));
	if (cDomain.equalsIgnoreCase(tDomain) && cPath.equals(tPath))
	    return true;
	return false;
    }

    public boolean isExpired() {
	String expires = getKey("expires",null);
	if (expires == null)
	    return false;

	for (SimpleDateFormat df : cDateFormats) {
            try {

                Date date = df.parse(expires);
		Date now = new Date();

		if (date.compareTo(now)<=0) {
		    return true;
		}

		return false;

	    } catch (Exception e) {
            }
        }

	System.err.println("Could not parse cookie date: " + getName() + ", " + expires);
        return false;

    }

    public boolean equals(Cookie c) {

	if (getName().equalsIgnoreCase(c.getName()) &&
	    getKey("domain","").equalsIgnoreCase(c.getKey("domain","")) &&
	    getKey("path","/").equals(c.getKey("path","/")))
	    return true;

	return false;
    }

    public String getKey(String key,String def) {
	CookieMap[] cm = map.toArray(new CookieMap[0]);
	for (int k=1;k<cm.length;k++) {
	    if (cm[k].key.equalsIgnoreCase(key))
		return cm[k].value;
	}	
	return def;
    }

    public void setKey(String key,String val) {
	for (int k=1;k<map.size();k++) {
	    if (map.get(k).key.equalsIgnoreCase(key)) {
		map.get(k).value = val;
		return;
	    }
	}

	map.add(new CookieMap(key,val));
    }

    public String getName() {
	CookieMap[] cm = map.toArray(new CookieMap[0]);
	if (cm.length == 0)
	    return null;
	return cm[0].key;
    }

    public String getValue() {
	CookieMap[] cm = map.toArray(new CookieMap[0]);
	if (cm.length == 0)
	    return null;
	return cm[0].value;
    }

    public String toString() {
	CookieMap[] cm = map.toArray(new CookieMap[0]);
	String ret = "";

	String name = getName();
	String value = getValue();

	if (name == null)
	    return null;

	return name + "=" + ((value != null) ? value : "");
    }

}

public class CookieManager {

    private ArrayList<Cookie> cookies;

    public CookieManager() {
	cookies = new ArrayList<Cookie>();
    }

    private void checkExpiration() {
	Iterator<Cookie> it = cookies.iterator();
	while (it.hasNext()) {
	    Cookie c = it.next();
	    if (c.isExpired()) {
		it.remove();
	    }
	}
    }

    public String getCookieString(URL url) {

	/*
	 * first test if some cookies are expired
	 */
	checkExpiration();

	/*
	 * format string
	 */
	String szCookies = "";
	Cookie[] cl = cookies.toArray(new Cookie[0]);
	for (int k=0;k<cl.length;k++) {
	    if (cl[k].appliesTo(url)) {
		if (szCookies.length() > 0)
		    szCookies += "; ";
		szCookies += cl[k].toString();
	    }
	}
	return szCookies;
    }

    public String getCookieList(URL url) {
	String szCookies = "";
	Cookie[] cl = cookies.toArray(new Cookie[0]);
	for (int k=0;k<cl.length;k++) {
	    if (cl[k].appliesTo(url)) {
		if (szCookies.length() > 0)
		    szCookies += ", ";
		szCookies += cl[k].getName();
	    }
	}
	return szCookies;
    }

    public void setCookie(URL sender, String cookie) {
	Cookie c = new Cookie(cookie);
	if (c.getKey("domain","").length()==0) {
	    c.setKey("domain",sender.getHost());
	}
	setCookie(c);
    }

    public void setCookie(Cookie c) {
	if (c.getName() == null)
	    return;

	for (int k=0;k<cookies.size();k++) {
	    if (cookies.get(k).equals(c)) {
		cookies.set(k,c);
		// System.err.println("-> cookie updated");
		return;
	    }
	}

	cookies.add(c);
    }

}
