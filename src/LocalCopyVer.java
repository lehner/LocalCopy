/*
 * Copyright (C) 2010 Christoph Lehner
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

public class LocalCopyVer {
    static String VERSION = "2.4";
    static String DEFPOLICIES = 
	"www.jstor.org:Text within pdf file:stable%2Fpdfplus\n" +
	"www.jstor.org:On multiple pdf files:Select+pdf+file+that+contains+a+certain+text\n" +
	"www3.interscience.wiley.com:Text within pdf file:fulltext\n" +
	"www3.interscience.wiley.com:On multiple pdf files:Select+pdf+file+that+contains+a+certain+text\n" +
	"arjournals.annualreviews.org:Index of pdf file:1\n" +
	"arjournals.annualreviews.org:On multiple pdf files:Select+n-th+pdf+file\n" +
	"www.sciencedirect.com:Text within pdf link caption:%26nbsp%3B\n" +
	"www.sciencedirect.com:On multiple pdf files:Select+pdf+link+whose+caption+contains+a+certain+text\n" +
	"www.iop.org:On multiple pdf files:Select+pdf+file+that+contains+a+certain+text\n" +
	"www.iop.org:Text within pdf file:article\n" +
	"scitation.aip.org:On multiple pdf files:Select+pdf+file+that+contains+a+certain+text\n" +
	"scitation.aip.org:Text within pdf file:getpdf\n" +
	"www.springerlink.com:On multiple pdf files:Select+pdf+file+that+contains+a+certain+text\n" +
	"www.springerlink.com:Text within pdf file:fulltext\n" +
	"www.informaworld.com:On multiple pdf files:Select+pdf+file+that+contains+a+certain+text\n" +
	"www.informaworld.com:Text within pdf file:fulltext\n" +
	"www.informaworld.com:On login form and pdf available:Don%27t+log+in+and+process+pdf+list\n" +
	"www.iop.org:On login form and pdf available:Don%27t+log+in+and+process+pdf+list\n";

    static String NEWPARAM15 = 	"www.sciencedirect.com:Text within pdf link caption:%26nbsp%3B\n" +
	"www.sciencedirect.com:On multiple pdf files:Select+pdf+link+whose+caption+contains+a+certain+text\n";

    static String NEWPARAM22 = "www.sciencedirect.com:Text within pdf link title:Download\n" +
	"www.sciencedirect.com:On multiple pdf files:Select+pdf+link+whose+title+contains+a+certain+text\n";

    static String NEWPARAM23 = "www.sciencedirect.com:Text within pdf link class:icon_pdf\n" +
	"www.sciencedirect.com:On multiple pdf files:Select+pdf+link+whose+class+contains+a+certain+text\n";

}
