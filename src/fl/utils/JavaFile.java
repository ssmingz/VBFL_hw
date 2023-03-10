package fl.utils;

import fl.instr.visitor.MethodStmtCountVisitor;

import org.eclipse.core.resources.IFolder;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.apache.commons.lang.StringEscapeUtils;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @description: some auxiliary methods
 * @author:
 * @time: 2021/7/21 17:50
 */
public class JavaFile {
    private static String _name = "@JavaFile ";

    /**
     * read string from file
     * 
     * @param file
     * @return : list of string lines in the file
     */
    public static List<String> readFileToStringList(File file) {
        List<String> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            String line = reader.readLine();
            while (line != null) {
                result.add(line);
                line = reader.readLine();
            }
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#readFileToStringList File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#readFileToStringList IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * get the top1 method-level result as the buggy method, with para-types
     * 
     * @param path : the absolute path of method-level result file
     * @return
     */
    public static List<BuggyMethodInfo> getBuggyMethodName(String path, int topN) {
        String cline = null;
        List<String> nMethods = new ArrayList<>();
        List<BuggyMethodInfo> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(path));
            cline = reader.readLine();
            if (cline == null) {
                JavaLogger.error(_name + "#getBuggyMethodName Result file empty : " + path);
            }
            // first line is "name;suspiciousness_value"
            while (topN != 0) {
                // org.apache.commons.lang3.time$FastDateParser#parse(java.lang.String):260;0.24253562503633297
                cline = reader.readLine();
                // org.apache.commons.lang3.time$FastDateParser#parse(java.lang.String)
                String fullMethod = cline.substring(0, cline.indexOf(":"));
                if (fullMethod.equals("<clinit>()")) {
                    continue;
                }
                if (!nMethods.contains(fullMethod)) {
                    nMethods.add(fullMethod);
                    topN--;
                }
            }
            // line = reader.readLine();
            // line = line.replace("$",".");
            // line = line.replace("#", ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
        // org.apache.commons.lang3.time$FastDateParser#parse(java.lang.String)
        for (String line : nMethods) {
            if (!line.contains("(")) {
                JavaLogger.error(_name + "#getBuggyMethodName Illegal format : " + line);
                continue;
            }
            String methodName = line.substring(line.lastIndexOf("#") + 1, line.indexOf("("));
            String tmp = line.replace("#", ".");
            String className = tmp.substring(tmp.indexOf("$") + 1, tmp.indexOf('.', tmp.indexOf("$") + 1));
            line = line.replace("$", ".");
            line = line.replace("#", ".");
            // org.apache.commons.lang3.math.NumberUtils.createNumber
            String fullName = line.substring(0, line.indexOf("("));
            JavaLogger.info("buggy method name: " + fullName);
            // org.apache.commons.lang3.math.NumberUtils.createNumber(java.lang.String)
            String nameWithTypes = line;
            if (methodName.equals(className)) {
                // since constructors are named as <init> in slices
                nameWithTypes = nameWithTypes.replace(methodName + "(", "<init>(");
            }
            BuggyMethodInfo bmi = new BuggyMethodInfo(methodName, className, line, nameWithTypes);
            result.add(bmi);
        }
        return result;
        // TODO: consider constructor, e.g.
        // org.apache.commons.lang3.math.NumberUtils.<init>
        // <init>
        // String methodName = fullName.substring(fullName.lastIndexOf(".")+1);
        // org.apache.commons.lang3.math.NumberUtils
        // String fullClass = fullName.substring(0, fullName.lastIndexOf("."));
        // NumberUtils
        // String className = fullClass.substring(fullClass.lastIndexOf(".")+1);
        // if(methodName.equals(className)) {
        // since constructors are named as <init> in slices
        // nameWithTypes = nameWithTypes.replace(methodName+"(", "<init>(");
        // }
        // return nameWithTypes;
    }

    /**
     * extract lines related to the buggy method with result generated by javaslicer
     * 
     * @param slicePath
     * @param buggyMethodName
     * @return
     */
    public static List<Integer> extractLineNumberFromSliceForJavaSlicer(String slicePath, String buggyMethodName) {
        List<Integer> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(slicePath));
            String line = null;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("The dynamic slice for criterion")) {
                    result.clear();
                }
                if (line.startsWith(buggyMethodName)) {
                    String cur = line.substring(line.indexOf(":") + 1, line.indexOf(" "));
                    if (result.size() == 0 || !result.contains(Integer.valueOf(cur))) {
                        result.add(Integer.valueOf(cur));
                    }
                }
            }
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#extractLineNumberFromSliceForJavaSlicer Slice not found : " + slicePath);
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#extractLineNumberFromSliceForJavaSlicer IO exception : " + slicePath);
            e.printStackTrace();
        }
        JavaLogger.info("Finish checking - result: " + result.toString());
        return result;
    }

    /**
     * extract lines related to the buggy method with result generated by sdgSlicer
     * 
     * @param slicePath
     * @param buggyMethodNameWithType
     * @return
     */
    public static List<Integer> extractLineNumberFromSliceForSDGSlicer(String slicePath,
            String buggyMethodNameWithType) {
        List<Integer> result = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(slicePath));
            String line = null;
            Map<Integer, Set<Integer>> mapBySdgId = new LinkedHashMap<>(); // key is for sdgNodeId, value is for its
                                                                           // related lineNumbers
            String targetSdgId = null;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(" :: ")) {
                    String idPart = line.substring(0, line.indexOf(" :: "));
                    String relList = line.substring(line.indexOf(" :: ") + 4);
                    String sdgId = idPart.substring(0, idPart.indexOf("#"));
                    Set<Integer> lines = new HashSet<>();
                    // resolve
                    if (relList.startsWith("<") && relList.endsWith(">")) {
                        relList = relList.substring(1);
                        relList = relList.substring(0, relList.length() - 1);
                        String[] rels = relList.split("><");
                        for (String rel : rels) {
                            String lineNo = rel.substring(rel.indexOf("#") + 1);
                            if (!lineNo.equals("0")) {
                                lines.add(Integer.valueOf(lineNo));
                            }
                        }
                        mapBySdgId.put(Integer.valueOf(sdgId), lines);
                    }
                }
                if (line.contains(":") && !line.contains(" :: ")) {
                    String sdgId = line.substring(0, line.indexOf(":"));
                    String content = line.substring(line.indexOf(":") + 1);
                    if (content.startsWith("entry")) {
                        if (content.contains(buggyMethodNameWithType)) {
                            targetSdgId = sdgId;
                            break;
                        }
                    }
                }
            }
            if (targetSdgId == null) {
                JavaLogger.error(_name + "#extractLineNumberSliceForSDGSlicer target method not in the slice : "
                        + buggyMethodNameWithType);
            } else {
                result = new ArrayList<>(mapBySdgId.get(Integer.valueOf(targetSdgId)));
                Collections.sort(result);
            }
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#extractLineNumberFromSliceForSDGSlicer Slice not found : " + slicePath);
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#extractLineNumberFromSliceForSDGSlicer IO exception : " + slicePath);
            e.printStackTrace();
        }
        JavaLogger.info("Finish checking - result: " + result.toString());
        return result;
    }

    /**
     * extract lines related to the buggy method with result generated by slicer4J
     * 
     * @param slicePath
     * @param buggyMethodNameWithType
     * @return
     */
    public static List<Integer> extractLineNumberFromSliceForSlicer4J(String slicePath, String buggyMethodNameWithType,
            MethodStmtCountVisitor msCounter) {
        List<Integer> result = new ArrayList<>();
        int start = msCounter._start;
        int end = msCounter._end;
        String clazz = buggyMethodNameWithType.substring(0, buggyMethodNameWithType.indexOf("("));
        clazz = clazz.replaceAll("\\$", ".");
        clazz = clazz.replaceAll("#", ".");
        clazz = clazz.substring(0, clazz.lastIndexOf('.'));
        try {
            BufferedReader reader = new BufferedReader(new FileReader(slicePath));
            String line = null;
            // "org.apache.commons.math3.util.MathArrays:816"
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains(":")) {
                    String l_file = line.substring(0, line.indexOf(":"));
                    int l_lineno = Integer.valueOf(line.substring(line.indexOf(":") + 1)).intValue();
                    l_file = l_file.replaceAll("\\$", ".");
                    l_file = l_file.replaceAll("#", ".");
                    if (clazz.contains(l_file)) {
                        if (l_lineno >= start && l_lineno <= end) {
                            result.add(l_lineno);
                        }
                    }
                }
            }
            if (result.size() == 0) {
                JavaLogger.error(_name + "#extractLineNumberSliceForSlicer4J target method not in the slice : "
                        + buggyMethodNameWithType);
            } else {
                Collections.sort(result);
            }
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#extractLineNumberFromSliceForSlicer4J Slice not found : " + slicePath);
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#extractLineNumberFromSliceForSlicer4J IO exception : " + slicePath);
            e.printStackTrace();
        }
        JavaLogger.info("Finish checking - result: " + result.toString());
        return result;
    }

    /**
     * generate compilation unit from the given source code string
     * 
     * @param source
     * @param fileName
     * @return
     */
    public static CompilationUnit genASTFromSource(String source, String fileName, String dirPath) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        // parser.setEnvironment(null, null, null, true);
        String property = System.getProperty("java.class.path", ".");
        parser.setEnvironment(property.split(File.pathSeparator), new String[] { dirPath }, null, true);
        System.out.println(property);
        parser.setUnitName(fileName.substring(fileName.lastIndexOf(File.separator)));
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
        parser.setCompilerOptions(options);
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        unit.recordModifications();
        return unit;
    }

    public static CompilationUnit genASTFromSource(String source, String fileName) {
        ASTParser parser = ASTParser.newParser(AST.JLS8);
        parser.setResolveBindings(true);
        parser.setSource(source.toCharArray());
        parser.setKind(ASTParser.K_COMPILATION_UNIT);
        // parser.setEnvironment(null, null, null, true);
        String property = System.getProperty("java.class.path", ".");
        parser.setEnvironment(property.split(File.pathSeparator), null, null, true);
        System.out.println(property);
        parser.setUnitName(fileName.substring(fileName.lastIndexOf(File.separator)));
        Map<?, ?> options = JavaCore.getOptions();
        JavaCore.setComplianceOptions(JavaCore.VERSION_1_7, options);
        parser.setCompilerOptions(options);
        CompilationUnit unit = (CompilationUnit) parser.createAST(null);
        unit.recordModifications();
        return unit;
    }

    /**
     * return string from file
     * 
     * @param file
     * @return
     */

    public static String readFileToString(File file) {
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            StringBuilder builder = new StringBuilder();
            String tmp = "";
            while ((tmp = bReader.readLine()) != null) {
                builder.append(tmp + '\n');
            }
            result = builder.toString();
            bReader.close();
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#readFileToString File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#readFileToString IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * write the given content to the given file, covering instead of appending
     * 
     * @param content
     * @param file
     */
    public static void writeStringToFile(String content, File file) {
        if (file == null) {
            JavaLogger.error(_name + "#writeStringToFile Illegal arguments (file) : null");
        }
        if (content == null) {
            JavaLogger.error(_name + "#writeStringToFile Illegal arguments (content) : null");
        }
        if (!file.exists()) {
            try {
                file.getParentFile().mkdirs();
                file.createNewFile();
            } catch (IOException e) {
                JavaLogger.error(_name + "#writeStringToFile Create new file failed : " + file.getAbsolutePath());
            }
        }
        BufferedWriter bufferedWriter = null;
        try {
            bufferedWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false)));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        try {
            bufferedWriter.write(content);
            bufferedWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (bufferedWriter != null) {
                try {
                    bufferedWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * copy a file to the path
     * 
     * @param toFile   : target path
     * @param fromFile : source path
     */
    public static void copyFile(File toFile, File fromFile) {
        if (toFile.exists()) {
            JavaLogger.info(toFile.getAbsoluteFile() + " already exists, cover this old file");
            toFile.delete();
        }
        try {
            toFile.createNewFile();
        } catch (IOException e) {
            JavaLogger.error(_name + "#copyFile Create new file failed");
            e.printStackTrace();
        }
        try {
            FileInputStream is = new FileInputStream(fromFile);
            FileOutputStream fos = new FileOutputStream(toFile);
            byte[] buffer = new byte[1024];
            int len = 0;
            while ((len = is.read(buffer)) != -1) {
                fos.write(buffer, 0, len);
            }
            is.close();
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * find all related test methods from tests.csv
     * 
     * @param testsFile
     * @return
     */
    public static LinkedHashMap<String, Set<String>> readTestsCSV(File testsFile) {
        LinkedHashMap<String, Set<String>> result = new LinkedHashMap<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(testsFile));
            String line = reader.readLine();
            while ((line = reader.readLine()) != null) {
                String className = line.substring(0, line.indexOf("#"));
                String methodName = line.substring(line.indexOf("#") + 1, line.indexOf(","));
                if (!result.containsKey(className)) {
                    Set<String> valueSet = new HashSet<>();
                    valueSet.add(methodName);
                    result.put(className, valueSet);
                } else {
                    result.get(className).add(methodName);
                }
            }
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#readTestsCSV tests.csv not found : " + testsFile.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#readTestsCSV IO exception : " + testsFile.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    /**
     * transform std.log to values.csv and attrMap
     * 
     * @param logFile
     * @param csvFile
     */
    public static LinkedHashMap<String, String> log2csv(File logFile, File csvFile) {
        LinkedHashMap<String, String> attrMapRes = new LinkedHashMap<>();
        if (!logFile.exists()) {
            JavaLogger.error(_name + "#log2csv Log file not found");
            return null;
        }
        Map<String, Map<String, String>> testsValues = new LinkedHashMap<>();
        // read log to map
        String line = null;
        String testName = "";
        Map<String, String> values = new LinkedHashMap<>();
        List<String> attrs = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.length() == 0) {
                    continue;
                }
                String content = line;
                if (content.equals("PASS") || content.equals("FAIL")) {
                    // this line is a test result P/F, end of this test recording
                    values.put("P/F", content);
                    Map<String, String> tmp = new LinkedHashMap<>(values);
                    testsValues.put(testName, tmp);
                } else if (content.contains(":")) {
                    // this line is a value
                    // if(content.indexOf(":") != content.lastIndexOf(":")) { // maybe the value
                    // contains ':'
                    // JavaLogger.error(_name + "#log2csv Too many \':\' for a log data : " +
                    // content);
                    // return false;
                    // }
                    if (testName.equals("")) {
                        continue;
                    }

                    int splitIndex = -1;
                    // handle 'isNull: 0'
                    if (content.contains("isNull:  0")) {
                        content = content.replace("isNull:  0", "notNull");
                    } else if (content.contains("isNull:  1")) {
                        content = content.replace("isNull:  1", "isNull");
                    }
                    Matcher m = Constant.VALUE_REGEX_3.matcher(content);
                    if (m.find()) {
                        splitIndex = m.end();
                    } else {
                        m = Constant.VALUE_REGEX_2.matcher(content);
                        if (m.find()) {
                            splitIndex = m.end();
                        }
                    }
                    if (splitIndex == -1) {
                        System.out.println("error when resolving at " + logFile.getAbsolutePath() + " : " + content);
                        continue;
                    }

                    String attr = content.substring(0, splitIndex - 1);
                    String value = content.substring(splitIndex);
                    value = StringEscapeUtils.escapeJava(value);
                    if (!attrs.contains(attr)) {
                        attrs.add(attr);
                    }
                    values.put(attr, value);
                } else if (!content.equals("")) {
                    // this line is a test name, start of a test recording
                    if (!testName.equals("") && !values.containsKey("P/F")) {
                        // when it is AssertionError, no FAIL line generated
                        values.put("P/F", "FAIL");
                        Map<String, String> tmp = new LinkedHashMap<>(values);
                        testsValues.put(testName, tmp);
                    }
                    testName = content + "@" + count++;
                    values.clear();
                }
            }
            if (!values.isEmpty()) {
                // no FAIL line generated and meet EOF
                if (!values.containsKey("P/F")) {
                    values.put("P/F", "FAIL");
                    Map<String, String> tmp = new LinkedHashMap<>(values);
                    testsValues.put(testName, tmp);
                }
            }
            attrs.add("P/F");
            Iterator itr = testsValues.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Map<String, String>> entry = (Map.Entry<String, Map<String, String>>) itr.next();
                Map<String, String> newMap = new LinkedHashMap<>();
                for (String newAttr : attrs) {
                    String newValue = "";
                    if (entry.getValue().containsKey(newAttr)) {
                        newValue = entry.getValue().get(newAttr);
                        newValue = csvEscapeFormat(newValue);
                    }
                    newMap.put(newAttr, newValue);
                }
                entry.setValue(newMap);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write attrMap
        String attrMapPath = csvFile.getAbsolutePath().substring(0,
                csvFile.getAbsolutePath().lastIndexOf(File.separator)) + "/attrMap";
        attrMapRes = writeAttrMap(attrMapPath, attrs);
        // write map to csv
        if (!csvFile.exists()) {
            try {
                csvFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            String lineSep = System.getProperty("line.separator");
            StringBuffer str = new StringBuffer();
            FileWriter fw = new FileWriter(csvFile, false);
            // head line
            str.append("A1");
            for (int i = 2; i <= attrs.size() + 1; i++) {
                str.append("," + "A" + i);
            }
            str.append(lineSep);
            Iterator itr = testsValues.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Map<String, String>> entry = (Map.Entry<String, Map<String, String>>) itr.next();
                testName = entry.getKey();
                str.append(testName);
                Iterator itr2 = entry.getValue().entrySet().iterator();
                while (itr2.hasNext()) {
                    Map.Entry<String, String> testValues = (Map.Entry<String, String>) itr2.next();
                    str.append("," + testValues.getValue());
                }
                str.append(lineSep);
            }
            fw.write(str.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return attrMapRes;
    }

    public static LinkedHashMap<String, String> spectrum_analysis(File logFile, File result) {
        LinkedHashMap<String, String> attrMapRes = new LinkedHashMap<>();
        if (!logFile.exists()) {
            JavaLogger.error(_name + "#log2csv Log file not found");
            return null;
        }
        Map<String, Map<String, String>> testsValues = new LinkedHashMap<>();
        // read log to map
        String line = null;
        String testName = "";
        Map<String, String> values = new LinkedHashMap<>();
        List<String> attrs = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(logFile));
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.contains("new ObjectConstructor<T>(){")) {
                    line.trim();
                }
                if (line.length() == 0) {
                    continue;
                }
                String content = line;
                if (content.equals("PASS") || content.equals("FAIL")) {
                    // this line is a test result P/F, end of this test recording
                    values.put("P/F", content);
                    Map<String, String> tmp = new LinkedHashMap<>(values);
                    testsValues.put(testName, tmp);
                } else if (content.contains(":")) {
                    // this line is a value
                    if (testName.equals("")) {
                        continue;
                    }

                    int splitIndex = -1;
                    Matcher m = Constant.VALUE_REGEX_3.matcher(content);
                    if (m.find()) {
                        splitIndex = m.end();
                    } else {
                        m = Constant.VALUE_REGEX_2.matcher(content);
                        if (m.find()) {
                            splitIndex = m.end();
                        }
                    }
                    if (splitIndex == -1) {
                        System.out.println("error when resolving at " + logFile.getAbsolutePath() + " : " + content);
                        continue;
                    }

                    String attr = content.substring(0, splitIndex - 1);
                    if (!attrs.contains(attr)) {
                        attrs.add(attr);
                    }
                    values.put(attr, "1");
                } else if (!content.equals("")) {
                    // this line is a test name, start of a test recording
                    if (!testName.equals("") && !values.containsKey("P/F")) {
                        // when it is AssertionError, no FAIL line generated
                        values.put("P/F", "FAIL");
                        Map<String, String> tmp = new LinkedHashMap<>(values);
                        testsValues.put(testName, tmp);
                    }
                    testName = content + "@" + count++;
                    values.clear();
                }
            }
            if (!values.isEmpty()) {
                // no FAIL line generated and meet EOF
                if (!values.containsKey("P/F")) {
                    values.put("P/F", "FAIL");
                    Map<String, String> tmp = new LinkedHashMap<>(values);
                    testsValues.put(testName, tmp);
                }
            }
            attrs.add("P/F");
            Iterator itr = testsValues.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Map<String, String>> entry = (Map.Entry<String, Map<String, String>>) itr.next();
                Map<String, String> newMap = new LinkedHashMap<>();
                for (String newAttr : attrs) {
                    String newValue = "0";
                    if (entry.getValue().containsKey(newAttr)) {
                        newValue = entry.getValue().get(newAttr);
                    }
                    newMap.put(newAttr, newValue);
                }
                entry.setValue(newMap);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // transform map
        List<String> alltests = new ArrayList<>(testsValues.keySet());
        Map<String, Double> ochiai_by_iden = new LinkedHashMap<>();
        Map<String, List<String>> newMap = new LinkedHashMap<>();
        Iterator it = attrs.iterator();
        while (it.hasNext()) {
            List<String> covers = new ArrayList<>();
            int ef = 0, nf = 0, ep = 0;
            String iden = (String) it.next();
            for (String test : alltests) {
                String cover = testsValues.get(test).get(iden);
                covers.add(cover);
                String pf = testsValues.get(test).get("P/F");
                if (pf.equals("FAIL") && cover.equals("0")) {
                    nf++;
                } else if (pf.equals("FAIL") && cover.equals("1")) {
                    ef++;
                } else if (pf.equals("PASS") && cover.equals("1")) {
                    ep++;
                }
            }
            // compute ochiai
            if (iden.equals("P/F")) {
                continue;
            }
            double ochiai_score = (1.0 * ef) / (Math.pow(1.0 * (ef + nf) * (ef + ep), 0.5));
            newMap.put(iden, covers);
            ochiai_by_iden.put(iden, ochiai_score);
        }

        // write map to csv
        if (!result.exists()) {
            try {
                result.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            String lineSep = System.getProperty("line.separator");
            StringBuffer str = new StringBuffer();
            FileWriter fw = new FileWriter(result, false);
            Iterator itr = newMap.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, List<String>> entry = (Map.Entry<String, List<String>>) itr.next();
                String iden = entry.getKey();
                str.append(iden);
                Iterator itr2 = entry.getValue().iterator();
                while (itr2.hasNext()) {
                    String cover = (String) itr2.next();
                    str.append("\t" + cover);
                }
                str.append(lineSep);
            }
            fw.write(str.toString());
            fw.close();
            System.out.println("Finish write matrix to file " + result.getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }

        // write ochiai score file
        String scorefile = result.getAbsolutePath().substring(0, result.getAbsolutePath().lastIndexOf(File.separator))
                + "/ochiai_ranking.txt";
        Map<String, Double> sorted = new LinkedHashMap<>();
        ochiai_by_iden.entrySet().stream()
                .sorted((p1, p2) -> p2.getValue().compareTo(p1.getValue()))
                .collect(Collectors.toList()).forEach(ele -> sorted.put(ele.getKey(), ele.getValue()));
        try {
            String lineSep = System.getProperty("line.separator");
            StringBuffer str = new StringBuffer();
            FileWriter fw = new FileWriter(scorefile, false);
            Iterator itr = sorted.entrySet().iterator();
            while (itr.hasNext()) {
                Map.Entry<String, Double> entry = (Map.Entry<String, Double>) itr.next();
                str.append(entry.getKey() + "\t" + entry.getValue() + lineSep);
            }
            fw.write(str.toString());
            fw.close();
            System.out.println("Finish write ochiai_rank to file " + scorefile);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return attrMapRes;
    }

    /**
     * write attributes map to a file
     * 
     * @param attrMapPath
     * @param attrs
     */
    private static LinkedHashMap<String, String> writeAttrMap(String attrMapPath, List<String> attrs) {
        LinkedHashMap<String, String> attrMapRes = new LinkedHashMap<>();
        File attrMap = new File(attrMapPath);
        if (!attrMap.exists()) {
            try {
                attrMap.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            String lineSep = System.getProperty("line.separator");
            StringBuffer str = new StringBuffer();
            FileWriter fw = new FileWriter(attrMap, false);
            int count = 1;
            attrMapRes.put("A" + count, "test_name");
            str.append("A" + count++ + ":test_name");
            str.append(lineSep);
            for (String attr : attrs) {
                attrMapRes.put("A" + count, attr);
                str.append("A" + count++ + ":" + attr);
                str.append(lineSep);
            }
            str.append(lineSep);
            fw.write(str.toString());
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return attrMapRes;
    }

    /**
     * return a new String value adapted to csv file escape format
     * 
     * @param newValue
     * @return
     */
    private static String csvEscapeFormat(String newValue) {
        String result = newValue.replaceAll("\"", "'");
        result = result.replaceAll(" ", "_");
        if (!isNumeric(result)) {
            // if(result.contains("\"") || result.contains(",") || result.contains("\n")) {
            result = "\"" + result + "\"";
        }
        return result;
    }

    /**
     * determine whether a string can be transformed to a numeric value
     * 
     * @param str
     * @return
     */
    private static boolean isNumeric(String str) {
        if (str == null || "".equals(str)) {
            return false;
        }
        return Constant.PATTERN_NUMERIC.matcher(str).matches();
    }

    /**
     * search file in a folder
     * 
     * @param projectDir
     * @param fileName
     * @return
     */
    public static File[] searchFile(File projectDir, String fileName) {
        return SearchFile.searchFileByName(projectDir, fileName);
    }

    /**
     * modify project build.xml
     * add other dependencies
     * 
     * @param projectPath
     */
    public static void modifyBuildXML(String projectPath) {
        // check original target .java file
        JavaLogger.info("check original xml file ...");
        String originalName = projectPath + "/build-original.txt";
        String modifiedName = projectPath + "/build.xml";
        File modifiedXMLFile = new File(modifiedName);
        File originalXMLFile = new File(originalName);
        if (!modifiedXMLFile.exists()) {
            return;
        }
        if (!originalXMLFile.exists()) {
            JavaFile.copyFile(originalXMLFile, modifiedXMLFile);
        } else {
            modifiedXMLFile.delete();
            JavaFile.copyFile(modifiedXMLFile, originalXMLFile);
        }
        JavaLogger.info("modify build.xml file ...");
        // read xml file
        String content = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(modifiedXMLFile));
            String line;
            String lineSep = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                content += line + lineSep;
                if (line.contains("<path id=\"compile.classpath\">")) {
                    content += "\t\t<pathelement location=\"${d4j.workdir}/lib/log4j-1.2.17.jar\"/>" + lineSep;
                    // content += "\t\t<pathelement location=\"${basedir}/lib/log4j-1.2.17.jar\"/>"
                    // + lineSep;
                }
                if (line.contains("<path id=\"test.classpath\">")) {
                    content += "\t\t<pathelement location=\"${d4j.workdir}/lib/log4j-1.2.17.jar\"/>" + lineSep;
                    // content += "\t\t<pathelement location=\"${basedir}/lib/log4j-1.2.17.jar\"/>"
                    // + lineSep;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write xml file
        try {
            FileWriter fw = new FileWriter(modifiedXMLFile, false);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void modifyBuildGradle(String projectPath) {
        // check original target .java file
        JavaLogger.info("check original build.gradle file ...");
        String originalName = projectPath + "/build.gradle-original.txt";
        String modifiedName = projectPath + "/build.gradle";
        File modifiedGradleFile = new File(modifiedName);
        File originalGradleFile = new File(originalName);
        if (!modifiedGradleFile.exists()) {
            return;
        }
        if (!originalGradleFile.exists()) {
            JavaFile.copyFile(originalGradleFile, modifiedGradleFile);
        } else {
            modifiedGradleFile.delete();
            JavaFile.copyFile(modifiedGradleFile, originalGradleFile);
        }
        JavaLogger.info("modify build.gradle file ...");
        // read gradle file
        String content = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(modifiedGradleFile));
            String line;
            String lineSep = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                content += line + lineSep;
                if (line.equals("dependencies {")) {
                    content += "\tcompile files('lib/log4j-1.2.17.jar')" + lineSep;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write xml file
        try {
            FileWriter fw = new FileWriter(modifiedGradleFile, false);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void modifyCoreBnd(String projectPath) {
        // check original target .java file
        JavaLogger.info("check original core.bnd file ...");
        String originalName = projectPath + "/conf/mockito-core.bnd-original.txt";
        String modifiedName = projectPath + "/conf/mockito-core.bnd";
        File modifiedBndFile = new File(modifiedName);
        File originalBndFile = new File(originalName);
        if (!modifiedBndFile.exists()) {
            return;
        }
        if (!originalBndFile.exists()) {
            JavaFile.copyFile(originalBndFile, modifiedBndFile);
        } else {
            modifiedBndFile.delete();
            JavaFile.copyFile(modifiedBndFile, originalBndFile);
        }
        JavaLogger.info("modify /conf/core.bnd file ...");
        // read gradle file
        String content = "";
        try {
            BufferedReader reader = new BufferedReader(new FileReader(modifiedBndFile));
            String line;
            String lineSep = System.getProperty("line.separator");
            while ((line = reader.readLine()) != null) {
                content += line + lineSep;
                if (line.contains("Export-Package= ")) {
                    content += "\t\t\t\tauxiliary.*, \\" + lineSep;
                }
                if (line.contains("Import-Package= ")) {
                    content += "\t\t\t\tauxiliary.*, \\" + lineSep;
                    content += "\t\t\t\torg.apache.log4j.*, \\" + lineSep;
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        // write xml file
        try {
            FileWriter fw = new FileWriter(modifiedBndFile, false);
            fw.write(content);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * write the generated tree to the target file
     * 
     * @param outputPath
     * @param tree
     */
    public static void writeTreeToFile(String outputPath, String tree) {
        File treeFile = new File(outputPath);
        if (!treeFile.exists()) {
            try {
                treeFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            FileWriter fw = new FileWriter(treeFile, false);
            fw.write(tree);
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void writeObjectToFile(Object obj, String filepath) {
        File file = new File(filepath);
        FileOutputStream out;
        try {
            if (!file.exists()) {
                if (!file.getParentFile().exists()) {
                    file.getParentFile().mkdirs();
                }
                file.createNewFile();
            }
            out = new FileOutputStream(file);
            ObjectOutputStream objOut = new ObjectOutputStream(out);
            objOut.writeObject(obj);
            objOut.flush();
            objOut.close();
            JavaLogger.info("Write object to " + filepath + " successfully!");
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#writeObjectToFile " + filepath + " not found!");
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#writeObjectToFile " + "write object " + filepath + " failed!");
            e.printStackTrace();
        }
    }

    public static Object readObjectFromFile(String filepath) {
        Object temp = null;
        File file = new File(filepath);
        FileInputStream in;
        try {
            in = new FileInputStream(file);
            ObjectInputStream objIn = new ObjectInputStream(in);
            temp = objIn.readObject();
            objIn.close();
            JavaLogger.info("Read object from " + filepath + " successfully!");
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#readObjectFromFile " + filepath + " not found!");
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#readObjectFromFile " + "read object " + filepath + " failed!");
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            JavaLogger.error(_name + "#readObjectFromFile " + "read object " + filepath + " failed!");
            e.printStackTrace();
        }
        return temp;
    }

    public static String getBinClasses(String projectPath) {
        String fname = projectPath + "/dir.bin.classes";
        File file = new File(fname);
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            while ((tmp = bReader.readLine()) != null) {
                tmp = tmp.trim();
                result = tmp;
                break;
            }
            result = projectPath + "/" + result;
            bReader.close();
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#getBinClasses File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#getBinClasses IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    public static String getSrcClasses(String projectPath) {
        String fname = projectPath + "/defects4j.build.properties";
        File file = new File(fname);
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            while ((tmp = bReader.readLine()) != null) {
                if (tmp.startsWith("d4j.dir.src.classes")) {
                    tmp = tmp.trim();
                    result = tmp.substring(tmp.indexOf('=') + 1);
                    break;
                }
            }
            result = projectPath + "/" + result;
            bReader.close();
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#getSrcClasses File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#getSrcClasses IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    public static String getSrcTests(String projectPath) {
        String fname = projectPath + "/defects4j.build.properties";
        File file = new File(fname);
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            while ((tmp = bReader.readLine()) != null) {
                if (tmp.startsWith("d4j.dir.src.tests")) {
                    tmp = tmp.trim();
                    result = tmp.substring(tmp.indexOf('=') + 1);
                    break;
                }
            }
            result = projectPath + "/" + result;
            bReader.close();
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#getSrcTests File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#getSrcTests IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    public static String getModifiedClass(String projectPath) {
        String fname = projectPath + "/defects4j.build.properties";
        String cname = getTargetClassByGzoltar(projectPath);
        File file = new File(fname);
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            while ((tmp = bReader.readLine()) != null) {
                if (tmp.startsWith("d4j.classes.modified")) {
                    tmp = tmp.trim();
                    result = tmp.substring(tmp.indexOf('=') + 1);
                    if (result.contains(",")) {
                        String[] result0 = result.split(",");
                        for (String r : result0) {
                            if (cname.contains(r)) {
                                result = r;
                                break;
                            }
                        }
                    }
                    break;
                }
            }
            bReader.close();
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#getSrcClasses File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#getSrcClasses IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return result;
    }

    private static String getTargetClassByGzoltar(String projectPath) {
        String gzoltar = projectPath + "/build/sfl/txt/ochiai.ranking.csv";
        File file = new File(gzoltar);
        String result = "";
        try {
            FileReader reader = new FileReader(file);
            BufferedReader bReader = new BufferedReader(reader);
            String tmp = "";
            tmp = bReader.readLine();
            if (tmp != null) {
                tmp = bReader.readLine();
                if (tmp != null) {
                    tmp = tmp.trim();
                    result = tmp.substring(0, tmp.indexOf("#"));
                }
            }
            bReader.close();
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#getSrcClasses File not found : " + file.getAbsolutePath());
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#getSrcClasses IO exception : " + file.getAbsolutePath());
            e.printStackTrace();
        }
        result = result.replaceAll("\\$", ".");
        return result;
    }

    public static List<Integer> extractLineNumberFromTraceForPDA(String slicePath, String buggyMethodNameWithType) {
        List<Integer> result = new ArrayList<>();
        String clazz = buggyMethodNameWithType.substring(0, buggyMethodNameWithType.indexOf("("));
        String method = clazz.substring(clazz.lastIndexOf('.') + 1);
        clazz = clazz.substring(0, clazz.lastIndexOf('.'));
        String tmp = buggyMethodNameWithType.substring(buggyMethodNameWithType.indexOf("(") + 1,
                buggyMethodNameWithType.indexOf(")"));
        String[] args = new String[0];
        if (!tmp.equals("")) {
            args = tmp.split(",");
        }
        try {
            BufferedReader reader = new BufferedReader(new FileReader(slicePath));
            String line = null;
            // "1.0:org.jfree.chart.renderer.category.AbstractCategoryItemRenderer#LegendItemCollection#getLegendItems#?:?,1791,1792,1793,1795,1796,1797,1798"
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String[] parts = line.split(":");
                if (parts.length != 3) {
                    System.out.println("Error format in " + slicePath + " : " + line);
                    continue;
                }
                String[] mparts = parts[1].split("#");
                String clazz0 = mparts[0];
                if (clazz0.contains("$")) {
                    clazz0 = clazz0.replaceAll("\\$", ".");
                }
                String method0 = mparts[2];
                String[] args0 = new String[0];
                if (!mparts[3].equals("?")) {
                    mparts[3] = mparts[3].substring(2);
                    args0 = mparts[3].split(",");
                }
                boolean flag = true;
                if (clazz0.equals(clazz) && method0.equals(method) && args0.length == args.length) {
                    for (int i = 0; i < args0.length; i++) {
                        if (!args[i].contains(args0[i])) {
                            flag = false;
                            break;
                        }
                    }
                    if (flag) {
                        String lines = parts[2];
                        String[] ls = lines.split(",");
                        for (String l : ls) {
                            if (!l.equals("?")) {
                                result.add(Integer.valueOf(l));
                            }
                        }
                        break;
                    }
                }
            }
            if (result.size() == 0) {
                JavaLogger.error(_name + "#extractLineNumberFromTraceForPDA target method not in the slice : "
                        + buggyMethodNameWithType);
            } else {
                Collections.sort(result);
            }
        } catch (FileNotFoundException e) {
            JavaLogger.error(_name + "#extractLineNumberFromTraceForPDA Slice not found : " + slicePath);
            e.printStackTrace();
        } catch (IOException e) {
            JavaLogger.error(_name + "#extractLineNumberFromTraceForPDA IO exception : " + slicePath);
            e.printStackTrace();
        }
        JavaLogger.info("Finish checking - result: " + result.toString());
        return result;
    }
}
