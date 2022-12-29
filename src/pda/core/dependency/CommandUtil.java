package pda.core.dependency;

import pda.common.conf.Constant;
import pda.common.utils.JavaFile;
import pda.common.utils.Pair;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommandUtil {
    static String fileSeperatorInDependency = "/";

    public static List<Pair<String[], String[]>> readDependency(String dependencyFileName){
        String path = Constant.DEPENDENCY_DIR + File.separator + dependencyFileName;
        return parseDependency(JavaFile.readFileToStringList(path));
    }

    public static List<Map<String[], List<String[]>>> readVertexEdge(String dependencyFileName){
        String path = Constant.DEPENDENCY_DIR + File.separator + dependencyFileName;
        path = dependencyFileName;
        return parseedge(JavaFile.readFileToStringList(path));
    }

    public static List<Map<String[], List<String[]>>> getDependency(String passPath, String irFilePath, String sourceFilePath) {
        StringBuilder stringBuilder = new StringBuilder();
        passPath = Constant.PASS_DIR + File.separator + passPath;
        irFilePath = Constant.DEPENDENCY_DIR + File.separator + irFilePath;
        sourceFilePath = Constant.DEPENDENCY_DIR + File.separator + sourceFilePath;
        //clang-12 -O0 -g -emit-llvm -fprofile-generate -fsyntax-only -S test.c -o test.bc
        //opt -load libDependencyPass.so -world test.c -o test_new.bc
        stringBuilder.append("clang-12 -O0 -g -emit-llvm -fprofile-generate -fsyntax-only -S " + sourceFilePath + " -o " + irFilePath);
        stringBuilder.append(" && ").append("opt -load " + passPath + " -world " + irFilePath);
        String[] strings = new String[] {"/bin/bash", "-c", stringBuilder.toString()};
        //make sure execute command when all its dependencies can be found by clang!
        //empty result obtained when execution error occured, this lead to error before gengraph.
        List<String> result = execute(strings);
        return parseedge(result);
    }

    private static List<Pair<String[], String[]>> parseDependency(List<String> result) {
        List<Pair<String[], String[]>> dependencyList = new ArrayList<>();
        for (String dependency : result) {
            String[] nodes = dependency.split("=>");
            String[] first;
            String[] second;
            first = splitStr(nodes[0]);
            second = splitStr(nodes[1]);
            dependencyList.add(new Pair<>(first, second));
        }
        return dependencyList;
    }

    private static List<Map<String[], List<String[]>>> parseedge(List<String> result) {
        List<Map<String[], List<String[]>>> vertexes = new ArrayList<>();
        Map<String[], List<String[]>> map = null;
        String[] vertex = null;
//        String regex = "^[0-9]+:::[0-9]+:::\\s+store";
//        Pattern p = Pattern.compile(regex);
//        Matcher matcher = null;
        boolean isStoreDefuse = false;
        for (int i = 0; i < result.size(); i ++) {
            if (result.get(i).equals("")) {
                vertexes.add(map);
            } else if (result.get(i).equals("#Vertex")) {
                map = new HashMap<>();
                i++;
//                matcher = p.matcher(result.get(i));
//                if (matcher.find())
//                    isStoreDefuse = true;
                List<String> vertexlist = new ArrayList<>();
                i = getVertex(i, result, vertexlist);
                vertex = vertexlist.toArray(new String[vertexlist.size()]);
//                vertex = splitVertex(result.get(i));
                map.put(vertex, new ArrayList<>());
            } else if (!result.get(i).equals("#Edge")){
//                String[] edgevertex = splitVertex(result.get(i));
//                if (isStoreDefuse && edgevertex[6].equals("from") && edgevertex[7].equals("def-use"))
//                    continue;
                List<String> vertexlist = new ArrayList<>();
                i = getVertex(i, result, vertexlist);
                String[] edgevertex = vertexlist.toArray(new String[vertexlist.size()]);
                map.get(vertex).add(edgevertex);
            }
        }
        return vertexes;
    }

    private static int getVertex(int start, List<String> result, List<String> vertex) {
        /*
#Vertex
value##  %13 = sext i32 %12 to i64, !dbg !4142
irtype##Variable
mthname##_Z4loopv
nodetype##int
location##26|9
valuename##i
absFilename##/home/liu/桌面/Projects/MyPass/test.cpp
#Edge
from##def-use
value##  %12 = load i32, i32* %4, align 4, !dbg !4140
irtype##Variable
mthname##_Z4loopv
nodetype##int
location##26|11
valuename##i
absFilename##/home/liu/桌面/Projects/MyPass/test.cpp
        * */
        int i = start;
        for (; i < result.size(); i++) {
            String temp = result.get(i);
            if (temp.startsWith("from##") || temp.startsWith("to##")) {
                vertex.add(0, temp.split("##")[0]);
            }
            vertex.add(0, temp.split("##")[1]);
            if (temp.startsWith("absFilename##")) {
                break;
            }
        }
        return i;
    }

//    private static String[] splitVertex(String vertex) {
//        //%11 = load i32, i32* %4, align 4, !dbg !23-=Variable:main:int:28|17:a:/home/liu/桌面/Projects/MyPass/testa.c
//        List<String> split = new ArrayList<>();
//        if (!vertex.contains("##")) {
//            String[] temp = vertex.split(":::");
//            assert temp.length > 2;
//            String[] node = temp[2].split("-=")[1].split(":");
//            assert node.length >= 5;
//            split.addAll(Arrays.asList(node));
//        } else {
//            String[] temp = vertex.split("##");
//            assert temp.length > 2;
//            String[] temp2 = temp[2].split(":::");
//            assert temp2.length > 2;
//            String[] node = temp2[2].split("-=")[1].split(":");
//            assert node.length >= 5;
//            split.addAll(Arrays.asList(node));
//            split.add(temp[0]);
//            split.add(temp[1]);
//        }
//        return split.toArray(new String[split.size()]);
//    }

    private static String[] splitStr(String node) {
        if (node.contains("#INSTRUCTION")) {
            String[] instruction = new String[6];
            instruction[0] = node.split(":")[0];
            instruction[1] = instruction[2] = "";
            instruction[3] = instruction[4] = "0";
            instruction[5] = node.split(":")[1].substring(0, node.split(":")[1].indexOf("#INSTRUCTION"));
            return instruction;
        }
        List<String> strs = new ArrayList<>();
        String[] infos = node.split(":");
        if (infos.length != 4) {
            System.err.println("The DependencyFile has incorrect format!");
            return null;
        }
        strs.add(infos[0]);
        strs.add(infos[1] + fileSeperatorInDependency + infos[2]);
        List<String> temp = Arrays.asList(infos[3].split("_"));
        strs.addAll(temp);
        if (temp.size() == 3)
            strs.add("");
        return strs.toArray(new String[strs.size()]);
    }

    private static List<String> execute(String[] command) {
        Process process = null;
        final List<String> message = new ArrayList<String>();
        try {
            ProcessBuilder builder = new ProcessBuilder(command);
//            builder.redirectErrorStream(true);
            process = builder.start();
            final InputStream inputStream = process.getInputStream();

            Thread processReader = new Thread(){
                public void run() {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    String line;
                    try {
                        while((line = reader.readLine()) != null) {
                            message.add(line);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    try {
                        reader.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };

            processReader.start();
            try {
                processReader.join();
                process.waitFor();
            } catch (InterruptedException e) {
                return new LinkedList<>();
            }
        } catch (IOException e) {
        } finally {
            if (process != null) {
                process.destroy();
            }
            process = null;
        }

        return message;
    }

}
