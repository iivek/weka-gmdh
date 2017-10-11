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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.jdesktop.jxlayer.JXLayer;
import org.pbjar.jxlayer.plaf.ext.transform.DefaultTransformModel;

/**
 *
 * @author ivek
 */
abstract public class ZoomableOverlayPanel extends JPanel {// implements Scrollable

    private JXLayer<? extends JComponent> bottomLayer;
    private BufferedImage im;
    private DefaultTransformModel model;
//    private double lastRedrawnScaleX,  lastRedrawnScaleY;
    private Dimension lastRedrawnSize;
    private boolean dirty;  // if not redrawn after scaling

    public boolean getDirty() {
        return dirty;
    }

    public void setDirty(boolean b) {
        this.dirty = b;
    }

    /**
     *
     * @param original
     * @param maxZoom
     * @param type
     * @return
     */
    static public BufferedImage createZoomableImage(
            Dimension original,
            double maxZoom,
            int type) {
        int maxZ = (int) java.lang.Math.round(maxZoom);
        BufferedImage image = new BufferedImage(
                original.width * maxZ,
                original.height * maxZ,
                type);
        return image;
    }

    /**
     * In case you want to meddle with our stuff from the outside
     *
     * @return
     */
    public BufferedImage getBufferedImage() {
        return im;
    }

    public void setLastRedrawnSize(Dimension s) {
        lastRedrawnSize.setSize(s);
    }

    public void setLastRedrawnSize() {
        synchronized (bottomLayer.getTreeLock()) {
            lastRedrawnSize.setSize(bottomLayer.getPreferredSize());
        }
    }

    public Dimension getLastRedrawnSize() {
        return lastRedrawnSize;
    }

    /* Take care, this is not synced
     */
    public void setModel(DefaultTransformModel model) {
        synchronized (model) {
            this.model = model;
        }
    }

    public DefaultTransformModel getModel() {
        synchronized (model) {
            return model;
        }
    }

    abstract public void drawOnGraphics(Graphics2D g2);

    public Graphics2D optimizeResolutionAndClearVisible(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setBackground(new Color(0, 0, 0, 0));
        g2.clearRect(0, 0, lastRedrawnSize.width, lastRedrawnSize.height);

        /* We will sync model/affine related stuff with TransformUI's Treelock
         */
        synchronized (model) {
            /* Scaling only
             */
            g2.scale(model.getTransform(bottomLayer).getScaleX(),
                    model.getTransform(bottomLayer).getScaleY());
        }

        return g2;
    }

    /**
     * Synchronize it from the outside if needed
     * @return
     */
    public double getScaleX() {
        return model.getTransform(bottomLayer).getScaleX();
    }

    /**
     * Synchronize it from the outside if needed
     * @return
     */
    public double getScaleY() {
        return model.getTransform(bottomLayer).getScaleY();
    }

    /**
     *
     * @return  visible subimage size, at optimal resolution
     */
    public Dimension getSubimageSize() {
        synchronized (bottomLayer.getTreeLock()) {
            return bottomLayer.getPreferredSize();
        }

    }

    /**
     * Redraw image, so that it will have maximum resolution at current size
     */
    public void redrawImage() {
        setLastRedrawnSize();
        Graphics2D g2 = optimizeResolutionAndClearVisible(im.getGraphics());

        drawOnGraphics(g2);
    }

    /**
     *
     * @param dim
     * @param maxZoom
     * @param layer     its preffered size has to be set to get proper behavior
     * @param model
     */
    public ZoomableOverlayPanel(
            Dimension dim,
            double maxZoom,
            JXLayer<? extends JComponent> layer,
            DefaultTransformModel model) {
        setModel(model);
        this.bottomLayer = layer;
        /*        lastRedrawnScaleX = af.getScaleX();
        lastRedrawnScaleY = af.getScaleY();
         * */
        lastRedrawnSize = layer.getPreferredSize();

        im = createZoomableImage(dim, maxZoom, BufferedImage.BITMASK);

        Graphics2D g2 = (Graphics2D) im.getGraphics();

        setOpaque(false);
        setDoubleBuffered(true);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.DST_OUT, 0.8f));
        /* Translate g2 it to the center of the screen, so it will remain static
         * relative to bottomLayer, and scale it appropriately
         */
        synchronized (bottomLayer.getTreeLock()) {
            int translationX = (int) Math.round((bottomLayer.getWidth() -
                    bottomLayer.getPreferredSize().width) / 2);
            int translationY = (int) Math.round((bottomLayer.getHeight() -
                    bottomLayer.getPreferredSize().height) / 2);

            g2.translate(translationX, translationY);

            double scaleX = bottomLayer.getPreferredSize().getWidth() / lastRedrawnSize.width;
            double scaleY = bottomLayer.getPreferredSize().getHeight() / lastRedrawnSize.height;
            g2.scale(scaleX, scaleY);

            g2.drawImage(im.getSubimage(0, 0,
                    (int) Math.round(bottomLayer.getPreferredSize().width / scaleX),
                    (int) Math.round(bottomLayer.getPreferredSize().height / scaleY)),
                    0, 0, null);
        }
    }
}
