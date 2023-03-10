package pda.core.dependency;

import org.eclipse.jdt.core.dom.MethodDeclaration;
import pda.common.conf.Constant;
import pda.common.java.D4jSubject;
import pda.common.utils.Pair;
import pda.common.utils.Utils;
import pda.core.dependency.dependencyGraph.*;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class DependencyMain {

    public static void main(String[] args) {
        D4jSubject subject = new D4jSubject("E:\\lvshare\\project\\", "math", 43);
        DependencyParser dependencyParser = new DependencyParser();
        dependencyParser.parse("E://trace//math//math_43", subject);
        Map<String, DependencyGraphVertex> a =  dependencyParser.getDependencyGraph().getVertexes();
        Iterator<Map.Entry<String, DependencyGraphVertex>> iterator = a.entrySet().iterator();
//        while (iterator.hasNext()){
//            Map.Entry<String, DependencyGraphVertex> entry = iterator.next();
//            if (entry.getKey().endsWith(")")){
//                System.out.println(entry.getValue());
//            }
//        }
        DependencyGraphVertex vertex1 = new VariableVertex("org.apache.commons.math.stat.descriptive.moment.Variance", "evaluate", 404, "test(values,begin,length)", 12);
        DependencyGraphVertex vertex2 = new TempVertex("org.apache.commons.math.stat.descriptive.moment.Variance", "evaluate", 404, "Temp-135241", 12, 25);
        System.out.println(dependencyParser.getDependencyTrace(vertex1, vertex2));
        try {
            DependencyGraph dependencyGraph = (DependencyGraph) Utils.deserialize("E:/math_43.ser");
            for (String s : dependencyGraph.getVertexes().keySet()) {
                DependencyGraphVertex dependencyGraphVertex = dependencyParser.dependencyGraph.getVertexes().get(s);
                DependencyGraphVertex dependencyGraphVertex1 = dependencyGraph.getVertexes().get(s);
                System.out.println(dependencyGraph.equals(dependencyParser.dependencyGraph));
                dependencyGraph.getVertexes().remove(s);
                System.out.println(dependencyGraph.equals(dependencyParser.dependencyGraph));
//                System.out.println(DependencyGraphVertex.checkSetEqual(dependencyGraphVertex1.getEndEdges(), dependencyGraphVertex.getEndEdges()));
                break;
            }
        }catch (Exception e){
            e.printStackTrace();
        }
//        System.out.println(dependencyParser.isEquivalent(vertex1, vertex2));
    }

}
