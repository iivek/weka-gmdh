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

import wGmdh.jGmdh.oldskul.*;
import java.io.Serializable;
import java.util.Enumeration;
import java.util.Vector;
import weka.core.Capabilities;
import weka.core.CapabilitiesHandler;
import weka.core.Option;
import weka.core.OptionHandler;
import weka.core.Utils;

/**
 * An abstraction of selection criteria and error measures.
 *
 * @author ivek
 */
public abstract class Performance
        implements Serializable, OptionHandler, CapabilitiesHandler {

    private static final long serialVersionUID = -5801952488935916002L;

    protected Measure measure;
    protected boolean debug = false;

    public void setMeasure(Measure m) {
        measure = m;
    }

    public Measure getMeasure() {
        return measure;
    }

    abstract public double calculate(Model model, int fold, double[] regressionGoal);

    /**
     *
     * @param options
     * @throws java.lang.Exception
     */
    public void setOptions(String[] options) throws Exception {

        setDebug(Utils.getFlag('D', options));
        Utils.checkForRemainingOptions(options);
    }

    /**
     *
     * @return
     */
    public String[] getOptions() {
        Vector result;

        result = new Vector();

        if (getDebug()) {
            result.add("-D");
        }

        return (String[]) result.toArray(new String[result.size()]);
    }

    /**
     *
     * @return
     */
    public Capabilities getCapabilities() {
        return new Capabilities(this);
    }

    public Enumeration listOptions() {
        Vector options;

        options = new Vector();

        options.addElement(new Option(
                "\tStandard debugging switch.\n",
                "D", 0, "-D"));

        return options.elements();
    }

    public void setDebug(boolean b) {
        debug = b;
    }

    public boolean getDebug() {
        return debug;
    }
}
