package bluej.graph;

import java.awt.Point;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.Iterator;

import bluej.Config;
import bluej.pkgmgr.dependency.Dependency;
import bluej.pkgmgr.graphPainter.GraphPainterStdImpl;
import bluej.pkgmgr.target.*;
import bluej.pkgmgr.Package;

/**
 * This class controls the selection (the set of selected elements in the graph).
 * To do this, it maintains a selection set, a marquee (a graphical selection rectangle)
 * and a rubber band (for drawing new edges). Both the marquee and the rubber band can
 * be inactive.
 */
public class SelectionController
    implements MouseListener, MouseMotionListener, KeyListener
{
    private GraphEditor graphEditor;
    private Graph graph;
    
    private Marquee marquee; 
    private SelectionSet selection;   // Contains the elements that have been selected
    private RubberBand rubberBand;
    
    private boolean moving = false; 
    private boolean resizing = false; 

    private int dragStartX;
    private int dragStartY;

    private int keyDeltaX;
    private int keyDeltaY;

    private int currentDependencyIndex;  // for cycling through dependencies

    private TraverseStragegy traverseStragegiImpl = new TraverseStragegyImpl();

    
    /**
     * Create the controller for a given graph editor.
     * @param graphEditor
     * @param graph
     */
    public SelectionController(GraphEditor graphEditor)
    {
        this.graphEditor = graphEditor;
        this.graph = graphEditor.getGraph();
        marquee = new Marquee(graph);
        selection = new SelectionSet();

    }

    // ======= MouseListener interface =======

    /**
     * A mouse-pressed event. Analyse what we should do with it.
     */
    public void mousePressed(MouseEvent evt)
    {
        graphEditor.requestFocus();
        int clickX = evt.getX();
        int clickY = evt.getY();

        SelectableGraphElement clickedElement = graph.findGraphElement(clickX, clickY);
        notifyPackage(clickedElement);
        
        if (clickedElement == null) {                           // nothing hit
            if (!isMultiselectionKeyDown(evt)) {
                selection.clear();
            }
            if (isButtonOne(evt))
                marquee.start(clickX, clickY);
        }
        else if (isButtonOne(evt)) {                            // clicked on something
            if (isMultiselectionKeyDown(evt)) {
                // a class was clicked, while multiselectionKey was down.
                if (clickedElement.isSelected()) {
                    selection.remove(clickedElement);
                }
                else {
                    selection.add(clickedElement);
                }
            }
            else {
                // a class was clicked without multiselection
                if (! clickedElement.isSelected()) {
                    selection.selectOnly(clickedElement);
                }
            }

            if(isDrawingDependency()) {
                if (clickedElement instanceof Target)
                    rubberBand = new RubberBand(clickX, clickY, clickX, clickY);
            }
            else {
                dragStartX = clickX;
                dragStartY = clickY;

                if(clickedElement.isHandle(clickX, clickY)) {
                    resizing = true;
                }
                else {
                    moving = true;                        
                }
            }
        }
    }

    /**
     * The mouse was released.
     */
    public void mouseReleased(MouseEvent evt)
    {
        if (isDrawingDependency()) {
            SelectableGraphElement selectedElement = graph.findGraphElement(evt.getX(), evt.getY());
            notifyPackage(selectedElement);
        }
        rubberBand = null;
        
        SelectionSet newSelection = marquee.stop();     // may or may not have had a marquee...
        if(newSelection != null) {
            selection.addAll(newSelection);
        }
        
        if(moving || resizing) {
            selection.moveStopped();
            moving = false;
            resizing = false;
        }
        
        graphEditor.repaint();
    }
    
    /**
     * A mouse-clicked event. This is only interesting if it was a double
     * click. If so, inform every element in the current selection.
     */
    public void mouseClicked(MouseEvent evt)
    {
        if (isButtonOne(evt)) {
            if (evt.getClickCount() > 1) {
                selection.doubleClick(evt);
            }
//            else {
//                SelectableGraphElement clickedElement = graph.findGraphElement(evt.getX(), evt.getY());
//                if(clickedElement != null)
//                    selection.selectOnly(clickedElement);
//            }
        }
    }

    /**
     * The mouse pointer entered this component.
     */
    public void mouseEntered(MouseEvent e) {}

    /**
     * The mouse pointer exited this component.
     */
    public void mouseExited(MouseEvent e) {}

    // ======= end of MouseListener interface =======

    // ======= MouseMotionListener interface: =======

    /**
     * The mouse was moved - not interested here.
     */
    public void mouseMoved(MouseEvent evt) {}

    /**
     * The mouse was dragged - either draw a marquee or move some classes.
     */
    public void mouseDragged(MouseEvent evt)
    {
        if (isButtonOne(evt)) {
            if (marquee.isActive()) {
                marquee.move(evt.getX(), evt.getY());
                graphEditor.repaint();
            }
            else if (rubberBand != null) {
                rubberBand.setEnd(evt.getX(), evt.getY());
            }
            else 
            {
                if(! selection.isEmpty()) {
                    int deltaX = snapToGrid(evt.getX() - dragStartX);
                    int deltaY = snapToGrid(evt.getY() - dragStartY);
    
                    if(resizing) {
                        selection.resize(deltaX, deltaY);
                    }
                    else { // moving
                        selection.move(deltaX, deltaY);
                    }
                }
            }
        }
        graphEditor.repaint();
    }

    // ======= end of MouseMotionListener interface =======

    // ======= KeyListener interface =======

    /**
     * A key was pressed in the graph editor.
     */
    public void keyPressed(KeyEvent evt)
    {
        boolean handled = true; // assume for a start that we are handling the
                                // key here

        if (isArrowKey(evt)) {
            if (evt.isControlDown()) {      // resizing
                if(!resizing)
                    startKeyboardResize();
                setKeyDelta(evt);
                selection.resize(keyDeltaX, keyDeltaY);
            }
            else if (evt.isShiftDown()) {   // moving targets
                if(!moving)
                    startKeyboardMove();
                setKeyDelta(evt);
                selection.move(keyDeltaX, keyDeltaY);
            }
            else {                          // navigate the diagram
                navigate(evt);
            }
        }

        else if (isPlusOrMinusKey(evt)) {
            resizeWithFixedRatio(evt);
        }

        // dependency selection
        else if (evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
            selectDependency(evt);
        }

        // post context menu
        else if (evt.getKeyCode() == KeyEvent.VK_SPACE || evt.getKeyCode() == KeyEvent.VK_ENTER) {
            postMenu();
        }

        // 'A' (with any or no modifiers) selects all
        else if (evt.getKeyCode() == KeyEvent.VK_A) {
            selectAll();
        }

        // Escape removes selections
        else if (evt.getKeyCode() == KeyEvent.VK_ESCAPE) {
            clearSelection();
        }

        else {
            handled = false;
        }

        if (handled)
            evt.consume();

        graphEditor.repaint();
    }

    
    /**
     * A key was released. Check whether a key-based move or resize operation
     * has ended.
     */
    public void keyReleased(KeyEvent evt)
    {
        if(moving && (!evt.isShiftDown())) {    // key-based moving stopped
            selection.moveStopped();
            moving = false;
        }
        else if(resizing && (!evt.isControlDown())) {    // key-based moving stopped
            selection.moveStopped();
            resizing = false;
        }
        graphEditor.repaint();
    }

    /**
     * Key typed - of no interest to us.
     */
    public void keyTyped(KeyEvent evt) {}

    // ======= end of KeyListener interface =======


    private void notifyPackage(GraphElement element)
    {
        if(element instanceof ClassTarget)
            ((Package)graph).targetSelected((Target)element);
        else
            ((Package)graph).targetSelected(null);
    }
    
    /**
     * Tell whether the package is currently drawing a dependency.
     */
    public boolean isDrawingDependency()
    {
        return (((Package)graph).getState() == Package.S_CHOOSE_USES_TO)
                || (((Package)graph).getState() == Package.S_CHOOSE_EXT_TO);
    }

    
    private static boolean isArrowKey(KeyEvent evt)
    {
        return evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_LEFT || evt.getKeyCode() == KeyEvent.VK_RIGHT;
    }

    /**
     * Move the current selection to another selected class, depending on
     * current selection and the key pressed.
     */
    private void navigate(KeyEvent evt)
    {
        Vertex currentTarget = findSingleVertex();
        currentTarget = traverseStragegiImpl.findNextVertex(graph, currentTarget, evt.getKeyCode());
        selection.selectOnly(currentTarget);
    }

    /**
     * Prepare a key-based move operation.
     */
    private void startKeyboardMove()
    {
        keyDeltaX = 0;
        keyDeltaY = 0;
        moving = true;
    }
     
    /**
     * Prepare a key-based resize operation.
     */
    private void startKeyboardResize()
    {
        keyDeltaX = 0;
        keyDeltaY = 0;
        resizing = true;
    }
     
    /**
     * Move all targets according to the supplied key.
     */
    private void setKeyDelta(KeyEvent evt)
    {
        switch(evt.getKeyCode()) {
            case KeyEvent.VK_UP : {
                keyDeltaY -= GraphEditor.GRID_SIZE;
                break;
            }
            case KeyEvent.VK_DOWN : {
                keyDeltaY += GraphEditor.GRID_SIZE;
                break;
            }
            case KeyEvent.VK_LEFT : {
                keyDeltaX -= GraphEditor.GRID_SIZE;
                break;
            }
            case KeyEvent.VK_RIGHT : {
                keyDeltaX += GraphEditor.GRID_SIZE;
                break;
            }
        }
    }

    /**
     * Is the pressed key a plus or minus key?
     */
    private boolean isPlusOrMinusKey(KeyEvent evt)
    {
        return evt.getKeyChar() == '+' || evt.getKeyChar() == '-';
    }

    /**
     * @param evt
     */
    private void resizeWithFixedRatio(KeyEvent evt)
    {
        int delta = (evt.getKeyChar() == '+' ? GraphEditor.GRID_SIZE : -GraphEditor.GRID_SIZE);
        selection.resize(delta, delta);
        selection.moveStopped();
    }
    
    /**
     * @param evt
     */
    private void selectDependency(KeyEvent evt)
    {
        Vertex vertex = selection.getAnyVertex();
        if(vertex != null) {
            selection.selectOnly(vertex);
            List dependencies = ((DependentTarget) vertex).dependentsAsList();

            Dependency currentDependency = (Dependency) dependencies.get(currentDependencyIndex);
            if (currentDependency != null) {
                selection.remove(currentDependency);
            }
            currentDependencyIndex += (evt.getKeyCode() == KeyEvent.VK_PAGE_UP ? 1 : -1);
            currentDependencyIndex %= dependencies.size();
            if (currentDependencyIndex < 0) {//% is not a real modulo
                currentDependencyIndex = dependencies.size() - 1;
            }
            currentDependency = (Dependency) dependencies.get(currentDependencyIndex);
            if (currentDependency != null) {
                selection.add(currentDependency);
            }
        }
    }

    /**
     * A menu popup trigger has been detected. Handle it.
     */
    public void handlePopupTrigger(MouseEvent evt)
    {
        int clickX = evt.getX();
        int clickY = evt.getY();

        SelectableGraphElement clickedElement = graph.findGraphElement(clickX, clickY);
        if (clickedElement != null) {
            selection.selectOnly(clickedElement);
            postMenu(clickedElement, clickX, clickY);
        }
    }
    
    /**
     * Post the context menu of one selected element of the current selection.
     * If any dependencies are selected, show the menu for one of those. Otherwise
     * show the menu for a randomly chosen target.
     */
    private void postMenu()
    {
        // first check whether we have selected edges
        Dependency dependency = (Dependency) selection.getAnyEdge();
        if (dependency != null) {
            Point p = ((GraphPainterStdImpl) GraphPainterStdImpl.getInstance()).getDependencyPainter(dependency)
                    .getPopupMenuPosition(dependency);
            postMenu(dependency, p.x, p.y);
        }
        else {
            // if not, choose a target
            Vertex vertex = selection.getAnyVertex();
            if(vertex != null) {
                selection.selectOnly(vertex);
                int x = vertex.getX() + vertex.getWidth() - 20;
                int y = vertex.getY() + 20;
                postMenu(vertex, x, y);
            }
        }
    }

    
    /**
     * Post the context menu for a given element at the given screen position.
     */
    private void postMenu(SelectableGraphElement element, int x, int y)
    {
        element.popupMenu(x, y);
    }


    /**
     * Return the marquee of this conroller.
     */
    public Marquee getMarquee()
    {
        return marquee;
    }

    
    private Vertex findSingleVertex()
    {
        Vertex vertex = selection.getAnyVertex();

        // if there is no selection we select an existing vertex
        if (vertex == null) {
            vertex = (Vertex) graph.getVertices().next();
        }
        return vertex;
    }


    /**
     * Clear the current selection.
     */
    public void clearSelection()
    {
        selection.clear();
    }


    /** 
     * Select all graph vertices.
     */
    private void selectAll()
    {
        for(Iterator i = graph.getVertices(); i.hasNext(); ) {
            selection.add((SelectableGraphElement) i.next());
        }
    }
    
    
    /**
     * Clear the current selection.
     */
    public void removeFromSelection(SelectableGraphElement element)
    {
        selection.remove(element);
    }

   
    /**
     * Check whether this mouse event was from button one.
     * (Ctrl-button one on MacOS does not count - that posts the menu
     * se we consider that button two.)
     */
    private boolean isButtonOne(MouseEvent evt)
    {
        return !evt.isPopupTrigger() && ((evt.getModifiers() & MouseEvent.BUTTON1_MASK) != 0);
    }
    

    /**
     * Check whether the key used for multiple selections is down.
     */
    private boolean isMultiselectionKeyDown(MouseEvent evt)
    {
        if (Config.isMacOS()) {
            return evt.isShiftDown() || evt.isMetaDown();
        }
        else {
            return evt.isShiftDown() || evt.isControlDown();
        }

    }


    /**
     * Modify the given point to be one of the deined grid points.
     * 
     * @param point  The original point
     * @return      A point close to the original which is on the grid.
     */
    private int snapToGrid(int x)
    {
        int steps = x / GraphEditor.GRID_SIZE;
        int new_x = steps * GraphEditor.GRID_SIZE;//new x-coor w/ respect to
                                                  // grid
        return new_x;
    }

    /*
    // dead code
    keypressed:
    //init dependencies
    if (currentTarget instanceof DependentTarget) {
        dependencies = ((DependentTarget) currentTarget).dependentsAsList();
    }
    else {
        dependencies = new LinkedList();//dummy empty list
    }
    
    navigate:
    
        currentDependencyIndex = 0;
        if (currentDependency != null) {
            graphElementManager.remove(currentDependency);
            currentDependency = null;
        }

*/
    
    /**
     * Return the rubber band of this graph.
     * @return  The rubber band instance, or null if no rubber band is currently in use.
     */
    public RubberBand getRubberBand()
    {
        return rubberBand;
    }
}