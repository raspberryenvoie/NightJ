package bluej.pkgmgr.graphPainter;

import java.awt.*;
import java.awt.Graphics2D;

import bluej.Config;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.dependency.ImplementsDependency;
import bluej.pkgmgr.target.DependentTarget;

/**
 * Paintes ImplementsDependencies
 * @author fisker
 * @version $Id: ImplementsDependencyPainter.java 2587 2004-06-10 14:11:21Z polle $
 */
public class ImplementsDependencyPainter implements DependencyPainter
{
    protected static final float strokeWithDefault = 1.0f;
    protected static final float strokeWithSelected = 2.0f;
    
    static final Color normalColour = Config.getItemColour("colour.arrow.implements");
    //static final Color bgGraph = Config.getItemColour("colour.graph.background");
    static final int ARROW_SIZE = 18;		// pixels
    static final double ARROW_ANGLE = Math.PI / 6;	// radians
    //static final int SELECT_DIST = 4;
    private static final float  dash1[] = {5.0f,2.0f};
    private static final BasicStroke dashedUnselected = new BasicStroke(strokeWithDefault,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, dash1, 0.0f);
    private static final BasicStroke dashedSelected = new BasicStroke(strokeWithSelected,
            BasicStroke.CAP_BUTT,
            BasicStroke.JOIN_MITER,
            10.0f, dash1, 0.0f);
    private static final BasicStroke normalSelected = new BasicStroke(strokeWithSelected);
    private static final BasicStroke normalUnselected = new BasicStroke(strokeWithDefault);
    
    
    public void paint(Graphics2D g, Dependency dependency) {
        if (!(dependency instanceof ImplementsDependency)){
            throw new IllegalArgumentException();
        }
        Stroke oldStroke = g.getStroke();
        
        ImplementsDependency d = (ImplementsDependency) dependency;
        Stroke dashedStroke, normalStroke;
        if (d.isSelected()) 
        {
            dashedStroke = dashedSelected;
            normalStroke = normalSelected;            
        } 
        else
        {
            dashedStroke = dashedUnselected;
            normalStroke = normalUnselected;
        }

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setColor(normalColour);

        // Start from the centre of the src class
        Point pFrom = new Point(d.getFrom().getX() + (int)d.getFrom().getWidth()/2, d.getFrom().getY() + (int)d.getFrom().getHeight()/2);
        Point pTo = new Point(d.getTo().getX() + (int)d.getTo().getWidth()/2, d.getTo().getY() + (int)d.getTo().getHeight()/2);

        // Get the angle of the line from src to dst.
        double angle = Math.atan2(-(pFrom.getY() - pTo.getY()), pFrom.getX() - pTo.getX());

        // Get the dest point
        pFrom = ((DependentTarget)d.getFrom()).getAttachment(angle + Math.PI);
        pTo = ((DependentTarget)d.getTo()).getAttachment(angle);

        Point pArrow = new Point(pTo.x + (int)((ARROW_SIZE - 2) * Math.cos(angle)), pTo.y - (int)((ARROW_SIZE - 2) * Math.sin(angle)));

        // draw the arrow head
        int[] xPoints =  { pTo.x, pTo.x + (int)((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)), pTo.x + (int)(ARROW_SIZE * Math.cos(angle - ARROW_ANGLE)) };
        int[] yPoints =  { pTo.y, pTo.y - (int)((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)), pTo.y - (int)(ARROW_SIZE * Math.sin(angle - ARROW_ANGLE)) };
        
        g.setStroke(dashedStroke);
        g.drawLine(pFrom.x, pFrom.y, pArrow.x, pArrow.y);
        
        g.setStroke(normalStroke);
        g.drawPolygon(xPoints, yPoints, 3);

        g.setStroke(oldStroke);
    }
    /* (non-Javadoc)
     * @see bluej.pkgmgr.graphPainter.DependencyPainter#getPopupMenuPosition(bluej.pkgmgr.dependency.Dependency)
     */
    public Point getPopupMenuPosition(Dependency dependency) {
        ImplementsDependency usesDependency;
        if (! (dependency instanceof ImplementsDependency)){
            throw new IllegalArgumentException("Not a ExtendsDependency");
        }
        usesDependency = (ImplementsDependency) dependency;
        Point pFrom = new Point(dependency.getFrom().getX() + dependency.getFrom().getWidth()/2,
                				dependency.getFrom().getY() + dependency.getFrom().getHeight()/2);
        Point pTo = new Point(dependency.getTo().getX() + dependency.getTo().getWidth()/2,
                			  dependency.getTo().getY() + dependency.getTo().getHeight()/2);
        // Get the angle of the line from src to dst.
        double angle = Math.atan2(-(pFrom.y - pTo.y), pFrom.x - pTo.x);
        pTo = ((DependentTarget)dependency.getTo()).getAttachment(angle);
        
//      draw the arrow head
        int[] xPoints =  { pTo.x, pTo.x + (int)((ARROW_SIZE) * Math.cos(angle + ARROW_ANGLE)), pTo.x + (int)(ARROW_SIZE * Math.cos(angle - ARROW_ANGLE)) };
        int[] yPoints =  { pTo.y, pTo.y - (int)((ARROW_SIZE) * Math.sin(angle + ARROW_ANGLE)), pTo.y - (int)(ARROW_SIZE * Math.sin(angle - ARROW_ANGLE)) };
                
        return new Point((xPoints[0] + xPoints[1] + xPoints[2])/3, 
                		 (yPoints[0] + yPoints[1] + yPoints[2])/3);
    }
}
