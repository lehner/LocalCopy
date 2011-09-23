/*
 * Copyright (C) 2011 Christoph Lehner
 * Modifications by:
 * - Zhi-Wei Huang (2010): added "eprint" field in update, 
 *                         use "doi" field for mapping when "eprint" field is not available
 * - Julien Rioux (2010):  If the SPIRES search with eprint fails, try again with doi.
 *                         Deal with "arXiv:" prefix in eprint.
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

import net.sf.jabref.plugin.core.JabRefPlugin;
import net.sf.jabref.plugin.PluginCore;
import net.sf.jabref.imports.ImportInspector;
import net.sf.jabref.imports.EntryFetcher;
import net.sf.jabref.imports.SPIRESFetcher;
import net.sf.jabref.plugin.core.generated._JabRefPlugin.EntryFetcherExtension;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Arrays;
import java.util.ArrayList;

class FieldsDialog extends JPanel implements ActionListener, WindowListener {

    private JButton okButton, cancelButton;
    private String[] fields;
    private JDialog d;
    private JCheckBox[] cb;

    public String[] sel = null;

    public FieldsDialog(JDialog d, String[] fields) {
        super(new BorderLayout());

	this.d = d;
	this.fields = fields;
	cb = new JCheckBox[fields.length];

	Globals.prefs.putDefaultValue("localcopy-update-fields","");
	ArrayList<String> al = new ArrayList<String>(Arrays.asList(Globals.prefs.getStringArray("localcopy-update-fields")));

        okButton = new JButton("Ok");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

	JPanel mainPane = new JPanel();
	JPanel mainPaneCnt = new JPanel();

	mainPaneCnt.setLayout(new GridLayout((int)Math.ceil((double)fields.length/2.0),2,10,0));
	mainPaneCnt.setBackground(Color.white);
	mainPane.setBorder(BorderFactory.createLoweredBevelBorder());
	mainPane.setBackground(Color.white);
	mainPaneCnt.setBorder(BorderFactory.createEmptyBorder(5,5,5,5));

	int i;
	for (i=0;i<fields.length;i++) {
	    cb[i] = new JCheckBox();
	    cb[i].setBackground(Color.white);
	    cb[i].setText(fields[i]);
	    if (al.contains(fields[i])) {
		cb[i].setSelected(true);
	    }
	    mainPaneCnt.add(cb[i]);
	}

	mainPane.add(mainPaneCnt);
	
	JPanel buttonPane = new JPanel(new GridLayout(1,2,10,10));
	buttonPane.add(okButton);
	buttonPane.add(cancelButton);

	JPanel dlgPane = new JPanel();
	dlgPane.setLayout(new BoxLayout(dlgPane,BoxLayout.PAGE_AXIS));
	dlgPane.add(mainPane);

	JPanel sepPane = new JPanel();
	sepPane.setBorder(BorderFactory.createEmptyBorder(2,2,2,2));

	dlgPane.add(sepPane);
	dlgPane.add(buttonPane);
	
	add(dlgPane);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() == okButton) {
	    
	    int i;
	    ArrayList<String> al = new ArrayList<String>();

	    for (i=0;i<fields.length;i++) {
		if (cb[i].isSelected()) {
		    al.add(fields[i]);
		}
	    }

	    String[] ar = new String[al.size()];
	    sel = al.toArray(ar);
	    Globals.prefs.putStringArray("localcopy-update-fields",sel);
	    if (sel.length == 0)
		sel = null;
	    d.dispose();
	} else if (evt.getSource() == cancelButton) {
	    d.dispose();
	    sel = null;
	}
    }

    public void windowClosing(WindowEvent e) {
	d.dispose();
	sel = null;
    }

    public void windowClosed(WindowEvent e) {
    }

    public void windowOpened(WindowEvent e) {
    }

    public void windowIconified(WindowEvent e) {
    }

    public void windowDeiconified(WindowEvent e) {
    }

    public void windowActivated(WindowEvent e) {
    }

    public void windowDeactivated(WindowEvent e) {
    }

    static String[] createAndShow(String[] fields) {
        JDialog d = new JDialog((Frame)null,"Update fields",true);
        FieldsDialog p = new FieldsDialog(d,fields);
	d.setResizable(false);
	d.getRootPane().setDefaultButton(p.okButton);
        p.setOpaque(true);
        d.setContentPane(p);
	d.addWindowListener(p);
	d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.pack();
	d.setLocationRelativeTo(null);
	d.setVisible(true);
	return p.sel;
    }
}


public class CommandUpdate implements ProgressDialog.Command, OutputPrinter, ImportInspector {

    private Console console;
    private BibtexEntry entry;
    private enum Choice { UNDECIDED, GET, IGNORE };
    private Choice userChoiceFile, userChoiceKey;
    private ArrayList<String> sel = null;
    
    public CommandUpdate() {
	userChoiceFile = Choice.UNDECIDED;
	userChoiceKey = Choice.UNDECIDED;
    }
    
    public void setProgress(int current, int max) {
	console.setProgressBarMaximum(max);
	console.setProgressBarValue(current);
    }

    public void addEntry(BibtexEntry entry) {
	this.entry = entry;
    }

    public void toFront() {
    }

    public void setStatus(String s) {
	//console.output("- Status: " + s + "\n",false);
    }

    public void showMessage(Object message, String title, int msgType) {
	showMessage(message.toString());
    }

    public void showMessage(String string) {
	console.output("- INSPIRE/SPIRES: " + string + "\n", false);
    }

    private boolean checkUpdate(BibtexEntry n,BibtexEntry o,String field) {

	if (sel.contains(field)==false)
	    return false;

	String nf = n.getField(field);
	String of = o.getField(field);

	if (nf == null || nf.length() == 0) // new data is empty, do not use it
	    return false;

	// new data is not empty
	if (of == null || of.length() == 0) // old data is empty, use new data
	    return true;

	// old data is not empty
	if (!of.equals(nf))
	    return true;
	
	return false;
    }

    public boolean check(Component dialog, BasePanel panel, BibtexEntry[] bes) {
	String[] fields = {BibtexFields.KEY_FIELD,"eprint","doi","url","journal","year","volume","pages","title","author","slaccitation"};
	Arrays.sort(fields);
	String[] sel = FieldsDialog.createAndShow(fields);
	if (sel != null) {
	    if (JOptionPane.showOptionDialog(dialog,
					     "Updating the fields may overwrite userdefined values.  Do you really want to continue?",
					     "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,null,null,null)
		==JOptionPane.YES_OPTION) {
		this.sel = new ArrayList<String>();
		for (int i=0;i<sel.length;i++)
		    this.sel.add(sel[i]);
		return true;
	    }
	}
	return false;
    }

    private EntryFetcher getFetcherByKey(String key) {
	JabRefPlugin jabrefPlugin = JabRefPlugin.getInstance(PluginCore.getManager());
	if (jabrefPlugin != null){
	    for (EntryFetcherExtension ext : jabrefPlugin.getEntryFetcherExtensions()){
		try {
		    EntryFetcher fetcher = ext.getEntryFetcher();
		    if (fetcher != null) {
			if (fetcher.getKeyName().equals(key))
			    return fetcher;
		    }
		} catch (ClassCastException ex) {
		    ex.printStackTrace();
		}
	    }
    	}
	return null;
    }

    public void run(Component dialog, BasePanel panel, BibtexEntry bes, Console console) {
	String id = bes.getField("eprint");
	String xid = "";
	String key = bes.getField(BibtexFields.KEY_FIELD);

	this.console  = console;
	
	// try [eprint], then [doi], ...
	if (id != null && id.toLowerCase().startsWith("http://arxiv.org/abs/")) {
	    id = id.substring(21);
	}
	if (id != null && id.toLowerCase().startsWith("arxiv:")) {
	    id = id.substring(6);
	}

	// find fetcher
	EntryFetcher f;
	String prefix = "";

	f = getFetcherByKey("Fetch INSPIRE");

	if (f == null) {
	    f = new SPIRESFetcher();
	    prefix = "find ";
	    console.output("- Using SPIRES (INSPIRE plugin not found)\n",false);
	} else {
	    console.output("- Using INSPIRE\n",false);
	}

	// find entry
	entry = null;
	if (id != null) {
	    xid = "eprint \"" + id + "\"";
	    console.output("- searching for " + xid + " ...\n",false);

	    f.processQuery(prefix + xid,this,this);
	} else {
	    console.output("- entry " + key + " has no [eprint] field, trying [doi]...\n",false);
	}

	if (entry == null) {
	    id = bes.getField("doi");
	    if (id != null) {
	        xid = "doi \"" + id + "\"";
	        console.output("- searching for " + xid + " ...\n",false);
	        f.processQuery(prefix + xid,this,this);
	    } else {
	        console.output("! entry " + key + " has no [doi] field, quit.\n",true);
	    }
	}

	if (entry != null) {
	    String msg = "";
	    
	    if (checkUpdate(entry,bes,BibtexFields.KEY_FIELD))
		msg+=BibtexFields.KEY_FIELD + " ";
	    if (checkUpdate(entry,bes,"eprint"))
		msg+="eprint ";
	    if (checkUpdate(entry,bes,"doi"))
		msg+="doi ";
	    if (checkUpdate(entry,bes,"url"))
		msg+="url ";
	    if (checkUpdate(entry,bes,"journal"))
		msg+="journal ";
	    if (checkUpdate(entry,bes,"year"))
		msg+="year ";
	    if (checkUpdate(entry,bes,"volume"))
		msg+="volume ";
	    if (checkUpdate(entry,bes,"pages"))
		msg+="pages ";
	    if (checkUpdate(entry,bes,"title"))
		msg+="title ";
	    if (checkUpdate(entry,bes,"author"))
		msg+="author ";
	    if (checkUpdate(entry,bes,"slaccitation"))
		msg+="slaccitation ";
	    
	    if (msg.length() == 0) {
		//console.output("- no changes.\n",true);
	    } else {
		String[] fields = msg.trim().split(" ");
		for (int i=0;i<fields.length;i++) {
		    if (fields[i].equals(BibtexFields.KEY_FIELD)) {
			boolean change = false;
			if (userChoiceKey == Choice.UNDECIDED) {
			    Object[] options = {"Update BibTeX key for this entry.", 
						"Update BibTeX key on all future new-key events.",
						"Ignore all future new-key events."};
			    String s = (String)JOptionPane.showInputDialog(dialog,"The entry " + key + " has a new BibTeX key.  " +
									   "Should I change the key from " + key + " to " + 
									   entry.getField(BibtexFields.KEY_FIELD) + "?",
									   "New BibTeX key",
									   JOptionPane.QUESTION_MESSAGE,
									   null,options,options[0]);
			    if (s != null) {
				if (s.equals(options[0])) {
				    change = true;
				} else if (s.equals(options[1])) {
				    change = true;
				    userChoiceKey = Choice.GET;
				} else {
				    userChoiceKey = Choice.IGNORE;
				}
			    }
			} else if (userChoiceKey == Choice.GET) {
			    change = true;
			}
			
			if (change) {
			    console.output("+ " + fields[i] + ": " + bes.getField(fields[i]) + " -> " 
					   + entry.getField(fields[i]) + " .\n",false);
			    console.keepConsole("One or more updates have been made.");
			    bes.setField(fields[i],entry.getField(fields[i]));
			    key = bes.getField(fields[i]);
			    panel.markBaseChanged();
			}
			
		    } else {
			console.output("+ " + fields[i] + ": " + bes.getField(fields[i]) + " -> " 
				       + entry.getField(fields[i]) + " .\n",false);
			console.keepConsole("One or more updates have been made.");
			bes.setField(fields[i],entry.getField(fields[i]));
			panel.markBaseChanged();
			
			if (fields[i].equals("doi") || fields[i].equals("url")) {
			    boolean download = false;
			    if (userChoiceFile == Choice.UNDECIDED) {
				Object[] options = {"Download this entry.", 
						    "Select download on all future new-pdf-file events.",
						    "Ignore all future new-pdf-file events."};
				String s = (String)JOptionPane.showInputDialog(dialog,"The entry " + key + " has a new DOI or URL field.  " +
									       "Should I try to download the new journal pdf?",
									       "New DOI or URL field",
									       JOptionPane.QUESTION_MESSAGE,
									       null,options,options[0]);
				if (s != null) {
				    if (s.equals(options[0])) {
					download = true;
				    } else if (s.equals(options[1])) {
					download = true;
					userChoiceFile = Choice.GET;
				    } else {
					userChoiceFile = Choice.IGNORE;
				    }
				}
			    } else if (userChoiceFile == Choice.GET) {
				download = true;
			    }
			    
			    if (download) {
				Download d = new Download(panel, dialog, console);
				if (fields[i].equals("doi")) {
				    d.downloadFromPage(bes,"http://dx.doi.org/" + entry.getField("doi"));
				} else {
				    d.downloadFromPage(bes,entry.getField("url"));
				}
				console.setProgressBarEnabled(false);
				console.setProgressBarIndeterminate(false);
			    }
			}
		    }
		}
	    }
	} else {
	    console.output("! entry " + key + " could not be found on INSPIRE/SPIRES.\n",true);
	}
    }
}
