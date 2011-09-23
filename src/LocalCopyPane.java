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
import net.sf.jabref.plugin.*;

import java.util.*;
import javax.swing.*;
import javax.swing.event.*;
import java.io.*;

import java.awt.event.*;
import java.awt.*;
import java.awt.datatransfer.*;

class CommandEprint implements ProgressDialog.Command {

    public boolean check(Component dialog, BasePanel panel, BibtexEntry[] bes) {
	return true;
    }

    public void run(Component dialog, BasePanel panel, BibtexEntry bes, Console console) {
	String id = bes.getField("eprint");
	String key = bes.getField(BibtexFields.KEY_FIELD);
	if (id != null) {
	    Download d = new Download(panel, dialog, console);
	    d.downloadEprint(bes,id);
	} else {
	    console.output("! entry " + key + " has no eprint field.\n",true);
	}
    }

}

class CommandDoiUrl implements ProgressDialog.Command {

    private enum Choice { UNDECIDED, DOI, URL };
    private Choice userChoice;

    public CommandDoiUrl() {
	userChoice = Choice.UNDECIDED;
    }

    public boolean check(Component dialog, BasePanel panel, BibtexEntry[] bes) {
	return true;
    }

    public void run(Component dialog, BasePanel panel, BibtexEntry bes, Console console) {
	String doi = bes.getField("doi");
	String url = bes.getField("url");
	String key = bes.getField(BibtexFields.KEY_FIELD);

	/*
	 * if no doi is set and url field contains doi information, re-format things
	 */
	if (url != null && url.toLowerCase().startsWith("doi:")) {
	    if (doi == null)
		doi = url.substring(4);
	    url = null;
	}

	/*
	 * if doi is set, but full address is given, strip it away
	 */
	if (doi != null && doi.toLowerCase().startsWith("http://dx.doi.org/")) {
	    doi = doi.substring(18);
	}

	/*
	 * if doi is set, but doi prefix is given, strip it away
	 */
	if (doi != null && doi.toLowerCase().startsWith("doi:")) {
	    doi = doi.substring(4);
	}
	
	/*
	 * trim fields
	 */
	if (doi != null)
	    doi = doi.trim();
	if (url != null)
	    url = url.trim();
	
	Download d = new Download(panel, dialog, console);

	if (doi != null && url != null) {
	    Choice thisChoice = userChoice;
	    if (thisChoice == Choice.UNDECIDED) {
		Object[] options = {"Use DOI field", "Use URL field","Always use DOI field","Always use URL field"};
		int n = JOptionPane.showOptionDialog(dialog,"The entry " + key + " has DOI and URL fields set.",
						     "Select download source",
						     JOptionPane.YES_NO_OPTION,
						     JOptionPane.QUESTION_MESSAGE,
						     null,options,options[1]);
		if (n == 0) {
		    thisChoice = Choice.DOI;
		} else if (n == 1) {
		    thisChoice = Choice.URL;
		} else if (n == 2) {
		    thisChoice = Choice.DOI;
		    userChoice = thisChoice;
		} else if (n == 3) {
		    thisChoice = Choice.URL;
		    userChoice = thisChoice;
		}
	    }

	    if (thisChoice == Choice.DOI) {
		d.downloadFromPage(bes,"http://dx.doi.org/" + doi);
	    } else if (thisChoice == Choice.URL) {
		d.downloadFromPage(bes,url);
	    }

	} else if (doi != null) {
	    d.downloadFromPage(bes,"http://dx.doi.org/" + doi);
	} else if (url != null) {
	    d.downloadFromPage(bes,url);
	} else {
	    console.output("! entry " + key + " has no doi/url field.\n",true);
	}
    }
}


class LocalCopySidePaneComponent extends SidePaneComponent
    implements ActionListener {

    private GridBagLayout gbl = new GridBagLayout() ;
    private GridBagConstraints con = new GridBagConstraints() ;
    private JButton btnArxiv = new JButton(GUIGlobals.getImage("save"));
    private JButton btnDOIUrl = new JButton(GUIGlobals.getImage("save"));
    private JButton btnUpdate = new JButton(GUIGlobals.getImage("redo"));
    private JButton btnDelete = new JButton(GUIGlobals.getImage("delete"));

    private JButton btnSettings = new JButton(GUIGlobals.getImage("preferences"));
    private SidePaneManager manager;
    private JMenuItem menu;
    public JabRefFrame frame;

    public LocalCopySidePaneComponent(SidePaneManager manager,JabRefFrame frame,JMenuItem menu) {
	    super(manager, GUIGlobals.getIconUrl("openUrl"), "Local copy");
	    this.manager = manager;
	    this.menu = menu;
	    this.frame = frame;

	    int butSize = btnArxiv.getIcon().getIconHeight() + 5;
	    Dimension butDim = new Dimension(butSize, butSize);

	    btnArxiv.setPreferredSize(butDim);
	    btnArxiv.setMinimumSize(butDim);
	    btnArxiv.addActionListener(this);
	    btnArxiv.setText("arXiv pdf");
	    btnArxiv.setToolTipText("Download pdf from arXiv preprint server.");

	    btnDOIUrl.setPreferredSize(butDim);
	    btnDOIUrl.setMinimumSize(butDim);
	    btnDOIUrl.addActionListener(this);
	    btnDOIUrl.setText("Journal pdf");
	    btnDOIUrl.setToolTipText("Download pdf from DOI/URL link.");

	    btnUpdate.setPreferredSize(butDim);
	    btnUpdate.setMinimumSize(butDim);
	    btnUpdate.addActionListener(this);
	    btnUpdate.setText("Update fields");
	    btnUpdate.setToolTipText("Update BibTeX fields from INSPIRE/SPIRES.");

	    btnDelete.addActionListener(this);
	    btnDelete.setToolTipText("Detach the local pdf from the BibTeX entry and delete the local pdf from the filesystem.");

	    btnSettings.addActionListener(this);
	    btnSettings.setToolTipText("Settings");

	    JPanel main = new JPanel();
	    main.setLayout(gbl);
	    con.gridwidth = GridBagConstraints.REMAINDER;
	    con.fill = GridBagConstraints.BOTH;
	    con.weightx = 1;

	    gbl.setConstraints(btnArxiv,con);
	    main.add(btnArxiv);

	    gbl.setConstraints(btnDOIUrl,con);
	    main.add(btnDOIUrl);

	    JPanel split = new JPanel();
	    split.setLayout(new BoxLayout(split, BoxLayout.LINE_AXIS));
	    btnUpdate.setMaximumSize(new Dimension(Short.MAX_VALUE,Short.MAX_VALUE));
	    split.add(btnUpdate);
	    split.add(btnDelete);
	    split.add(btnSettings);
	    gbl.setConstraints(split,con);
	    main.add(split);

	    main.setBorder(BorderFactory.createEmptyBorder(1,1,1,1));
	    setContent(main);
	    setName("localcopy");
    }

    public void setActiveBasePanel(BasePanel panel) {
        super.setActiveBasePanel(panel);
	if (panel == null) {
	    boolean status = Globals.prefs.getBoolean("localcopyShow");
	    manager.hide("localcopy");
	    Globals.prefs.putBoolean("localcopyShow",status);
	    menu.setEnabled(false);
	} else {
	    if (Globals.prefs.getBoolean("localcopyShow")) {
		manager.show("localcopy");
	    }
	    menu.setEnabled(true);
	}
    }

    private void debugFields(BibtexEntry b) {
	Set keys = b.getAllFields();
	Iterator iter = keys.iterator();
	while (iter.hasNext()) {
	    String key = (String)iter.next();
	    System.err.println("Field " + key + " = " + b.getField(key));
	}
    }

    private static String getClipboardContents() {
	String result = "";
	Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
	Transferable contents = clipboard.getContents(null);
	boolean hasTransferableText = (contents != null) && contents.isDataFlavorSupported(DataFlavor.stringFlavor);
	if (hasTransferableText) {
	    try {
		result = (String)contents.getTransferData(DataFlavor.stringFlavor);
	    }
	    catch (UnsupportedFlavorException ex){
	    }
	    catch (IOException ex) {
	    }
	}
	return result;
    }

    public void actionPerformed(ActionEvent e) {
	if (e.getSource() == btnSettings) {
	    JPopupMenu popup = new JPopupMenu();
	    JMenuItem menuItem;
	    JMenu menu;

	    final Icon iconPolicy = new ImageIcon(GUIGlobals.getIconUrl("integrityCheck"));
	    final Icon iconDelete = new ImageIcon(GUIGlobals.getIconUrl("remove"));
	    final Icon iconRealm = new ImageIcon(GUIGlobals.getIconUrl("toggleGroups"));
	    final Icon iconKey = new ImageIcon(GUIGlobals.getIconUrl("makeKey"));
	    final Icon iconHelp = new ImageIcon(GUIGlobals.getIconUrl("help"));
	    final Icon iconWww = new ImageIcon(GUIGlobals.getIconUrl("www"));
	    final Icon iconInvalidate = new ImageIcon(GUIGlobals.getIconUrl("markEntries"));
	    final Icon iconRestore = new ImageIcon(GUIGlobals.getIconUrl("undo"));
	    final Icon iconImport = new ImageIcon(GUIGlobals.getIconUrl("dbImport"));
	    final Icon iconExport = new ImageIcon(GUIGlobals.getIconUrl("dbExport"));
	    final Icon iconSaveAs = new ImageIcon(GUIGlobals.getIconUrl("saveAs"));
	    
	    // Authentication menu
	    String[] hosts = IDManager.savedHosts();
	    if (hosts.length > 0) {
		menu = new JMenu("Authentication");
		menu.setIcon(iconKey);
		
		int i,j;
		for (i=0;i<hosts.length;i++) {
		    final String host = hosts[i];

		    JMenu hostMenu = new JMenu(host);
		    hostMenu.setIcon(iconWww);
		    menu.add(hostMenu);

		    String[] realms = IDManager.savedRealms(host);
		    for (j=0;j<realms.length;j++) {
			final String realm = realms[j];

			String mod = "";
			if (IDManager.isInvalidated(host,realm))
			    mod = " *";
			
			JMenu realmMenu = new JMenu(realm + mod);
			realmMenu.setIcon(iconRealm);
			hostMenu.add(realmMenu);

			if (mod.equals("")) {
			    menuItem = new JMenuItem("Invalidate (ask user for password on next use)",iconInvalidate);
			    menuItem.addActionListener(new ActionListener() {
				    public void actionPerformed(ActionEvent evt) {
					IDManager.invalidate(host,realm);
				    }
				});
			    realmMenu.add(menuItem);
			}
			
			menuItem = new JMenuItem("Delete",iconDelete);
			menuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent evt) {
				    IDManager.remove(host,realm);
				}
			    });
			realmMenu.add(menuItem);
		    }
		}
		popup.add(menu);
	    }

	    // Policy menu
	    hosts = PolicyManager.savedHosts();
	    menu = new JMenu("Policy");
	    menu.setIcon(iconPolicy);
	    popup.add(menu);

	    menuItem = new JMenuItem("Restore default values",iconRestore);
	    menuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent evt) {
			PolicyManager.importFrom(LocalCopyVer.DEFPOLICIES);
		    }
		});
	    menu.add(menuItem);

	    menuItem = new JMenuItem("Import from clipboard",iconImport);
	    menuItem.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent evt) {
			PolicyManager.importFrom(getClipboardContents());
		    }
		});
	    menu.add(menuItem);

	    
	    if (hosts.length > 0) {
		
		int i,j;

		menuItem = new JMenuItem("Export to clipboard",iconExport);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
			    StringSelection ss = new StringSelection(PolicyManager.export());
			    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(ss, null);
			}
		    });
		menu.add(menuItem);

		final Component frm = frame;
		menuItem = new JMenuItem("Delete all",iconDelete);
		menuItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
			    if (JOptionPane.showOptionDialog(frm,
							     "Do you really want to delete all policies?",
							     "Warning",JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,null,null,null)
				==JOptionPane.YES_OPTION) {
				String[] hosts = PolicyManager.savedHosts();
				for (int i=0;i<hosts.length;i++) {
				    String[] realms = PolicyManager.savedRealms(hosts[i]);
				    for (int j=0;j<realms.length;j++) {
					PolicyManager.remove(hosts[i],realms[j]);
				    }
				}
			    }
			}
		    });
		menu.add(menuItem);
		menu.addSeparator();

		for (i=0;i<hosts.length;i++) {
		    final String host = hosts[i];

		    JMenu hostMenu = new JMenu(host);
		    hostMenu.setIcon(iconWww);
		    menu.add(hostMenu);

		    hostMenu.addMenuListener(new MenuListener() {
			public void menuCanceled(MenuEvent e) {
			}
			public void menuSelected(MenuEvent e) {
			    JMenu hostMenu = (JMenu)e.getSource();
			    int j;
			    if (hostMenu.getItemCount() == 0) {
				JMenuItem menuItem = new JMenuItem("Delete all",iconDelete);
				menuItem.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent evt) {
					    String[] realms = PolicyManager.savedRealms(host);
					    for (int j=0;j<realms.length;j++) {
						PolicyManager.remove(host,realms[j]);
					    }
					}
				    });
				hostMenu.add(menuItem);
				hostMenu.addSeparator();
				
				String[] realms = PolicyManager.savedRealms(host);
				for (j=0;j<realms.length;j++) {
				    final String realm = realms[j];
				    
				    JMenu realmMenu = new JMenu(realm + ": " + PolicyManager.getPolicy(host,realm).getSel());
				    realmMenu.setIcon(iconRealm);
				    hostMenu.add(realmMenu);
				    
				    menuItem = new JMenuItem("Delete",iconDelete);
				    menuItem.addActionListener(new ActionListener() {
					    public void actionPerformed(ActionEvent evt) {
						PolicyManager.remove(host,realm);
					    }
					});
				    realmMenu.add(menuItem);
				}
				
			    }
			}
			public void menuDeselected(MenuEvent e) {
			}
		    });

		}
	    }

	    popup.addSeparator();
	    menuItem = new JMenuItem("Filename template - arXiv",iconSaveAs);
	    menuItem.addActionListener(this);
	    popup.add(menuItem);
	    menuItem = new JMenuItem("Filename template - Journal",iconSaveAs);
	    menuItem.addActionListener(this);
	    popup.add(menuItem);
	    popup.addSeparator();

	    menuItem = new JMenuItem("About LocalCopy",iconHelp);
	    menuItem.addActionListener(this);
	    popup.add(menuItem);
	    popup.show(btnSettings,(int)btnSettings.getSize().getWidth(),0);
	} else if (e.getSource().getClass().getName().equals("javax.swing.JMenuItem")) {
	    JMenuItem m = (JMenuItem)e.getSource();
	    String cmd = m.getText();
	    if (cmd.startsWith("About")) {
		JOptionPane.showMessageDialog(frame,
					      "LocalCopy is freely distributable under the terms of the\n" +
					      "GNU General Public License, version 2.  The plugin is\n" +
					      "currently maintained by Christoph Lehner.  For further\n" +
					      "information please visit http://www.lhnr.de/ext/.", 
					      "About LocalCopy (version " + LocalCopyVer.VERSION + ")",JOptionPane.INFORMATION_MESSAGE);
	    } else if (cmd.startsWith("Filename template")) {
		String what = cmd.substring(cmd.indexOf("-")+2);
		FNTemplate.setTemplate(panel,what);
	    }
	} else {
	    BasePanel panel = frame.basePanel();
	    if (panel != null) {
		BibtexEntry[] bes = panel.mainTable.getSelectedEntries();
		if ((bes != null) && (bes.length > 0)) {
		    if (e.getSource() == btnArxiv) {
			ProgressDialog.createAndShow(this,new CommandEprint(),bes);
		    } else if (e.getSource() == btnDOIUrl) {
			ProgressDialog.createAndShow(this,new CommandDoiUrl(),bes);
		    } else if (e.getSource() == btnUpdate) {
			ProgressDialog.createAndShow(this,new CommandUpdate(),bes);
		    } else if (e.getSource() == btnDelete) {
			FileListItem[] fli = FileListDialog.createAndShow(bes);
			if (fli!=null) {
			    int i;
			    for (i=fli.length-1;i>=0;i--) { // go through reverse list, so that removeEntry picks correct one also for multiple pdfs/entry

				MetaData metaData = panel.metaData();
				if (metaData!=null) {
				    String dir0 = metaData.getFileDirectory("file");
				    String dir1 = metaData.getFileDirectory("pdf");
				    File file = Util.expandFilename(fli[i].fn, new String[] { dir0, dir1, "." });
				    
				    if ((file == null) || !file.exists()) {
					frame.showMessage("File " + fli[i].fn + " not found!");
				    } else {
					try {
					    String link = file.getCanonicalPath();
					    if (!file.delete())
						frame.showMessage("File " + link + " could not be deleted from filesystem!");
					    else {
						fli[i].m.removeEntry(fli[i].id);
						fli[i].b.setField("file",fli[i].m.getStringRepresentation());
						panel.markBaseChanged();
					    }

					} catch (IOException ex) {
					    frame.showMessage("Error deleting file " + fli[i].fn + ":\n" + ex.getLocalizedMessage());
					}
				    }
				} else
				    frame.showMessage("No metaData available.");
			    }
			}
		    }
		} else {
		    frame.output("Nothing selected.");
		}
	    }
	}
    }

    public void componentOpening() {
	Globals.prefs.putBoolean("localcopyShow",true);
    }

    public void componentClosing() {
	Globals.prefs.putBoolean("localcopyShow",false);
    }
}





public class LocalCopyPane implements SidePanePlugin, ActionListener {

    protected SidePaneManager manager;
    private JMenuItem toggleMenu;
    private JabRefFrame frame;
    private LocalCopySidePaneComponent c = null;

    public void init(JabRefFrame frame, SidePaneManager manager) {
	this.manager = manager;
	this.frame = frame;

	toggleMenu = new JMenuItem("Toggle local copy panel",new ImageIcon(GUIGlobals.getIconUrl("openUrl")));
	toggleMenu.setMnemonic(KeyEvent.VK_L);
	toggleMenu.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_L, ActionEvent.ALT_MASK));

	toggleMenu.addActionListener(this);
	
	Globals.prefs.defaults.put("localcopyShow",true);

	/*
	 * Check new version
	 */
	Globals.prefs.putDefaultValue("localcopy-param-ver",0.0);
	double pv = Globals.prefs.getDouble("localcopy-param-ver");
	if (pv < 1.0) {
	    pv = 1.0;
	    PolicyManager.importFrom(LocalCopyVer.DEFPOLICIES);	 
	    System.err.println("LocalCopy: updated to version " + pv);
	} else if (pv < 1.5) {
	    pv = 1.5;
	    PolicyManager.importFrom(LocalCopyVer.NEWPARAM15);	 
	    System.err.println("LocalCopy: updated to version " + pv);
	} else if (pv < 2.2) {
	    pv = 2.2;
	    PolicyManager.importFrom(LocalCopyVer.NEWPARAM22);
	    System.err.println("LocalCopy: updated to version " + pv);
	} else if (pv < 2.3) {
	    pv = 2.3;
	    PolicyManager.importFrom(LocalCopyVer.NEWPARAM23);
	    System.err.println("LocalCopy: updated to version " + pv);
	}

	Globals.prefs.putDouble("localcopy-param-ver",pv);
    }
    
    public SidePaneComponent getSidePaneComponent() {
	c = new LocalCopySidePaneComponent(manager,frame,toggleMenu);
	return c;
    }

    public JMenuItem getMenuItem() {
	if (Globals.prefs.getBoolean("localcopyShow")) {
	    manager.show("localcopy");
	}
	if (c != null)
	    c.setActiveBasePanel(frame.basePanel());
	return toggleMenu;
    }

    public String getShortcutKey() {
	return "alt L";
    }

    public void actionPerformed(ActionEvent e) {
        if (e.getSource() == toggleMenu) {
	    manager.toggle("localcopy");
	}
    }
}
