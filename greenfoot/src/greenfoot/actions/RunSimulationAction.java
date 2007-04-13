package greenfoot.actions;

import greenfoot.core.Simulation;
import greenfoot.event.SimulationEvent;
import greenfoot.event.SimulationListener;

import java.awt.EventQueue;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import bluej.utility.Debug;

/**
 * @author Poul Henriksen
 * @version $Id: RunSimulationAction.java 4927 2007-04-13 02:57:12Z davmac $
 */
public class RunSimulationAction extends AbstractAction
    implements SimulationListener
{
    private static final String iconFile = "run.png";
    private static RunSimulationAction instance = new RunSimulationAction();
    
    /**
     * Singleton factory method for action.
     */
    public static RunSimulationAction getInstance()
    {
        return instance;
    }
    

    private Simulation simulation;

    private RunSimulationAction()
    {
        super("Run", new ImageIcon(RunSimulationAction.class.getClassLoader().getResource(iconFile)));
    }

    /**
     * Attach this action to a simulation object that it controls.
     */
    public void attachSimulation(Simulation simulation)
    {
        this.simulation = simulation;
        simulation.addSimulationListener(this);
    }
    
    public void actionPerformed(ActionEvent e)
    {
        if(simulation == null) {
            Debug.reportError("attempt to run a simulation while none exists.");
            return;
        }
        
        simulation.setPaused(false);
    }

    /**
     * Observing for the simulation state so we can dis/en-able us appropiately
     */
    public void simulationChanged(final SimulationEvent e)
    {
        EventQueue.invokeLater(new Runnable() {
            public void run()
            {
                int eventType = e.getType();
                if (eventType == SimulationEvent.STOPPED) {
                    setEnabled(true);
                }
                else if (eventType == SimulationEvent.STARTED) {
                    setEnabled(false);
                }
                else if (eventType == SimulationEvent.DISABLED) {
                    setEnabled(false);
                }
            }
        });
    }
}
