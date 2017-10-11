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
 * A delegate for performance measures on an independent set for model selection
 *
 * TODO: a strategy design pattern would make choice of measures/metrics more
 * flexible
 *
 * @author ivek
 */
public class StructureValidationPerformance extends Performance   {

    private static final long serialVersionUID = -7220786679354929625L;

    public StructureValidationPerformance(Measure m)   {
        super();
        setMeasure(m);
    }

    @Override
    public double calculate(Model model, int fold, double[] regressionGoal) {
        double result = -1;
        try {
            result = measure.calculate(
                    model, model.validationSetOutput.get(fold), regressionGoal);
        } catch (Exception ex) {
            Logger.getLogger(StructureLearningPerformance.class.getName()).log(Level.SEVERE, null, ex);
        }
        return result;
    }


}
