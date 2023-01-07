package pda.core.dependency;

import pda.common.utils.Pair;
import pda.common.utils.Utils;
import pda.core.dependency.dependencyGraph.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DependencyParser2 extends DependencyParser {

    public void parse(String passPath, String irFilePath, String sourceFilePath) {
        if (!graphPath.equals("")) {
            String graph = graphPath + File.separator + irFilePath;
            File file = new File(graph);
            if (file.exists()) {
                try {
                    dependencyGraph = (DependencyGraph) Utils.deserialize(graph);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    genGraph(CommandUtil.getDependency(passPath, irFilePath, sourceFilePath));
                    // Utils.serialize(dependencyGraph, graph);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Please set the graph save path in the config.txt");
        }
    }

    public void parse(String dependencyFilePath) {
        if (!graphPath.equals("")) {
            String graph = graphPath + File.separator + dependencyFilePath;
            File file = new File(graph);
            if (file.exists()) {
                try {
                    dependencyGraph = (DependencyGraph) Utils.deserialize(graph);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                try {
                    genGraph(CommandUtil.readVertexEdge(dependencyFilePath));
                    // Utils.serialize(dependencyGraph, graph);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            System.err.println("Please set the graph save path in the config.txt");
        }
    }

    private DependencyGraphVertex addNode(String[] node) {
        DependencyGraphVertex vertex = null;
        if (node[2].split("\\|").length > 1) {
            int line = Integer.parseInt(node[2].split("\\|")[0]);
            int column = Integer.parseInt(node[2].split("\\|")[1]);
            // %11 = load i32, i32* %4, align 4, !dbg
            // !23-Variable:main:int:28|17:a:/home/liu/桌面/Projects/MyPass/testa.c
            /*
             * from##def-use
             * value## %12 = load i32, i32* %4, align 4, !dbg !4140
             * irtype##Variable
             * mthname##_Z4loopv
             * nodetype##int
             * location##26|11
             * valuename##i
             * absFilename##/home/liu/桌面/Projects/MyPass/test.cpp
             */
            if (node[1].equals("null"))// test test.cpp
                return vertex;
            // if (line == 0 || column == 0) {
            // return vertex;
            // }
            if (node[5].equals("Expression")) {
                vertex = new TempVertex(node[0], node[4], line, node[1], column, 0);// node[1]
            } else {// if (node[0].equals("Variable"))
                vertex = new VariableVertex(node[0], node[4], line, node[1], column);
            }
            dependencyGraph.addVertex(vertex);
            return vertex;
        }
        return vertex;
    }

    private DependencyGraphVertex genNode(String[] node) {
        int line = 0, column = 0;
        if (node[2].split("\\|").length > 1) {
            try {
                line = Integer.parseInt(node[2].split("\\|")[0]);
                column = Integer.parseInt(node[2].split("\\|")[1]);
            } catch (Exception e) {
                e.printStackTrace();
            }
            Map<String, DependencyGraphVertex> vertexes = dependencyGraph.getVertexes();
            for (String key : vertexes.keySet()) {
                DependencyGraphVertex vertex = vertexes.get(key);
                int tline = 0;
                int tcolumn = 0;
                if (vertex instanceof VariableVertex) {
                    VariableVertex temp = ((VariableVertex) vertex);
                    tline = temp.getLineNo();
                    tcolumn = temp.getColNo();
                    if (line == tline && column == tcolumn) {
                        if (!node[1].equals("null") && !node[4].equals("null")
                                && (temp.getMethodName().equals("null") || temp.getVarName().equals("null"))) {
                            temp.setMethodName(node[4]);
                            temp.setVarName(node[1]);
                            vertexes.remove(key);
                            dependencyGraph.addVertex(temp);
                        }
                        return vertex;
                    }
                } else if (vertex instanceof TempVertex) {
                    TempVertex temp = ((TempVertex) vertex);
                    tline = temp.getLineNo();
                    tcolumn = temp.getColNo();
                    if (line == tline && column == tcolumn) {
                        if (!node[1].equals("null") && !node[4].equals("null")
                                && (temp.getMethodName().equals("null") || temp.getVarName().equals("null"))) {
                            temp.setMethodName(node[4]);
                            temp.setVarName(node[1]);
                            vertexes.remove(key);
                            dependencyGraph.addVertex(temp);
                        }
                        return vertex;
                    }
                }
            }
        }
        return addNode(node);
    }

    private void genGraph(List<Map<String[], List<String[]>>> vertexAEdges) {
        if (vertexAEdges.size() == 0) {
            System.out.println(
                    "Error occured before generate graph!\nCheck your compiling process, including the correctness of file path etc..");
            dependencyGraph = new DependencyGraph();
            return;
        }
        // to#def-use#21::0:: %14 = call i32 @add(i32 %11, i32 %13), !dbg
        // !26-call:main:null:28|13:null:/home/liu/桌面/Projects/MyPass/testa.c
        System.out.println("Starting generate graph....");
        long startTime = System.currentTimeMillis();
        dependencyGraph = new DependencyGraph();
        int i = 0;
        for (Map<String[], List<String[]>> vAe : vertexAEdges) {
            i++;
            DependencyGraphVertex one = null;
            for (String[] vertex : vAe.keySet()) {
                one = genNode(vertex);
                DependencyGraphVertex another = null;
                for (String[] edgevertex : vAe.get(vertex)) {
                    another = genNode(edgevertex);
                    if (one == another || one == null || another == null)
                        continue;
                    assert edgevertex.length >= 8;
                    if (edgevertex[7].equals("def-use")) {
                        if (edgevertex[8].equals("from")) {
                            dependencyGraph.addEdge(another, one, EdgeType.DEF_USE);
                        } else if (edgevertex[8].equals("to")) {
                            dependencyGraph.addEdge(one, another, EdgeType.DEF_USE);
                        }
                    } else if (edgevertex[7].equals("data1") || edgevertex[7].equals("loop data1")) {
                        if (edgevertex[8].equals("from")) {
                            dependencyGraph.addEdge(another, one, EdgeType.DATA_DEPENDENCY);
                        } else if (edgevertex[8].equals("to")) {
                            dependencyGraph.addEdge(one, another, EdgeType.DATA_DEPENDENCY);
                        }
                    } else if (edgevertex[7].equals("control")) {// ?
                        if (edgevertex[8].equals("from")) {
                            dependencyGraph.addEdge(another, one, EdgeType.CONTROL_DEPENDENCY);
                        } else if (edgevertex[8].equals("to")) {
                            dependencyGraph.addEdge(one, another, EdgeType.CONTROL_DEPENDENCY);
                        }
                    }
                }
            }
        }
        System.out.println("Finish generate graph!");
        System.out.println("Total time: " + String.valueOf((System.currentTimeMillis() - startTime) / 1000) + "s");
    }

    public boolean isEquivalent(DependencyGraphVertex vertex1, DependencyGraphVertex vertex2) {
        if (dependencyGraph.getVertexes().containsKey(vertex1.getVertexId())
                && dependencyGraph.getVertexes().containsKey(vertex2.getVertexId())) {
            vertex1 = dependencyGraph.getVertexes().get(vertex1.getVertexId());
            vertex2 = dependencyGraph.getVertexes().get(vertex2.getVertexId());
        } else if (!dependencyGraph.getVertexes().containsKey(vertex1.getVertexId())) {
            System.out.println(vertex1.getVertexId() + " is not in graph!!!");
            String line = vertex1.getVertexId().split("\\|")[0];
            String name = vertex1.getVertexId().split("-")[1];
            List<String> nearVertex = new ArrayList<>();
            for (String s : dependencyGraph.getVertexes().keySet()) {
                if (s.startsWith(line) && s.endsWith(name)) {
                    nearVertex.add(s);
                }
            }
            System.out.println("Perhaps it is :" + nearVertex);
            return false;
        } else {
            System.out.println(vertex2.getVertexId() + " is not in graph!!!");
            String line = vertex2.getVertexId().split("\\|")[0];
            String name = vertex2.getVertexId().split("-")[1];
            List<String> nearVertex = new ArrayList<>();
            for (String s : dependencyGraph.getVertexes().keySet()) {
                if (s.startsWith(line) && s.endsWith(name)) {
                    nearVertex.add(s);
                }
            }
            System.out.println("Perhaps it is :" + nearVertex);
            return false;
        }
        if (vertex1.sameVertex(vertex2)) {
            return true;
        }
        // if (!v1.getSimpleName().toString().equals(v2.getSimpleName().toString())){
        // return false;
        // }
        // 变量节点一定存在一个def-use关系 最起码是和函数参数存在def-use
        DependencyGraphVertex v1_def = vertex1, v2_def = vertex2;
        // boolean flag = true;
        // while (flag) {
        // flag = false;
        for (DependencyGraphEdge endEdge : v1_def.getEndEdges()) {
            if (endEdge.getEdgeType().equals(EdgeType.DEF_USE)) {
                v1_def = endEdge.getStartVertex();
                break;
                // flag = true;
            }
        }
        // }
        // flag = true;
        // while (flag) {
        // flag = false;
        for (DependencyGraphEdge endEdge : v2_def.getEndEdges()) {
            if (endEdge.getEdgeType().equals(EdgeType.DEF_USE)) {
                v2_def = endEdge.getStartVertex();
                break;
                // flag = true;
            }
        }
        // }

        return v1_def.sameVertex(v2_def);
    }

    // private DependencyGraphVertex addNode(String[] node) {
    // DependencyGraphVertex vertex = null;
    // if (node == null) {
    // vertex = new VariableVertex(null , null, 0, null, 0);
    // } else if (node[0].equals("Variable") || node[0].equals("DEF") ||
    // node[0].equals("USE")){
    // vertex = new VariableVertex(node[1], node[2],
    // Integer.parseInt(node[3]), node[5], Integer.parseInt(node[4]));
    // } else if (node[0].equals("Temp")) {
    // vertex = new TempVertex(node[1], node[2],
    // Integer.parseInt(node[3]), node[6], Integer.parseInt(node[4]),
    // Integer.parseInt(node[7]));
    // }
    // dependencyGraph.addVertex(vertex);
    // return vertex;
    // }
    // private DependencyGraphVertex addNode(String[] node) {
    // DependencyGraphVertex vertex = null;
    // if (node == null) {
    // vertex = new VariableVertex(null , null, 0, null, 0);
    // } else if (node[0].equals("Variable") || node[0].equals("DEF") ||
    // node[0].equals("USE")){
    // vertex = new VariableVertex(node[1], node[2],
    // Integer.parseInt(node[3]), node[5], Integer.parseInt(node[4]));
    // } else if (node[0].equals("Temp")) {
    // vertex = new TempVertex(node[1], node[2],
    // Integer.parseInt(node[3]), node[6], Integer.parseInt(node[4]),
    // Integer.parseInt(node[7]));
    // }
    // dependencyGraph.addVertex(vertex);
    // return vertex;
    // }
    //
    // private void genGraph(List<Pair<String[], String[]>> nodeslist) {
    // System.out.println("Starting generate graph....");
    // long startTime = System.currentTimeMillis();
    // dependencyGraph = new DependencyGraph();
    // for (Pair<String[], String[]> nodes: nodeslist) {
    // DependencyGraphVertex startVertex = addNode(nodes.getFirst());
    // DependencyGraphVertex endVertex = addNode(nodes.getSecond());
    // if (startVertex.sameVertex(endVertex))
    // continue;
    // // 保证所有的边都是被依赖方指向依赖方
    // if (nodes.getFirst()[0].equals("DEF") && nodes.getSecond()[0].equals("USE"))
    // dependencyGraph.addEdge(startVertex, endVertex, EdgeType.DEF_USE);
    // else//to be continue...
    // dependencyGraph.addEdge(endVertex, startVertex, EdgeType.DATA_DEPENDENCY);
    // }
    // System.out.println("Finish generate graph!");
    // System.out.println("Total time: " +
    // String.valueOf((System.currentTimeMillis() - startTime) / 1000) + "s");
    // }

}