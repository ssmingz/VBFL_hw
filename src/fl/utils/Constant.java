package fl.utils;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.TypeDeclaration;

import java.util.*;
import java.util.regex.Pattern;

/**
 * @description: declare all constant variables
 * need to recheck GZOLTAR_RESULT_PATH
 */
public class Constant {
    private final static String _name = "@Constant ";

    public static int INSTRUMENT_COUNTER = 0;
    public static final Pattern PATTERN_NUMERIC = Pattern.compile("^-{0,1}[0-9]+([.]{1}[0-9]+){0,1}$");
    public static final Pattern DATE_REGEX = Pattern.compile("(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)(([0-9]{3}[1-9]|[0-9]{2}[1-9][0-9]{1}|[0-9]{1}[1-9][0-9]{2}|[1-9][0-9]{3})-(((0[13578]|1[02])-(0[1-9]|[12][0-9]|3[01]))|((0[469]|11)-(0[1-9]|[12][0-9]|30))|(02-(0[1-9]|[1][0-9]|2[0-8]))))|((([0-9]{2})(0[48]|[2468][048]|[13579][26])|((0[48]|[2468][048]|[3579][26])00))-02-29)");

    public static final Pattern VALUE_REGEX_2 = Pattern.compile("-[1-9][0-9]*/[1-9][0-9]*/[1-9][0-9]*:");
    public static final Pattern VALUE_REGEX_3 = Pattern.compile("-[1-9][0-9]*/[1-9][0-9]*:");
    public static final Pattern NODE_REGEX_3 = Pattern.compile(":[1-9][0-9]*\\|[0-9]*\\|[0-9]*-");
    public static final Pattern NODE_REGEX_2 = Pattern.compile(":[1-9][0-9]*\\|[0-9]*-");
    public static final Pattern NODE_REGEX_1 = Pattern.compile(":[1-9][0-9]*-");

    public static String PACKAGE_OF_INSTRAUX = null;
    public static int SOOT_ANALYSIS_COUNTER = 0;

    public static int DEBUG = 0;


    // long,native,new,package,private,protected,public,return,
    // short,static,strictfp,super,switch,synchronized,this,
    // throw,throws,transient,try,void,volatile,while

    public static final List<String> KEYWORDS = Arrays.asList(
            "abstract", "assert", "boolean", "break", "byte", "case", "catch", "char", "class",
            "continue", "default", "do", "double", "else", "enum", "extends", "final", "finally",
            "float", "for", "if", "implements", "import", "int", "interface", "instanceof",
            "long", "native", "new", "package", "private", "protected", "public", "return",
            "short", "static", "strictfp", "super", "switch", "synchronized", "this", "throw",
            "throws", "transient", "try", "void", "volatile", "while", "goto", "const", "true", "false", "null"
    );

    public static Map<String, String> PROJECT_NAME_MAP = new HashMap<String, String>(){{
        put("chart","Chart");
        put("cli","Cli");
        put("closure","Closure");
        put("codec","Codec");
        put("collections","Collections");
        put("compress","Compress");
        put("csv","Csv");
        put("gson","Gson");
        put("jacksonCore","JacksonCore");
        put("jacksonDatabind","JacksonDatabind");
        put("jacksonXml","JacksonXml");
        put("jsoup","Jsoup");
        put("jxPath","JxPath");
        put("lang","Lang");
        put("math","Math");
        put("mockito","Mockito");
        put("time","Time");
    }};

    public static Map<String, String> PROJECT_NAME_MAP2 = new HashMap<String, String>(){{
        put("chart","chart");
        put("cli","cli");
        put("closure","closure");
        put("codec","codec");
        put("collections","collections");
        put("compress","compress");
        put("csv","csv");
        put("gson","gson");
        put("jacksoncore","jacksonCore");
        put("jacksondatabind","jacksonDatabind");
        put("jacksonxml","jacksonXml");
        put("jsoup","jsoup");
        put("jxpath","jxPath");
        put("lang","lang");
        put("math","math");
        put("mockito","mockito");
        put("time","time");
    }};

    public static String D4J_HOME = "/defects4j";

    public static String ALL_TESTS_AFTER_TP = "/home/all_tests_afterPurified";

    public static String FAIL_TESTS = "/home/failed_tests_afterTP";

    public static String PROJECT_ID = "";
    public static String BUG_ID = "";
    public static String PROJECT_PATH = "";
    public static String SLICE_PATH = "";
    public static int TOP_N = 0;
    public static String SLICER_SWITCH = "";

    public static String CURRENT_CLAZZ = "";
    public static String CURRENT_METHOD = "";
    public static int CURRENT_METHOD_ID;
    public static String TOPN_METHODS_PATH;
    public static String PACKAGE_OF_INSTRAUX_TEST = null;
    public static TypeDeclaration CURRENT_CLZ_NODE = null;

    public static Map<ASTNode, ASTNode> nodeBuffer = new LinkedHashMap<>();

    /*
    public static Map<String, int[]> PROJECT_BUG_IDS = new HashMap(){{
        put("chart", new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26});
        put("cli",new int[] {1,2,3,4,5,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40});
        put("codec",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18});
        put("collections",new int[] {25,26,27,28});
        put("compress",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,31,32,33,34,35,36,37,
            38,39,40,41,42,43,44,45,46,47});
        put("csv",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16});
        put("gson",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18});
        put("jacksonCore",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26});
        put("jacksonDatabind",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,
            32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,
            66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,
            101,102,103,104,105,106,107,108,109,110,111,112});
        put("jacksonXml",new int[] {1,2,3,4,5,6});
        put("jsoup",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,
            33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,
            65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93});
        put("jxPath",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22});
        put("lang",new int[] {1,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,
            40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65});
        put("math",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,
            42,43,44,45,46,47,48,49,50,51,52,53,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,
            83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106});
        put("time",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,22,23,24,25,26,27});
        put("mockito",new int[] {1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38});
    }};
*/


    public static Map<String, int[]> PROJECT_BUG_IDS = new HashMap<String, int[]>(){{
        put("chart", new int[] {1,3,4,5,7,8,11,12,17,20,21,22});
        put("cli",new int[] {5,8,26,28,29,30,32,39});
        put("codec",new int[] {2,3,4,5,6,7,8,9,10,11});
        put("collections",new int[] {});
        put("compress",new int[] {3,4,5,6,9,12,15,41,46});
        put("csv",new int[] {3,5,8,11,16});
        put("gson",new int[] {1,4,5,6,12,15,17});
        put("jacksoncore",new int[] {1,2,3,5,6,7,8,10,12,14,15,17,18,19,21,22});
        put("jacksondatabind",new int[] {1,3,4,5,6,8,9,10,11,14,15,18,19,22,24,25,27,29,30,32,35,37,38,45,54,74,88,107});
        put("jacksonxml",new int[] {1,2,3,5});
        put("jsoup",new int[] {3,6,11,12,27,29,30,32,36,41,43,45,46,52,56,58,61,70});
        put("jxpath",new int[] {2,3,4,5,6,8,9,20,21});
        put("lang",new int[] {6,7,10,11,12,13,16,17,19,28,44,46,47,52,53,54,55,58,59,61,62,63,64});
        put("math",new int[] {19,23,24,31,32,33,38,42,43,53,60,61,64,69,76,79,85,88,94,96,97,103,105});
        put("time",new int[] {4,5,7,8,10,14,15,16,17,18,23,25});
        put("mockito",new int[] {1,2,3,6,11,12,13,17,18,19,21,22,24,25,29,30,34,35,36,37,38});
    }};


}
