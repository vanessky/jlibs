/**
 * Copyright 2015 Santhosh Kumar Tekuri
 *
 * The JLibs authors license this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package jlibs.nblr.editor.actions;

import jlibs.nblr.editor.RuleScene;
import jlibs.nblr.rules.Edge;
import jlibs.nblr.rules.Node;
import org.netbeans.api.visual.action.PopupMenuProvider;
import org.netbeans.api.visual.widget.Widget;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Santhosh Kumar T
 */
public class NodePopupProvider implements PopupMenuProvider{
    private RuleScene scene;

    public NodePopupProvider(RuleScene scene){
        this.scene = scene;
    }

    private Node node;
    public JPopupMenu getPopupMenu(Widget widget, Point localLocation){
        node = (Node)scene.findObject(widget);

        JMenu insertNodeMenu = new JMenu("Insert Node");
        insertNodeMenu.add(new InsertBeforeNodeAction(scene, node));
        insertNodeMenu.add(new InsertAfterNodeAction(scene, node));
        insertNodeMenu.add(new AddBranchAction(scene, node));

        JMenu insertStringMenu = new JMenu("Insert String");
        insertStringMenu.add(new InsertStringBeforeNodeAction(scene, node));
        insertStringMenu.add(new InsertStringAfterNodeAction(scene, node));
        insertStringMenu.add(new AddStringBranchAction(scene, node));

        JMenu actionMenu = new JMenu("Set Action");
        actionMenu.add(new AssignBufferAction(scene, node));
        actionMenu.add(new AssignPublishAction(scene, node));
        actionMenu.add(new AssignEventAction(scene, node));
        actionMenu.add(new AssignErrorAction(scene, node));
        actionMenu.addSeparator();
        actionMenu.add(new ClearAction(scene, node));

        JPopupMenu popup = new JPopupMenu();
        popup.add(insertNodeMenu);
        popup.add(insertStringMenu);
        popup.add(actionMenu);
        popup.add(setNameAction);
        popup.addSeparator();
        popup.add(new ChoiceAction("Delete",
            deleteSourceAction,
            deleteSinkAction,
            deleteNodeWithEmptyOutgoingEdges,
            deleteNodeWithEmptyIncomingEdges
        ));

        return popup;
    }
    
    private Action setNameAction = new AbstractAction("Set Name..."){
        @Override
        public void actionPerformed(ActionEvent ae){
            String name = JOptionPane.showInputDialog(scene.getView(), "Name", node.name);
            if(name!=null){
                Node clashingNode = scene.getRule().nodeWithName(name);
                if(clashingNode!=null && clashingNode!=node){
                    JOptionPane.showMessageDialog(scene.getView(), "Node with name '"+name+"' already exists");
                    return;
                }
                node.name = name.isEmpty() ? null : name;
                scene.layout(node);
            }
        }
    };

    private Action deleteSourceAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent ae){
            scene.getRule().node = newSource();
            for(Edge edge: node.outgoing())
                edge.delete();
            scene.refresh();
        }

        private Node newSource(){
            if(scene.getRule().node==node){
                Set<Node> targets = new HashSet<Node>();
                for(Edge outgoing: node.outgoing){
                    if(!outgoing.loop())
                        targets.add(outgoing.target);
                }

                Node newSource = null;
                for(Node target: targets){
                    boolean canBeSource = true;
                    for(Edge incoming: target.incoming){
                        if(!incoming.loop() && incoming.source!=node){
                            canBeSource = false;
                            break;
                        }
                    }
                    if(canBeSource){
                        if(newSource!=null) // multiple sources
                            return null;
                        newSource = target;
                    }
                }
                return newSource;
            }else
                return null;
        }

        @Override
        public boolean isEnabled(){
            return newSource()!=null;
        }
    };
    
    private Action deleteSinkAction = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent ae){
            for(Edge edge: node.incoming())
                edge.delete();
            scene.refresh();
        }

        @Override
        public boolean isEnabled(){
            if(scene.getRule().node==node)
                return false;

            for(Edge edge: node.outgoing){
                if(!edge.loop())
                    return false;
            }
            return true;
        }
    };

    private Action deleteNodeWithEmptyOutgoingEdges = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent ae){
            for(Edge incoming: node.incoming()){
                if(!incoming.loop()){
                    for(Edge outgoing: node.outgoing()){
                        if(!outgoing.loop()){
                            Edge newEdge = incoming.source.addEdgeTo(outgoing.target);
                            newEdge.matcher = incoming.matcher;
                            newEdge.ruleTarget = incoming.ruleTarget;
                            incoming.delete();
                        }
                    }
                }
            }
            for(Edge outgoing: node.outgoing()){
                if(!outgoing.loop())
                    outgoing.setTarget(null);
            }
            scene.refresh();
        }

        @Override
        public boolean isEnabled(){
            if(scene.getRule().node==node)
                return false;
            
            for(Edge edge: node.outgoing){
                if(!edge.loop()){
                    if(edge.matcher!=null || edge.ruleTarget!=null)
                        return false;
                }
            }
            return true;
        }
    };
    
    private Action deleteNodeWithEmptyIncomingEdges = new AbstractAction(){
        @Override
        public void actionPerformed(ActionEvent ae){
            for(Edge outgoing: node.outgoing()){
                if(!outgoing.loop()){
                    for(Edge incoming: node.incoming()){
                        if(!incoming.loop()){
                            Edge newEdge = outgoing.target.addEdgeFrom(incoming.source);
                            newEdge.matcher = outgoing.matcher;
                            newEdge.ruleTarget = outgoing.ruleTarget;
                            outgoing.delete();
                        }
                    }
                }
            }
            for(Edge incoming: node.incoming()){
                if(!incoming.loop())
                    incoming.setSource(null);
            }
            scene.refresh();
        }

        @Override
        public boolean isEnabled(){
            if(scene.getRule().node==node)
                return false;
            
            for(Edge edge: node.incoming){
                if(!edge.loop()){
                    if(edge.matcher!=null || edge.ruleTarget!=null)
                        return false;
                }
            }
            return true;
        }
    };
}
