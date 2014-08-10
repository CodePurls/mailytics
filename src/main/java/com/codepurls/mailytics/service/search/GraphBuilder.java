package com.codepurls.mailytics.service.search;

import java.util.HashMap;
import java.util.Map;

import com.codepurls.mailytics.api.v1.transfer.Graph;
import com.codepurls.mailytics.api.v1.transfer.Graph.Edge;
import com.codepurls.mailytics.api.v1.transfer.Graph.Node;

public class GraphBuilder {

  public Map<Node, Node> nodes = new HashMap<>();
  public Map<Edge, Edge> edges = new HashMap<>();

  public void addNode(Node node) {
    nodes.put(node, node);
  }

  public void addEdge(Node src, Node dest, float weight) {
    Node existingSrc = nodes.get(src);
    if (existingSrc == null) {
      existingSrc = src;
      nodes.put(existingSrc, existingSrc);
    }
    existingSrc.outlinks++;
    Node existingDest = nodes.get(dest);
    if (existingDest == null) {
      existingDest = dest;
      nodes.put(existingDest, existingDest);
    }
    existingDest.inlinks++;
    Edge e = new Edge();
    e.src = src;
    e.dest = dest;
    e.weight = weight;
    Edge existingEdge = edges.get(e);
    if (existingEdge != null) {
      e = existingEdge;
      e.weight++;
    }
    edges.put(e, e);
  }

  public Graph build() {
    Graph g = new Graph();
    g.edges = edges.keySet();
    g.nodes = nodes.keySet();
    return g;
  }
}
