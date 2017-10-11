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

package wGmdh.jGmdh.gui;

import java.awt.Adjustable;
import java.awt.Component;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;

/*
 * I need JScrollPane with subpixel accuracy. To achieve this systematically,
 * i'd need some kind of alternative to BoundedRangeModel that can deal with
 * doubles rather than ints, but all such Components use that interface;
 * it would require rewriting such Components' code making them work with a
 * more general Model.
 * For now, I am satisfied with a hack, additional listeners on the
 * scrollbars that set the double values.
 *
 * @author ivek
 */
public class SubpixelScrollPane
        extends JScrollPane implements AdjustmentListener {

    private static final long serialVersionUID = 4890603113076834618L;

    double horizontalPosition;
    double verticalPosition;
    /* true if invoked from GUI, false if invoked internally by us.
     */
    boolean invokedFromGUI_h;
    boolean invokedFromGUI_v;

    SubpixelScrollPane() {
        super();
        this.getHorizontalScrollBar().addAdjustmentListener(this);
        this.getVerticalScrollBar().addAdjustmentListener(this);
        invokedFromGUI_h = false;
        invokedFromGUI_v = false;
        horizontalPosition = 0;
        verticalPosition = 0;
    }

    SubpixelScrollPane(Component view) {
        super(view);
        this.getHorizontalScrollBar().addAdjustmentListener(this);
        this.getVerticalScrollBar().addAdjustmentListener(this);
        invokedFromGUI_h = false;
        invokedFromGUI_v = false;
        horizontalPosition = this.getHorizontalScrollBar().getValue();
        verticalPosition = this.getHorizontalScrollBar().getValue();
    }

    public void setVerticalScrollBarValue(double newValue) {
        verticalPosition = newValue;
        invokedFromGUI_v =
                false;
        getVerticalScrollBar().setValue((int) java.lang.Math.round(newValue));
    }

    public double getVerticalScrollBarValue() {
        return verticalPosition;
    }

    public void setHorizontalScrollBarValue(double newValue) {
        horizontalPosition = newValue;
        invokedFromGUI_h =
                false;
        getHorizontalScrollBar().setValue((int) java.lang.Math.round(newValue));
    }

    public double getHorizontalScrollBarValue() {
        return horizontalPosition;
    }

    /**
     * Invoked when set internally by us or externally by the user
     * @param e
     */
    public void adjustmentValueChanged(AdjustmentEvent e) {

        if (e.getValueIsAdjusting()) {
            /* now we're sure that the adjustment took place from GUI
             */

            JScrollBar source = (JScrollBar) e.getAdjustable();
            int orient = source.getOrientation();
            if (orient == Adjustable.HORIZONTAL) {
                if (invokedFromGUI_h) {
                    horizontalPosition = e.getValue();
                } else {
                    invokedFromGUI_h = true;
                }

            }
            if (orient == Adjustable.VERTICAL) {
                if (invokedFromGUI_v) {
                    verticalPosition =
                            e.getValue();
                } else {
                    invokedFromGUI_v = true;
                }
            }
        }
    }
}