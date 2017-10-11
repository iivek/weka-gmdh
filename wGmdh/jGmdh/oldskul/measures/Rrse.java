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

import java.util.Hashtable;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.oldskul.Model;

/**
 * Root Relative Squared Error.
 * Optimized when the error is calculated against the same data over and over
 * again. Not appropriate if you need to calculate error on different data
 * often
 *
 * @author ivek
 */
public class Rrse extends Measure {

    private static final long serialVersionUID = -746248272276730233L;
    /* To speed things up when we need errors on the same data more than once,
     * we'll store pairs of already seen data and their denumerators
     */
    private Hashtable<double[], Double> ht = new Hashtable<double[], Double>();

    private double calculateNormalizingFactor(double goal[]) {

        int length = goal.length;
        double average = 0;
        for (int i = 0; i < length; ++i) {
            average += goal[i];
        }
        average /= length;

        double denominator = 0;
        for (int i = 0; i < length; ++i) {
            denominator += Math.pow(goal[i] - average, 2);
        }
        denominator = Math.sqrt(denominator);
        ht.put(goal, denominator);

        return denominator;
    }


    public double calculate(Model model,
            double[] modeled, double[] goal) throws Exception {
        int length = modeled.length;
        if (length != goal.length) {
            throw new TooBig("Array lengths differ.");
        } else {
            /* Calculate numerator
             */
            double numerator = 0;
            for (int i = 0; i < modeled.length; ++i) {
                numerator += Math.pow(goal[i] - modeled[i], 2);
            }
            /* Take a Peek in ht - if we've seen these goals before, this means
             * that we already had calculated the corresponding denominator, so
             * we'll simply retrieve it. We'll need to calculate it otherwise.
             */
            Double denominator = ht.get(goal);
            if(denominator != null) {
                return Math.sqrt(numerator)/denominator;
            }
            else    {
                return Math.sqrt(numerator)/calculateNormalizingFactor(goal);
            }
        }
    }
}
