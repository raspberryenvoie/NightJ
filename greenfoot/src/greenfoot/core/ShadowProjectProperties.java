package greenfoot.core;

import java.util.HashMap;
import java.util.Map;

/**
 * A class which resides on the debug VM.  Its job is to hold copies of the project
 * properties, mirrored across from the server VM.
 * 
 * Note that at the moment, this appears to be a writeable item, with methods to set
 * properties.  This is only temporary, as it lets us keep in all the existing setString
 * etc methods to see where these setters are when we transition code across to the
 * server VM.  Once all the Greenfoot FX rewrite has been completed, there should be no
 * code left on the debug VM attempting to call the setters.  At that point, this class
 * can implement ReadOnlyProjectProperties and we can remove the ProjectProperties interface
 * entirely, and remove all the dummy setters from this class.
 */
public class ShadowProjectProperties implements ProjectProperties
{
    private final Map<String, String> properties = new HashMap<>();

    /**
     * Called when a property has changed on the server VM, and the change needs
     * to be inserted into our shadow copy of the properties
     */
    public void propertyChangedOnServerVM(String key, String value)
    {
        if (value == null)
        {
            properties.remove(key);
        }
        else
        {
            properties.put(key, value);
        }
    }
    
    
    @Override
    public void setString(String key, String value)
    {
        // This method is retained as a marker for code that needs changing.
        // All current debug VM uses of setString should end up on the server VM
        // At that point, this method should be removed.

        // Once removeProperty, setString and save have been removed,
        // ShadowProjectProperties should be changed to implement
        // ReadOnlyProjectProperties, and the ProjectProperties interface
        // should be deleted, too.
    }

    @Override
    public String removeProperty(String key)
    {
        // This method is retained as a marker for code that needs changing.
        // All current debug VM uses of setString should end up on the server VM
        // At that point, this method should be removed.

        // Once removeProperty, setString and save have been removed,
        // ShadowProjectProperties should be changed to implement
        // ReadOnlyProjectProperties, and the ProjectProperties interface
        // should be deleted, too.
        
        return null;
    }

    @Override
    public void save()
    {
        // This method is retained as a marker for code that needs changing.
        // All current debug VM uses of setString should end up on the server VM
        // At that point, this method should be removed.

        // Once removeProperty, setString and save have been removed,
        // ShadowProjectProperties should be changed to implement
        // ReadOnlyProjectProperties, and the ProjectProperties interface
        // should be deleted, too.
    }

    @Override
    public String getString(String key, String defaultValue)
    {
        return properties.getOrDefault(key, defaultValue);
    }
}
