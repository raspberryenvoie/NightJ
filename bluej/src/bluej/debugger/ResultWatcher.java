package bluej.debugger;

/**
 * Debugger interface implemented by classes interested in the result of an invocation
 *
 * @author  Michael Cahill
 * @version $Id: ResultWatcher.java 1459 2002-10-23 12:13:12Z jckm $
 */
public interface ResultWatcher
{
	/**
	 * An invocation has completed - here is the result
	 */
	void putResult(DebuggerObject result, String name);
	
	/**
	 * An invocation has failed - here is the error message
	 */
	void putError (String message);
}
