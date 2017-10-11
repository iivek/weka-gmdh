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

import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.oldskul.Model;

/**
 * Sum of Squared Errors
 *
 * @author ivek
 */
public class Sse extends Measure {

    private static final long serialVersionUID = -7916131037223479679L;

    public double calculate(Model model,
            double[] modeled, double[] goal) throws Exception {
        if (modeled.length != goal.length) {
            throw new TooBig("Array lengths differ.");
        } else {
            double sse = 0;
            for (int i = 0; i < modeled.length; ++i) {
                sse += Math.pow(goal[i] - modeled[i], 2);
            }
            return sse;
        }
    }
}
