package bluej;

import java.awt.*;

import javax.swing.*;
import javax.swing.border.AbstractBorder;
import javax.swing.border.Border;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.DefaultMetalTheme;

import bluej.prefmgr.PrefMgr;

/**
 * Class to provide simple UI customisations such as colours and fonts.
 * Specifically created to allow access to default Fonts for user interface
 * components for i18n purposes.
 *
 * @author  Bruce Quig
 * @version $Id: BlueJTheme.java 2277 2003-11-05 16:32:48Z polle $
 */
public class BlueJTheme extends DefaultMetalTheme
{
    private final FontUIResource controlFont = 
        new FontUIResource(PrefMgr.getStandardFont());
    private final FontUIResource systemFont = 
        new FontUIResource(controlFont);
    private final FontUIResource userFont = 
        new FontUIResource(controlFont);  
    private final FontUIResource menuFont = 
        new FontUIResource(PrefMgr.getStandardMenuFont());

	// icon to be used for BlueJ windows
	private static Image iconImage = null;

	// common strings - must be accessed through getOkLabel()
	private static String okayLabel;
	private static String cancelLabel;
	private static String continueLabel;

	// a dimension for ok and cancel buttons that is as large as
	// needed to display either
	private static Dimension okCancelDimension;
	
	// JSplitPane divider width constant
	public static final int splitPaneDividerWidth = 3;

	// Other general spacing constants. We should try to use these for consistency
	public static final int generalSpacingWidth = 5;

	public static final Border generalBorder =
	    BorderFactory.createEmptyBorder(10,10,10,10);

	public static final Border generalBorderWithStatusBar =
	    BorderFactory.createEmptyBorder(10,10,0,10);

	public static final Border dialogBorder =
	    BorderFactory.createEmptyBorder(12,12,11,11);
    
    public static final Border roundedShadowBorder = new RoundedShadowBorder();

	public static final Border shadowBorder = new ShadowBorder();
    
	public static final int commandButtonSpacing = 5;
	public static final int commandButtonPadding = 12;

	public static final int componentSpacingSmall = 5;
	public static final int componentSpacingLarge = 11;

	public static final int dialogCommandButtonsVertical = 17;
    
    /**
     * Name of theme
     */
    public String getName() 
    {  
        return "BlueJTheme"; 
    } 

    public FontUIResource getControlTextFont() 
    {  
        return controlFont;
    }
 
    public FontUIResource getSystemTextFont() 
    {  
        return systemFont;
    }
 
    public FontUIResource getUserTextFont() 
    {  
        return userFont;
    }
 
    public FontUIResource getMenuTextFont() 
    {  
        return menuFont;
    }
      
	/**
	 * Get the icon for most BlueJ frames.
	 * 
	 * @return	an icon to be used as the frame icon for most BlueJ windows
	 */
	public static Image getIconImage()
	{
		if (iconImage == null)
			iconImage = Config.getImageAsIcon("image.icon").getImage();

		return iconImage;
	}

	public static String getOkLabel()
	{
		if (okayLabel == null) {
			okayLabel = Config.getString("okay");
		}
		return okayLabel;
	}

	public static String getCancelLabel()
	{
		if (cancelLabel == null) {
			cancelLabel = Config.getString("cancel");
		}
		return cancelLabel;
	}
	
	public static String getContinueLabel()
	{
		if (continueLabel == null) {
			continueLabel = Config.getString("continue");
		}
		return continueLabel;
	}

	/**
	 * Get a standard BlueJ "ok" button.
	 * 
	 * @return	A JButton that says "ok"
	 */
	public static JButton getOkButton()
	{
		computeButtonWidths();
		
		JButton okButton = new JButton(getOkLabel());
		// try to make the OK, cancel and continue buttons have equal size
		okButton.setPreferredSize(okCancelDimension);
		return okButton;
	}
	
	/**
	 * Get a standard BlueJ "cancel" button.
	 * 
	 * @return	A JButton that says "cancel"
	 */
	public static JButton getCancelButton()
	{
		computeButtonWidths();

		JButton cancelButton = new JButton(getCancelLabel());
		// try to make the OK, cancel and continue  buttons have equal size
		cancelButton.setPreferredSize(okCancelDimension);
		return cancelButton;	
	}

	/**
	 * Get a standard BlueJ "continue" button.
	 * 
	 * @return	A JButton that says "Continue"
	 */
	public static JButton getContinueButton()
	{
		computeButtonWidths();

		JButton continueButton = new JButton(getContinueLabel());
		// try to make the OK, cancel and continue  buttons have equal size
		continueButton.setPreferredSize(okCancelDimension);
		return continueButton;	
	}

	/**
	 * Computer the maximum width of the ok, cancel and continue buttons
	 * and set the okCancelDimension to be representative of that size.
	 */
	private static void computeButtonWidths()
	{
		if (okCancelDimension != null)
			return;
		
		JButton okButton = new JButton(getOkLabel());
		JButton cancelButton = new JButton(getCancelLabel());
		JButton continueButton = new JButton(getContinueLabel());
		
		int maxWidth = Math.max(cancelButton.getPreferredSize().width,
									okButton.getPreferredSize().width);
		maxWidth = Math.max(maxWidth,
								continueButton.getPreferredSize().width);
									
		okCancelDimension = new Dimension(maxWidth, okButton.getPreferredSize().height);
	}
    
	/**
	 * A border with rounded corners and a shadow
	 * 
	 * @author Poul Henriksen
	 *
	 */
	private static class RoundedShadowBorder extends AbstractBorder{  
	    private Insets insets;
	    private ImageIcon topLeftCorner = Config.getImageAsIcon("image.border.topleft");      
	    private ImageIcon topRightCorner = Config.getImageAsIcon("image.border.topright");
	    private ImageIcon bottomLeftCorner = Config.getImageAsIcon("image.border.bottomleft");
	    private ImageIcon bottomRightCorner = Config.getImageAsIcon("image.border.bottomright");
	    private Color shadowColor = new Color(145,145,145);
	    private Color backgroundColor = Color.WHITE;
	    private Color borderColor = Color.BLACK;
	    
	    public RoundedShadowBorder() {
            insets = new Insets(0,0,0,0);
	        insets.bottom = bottomLeftCorner.getIconHeight();	         
	        insets.top = topLeftCorner.getIconHeight();
	        insets.left = topLeftCorner.getIconHeight();
	        insets.right = topRightCorner.getIconHeight();
	    }
	    
	    /** 
	     * Reinitializes the insets parameter with this Border's current Insets. 
	     * @param c the component for which this border insets value applies
	     * @param insets the object to be reinitialized
	     * @return the <code>insets</code> object
	     */
	    public Insets getBorderInsets(Component c, Insets insets) {
	        insets.bottom = this.insets.bottom;
	        insets.top = this.insets.top;
	        insets.left = this.insets.left;
	        insets.right = this.insets.right;
	        return insets;
	    }
	    
	    /**
	     * Returns a new <code>Insets</code>
	     * instance.
	     * @param c the component for which this border insets value applies
	     * @return the new <code>Insets</code> object
	     */
	    public Insets getBorderInsets(Component c) {
	        return (Insets) insets.clone();
	    }       
	    
	    /**
	     * Returns false.
	     */
	    public boolean isBorderOpaque() {
	        return false;
	    }
	    
	    /**
	     * Paints the border
	     */
	    public void paintBorder(
	            Component c,
				Graphics g,
				int x,
				int y,
				int width,
				int height) {       
	        
	        //Top
	        g.setColor(backgroundColor);
	        g.drawLine(x,y,x+width,y);
	        g.drawLine(x,y+1,x+width,y+1);
	        g.drawLine(x,y+2,x+width,y+2);
	        g.setColor(borderColor);
	        g.drawLine(x,y+3,x+width,y+3);
	        
	        //Bottom
	        g.setColor(borderColor);
	        g.drawLine(x,y+height-4,x+width,y+height-4);
	        g.setColor(shadowColor);
	        g.drawLine(x,y+height-3,x+width,y+height-3);
	        g.drawLine(x,y+height-2,x+width,y+height-2);
	        g.setColor(backgroundColor);
	        g.drawLine(x,y+height-1,x+width,y+height-1);
	        
	        //Left
	        g.setColor(backgroundColor);
	        g.drawLine(x,y,x, y+height);
	        g.drawLine(x+1,y,x+1,y+height);
	        g.drawLine(x+2,y,x+2,y+height);
	        g.setColor(borderColor);
	        g.drawLine(x+3,y,x+3,y+height);
	        
	        //Right
	        g.setColor(borderColor);
	        g.drawLine(x+width-4,y,x+width-4,y+height);
	        g.setColor(shadowColor);           
	        g.drawLine(x+width-3,y,x+width-3,y+height);
	        g.drawLine(x+width-2,y,x+width-2,y+height);
	        g.setColor(backgroundColor);           
	        g.drawLine(x+width-1,y,x+width-1,y+height);
	        
	        //Corners
	        topLeftCorner.paintIcon(c,g,x,y);
	        topRightCorner.paintIcon(c, g, x + width - insets.right, y );
	        bottomLeftCorner.paintIcon(c, g, x, y + height -insets.bottom);
	        bottomRightCorner.paintIcon(c, g, x + width - insets.right, y + height -insets.bottom);	        
	    }
	    
	}
    
    /**
     * A border with rounded corners and a shadow     * 
     * @author Poul Henriksen
     */
	private static class ShadowBorder extends AbstractBorder{  
	    private Insets insets;
	    private Color shadowColor = new Color(145,145,145);
	    private Color backgroundColor = Color.WHITE;
	    private Color borderColor = Color.BLACK;
	    
	    public ShadowBorder() {
	        insets = new Insets(4,4,4,4);				
	    }
	    
	    /** 
	     * Reinitializes the insets parameter with this Border's current Insets. 
	     * @param c the component for which this border insets value applies
	     * @param insets the object to be reinitialized
	     * @return the <code>insets</code> object
	     */
	    public Insets getBorderInsets(Component c, Insets insets) {
	        insets.bottom = this.insets.bottom;
	        insets.top = this.insets.top;
	        insets.left = this.insets.left;
	        insets.right = this.insets.right;
	        return insets;
	    }
	    
	    /**
	     * Returns a new <code>Insets</code>
	     * instance.
	     * @param c the component for which this border insets value applies
	     * @return the new <code>Insets</code> object
	     */
	    public Insets getBorderInsets(Component c) {
	        return (Insets) insets.clone();
	    }       
	    
	    /**
	     * Returns false.
	     */
	    public boolean isBorderOpaque() {
	        return false;
	    }
	    
	    /**
	     * Paints the border
	     */
	    public void paintBorder(
	            Component c,
				Graphics g,
				int x,
				int y,
				int width,
				int height) {       
	        
	        //Right
	        g.setColor(borderColor);
	        g.drawLine(x+width-4,y,x+width-4,y+height);
	        g.setColor(shadowColor);           
	        g.drawLine(x+width-3,y,x+width-3,y+height);
	        g.drawLine(x+width-2,y,x+width-2,y+height);
	        g.setColor(backgroundColor);           
	        g.drawLine(x+width-1,y,x+width-1,y+height);
	        g.fillRect(x+width-3,y,x+width,y+5);
            
	        //Bottom
	        g.setColor(borderColor);
	        g.drawLine(x+3,y+height-4,x+width-4,y+height-4);
	        g.setColor(shadowColor);
	        g.drawLine(x,y+height-3,x+width-2,y+height-3);
	        g.drawLine(x,y+height-2,x+width-2,y+height-2);
	        g.setColor(backgroundColor);
	        g.drawLine(x,y+height-1,x+width,y+height-1);
            g.fillRect(x,y+height-3,x+5,y+height);
	        
	        //Top
	        g.setColor(backgroundColor);
	        g.drawLine(x,y,x+width,y);
	        g.drawLine(x,y+1,x+width,y+1);
	        g.drawLine(x,y+2,x+width,y+2);
	        g.setColor(borderColor);
	        g.drawLine(x,y+3,x+width-4,y+3);
	        
	        //Left
	        g.setColor(backgroundColor);
	        g.drawLine(x,y,x, y+height);
	        g.drawLine(x+1,y,x+1,y+height);
	        g.drawLine(x+2,y,x+2,y+height);
	        g.setColor(borderColor);
	        g.drawLine(x+3,y+3,x+3,y+height-4);       
	        
	    }	    
	}
}
