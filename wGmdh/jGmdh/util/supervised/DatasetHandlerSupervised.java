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

package wGmdh.jGmdh.util.supervised;

import java.util.Iterator;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import weka.core.Instances;

/**
 * Interface intended to take care of the dataset, extract learning and
 * structure validation sets, and delegate calls to the right methods.
 * Additionaly, it is made to extract outputs from Nodes
 *
 * @author ivek
 */
public interface DatasetHandlerSupervised {

    public Iterator<Instances> getValidationSets();

    public Iterator<double[]> getValidationGoals();

    public Iterator<Instances> getLearningSets();

    public Iterator<double[]> getLearningGoals();

    public Iterator<double[][]> getValidationInputs(Model m);

    public Iterator<double[][]> getLearningInputs(Model m);
    
    public Iterator<double[]> getValidationOutputs(Node n);

    public Iterator<double[]> getLearningOutputs(Node n);
}
