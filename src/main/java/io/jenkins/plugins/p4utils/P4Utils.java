package io.jenkins.plugins.p4utils;

import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.exception.P4JavaException;
import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.option.server.TrustOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import hudson.model.TaskListener;
import org.jenkinsci.plugins.scriptsecurity.sandbox.whitelists.Whitelisted;

import java.io.*;
import java.net.URISyntaxException;
import java.util.List;

public class P4Utils {

    public static IOptionsServer connectToPerforce(String p4server, String username, String password) throws P4JavaException, URISyntaxException {
        if(p4server.startsWith("ssl:"))
        {
            p4server = "p4javassl://" + p4server.substring("ssl:".length());
        }

        if(!p4server.startsWith("p4java://") || !p4server.startsWith("p4javassl://"))
        {
            p4server = "p4java://" + p4server;
        }

        IOptionsServer server = ServerFactory.getOptionsServer(p4server, null);

        if(p4server.startsWith("p4javassl://")) {
            server.addTrust(new TrustOptions().setAutoAccept(true));
        }

        server.connect();

        server.setUserName(username);
        server.login(password);
        return server;
    }

    public static InputStream getP4PrintInputStream(IOptionsServer server, String filePath) throws P4JavaException {
        List<IFileSpec> fileSpecs = List.of(new FileSpec(filePath));
        GetFileContentsOptions printOptions = new GetFileContentsOptions();
        printOptions.setNoHeaderLine(true);

        InputStream printResult = server.getFileContents(fileSpecs, printOptions);
        return printResult;
    }

    public static boolean createFolderRecursively(String folderPath) {
        File folder = new File(folderPath);

        if (folder.exists()) {
            return true;
        }

        File parentFolder = folder.getParentFile();
        if (parentFolder != null && !parentFolder.exists()) {
            if (!createFolderRecursively(parentFolder.getPath())) {
                return false;
            }
        }

        if (folder.mkdir()) {
            return true;
        } else {
            return false;
        }
    }

    public static void saveToFile(String fileName, InputStream inputStream, TaskListener listener) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
    }

    public static String convertInputStreamToString(InputStream inputStream) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        String line;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line).append("\n");
            }
        }
        return stringBuilder.toString();
    }
    @Whitelisted
    public static String p4printToString(String p4FilePath){
        try {
            P4PrintGlobalConfiguration globalConfig = P4PrintGlobalConfiguration.get();
            String p4server = globalConfig.getP4server();
            String p4login = globalConfig.getP4login();
            String p4password = globalConfig.getP4password();
            IOptionsServer server = P4Utils.connectToPerforce(p4server, p4login, p4password);
            InputStream printResult = P4Utils.getP4PrintInputStream(server, p4FilePath);
            return convertInputStreamToString(printResult);
        }
        catch (Exception e)
        {

        }
        return null;
    }
}
