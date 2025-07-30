package com.demo.hooks;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class UpdateExpectationHook {

    private static final String LINE_BREAK = "\n";
    private static final String JSON_FILE_SUFFIX = ".json";
    private static final String VELOCITY_FILE_SUFFIX = ".velocity";
    private static final String INIT_JSON_FILE_PATH = "config/initializerJson.json";
    private static final String EXPECTATIONS_FOLDER_PATH = "expectations";

    public static void main(String[] args) {
        File[] jsonFiles = listJsonFiles(EXPECTATIONS_FOLDER_PATH);
        String expectations = generateExpectationStrings(jsonFiles);
        // 把长字符串写入initializerJson.json文件中，以供mockserver在启动的时候加载使用
        writeFile(Paths.get(INIT_JSON_FILE_PATH), expectations);
    }

    /**
     * <p>
     * 使用expectations文件夹里的json文件来生成mockserver的expectation列表。
     * 这个列表是一个大的json文件，里面的每个item就是一个expectation，都由一个json文件压缩而来。
     * </p>
     * <p>
     * 这个方法会替换json文件中的占位符{{expectationName}}，然后把多个json文件压缩而来的String添加必要的缩进空格、逗号和换行，
     * 从而生成一个更长的字符串。最终这个字符串就会被写到initializerJson.json文件中，以供mockserver在启动的时候加载使用
     * </p>
     */
    private static String generateExpectationStrings(File[] jsonFiles) {
        StringBuilder expectations = new StringBuilder();
        expectations.append("[").append(LINE_BREAK);
        for (int i = 0; i < jsonFiles.length; i++) {
            File file = jsonFiles[i];
            String content = replaceJsonTemplate(file);
            expectations.append(fillSpaces(2)).append(content);

            // append comma for non-last expectation
            if (i != jsonFiles.length - 1) {
                expectations.append(",");
            }

            expectations.append(LINE_BREAK);
        }
        expectations.append("]");
        return expectations.toString();
    }

    /**
     * 这个方法将会依次执行以下事情：
     * <ol>
     *     <li>读取json文件(例如abc.json)为一个长字符串，然后尝试查找同名的velocity文件，即abc.velocity</li>
     *     <li>如果找到abc.velocity，说明这个expectation是使用velocity模板来动态返回的。
     *         则使用abc.velocity的内容替换abc.json里的占位符{{abc}}，然后返回abc.json的文件内容</li>
     *     <li>如果没找到abc.velocity，说明这个expectation是没有使用velocity模板的，就直接返回abc.json的文件内容</li>
     * </ol>
     */
    private static String replaceJsonTemplate(File jsonFile) {
        String jsonFileContent = readFileToOneLine(jsonFile.toURI());
        String fileName = jsonFile.getName();
        String apiName = fileName.substring(0, fileName.indexOf(JSON_FILE_SUFFIX));
        String folderPath = jsonFile.getParentFile().getAbsolutePath();
        File velocityFile = new File( folderPath + File.separator + apiName + VELOCITY_FILE_SUFFIX);
        if (velocityFile.exists()) {
            String velocityFileContent = readFileToOneLine(velocityFile.toURI());
            // replace the double quotes (") to (\") for velocity scripts
            velocityFileContent = velocityFileContent.replace("\"", "\\\"");
            String placeholder = "{{" + apiName + "}}";
            jsonFileContent = jsonFileContent.replace(placeholder, velocityFileContent);
        }
        return jsonFileContent;
    }

    /**
     * 从filePath指定的路径读取文件内容，并合并多行，返回一个长的String
     */
    private static String readFileToOneLine(URI filePath) {
        StringBuilder builder = new StringBuilder();
        String content = readFile(Path.of(filePath));
        for (String line : content.split(LINE_BREAK)) {
            builder.append(line.trim());
        }
        return builder.toString();
    }

    /**
     * 从filePath读取文件，返回一个String
     */
    private static String readFile(Path filePath) {
        String content = "";
        try {
            content = Files.readString(filePath);
        } catch (IOException e) {
            String msgPattern = "Error reading file: %s, due to: ";
            System.err.println(msgPattern.formatted(filePath.toString(), e.getMessage()));
            System.exit(-1);
        }
        return content;
    }

    /**
     * 把content写入filePath代表的文件里
     */
    private static void writeFile(Path filePath, String content) {
        try {
            Files.write(filePath, content.getBytes());
        } catch (IOException e) {
            String msgPattern = "Error writing file: %s, due to: ";
            System.err.println(msgPattern.formatted(filePath.toString(), e.getMessage()));
            System.exit(-1);
        }
    }

    /**
     * 列出mockapi文件夹里的所有以.json结尾的文件，便于后续处理。
     * 因为这个mockapi文件夹里除了.json文件以外，还会有.velocity模板文件，这个方法只会处理.json文件。
     */
    private static File[] listJsonFiles(String mockapiFolder) {
        File folder = Paths.get(mockapiFolder).toFile();
        File[] jsonFiles = folder.listFiles((dir, name) -> name.endsWith(JSON_FILE_SUFFIX));
        return jsonFiles;
    }

    /**
     * 生成一个String，包含指定数量的空格
     */
    private static String fillSpaces(int num) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < num; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }
}