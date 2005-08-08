package bluej.debugger.gentype;

import java.util.ArrayList;
import java.util.Map;

/**
 * A wildcard type with an upper and/or lower bound.<p>
 * 
 * Note that both an upper and lower bound is allowed. This type doesn't occur
 * naturally- it can't be specified in the Java language. But in some cases we
 * can deduce the type of some object to be this.<p>
 *
 * This is an Immutable type.
 * 
 * @author Davin McCall
 * @version $Id: GenTypeWildcard.java 3508 2005-08-08 04:18:26Z davmac $
 */
public class GenTypeWildcard extends GenTypeParameterizable
{
    GenTypeSolid upperBound; // ? extends upperBound
    GenTypeSolid lowerBound; // ? super lowerBound
    
    public GenTypeWildcard(GenTypeSolid upper, GenTypeSolid lower)
    {
        upperBound = upper;
        lowerBound = lower;
    }
    
    /**
     * Constructor with a given range of upper and lower bounds. The arrays
     * used should not be modified afterwards.
     * 
     * @param uppers  The upper bounds
     * @param lowers  The lower bounds
     */
    public GenTypeWildcard(GenTypeSolid [] uppers, GenTypeSolid [] lowers)
    {
        if (uppers.length != 0)
            upperBound = IntersectionType.getIntersection(uppers);
        if (lowers.length != 0)
            lowerBound = GenTypeSolid.lub(lowers);
    }
    
    public String toString()
    {
        return toString(false);
    }
    
    public String toString(boolean stripPrefix)
    {
        if (lowerBound != null) {
            return "? super " + lowerBound.toString(stripPrefix);
        }
        else if (upperBound != null) {
            String uboundStr = upperBound.toString();
            if (! uboundStr.equals("java.lang.Object"))
                return "? extends " + upperBound.toString(stripPrefix);
        }
        return "?";
    }
    
    public String toString(NameTransform nt)
    {
        if (lowerBound != null) {
            return "? super " + lowerBound.toString(nt);
        }
        else if (upperBound != null) {
            String uboundStr = upperBound.toString();
            if (! uboundStr.equals("java.lang.Object"))
                return "? extends " + upperBound.toString(nt);
        }
        return "?";
    }
    
    public String arrayComponentName()
    {
        return getErasedType().arrayComponentName();
    }
    
    /*
     * Do not create abominations such as "? extends ? extends ...".
     *   "? extends ? super ..."    => "?" (ie. the bounds is eliminated).
     *   "? extends ? extends X"    => "? extends X".
     *   "? super ? super X"        => "? super X".
     */
    public JavaType mapTparsToTypes(Map tparams)
    {
        GenTypeSolid newUpper = null;
        GenTypeSolid newLower = null;
        
        // Find new upper bounds
        if (upperBound != null) {
            ArrayList newUppers = new ArrayList();
            GenTypeSolid [] upperBounds = upperBound.getUpperBounds();
            
            // find the new upper bounds
            for (int i = 0; i < upperBounds.length; i++) {
                GenTypeParameterizable newBound = (GenTypeParameterizable) upperBounds[i].mapTparsToTypes(tparams);
                if (newBound instanceof GenTypeWildcard) {
                    GenTypeWildcard newWcBound = (GenTypeWildcard) newBound;
                    newUppers.add(newWcBound.upperBound);
                }
                else
                    newUppers.add(newBound);
            }
            GenTypeSolid [] newUppersA = (GenTypeSolid []) newUppers.toArray(new GenTypeSolid[newUppers.size()]);
            newUpper = IntersectionType.getIntersection(newUppersA);
        }
        
        // find the new lower bounds
        // This is easier. If the lower bound is an intersection type, it comes from
        // lub() and therefore contains no immediate type parameters.
        if (lowerBound != null) {
            GenTypeParameterizable newLowerP = (GenTypeParameterizable) lowerBound.mapTparsToTypes(tparams);
            newLower = newLowerP.getLowerBound();
        }
        
        if (newUpper != null && newUpper.equals(newLower))
            return newUpper;
        else
            return new GenTypeWildcard(newUpper, newLower);
    }
    
    public boolean equals(GenTypeParameterizable other)
    {
        if (this == other)
            return true;
        
        GenTypeSolid otherLower = other.getLowerBound();
        GenTypeSolid otherUpper = other.getUpperBound();
        
        if (upperBound != null && ! upperBound.equals(otherUpper))
            return false;
        if (upperBound == null && otherUpper != null)
            return false;
        if (lowerBound != null && ! lowerBound.equals(otherLower))
            return false;
        if (lowerBound == null && otherLower != null)
            return false;
        
        return true;
    }
    
    public JavaType getErasedType()
    {
        return upperBound.getErasedType();
    }
    
    public boolean isPrimitive()
    {
        return false;
    }
    
    public boolean isAssignableFrom(JavaType t)
    {
        // This shouldn't be called.
        return false;
    }
    
    public boolean isAssignableFromRaw(JavaType t)
    {
        // This shouldn't be called.
        return false;
    }
    
    /**
     * Get the upper bounds of this wildcard type, as an array. The upper
     * bounds are those occurring in "extends" clauses.
     * 
     * @return A copy of the upper bounds.
     */
    public GenTypeSolid[] getUpperBounds()
    {
        return upperBound.getUpperBounds();
    }
    
    public GenTypeSolid getUpperBound()
    {
        return upperBound;
    }
    
    /**
     * Get the lower bounds of this wildcard type, as an array. The lower
     * bounds are those occurring in "super" clauses.
     * 
     * @return A copy of the lower bounds.
     */
    public GenTypeSolid getLowerBound()
    {
        return lowerBound;
    }
    
    public boolean contains(GenTypeParameterizable other)
    {
        GenTypeSolid otherUpper = other.getUpperBound();
        GenTypeSolid otherLower = other.getLowerBound();
        
        if (upperBound != null) {
            if (otherUpper == null || ! upperBound.isAssignableFrom(otherUpper))
                return false;
        }
        
        if (lowerBound != null) {
            if (otherLower == null || ! otherLower.isAssignableFrom(lowerBound))
                return false;
        }
        
        return true;
    }
}
