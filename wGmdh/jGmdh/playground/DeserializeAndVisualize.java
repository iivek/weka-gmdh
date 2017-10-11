/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wGmdh.jGmdh.playground;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.URL;
import wGmdh.jGmdh.exceptions.ExpressionEqualToZero;
import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.gui.GuiManager;
import wGmdh.jGmdh.oldskul.MultiSelectCombi;

/**
 *
 * @author vekk
 */
public class DeserializeAndVisualize {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args)
            throws IOException, ExpressionEqualToZero, TooBig, Exception {

        /*MultiSelectCombi msc = (MultiSelectCombi)
                weka.core.SerializationHelper.read(DeserializeAndVisualize.class.getResource(
                "/Resources/Ie1_6_20k.gmdh").toString());
         */
        ObjectInputStream ois = new ObjectInputStream(new URL(
                DeserializeAndVisualize.class.getResource(
                "/Resources/Ie1_6_20k.gmdh").toString()).openStream());
//        "/Resources/JTc1_6_20k.gmdh").toString()).openStream());
//                        "/Resources/CPm1_6_20k.gmdh").toString()).openStream());
        MultiSelectCombi msc = (MultiSelectCombi) ois.readObject();

        /* Graphical stuff
         */
        final GuiManager drawModel = new GuiManager(msc.selectedLayers,
                1000, 800, 50, 50);

        javax.swing.SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                drawModel.launchGUI(null);
            }
        });
    }
}
