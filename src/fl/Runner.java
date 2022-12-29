package fl;

import org.apache.commons.cli.*;

import fl.instr.Instrument;
import fl.instr.InstrumentForFilter;
import fl.utils.Constant;
import fl.utils.JavaLogger;

import java.io.File;


/**
 * java -jar fl-runner.jar ${project_path} ${project_id} ${bug_id} ${slice_path} PDAtrace/PDAslice TOP_N
 * @author
 * @date 2021/7/21
 */
public class Runner {

    public static void main(String[] args) {

        Options options = options();
        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;
        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            JavaLogger.error(e.getMessage());
            formatter.printHelp("<command> -dir <arg> -name <arg> -id <arg> -slice <arg> -mode <arg> -range <arg>", options);
            System.exit(1);
        }

        Constant.PROJECT_PATH = cmd.getOptionValue("dir");
        Constant.PROJECT_ID = cmd.getOptionValue("name");
        Constant.BUG_ID = cmd.getOptionValue("id");
        Constant.SLICE_PATH = cmd.getOptionValue("slice");
        Constant.SLICER_SWITCH = cmd.getOptionValue("mode");
        Constant.TOP_N = Integer.valueOf(cmd.getOptionValue("range"));

        resetLogs();
        Instrument instr = new Instrument();
        instr.run();

    }

    private static Options options() {
        Options options = new Options();

        Option option = new Option("dir", "DirectoryOfProject", true, "The base directory of buggy projects.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("name", "ProjectName", true, "The project name to trace.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("id", "BugId", true,
                "Ids of bugs, can be 1 or 2,3,4 or 1-10, and even their combinations like 1-10,13,18");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("slice", "PathOfSlice", true,
                "The path of slice.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("mode", "ModeOfSlice", true,
                "Switch of slice, default is PDAtrace.");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("range", "RangeOfTopN", true,
                "Choice of top N, default is 10");
        option.setRequired(true);
        options.addOption(option);

        return options;
    }

    private static void resetLogs() {
        String jarPath = Runner.class.getProtectionDomain().getCodeSource().getLocation().getPath();
        String logsPath = jarPath.substring(0, jarPath.lastIndexOf("/")) + "/logs";
        File logsDir = new File(logsPath);
        File[] logList = logsDir.listFiles();
        if(logList == null) {
            return;
        }
        for(File aLog : logList) {
            aLog.delete();
        }
    }
}
