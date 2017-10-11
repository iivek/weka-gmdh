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
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import wGmdh.jGmdh.hybrid.SseRegressionEquations;
import wGmdh.jGmdh.hybrid.SseRegressionEquations.Summand;

/**
 *
 * @author ivek
 */
public final class NodebuttonPanel extends JPanel {

    private static final long serialVersionUID = -6597548787575324709L;
    /* Double buffering will be used
     * Lines that represent links won't be stored as classes due to
     * a possibly large number of them. They will be stored drawn onto a
     * Graphics, using a fixed amount of memory.
     * Antialiased children.
     */
    private GuiManager mgr;
    Dimension bufferDimension;
    NodeGraphics mouseOver;
    /* allow multiple dragged nodes - pave the way for drag-select... a TODO
     */
    ArrayList<NodeGraphics> dragged;

    /* If true, node after dragging will snap back to its y-coordinate
     */
    boolean snapToLayer = true;
    Point beforeDrag = new Point();
    boolean firstDrag;
    /* Traversal queues. When highlighting we use alreadyTraversed flags to
     * avoid multiple traversals. When arming/disarming/linking we don't check
     * for multiple traversals belonging to one armed node; we only guarantee
     * that node belonging to one armed model won't get visited when traversing
     * some other armed model.
     *
     * One flag per NodeGraphics that marks traversals is already hard on memory,
     * another one would be too much. We use that flag by highlighting thread
     * exclusively, for we want it to be fast, while arming/disarming/relinking
     * can wait.
     */
    java.util.Queue highlightingTraversalQueue;
    java.util.Queue armingTraversalQueue;
    /* Keeps track of nodes linked to a node that is currently dragged by
     * the user
     */
    java.util.ArrayList<NodeGraphics> parentsLinkedToDragged;
    java.util.ArrayList<NodeGraphics> childrenLinkedToDragged;
    /*
     * Debugging stuff
     */
    /*    public int entered = 0;
    public int exited = 0;
    public int unhthread = 0;
     */
    // Debugging stuff ends

    public void setGuiManager(GuiManager mgr) {
        this.mgr = mgr;
    }

    public GuiManager getGuiManager() {
        return this.mgr;
    }

    NodebuttonPanel(GuiManager mgr) {
        highlightingTraversalQueue = new java.util.LinkedList();
        armingTraversalQueue = new java.util.LinkedList();
        parentsLinkedToDragged = new ArrayList();
        childrenLinkedToDragged = new ArrayList();
        /* no sync needed here, because we still have only one thread
         * that messes with these next variables
         */
        mouseOver = null;
        dragged = new ArrayList<NodeGraphics>();
        firstDrag = true;
        setGuiManager(mgr);
    }

    @Override
    public void paintChildren(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        super.paintChildren(g2);
    }

    @Override
    public void finalize() throws Throwable {
        super.finalize();
    }

    /*
     * Adds NodeButtons as Node representations to JPanel
     */
    public void addNodeAsButton(NodeGraphics nodeGr, boolean visible) {

        nodeGr.button.setBounds(nodeGr.button.getLocation().x,
                nodeGr.button.getLocation().y,
                NodeGraphics.buttonWidth, NodeGraphics.buttonHeight);
        nodeGr.button.setVisible(visible);
        this.add(nodeGr.button);
    }

    /**
     * Array of doubles to html, without <html>parts.
     * Each member in new row.
     *
     * @param d
     * @return s
     */
    public String toString(double[] d) {
        String s = new String();
        for (int i = 0; i < d.length; ++i) {
            s += "    " + d[i] + ",<br> ";
        }
        return s;
    }

    /**
     * Used to draw model links on Graphics, performs one iteration of
     * non-recursive, breadth-first traversal, using a (FIFO) queue.
     * Called from the highlighting thread
     *
     * @param g2            Image to draw to.
     * @param visited       Model to process.
     * @return              NodeGraphics to process in the next iteration
     */
    protected NodeGraphics highlightModelIteration(
            Graphics2D g2, NodeGraphics visited) {

        /* Initialize
         */
        int xParent = (int) (visited.button.getSize().width) / 2 + 1;

        /* No need to highlight the node more than once. Just return
         * the next node to process
         * we wait for the EDT to highlight the button so we can be certain
         * that its state is updated
         */
        if (visited.isHighlighted) {
            return (NodeGraphics) highlightingTraversalQueue.poll();
        }

        mgr.highlightedNodes.add(visited);
        visited.isHighlighted = true;
        /* Button state changes are best to be done on the EDT.
         */
        HighlightButtonRunnable highlightButtonRunnable = new HighlightButtonRunnable();
        highlightButtonRunnable.setOperateOn(visited);
        try {
            /* sacrifice steps for appearance - we want highlighting
             * to progress at the same rate as drawing the lines/links
             * drawing lines. EDIT: Not true.
             */
            SwingUtilities.invokeLater(highlightButtonRunnable);
        } catch (Exception exc) {
            System.err.println("GUI didn't respond to highlighting");
        }

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                /* Enqueue its children
                 */
                //  if(child == null)   System.out.println("red alert");
                highlightingTraversalQueue.add(child);
                int xChild = (int) (child.button.getSize().width) / 2 + 1;
                /* Draw vertices
                 */
                g2.drawLine(visited.button.getLocation().x + xParent,
                        visited.button.getLocation().y + visited.button.getSize().height,
                        child.button.getLocation().x + xChild,
                        child.button.getLocation().y);
            }
        }
        return (NodeGraphics) highlightingTraversalQueue.poll();
    }

    /**
     * Called from the highlighting thread, repaints after each iteration.
     *
     * @param g
     * @param root
     * @return      mathematical representation of GMDH polynomial
     */
    public String highlightModelIterative(
            Graphics g, NodeGraphics root) throws TooBig {

        String output = new String();
        /* Loop until queue has emptied; this means all the nodes have been
         * visited
         */
        NodeGraphics current = root;

        /*debugging stuff
         */
        /*System.out.print(current.node.getIdentifier() + ": ");
        for (int i = 0; i < current.directReferences.size(); ++i) {
        System.out.print("  " + current.directReferences.get(i).i + "  ");
        }*/


        while (current != null) {
            synchronized (mgr.mouseOverLock) {
                if (root != mouseOver) {
//                    System.out.println("clearing traversal queue");
                    highlightingTraversalQueue.clear();
                    /* This is commonly followed by external unhiglighting,
                     * as we don't want a partially highlighted model to
                     * stay painted
                     */
                    return output;
                }
            }

            /* Append polynomial representation of current to the output string
             */
            /* Note: this code can be put into highlightModelIteration, to avoid
             * double checking of !current.node.graphics.isHighlighted - it
             * gets checked twice, once in highlightModelIteration and once
             * again here below
             */
            if (!current.node.graphics.isHighlighted) {
                if (current.node instanceof Model) {
                    /* TODO: only the coefficients from the first fold are shown
                     * at the moment
                     */
                    output = output.concat(((Model) current.node).toHTML(0));
                }
            }

            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(
                    RenderingHints.KEY_ALPHA_INTERPOLATION,
                    RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
            g2.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);
            current = highlightModelIteration(g2, current);

        }
        return output;
    }

    public static class HighlightButtonRunnable implements Runnable {

        private NodeGraphics operateOn;

        HighlightButtonRunnable() {
        }

        public void run() {
            operateOn.button.setHighlighted(true);
        }

        public synchronized void setOperateOn(NodeGraphics ng) {
            operateOn = ng;
        }

        public synchronized NodeGraphics getOperateOn() {
            return operateOn;
        }
    }

    /**
     * A Runnable that draws lines on Graphics.
     */
    public static class DrawLineRunnable implements Runnable {

        private int x1,  y1,  x2,  y2;
        Graphics g;

        DrawLineRunnable(Graphics g, int x1, int y1, int x2, int y2) {
            this.x1 = x1;
            this.x2 = x2;
            this.y1 = y1;
            this.y2 = y2;
            this.g = g;
        }

        public void run() {
            g.drawLine(x1, y1, x2, y2);
        }
    }

    /**
     * Unhighlights the buttons. Repaints from the corresponding runnable.
     *
     * @param g2            Image to draw to.
     * @param graphNode     Model to draw.
     */
    public synchronized void unhighlightModel(Graphics g) {

        /* Unhighlight NodeButtons. Never clean the Graphics
         */
        NodeGraphics current;
        while (true) {
            current = (NodeGraphics) mgr.highlightedNodes.poll();
            if (current == null) {
                break;
            }
            try {
                SwingUtilities.invokeLater(
                        new UnhighlightButtonRunnable(current));
            } catch (Exception exc) {
                System.err.println("GUI didn't respond to unhighlighting");
            }
            current.isHighlighted = false;
        }
    }

    public class UnhighlightButtonRunnable implements Runnable {

        private NodeGraphics operateOn;

        UnhighlightButtonRunnable() {
        }

        UnhighlightButtonRunnable(NodeGraphics ng) {
            operateOn = ng;
        }

        public void run() {
            operateOn.button.setHighlighted(false);
        }

        public synchronized void setOperateOn(NodeGraphics ng) {
            operateOn = ng;
        }

        public synchronized NodeGraphics getOperateOn() {
            return operateOn;
        }
    }

    /**
     * This method gets invoked on every armed GMDH network when user starts
     * dragging of a button. Every node in the network, whose root is
     * received as a parameter, gets visited. If a visited node has a link
     * to the dragged node, it gets stored, otherwise this link gets drawn on g.
     *
     * @param g
     * @param root
     * @param dragged
     */
    public void whenDraggedIterative(
            Graphics g, NodeGraphics root, NodeGraphics dragged) {

        NodeGraphics current;
        current = root;
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        while (current != null) {
            //System.out.println("before whendraggediterative draglock taken");
            synchronized (mgr.dragLock) {
                //System.out.println("whendraggediterative draglock taken");
                if (dragged == null) {
                    highlightingTraversalQueue.clear();
                    return;
                }
            }
            //System.out.println("whendraggediterative draglock left");

            current = whenDraggedIteration(g2, current, dragged);

        }

        /* No repainting here
         */
        return;
    }

    /**
     * Invoked by whenDraggedIterative
     *
     * @param g2
     * @param visited
     * @param dragged
     * @return
     */
    protected NodeGraphics whenDraggedIteration(
            Graphics2D g2, NodeGraphics visited, NodeGraphics dragged) {

        /* We use the same flag and the same traversal queue and the same
         * list of visited NodeGraphics as
         * in highlightIteration.
         */
        if (visited.isHighlighted) {
            return (NodeGraphics) highlightingTraversalQueue.poll();
        }

        mgr.highlightedNodes.add(visited);
        visited.isHighlighted = true;

        /* No button state changes.
         */

        /* Proceed if it has children
         */
        g2.setColor(new Color(200, 200, 10));
        //System.out.println("before whendraggediteration draglock taken 1");
        synchronized (mgr.dragLock) {
            //System.out.println("whendraggediteration draglock taken 1");
            if (visited.node instanceof Model) {
                for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                    /* Enqueue its children
                     */
                    NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                    highlightingTraversalQueue.add(child);

                    if (child == dragged) {
                        /* If a child is the dragged NodeGraphics, store this
                         * (visited) NodeGraphics in linkedToDragged.
                         */
                        parentsLinkedToDragged.add(visited);
                    } else {
                        /* Else draw the links to children unless we are dragged
                         */
                        if (visited != dragged) {
                            int xParent = (int) (visited.button.getSize().width) / 2 + 1;
                            g2.drawLine(visited.button.getLocation().x + xParent,
                                    visited.button.getLocation().y + visited.button.getSize().height,
                                    child.button.getLocation().x + (int) (child.button.getSize().width) / 2 + 1,
                                    child.button.getLocation().y);
                        }
                    }
                }
            }
        }
        //System.out.println("whendraggediteration draglock left 1");
        return (NodeGraphics) highlightingTraversalQueue.poll();
    }

    /**
     * Used to draw model links on Graphics + arming, performs one iteration
     * of non-recursive, breadth-first traversal, using a (FIFO) queue.
     * Called from the arming worker thread
     *
     * @param g2            Image to draw to.
     * @param graphNode     Model to draw.
     * @param index         Index of root of this GMDH network which it's
     *                      stored under in armedNodes
     * @return              NodeGraphics to process in the next iteration
     */
    protected NodeGraphics armModelIteration(
            Graphics2D g2, NodeGraphics visited, int index) {

        int xParent = (int) (visited.button.getSize().width) / 2 + 1;

        /* No need to visit the node more than once. Just return
         * the next node to process.
         */
        ++visited.directReferences.get(index).i;
        if (visited.directReferences.get(index).i > 1) {
            return (NodeGraphics) armingTraversalQueue.poll();
        }

        /* Button state changes are best to be done on the EDT.
         * this.button.isArmed counts how many networks the node belongs to
         */
        try {
            SwingUtilities.invokeLater(new ArmButtonRunnable(visited));
        } catch (Exception exc) {
            System.err.println("GUI didn't respond to arming");
        }

        //System.out.println("Visited: " + visited.node.getIdentifier() + " " + visited.directReferences.get(index).i);

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                /* Enqueue its children
                 */
                NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                armingTraversalQueue.add(child);
                int xChild = (int) (child.button.getSize().width) / 2 + 1;
                /* Draw vertices
                 */
                g2.drawLine(visited.button.getLocation().x + xParent,
                        visited.button.getLocation().y + visited.button.getSize().height,
                        child.button.getLocation().x + xChild,
                        child.button.getLocation().y);
            }
        }
        return (NodeGraphics) armingTraversalQueue.poll();
    }

    /**
     * Used to draw model links on Graphics + arming, performs one iteration
     * of non-recursive, breadth-first traversal, using a (FIFO) queue.
     * Called from the arming worker thread
     *
     * @param g2            Image to draw to.
     * @param visited
     * @param index         Index of root of this GMDH network which it's
     *                      stored under in armedNodes
     * @return              NodeGraphics to process in the next iteration
     */
    protected NodeGraphics armModelIterationHelper(
            Graphics2D g2, NodeGraphics visited, int index) {

        int xParent = (int) (visited.button.getSize().width) / 2 + 1;

        /* No need to visit the node more than once. Just return
         * the next node to process.
         */
        if (visited.directReferences.size() > index) {
            return (NodeGraphics) armingTraversalQueue.poll();
        }
        /* Padding with zeroes
         */
        while (visited.directReferences.size() <= index) {
            visited.directReferences.add(new UsefulButNonethelessStupidIntWrapper(0));
        }

        /* No button state changes, we only want to update flags
         */

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                /* Enqueue its children
                 */
                NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                int xChild = (int) (child.button.getSize().width) / 2 + 1;
                armingTraversalQueue.add(child);
                /* Draw vertices
                 */
                g2.drawLine(visited.button.getLocation().x + xParent,
                        visited.button.getLocation().y + visited.button.getSize().height,
                        child.button.getLocation().x + xChild,
                        child.button.getLocation().y);
            }
        }
        return (NodeGraphics) armingTraversalQueue.poll();
    }

    /**
     * Called from the arming thread, (painting also gets done from there).
     *
     * @param g
     * @param root
     */
    public void armModelIterative(
            Graphics g, NodeGraphics root) {

        /* This will be the index of the new root in armedNodes and also
         */
        int index;
        if (!mgr.armedNodes.isEmpty()) {
            index = mgr.armedNodes.get(0).directReferences.size();
        } else {
            index = 0;
        }

        /* Traverse all the armed nodes and add flags
         */
        NodeGraphics current;
        Graphics2D g2 = (Graphics2D) g;
        g2.setColor(new Color(200, 10, 217));
        g2.setRenderingHint(
                RenderingHints.KEY_ALPHA_INTERPOLATION,
                RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
        g2.setRenderingHint(
                RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        for (int i = 0; i < mgr.armedNodes.size(); ++i) {
            current = mgr.armedNodes.get(i);

            while (current != null) {
                current = armModelIterationHelper(g2, current, index);
            }
        }

        current = root;
        while (current != null) {

            /* Padding directReferences with zeros
             */
            while (current.directReferences.size() <= index) {
                current.directReferences.add(new UsefulButNonethelessStupidIntWrapper(0));
            }
            current = armModelIteration(g2, current, index);
        }

        mgr.armedNodes.add(root);
    }

    /**
     * Used to draw model links on Graphics. No button state changes.
     * Performs one iteration of non-recursive, breadth-first traversal,
     * using a (FIFO) queue. Called from the arming worker thread
     *
     * @param g2            Graphics to draw to.
     * @param graphNode
     * @param index
     * @return              NodeGraphics to process in the next iteration
     */
    protected NodeGraphics linkModelIteration(
            Graphics g2, NodeGraphics graphNode, int index) {

        /* Initialize
         */
        final NodeGraphics visited = graphNode;
        /* No need to visit the node more than once. Just return
         * the next node to process. Iterate through directReferences from
         * directReferences[0] to directReferences[index-1]. If any entry in
         * that range is >0, this means we already have visited current node
         * (and all its offsprings)so we break the iteration.
         */
        for (int i = 0; i < index - 1; ++i) {
            if (visited.directReferences.get(i).i > 0) {
                return null;
            }
        }
        int xParent = (int) (visited.button.getSize().width) / 2 + 1;

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                /* Enqueue its children
                 */
                NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                int xChild = (int) (child.button.getSize().width) / 2 + 1;
                armingTraversalQueue.add(child);
                /* Draw vertices
                 */
                //g2.setColor(new Color(0, 255, 217));
                g2.drawLine(visited.button.getLocation().x + xParent,
                        visited.button.getLocation().y + visited.button.getSize().height,
                        child.button.getLocation().x + xChild,
                        child.button.getLocation().y);
            }
        }
        return (NodeGraphics) armingTraversalQueue.poll();
    }

    /**
     * Used in conjunction with relinkArmed. It uses a fake directReferences
     * entry as a flag to determine whether we visited the node or not.
     * Other than the condition when it returns null and the fakeIndex
     * management, it is the same as linkModelIteration, so check that one out.
     *
     * @param g2            Graphics to draw to.
     * @param graphNode
     * @param index         fake directReferences index, same as
     *                      directReferences.size()-1
     * @return              NodeGraphics to process in the next iteration
     */
    protected NodeGraphics fakeLinkModelIteration(
            Graphics g2, NodeGraphics graphNode, int index) {

        /* Initialize
         */
        final NodeGraphics visited = graphNode;
        /* No need to visit the node more than once.
         */
        if (visited.directReferences.size() > index) {
            return (NodeGraphics) armingTraversalQueue.poll();
        } else {
            // value is unimportant - let's say zero
            visited.directReferences.add(new UsefulButNonethelessStupidIntWrapper(0));
        }

        int xParent = (int) (visited.button.getSize().width) / 2 + 1;

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                /* Enqueue its children
                 */
                NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                int xChild = (int) (child.button.getSize().width) / 2 + 1;
                armingTraversalQueue.add(child);
                /* Draw vertices
                 */
                //g2.setColor(new Color(0, 255, 217));
                g2.drawLine(visited.button.getLocation().x + xParent,
                        visited.button.getLocation().y + visited.button.getSize().height,
                        child.button.getLocation().x + xChild,
                        child.button.getLocation().y);
            }
        }
        return (NodeGraphics) armingTraversalQueue.poll();
    }

    /**
     * Call it from the arming thread.
     *
     * @param g
     * @param index
     */
    protected void linkModelIterative(
            Graphics g, int index) {

        /* Loop until queue has emptied; this means all the nodes have been
         * visited
         */
        NodeGraphics current = mgr.armedNodes.get(index);
        while (current != null) {
            current = linkModelIteration(g, current, index);
        }
    }

    /**
     * Call it from the arming thread and sync with overlay.
     *
     * In order to avoid multiple traversals, we will fake an entra
     * armedNodes.get(i).directReferences - we add one extra reference to each
     * node that marks if the node already has been visited (and its links
     * have been drawn). At the end of the method this fake reference gets
     * erased. Therefore it is essential to synchronize it (and related methods)
     * with the arming executor - armedNodes.get(i).directReferences might
     * get messed up otherwise.
     *
     * @param buffer
     */
    public void relinkArmed(Graphics buffer) {
        /* This will be the index of the fake entry
         */
        int fakeIndex;
        if (!mgr.armedNodes.isEmpty()) {
            fakeIndex = mgr.armedNodes.get(0).directReferences.size();
        } else {
            fakeIndex = 0;
        }

        /* Visit every armed node
         */
        for (int index = 0; index < mgr.armedNodes.size(); ++index) {
            NodeGraphics current = mgr.armedNodes.get(index);
            /* Now iteratively visit all children of that node. fakeIndex
             * is a stopping condition, that marks the node that already has
             * been visited.
             */
            while (current != null) {
                /*                System.out.print(current.node.getIdentifier() + ": ");
                for (int i = 0; i < current.directReferences.size(); ++i) {
                System.out.print("  " + current.directReferences.get(i).i + "  ");
                }
                 */
                current = fakeLinkModelIteration(buffer, current, fakeIndex);
            }
        }

        /* At the end, we remove the entry at the fakeIndex position, for all
         * armed nodes
         */
        for (int index = 0; index < mgr.armedNodes.size(); ++index) {
            NodeGraphics current = mgr.armedNodes.get(index);
            while (current != null) {
                current = removeFakeEntries(current, fakeIndex);
            }
        }

    }

    /**
     * Used in conjunction with relinkArmed. Removes fake entries in
     * directReferences that are used as flags. This method also avoids
     * multiple traversals.
     *
     * @param graphNode
     * @param index         index to remove
     * @return
     */
    protected NodeGraphics removeFakeEntries(
            NodeGraphics graphNode, int index) {

        /* Initialize
         */
        final NodeGraphics visited = graphNode;

        /* No need to visit the node more than once.
         */
        if (visited.directReferences.size() > index) {
            /* Proceed if it has children
             */
            if (visited.node instanceof Model) {
                for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {
                    /* Enqueue its children
                     */
                    NodeGraphics child = ((Model) visited.node).links.get(i).graphics;
                    armingTraversalQueue.add(child);
                }
            }
            /* erase the entry
             */
            visited.directReferences.remove(index);
        //System.out.println("removing fake: " + visited.node.getIdentifier());
        }

        return (NodeGraphics) armingTraversalQueue.poll();
    }

    /**
     * Traverses mgr.highlightedNodes and draws links to their children
     *
     * @param g
     * @return
     */
    protected boolean relinkHighlighted(Graphics g) {
        boolean empty = mgr.highlightedNodes.isEmpty();
        if (empty) {
            return !empty;
        }

        for (Iterator<NodeGraphics> iter = mgr.highlightedNodes.iterator(); iter.hasNext();) {
            NodeGraphics currentParent = (NodeGraphics) iter.next();
            int xParent = (int) (currentParent.button.getSize().width) / 2 + 1;
            if (currentParent.node instanceof Model) {
                for (Iterator<Node> iter2 = ((Model) currentParent.node).links.iterator();
                        iter2.hasNext();) {
                    Node currentChild = iter2.next();
                    int xChild = (int) (currentChild.graphics.button.getSize().width) / 2 + 1;
                    g.drawLine(currentParent.button.getLocation().x + xParent,
                            currentParent.button.getLocation().y + currentParent.button.getSize().height,
                            currentChild.graphics.button.getLocation().x + xChild,
                            currentChild.graphics.button.getLocation().y);
                }
            }
        }

        return true;
    }

    public static class ArmButtonRunnable implements Runnable {

        private NodeGraphics operateOn;

        ArmButtonRunnable() {
        }

        ArmButtonRunnable(NodeGraphics ng) {
            operateOn = ng;
        }

        public void run() {
            operateOn.button.setArmed(true);
        }

        public synchronized void setOperateOn(NodeGraphics ng) {
            operateOn = ng;
        }

        public synchronized NodeGraphics getOperateOn() {
            return operateOn;
        }
    }

    protected NodeGraphics unarmModelIteration(
            Graphics g, NodeGraphics graphNode, int index) {

        final NodeGraphics visited = graphNode;

        /* Avoid redrawing the links we already have visited
         */
        if (visited.directReferences.size() == mgr.armedNodes.size()) {
            return (NodeGraphics) armingTraversalQueue.poll();
        }

        //System.out.println("Visited: " + visited.node.identifier + " " + flag + " " + visited.directReferences.get(index).b);
        //System.out.println("        " + visited.directReferences.get(index).b);

        visited.directReferences.remove(index);

        /* Button state changes are best to be done on the EDT.
         * Unarm button if we aim to have 0 armed nodes
         */
        try {
            SwingUtilities.invokeLater(new UnarmButtonRunnable(visited));
        } catch (Exception exc) {
            System.err.println("GUI didn't respond to unarming");
        }
        /* We may have some zeroes in directReferences and an unarmed button
         * at this point but it's OK. We shall simply have less zeroes to
         * pad when arming next time.
         */

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {

                /* Enqueue its children
                 */
                armingTraversalQueue.add(
                        ((Model) visited.node).links.get(i).graphics);
            }
        }
        return (NodeGraphics) armingTraversalQueue.poll();
    }

    private NodeGraphics unarmModelIterationHelper(
            Graphics g, NodeGraphics graphNode, int indexToRemove,
            int indexOfCurrent) {

        final NodeGraphics visited = graphNode;

        /* Avoid redrawing the links we already have visited
         */
        if (visited.directReferences.size() == mgr.armedNodes.size()) {
            //               System.out.println("alrite");
            return (NodeGraphics) armingTraversalQueue.poll();
        }

        visited.directReferences.remove(indexToRemove);

        /* No button state change - we don't recount the number of networks
         * this node belongs to, we only traverse the networks to remove
         * loose entries.

        /* Proceed if it has children
         */
        if (visited.node instanceof Model) {
            for (int i = 0; i < ((Model) visited.node).links.size(); ++i) {

                /* Enqueue its children
                 */
                armingTraversalQueue.add(
                        ((Model) visited.node).links.get(i).graphics);
            }
        }
        return (NodeGraphics) armingTraversalQueue.poll();
    }

    /**
     * Unarms the buttons and clears links, if neccessary. Non-recursive,
     * breadth-first, using a (FIFO) queue. Will be called from the
     * arming thread
     *
     * @param g             Image to draw to.
     * @param root          Model to draw.
     */
    public void unarmModelIterative(
            Graphics g, NodeGraphics root) {

        int index = mgr.armedNodes.indexOf(root);
        NodeGraphics current = root;
        /* Find the node that needs to be unarmed and remove it from the list
         */
        mgr.armedNodes.remove(index);

        /* This method is called from the armingThread which is synchronized
         * with the EDT, so we can safely use currentParent.directReferences from
         * here
         */
        int listSizeAfterUnarming = current.directReferences.size() - 1;

        /* Loop until queue has emptied; this means all the nodes have been
         * visited
         */
        while (current != null) {
            current = unarmModelIteration(g, current, index);
        }

        /* we have to iterate through all remaining nodes to arm
         */
        for (int i = 0; i < mgr.armedNodes.size(); ++i) {
            current = mgr.armedNodes.get(i);
            /* If currentParent root doesn't have hanging references, it doesn't
             * need processing.
             */
            if (current.directReferences.size() == mgr.armedNodes.size()) {
                continue;
            }

            while (current != null) {
                current = unarmModelIterationHelper(g, current, index, i);
            }
        }
//        System.out.println("    nr. highlighted = " + mgr.highlightedNodes.size());
    }

    public static class UnarmButtonRunnable implements Runnable {

        private NodeGraphics operateOn;

        UnarmButtonRunnable() {
        }

        UnarmButtonRunnable(NodeGraphics ng) {
            operateOn = ng;
        }

        public void run() {
            operateOn.button.setArmed(false);
        }

        public synchronized void setOperateOn(NodeGraphics ng) {
            operateOn = ng;
        }

        public synchronized NodeGraphics getOperateOn() {
            return operateOn;
        }
    }

    public static class UnpressButtonRunnable implements Runnable {

        private NodeGraphics operateOn;

        UnpressButtonRunnable() {
        }

        UnpressButtonRunnable(NodeGraphics ng) {
            operateOn = ng;
        }

        public synchronized void run() {
            operateOn.button.setPressed(false);
        // System.out.println("visited: " + operateOn.node.identifier);
        }

        public synchronized void setOperateOn(NodeGraphics ng) {
            operateOn = ng;
        }

        public synchronized NodeGraphics getOperateOn() {
            return operateOn;
        }
    }

    /**
     * TODO: Provided we have a good graph traversal algorithm for drawing
     * the links/vertices, if we have multiple models to draw, treat them
     * as one graph, thus avoiding unneccessary multiple traversals.
     * For the time being, we treat and draw each model separately, but
     * specifically mark nodes that are shared by different models
     */
    @Override
    public void paintComponent(Graphics g) {
        /* If panel has resized, resize offscreenFull
         */
        //System.out.println("  before panel.paintComponent takes overlay");
        synchronized (mgr.overlay) {
            //System.out.println("  panel.paintComponent takes overlay");
            if (bufferDimension.height != getSize().height ||
                    bufferDimension.width != getSize().width //||  offscreenFull == null
                    ) {
                resetBuffer();
            }
        }
        //System.out.println("  before panel.paintComponent leaves overlay");
            /*
         * Antialiased drawings
         */
        /*                Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
         */
        /* Copy the offscreenFull buffer
         */
        super.paintComponent(g);
    //g.drawOnGraphics(offscreenFull, 0, 0, null);
        /*
     * buffer gets cleared elsewhere
     */
    }

    /**
     * Takes care of ofscreenImage's dimensions; we want them to be the same
     * as our panel's dimensions
     */
    private void resetBuffer() {

        bufferDimension.width = getSize().width;
        bufferDimension.height = getSize().height;

        //Graphics buffer = null;
        /*            if (offscreenFull != null) {
        buffer = offscreenFull.getGraphics();
        }
        if (buffer != null) {
        buffer.dispose();
        buffer = null;
        }
        if (offscreenFull != null) {
        offscreenFull.flush();
        offscreenFull = null;
        }
        offscreenFull = createImage(
        bufferDimension.width, bufferDimension.width);
         */
        /*            buffer = null;
        if (offscreenArmed != null) {
        buffer = offscreenArmed.getGraphics();
        }
        if (buffer != null) {
        buffer.dispose();
        buffer = null;
        }
        if (offscreenArmed != null) {
        offscreenArmed.flush();
        offscreenArmed = null;
        }

        offscreenArmed = ZoomableOverlayPanel.createZoomableImage(new Dimension(getWidth(), getHeight()),
        mgr.ZOOM_MAX / mgr.ZOOM_SCALE, BufferedImage.BITMASK);
         */
        Runtime.getRuntime().gc();

    }
};
