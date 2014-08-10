package com.codepurls.mailytics.api.v1.transfer;

import java.util.Set;

public class Graph {
  public static class Node {
    public String name;
    public int outlinks;
    public int inlinks;

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((name == null) ? 0 : name.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Node other = (Node) obj;
      if (name == null) {
        if (other.name != null) return false;
      } else if (!name.equals(other.name)) return false;
      return true;
    }

    public static Node of(String name) {
      Node n = new Node();
      n.name = name;
      return n;
    }
  }

  public static class Edge {
    public Node  src, dest;
    public float weight;

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((dest == null) ? 0 : dest.hashCode());
      result = prime * result + ((src == null) ? 0 : src.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      Edge other = (Edge) obj;
      if (dest == null) {
        if (other.dest != null) return false;
      } else if (!dest.equals(other.dest)) return false;
      if (src == null) {
        if (other.src != null) return false;
      } else if (!src.equals(other.src)) return false;
      return true;
    }
  }
  public Set<Node> nodes;
  public Set<Edge> edges;
}
