package bluej.extensions;

import bluej.extensions.event.BJEventListener;
import bluej.extmgr.ExtensionsManager;
import bluej.extmgr.ExtensionWrapper;

import bluej.Config;
import bluej.pkgmgr.Package;
import bluej.pkgmgr.PkgMgrFrame;
import bluej.pkgmgr.Project;
import bluej.utility.DialogManager;

import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.io.File;
import java.io.InputStream;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Window;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Provides services to BlueJ extensions. This is the top-level object of the proxy hierarchy. It looks like this:<PRE>
 * BlueJ
 *   |
 *   +---- BProject
 *   |         |
 *   |         +---- BPackage
 *   |                  |
 *   |                  +---- BClass
 *   |                  |
 *   |                  +---- BObject
 *   |                           |
 *   |                           +---- BMethod
 *   |                                     |
 *   |                                     +---- BField
 *   |
 *   |
 *   +---- BMenu
 *   |       |
 *   |       +---- BMenuItem
 *   |
 *   +---- BPrefPanel</PRE>
 *
 * It is received from the extension's {@link bluej.extensions.Extension#Extension(bluej.extensions.BlueJ) constructor}. 
 * @author Clive Miller
 * @version $Id: BlueJ.java 1459 2002-10-23 12:13:12Z jckm $
 */
public class BlueJ
{
    private final ExtensionWrapper ew;
    private BMenu menu;
    private BPrefPanel prefPanel;
    private Properties localLabels;

    /**
     * Extensions should not call this constructor!
     */
    public BlueJ (ExtensionWrapper ew)
    {
        this.ew = ew;
    }

    ExtensionWrapper getEW()
    {
        return ew;
    }
    
    /**
     * Gets the current package. That is, the most recently accessed package.
     * @return the current package
     */
    public BPackage getCurrentPackage()
    {
        PkgMgrFrame pmf = PkgMgrFrame.getMostRecent();
        if (pmf == null) return null;
        Package pkg = pmf.getPackage();
        return pkg == null ? new BPackage (pmf)
                           : new BPackage (pkg);
    }
    
    /**
     * Gets all the currently open projects.
     * @return an array of the currently open project objects. This could be an
     * empty array, but will only be <code>null</code> if the extension has been
     * invalidated.
     */
    public synchronized BProject[] getOpenProjects()
    {
        if (!ew.isValid()) return null;
        Set projectSet = new TreeSet();
        PkgMgrFrame[] pmfs = PkgMgrFrame.getAllFrames();
        for (int i=0; i<pmfs.length; i++) projectSet.add (pmfs[i].getProject());
        BProject[] projects = new BProject [projectSet.size()];
        { // reduce scope of i
            int i=0;
            for (Iterator it=projectSet.iterator(); it.hasNext(); i++) {
                PkgMgrFrame pmf = (PkgMgrFrame)it.next();
                projects[i] = new BProject (pmf.getProject());
            }
        }
        return projects;
    }

    /**
     * Gets a project that contains the given package
     * @param pkg the package to look for
     * @return the project that contains this package, or <code>null</code> if none do (!)
     */
    public BProject getProject (BPackage pkg)
    {
        PkgMgrFrame pmf = PkgMgrFrame.findFrame (pkg.getRealPackage());
        if (pmf == null) return null;
        return new BProject (pmf.getProject());
    }
    
    /**
     * Returns a proxy menu object
     * @return the menu object
     */
    public BMenu getMenu()
    {
        if (menu == null) menu = new PBMenu (ew);
        return menu;
    }

    /**
     * Returns a proxy pref panel object
     * @return the proxy pref panel object
     */
    public BPrefPanel getPrefPanel()
    {
        if (prefPanel == null) prefPanel = new BPrefPanel (ew);
        return prefPanel;
    }
    
    /**
     * Returns the array of arguments with which BlueJ was started.
     * @return args
     */
    public List getArgs()
    {
        return ExtensionsManager.getExtMgr().getArgs();
    }
    
    /**
     * Shows a message box, with this text and an OK button
     * @param message the text to be displayed in the box.
     */
    public void showMessage (String message)
    {
        DialogManager.showText (PkgMgrFrame.getMostRecent(), message);
    }
    
    /**
     * Returns the path to the BlueJ system library directory 
     * <CODE>&lt;bluej&gt;/lib/</CODE>
     * @return the lib directory
     */
    public File getSystemLib()
    {
        return ExtensionsManager.getExtMgr().getBlueJLib();
    }
    
    /**
     * Returns the path to a file contained in the
     * user's bluej settings <CODE>&lt;user&gt;/bluej/<I>file</I></CODE>
     * @param subpath the name of a file or subpath and file
     * @return the path to the user file, which may not exist.
     */
    public File getUserFile (String subpath)
    {
        return Config.getUserConfigFile (subpath);
    }
    
    /**
     * Registers a package listener with the BlueJ object.
     * Opening and Closing events will be passed to the listener.
     * @param pl the listener
     */
    public void addBJEventListener (BJEventListener el)
    {
        ew.addBJEventListener (el);
    }

    /**
     * Gets the bluej default dialog border
     * @return a blank border of 5 pixels
     */
    public javax.swing.border.Border getDialogBorder()
    {
        return Config.dialogBorder;
    }
     
    /**
     * Centres a frame onto the package frame
     * @param frame the frame to be centred
     */
    public void centreWindow (java.awt.Window child)
    {
        centreWindow (child, PkgMgrFrame.getMostRecent());
    }
    
    /**
     * centreWindow - tries to center a window within a parent window
     */
    public static void centreWindow(Window child, Window parent)
    {
        child.pack();

        Point p_topleft = parent.getLocationOnScreen();
        Dimension p_size = parent.getSize();
        Dimension d_size = child.getSize();

        Dimension screen = parent.getToolkit().getScreenSize(); // Avoid window going off the screen
        int x = p_topleft.x + (p_size.width - d_size.width) / 2;
        int y = p_topleft.y + (p_size.height - d_size.height) / 2;
        if (x + d_size.width > screen.width) x = screen.width - d_size.width;
        if (y + d_size.height > screen.height) y = screen.height - d_size.height;
        if (x < 0) x = 0;
        if (y < 0) y = 0;
        child.setLocation(x,y);
    }

    /**
     * As for showLabelDialog, but you can specify the modal parent frame
     * @param title the title of the dialog box
     * @param body the contents of the dialog
     * @param parent the Frame that is to be the modal parent
     * @return the dialog
     */
    public JDialog showGeneralDialog (String title, Component body, Frame parent)
    {
        final JDialog dialog = new JDialog (parent, title, true);
        addDialogBody (dialog, body);
        return dialog;
    }
    
    /**
     * As for showLabelDialog, but you can specify the modal parent dialog
     * @param title the title of the dialog box
     * @param body the contents of the dialog
     * @param parent the Dialog that is to be the modal parent
     * @return the dialog
     */
    public JDialog showGeneralDialog (String title, Component body, Dialog parent)
    {
        final JDialog dialog = new JDialog (parent, title, true);
        addDialogBody (dialog, body);
        return dialog;
    }
    
    /**
     * Creates a skeleton dialog box plus a close button in the local language.
     * @param title the title of the dialog box
     * @param body the contents of the dialog box
     * @return the dialog so you can dispose of it if you need to.
     */
    public JDialog showGeneralDialog (String title, Component body)
    {
        return showGeneralDialog (title, body, PkgMgrFrame.getMostRecent());
    }

    private void addDialogBody (final JDialog dialog, Component body)
    {
        JPanel panel = new JPanel();
        panel.setLayout (new BoxLayout (panel, BoxLayout.Y_AXIS));
        panel.add (body);

        dialog.addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent e)
            {
                dialog.dispose();
            }
        });

        panel.add (Box.createVerticalStrut (5));
        
        JButton close = new JButton (Config.getString("close"));
        close.setAlignmentX (0.5f);
        close.addActionListener (new ActionListener() {
            public void actionPerformed (ActionEvent e) {
                dialog.dispose();
            }
        });
        panel.add (close);
        panel.setBorder(getDialogBorder());
        dialog.setContentPane(panel);

        dialog.pack();
        centreWindow (dialog);
        dialog.setVisible(true);
    }

     /**
      * Gets a property from BlueJ properties, but include a default value.
      * @param property The name of the required global property
      * @param def The default value to use if the property
      * cannot be found.
      * @return the value of that property.
      */
    public String getBJPropString (String property, String def)
    {
        return Config.getPropString ( property, def);
    }

     /**
      * Gets a property from BlueJ properties file, but include a default value.
      * You MUST use the setBJPropString to write the propertie that you want stored.
      * You can then come back and retrieve it using this function.
      * @param property The name of the required global property
      * @param def The default value to use if the property
      * cannot be found.
      * @return the value of that property.
      */
    public String getExtPropString (String property, String def)
    {
        return Config.getPropString (ExtensionsManager.getSettingsString (ew, property), def);
    }
     
     /**
      * Sets a property from BlueJ properties file.
      * The property name does not NEED to be fully qulified since a prefix will be prepended to it.
      * @param property The name of the required global property
      * @param value the required value of that property.
      */
    public void setExtPropString (String property, String value)
    {
        Config.putPropString (ExtensionsManager.getSettingsString (ew, property), value);
    }
    
    /**
     * Gets a language-independent label. First the BlueJ library is
     * searched, <CODE>&lt;bluej&gt;/lib/&lt;language&gt;/labels</CODE>,
     * then the local, extension's library (if it has one) is searched:
     * <CODE>lib/&lt;language&gt;/labels</CODE>, for example,
     * <CODE>lib/english/labels</CODE> for the English language settings.
     * If no labels are found for the current language, the default language (english) will be tried.
     * @param id the id of the label to be searched
     * @return the label appropriate to the current language, or,
     * if that fails, the name of the label will be returned.
     */
    public String getLabel (String id)
    {
        String label = Config.getString (id, null);
        if (label == null) {
            if (localLabels == null) { // Lazy initialisation
                localLabels = new Properties(); // temp - will become default
                String defaultLanguage = Config.DEFAULT_LANGUAGE;
                String localLanguage = Config.getPropString("bluej.language", defaultLanguage);

                loadLanguageFile (defaultLanguage);
                if (!defaultLanguage.equals(localLanguage)) {
                    localLabels = new Properties (localLabels); // temp becomes default
                    loadLanguageFile (localLanguage); // so now local language is searched first
                }
            }
            
            label = localLabels.getProperty (id, id);
        }
        return label;
    }
    
    /**
     * Gets a language-independent label, and replaces the first occurrance
     * of a <code>$</code> symbol with the given replacement string.
     * If there is no occurrance of <code>$</code> then it will be added
     * after a space, to the end of the resulting string
     * @param id the id of the label to be searched in the dictionaries
     * @param replacement the string to replace the <code>$</code>.
     * @return the label, suitably modified.
     * @see getLabel(String)
     */
    public String getLabelInsert (String id, String replacement)
    {
        String label = getLabel (id);
        int p = label.indexOf ('$');
        if (p == -1) {
            label += " $";
            p = label.indexOf ('$');
        }
        label = label.substring (0, p) + replacement + label.substring (p+1);
        return label;
    }

    private void loadLanguageFile (String language)
    {
        String languageFileName = "lib/" + language + "/labels";
        InputStream is = ew.getExtensionClass().getClassLoader().getResourceAsStream (languageFileName);
        try {
            localLabels.load (is);
            is.close();
        }
        catch(Exception ex) {
            // ignore as it might well not exist. Could be a nullPointerException too.
        }
    }
    
    /**
     * Open a project
     */
    public BProject openProject (File dir)
    {
        PkgMgrFrame currentFrame = PkgMgrFrame.getMostRecent();
        if (currentFrame == null) return null;
        Project openProj = Project.openProject (dir.getAbsolutePath());
        if(openProj != null) {
            Package pkg = openProj.getPackage(openProj.getInitialPackageName());
            PkgMgrFrame pmf;
            if ((pmf = currentFrame.findFrame(pkg)) == null) {
                if (currentFrame.isEmptyFrame()) {
                    pmf = currentFrame;
                    currentFrame.openPackage(pkg);
                }
                else {
                    pmf = currentFrame.createFrame(pkg);

                    DialogManager.tileWindow(pmf, currentFrame);
                }
            }

            pmf.show();
            return new BProject (openProj);
        } else {
            return null;
        }
    }
        
    /**
     * Create new project
     */
    public void createProject (File projectPath)
    {
        String pathString = projectPath.getAbsolutePath();
        if (!pathString.endsWith (File.separator)) pathString += File.separator;
        Project.createNewProject(pathString);
    }
    
    /**
     * Close BlueJ
     */
    public void closeBlueJ()
    {
        PkgMgrFrame.getMostRecent().wantToQuit();
    }
}