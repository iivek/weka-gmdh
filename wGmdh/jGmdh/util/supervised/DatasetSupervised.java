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

import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Instances;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 *
 * @author ivek
 */
public abstract class DatasetSupervised implements DatasetHandlerSupervised, Serializable, OptionHandler {

    protected int folds;
    protected Vector<Instances> validationSets;
    protected Vector<Instances> learningSets;
    protected Vector<double[]> validationGoals;
    protected Vector<double[]> learningGoals;

    private Instances instances;

    /**
     * @return the dataset
     */
    public Instances getInstances() {
        return instances;
    }

    /**
     * @param dataset the dataset to set
     */
    public void setInstances(Instances dataset) {
        this.instances = dataset;
    }

    /**
     * @return the folds
     */
    public int getDimension() {
        return folds;
    }

    /**
     * @param dimension     nr folds
     */
    protected void setDimension(int dimension) {
        this.folds = dimension;
    }

    public void initialize()    {
    }

    public Enumeration listOptions() {
         Vector options;

        options = new Vector();
        return options.elements();
    }


    public void setOptions(String[] options) throws Exception {
        Utils.checkForRemainingOptions(options);
    }

    public String[] getOptions() {
         Vector result;

        result = new Vector();

        return (String[]) result.toArray(new String[result.size()]);
    }
}
