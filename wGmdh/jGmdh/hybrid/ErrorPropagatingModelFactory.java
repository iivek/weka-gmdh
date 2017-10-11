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

package wGmdh.jGmdh.hybrid;

import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.exceptions.TooSmall;
import wGmdh.jGmdh.oldskul.ModelFactory;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import wGmdh.jGmdh.oldskul.measures.Performance;
import wGmdh.jGmdh.util.supervised.DatasetHandlerSupervised;

/**
 *
 * @author ivek
 */
public class ErrorPropagatingModelFactory implements ModelFactory {

    private static final long serialVersionUID = -5893369978565802039L;

    public Model instantiate() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Deprecated
    public Model instantiate(
            double[] regressionGoals,
            Performance selectionCriterion,
            Performance errorMeasure,
            Node... links) throws TooBig, ExpressionEqualToZero, TooSmall {

        return new ErrorPropagatingModel(
                regressionGoals, selectionCriterion, errorMeasure, links);
    }

    public Model instantiate(Performance selectionCriterion, Performance errorMeasure, Node... links) throws Exception {
        return new ErrorPropagatingModel(
                selectionCriterion, errorMeasure, links);
    }

    public Model instantiate(DatasetHandlerSupervised provider, Performance selectionCriterion, Performance errorMeasure, Node... links) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}

