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

package wGmdh.jGmdh.oldskul.measures;

import wGmdh.jGmdh.oldskul.Model;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A delegate for error measures.
 * 
 * TODO: a strategy design pattern would make choice of measures even more
 * flexible (i.e. during runtime)
 *
 * @author ivek
 */
public class StructureLearningPerformance extends Performance {

    private static final long serialVersionUID = -4333074083199891703L;

    public StructureLearningPerformance(Measure m) {
        super();
        setMeasure(m);
    }

    @Override
    public double calculate(Model model, int fold, double[] regressionGoal) {
        double result = -1;
        try {
            result = measure.calculate(
                    model, model.trainSetOutput.get(fold), regressionGoal);
/* Debug:
 for (int i = 0; i < regressionGoal.length; ++i) {
                System.out.println("Performance learn sayS: " + model.trainSetOutput.get(fold)[i] + ", " + regressionGoal[i]);
            }
*/

        } catch (Exception ex) {
            Logger.getLogger(StructureLearningPerformance.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
}
