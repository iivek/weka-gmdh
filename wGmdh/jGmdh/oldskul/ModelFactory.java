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
import wGmdh.jGmdh.oldskul.measures.Performance;
import wGmdh.jGmdh.util.supervised.DatasetHandlerSupervised;

/**
 * A model factory interface
 *
 * @author ivek
 */
public interface ModelFactory extends Serializable{

    public Model instantiate() throws Exception;

    public Model instantiate(DatasetHandlerSupervised provider,
            Performance selectionCriterion,
            Performance errorMeasure, Node... links) throws Exception;
    @Deprecated
    public Model instantiate(double[] regressionGoals,
            Performance selectionCriterion,
            Performance errorMeasure, Node... links) throws Exception;

    public Model instantiate(Performance selectionCriterion,
            Performance errorMeasure, Node... links)
            throws Exception;
}
