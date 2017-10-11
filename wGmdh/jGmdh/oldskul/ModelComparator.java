/*
 *    This program is free software; you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation; either version 2 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program; if not, write to the Free Software
 *    Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

/*
 *    Copyright (C) 2010 Ivan Ivek
 */

package wGmdh.jGmdh.oldskul;

import java.io.Serializable;
import java.util.Comparator;

/**
 *
 * A comparator class used for sorting Model objects
 * by descending validation set error.
 *
 * @author ivek
 */
public class ModelComparator implements Comparator, Serializable {

    private static final long serialVersionUID = 5758536425315582836L;

    public int compare(Object model1, Object model2) {
        if (((Model) model1).getErrorStructureValidationSet() >
                ((Model) model2).getErrorStructureValidationSet()) {
            return -1;
        }

        if (((Model) model1).getErrorStructureValidationSet() ==
                ((Model) model2).getErrorStructureValidationSet()) {
            return 0;
        }
        if (((Model) model1).getErrorStructureValidationSet() <
                ((Model) model2).getErrorStructureValidationSet()) {
            return 1;
        }
        Double dbl = new Double(((Model) model1).getErrorStructureValidationSet());
        if (dbl.isNaN())    {
            return -1;
        }
        dbl = new Double(((Model) model2).getErrorStructureValidationSet());
        if (dbl.isNaN())    {
            return 1;
        }
        // can never happen
        System.out.println("can never happen" + ((Model) model1).getErrorStructureValidationSet() +
                " " + ((Model) model2).getErrorStructureValidationSet());
        return 0;


    }
}
