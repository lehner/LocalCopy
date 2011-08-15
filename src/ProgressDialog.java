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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

class ProgressDialog extends JPanel
    implements ActionListener, WindowListener {

    private JButton stopButton;
    private JTextArea taskOutput;
    private JProgressBar progressBar;
    private Task task;
    private JDialog d;
    private Command command;
    private LocalCopySidePaneComponent parent;
    private BibtexEntry[] bes; 

    interface Command {
	boolean check(Component dialog, BasePanel panel, BibtexEntry[] bes);
	void run(Component dialog, BasePanel panel, BibtexEntry bes, Console io);
    };

    class Task extends Thread implements Console {
	private boolean done = false, canceled = false, errors = false;
	private String keepConsoleString = null;

	public void cancel(boolean ignore) {
	    done();
	    output("* please stand by while the current operation is closed down properly.\n", false);
	    canceled = true;
	}

	public void keepConsole(String title) {
	    keepConsoleString = title;
	}

	public void setDoneFlag() {
	    done = true;
	}

	public boolean isCanceled() {
	    return canceled;
	}

	public boolean isDone() {
	    return done;
	}

	public void output(String msg, boolean berr) {
	    if (berr) {
		errors = true;
		keepConsole("One or more errors occurred.");
	    }
	    taskOutput.insert(msg,0);
	}

	public void setProgressBarEnabled(boolean b) {
	    progressBar.setEnabled(b);
	}

	public void setProgressBarIndeterminate(boolean b) {
	    progressBar.setIndeterminate(b);
	}

	public void setProgressBarValue(int v) {
	    progressBar.setValue(v);
	}

	public void setProgressBarMaximum(int v) {
	    progressBar.setMaximum(v);
	}
	
        public void run() {

	    String msg;
	    int i;

	    keepConsoleString = null;

	    try {
		for (i=0;i<bes.length;i++) {
		    if (isCanceled()) {
			parent.frame.output("Task canceled.");
			break;
		    }
		    String key = bes[i].getField(BibtexFields.KEY_FIELD);
		    if (key == null || key.length() == 0)
			key = "unnamed entry";
		    output("Processing " + key + "...\n",false);
		    command.run((Component)d,parent.frame.basePanel(),bes[i],this);
		}
	    } catch (Exception e) {
		output("! unknown exception occured: " + e.getLocalizedMessage() + "\n",true);
		e.printStackTrace();
	    }

	    if (keepConsoleString != null) {
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
			    d.setTitle(keepConsoleString + "  Please read output.");
			    stopButton.setText("Close dialog");
			    stopButton.setEnabled(true);
			    setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
			}}); 
		setDoneFlag();
	    } else {
		d.dispose();
	    }
	}
	
        public void done() {
	    stopButton.setEnabled(false);
	    setCursor(new Cursor(Cursor.WAIT_CURSOR));
        }
    }


    public ProgressDialog(JDialog d, LocalCopySidePaneComponent parent, Command command,
			  BibtexEntry[] bes) {
        super(new BorderLayout());

	this.d = d;
	this.command = command;
	this.parent = parent;
	this.bes = bes;

        stopButton = new JButton("Cancel");
        stopButton.addActionListener(this);

	progressBar = new JProgressBar();
	progressBar.setEnabled(false);	

        taskOutput = new JTextArea(15, 60);
        taskOutput.setMargin(new Insets(5,5,5,5));
        taskOutput.setEditable(false);
	taskOutput.setBackground(Color.white);

	JPanel p = new JPanel(new BorderLayout());
	p.setBorder(BorderFactory.createEmptyBorder(5,0,5,0));
	p.add(new JScrollPane(taskOutput),BorderLayout.CENTER);

        add(stopButton, BorderLayout.PAGE_START);
        add(p, BorderLayout.CENTER);
	add(progressBar, BorderLayout.PAGE_END);
        setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

	task = new Task();
	task.start();
    }

    public void actionPerformed(ActionEvent evt) {
	if (task.isDone()) {
	    d.dispose();
	} else {
	    task.cancel(false);
	    stopButton.setEnabled(false);
	}
    }

    public void windowClosing(WindowEvent e) {
	if (task.isDone()) {
	    d.dispose();
	} else {
	    task.cancel(false);
	    stopButton.setEnabled(false);
	}
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

    static void createAndShow(LocalCopySidePaneComponent parent, Command command, BibtexEntry[] bes) {

	if (command.check(parent.frame,parent.frame.basePanel(),bes) == false) {
	    return;
	}

        JDialog d = new JDialog(parent.frame,"Progress",true);
        ProgressDialog p = new ProgressDialog(d,parent,command,bes);
        p.setOpaque(true);
        d.setContentPane(p);
	d.addWindowListener(p);
	d.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        d.pack();
	d.setLocationRelativeTo(null);
	d.setVisible(true);
    }
}


