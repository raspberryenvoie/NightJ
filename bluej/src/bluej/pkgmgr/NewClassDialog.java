package bluej.pkgmgr;

import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.border.Border;

import bluej.*;
import bluej.Config;
import bluej.utility.*;

/**
 * Dialog for creating a new class
 *
 * @author  Justin Tan
 * @author  Michael Kolling
 * @version $Id: NewClassDialog.java 3175 2004-11-25 14:33:52Z fisker $
 */
class NewClassDialog extends EscapeDialog
{
    private JTextField textFld;
    ButtonGroup templateButtons;

    private String newClassName = "";
    private boolean ok;		// result: which button?

    public NewClassDialog(JFrame parent)
    {
        super(parent, Config.getString("pkgmgr.newClass.title"), true);

        addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent E)
                {
                    ok = false;
                    setVisible(false);
                }
            });

        JPanel mainPanel = new JPanel();
        {
            mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
            mainPanel.setBorder(BlueJTheme.dialogBorder);

            JLabel newclassTag = new JLabel(Config.getString("pkgmgr.newClass.label"));
            {
                newclassTag.setAlignmentX(LEFT_ALIGNMENT);
            }

            textFld = new JTextField(24);
            {
                textFld.setAlignmentX(LEFT_ALIGNMENT);
            }

            mainPanel.add(newclassTag);
            mainPanel.add(textFld);
            mainPanel.add(Box.createVerticalStrut(5));

            JPanel choicePanel = new JPanel();
            {
                choicePanel.setLayout(new BoxLayout(choicePanel, BoxLayout.Y_AXIS));
                choicePanel.setAlignmentX(LEFT_ALIGNMENT);

				//create compound border empty border outside of a titled border
				Border b = BorderFactory.createCompoundBorder(
							BorderFactory.createTitledBorder(Config.getString("pkgmgr.newClass.classType")),
							BorderFactory.createEmptyBorder(0, 10, 0, 10));
							
                choicePanel.setBorder(b);

                addClassTypeButtons(choicePanel);
            }

            choicePanel.setMaximumSize(new Dimension(textFld.getMaximumSize().width,
                                                     choicePanel.getMaximumSize().height));
            choicePanel.setPreferredSize(new Dimension(textFld.getPreferredSize().width,
                                                       choicePanel.getPreferredSize().height));

            mainPanel.add(choicePanel);
            mainPanel.add(Box.createVerticalStrut(BlueJTheme.dialogCommandButtonsVertical));

            JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            {
                buttonPanel.setAlignmentX(LEFT_ALIGNMENT);

                JButton okButton = BlueJTheme.getOkButton();
                {
                    okButton.addActionListener(new ActionListener()
                    {
						public void actionPerformed(ActionEvent evt)
						{
							doOK();							
						}
                    });
                }

                JButton cancelButton = BlueJTheme.getCancelButton();
                {
                    cancelButton.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent evt)
						{
							doCancel();							
						}
					});
                }

                buttonPanel.add(okButton);
                buttonPanel.add(cancelButton);

                getRootPane().setDefaultButton(okButton);
            }

            mainPanel.add(buttonPanel);
        }

        getContentPane().add(mainPanel);
        pack();

        DialogManager.centreDialog(this);
    }

    /**
     * Add the class type buttons (defining the class template to be used
     * to the panel. The templates are defined in the "defs" file.
     */
    private void addClassTypeButtons(JPanel panel)
    {
        String templateSuffix = ".tmpl";
        int suffixLength = templateSuffix.length();

        // first, get templates out of defined templates from bluej.defs
        // (we do this rather than usign the directory only to be able to
        // force an order on the templates.)

        String templateString = Config.getPropString("bluej.classTemplates");

        StringTokenizer t = new StringTokenizer(templateString);
        List templates = new ArrayList();

        while (t.hasMoreTokens())
            templates.add(t.nextToken());

        // next, get templates from files in template directory and
        // merge them in

        File templateDir = Config.getClassTemplateDir();
        if(!templateDir.exists()) {
            DialogManager.showError(this, "error-no-templates");
        }
        else {
            String[] files = templateDir.list();
            
            for(int i=0; i < files.length; i++) {
                if(files[i].endsWith(templateSuffix)) {
                    String template = files[i].substring(0, files[i].length() - suffixLength);
                    if(!templates.contains(template))
                        templates.add(template);
                }
            }
        }

        // create a radio button for each template found

        JRadioButton button;
        JRadioButton previousButton = null;
        templateButtons = new ButtonGroup();

        for(Iterator i=templates.iterator(); i.hasNext(); ) {
            String template = (String)i.next();
            
            //Avoid <enum> when we are not running 1.5
            if(template.equals("enum") && ! Config.isJava15()) {
                continue;
            }
            
            String label = Config.getString("pkgmgr.newClass." + template, template);
            button = new JRadioButton(label, (previousButton==null));  // enable first
            button.setActionCommand(template);
            templateButtons.add(button);
            panel.add(button);
//            if(previousButton != null)
//                previousButton.setNextFocusableComponent(button);
            previousButton = button;
        }
    }

    /**
     * Show this dialog and return true if "OK" was pressed, false if
     * cancelled.
     */
    public boolean display()
    {
        ok = false;
        textFld.requestFocus();
        setVisible(true);  // modal - we sit here until closed
        return ok;
    }

    public String getClassName()
    {
        return newClassName;
    }

    public String getTemplateName()
    {
        return templateButtons.getSelection().getActionCommand();
    }

    /**
     * Close action when OK is pressed.
     */
    public void doOK()
    {
        newClassName = textFld.getText().trim();

        if (JavaNames.isIdentifier(newClassName)) {
            ok = true;
            setVisible(false);
        }
        else {
            DialogManager.showError((JFrame)this.getParent(), "invalid-class-name");
            textFld.selectAll();
            textFld.requestFocus();
        }
    }

    /**
     * Close action when Cancel is pressed.
     */
    public void doCancel()
    {
        ok = false;
        setVisible(false);
    }
}
