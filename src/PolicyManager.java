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
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.*;

class MPolicy {
    public PolicyItem sel;
    public boolean remember, canceled;
}

class PolicyDialog extends JPanel
    implements ActionListener, WindowListener {

    private JButton okButton, cancelButton;
    private JComboBox cb = null;
    private JTextField tf = null;
    private JCheckBox saveP;
    private JDialog d;
    private String[] policies;

    public MPolicy pol = new MPolicy();

    public PolicyDialog(JDialog d, String host, String desc, String[] policies, String policy) {
        super(new BorderLayout());

	this.d = d;
	this.policies = policies;

	Globals.prefs.putDefaultValue("localcopy-policy-remember",true);
	pol.remember = Globals.prefs.getBoolean("localcopy-policy-remember");
	pol.canceled = false;

	JLabel labelP = new JLabel("");

        okButton = new JButton("Ok");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

	JTextArea labelInfo = new JTextArea(4,40);
	labelInfo.setEditable(false);
	labelInfo.setBackground(Color.white);
	labelInfo.setText(desc);
	
	saveP = new JCheckBox("Remember policy for " + host);
	saveP.setSelected(pol.remember);

	JPanel mainPane = new JPanel();
	mainPane.setLayout(new BoxLayout(mainPane,BoxLayout.LINE_AXIS));
	JPanel leftPane = new JPanel(new GridLayout(3,1,10,10));
	JPanel rightPane = new JPanel(new GridLayout(3,1,10,10));

	mainPane.add(leftPane);	
	mainPane.add(rightPane);	

	JPanel buttonPane = new JPanel(new GridLayout(1,2,10,10));
	buttonPane.add(okButton);
	buttonPane.add(cancelButton);

	leftPane.add(labelP);
	leftPane.add(new JLabel(""));
	leftPane.add(new JLabel(""));

	if (policies.length > 1) {

	    if (policy == null)
		policy = policies[0];

	    cb = new JComboBox();
	    int i;
	    cb.setEditable(false);
	    for (i=0;i<policies.length;i++) {
		cb.insertItemAt(policies[i],i);
		if (policy.equals(policies[i]))
		    cb.setSelectedItem(policies[i]);
	    }
	    cb.addActionListener(this);
	    labelP.setLabelFor(cb);
	    labelP.setText("Policy: ");
	    rightPane.add(cb);
	} else if (policies.length == 1) {
	    this.policies = policies[0].split("\\|");
	    policies = this.policies;
	    tf = new JTextField();
	    tf.setText((policy != null) ? policy : policies[2]);
	    labelP.setText(policies[1]);
	    labelP.setLabelFor(tf);
	    rightPane.add(tf);
	}

	rightPane.add(saveP);
	rightPane.add(buttonPane);

	JPanel pInfo = new JPanel(new BorderLayout());
	JScrollPane pInfoSP = new JScrollPane(labelInfo);
	pInfo.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
	pInfo.add(pInfoSP,BorderLayout.CENTER);
	//pInfoSP.setMaximumSize(new Dimension(400,400));
	//pInfo.setMaximumSize(new Dimension(400,400));

	JPanel dlgPane = new JPanel();
	dlgPane.setLayout(new BoxLayout(dlgPane,BoxLayout.PAGE_AXIS));
	dlgPane.add(pInfo);
	dlgPane.add(mainPane);
	
	add(dlgPane);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
    }

    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() == okButton) {
	    if (cb != null) {
		pol.sel = new PolicyItem(policies[cb.getSelectedIndex()]);
	    } else if (tf != null) {
		String s = tf.getText();
		if (policies[0].equals("$")) {
		    int maxLength = Integer.parseInt(policies[3]);
		    if (s.length() > maxLength) {
			JOptionPane.showMessageDialog(this,
						      "The entered text is too long by " + 
						      (s.length()-maxLength) + " character(s)!", 
						      "Error",JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    pol.sel = new PolicyItem(s);
		} else if (policies[0].equals("#")) {
		    int val = 0;
		    try {
			val = Integer.parseInt(s);
		    } catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(this,
						      "The entered value could not be interpreted as a number!", 
						      "Error",JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    int min = Integer.parseInt(policies[3]);
    		    int max = Integer.parseInt(policies[4]);
		    if (val < min) {
			JOptionPane.showMessageDialog(this,
						      "The entered value is too small!  Minimum: " + min, 
						      "Error",JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    if (val > max) {
			JOptionPane.showMessageDialog(this,
						      "The entered value is too large!  Maximum: " + max, 
						      "Error",JOptionPane.ERROR_MESSAGE);
			return;
		    }
		    pol.sel = new PolicyItem(val);
		}
	    }
	    pol.remember = saveP.isSelected();
	    pol.canceled = false;
	    Globals.prefs.putBoolean("localcopy-policy-remember",pol.remember);
	    d.dispose();
	} else if (evt.getSource() == cancelButton) {
	    pol.canceled = true;
	    d.dispose();
	}
    }

    public void windowClosing(WindowEvent e) {
	d.dispose();
	pol.canceled = true;
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

    static MPolicy createAndShow(String host, String realm, String desc, String[] policies, String policy) {
        JDialog d = new JDialog((Frame)null,realm,true);
        PolicyDialog p = new PolicyDialog(d,host,desc,policies,policy);
	d.setResizable(false);
	d.getRootPane().setDefaultButton(p.okButton);
        p.setOpaque(true);
        d.setContentPane(p);
	d.addWindowListener(p);
	d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.pack();
	d.setLocationRelativeTo(null);
	d.setVisible(true);
	return p.pol;
    }
}


public class PolicyManager {

    private static String getPref(String c,String n,String d) {
	if (Globals.prefs.hasKey("localcopy-policy-" + c + "-" + n)) {
	    return new String(Base64.decode(Globals.prefs.get("localcopy-policy-" + c + "-" + n)));
	}
	return d;
    }

    private static void setPref(String c,String n,String v) {
	Globals.prefs.put("localcopy-policy-" + c + "-" + n,Base64.encode(v));
    }

    private static void remPref(String c,String n) {
	if (Globals.prefs.hasKey("localcopy-policy-" + c + "-" + n)) {
	    Globals.prefs.remove("localcopy-policy-" + c + "-" + n);
	}
    }

    private static boolean hasPref(String c,String n) {
	return Globals.prefs.hasKey("localcopy-policy-" + c + "-" + n);
    }

    public static PolicyItem getPolicy(String host, String realm, String desc, String sel) {
	String[] a = { sel };
	return getPolicy(host,realm,desc,a);
    }

    public static PolicyItem getPolicy(String host, String realm, String desc, String[] sel) {
	String c = code(host,realm);

	PolicyItem it = getPolicy(host, realm);
	if (it != null)
	    return it;

	MPolicy p = PolicyDialog.createAndShow(host,realm,desc,sel,
					       getDefaultPolicy(host,realm));
	if (p.canceled)
	    return null;

	if (p.remember)
	    setPolicy(host, realm, p.sel);
	return p.sel;
    }

    public static void remove(String host, String realm) {
	String c = code(host,realm);
	remPref(c,"sel");
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

    public static String getDefaultPolicy(String thost, String trealm) {
	String[] lines = LocalCopyVer.DEFPOLICIES.split("\n");
	int i;
	try {
	    for (i=0;i<lines.length;i++) {
		String[] val = lines[i].split(":");
		if (val.length == 3) {
		    String host = val[0];
		    String realm = val[1];
		    String sel = URLDecoder.decode(val[2],"UTF-8");
		    if (host.equals(thost) && realm.equals(trealm))
			return sel;
		}
	    }
	} catch (UnsupportedEncodingException e) {
	}
	return null;
    }

    public static PolicyItem getPolicy(String host, String realm) {
	String c = code(host,realm);
	if (hasPref(c,"sel")) {
	    return new PolicyItem(getPref(c,"sel",""));
	}
	return null;
    }

    private static void setPolicy(String host, String realm, PolicyItem p) {
	String c = code(host,realm);
	setPref(c,"sel",p.getSel());
	setPref(c,"host",host);
	setPref(c,"realm",realm);
	register(c);
    }

    public static void importFrom(String imp) {
	String[] lines = imp.split("\n");
	int i;
	try {
	    for (i=0;i<lines.length;i++) {
		String[] val = lines[i].split(":");
		if (val.length == 3) {
		    String host = val[0];
		    String realm = val[1];
		    String sel = URLDecoder.decode(val[2],"UTF-8");
		    setPolicy(host,realm,new PolicyItem(sel));
		}
	    }
	} catch (UnsupportedEncodingException e) {
	}
    }

    public static String export() {
	String cp = (Globals.prefs.hasKey("localcopy-policy-list"))?
	    (Globals.prefs.get("localcopy-policy-list")):"";
	if (cp.length() == 0)
	    return "";
	String[] codes = cp.split(" ");
	String exp = "";
	int i;
	TreeSet<String> hosts = new TreeSet<String>();
	try {
	    for (i=0;i<codes.length;i++) {
		exp += getPref(codes[i],"host","") + ":" + getPref(codes[i],"realm","") + ":" + 
		    URLEncoder.encode(getPref(codes[i],"sel",""),"UTF-8") + "\n";
	    }
	} catch (UnsupportedEncodingException e) {
	    exp = "";
	}
	return exp;
    }

    public static String[] savedHosts() {
	String cp = (Globals.prefs.hasKey("localcopy-policy-list"))?
	    (Globals.prefs.get("localcopy-policy-list")):"";
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
	String cp = (Globals.prefs.hasKey("localcopy-policy-list"))?
	    (Globals.prefs.get("localcopy-policy-list")):"";
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
	String cp = (Globals.prefs.hasKey("localcopy-policy-list"))?
	    (Globals.prefs.get("localcopy-policy-list")):"";
	String[] codes = cp.split(" ");
	String newCodes = c;
	int i;
	for (i=0;i<codes.length;i++) {
	    if (codes[i].length() > 0 && !codes[i].equals(c)) {
		newCodes += " " + codes[i];
	    }
	}
	Globals.prefs.put("localcopy-policy-list",newCodes);
    }

    private static void unregister(String c) {
	String cp = (Globals.prefs.hasKey("localcopy-policy-list"))?
	    (Globals.prefs.get("localcopy-policy-list")):"";
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
	Globals.prefs.put("localcopy-policy-list",newCodes);
    }
}
