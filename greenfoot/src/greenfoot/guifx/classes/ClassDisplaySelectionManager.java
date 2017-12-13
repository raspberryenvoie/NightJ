/*
 This file is part of the Greenfoot program. 
 Copyright (C) 2017  Poul Henriksen and Michael Kolling 
 
 This program is free software; you can redistribute it and/or 
 modify it under the terms of the GNU General Public License 
 as published by the Free Software Foundation; either version 2 
 of the License, or (at your option) any later version. 
 
 This program is distributed in the hope that it will be useful, 
 but WITHOUT ANY WARRANTY; without even the implied warranty of 
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 GNU General Public License for more details. 
 
 You should have received a copy of the GNU General Public License 
 along with this program; if not, write to the Free Software 
 Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA. 
 
 This file is subject to the Classpath exception as provided in the  
 LICENSE.txt file that accompanied this code.
 */
package greenfoot.guifx.classes;

import java.util.HashSet;
import java.util.Set;

/**
 * A class for handling a single-selection across a set of ClassDisplay items.
 */
public class ClassDisplaySelectionManager
{
    private final Set<ClassDisplay> classDisplayList = new HashSet<>();
    private ClassDisplay selected = null;

    public ClassDisplaySelectionManager()
    {
    }

    /**
     * Add a ClassDisplay to the set of possibly-selected classes
     */
    public void addClassDisplay(ClassDisplay classDisplay)
    {
        classDisplayList.add(classDisplay);
    }

    /**
     * Select the given item.  The setSelected method of all classes will
     * be called with the relevant true/false state.
     */
    public void select(ClassDisplay target)
    {
        target.setSelected(true);
        selected = target;
        for (ClassDisplay display : classDisplayList)
        {
            if (!display.equals(target))
            {
                display.setSelected(false);
            }
        }
    }

    /**
     * Gets the currently selected ClassDisplay.  May be null.
     */
    public ClassDisplay getSelected()
    {
        return selected;
    }
}
