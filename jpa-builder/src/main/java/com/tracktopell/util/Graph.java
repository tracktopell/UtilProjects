/*
 * Graph.java
 *
 */

package com.tracktopell.util;

import java.util.ArrayList;

/**
 *
 * @author tracktopell
 */
public class Graph {
    public class GraphNode{
        String name;
        ArrayList<GraphNode> from;
        GraphNode(String name) {
            this.name = name;
            this.from = new ArrayList<GraphNode>();
        }
        
        public String getName() {
            return this.name;
        }
        
        void linkedFrom(GraphNode forAdd) {
            if(!from.contains(from)) {
                from.add(forAdd);
            }
        }
        public String toString() {
            return "'"+name+"'";
        }
        
        void removeLinks() {
            from.clear();
        }
    }
    
    private ArrayList<GraphNode> nodes;
    
    public Graph() {
        this.nodes = new ArrayList<GraphNode>();
    }
    public void add(String name) {
        if(get(name)!=null)
            return;
        GraphNode forAdd=new GraphNode(name);
        this.nodes.add(forAdd);        
    }
    
    public GraphNode get(String name) {
        for(GraphNode searching: this.nodes) {
            if(searching.name.equals(name)){
                return searching;
            }
        }
        return null;    
    }
    
    public void link(String from,String to) {        
        get(to).linkedFrom(get(from));
    }
    
    public String toString(){
        StringBuffer buffer = new StringBuffer ("G={V={");
        int i=0;
        for(GraphNode nodeIter:  this.nodes) {
            if(++i > 1 )
                buffer.append(", ");
            //buffer.append(nodeIter+"["+nodeIter.from.size()+"]");
            buffer.append(nodeIter);
        }
        buffer.append("}, A={");
        
        i=0;
        for(GraphNode nodeIter:  nodes) {
            int j=0;
            StringBuffer subBuffer=new StringBuffer();
            for(GraphNode nodeLinkIter:  nodeIter.from) {
                if(++j >1)
                    subBuffer.append(", ");
                subBuffer.append("(");                
                subBuffer.append(nodeLinkIter);
                subBuffer.append(", ");                
                subBuffer.append(nodeIter);                
                subBuffer.append(")");
                j++;
            }
            if(j>0){
                if(++i > 1 )
                    buffer.append(", ");    
                buffer.append(subBuffer);
            }
        }
        buffer.append("} }");
        
        return buffer.toString();
    }
    
    public void removeLinksTo(String name) {
        GraphNode searching = get(name);        
        searching.removeLinks();
    }
    
    public void remove(String name) {
        GraphNode searching = get(name);
        searching.removeLinks();        
        int i=0;
        for(GraphNode iterNode: this.nodes) {
            if(searching == iterNode){
                this.nodes.remove(i);
                break;
            }
            i++;
        }
    }
    
    public ArrayList<GraphNode> getLonelyNodes() {
        ArrayList<GraphNode> result = new ArrayList<GraphNode>();
        for(GraphNode iterNode: this.nodes) {
            if(iterNode.from.size()==0){
                boolean linked=false;
                for(GraphNode iterNode2: this.nodes) {
                    if(iterNode != iterNode2) {
                        for(GraphNode iterNodeLinked: iterNode2.from) {
                            //System.out.print("\t\t\t-->>getLonelyNodes: ["+iterNode2+"] esta conetado desde "+iterNode+" ( Segun hay <="+iterNodeLinked+") ?");
                            if(iterNode.name.equals(iterNodeLinked.name)) {
                                //System.out.println(" --->> OK !!");
                                linked=true;
                                break;
                            } else {
                                //System.out.println("");
                            }
                        }
                        if(linked)
                            break;
                    }
                }
                if(!linked) {
                    //System.out.println("\t\t\t-->>getLonelyNodes: ["+iterNode+"] it's only linked to othres Nodes");
                    result.add(iterNode);
                }
            }
        }                
        return result;
            
    }
        
    public ArrayList<GraphNode> getDiconectedNodes() {
        ArrayList<GraphNode> result = new ArrayList<GraphNode>();
        //Hashtable<GraphNode,Boolean> isLinkedTo=new Hashtable<GraphNode,Boolean>();
        for(GraphNode iterNode: this.nodes) {
            //isLinkedTo.put(iterNode,false);
            if(iterNode.from.size()>0){
                boolean linked=false;
                for(GraphNode iterNode2: this.nodes) {
                    if(iterNode != iterNode2) {
                        for(GraphNode iterNodeLinked: iterNode2.from) {
                            //System.out.print("\t\t\t-->>getDiconectedNodes: ["+iterNode2+"] esta conetado desde "+iterNode+" ( Segun hay <="+iterNodeLinked+") ?");
                            if(iterNode.name.equals(iterNodeLinked.name)) {
                                //System.out.println(" --->> OK !!");
                                linked=true;
                                break;
                            } else {
                                //System.out.println("");
                            }
                        }
                        if(linked)
                            break;
                    }
                }
                if(!linked) {
                    //System.out.println("\t\t\t-->>getDiconectedNodes: ["+iterNode+"] it's only linked to othres Nodes");
                    result.add(iterNode);
                }
            }
        }
        
        
        return result;
    }
    
    public Graph getCopy() {
        Graph theCopy = new Graph ();
        
        for(GraphNode iterNode: this.nodes) {
            theCopy.add(iterNode.name);
        }
        for(GraphNode iterNode: this.nodes) {
            for(GraphNode iterNodeLinked: iterNode.from) {
                theCopy.link(iterNodeLinked.name,iterNode.name);
            }
        }
        return theCopy;
    }
    
    public ArrayList<GraphNode> getPathForCreation() throws Exception{
        Graph theCopy = this.getCopy();
        ArrayList<GraphNode> result = new ArrayList<GraphNode> ();
        ArrayList<GraphNode> aloneNodes = null;
        ArrayList<GraphNode> disconectedNodes = null;
        
        int numNodes = theCopy.nodes.size();
        while(numNodes > 0 ) {
            //System.out.println("\t=============>numNodes="+numNodes);
            aloneNodes = theCopy.getLonelyNodes();
            result.addAll(aloneNodes);
            //System.out.println("\t==>>aloneNodes="+aloneNodes);
            for(GraphNode iterNode: aloneNodes) {
                theCopy.remove(iterNode.name);
            }
            //----------------------------
            disconectedNodes = theCopy.getDiconectedNodes();
            result.addAll(disconectedNodes);                        
            //System.out.println("\t==>>disconectedNodes="+disconectedNodes);
            for(GraphNode iterNode: disconectedNodes) {
                theCopy.remove(iterNode.name);
            }
            
            if(numNodes == theCopy.nodes.size()){
                throw new Exception("Cycle found, can't find Nodes for add to path");
            }
            numNodes = theCopy.nodes.size();
            //System.out.println("\t\t==>numNodes="+numNodes);
        }    
        return result;
    }
}