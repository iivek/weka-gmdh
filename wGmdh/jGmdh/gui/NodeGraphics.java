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

import wGmdh.jGmdh.gui.GuiManager.ZoomableLinks;
import wGmdh.jGmdh.oldskul.AttributeNode;
import wGmdh.jGmdh.oldskul.Model;
import wGmdh.jGmdh.oldskul.Node;
import java.awt.AWTEvent;
import java.awt.AWTEventMulticaster;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Iterator;
import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * A class used for graphic representation of models/networks. A Node object
 * must exist before NodeGraphics is instantiated.
 *
 * @author ivek
 */
public class NodeGraphics
        implements ChangeListener, MouseListener, MouseMotionListener,
        ActionListener {
    /*
     * We have not added the graphical stuff directly to the Node class to
     * keep it lightweight; only a small number of all Node objects will be
     * drawn and therefore they all don't need things such as coordinates,
     * size, colour etc.
     * The price we would pay to have completely separate classes for raw
     * trainingSetOutput (Node) and drawing (NodeGraphics) is to redetermine
     * and make a copy of the Nodes' links. A solution, used here, is to make a
     * reference to a Node inside NodeGraphics and vice versa, a kind of a
     * simple interface those two classes can communicate through.
     */

    /* Parameters that all NodeButtons have in common
     */
    final public static Font font = new Font("Serif", Font.PLAIN, 4);
    final public static Color fontColor = Color.RED;
    final public static int textX = 2;
    final public static int textY = 12;
    final public static Color perimeterColor = new Color(56, 56, 56);
    final public static Color highlightedColor = new Color(166, 166, 166);
    final public static Color armedColor = new Color(81, 81, 81);
    final public static Color pressedColor = new Color(39, 39, 39);
    final public static int buttonWidth = 25;
    final public static int buttonHeight = 25;

    /**
     * Custom lightweight button.
     */
    public class NodeButton extends JComponent {

        private static final long serialVersionUID = 6963618968719016570L;

        ActionListener actionListener;
//        ItemListener itemListener;
        String label;
        protected boolean isPressed;
        protected int isArmed;
        protected boolean isHighlighted;

        /**
         * Constructs a RoundButton with no label.
         */
        public NodeButton() {
            setOpaque(false);
            isArmed = 0;
            isPressed = false;
            isHighlighted = false;
        }

        /**
         * Constructs a RoundButton with the specified label.
         * @param label the label of the button
         */
        public NodeButton(String label) {
            this.label = label;
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
            setOpaque(false);
            isArmed = 0;
            isPressed = false;
            isHighlighted = false;
        }

        /**
         * Gets the label
         * @param label
         */
        public synchronized String getLabel() {
            return label;
        }

        /**
         * Sets the label
         * @param label
         */
        public synchronized void setLabel(String label) {
            this.label = label;
            invalidate();
            repaint();
        }

        public synchronized void setArmed(boolean bool) {
            if (bool) {
                ++isArmed;
            } else {
                --isArmed;
            }
            repaint();
        }

        public synchronized void resetArmed() {
            isArmed = 0;
            repaint();
        }

        public synchronized void setPressed(boolean bool) {
            isPressed = bool;
            repaint();
        }

        public synchronized void setHighlighted(boolean bool) {
            isHighlighted = bool;
            repaint();
        }

        /**
         * paints the button
         */
        @Override
        public synchronized void paintComponent(Graphics g) {
            int s = Math.min(getSize().width, getSize().height);
            // paint the interior of the button

            if (isPressed) {
                g.setColor(pressedColor);
            } else if (isHighlighted) {
                g.setColor(highlightedColor);
            } else if (isArmed > 0) {
                /* We allow simultaneous arming and highlighting (and pressing)
                 * but we make sure that arming has higher priority than highlighting
                 */
                g.setColor(armedColor);
            } else {
                g.setColor(getBackground());
            }
            g.fillArc(0, 0, s, s, 0, 360);

            // draw the perimeter of the button
            g.setColor(perimeterColor);
            int s2 = s - 2;
            g.drawArc(1, 1, s2, s2, 0, 360);
            g.setColor(fontColor);
            g.setFont(font);
            g.drawString(String.valueOf(node.getIdentifier()), textX, textY);

        }

        /**
         * Adds the specified action listener to receive action events
         * from this button.
         * @param listener the action listener
         */
        public void addActionListener(ActionListener listener) {
            actionListener = AWTEventMulticaster.add(actionListener, listener);
            enableEvents(AWTEvent.MOUSE_EVENT_MASK);
        }

        /**
         * Removes the specified action listener so it no longer receives
         * action events from this button.
         * @param listener the action listener
         */
        public void removeActionListener(ActionListener listener) {
            actionListener = AWTEventMulticaster.remove(actionListener, listener);
        }

        /**
         * Adds the specified item listener to receive item events
         * from this button.
         * @param listener the item listener
         */
        /*        public void addItemListener(ItemListener listener) {
        itemListener = AWTEventMulticaster.add(itemListener, listener);
        enableEvents(AWTEvent.ITEM_EVENT_MASK);
        }
         */
        /**
         * Removes the specified item listener so it no longer receives
         * item events from this button.
         * @param listener the item listener
         */
        /*        public void removeItemListener(ItemListener listener) {
        itemListener = AWTEventMulticaster.remove(itemListener, listener);
        }
         */
        /**
         * Determine if point (x,y) is inside us.
         */
        @Override
        public boolean contains(int x, int y) {
            int mx = (int) Math.round(getSize().width / 2);
            int my = (int) Math.round(getSize().height / 2);
            return (((mx - x) * (mx - x) + (my - y) * (my - y)) <= mx * mx);
        }

        /**
         * Distributes MouseEvents to our actionListener
         */
        @Override
        public void processMouseEvent(MouseEvent e) {
            Graphics g;
            switch (e.getID()) {
                case MouseEvent.MOUSE_CLICKED:
                    if (actionListener != null) {
                        /* Don't mess with isArmed from here for individual,
                         * buttons; set/unset it for the entire network of
                         * buttons
                         */
                        actionListener.actionPerformed(new ActionEvent(
                                this, e.getID(), label));
                    }
                    break;
                case MouseEvent.MOUSE_PRESSED:
                    if (actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(
                                this, e.getID(), label));
                    }

                    // render ourself inverted....
                    isPressed = true;
                    // no double buffering - a simple repaint
                    break;
                case MouseEvent.MOUSE_RELEASED:
                    if (actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(
                                this, e.getID(), label));
                    }
                    // render ourself normal again
                    if (isPressed == true) {
                        isPressed = false;

                    // no double buffering - a simple repaint
                    }

                    break;
                case MouseEvent.MOUSE_ENTERED:
                    if (actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(
                                this, e.getID(), label));
                    }
                    break;
                case MouseEvent.MOUSE_EXITED:
                    if (actionListener != null) {
                        actionListener.actionPerformed(new ActionEvent(
                                this, e.getID(), label));
                    }
                    break;
            }

            super.processMouseEvent(e);
        }
    }
    /* NodeButton ends
     */
    public Node node;
    /* TODO: make NodeGraphics extend JComponent instead of a NodeButton member
     */
    public NodeButton button;
    Point lastMotion;
    /* isHighlighted is to be set immediately on worker thread,
     * this.node.isHighlighted is to be used in sync with the EDK..
     */
    boolean isHighlighted;
    /* ... and the same thing with directReferences. directReferences has a
     * role of a reference counter; when it reaches zero, the button should
     * get unarmed. Note: NodeButton has its internal reference counter that
     * is not synced with the arming thread, which is the case with
     * directReferences.
     * more precisely, for each NodeGraphics, directReferences[i] stores
     * the number of nodes NodeGraphics.this.node is a child to in a GMDH
     * network where armedNodes[i] is root
     */
    ArrayList<UsefulButNonethelessStupidIntWrapper> directReferences;
    /*
     * Flags that mark traversals.
     */
//    boolean alreadyTraversed;
    boolean isArmedAsRoot;
    /* Listeners that we will add to the buttons
     */

    public void actionPerformed(ActionEvent e) {
        /* NodeButtons are put exclusively into NetworkPanels
         */
        NodebuttonPanel parent = (NodebuttonPanel) button.getParent();
        GuiManager manager = parent.getGuiManager();

        switch (e.getID()) {
            case MouseEvent.MOUSE_ENTERED:
                onMouseEntered(manager.forceUnhighlightingBeforeNextHighlighting);
                manager.forceUnhighlightingBeforeNextHighlighting = false;
                break;
            case MouseEvent.MOUSE_EXITED:
                onMouseExited();
                break;
            case MouseEvent.MOUSE_PRESSED:
                synchronized (manager.mouseOverLock) {
                    parent.mouseOver = null;
                }
                break;
            case MouseEvent.MOUSE_CLICKED:
                onMouseClicked(parent, manager);
        }
    }

    /**
     *
     * @param forceUnhighlighting
     */
    public void onMouseEntered(boolean forceUnhighlighting) {

        /* NodeButtons are expected to be put exclusively into NetworkPanels
         */
        NodebuttonPanel parent = (NodebuttonPanel) button.getParent();
        GuiManager manager = parent.getGuiManager();
        if (!manager.mousePressed) {
            /* Write on screen stuff such as Error and coeffs
             */
            Node actual = NodeGraphics.this.node;
            if (actual instanceof Model) {
                manager.infoArea2.setText("<html><body><font size=2><b>ID:  </b>" +
                        (actual.getIdentifier()) + "<br><br>" +
                        "<b>Structure Selection Criterion:  </b><br>" +
                        ((Model) actual).getErrorStructureValidationSet() + "<br>" +
                        "<b>Structure Learning Error:  </b><br>" +
                        ((Model) actual).getErrorStructureLearningSet() +
                        "<br></font></body> </html>");

            }
            if (actual instanceof AttributeNode) {
                manager.infoArea2.setText(
                        "<html><body><font size=2><b>Attribute:</b><br>" +
                        ((AttributeNode) actual).attr +
                        "<br></font></body></html>");
            }

//                    ++parent.entered;
            synchronized (manager.mouseOverLock) {
                parent.mouseOver = NodeGraphics.this;
                /* Highlighting thread
                 */
                if (forceUnhighlighting) {
                    manager.executeUnhighlightRunnable();
                }
                manager.executeHighlightRunnable(
                        NodeGraphics.this);
            }
        }
    }

    public void onMouseClicked(NodebuttonPanel parent, GuiManager manager) {

//        ((NodebuttonPanel) button.getParent()).getGuiManager().executeAfterdragRunnable();

        if (isArmedAsRoot) {
            isArmedAsRoot = false;
            synchronized (manager.mouseOverLock) {
                parent.mouseOver = null;
            }

            /* Take the arming lock. Check if we can find a
             * node the user would like to disarm in the
             * nodesToArm list. If it is there, the arming
             * thread has not yet started arming it, so we
             * can unmark it for disarming, from this thread.
             */
            synchronized (manager.armLock) {
                int index = manager.nodesToArm.indexOf(NodeGraphics.this);
                if (index != -1) {
                    /* Also remove the related task.
                     * TODO: avoid casting to list, by doing
                     * everything with a list
                     */
                    java.util.List<UsefulButNonethelessStupidBoolWrapper> taskList =
                            (java.util.List<UsefulButNonethelessStupidBoolWrapper>) manager.tasks;
                    int cnt = -1;
                    int i = -1;
                    int goal = index;
                    while (cnt < goal) {
                        ++i;
                        if (taskList.get(i).b) {
                            ++cnt;
                        }
                    }
                    taskList.remove(i);

                    //   GuiManager.this.nodesToArm.get(index).button.setPressed(false);
                    manager.nodesToArm.remove(index);

                } else {
                    /* Queue this node up for unarming...
                     * If there are no tasks pending,
                     * armRunnable has returned and we need to
                     * resubmit it to the Executor
                     */
                    boolean pokeExecutor = false;
                    if (manager.tasks.isEmpty()) {
                        pokeExecutor = true;
                    }
                    manager.nodesToUnarm.add(
                            NodeGraphics.this);
                    manager.tasks.add(new UsefulButNonethelessStupidBoolWrapper(false));
                    if (pokeExecutor) {
                        manager.executeArmingTask();
                    }
                }
            }

        } else {
            isArmedAsRoot = true;

            synchronized (manager.mouseOverLock) {
                parent.mouseOver = null;
            }
            manager.executeUnhighlightRunnable();

            /* We don't care if it a node gets unhighlighted or
             * armed first; either way it will get drawn as armed
             */
            synchronized (manager.armLock) {
                /* Queue this node up for arming. If there are
                 * no tasks pending, armRunnable has returned
                 * and we need to resubmit it to the Executor
                 */
                boolean pokeExecutor = false;
                if (manager.tasks.isEmpty()) {
                    pokeExecutor = true;
                }
                manager.nodesToArm.add(
                        NodeGraphics.this);
                manager.tasks.add(new UsefulButNonethelessStupidBoolWrapper(true));
                if (pokeExecutor) {
                    manager.executeArmingTask();
                }
            }
        }
    }

    protected void onMouseExited() {
        //System.out.println("onMouseExited");
        NodebuttonPanel parent = (NodebuttonPanel) button.getParent();
        GuiManager manager = parent.getGuiManager();
        if (!manager.mousePressed) {
            /* Unhighlighting
             */
            if (!manager.mousePressed) {
                synchronized (manager.mouseOverLock) {
                    parent.mouseOver = null;
                }
                manager.executeUnhighlightRunnable();
                manager.infoArea2.setText(null);
            }
        }
    }

    protected void onMouseReleased() {
        //System.out.println("onMouseReleased");

        NodebuttonPanel parent = (NodebuttonPanel) button.getParent();
        GuiManager manager = parent.getGuiManager();
//        ++parent.exited;

        /* If the button is dragged and armed, we should update its links
         * because they have also moved.
         *
         * So,at this point we need an up-to-date information whether the
         * node is armed or not; if it is armed, we need to draw the links
         * on the ArmedBuffer and if it's not, no lines should be
         * drawn there. A good thing is that this method is called by
         * the MouseListener on the EDK (button states also get changed
         * from there), so, if our button is in the armed  state, we should
         * draw lines, and if it isn't we don't draw any, because if we're
         * here (on the EDK) no button state changes are pending and we're
         * up-to-date.
         *
         * We will draw those links from the EDK because there are only a
         * few of them,
         */

        //System.out.println("befoer onmousereleased draglock taken");
        synchronized (manager.dragLock) {
            //System.out.println("onmousereleased draglock taken");
            /* Resetting firstDrag state
             */
            parent.firstDrag = true;
            /* Draw links to/from NodeGraphics this on
             * panel.offscreenArmed
             */
            if (parent.dragged != null) {
                for (Iterator<NodeGraphics> it = parent.dragged.iterator();
                        it.hasNext();) {
                    NodeGraphics dragged = it.next();

                    if (parent.snapToLayer) {
                        dragged.button.setLocation(button.getLocation().x, parent.beforeDrag.y);
                    }

                    if (dragged.button.isArmed > 0) {
                        manager.drawLinksWhenDragged(dragged);
                    }
                }
                manager.needsDrawing = false;
            }

            parent.parentsLinkedToDragged.clear();
            parent.childrenLinkedToDragged.clear();
            parent.dragged.clear();
        }
        //System.out.println("onmousereleased draglock left");

        parent.repaint();

    //manager.executeAfterdragRunnable();
    }

    public void mousePressed(MouseEvent e) {
        /* NodeButtons are put exclusively into NetworkPanels
         */
        ((NodebuttonPanel) button.getParent()).getGuiManager().mousePressed = true;
    }

    public void mouseReleased(MouseEvent e) {
        ((NodebuttonPanel) button.getParent()).getGuiManager().mousePressed = false;
//            ((NodebuttonPanel) button.getParent()).getGuiManager().executeAfterdragRunnable();
        onMouseReleased();
    }

    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public NodeGraphics(Node node) {
        // double referencing
        this.node = node;
        this.node.graphics = this;

        this.button = new NodeButton();
        //this.button.addMouseMotionListener(this);
        this.button.addActionListener(this);
        this.button.addMouseMotionListener(this);
        this.button.addMouseListener(this);

        lastMotion = new Point(0, 0);
        // sync this if your change demands it
        directReferences = new ArrayList<UsefulButNonethelessStupidIntWrapper>(2);
    //    alreadyTraversed = false;
    }

    public NodeGraphics(Node node, int x, int y, int sizeX, int sizeY) {
        // double referencing
        this.node = node;
        this.node.graphics = this;

        this.button = new NodeButton();
        //this.button.addMouseMotionListener(this);
        this.button.addActionListener(this);
        this.button.addMouseMotionListener(this);
        this.button.addMouseListener(this);


        this.button.setSize(sizeX, sizeY);
        this.button.setLocation(x, y);
        lastMotion = new Point(0, 0);
        // sync this if your change demands it
        directReferences = new ArrayList<UsefulButNonethelessStupidIntWrapper>(2);
//        alreadyTraversed = false;
    }

    public class GraphCoordinates {

        int x, y;

        GraphCoordinates(int x, int y) {
            this.x = x;
            this.y = y;
        }

        private GraphCoordinates() {
        }

        void setCoordinates(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    /**
     * ChangeListener's implementations.
     * @param event
     */
    public void stateChanged(ChangeEvent e) {
    }

    /**
     * MouseMotionListener's overrides
     */
    @Override
    public void mouseDragged(MouseEvent e) {
        /* NodeButtons are put exclusively into NetworkPanels
         */
        NodebuttonPanel parentPanel = (NodebuttonPanel) button.getParent();
        final GuiManager manager = parentPanel.getGuiManager();

        //System.out.println("before mousedragged draglock taken ");
        synchronized (manager.dragLock) {
            //System.out.println("mousedragged draglock taken");
            if (parentPanel.firstDrag) {
                /* Force end of highlighting. Furthermore, unhighlight, that
                 * way we won't have anything highlighted when dragging.
                 */
                synchronized (manager.mouseOverLock) {
                    parentPanel.mouseOver = null;
                }
                /* ... and make sure unhighlighting has finished. I chose to
                 * use the same resources when un(highlighting) (otherwise
                 * each node would need another separate flag, and another list
                 * would be needed) and (dis)arming - they can't be run
                 * in parallel, so we have to wait for unhighllighting to finish
                 * first and then proceed with the arming. It doesn't
                 * take long though, because we unhiglight by traversing the
                 * list of nodes rather than the graph of nodes.
                 */
                manager.executeUnhiglightRunnableAndWait();

                /* Draw a temporary shadow of a button at a place where it once was
                 */
                /*                    Graphics bufferArmed = panel.offscreenArmed.getGraphics();
                bufferArmed.drawArc(button.getX(), button.getY(), button.getSize().width, button.getSize().height, 0, 360);
                 */

                parentPanel.dragged.add(this);


                if (parentPanel.snapToLayer) {
                    parentPanel.beforeDrag.setLocation(button.getLocation());
                }
                //System.out.println("mousedragged draglock left");
                /* Invoke the WhenDraggedRunnable instance...
                 */
                manager.executeDraggedRunnable();
                parentPanel.firstDrag = false;
            }
            int newx = button.getX() + e.getX() - lastMotion.x;
            int temp = manager.width - NodeGraphics.buttonWidth;
            if (newx < 0) {
                newx = 0;
            } else if (newx >= manager.width - NodeGraphics.buttonWidth) {
                newx = temp;
            }
            int newy = button.getY() + e.getY() - lastMotion.y;
            temp = manager.height - NodeGraphics.buttonHeight;
            if (newy < 0) {
                newy = 0;
            } else if (newy >= manager.height - NodeGraphics.buttonHeight) {
                newy = temp;
            }
            button.setLocation(newx, newy);
            if (!parentPanel.firstDrag) {

                /* At this point offscreen armed buffer has all links except the
                 * ones that lead to/from dragged drawn. Now we copy
                 * offscreen armed buffer (the one with the armed links) to
                 * offscreen full buffer and fill in with the missing links.
                 */
                //System.out.println("  before mousedragged takes overlay");
                synchronized (manager.overlay) {
                    //System.out.println("  mousedragged takes overlay");
                    ZoomableLinks overlay = parentPanel.getGuiManager().getOverlay();
                    Dimension dim = parentPanel.getGuiManager().getOverlay().getSubimageSize();
                    //   parentPanel.getGuiManager().getOverlay().setLastRedrawnSize();
                    Graphics2D armedBuffer = (Graphics2D) overlay.getBufferedImage().getGraphics();
                    overlay.setLastRedrawnSize();
                    Graphics2D draggedLinksBuffer = overlay.optimizeResolutionAndClearVisible(
                            overlay.getBufferedImage().getGraphics());
                    draggedLinksBuffer.setRenderingHint(
                            RenderingHints.KEY_ALPHA_INTERPOLATION,
                            RenderingHints.VALUE_ALPHA_INTERPOLATION_SPEED);
                    draggedLinksBuffer.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    armedBuffer.drawImage(
                            parentPanel.getGuiManager().getOverlay().offscreenArmed.getSubimage(
                            0, 0, dim.width, dim.height),
                            0, 0, null);

                    /* If there are nested graphs armed, there will be
                     * redundant nodes in linkedToDragged, resulting in drawing
                     * more links than neccessary. Although an ad hoc turnaround
                     * is provided, this still remains a TODO.
                     * Adhockery:
                     *  problem: linkedToDragged redundancy
                     *      -> suboptimal links drawing speed
                     *      -> possible links drawing on some unarmed model
                     *  solution: traverse through panel.dragged.directReferences.
                     *      if there exists an element !=0, draw dragged's links;
                     *      otherwise, don't draw anything
                     * This way we isolated nested graphs, but it is still possible
                     * for the algorithm to revisit the node inside the same graph.
                     */

                    manager.needsDrawing = false;

                    for (Iterator<NodeGraphics> it = parentPanel.dragged.iterator(); it.hasNext();) {
                        NodeGraphics dragged = it.next();
                        Iterator<UsefulButNonethelessStupidIntWrapper> iter2 =
                                dragged.directReferences.iterator();
                        while (iter2.hasNext()) {
                            if (iter2.next().i > 0) {
                                manager.needsDrawing = true;
                                break;
                            }
                        }
                    }
                    if (manager.needsDrawing) {
                        for (Iterator<NodeGraphics> it = parentPanel.dragged.iterator(); it.hasNext();) {
                            NodeGraphics dragged = it.next();
                            /* Dragged is child
                             */
                            int xDragged = (int) (dragged.button.getSize().width) / 2 + 1;
                            Iterator<NodeGraphics> iter = parentPanel.parentsLinkedToDragged.iterator();
                            while (iter.hasNext()) {
                                NodeGraphics current = iter.next();
                                int xParent = (int) (current.button.getSize().width) / 2 + 1;
                                draggedLinksBuffer.drawLine(current.button.getLocation().x + xParent,
                                        current.button.getLocation().y + current.button.getSize().height,
                                        dragged.button.getLocation().x + xDragged,
                                        dragged.button.getLocation().y);
                            }

                            /* Dragged is parent
                             */
                            iter = parentPanel.childrenLinkedToDragged.iterator();
                            while (iter.hasNext()) {
                                NodeGraphics current = iter.next();
                                //int xChild = (int) (current.button.getSize().width) / 2 + 1;
                                draggedLinksBuffer.drawLine(dragged.button.getLocation().x + xDragged,
                                        dragged.button.getLocation().y + dragged.button.getSize().height,
                                        current.button.getLocation().x + (int) (current.button.getSize().width) / 2 + 1,
                                        current.button.getLocation().y);
                            }
                            parentPanel.repaint();
                        }
                    }
                }
                //System.out.println("  mousedragged left overlay");
            }
        }
//        System.out.println("mousedragged draglock left");
    }

    @Override
    public void mouseMoved(
            MouseEvent e) {
        /* Store the last motion coordinates relative to the button
         */
        lastMotion.x = e.getX();
        lastMotion.y = e.getY();
    }

    public NodeButton getButton() {
        return button;
    }
}
