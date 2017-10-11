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

import wGmdh.jGmdh.exceptions.TooBig;
import wGmdh.jGmdh.gui.NodeGraphics.NodeButton;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jdesktop.jxlayer.JXLayer;
import org.pbjar.jxlayer.plaf.ext.TransformUI;
import org.pbjar.jxlayer.plaf.ext.transform.DefaultTransformModel;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;

/**
 * GMDH network visualization. Nodes are represented by JComponents, while
 * their links are only drawn on the JPanel. 3 additional worker threads are
 * used, one for highlighting and unhighlighting a node when mouse pointer
 * enters the region of the node, one that arms and disarms a node and one
 * used when a node is dragged to find its parents and children
 *
 * Note: no generic solutions are provided to i.e. zooming and connecting Nodes;
 * problems were solved as they came. Also there is no abstracion to
 * implement to get a graphical representation of GMDH. Make your own from scrap.
 *
 * @author ivek
 */
public class GuiManager {

    String infoPanelString = null;
    boolean continueThread = true;
    public int height,  width;     // JPanel dimensions
    public int borderX,  borderY;
    /* Layers of Nodes that, if they have NodeGraphics instantiated, will get
     * added to panel
     */
    public ArrayList<ArrayList<? extends Node>> layers;
    /* NodeGraphics that can be found on our JPanel. Otherwise we would have
     * search through this.layers for the ones that have existing NodeGraphics.
     * Used when evaluating cost function in simulated annealing, gets filled in
     * representModels
     */
    public ArrayList<ArrayList<NodeGraphics>> visible;
    protected JFrame frame;
    protected final NodebuttonPanel panel;
    protected SubpixelScrollPane scrollPane;
    protected JButton snapToGridJB;
    protected JButton sortByErrorJB;
    //Modified by JToggleButton's ItemListener
    boolean mousePressed;
    boolean zoomIsDirty = false;
    /* A queue of highlighted nodes. No need for syncing because it will be
     * modified on the highlighting thread exclusively
     */
    java.util.concurrent.ConcurrentLinkedQueue highlightedNodes;
    /* A queue that our arming runnable will work on
     */
    protected java.util.List<NodeGraphics> armedNodes;
    protected java.util.List<NodeGraphics> nodesToArm;
    protected java.util.List<NodeGraphics> nodesToUnarm;
    /* Index of armed node whose formula is currently displayed
     */
    protected int currentlyDisplayed = -1;
    /* This is used to force unhighlighting when entering the button for the
     * first time after cycling through armed nodes
     */
    boolean forceUnhighlightingBeforeNextHighlighting = false;

    /* A queue utilized to store the order of arm/disarm commands that are
     * issued from the EDT. These will get executed by armingExecutor.
     * true - arm, false - disarm.
     */
    java.util.Queue<UsefulButNonethelessStupidBoolWrapper> tasks;
    final protected Object armLock = new Object();
    final protected Object mouseOverLock = new Object();
    final protected Object dragLock = new Object();
    final protected Object zoomIsDirtyLock = new Object();
    boolean needsDrawing;   // what it needs is some recoding actually...
    Dimension lastRedrawnDimension;     /* Part of the armed links buffer that
     * is in use*/

    JXLayer uberLayer;
    JXLayer layer;
    DefaultTransformModel model;
    TransformUI ui;
    protected JLabel sliderLabel;
    protected ZoomSlider zoomSlider;
    protected JPanel controlPanel;
    protected JPanel infoPanel;
    protected JPanel infoButtonPanel;
    protected JEditorPane infoArea;
    protected JEditorPane instructionArea;
    protected JScrollPane instructionScrollPane;
    protected JScrollPane infoScrollPane;
    protected JEditorPane infoArea2;
    protected JScrollPane infoScrollPane2;
    protected JButton cycleButton;
    protected JButton namespaceButton;
    /*
     * We want fast and responsive highlighting/unhighlighting. To avoid
     * network traversal when unhighlighting, we will store the highlighted
     * buttons in this queue. (do we need a thread-safe queue?).
     */

    /* Worker thread relates stuff
     */
    ExecutorService highlightingExecutor;
    ExecutorService armingExecutor;
    ExecutorService firstDragExecutor;

    public GuiManager(ArrayList<ArrayList<? extends Node>> layers,
            int x, int y, int borderX, int borderY) {
        this.width = x;
        this.height = y;
        this.borderX = borderX;
        this.borderY = borderY;
        this.layers = layers;

        highlightedNodes = new java.util.concurrent.ConcurrentLinkedQueue();
        armedNodes = new java.util.LinkedList();
        nodesToArm = new java.util.LinkedList();
        nodesToUnarm = new java.util.LinkedList();
        tasks = new java.util.LinkedList();
        panel = new NodebuttonPanel(this);
        lastRedrawnDimension = new Dimension(x, y);

        mousePressed = false;
    }

    public void fullRepaint() {
        panel.repaint();
    }

    public ExecutorService getHighlightingExecutor() {
        return highlightingExecutor;
    }

    public ExecutorService getArmingExecutor() {
        return armingExecutor;
    }

    public ExecutorService getFirstDragExecutor() {
        return firstDragExecutor;
    }

    public Dimension getLastRedrawnDimension() {
        return lastRedrawnDimension;
    }

    public DefaultTransformModel getDefaultTransformModel() {
        return model;
    }

    public GuiManager(int x, int y, int borderX, int borderY) {
        this.width = x;
        this.height = y;
        this.borderX = borderX;
        this.borderY = borderY;

        highlightedNodes =
                new java.util.concurrent.ConcurrentLinkedQueue();
        armedNodes =
                new java.util.LinkedList();
        nodesToArm =
                new java.util.LinkedList();
        nodesToUnarm =
                new java.util.LinkedList();
        tasks =
                new java.util.LinkedList();
        panel =
                new NodebuttonPanel(this);
        lastRedrawnDimension =
                new Dimension(x, y);

        mousePressed =
                false;
    }

    /**
     * Iterates through layers, initializes their NodeGraphics and adds them to
     * JPanel.
     */
    protected void representAllModels() {
        /*
         * Wrap each Node in NodeGraphics and calculate their coordinates
         */
        int verticalDist, horizontalDist;
        verticalDist =
                (int) Math.round((height - 2 * borderY - 0.5 *
                NodeGraphics.buttonHeight) / (layers.size() - 1));
        for (int i = 1; i < layers.size(); ++i) {
            horizontalDist = (int) Math.round((width - 2 * borderX -
                    0.5 * NodeGraphics.buttonWidth) / (layers.get(i).size() - 1));

            for (int j = 0; j < layers.get(i).size(); ++j) {
                panel.addNodeAsButton(new NodeGraphics(layers.get(i).get(j),
                        width - borderX - NodeGraphics.buttonWidth - j * horizontalDist,
                        height - borderY - NodeGraphics.buttonHeight - i * verticalDist,
                        NodeGraphics.buttonWidth, NodeGraphics.buttonHeight), true);
            }

        }
    }

    protected void representAttributes() {

        // layers.get(0) is the attribute layer
        int horizontalDist = (int) Math.round((width - 2 * borderX -
                NodeGraphics.buttonWidth) / (layers.get(0).size() - 1));

        for (int j = 0; j <
                layers.get(0).size(); ++j) {
            panel.addNodeAsButton(new NodeGraphics(layers.get(0).get(j),
                    width - borderX - NodeGraphics.buttonWidth - j * horizontalDist,
                    height - borderY - NodeGraphics.buttonHeight,
                    NodeGraphics.buttonWidth, NodeGraphics.buttonHeight), true);

        }

    }

    /* Representing each and every Model as a JButton may require too much
     * memory, or be inapropriate for other reasons. So, it may be useful to
     * pass several Models for graphical representation, draw only them and
     * their submodels and disregard all others.
     */
    protected void representModels(List<? extends Model> toDraw) {

        /* initialize NodeGraphics to every submodel contained in toDraw.
         */
        Runtime r = Runtime.getRuntime();
//        System.out.println("available memory represent models: " + r.freeMemory());

        for (Iterator<Model> toDrawIter = (Iterator<Model>) toDraw.iterator();
                toDrawIter.hasNext();) {
            initializeModelGraphics(toDrawIter.next());
        }

//        System.out.println("memory after NGs: " + r.freeMemory());
        /* Fill this.visible with attributeNodes
         */
        visible =
                new ArrayList<ArrayList<NodeGraphics>>();
        ArrayList<NodeGraphics> helperLayer = new ArrayList<NodeGraphics>();
        for (int j = 0; j <
                layers.get(0).size(); ++j) {
            helperLayer.add(layers.get(0).get(j).graphics);
        }

        visible.add(helperLayer);

        int verticalDist = (int) Math.round((height - 2 * borderY - 0.5 *
                NodeGraphics.buttonHeight) / (layers.size() - 1));
        int totalCounter = 0;
        for (int i = 1; i <
                layers.size(); ++i) {

            int counter = 0;
            /*  traverse layer once and count the number of nodes to draw per
             *  each layer.
             */
            for (int j = 0; j <
                    layers.get(i).size(); ++j) {
                if (layers.get(i).get(j).graphics != null) {
                    ++counter;
                }

            }
            if (counter == 0) {
                break;
            }
            /* now we know where to put the nodes.
             */

            int horizontalDist = 0;
            horizontalDist =
                    (int) Math.round((width - 2 * borderX -
                    0.5 * NodeGraphics.buttonWidth) / ++counter);

            totalCounter +=
                    counter;
            /* traverse the layer once more and add nodes which have nodeGraphics
             * initialized to the panel.
             */
            counter =
                    0;
            helperLayer =
                    new ArrayList<NodeGraphics>();
            for (int j = 0; j <
                    layers.get(i).size(); ++j) {
                NodeGraphics ng = layers.get(i).get(j).graphics;
                if (ng != null) {
                    ng.button.setLocation(
                            width - borderX - NodeGraphics.buttonWidth - ++counter * horizontalDist,
                            height - borderY - NodeGraphics.buttonHeight - i * verticalDist);
                    ng.button.setSize(
                            NodeGraphics.buttonWidth, NodeGraphics.buttonHeight);
                    panel.addNodeAsButton(ng, true);
                    helperLayer.add(ng);
                }

            }
            /* Store visible nodes' NodeGraphics in layers
             */
            visible.add(helperLayer);
        }

    /* Now we have NodeGraphics' locations initialized. Next we relocate
     * them using simulated annealing.
     */
//       SimulatedAnnealingRelocation sa =
//               new SimulatedAnnealingRelocation(visible);
//sa.startSingleNode(visible, 1, 10, totalCounter, 100, 0.7);
//sa.startLayer(visible, 1, 10, totalCounter, 100, 0.7);


    }

    protected class SimulatedAnnealingRelocation {

        Random randomizer;
        ArrayList<ArrayList<NodeGraphics>> nodes;
        NeighboringConfigurationSingleNode neighborSingleNode;
        NeighboringConfigurationLayer neighborLayer;
        /* x-distance weight
         */
        double w1 = 3;
        /* weight penalty for closeness on single layer
         */
        double w2 = 5;
        /* weight for penalizing closeness to JPanel edges
         */
        double w3 = 10;
        int maxMove = 10;   // it could be made dependent on the temperature
        int maxTranslation = 10;
        int maxSpacingChange = 10;

        private SimulatedAnnealingRelocation() {
        }

        public SimulatedAnnealingRelocation(ArrayList<ArrayList<NodeGraphics>> nodes) {
            randomizer = new Random();
            this.nodes = nodes;
            neighborSingleNode = new NeighboringConfigurationSingleNode();
            neighborSingleNode.beforeMove = nodes;
            neighborLayer = new NeighboringConfigurationLayer();
            neighborLayer.beforeMove = nodes;
        }

        protected double penalizeNodeToNode(double weight, Point loc1, Point loc2) {

            double cost = weight * (1 / (Math.pow(loc1.x - loc2.x, 2) +
                    Math.pow(loc1.y - loc2.y, 2)));
            /* don't allow configurations with overlapping locations - put that
             * cost to +Inf
             */

            return cost;
        }

        /**
         * Computes cost difference of 2 neigboring configurations.
         *
         * @param nc
         * @return
         */
        protected double costDifference(NeighboringConfigurationSingleNode nc) {

            double costBeforeMove = 0;

            for (int j = 0; j < nc.layerOfMoved; ++j) {
                for (int i = 0; i < nodes.get(j).size(); ++i) {
                    costBeforeMove += penalizeNodeToNode(w1,
                            nc.movedNode.button.getLocation(),
                            nodes.get(j).get(i).getButton().getLocation());
                }
            }
            for (int i = 0; i < nc.indexOfMoved; ++i) {
                costBeforeMove += penalizeNodeToNode(w1,
                        nc.movedNode.button.getLocation(),
                        nodes.get(nc.layerOfMoved).get(i).getButton().getLocation());
            }
            for (int i = nc.indexOfMoved + 1; i < nodes.get(nc.layerOfMoved).size(); ++i) {
                costBeforeMove += penalizeNodeToNode(w1,
                        nc.movedNode.button.getLocation(),
                        nodes.get(nc.layerOfMoved).get(i).getButton().getLocation());
            }
            for (int j = nc.layerOfMoved + 1; j < nc.beforeMove.size(); ++j) {
                for (int i = 0; i < nodes.get(j).size(); ++i) {
                    costBeforeMove += penalizeNodeToNode(w1,
                            nc.movedNode.button.getLocation(),
                            nodes.get(j).get(i).getButton().getLocation());
                }
            }

            /* Penalize closeness to the edge of JPanel
             */
            int edgex1 = 0 + borderX;
            int edgex2 = width - borderX;
            int ourLocationX = nc.movedNode.button.getLocation().x;
            if (ourLocationX < edgex1 || ourLocationX > edgex2) {
                // just in case... although, if initialized properly, we should
                // never be here. or, in case that initialization is in the
                // singularity zone, well designed annealing should get us out
                // of it. so it's not actually neccessary
                costBeforeMove = Double.POSITIVE_INFINITY;
            } else {
                costBeforeMove += w3 * (1 / Math.pow(ourLocationX - edgex1, 2) +
                        1 / Math.pow(ourLocationX - edgex2, 2));
            }

            ourLocationX = nc.movedNode.button.getLocation().x + nc.move;
            Point ourLocation = new Point(
                    ourLocationX, nc.movedNode.button.getLocation().y);
            double costAfterMove = 0;

            /* Pair-to-pair distance penalization
             */
            for (int j = 0; j < nc.layerOfMoved; ++j) {
                for (int i = 0; i < nodes.get(j).size(); ++i) {
                    costAfterMove += penalizeNodeToNode(w1,
                            ourLocation,
                            nodes.get(j).get(i).getButton().getLocation());
                }
            }
            for (int i = 0; i < nc.indexOfMoved; ++i) {
                costAfterMove += penalizeNodeToNode(w1,
                        ourLocation,
                        nodes.get(nc.layerOfMoved).get(i).getButton().getLocation());
            }
            for (int i = nc.indexOfMoved + 1; i < nodes.get(nc.layerOfMoved).size(); ++i) {
                costAfterMove += penalizeNodeToNode(w1,
                        ourLocation,
                        nodes.get(nc.layerOfMoved).get(i).getButton().getLocation());
            }
            for (int j = nc.layerOfMoved + 1; j < nc.beforeMove.size(); ++j) {
                for (int i = 0; i < nodes.get(j).size(); ++i) {
                    costAfterMove += penalizeNodeToNode(w1,
                            ourLocation,
                            nodes.get(j).get(i).getButton().getLocation());
                }
            }

            /* Penalize closeness to the edge of JPanel. Put singularities
             * there - make the cost function go to infinity there.
             */
            if (ourLocationX < edgex1 || ourLocationX > edgex2) {
                costAfterMove = Double.POSITIVE_INFINITY;
            } else {
                costAfterMove += w3 * (1 / Math.pow(ourLocationX - edgex1, 2) +
                        1 / Math.pow(ourLocationX - edgex2, 2));
            }
            /* Too complex to calculate vertex crossings and try to penalize
             * them. Vertex lengths also. So that's about it.
             */

            return costAfterMove - costBeforeMove;
        }

        /**
         * Computes cost difference of 2 neigboring configurations.
         *
         * @param nc
         * @return
         */
        protected double costDifference(NeighboringConfigurationLayer nc) {

            ArrayList<NodeGraphics> ngs = nodes.get(nc.changedLayer);

            double costBefore = 0;
            double costAfter = 0;

            /* Penalize closeness to the edge of JPanel
             */
            int edgex1 = 0 + borderX;
            int edgex2 = width - borderX;

            /* We insist that spacing is > 0.
             */
            int spacing = 0;
            int spacingAfter = 0;
            if (ngs.size() > 1) {
                spacing = nc.getSpacing();
                spacingAfter = spacing + nc.spacingChange;
                if (spacing == 0) {
                    costBefore = Double.POSITIVE_INFINITY;
                    // hamper change if we're packed
                    return Double.POSITIVE_INFINITY;
                }
                if (spacingAfter == 0) {
                    costAfter = Double.POSITIVE_INFINITY;
                    return Double.POSITIVE_INFINITY;
                }
            }
            int startx = ngs.get(ngs.size() - 1).button.getLocation().x;
            int endx = startx + (ngs.size() - 1) * spacing;
            int startxAfter = startx + nc.translation;
            int endxAfter = startxAfter + (ngs.size() - 1) * spacingAfter;

            if (startx <= edgex1 || startx >= edgex2 ||
                    endx <= edgex1 || endx >= edgex2) {
                // if initialized properly we should never be here. if not, we
                // may even be stuck here forever (depending on the magnitude of
                // translation and spacing)
                costBefore = Double.POSITIVE_INFINITY;
            } else {
                for (int i = 0, ourLocationX = startx;
                        i < ngs.size(); ++i) {
                    costBefore += w3 * (1 / Math.pow(ourLocationX - edgex1, 2) +
                            1 / Math.pow(ourLocationX - edgex2, 2));
                    ourLocationX += spacing;
                }
            }
            if (startxAfter <= edgex1 || startxAfter >= edgex2 ||
                    endxAfter <= edgex1 || endxAfter >= edgex2) {
                costAfter = Double.POSITIVE_INFINITY;
                return Double.POSITIVE_INFINITY;
            } else {
                for (int i = 0, ourLocationX = startxAfter;
                        i < ngs.size(); ++i) {
                    costAfter += w3 * (1 / Math.pow(ourLocationX - edgex1, 2) +
                            1 / Math.pow(ourLocationX - edgex2, 2));
                    ourLocationX += spacingAfter;
                }
            }
            Point location1After = ngs.get(ngs.size() - 1).button.getLocation();
            location1After.x += nc.translation;
            for (int k = ngs.size() - 1; k >= 0; --k) {
                Point location1 = ngs.get(k).button.getLocation();

                for (int j = 0; j < nc.changedLayer; ++j) {
                    for (int i = 0; i < nodes.get(j).size(); ++i) {
                        costBefore += penalizeNodeToNode(w1,
                                location1,
                                nodes.get(j).get(i).getButton().getLocation());
                        costAfter += penalizeNodeToNode(w1,
                                location1After,
                                nodes.get(j).get(i).getButton().getLocation());
                    }
                }
                for (int j = nc.changedLayer + 1; j < nodes.size(); ++j) {
                    for (int i = 0; i < nodes.get(j).size(); ++i) {
                        costBefore += penalizeNodeToNode(w1,
                                location1,
                                nodes.get(j).get(i).getButton().getLocation());
                        costAfter += penalizeNodeToNode(w1,
                                location1After,
                                nodes.get(j).get(i).getButton().getLocation());
                    }
                }
                location1After.x += spacingAfter;
            }
            /* ...and between nodes on changed layer. all this can be simplified
             * i guess.
             */
            Point location2After = ngs.get(ngs.size() - 1).button.getLocation();
            for (int j = 0; j < ngs.size(); ++j) {
                location1After.x = ngs.get(ngs.size() - 1).button.getLocation().x +
                        nc.translation + j * spacingAfter;
                for (int i = j + 1; i < ngs.size(); ++i) {
                    location2After.x = ngs.get(ngs.size() - 1).button.getLocation().x +
                            nc.translation + i * spacingAfter;
                    costBefore += penalizeNodeToNode(w2,
                            ngs.get(i).button.getLocation(),
                            ngs.get(j).button.getLocation());
                    costAfter += penalizeNodeToNode(w2,
                            location1After,
                            location2After);
                }
            }

            Double result = costAfter - costBefore;
            /* If both neighbors' costs are +Inf, hamper change.
             */
            /*        if (result == Double.NaN) {
            result = Double.POSITIVE_INFINITY;
            }
             */
            return result;
        }

        protected void startSingleNode(
                ArrayList<ArrayList<NodeGraphics>> config,
                double initialTemp,
                int problemSizeMultiplier, int problemSize,
                int coolingsteps, double temperatureMultiplier) {

            double temp = initialTemp;
            while (--coolingsteps >= 0) {
                int nrPasses = problemSizeMultiplier * problemSize;
                while (--nrPasses >= 0) {
                    generateNeighbor(config, maxMove, neighborSingleNode);
                    double difference = costDifference(neighborSingleNode);
                    if (difference < 0) {
                        acceptNeighbor(neighborSingleNode);
                    } else if (Math.exp(-difference / temp) > randomizer.nextDouble()) {
                        /* Note: you can invert the sign of difference to
                         * have one operation less.
                         * > is chosen so configurations with +Inf never get
                         * accepted.
                         */
                        acceptNeighbor(neighborSingleNode);
                    }
                }
                /* Geometric cooling scheme
                 */
                temp *= temperatureMultiplier;
            }
        }

        protected void startLayer(
                ArrayList<ArrayList<NodeGraphics>> config,
                double initialTemp,
                int problemSizeMultiplier, int problemSize,
                int coolingsteps, double temperatureMultiplier) {

            double temp = initialTemp;
            while (--coolingsteps >= 0) {
                int nrPasses = problemSizeMultiplier * problemSize;
                while (--nrPasses >= 0) {
                    generateNeighbor(config, maxTranslation, maxSpacingChange, neighborLayer);
                    double difference = costDifference(neighborLayer);
                    if (difference < 0) {
                        acceptNeighbor(neighborLayer);
                    } else if (Math.exp(-difference / temp) > randomizer.nextDouble()) {
                        /* Note: you can invert the sign of difference everywhere
                         * to have one operation less.
                         * > is chosen so configurations with +Inf never get
                         * accepted.
                         */
                        acceptNeighbor(neighborLayer);
                    }
                }
                /* Geometric cooling scheme
                 */
                temp *= temperatureMultiplier;
            }
        }

        protected class NeighboringConfigurationSingleNode {

            ArrayList<ArrayList<NodeGraphics>> beforeMove;
            NodeGraphics movedNode;
            int layerOfMoved;
            int indexOfMoved;
            int move;
            double costBeforeMove;
            double costAfterMove;
        }

        /* Keep distances between nodes on the same layer same and constant
         */
        protected class NeighboringConfigurationLayer {

            ArrayList<ArrayList<NodeGraphics>> beforeMove;
            int changedLayer;
            int translation;
            int spacingChange;
            double costBeforeMove;
            double costAfterMove;

            protected int getOrigin() {
                ArrayList<NodeGraphics> us = beforeMove.get(changedLayer);
                return us.get(us.size() - 1).button.getLocation().x;
            }

            protected int getSpacing() {
                if (beforeMove.get(changedLayer).size() == 1) {
                    return -1;
                }
                return beforeMove.get(changedLayer).get(0).button.getLocation().x -
                        beforeMove.get(changedLayer).get(1).button.getLocation().x;
            }
        }

        /**
         *
         * @param config    input configuration. will get modified by the method.
         * @param maxMove   upper bound for the random movement
         * @param newConfig results get returned in this parameter
         */
        protected void generateNeighbor(
                ArrayList<ArrayList<NodeGraphics>> config, int maxMove,
                NeighboringConfigurationSingleNode newConfig) {

            /* Randomize movement.
             */
            int move = randomizer.nextInt(2 * maxMove + 1) - maxMove;

            /* Randomly obtain the node to be relocated. All have equal chances.
             */
            int total = 0;
            ArrayList<Double> boundaries = new ArrayList<Double>();
            for (int j1 = 0; j1 < config.size(); ++j1) {
                total += config.get(j1).size();
                boundaries.add(new Double(config.get(j1).size()));
            }
            double untilNow = 0;
            for (int j1 = 0; j1 < boundaries.size(); ++j1) {
                untilNow += boundaries.get(j1) / total;
                boundaries.set(j1, untilNow);
            }
            double event = randomizer.nextFloat();
            ArrayList<NodeGraphics> selectedLayer = null;
            for (int j1 = 0; j1 < boundaries.size(); ++j1) {
                if (event <= boundaries.get(j1)) {
                    newConfig.layerOfMoved = j1;
                    selectedLayer = config.get(j1);
                    break;
                }
            }
            newConfig.beforeMove = config;
            newConfig.move = move;
            newConfig.indexOfMoved = randomizer.nextInt(selectedLayer.size());
            newConfig.movedNode =
                    selectedLayer.get(newConfig.indexOfMoved);
        }

        /**
         *
         * @param config    input configuration. will get modified by the method.
         * @param maxMove   upper bound for the random movement
         * @param newConfig results get returned in this parameter
         */
        protected void generateNeighbor(
                ArrayList<ArrayList<NodeGraphics>> config,
                int maxTranslation, int maxSpacingChange,
                NeighboringConfigurationLayer newConfig) {

            /* translate and stretch/shrink at the same time
             */
            newConfig.beforeMove = config;
            newConfig.translation = randomizer.nextInt(2 * maxMove + 1) - maxMove;
            newConfig.changedLayer = randomizer.nextInt(config.size());
            newConfig.spacingChange = randomizer.nextInt(2 * maxSpacingChange + 1) - maxSpacingChange;
        }

        /**
         * Accepts modification contained in nc
         *
         * @param nc
         */
        protected void acceptNeighbor(NeighboringConfigurationSingleNode nc) {

            NodeButton button = nc.movedNode.button;
            Point old = button.getLocation();
            button.setLocation(old.x + nc.move, old.y);
        }

        /**
         * Accepts modification contained in nc
         *
         * @param nc
         */
        protected void acceptNeighbor(NeighboringConfigurationLayer nc) {

            ArrayList<NodeGraphics> us = nodes.get(nc.changedLayer);
            Point lastLocation = us.get(us.size() - 1).button.getLocation();
            lastLocation.x += nc.translation;
            int newSpacing = nc.getSpacing() + nc.spacingChange;
            /*            System.out.println("accepting change " + nc.spacingChange + " " +
            nc.translation);
             */ for (int i = 0; i < us.size(); ++i) {
                /*                System.out.println("    old x: " +
                us.get(us.size()-1-i).button.getLocation() +
                " new x: " + lastLocation);
                 */ us.get(us.size() - 1 - i).button.setLocation(lastLocation);
                lastLocation.x += newSpacing;
            }
        }
    }
    /**
     * Preorder traversal. On visit initialize NodeGraphics, if not initialized.
     *
     * Recursion is not the smartest thing to do here. It can be very deep so
     * it can take alot of stack space, especially with the not so small
     * objects used.
     */
    public int recursionCount = 0;

    protected void initializeModelGraphics(Model model) {

        //System.out.println(++recursionCount);

        if (model.graphics != null) {
            return;
        }

        new NodeGraphics(model);

        for (Iterator<Node> iter = model.links.iterator(); iter.hasNext();) {
            Node visited = iter.next();
            if (visited instanceof Model) {
                initializeModelGraphics((Model) visited);
            }

        }
    }

    /*    protected void initializeModelGraphics(Model root) {

    Stack toExamine = new Stack();
    toExamine.push(root);

    root.graphics = new NodeGraphics(root);
    while (!toExamine.isEmpty()) {
    Model visited = (Model) toExamine.pop();
    int size = visited.links.size();

    for (int i = 0; i < size; ++i) {
    Node child = (Node) visited.links.get(i);
    if (child instanceof Model) {
    child.graphics = new NodeGraphics(child);
    toExamine.push(child);
    }
    }
    }
    }
     */
  /*  protected void initializeModelGraphics(Model root) {

        java.util.Queue toExamine = new java.util.LinkedList();
        toExamine.add(root);

        root.graphics = new NodeGraphics(root);
        while (!toExamine.isEmpty()) {
            Model visited = (Model) toExamine.poll();
            int size = visited.links.size();

            for (int i = 0; i < size; ++i) {
                Node child = (Node) visited.links.get(i);
                if (child instanceof Model) {
                    child.graphics = new NodeGraphics(child);
                    toExamine.add(child);
                }
            }
        }
    }
*/
    public void SnapToGrid() {
    }

    public void SortByError() {
    }

    /**
     * Zooming stuffz
     */
    final protected class ZoomableLinks extends ZoomableOverlayPanel {

        protected BufferedImage offscreenArmed;

        ZoomableLinks(
                Dimension dim,
                double maxZoom,
                JXLayer<? extends JComponent> layer,
                DefaultTransformModel m) {
            super(dim, maxZoom, layer, m);
            offscreenArmed = ZoomableOverlayPanel.createZoomableImage(dim,
                    ZOOM_MAX / ZOOM_SCALE, BufferedImage.BITMASK);
        }

        @Override
        public void drawOnGraphics(Graphics2D g2) {
            return;
        }

        public BufferedImage getOffscreenBuffer() {
            return offscreenArmed;
        }
    }
    final int ZOOM_MIN = 500;
    final int ZOOM_MAX = 3000;
    final int ZOOM_INIT = 1000;
    final double ZOOM_SCALE = 1000;
    protected ZoomableLinks overlay;
    double globalScale;

    public ZoomableLinks getOverlay() {
        return overlay;
    }

    public final class ZoomSlider extends JSlider {

        protected int lastValue;

        ZoomSlider() {
            super();
            lastValue = 1;
        }

        ZoomSlider(int orientation, int min, int max, int init) {
            super(orientation, min, max, init);
            lastValue = init;
        }

        public int getLast() {
            return lastValue;
        }

        public void setLast(int newLast) {
            lastValue = newLast;
        }
    }

    protected Graphics2D redrawNative(
            boolean repaints) {
        /*
         * Refill the buffers in order to have optimal resoultion at the
         * current zoom factor:
         */

        /* 1. redraw links in armedBuffer...
         */
        Dimension dim = overlay.getSubimageSize();
        overlay.setLastRedrawnSize();
        Graphics2D out;

        //System.out.println("    before redrawnative overlay");
        synchronized (overlay) {
            //System.out.println("    redrawnative overlay");
            Graphics2D armedBuffer = overlay.optimizeResolutionAndClearVisible(
                    overlay.offscreenArmed.getGraphics());
            armedBuffer.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            armedBuffer.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            /* ...and store current zoom factor
             */
            lastRedrawnDimension.setSize(dim);

            panel.relinkArmed(armedBuffer);

            /* 2. copy armedBuffer to main overlay buffer and redraw links
             *    of currently higlighted model
             */
            Graphics2D fullBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();
            fullBuffer.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            fullBuffer.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            fullBuffer.setBackground(new Color(0, 0, 0, 0));
            fullBuffer.clearRect(0, 0, dim.width, dim.height);
            fullBuffer.drawImage(
                    overlay.offscreenArmed.getSubimage(
                    0, 0, dim.width, dim.height),
                    0, 0, null);
            out =
                    fullBuffer;
            /* And redraw the highlighted links...
             * in case they got erased in the process.
             */
            synchronized (model) {
                fullBuffer.scale(overlay.getScaleX(), overlay.getScaleY());
            }

            panel.relinkHighlighted(fullBuffer);
        }

        //System.out.println("    after redrawnative overlay");
        if (repaints) {
            overlay.repaint();
        }

        return out;

    }

    public void onFrameClosing() {
        highlightingExecutor.shutdown();
        armingExecutor.shutdown();
        firstDragExecutor.shutdown();
    }

    /**
     * Sets everything up for display: initializes and displays the
     * layer of attributes and all models listed in modelsToAdd
     *
     * @param modelsToAdd   if zero, displays all models listed in this.layers
     */
    public void launchGUI(List<Model> modelsToAdd) {

        //System.out.println("launch GUI");

        /* thread executor(s)
         */
        highlightingExecutor = Executors.newSingleThreadExecutor();
        armingExecutor =
                Executors.newSingleThreadExecutor();
        firstDragExecutor =
                Executors.newSingleThreadExecutor();

        frame =
                new JFrame();
        frame.setTitle("GMDH Network");
        //this.setIconImage(Toolkit.getDefaultToolkit().getImage("sth.gif"));
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {

            /*Signals closing to the worker threads
             */
            @Override
            public void windowClosing(WindowEvent e) {
                onFrameClosing();
            }
        });

        //System.out.println(width + "," + height);
        panel.setPreferredSize(new Dimension(width, height));
        panel.setLayout(null);
        panel.setDoubleBuffered(true);

        //scrollPane.setLayout(BorderLayout.CENTER);
//        snapToGridJB = new JButton("SnapToGrid");
//        snapToGridJB.setBounds(100, 100, 200, 50);
//        frame.getContentPane().add(snapToGridJB);
//        sortByErrorJB = new JButton("SortByError");
//        sortByErrorJB.setBounds(500, 100, 200, 50);
//        frame.getContentPane().add(sortByErrorJB);
        panel.bufferDimension = new Dimension();
        panel.bufferDimension.height = height;
        panel.bufferDimension.width = width;
        /*
         * Add Node representations
         */
        representAttributes();
//ArrayList<Model> l = new ArrayList<Model>();
//l.add((Model) layers.get(19).get(20));
//l.add((Model) layers.get(20).get(10));
//l.add((Model) layers.get(18).get(19));

        if (modelsToAdd == null) {
            representAllModels();
        } else {
            //System.out.println("stepping into represent models");
            representModels(modelsToAdd);
        }

        /*
         * Note: We could combine (upper) JButton drawing and NodeGraphics
         * wrapping (in constructor) in the same for loop
         *
         */
        {
            /*************************************
             *      Playground
             */
            /*            JButton jButton1 = new JButton();
            jButton1.setText("jButton1");
            jButton1.setBounds(139, 185, 99, 34);
            
            JColorChooser colorChooser = new JColorChooser();
             */

            /*
             * Zoomable panel
             */
            globalScale = ZOOM_INIT / ZOOM_SCALE;

            model = new DefaultTransformModel();
            ui = new TransformUI(model);
            layer = new JXLayer<JComponent>(panel, ui);

            layer.setOpaque(true);
            model.setPreserveAspectRatio(true);
            model.setScale(globalScale);
            model.setScaleToPreferredSize(true);

            uberLayer =
                    new JXLayer<JComponent>(layer);
            overlay =
                    new ZoomableLinks(
                    new Dimension(width, height),
                    ZOOM_MAX / ZOOM_SCALE,
                    (JXLayer<JComponent>) uberLayer,
                    model);
            uberLayer.setGlassPane(overlay);
            panel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLoweredBevelBorder(),
                    BorderFactory.createEmptyBorder(10, 10, 10, 10)));

            /*
             * ScrollPane
             */
            scrollPane =
                    new SubpixelScrollPane(uberLayer);
            /*           scrollPane.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLoweredBevelBorder(),
            BorderFactory.createEmptyBorder(10, 10, 10, 10)));
             */
            /*
             * Zoom control slider
             */
            //Create the label.
            sliderLabel =
                    new JLabel("Zoom", JLabel.CENTER);
            sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

            //Create the slider.
            zoomSlider =
                    new ZoomSlider(JSlider.VERTICAL,
                    ZOOM_MIN, ZOOM_MAX, ZOOM_INIT);
            /* TODO: A new model class that can output doubles and a new JComponent
             * that will know how to use that custom model. Then make a
             * custom contiguous JSlider
             */

            //Turn on labels at major tick marks.
            //zoomSlider.setMajorTickSpacing(4);
            //zoomSlider.setMinorTickSpacing(1);
            zoomSlider.setPaintTicks(true);
            zoomSlider.setPaintLabels(false);
            //zoomSlider.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
            zoomSlider.addChangeListener(new ChangeListener() {

                /* Listen to the slider
                 */
                public void stateChanged(ChangeEvent e) {

                    /* Here we deal with zoom centering
                     */
                    ZoomSlider source = (ZoomSlider) e.getSource();

                    int value = source.getValue();
//                    System.out.println("value =" + value + "oldValue = " + source.getLast());
                    globalScale =
                            value / ZOOM_SCALE;
                    synchronized (model) {
                        model.setScale(globalScale);
                    }

                    int w = scrollPane.getViewport().getWidth();
                    int h = scrollPane.getViewport().getHeight();

                    double posx = scrollPane.getHorizontalScrollBarValue();
                    double posy = scrollPane.getVerticalScrollBarValue();

                    int imageWidth = layer.getWidth();
                    int imageHeight = layer.getHeight();

                    /* Check if scroll bars are visible (we are using the
                     * ..._AS_NEEDED policy)
                     */
                    if (!(w == imageWidth && h == imageHeight)) {
                        double ratio = ((double) value) / source.getLast();
                        // source.getLast() is never 0
                        double newW = w * ratio;
                        double newH = h * ratio;

                        double newx, newy;
                        newx =
                                posx * ratio + (newW - w) / 2;
                        newy =
                                posy * ratio + (newH - h) / 2;

                        scrollPane.setHorizontalScrollBarValue(newx);
                        scrollPane.setVerticalScrollBarValue(newy);

                    } else {
                        /* Scroll bars are invisible now so we reset their subpixel
                         * positions
                         */
                        scrollPane.setHorizontalScrollBarValue(0);
                        scrollPane.setVerticalScrollBarValue(0);
                    }

                    source.setLast(value);

                    /* User has altered the zoom factor and now lines that
                     * represent Nodes' links are stretched/shrinked to fit the
                     * new resolution. We want those links to be redrawn in
                     * native resolution so we mark the flag from here and then
                     * redraw the links from the arming thread ASAP.
                     */
                    if (!source.getValueIsAdjusting()) {
                        /* We wait until slider is adjusted
                         */
                        synchronized (zoomIsDirtyLock) {
                            zoomIsDirty = true;
                            executeArmingTask();

                        }


                    }
                }
            });

            /*
             * Control panel
             */
            controlPanel =
                    new JPanel();
            controlPanel.add(sliderLabel);
            controlPanel.add(zoomSlider);

            JButton button2 = new JButton("Force redraw");
            button2.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    redrawNative(false);
                    panel.repaint();
                }
            });

//            controlPanel.add(button2);

            /* Add textbox
             */
            infoPanel =
                    new JPanel();
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
            infoPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            //           JLabel selectedLabel = new JLabel("Selected: ", JLabel.CENTER);
            //           JLabel floatingLabel = new JLabel("Floating over: ", JLabel.CENTER);
            sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            infoButtonPanel =
                    new JPanel();
            infoButtonPanel.setLayout(new BoxLayout(infoButtonPanel, BoxLayout.LINE_AXIS));
            infoArea =
                    new JEditorPane();
            infoArea.setContentType("text/html");
            instructionArea =
                    new JEditorPane();
            instructionArea.setContentType("text/html");

            /*infoArea.setColumns(20);
            infoArea.setLineWrap(true);
            infoArea.setRows(20);
            infoArea.setWrapStyleWord(true);
             */
            infoArea.setEditable(false);

            infoScrollPane =
                    new JScrollPane(infoArea);
            instructionScrollPane =
                    new JScrollPane(instructionArea);

            /* SSE and coeffs
             */
            infoArea2 =
                    new JEditorPane();
            infoArea2.setContentType("text/html");
            /*   infoArea2.setColumns(20);
            infoArea2.setLineWrap(true);
            infoArea2.setRows(5);
            infoArea2.setWrapStyleWord(true);
             * */
            infoArea2.setEditable(false);
            infoScrollPane2 =
                    new JScrollPane(infoArea2);

            //Dimension preferredSize = component.getPreferredSize();
            //component.setPreferredSize( new Dimension( width, preferredSize.height )

            cycleButton =
                    new JButton("Cycle");
            cycleButton.addActionListener(new ActionListener() {

                int lastDisplayedIndex = 0;

                /**
                 * Cycles through armed nodes and highlights them
                 */
                public void actionPerformed(ActionEvent e) {
                    synchronized (armLock) {
                        if (armedNodes.size() != 0) {
                            NodeGraphics current =
                                    armedNodes.get(lastDisplayedIndex++);
                            /* Highlight the armed node
                             */
                            /*synchronized (mouseOverLock) {
                            panel.mouseOver = current;
                            }*/
                            forceUnhighlightingBeforeNextHighlighting =
                                    true;
                            current.onMouseEntered(
                                    forceUnhighlightingBeforeNextHighlighting);

                            if (lastDisplayedIndex >= armedNodes.size()) {
                                lastDisplayedIndex = 0;
                            }

                        } else {
                            lastDisplayedIndex = 0;
                        }

                    }

                }
            });
            /* TODO: a local namespace, that starts counting models from 1
             * instead of using uniqueIDs
             */
            /*            namespaceButton = new JButton("Global");
            namespaceButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {

            }
            });
             *             infoButtonPanel.add(namespaceButton);
             */

//            infoPanel.add(selectedLabel);
            infoPanel.add(infoScrollPane);
            infoPanel.add(Box.createRigidArea(new Dimension(0, 10)));
            infoPanel.add(infoButtonPanel);
            infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.X_AXIS));

            infoButtonPanel.add(cycleButton);
            infoButtonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
            infoButtonPanel.setLayout(new BoxLayout(infoButtonPanel, BoxLayout.Y_AXIS));

//            infoPanel.add(floatingLabel);
            controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            controlPanel.add(infoScrollPane2);
            controlPanel.add(Box.createRigidArea(new Dimension(0, 20)));
            controlPanel.add(instructionScrollPane);
            controlPanel.setLayout(new BoxLayout(controlPanel, BoxLayout.Y_AXIS));
            controlPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

            /* Resize JEditorPanes
             */
            controlPanel.setPreferredSize(new Dimension(
                    160, scrollPane.getPreferredSize().height));
            Dimension max = infoPanel.getPreferredSize();
            infoScrollPane2.setPreferredSize(new Dimension(
                    controlPanel.getPreferredSize().width, 123));
            infoScrollPane2.setMaximumSize(new Dimension(
                    controlPanel.getMaximumSize().width, 123));
            infoScrollPane.setPreferredSize(new Dimension(
                    infoPanel.getPreferredSize().width, 123));
            infoScrollPane.setMaximumSize(new Dimension(
                    infoPanel.getMaximumSize().width, 123));
            /*            scrollPane.addMouseListener(new MouseListener() {
            
            public void onMouseClick(MouseEvent e) {
            }
            
            public void mousePressed(MouseEvent e) {
            }
            
            public void mouseReleased(MouseEvent e) {
            }
            
            public void mouseEntered(MouseEvent e) {
            
            //      if(scrollPane.getMousePosition(true) == null) return;
            //   e.consume();
            
            }
            
            public void mouseExited(MouseEvent e) {
            if (scrollPane.getMousePosition(true) != null) {
            return;
            }
            if (!highlightedNodes.isEmpty()) {
            ((NodeGraphics) highlightedNodes.peek()).onMouseExit();
            }
            //e.consume();
            }
            });
             */
            /* Add components to frame
             */
            frame.add(scrollPane, BorderLayout.CENTER);
            frame.add(controlPanel, BorderLayout.EAST);
            frame.add(infoPanel, BorderLayout.SOUTH);
//            frame.setLayout(new BoxLayout(frame, BoxLayout.LINE_AXIS));
//            frame.getContentPane().add(jButton1);
            //frame.getContentPane().add(colorChooser);
            frame.pack();
            frame.setVisible(true);
        }




    }

    class WhenDraggedRunnable implements Runnable {

        public void run() {
            //System.out.println("before whendraggedrunnable draglock taken");
            synchronized (dragLock) {
                //System.out.println("whendraggedrunnable draglock taken");
                /* Clear armed links
                 */
                Dimension dim = overlay.getSubimageSize();
                overlay.setLastRedrawnSize();
                //System.out.println("    before WhenDraggedRunnable syncs with overlay");
                synchronized (overlay) {
                    //System.out.println("    WhenDraggedRunnable syncs with overlay");
                    Graphics2D armedBuffer = overlay.optimizeResolutionAndClearVisible(
                            overlay.offscreenArmed.getGraphics());
                    armedBuffer.setRenderingHint(
                            RenderingHints.KEY_ALPHA_INTERPOLATION,
                            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                    armedBuffer.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    Graphics2D fullBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();

                    /* Iterate through dragged NodeGraphics
                     */

                    for (Iterator<NodeGraphics> it = panel.dragged.iterator(); it.hasNext();) {

                        NodeGraphics dragged = it.next();

                        for (int iter = 0; iter < armedNodes.size(); ++iter) {
                            panel.whenDraggedIterative(
                                    armedBuffer, armedNodes.get(iter), dragged);
                        }

                        /* At this point we have a list of dragged's parents, if any. We still
                         * have to fill the list with dragged's children
                         */
                        if (dragged.node instanceof Model) {
                            Iterator<Node> iter = ((Model) dragged.node).links.iterator();
                            while (iter.hasNext()) {
//                                System.out.println("adding children");
                                panel.childrenLinkedToDragged.add(iter.next().graphics);
                            }
                        }// else dragged.node has no children - it is an AttributeNode
                    }
                }
                //System.out.println("    after WhenDraggedRunnable syncs with overlay");
                //System.out.println("whendraggedrunnable draglock left");

            }
        }
    }

    /**
     * This is also made to be queued and called from the firstDragExecutor
     */
    private void resetFirstDragState() {

        NodebuttonPanel parent = this.panel;

        //System.out.println("before resetfirstdragstate taken");
        synchronized (dragLock) {
            //System.out.println("resetfirstdragstate taken");
            /* Resetting firstDrag state
             */
            parent.firstDrag = true;
            /* Draw links to/from NodeGraphics this on
             * panel.offscreenArmed
             */
            if (parent.dragged != null) {
                for (Iterator<NodeGraphics> it = parent.dragged.iterator(); it.hasNext();) {
                    NodeGraphics dragged = it.next();
                    drawLinksWhenDragged(dragged);
                }

                needsDrawing = false;
            }

            parent.parentsLinkedToDragged.clear();
            parent.childrenLinkedToDragged.clear();
            parent.dragged.clear();

        //System.out.println("resetting first drag");
            /* We have now finished the procedure related
         * to dragging
         */
        }

        //System.out.println("resetfirstdragstate left");

    }

    class AfterdragRunnable implements Runnable {

        public void run() {
            resetFirstDragState();
        }
    }

    protected void executeAfterdragRunnable() {
        firstDragExecutor.execute(new AfterdragRunnable());
    }

    class HighlightRunnable implements Runnable {

        NodeGraphics root;

        HighlightRunnable(NodeGraphics highlight) {
            root = highlight;
        }

        /**
         * Synchronized internally
         */
        public void run() {

            /* Copy the offscreenArmed to overlay.
             */
            Dimension dim = overlay.getSubimageSize();
            /* We need to stretch/shrink offscreenArmed appropriately
             */
            overlay.setLastRedrawnSize();

            //System.out.println("    before highlightrunnable overlay");
            synchronized (overlay) {
                //System.out.println("    highlightrunnable overlay");
                Graphics2D highlightBuffer = overlay.optimizeResolutionAndClearVisible(
                        overlay.getBufferedImage().getGraphics());
                Graphics2D armBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();
                armBuffer.scale((double) dim.width / lastRedrawnDimension.width,
                        (double) dim.height / lastRedrawnDimension.height);
                armBuffer.drawImage(
                        overlay.offscreenArmed.getSubimage(
                        0, 0, (int) Math.round(
                        lastRedrawnDimension.getWidth()),
                        (int) Math.round(lastRedrawnDimension.getHeight())),
                        0, 0, null);
                synchronized (model) {
                    armBuffer.scale(overlay.getScaleX(),
                            overlay.getScaleX());
                }
                try {
                    /* Draw highlighted links
                     */
                    infoPanelString = panel.highlightModelIterative(highlightBuffer, root);
                } catch (TooBig ex) {
                    Logger.getLogger(GuiManager.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            //System.out.println("    after highlightrunnable overlay");

            SwingUtilities.invokeLater(new Runnable() {

                public void run() {
                    infoArea.setText(infoPanelString);
                }
            });
            panel.repaint();
        }
    }

    class RelinkHighlightedRunnable implements Runnable {

        RelinkHighlightedRunnable() {
        }

        public void run() {
            //System.out.println("    before RelinkHighlightedRunnable overlay");
            synchronized (overlay) {
                //System.out.println("    RelinkHighlightedRunnable overlay");
                Graphics2D highlightBuffer =
                        (Graphics2D) overlay.getBufferedImage().getGraphics();
                highlightBuffer.setRenderingHint(
                        RenderingHints.KEY_ALPHA_INTERPOLATION,
                        RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                highlightBuffer.setRenderingHint(
                        RenderingHints.KEY_ANTIALIASING,
                        RenderingHints.VALUE_ANTIALIAS_ON);
                panel.relinkHighlighted(highlightBuffer);
                panel.repaint();
            }
            //System.out.println("    after RelinkHighlightedRunnable overlay");
        }
    }

    class UnhighlightRunnable implements Runnable {

        /**
         * synchronized internally
         */
        public void run() {
            /* copy the offscreenArmed highlightBuffer to offscreenFull
             */
            Dimension dim = overlay.getSubimageSize();

            /* We need to stretch/shrink offscreenArmed appropriately
             */
            overlay.setLastRedrawnSize();

            //System.out.println("    before UnhighlightRunnable overlay");
            synchronized (overlay) {
                //System.out.println("    UnhighlightRunnable overlay");

                Graphics2D highlightBuffer = overlay.optimizeResolutionAndClearVisible(
                        overlay.getBufferedImage().getGraphics());
                Graphics2D armBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();
                armBuffer.scale((double) dim.width / lastRedrawnDimension.width,
                        (double) dim.height / lastRedrawnDimension.height);
                armBuffer.drawImage(
                        overlay.offscreenArmed.getSubimage(
                        0, 0, (int) Math.round(
                        lastRedrawnDimension.getWidth()),
                        (int) Math.round(lastRedrawnDimension.getHeight())),
                        0, 0, null);

                panel.unhighlightModel(highlightBuffer);
            }
            //System.out.println("    after UnhighlightRunnable overlay");

            /* Clear the infoarea
             */
            /*            infoPanelString = "";

            SwingUtilities.invokeLater(new Runnable() {

            public void run() {
            infoArea.setText(infoPanelString);
            }
            });
             */
            panel.repaint();
        }
    }

    class DoArmingTask implements Runnable {

        public void run() {
            UsefulButNonethelessStupidBoolWrapper task;
            while (true) {
                /* Check if links need redrawing (current resolution becomes
                 * their native resolution); if so, redrawing removes the need
                 * to stretch/shrink the overlay
                 */
                synchronized (zoomIsDirtyLock) {
                    if (zoomIsDirty) {
                        redrawNative(true);
                        zoomIsDirty = false;
                    }
                }
                synchronized (GuiManager.this.armLock) {
                    if ((task = tasks.poll()) == null) {
                        break;
                    }
                }

                if (task.b == true) {
                    /* Arming
                     */

                    /* If we're here, GuiManager.this.armedNodes
                     * can't be empty
                     */
                    NodeGraphics temp;

                    synchronized (GuiManager.this.armLock) {
                        temp = nodesToArm.get(0);
                        nodesToArm.remove(0);
                    }
                    Dimension dim = overlay.getSubimageSize();
                    overlay.setLastRedrawnSize();

                    //System.out.println("    before arming syncs with overlay");
                    synchronized (overlay) {
                        //System.out.println("    arming syncs with overlay");
                        Graphics2D armedBuffer = overlay.optimizeResolutionAndClearVisible(
                                overlay.offscreenArmed.getSubimage(
                                0, 0, dim.width, dim.height).getGraphics());
                        armedBuffer.setRenderingHint(
                                RenderingHints.KEY_ALPHA_INTERPOLATION,
                                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                        armedBuffer.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        Graphics2D fullBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();

                        panel.armModelIterative(
                                armedBuffer,
                                temp);

                        /*  Store current zoom factor
                         */
                        lastRedrawnDimension.setSize(dim);

                        /* Copy the offscreenArmed to offscreenFull
                         */
                        fullBuffer.setBackground(new Color(0, 0, 0, 0));
                        fullBuffer.clearRect(0, 0, dim.width, dim.height);
                        fullBuffer.drawImage(
                                overlay.offscreenArmed.getSubimage(
                                0, 0, dim.width, dim.height),
                                0, 0, null);
                    }
                    //System.out.println("    after arming syncs with overlay");

                } else {
                    /* Unarming
                     */
                    /* If we're here, GuiManager.this.nodesToUnarm
                     * can't be empty
                     */
                    /* Clear armed links
                     */
                    Dimension dim = overlay.getSubimageSize();
                    overlay.setLastRedrawnSize();
                    //System.out.println("    before unarming syncs with overlay");
                    synchronized (overlay) {
                        //System.out.println("    arming syncs with overlay");
                        Graphics2D armedBuffer = overlay.optimizeResolutionAndClearVisible(
                                overlay.offscreenArmed.getGraphics());
                        armedBuffer.setRenderingHint(
                                RenderingHints.KEY_ALPHA_INTERPOLATION,
                                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                        armedBuffer.setRenderingHint(
                                RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
                        Graphics2D fullBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();

                        /* Unarming - it's what we're here for
                         */
                        NodeGraphics temp;
                        synchronized (GuiManager.this.armLock) {
                            temp = nodesToUnarm.get(0);
                            nodesToUnarm.remove(0);
                        }
                        panel.unarmModelIterative(armedBuffer, temp);

                        lastRedrawnDimension.setSize(dim);

                        panel.relinkArmed(armedBuffer);
                        /* Copying offscreenArmed buffer to output (fullBuffer)
                         */
                        fullBuffer.setBackground(new Color(0, 0, 0, 0));
                        fullBuffer.clearRect(0, 0, dim.width, dim.height);
                        fullBuffer.drawImage(
                                overlay.offscreenArmed.getSubimage(
                                0, 0, dim.width, dim.height),
                                0, 0, null);
                    }
                    //System.out.println("    after arming syncs with overlay");
                }
            }
        }
    }

    protected void executeHighlightRunnable(NodeGraphics ng) {
        highlightingExecutor.execute(
                new HighlightRunnable(ng));
    }

    protected void executeRelinkHighlightedRunnable() {
        highlightingExecutor.execute(
                new RelinkHighlightedRunnable());
    }

    protected void executeUnhighlightRunnable() {
        highlightingExecutor.execute(
                new UnhighlightRunnable());
    }

    protected void executeArmingTask() {
        armingExecutor.execute(new DoArmingTask());
    }

    protected void executeUnhiglightRunnableAndWait() {
        FutureTask futureUnhighlight = new FutureTask(new UnhighlightRunnable(), true);
        highlightingExecutor.submit(futureUnhighlight);
        try {
            futureUnhighlight.get();

        } catch (Exception ex) {
            System.out.println(ex);
        }

    }

    protected void executeDraggedRunnable() {
        firstDragExecutor.execute(new WhenDraggedRunnable());
    }

    /**
     * When dragging is through, this method is used to superpose direct links
     * of dragged node onto the armed buffer. The rest of the links will not
     * get drawn on the buffer. Works properly only if the zoomable overlay's
     * armed buffer has resolution optimized with regard to its size.
     *
     * Make sure to call it from a block synchronized with dragLock
     *
     * @param dragged
     */
    protected void drawLinksWhenDragged(NodeGraphics dragged) {

        Dimension dim = overlay.getSubimageSize();

        //System.out.println("    before drawlinkswhendragged syncs with overlay");
        synchronized (overlay) {
            //System.out.println("    drawlinkswhendragged syncs with overlay");
            Graphics2D bufferArmed = (Graphics2D) overlay.offscreenArmed.getSubimage(
                    0, 0, dim.width, dim.height).getGraphics();
            bufferArmed.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            bufferArmed.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            synchronized (model) {
                bufferArmed.scale(overlay.getScaleX(), overlay.getScaleY());
            }

            /* Dragged is child
             */
            int xDragged = (int) (dragged.button.getSize().width) / 2 + 1;
            Iterator<NodeGraphics> iter = panel.parentsLinkedToDragged.iterator();
            while (iter.hasNext()) {
                NodeGraphics current = iter.next();
                int xParent = (int) (current.button.getSize().width) / 2 + 1;
                bufferArmed.drawLine(current.button.getLocation().x + xParent,
                        current.button.getLocation().y + current.button.getSize().width,
                        dragged.button.getLocation().x + xDragged,
                        dragged.button.getLocation().y);
            }

            /* Dragged is parent
             */
            iter = panel.childrenLinkedToDragged.iterator();
            while (iter.hasNext()) {
                NodeGraphics current = iter.next();
//                int xChild = (int) (current.button.getSize().width) / 2 + 1;
                bufferArmed.drawLine(dragged.button.getLocation().x + xDragged,
                        dragged.button.getLocation().y + dragged.button.getSize().height,
                        current.button.getLocation().x + (int) (current.button.getSize().width) / 2 + 1,
                        current.button.getLocation().y);
            }

            /* Copy the offscreenArmed to offscreenFull
             */
            Graphics2D fullBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();
            fullBuffer.setBackground(new Color(0, 0, 0, 0));
            fullBuffer.clearRect(0, 0, dim.width, dim.height);
            fullBuffer.drawImage(
                    overlay.offscreenArmed.getSubimage(
                    0, 0, dim.width, dim.height),
                    0, 0, null);
        }

        //System.out.println("    after drawlinkswhendragged syncs with overlay");
    }
}