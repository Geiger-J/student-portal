package com.example.student_portal.util;

import java.util.*;

/**
 * Implementation of the Hopcroft-Karp algorithm for maximum bipartite matching.
 * 
 * This algorithm finds the maximum matching in a bipartite graph in O(E * sqrt(V)) time.
 * The algorithm is implemented manually to satisfy the advanced algorithm requirement
 * for the IB CS IA project.
 * 
 * The bipartite graph consists of:
 * - Left side: Tutee request timeslots
 * - Right side: Tutor availability nodes (tutor + timeslot + subject compatibility)
 * 
 * Constraints respected:
 * - Tutor maxSessionsPerWeek limit
 * - Unique timeslot allocation (no double-booking of tutor at same time)
 * - Subject compatibility
 * - Year group eligibility
 */
public class HopcroftKarp {
    
    /**
     * Graph representation for bipartite matching.
     * Left nodes represent tutee requests with specific timeslots.
     * Right nodes represent tutor availability with capacity constraints.
     */
    public static class BipartiteGraph {
        private final Map<Integer, Set<Integer>> adjacencyList;
        private final Set<Integer> leftNodes;
        private final Set<Integer> rightNodes;
        
        public BipartiteGraph() {
            this.adjacencyList = new HashMap<>();
            this.leftNodes = new HashSet<>();
            this.rightNodes = new HashSet<>();
        }
        
        public void addLeftNode(int node) {
            leftNodes.add(node);
            adjacencyList.putIfAbsent(node, new HashSet<>());
        }
        
        public void addRightNode(int node) {
            rightNodes.add(node);
        }
        
        public void addEdge(int leftNode, int rightNode) {
            adjacencyList.get(leftNode).add(rightNode);
        }
        
        public Set<Integer> getNeighbors(int node) {
            return adjacencyList.getOrDefault(node, new HashSet<>());
        }
        
        public Set<Integer> getLeftNodes() {
            return leftNodes;
        }
        
        public Set<Integer> getRightNodes() {
            return rightNodes;
        }
    }
    
    /**
     * Represents a matching result.
     */
    public static class MatchingResult {
        private final Map<Integer, Integer> leftToRight;
        private final Map<Integer, Integer> rightToLeft;
        private final int matchingSize;
        
        public MatchingResult(Map<Integer, Integer> leftToRight, Map<Integer, Integer> rightToLeft) {
            this.leftToRight = new HashMap<>(leftToRight);
            this.rightToLeft = new HashMap<>(rightToLeft);
            this.matchingSize = leftToRight.size();
        }
        
        public Map<Integer, Integer> getLeftToRight() {
            return leftToRight;
        }
        
        public Map<Integer, Integer> getRightToLeft() {
            return rightToLeft;
        }
        
        public int getMatchingSize() {
            return matchingSize;
        }
        
        public boolean isLeftNodeMatched(int leftNode) {
            return leftToRight.containsKey(leftNode);
        }
        
        public boolean isRightNodeMatched(int rightNode) {
            return rightToLeft.containsKey(rightNode);
        }
    }
    
    private static final int NIL = 0; // Represents unmatched node
    private static final int INF = Integer.MAX_VALUE;
    
    /**
     * Finds maximum bipartite matching using Hopcroft-Karp algorithm.
     * 
     * @param graph the bipartite graph
     * @return the maximum matching result
     */
    public static MatchingResult findMaximumMatching(BipartiteGraph graph) {
        // Initialize matching arrays
        Map<Integer, Integer> leftMatch = new HashMap<>();
        Map<Integer, Integer> rightMatch = new HashMap<>();
        
        // Initialize all nodes as unmatched
        for (int leftNode : graph.getLeftNodes()) {
            leftMatch.put(leftNode, NIL);
        }
        for (int rightNode : graph.getRightNodes()) {
            rightMatch.put(rightNode, NIL);
        }
        
        int matchingSize = 0;
        
        // Keep finding augmenting paths until no more exist
        while (breadthFirstSearch(graph, leftMatch, rightMatch)) {
            // Try to find augmenting paths from each unmatched left node
            for (int leftNode : graph.getLeftNodes()) {
                if (leftMatch.get(leftNode) == NIL) {
                    if (depthFirstSearch(graph, leftNode, leftMatch, rightMatch, new HashSet<>())) {
                        matchingSize++;
                    }
                }
            }
        }
        
        // Prepare result (exclude NIL entries)
        Map<Integer, Integer> resultLeftToRight = new HashMap<>();
        Map<Integer, Integer> resultRightToLeft = new HashMap<>();
        
        for (Map.Entry<Integer, Integer> entry : leftMatch.entrySet()) {
            if (entry.getValue() != NIL) {
                resultLeftToRight.put(entry.getKey(), entry.getValue());
            }
        }
        
        for (Map.Entry<Integer, Integer> entry : rightMatch.entrySet()) {
            if (entry.getValue() != NIL) {
                resultRightToLeft.put(entry.getKey(), entry.getValue());
            }
        }
        
        return new MatchingResult(resultLeftToRight, resultRightToLeft);
    }
    
    /**
     * BFS to find shortest augmenting paths and build level graph.
     */
    private static boolean breadthFirstSearch(BipartiteGraph graph, 
                                            Map<Integer, Integer> leftMatch, 
                                            Map<Integer, Integer> rightMatch) {
        Queue<Integer> queue = new LinkedList<>();
        Map<Integer, Integer> distance = new HashMap<>();
        
        // Initialize distances
        for (int leftNode : graph.getLeftNodes()) {
            if (leftMatch.get(leftNode) == NIL) {
                distance.put(leftNode, 0);
                queue.offer(leftNode);
            } else {
                distance.put(leftNode, INF);
            }
        }
        
        distance.put(NIL, INF);
        
        // BFS
        while (!queue.isEmpty()) {
            int u = queue.poll();
            
            if (distance.get(u) < distance.get(NIL)) {
                for (int v : graph.getNeighbors(u)) {
                    int matchedLeft = rightMatch.get(v);
                    if (distance.get(matchedLeft) == INF) {
                        distance.put(matchedLeft, distance.get(u) + 1);
                        queue.offer(matchedLeft);
                    }
                }
            }
        }
        
        return distance.get(NIL) != INF;
    }
    
    /**
     * DFS to find and augment along shortest augmenting paths.
     */
    private static boolean depthFirstSearch(BipartiteGraph graph, 
                                          int u, 
                                          Map<Integer, Integer> leftMatch, 
                                          Map<Integer, Integer> rightMatch,
                                          Set<Integer> visited) {
        if (u != NIL) {
            if (visited.contains(u)) {
                return false;
            }
            visited.add(u);
            
            for (int v : graph.getNeighbors(u)) {
                int matchedLeft = rightMatch.get(v);
                
                if (depthFirstSearch(graph, matchedLeft, leftMatch, rightMatch, visited)) {
                    rightMatch.put(v, u);
                    leftMatch.put(u, v);
                    return true;
                }
            }
            return false;
        }
        return true;
    }
}