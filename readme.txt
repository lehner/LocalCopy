
localcopy - Plugin
---------------------------------------

Download a local copy of an article from the preprint server arXiv and
from journals linked by the DOI system.

Author
  * Christoph Lehner (clehner // users.sourceforge.net)

Build instructions:
  * Run build.xml with target jars (default)
  
Changelog: 

  * 2009-03-23 - V0.1 - Initial release.

  * 2009-03-24 - V0.2 - Respond to cancelation during download, show
			progress of individual file downloads, added
			backport of CookieManager, CookieStore,
			CookiePolicy and InMemoryCookieStore for
			compatibility with systems that only have JDK
			1.5.

  * 2009-03-25 - V0.3 - Implemented a lightweight mechanism for
    	       	      	handling cookies compatible with JDK 1.5,
    	       	      	fixed a bug when renaming files on Windows
    	       	      	machines.

  * 2009-03-26 - V0.4 - Added support for url field, fixed bug when
    	       	        redirection uses relative paths, support for
    	       	        pdf links that span over multiple lines,
    	       	        support for direct links to pdf files.

  * 2009-03-26 - V0.5 - Better heuristics for finding pdf links on a
    	       	      	page.  Several minor enhancements.

  * 2009-03-29 - V0.6 - Better robustness against incorrectly formed
    	       	      	HTML and DOI links, minor changes in UI.

  * 2009-04-01 - V0.7 - Bug fix for <a ...></a> tag without href,
                        added warning before an external file is
                        overwritten, added support for
                        database-specific file directories, use main
                        file directory by default.

  * 2009-04-05 - V0.8 - Implemented "update fields" button to check
                        for updates on SPIRES.  Allows for automatic
                        download of journal pdf if a new DOI/URL link
                        is found.

  * 2009-04-25 - V0.9 - Strip DOI: prefix in doi field if present.

  * 2009-06-01 - V1.0 - Implemented default choice for URL/DOI
    	       	      	selection, added HTTPS support, implemented
    	       	      	authentication system for HTTP-Auth and login
    	       	      	forms, implemented policy system, implemented
    	       	      	new cookie management system, major redesign
    	       	      	of code layout.

  * 2009-06-08 - V1.1 - Bug fix in Cookie.appliesTo, allow multiple
    	       	      	redirects of pdf links, use default policies
    	       	      	as default choice in policy dialogs if
    	       	      	available, changed package to localcopy, bug
    	       	      	fix in HtmlPage.getPDFLinks handling malformed
    	       	      	URLs (bug introduced in V1.0).

  * 2009-06-15 - V1.2 - Bug fix in Download.downloadFile.

  * 2009-07-10 - V1.3 - New feature: Select fields you want to update.

  * 2009-09-21 - V1.4 - Autogenerate BibTeX key before downloading if
    	       	      	key is empty.  Select alternative filename if
    	       	      	file existed.
			 
  * 2009-09-30 - V1.5 - Check if filename is supported by filesystem.
    	       	      	If not offer to select new filename without
    	       	      	new download.  Automatically replace ':' with
    	       	      	'_' in filename if on a windows platform.  In
    	       	      	future releases there may be a more
    	       	      	sophisticated approach.

  * 2010-01-27 - V1.6 - New policy type:  Text in caption of pdf link
    	       	      	with <a href="...">caption</a>.  Updated policy
			for sciencedirect.


  * 2010-02-17 - V1.7 - Support for single ' in links.

  * 2010-03-13 - V1.8 - New feature: Delete local copies from the
    	       	        filesystem.

  
  * 2010-03-15 - V1.9 - Contribution by Zhi-Wei Huang:
    	       	      	Update fields from SPIRES extended: use
    	       	      	"doi" when "eprint" is not available for
			mapping, also update "eprint" field.

  * 2010-04-19 - V2.0 - Bug fix in HtmlPage.java: Convert &amp; to &
    	       	      	in links.

  * 2010-08-09 - V2.1 - Implement proper timeout in http-connections.
    	       	      	User-defined filename templates implemented.

  * 2010-09-02 - V2.2 - Add new policies for link-matching: title and
    	       	      	class of link (<a href="..." class="..." title="...">).
			Implement new policy for sciencedirect.

  * 2011-08-14 - V2.3 - Add changes by Julien Rioux (Use also DOI in
    	       	      	update from SPIRES, handle arXiv: prefix in
    	       	      	eprint).  Improved Julien's code to download
    	       	      	all versions from arXiv.  Implement John
    	       	      	Kehayias's proposal to use same filename
    	       	      	restrictions on all OS to allow for easier
    	       	      	sharing of files.  Add Nicholas Jackson's
    	       	      	patch to recognize application/xhtml.

  * 2011-09-19 - V2.4 - Bug fix in DownloadHttpSession.java:
    	       	      	getHeaderFieldKey can be null
