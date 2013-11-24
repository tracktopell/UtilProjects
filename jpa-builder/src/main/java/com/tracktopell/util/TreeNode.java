/*
 * TreeNode.java
 *
 */

package com.tracktopell.util;

import java.io.PrintStream;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

/**
 *
 * @author tracktopell
 */
public class TreeNode<T> {
    T info;
    HashSet<TreeNode> childs;
    public TreeNode(T i) {
        info = i;
        childs = new HashSet<TreeNode>();
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer(this.info.toString());
        buffer .append(" {");
        int i=0;
        for(TreeNode  tr: childs){
            if(i>0)
                buffer .append(", ");
            buffer .append(tr.info.toString());
            i++;
        }
        buffer .append("}: ");
        buffer .append(i);        
        return buffer.toString();
    }
    
    public void print(PrintStream out) {
        for(TreeNode  tr: childs){
            tr.print(out);            
            out.println("");
        }
        out.println(this.toString());
    }
    
    public void buildDepthList(List<T> list) {
        for(TreeNode  tr: childs){
            tr.buildDepthList(list);            
        }
        list.add(this.info);       
    }
    
    public void add(TreeNode toAdd){
        this.childs.add(toAdd);
    }
    
    public TreeNode search(T info2Search) {
        if(info==null)
            return null;
        if(info.equals(info2Search))
            return this;
        if(childs.size()==0)
            return null;
        TreeNode  tf=null;
        for(TreeNode  tr: childs){
            tf = tr.search(info2Search);
            if(tf!=null)
                return tf;
        }
        
        return tf;
    }        
}
