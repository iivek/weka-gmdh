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
 *    Additive version of Multilayered Selectional Combinatorial GMDH
 *    Copyright (C) 2010 Ivan Ivek
 */

package wGmdh;

import wGmdh.jGmdh.hybrid.ErrorPropagatingModelFactory;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;
import wGmdh.jGmdh.oldskul.Node;
import weka.core.Instances;

/**
 * TODO: visual representation is inappropriate - the nested polynomial as
 * output is wrong.
 *
 * @author ivek
 */
public class AdditiveMsc extends Msc {

    private static final long serialVersionUID = -6012189927838962858L;

    @Override
    public void initClassifier(Instances instances) throws Exception {

        Node.setGlobalIdentifier(0);    // this is optional, simply to start from
        // zero (a single overflow doesn't
        // matter but more than one should never
        // happen)

        currentlyBestStructure = null;

        /* Initialize our GMDH algorithm with provided datasets.
         */
        gmdhNet = null;
        Runtime.getRuntime().gc();
        getDataProvider().setInstances(instances);
        getDataProvider().initialize();
        gmdhNet = new MultiSelectCombi(
                getDataProvider(), new ErrorPropagatingModelFactory(), getSelector());
        getSelector().initialize(gmdhNet.getDataset());
        gmdhNet.initAttributeLayer();

        /* Provide gmdhNet with chosen performance classes
         */
        gmdhNet.setTrainingPerformance(structureLearningPerformance);
        gmdhNet.setSelectionPerformance(structureValidationPerformance);
    }

    /**
     * Isn't implemented yet
     *
     * @return string describing the model.
     */
 /*   @Override
    public String toString() {
        String output = null;
        return output;
    }
*/
    /**
     * Isn't implemented yet
     *
     * @return string describing the model.
     */
  /*  @Override
    public String graph() throws Exception {
        return null;
    }
*/
}
