package pda.core.slice;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;

import pda.common.java.D4jSubject;
import pda.common.utils.JavaFile;

public class SlicerMain {
    public static MethodDeclaration _CURRENT_METHOD_DECL = null;
    public static void main(String[] args) {
        //args[0]="/home/d4jsrc";
        //args[1]=pro;
        //args[2]=""+j;
        //args[3]=String.format("/home/tracing/info/%s/%s_%d",pro,pro,j);
        //args[4]=String.format("/home/topN_traceLineNo_noOrder/%s/%s_%d/traceLineByTopN.txt",pro,pro,j);
        //args[5]=String.format("/home/topN_traceLineNo_afterSlicing/%s/%s_%d/traceLineByTopN.txt",pro,pro,j);

        String base = args[0];
        String pid = args[1];
        int bid = Integer.valueOf(args[2]).intValue();

        File oldresult = new File(args[5]);
        if (oldresult.exists()) {
            oldresult.delete();
            System.out.println(String.format("delete oldresult file : %s %d",pid,bid));
        }
        
        checkTraceLineByTopN4Slicing_withoutOrder(pid, bid);
        String traceDir = args[3];
        String tracelineList = args[4];
        String output = args[5];
        D4jSubject subject = new D4jSubject(base, pid, bid);
        Slicer slicer = new Slicer(subject, traceDir);
        List<String> newlinelist = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(tracelineList));
            String line = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String newline = "";
                String prefix = line.substring(0, line.lastIndexOf(":"));
                String clazz = line.substring(line.indexOf(":") + 1, line.indexOf("#"));
                if (clazz.contains("$"))
                    clazz = clazz.substring(0, clazz.indexOf("$"));
                String linestr = line.substring(line.lastIndexOf(":"));
                if (linestr.equals("?")) {
                    newline = newline + prefix + ":" + linestr;
                } else {
                    String[] linelist = linestr.split(",");
                    newline = newline + prefix + ":?";
                    String criterion = null;
                    _CURRENT_METHOD_DECL = null;
                    for (int i = linelist.length - 1; i > 0; i--) {
                        int line2check = Integer.valueOf(linelist[i]).intValue();
                        String projectpath = base + "/" + pid + "/" + pid + "_" + bid + "_buggy";
                        String varcounter_arg1 = projectpath;
                        String varcounter_arg2 = JavaFile.getSrcClasses(projectpath) + "/" + clazz.replaceAll("\\.", "/") + ".java";
                        int varcount = VarCounter.countVars(varcounter_arg1, varcounter_arg2, line2check);
                        if (varcount != 0) {
                            criterion = clazz + ":" + line2check;
                            break;
                        }
                    }
                    if(criterion == null) {
                        if(linelist.length-1>0) {
                            int lastline = Integer.valueOf(linelist[linelist.length-1]).intValue();
                            criterion = clazz + ":" + lastline;
                        }
                    }
                    if (criterion != null) {
                        List<String> result = slicer.slice(criterion);
                        // get paras lines
                        LinkedHashSet<Integer> plines = new LinkedHashSet();
                        if (_CURRENT_METHOD_DECL != null) {
                            CompilationUnit cu = (CompilationUnit) _CURRENT_METHOD_DECL.getRoot();
                            for(Object para : _CURRENT_METHOD_DECL.parameters()) {
                                int pline = cu.getLineNumber(((SingleVariableDeclaration) para).getStartPosition());
                                plines.add(pline);
                            }
                            for(Integer pline : plines) {
                                newline = newline + "," + pline;
                            }
                        } else {
                            System.out.println("[ERROR] no para-lines find in method : " + line + " " + pid + " " + bid);
                        }
    
                        Set<String> slices = new HashSet<>();
                        slices.addAll(plines.stream().map(x->x.toString()).collect(Collectors.toList()));
                        for (String l : result) {
                            if (l.trim().split(":")[0].equals(clazz))
                                slices.add(l.trim().split(":")[1]);
                        }
                        String crtline = criterion.substring(criterion.indexOf(':')+1);
                        if (!slices.contains(crtline)) {
                            slices.add(crtline);
                        }
                        for (String l : linestr.split(",")) {
                            if (slices.contains(l) && !newline.contains(l))
                                newline = newline + "," + l;
                        }
                    }
                }
                newlinelist.add(newline);
            }
        } catch (FileNotFoundException e) {
            System.out.println("[ERROR] traceLineByTopN.txt does not exist : " + pid + " " + bid);
            return;
        } catch (IOException e) {
            e.printStackTrace();
        }
        String resultStr = "";
        for (String nl : newlinelist) {
            resultStr = resultStr + nl + "\n";
            System.out.println(nl);
        }
        JavaFile.writeStringToFile(output, resultStr);
    }

    static void checkTraceLineByTopN4Slicing_withoutOrder(String name, int i) {
        String inpath = "/home/ochiai_method_results_topN" + "/" + name + "/" + name + "_" + i + "/topN.txt";
        String idpath = "/home/tracing/info" + "/" + name + "/" + name + "_" + i + "/identifier.txt";
        String tracepath = "/home/tracing/info" + "/" + name + "/" + name + "_" + i + "/trace.out";
        String outpath = "/home/topN_traceLineNo_noOrder" + "/" + name + "/" + name + "_" + i;
        File input = new File(inpath), idfile = new File(idpath), trace = new File(tracepath);
        if(!input.exists()) {
            System.out.println("[Error] File not exist : " + inpath);
            return;
        }
        if(!idfile.exists()) {
            System.out.println("[Error] File not exist : " + idpath);
            return;
        }
        if(!trace.exists()) {
            System.out.println("[Error] File not exist : " + tracepath);
            return;
        }
        // load identifier map
        Map<String, Integer> idmap = new HashMap<>();
        try {
            FileReader reader = new FileReader(idfile);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            while((tmp = bReader.readLine()) != null) {
                tmp = tmp.trim();
                Integer id = Integer.valueOf(tmp.substring(0,tmp.indexOf('\t')));
                String method = tmp.substring(tmp.indexOf('\t')+1);
                idmap.put(method, id);
            }
            bReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            FileReader reader = new FileReader(input);
            BufferedReader bReader = new BufferedReader(reader);
            StringBuilder builder = new StringBuilder();
            String tmp = "";
            while((tmp = bReader.readLine()) != null) {
                tmp = tmp.trim();
                double pos = Double.valueOf(tmp.substring(0,tmp.indexOf(':'))).doubleValue();
                if(pos>10.0) {
                    continue;
                }
                String method = tmp.substring(tmp.indexOf(':')+1);
                String mid = idmap.get(method).toString();
                List<Integer> lines = new ArrayList<>();
                // load trace.out
                try {
                    FileReader reader2 = new FileReader(trace);
                    BufferedReader bReader2 = new BufferedReader(reader2);
                    StringBuilder builder2 = new StringBuilder();
                    String tmp2 = "";
                    while((tmp2 = bReader2.readLine()) != null) {
                        if(!tmp2.contains("#")) {
                            continue;
                        }
                        tmp2 = tmp2.trim();
                        String mid2 = tmp2.substring(0,tmp2.indexOf('#'));
                        Integer line = Integer.valueOf(tmp2.substring(tmp2.indexOf("#")+1));
                        if(mid.equals(mid2)) {
                            lines.add(line);
                        }
                    }
                    bReader2.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                String linearr = "?";
                for(Integer l : lines) {
                    linearr += "," + l.toString();
                }
                builder.append(pos+":"+method+":"+linearr+"\n");
            }
            if(builder.length()>0 && builder.charAt(builder.length()-1)=='\n') {
                builder.deleteCharAt(builder.length()-1);
            }
            //System.out.println(builder.toString());
            File output = new File(outpath);
            output.mkdirs();
            outpath = outpath + "/traceLineByTopN.txt";
            File traceLinesByTopN = new File(outpath);
            JavaFile.writeStringToFile(builder.toString(), traceLinesByTopN);
            System.out.println("Write topN methods trace lines successfully :" + outpath);
            bReader.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
