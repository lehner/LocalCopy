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
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.security.*;
import java.util.TreeSet;
import java.util.ArrayList;
import net.sf.jabref.*;
import net.sf.jabref.gui.*;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.io.*;

public class FileListDialog extends JPanel
    implements ActionListener, WindowListener {

    private JButton okButton, cancelButton;
    private JDialog d;
    private JTable table;
    private FLTableModel tm;
    private Boolean canceled = false;
    public FileListItem[] fli = null;

    public FileListDialog(JDialog d, BibtexEntry[] bes) {
        super(new BorderLayout());

	this.d = d;

	setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));

        okButton = new JButton("Detach and delete");
        okButton.addActionListener(this);

        cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(this);

	tm = new FLTableModel(bes);
	table = new JTable(tm);
	table.setPreferredScrollableViewportSize(new Dimension(500, 70));
        table.setFillsViewportHeight(true);

	//	table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	table.getColumnModel().getColumn(0).setPreferredWidth(10);
	table.getColumnModel().getColumn(1).setPreferredWidth(100);
	table.getColumnModel().getColumn(2).setPreferredWidth(100);
	table.getColumnModel().getColumn(3).setPreferredWidth(300);
	table.getColumnModel().getColumn(4).setPreferredWidth(200);


	JPanel listPane = new JPanel();
	listPane.setLayout(new BoxLayout(listPane, BoxLayout.PAGE_AXIS));
	JLabel label = new JLabel("Please select the files to be detached from the BibTeX entry and to be deleted from the filesystem:");
	listPane.add(label);
	listPane.add(Box.createRigidArea(new Dimension(0,5)));
	listPane.add(new JScrollPane(table));
	listPane.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));

	JPanel buttonPane = new JPanel();
	buttonPane.setLayout(new BoxLayout(buttonPane, BoxLayout.LINE_AXIS));
	buttonPane.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));

	buttonPane.add(Box.createHorizontalGlue());
	buttonPane.add(okButton);
	buttonPane.add(Box.createRigidArea(new Dimension(10, 0)));
	buttonPane.add(cancelButton);

	add(listPane, BorderLayout.CENTER);
        add(buttonPane, BorderLayout.PAGE_END);
    }

    class FLTableModel extends AbstractTableModel {
        private String[] columnNames = {"","Key","Author", "Title", "File"};
        public Object[][] data;

	public FLTableModel(BibtexEntry[] b) {
	    int i,j,N = 0;
	    for (i=0;i<b.length;i++) {
		FileListTableModel m = new FileListTableModel();
		m.setContent(b[i].getField("file"));
		N+=m.getRowCount();
	    }

	    data = new Object[N][8];

	    N=0;
	    for (i=0;i<b.length;i++) {
		FileListTableModel m = new FileListTableModel();
		m.setContent(b[i].getField("file"));
		for (j=0;j<m.getRowCount();j++) {
		    FileListEntry f = m.getEntry(j);
		    data[N][0] = new Boolean(true);
		    data[N][1] = b[i].getField(BibtexFields.KEY_FIELD);
		    data[N][2] = b[i].getField("author");
		    data[N][3] = b[i].getField("title");
		    data[N][4] = f.getLink();
		    data[N][5] = m;
		    data[N][6] = new Integer(j);
		    data[N][7] = b[i];
		    N++;
		}
	    }
	}

        public int getColumnCount() {
            return columnNames.length;
        }

        public int getRowCount() {
            return data.length;
        }

        public String getColumnName(int col) {
            return columnNames[col];
        }

        public Object getValueAt(int row, int col) {
            return data[row][col];
        }

        public Class getColumnClass(int c) {
            return getValueAt(0, c).getClass();
        }

        public boolean isCellEditable(int row, int col) {
            if (col < 1) {
                return true;
            } else {
                return false;
            }
        }

        public void setValueAt(Object value, int row, int col) {
            data[row][col] = value;
            fireTableCellUpdated(row, col);
        }
    }


    public void actionPerformed(ActionEvent evt) {
	if (evt.getSource() == okButton) {
	    ArrayList<FileListItem> al = new ArrayList<FileListItem>();
	    for (int i = 0;i < tm.getRowCount();i++) {
		Boolean sel = (Boolean)tm.getValueAt(i,0);
		FileListItem it = new FileListItem();
		if (sel) {
		    it.m = (FileListTableModel)tm.getValueAt(i,5);
		    it.id = ((Integer)tm.getValueAt(i,6)).intValue();
		    it.fn = (String)tm.getValueAt(i,4);
		    it.b = (BibtexEntry)tm.getValueAt(i,7);
		    al.add(it);
		}
	    }
	    fli = (FileListItem[])al.toArray(new FileListItem[0]);
	    d.dispose();
	} else if (evt.getSource() == cancelButton) {
	    canceled = true;
	    d.dispose();
	}
    }

    public void windowClosing(WindowEvent e) {
	d.dispose();
	canceled = true;
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

    static FileListItem[] createAndShow(BibtexEntry[] bes) {
        JDialog d = new JDialog((Frame)null,"Delete local copies",true);
        FileListDialog p = new FileListDialog(d,bes);
	d.setResizable(true);
	d.getRootPane().setDefaultButton(p.okButton);
        p.setOpaque(true);
        d.setContentPane(p);
	d.addWindowListener(p);
	d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.pack();
	d.setLocationRelativeTo(null);
	d.setVisible(true);
	return p.fli;
    }
}
