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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.*;
import java.util.Set;
import java.util.Iterator;

import net.sf.jabref.*;
import net.sf.jabref.labelPattern.LabelPatternUtil;

public class FNTemplate {

    private static String getPref(String c) {
	String d = "${BIBTEXKEY}";
	if (c.equals("arXiv")) {
	    d += "-eprint";
	}

	if (Globals.prefs.hasKey("localcopy-fntemplate-" + c)) {
	    return new String(Base64.decode(Globals.prefs.get("localcopy-fntemplate-" + c)));
	}
	return d;
    }

    private static void setPref(String c,String v) {
	Globals.prefs.put("localcopy-fntemplate-" + c,Base64.encode(v));
    }

    private static String getKey(BasePanel panel, BibtexEntry b) {
	String k = b.getField(BibtexFields.KEY_FIELD);
	if (k == null || k.length()==0) {
	    /* Globals.prefs.getKeyPattern() */
	    LabelPatternUtil.makeLabel(new MetaData(), panel.database(), b);
	    k = b.getField(BibtexFields.KEY_FIELD);
	    b.setField(BibtexFields.KEY_FIELD,"");
	    
	    k = (String)JOptionPane.showInputDialog(panel.frame(),"Please specify the BibTeX key used for this entry.\n"+
						    "It will be used as base for the filename.",
						    "BibTeX key is not set",
						    JOptionPane.PLAIN_MESSAGE,
						    null,
						    null,
						    k);
	    
	    if (k == null)
		return null;
	    
	    b.setField(BibtexFields.KEY_FIELD,k);
	    panel.markBaseChanged();
	}
	return k;
    }

    public static void setTemplate(BasePanel panel, String c) {
	String t = getPref(c);
	t = (String)JOptionPane.showInputDialog(panel.frame(),"Please specify the filename template for " + c + "-downloads.\n"+
					"${FIELD} will be replaced with the BibTex field entry named FIELD.\n" +
					"Note that FIELD has to be in upper-case letters.  Examples:\n"+
					" ${BIBTEXKEY} = BibTex key\n"+
					" ${AUTHOR} = Author\n"+
					" ${TITLE} = Title\n"+
					" ${YEAR} = Year", "Specify filename template",
					JOptionPane.PLAIN_MESSAGE,
					null,
					null,
					t);
	if (t != null)
	    setPref(c,t);	    
    }

    public static String getFN(String c, BasePanel panel, BibtexEntry e) {
	String t = getPref(c);
	
	if (t.indexOf("${BIBTEXKEY}")!=-1) {
	    t = t.replace("${BIBTEXKEY}",getKey(panel,e));
	}

	Set<String> set = e.getAllFields();
	Iterator<String> i = set.iterator();
	while (i.hasNext()) {
	    String f = i.next();
	    if (f != null) {
		String v = e.getField(f);
		if (v != null) {
		    t = t.replace("${" + f.toUpperCase().trim() + "}",v);
		}
	    }
	}

	return t;
    }
}
