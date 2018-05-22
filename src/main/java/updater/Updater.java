package updater;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Updater {

    private static void printLines(String name, InputStream ins) throws Exception {
        String line = null;
        BufferedReader in = new BufferedReader(new InputStreamReader(ins));
        while ((line = in.readLine()) != null) {
            System.out.println(name + " " + line);
        }
    }

    private static void runProcess(String command) throws Exception {
        Process pro = Runtime.getRuntime().exec(command);
        printLines(command + " stdout:", pro.getInputStream());
        printLines(command + " stderr:", pro.getErrorStream());
        pro.waitFor();
        System.out.println(command + " exitValue() " + pro.exitValue());
    }

    public static void update() {

        String[] command = {"gradle shadowjar"};
        ProcessBuilder builder = new ProcessBuilder(command);
        builder = builder.directory(new File("Weebot"));
        try {
            Process p = builder.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            runProcess("./gradlew shadowjar");
        } catch (Exception e) {
            try {
                runProcess("gradle shadowjar");
            } catch (Exception e2) {
                e.printStackTrace();
                e2.printStackTrace();
                return;
            }
        }

        try {
            runProcess("java -jar buil/libs/Weebot*");
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
