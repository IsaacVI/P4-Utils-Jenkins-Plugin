package io.jenkins.plugins.p4utils;

import com.perforce.p4java.server.IOptionsServer;
import hudson.model.TaskListener;
import jenkins.security.MasterToSlaveCallable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;

public  class P4PrintToFileCallable extends MasterToSlaveCallable<Boolean, IOException> {
    private final String filePath;
    private final String p4FilePath;
    private final TaskListener listener;
    private final String p4server;
    private final String p4login;
    private final String p4password;

    P4PrintToFileCallable(String filePath, String p4FilePath, TaskListener listener, String p4server, String p4login, String p4password) {
        this.filePath = filePath;
        this.p4FilePath = p4FilePath;
        this.listener = listener;
        this.p4server = p4server;
        this.p4login = p4login;
        this.p4password = p4password;
    }

    @Override
    public Boolean call() throws IOException {

        IOptionsServer server = null;
        try {

            server = P4Utils.connectToPerforce(p4server, p4login, p4password);

            String ws = Paths.get(filePath).toAbsolutePath().getRoot().toString();

            P4Utils.createFolderRecursively(ws);

            File folder = new File(filePath);
            P4Utils.createFolderRecursively(folder.getParentFile().getPath());

            InputStream printResult = P4Utils.getP4PrintInputStream(server, p4FilePath);

            P4Utils.saveToFile(filePath, printResult, listener);

            listener.getLogger().println("P4 Print - File saved to: " + filePath);
            return Boolean.TRUE;
        }
        catch (Exception e)
        {
            listener.getLogger().println("P4 Print Error: " + e.getMessage());
        }
        finally {
            if (server != null) {
                try {
                    server.disconnect();
                } catch (Exception e) {
                    listener.getLogger().println("P4 Print Error: " + e.getMessage());
                }
            }
        }
        return Boolean.FALSE;
    }
}