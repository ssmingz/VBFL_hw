package pda.core.dependency;

import pda.common.utils.JavaFile;
import pda.core.dependency.dependencyGraph.DependencyGraph;
import pda.core.dependency.dependencyGraph.DependencyGraphVertex;
import pda.core.dependency.dependencyGraph.TempVertex;
import pda.core.dependency.dependencyGraph.VariableVertex;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DependencyMain2 {
    public static void main(String[] args) {
        DependencyParser2 parser = new DependencyParser2();
        //set the default path2dir in Constant.java before use parser!

//        parser.parse("Dependency.txt");// use this while the dependencyfile exists.
//        DependencyGraph dg =  parser.getDependencyGraph();
//        DependencyGraphVertex vertex1 = new TempVertex("/home/liu/桌面/Projects/MyPass/test.cpp", "_Z3addRiPPi", 38, "[c,i32 0]", 11, 0);
//        DependencyGraphVertex vertex2 = new VariableVertex("/home/liu/桌面/Projects/MyPass/test.cpp", "_Z3addRiPPi", 46, "c", 16);
//        System.out.println(parser.getDependencyTrace(vertex1, vertex2));
//        System.out.println(parser.isEquivalent(vertex1, vertex2));

        // use this while the source file "test.cpp" is known. "test.bc" is the target file to be output
//        parser.parse("libDependencyPass.so", "test.bc", "test.cpp");

        parser.parse("/home/liumengjiao/Desktop/graphs/1/graph/_Z30promote_first_timestamp_columnP4ListI12Create_fieldE.txt");
        DependencyGraph dg =  parser.getDependencyGraph();
        StringBuilder stringBuilder = new StringBuilder();
        for (String id1 : dg.getVertexes().keySet()) {
            DependencyGraphVertex vertex1 = dg.getVertexes().get(id1);
            for (String id2 :dg.getVertexes().keySet()) {
                DependencyGraphVertex vertex2 = dg.getVertexes().get(id2);
                if (id1.contains("4163|57") && id2.contains("4167|29")) {
                    if (id1.equals(id2))
                        continue;
                    boolean flag = parser.isEquivalent(vertex1, vertex2);
                    List<String> trace = parser.getDependencyTrace(vertex1, vertex2);
                    if (trace.isEmpty())
                        continue;
                    stringBuilder.append(id1.split(":")[1] + " -> " + id2.split(":")[1] + " : ");
                    stringBuilder.append(trace).append("\n");
                }
            }
        }
        JavaFile.writeStringToFile("/home/liu/桌面/Projects/ProgramAnalysis-ldl/dependencyTrace.txt", stringBuilder.toString());
    }
}
