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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.security.*;
import java.util.TreeSet;

import net.sf.jabref.*;

class AuthID {
    public String user, pass;
    public boolean remember, canceled;
};

class AuthDialog extends JPanel
    implements ActionListener, WindowListener {

    private JButton okButton, cancelButton;
    private JPasswordField passField;
    private JTextField userField;
    private JCheckBox saveID;
    private JDialog d;

    public AuthID authID = new AuthID();

    public AuthDialog(JDialog d, String title, String user, String pass) {
        super(new BorderLayout());

	this.d = d;

	Globals.prefs.putDefaultValue("localcopy-auth-remember",false);
	authID.user = user;
	authID.pass = pass;
	authID.remember = Globals.prefs.getBoolean("localcopy-auth-remember");
	authID.canceled = false;
	
	passField = new JPasswordField(pass,10);
	passField.addActionListener(this);

	JLabel labelP = new JLabel("Password: ");
        labelP.setLabelFor(passField);

	userField = new JTextField(user,10);
	userField.addActionListener(this);

	JLabel labelU = new JLabel("Username: ");
        labelU.setLabelFor(userField);

        okButton = new JButton("Ok");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

	JLabel labelInfo = new JLabel(title);

	saveID = new JCheckBox("Remember username and password");
	saveID.setSelected(authID.remember);

	JPanel mainPane = new JPanel();
	mainPane.setLayout(new BoxLayout(mainPane,BoxLayout.LINE_AXIS));
	JPanel leftPane = new JPanel(new GridLayout(5,1,10,10));
	JPanel rightPane = new JPanel(new GridLayout(5,1,10,10));
	
	leftPane.add(new JLabel(GUIGlobals.getImage("integrityFail")));
	leftPane.add(labelU);
	leftPane.add(labelP);
	leftPane.add(new JLabel(""));
	leftPane.add(new JLabel(""));

	rightPane.add(labelInfo);
	rightPane.add(userField);
	rightPane.add(passField);
	rightPane.add(saveID);

	JPanel buttonPane = new JPanel(new GridLayout(1,2,10,10));
	buttonPane.add(okButton);
	buttonPane.add(cancelButton);

	rightPane.add(buttonPane);
	
	mainPane.add(leftPane);
	mainPane.add(rightPane);

	JPanel dlgPane = new JPanel();
	dlgPane.setLayout(new BoxLayout(dlgPane,BoxLayout.PAGE_AXIS));
        dlgPane.add(mainPane);
	
	add(dlgPane);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() == okButton || 
	    evt.getSource() == userField ||
	    evt.getSource() == passField) {
	    authID.user = userField.getText();
	    authID.pass = new String(passField.getPassword());
	    authID.remember = saveID.isSelected();
	    authID.canceled = false;
	    Globals.prefs.putBoolean("localcopy-auth-remember",authID.remember);
	    d.dispose();
	} else if (evt.getSource() == cancelButton) {
	    authID.canceled = true;
	    d.dispose();
	}
    }

    public void windowClosing(WindowEvent e) {
	d.dispose();
	authID.canceled = true;
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

    static AuthID createAndShow(String title, String user, String pass) {
        JDialog d = new JDialog((Frame)null,"Authentication",true);
        AuthDialog p = new AuthDialog(d,title,user,pass);
	d.setResizable(false);
	d.getRootPane().setDefaultButton(p.okButton);
        p.setOpaque(true);
        d.setContentPane(p);
	d.addWindowListener(p);
	d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.pack();
	d.setLocationRelativeTo(null);
	d.setVisible(true);
	return p.authID;
    }
}


public class IDManager {

    private static String getPref(String c,String n,String d) {
	if (Globals.prefs.hasKey("localcopy-auth-" + c + "-" + n)) {
	    return new String(Base64.decode(Globals.prefs.get("localcopy-auth-" + c + "-" + n)));
	}
	return d;
    }

    private static void setPref(String c,String n,String v) {
	Globals.prefs.put("localcopy-auth-" + c + "-" + n,Base64.encode(v));
    }

    private static void remPref(String c,String n) {
	if (Globals.prefs.hasKey("localcopy-auth-" + c + "-" + n)) {
	    Globals.prefs.remove("localcopy-auth-" + c + "-" + n);
	}
    }

    private static boolean hasPref(String c,String n) {
	return Globals.prefs.hasKey("localcopy-auth-" + c + "-" + n);
    }

    public static IDItem getID(String host, String realm) {
	String c = code(host,realm);

	IDItem it = getDefaultID(host, realm);
	if (it != null)
	    return it;

	String user = getPref(c,"user","");

	AuthID id = AuthDialog.createAndShow(host + ", " + realm,user,"");
	if (id.canceled)
	    return null;

	it = new IDItem(id.user,id.pass);
	if (id.remember) {
	    setDefaultID(host, realm, it);
	}

	return it;
    }

    public static void invalidate(String host, String realm) {
	String c = code(host,realm);
	remPref(c,"pass");
    }

    public static boolean isInvalidated(String host, String realm) {
	String c = code(host,realm);
	if (hasPref(c,"user") && !hasPref(c,"pass"))
	    return true;
	return false;
    }

    public static void remove(String host, String realm) {
	String c = code(host,realm);
	remPref(c,"user");
	remPref(c,"pass");
	remPref(c,"host");
	remPref(c,"realm");
	unregister(c);
    }

    private static String code(String host, String realm) {
	try {
	    MessageDigest md5 = MessageDigest.getInstance("MD5");
	    String str = host + ":" + realm;
	    md5.reset();
	    md5.update(str.getBytes());
	    byte[] result = md5.digest();
	    StringBuffer hexString = new StringBuffer();
	    for (int i=0; i<result.length; i++) {
		hexString.append(Integer.toHexString(0xFF & result[i]));
	    }
	    return hexString.toString();
	} catch (Exception e) {
	    System.err.println(e.getMessage());
	    e.printStackTrace();
	}
	return Base64.encode(host);
    }

    private static IDItem getDefaultID(String host, String realm) {
	String c = code(host,realm);
	if (hasPref(c,"user") && hasPref(c,"pass")) {
	    return new IDItem(getPref(c,"user",""),getPref(c,"pass",""));
	}

	return null;
    }

    private static void setDefaultID(String host, String realm, IDItem i) {
	String c = code(host,realm);
	setPref(c,"user",i.getUserID());
	setPref(c,"pass",i.getPassword());
	setPref(c,"host",host);
	setPref(c,"realm",realm);
	register(c);
    }

    public static String[] savedHosts() {
	String cp = (Globals.prefs.hasKey("localcopy-auth-list"))?
	    (Globals.prefs.get("localcopy-auth-list")):"";
	if (cp.length() == 0)
	    return new String[0];
	String[] codes = cp.split(" ");
	int i;
	TreeSet<String> hosts = new TreeSet<String>();
	for (i=0;i<codes.length;i++) {
	    hosts.add(getPref(codes[i],"host",""));
	}
	return hosts.toArray(new String[0]);
    }

    public static String[] savedRealms(String host) {
	String cp = (Globals.prefs.hasKey("localcopy-auth-list"))?
	    (Globals.prefs.get("localcopy-auth-list")):"";
	String[] codes = cp.split(" ");
	int i;
	TreeSet<String> realms = new TreeSet<String>();
	for (i=0;i<codes.length;i++) {
	    if (getPref(codes[i],"host","").equalsIgnoreCase(host)) {
		realms.add(getPref(codes[i],"realm",""));
	    }
	}
	return realms.toArray(new String[0]);
    }

    private static void register(String c) {
	String cp = (Globals.prefs.hasKey("localcopy-auth-list"))?
	    (Globals.prefs.get("localcopy-auth-list")):"";
	String[] codes = cp.split(" ");
	String newCodes = c;
	int i;
	for (i=0;i<codes.length;i++) {
	    if (codes[i].length() > 0 && !codes[i].equals(c)) {
		newCodes += " " + codes[i];
	    }
	}
	Globals.prefs.put("localcopy-auth-list",newCodes);	
    }

    private static void unregister(String c) {
	String cp = (Globals.prefs.hasKey("localcopy-auth-list"))?
	    (Globals.prefs.get("localcopy-auth-list")):"";
	String[] codes = cp.split(" ");
	String newCodes = "";
	int i;
	for (i=0;i<codes.length;i++) {
	    if (codes[i].length() > 0 && !codes[i].equals(c)) {
		if (newCodes.length() > 0)
		    newCodes += " ";
		newCodes += codes[i];
	    }
	}
	Globals.prefs.put("localcopy-auth-list",newCodes);
    }
}
