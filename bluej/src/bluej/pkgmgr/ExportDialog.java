package bluej.pkgmgr;

import bluej.Config;
import bluej.prefmgr.PrefMgr;
import bluej.utility.Debug;
import bluej.utility.Utility;
import bluej.utility.JavaNames;
import bluej.utility.DialogManager;

import java.util.List;
import java.util.Iterator;
import java.util.Collections;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

/**
 * Dialog for choosing options when exporting
 *
 * @author  Michael Kolling
 * @version $Id: ExportDialog.java 582 2000-06-23 06:55:35Z mik $
 */
class ExportDialog extends JDialog
implements ActionListener
{
    // Internationalisation
    private static final String cont = Config.getString("continue");
    private static final String cancel = Config.getString("cancel");
    private static final String dialogTitle = Config.getString("pkgmgr.export.title");
    private static final String helpLine1 = Config.getString("pkgmgr.export.helpLine1");
    private static final String helpLine2 = Config.getString("pkgmgr.export.helpLine2");
    private static final String directoryLabel = Config.getString("pkgmgr.export.directoryLabel");
    private static final String jarFileLabel = Config.getString("pkgmgr.export.jarFileLabel");
    private static final String classLabelText = Config.getString("pkgmgr.export.classLabel");
    private static final String sourceLabel = Config.getString("pkgmgr.export.sourceLabel");
    private static final String noClassText = Config.getString("pkgmgr.export.noClassText");
    private static final Color envOpColour = Config.getItemColour("colour.menu.environOp");

    private String mainClassName = null;

    private JRadioButton directoryButton;
    private JRadioButton jarButton;
    private JComboBox classSelect;
    private JCheckBox sourceBox;

    private boolean ok;		// result: which button?
    private Project project;

    public ExportDialog(PkgMgrFrame parent)
    {
        super(parent, dialogTitle, true);
        project = parent.getProject();
        makeDialog();
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        setVisible(true);
        return ok;
    }

    /**
     * Return the name of the main class in the project.
     */
    public String getMainClass()
    {
        return mainClassName;
    }

    /**
     * Return true if user wants to save in jar file, false for saving
     * in directory.
     */
    public boolean saveAsJar()
    {
        return jarButton.isSelected();
    }

    /**
     * Return true if user wants to include the source.
     */
    public boolean includeSource()
    {
        return sourceBox.isSelected();
    }

    public void actionPerformed(ActionEvent evt)
    {
        String cmd = evt.getActionCommand();
        if(cont.equals(cmd))
            doOK();
        else if(cancel.equals(cmd))
            doCancel();
    }

    /**
     * Close action when OK is pressed.
     */
    private void doOK()
    {
        mainClassName = (String)classSelect.getSelectedItem();
        if(mainClassName.equals(noClassText))
            mainClassName = null;
        ok = true;
        setVisible(false);
    }

    /**
     * Close action when Cancel is pressed.
     */
    private void doCancel()
    {
        ok = false;
        setVisible(false);
    }

    /**
     * Create the dialog interface.
     */
    private void makeDialog()
    {
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(Config.dialogBorder);

            JLabel helpText1 = new JLabel(helpLine1);
            mainPanel.add(helpText1);

            JLabel helpText2 = new JLabel(helpLine2);
            mainPanel.add(helpText2);

            Font smallFont = helpText1.getFont().deriveFont(10);
            helpText1.setFont(smallFont);
            helpText2.setFont(smallFont);

            mainPanel.add(Box.createVerticalStrut(5));

            JPanel inputPanel = new JPanel();
            {
                inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
                inputPanel.setAlignmentX(LEFT_ALIGNMENT);

                //create compound border empty border outside of a titled border
				inputPanel.setBorder(BorderFactory.createCompoundBorder(
						BorderFactory.createEtchedBorder(),
						BorderFactory.createEmptyBorder(5, 10, 5, 10)));

                directoryButton = new JRadioButton(directoryLabel, true);
                jarButton = new JRadioButton(jarFileLabel, false);

                ButtonGroup bGroup = new ButtonGroup();
                {
                    bGroup.add(directoryButton);
                    bGroup.add(jarButton);
                }
                //directoryButton.setNextFocusableComponent(jarButton);

                inputPanel.add(directoryButton);
                inputPanel.add(jarButton);
                inputPanel.add(Box.createVerticalStrut(5));

                JLabel classLabel = new JLabel(classLabelText);
                {
                    classLabel.setAlignmentX(LEFT_ALIGNMENT);
                }
                inputPanel.add(classLabel);

                classSelect = new JComboBox();
                makeClassPopup(classSelect);
                inputPanel.add(classSelect);
                inputPanel.add(Box.createVerticalStrut(5));

                sourceBox = new JCheckBox(sourceLabel, true);
                inputPanel.add(sourceBox);
            }

            mainPanel.add(inputPanel);
            mainPanel.add(Box.createVerticalStrut(Config.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton okButton = new JButton(cont);
                {
                    okButton.addActionListener(this);
                }

                JButton cancelButton = new JButton(cancel);
                {
                    cancelButton.addActionListener(this);
                }

                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);

                getRootPane().setDefaultButton(okButton);

                // try to make the OK and cancel buttons have equal width
                cancelButton.setPreferredSize(
                       new Dimension(okButton.getPreferredSize().width,
                                     cancelButton.getPreferredSize().height));
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    /**
     * Fill the class name popup selector with all classes of the project
     */
    private void makeClassPopup(JComboBox popup)
    {
    	popup.setFont(PrefMgr.getStandardMenuFont());
        popup.addItem(noClassText);

        List packageNames = project.getPackageNames();
        Collections.sort(packageNames);

        for (Iterator packages = packageNames.iterator(); packages.hasNext(); ) {
            String pkgName = (String)packages.next();
            List classNames = project.getPackage(pkgName).getAllClassnames();
            Collections.sort(classNames);
            if(pkgName.length() > 0) 
                for (Iterator classes = classNames.iterator(); classes.hasNext();)
                    popup.addItem(pkgName + "." + classes.next());
            else
                for (Iterator classes = classNames.iterator(); classes.hasNext();)
                    popup.addItem(classes.next());
        }
    }
}
