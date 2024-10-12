package io.jenkins.plugins;

import com.perforce.p4java.impl.generic.core.file.FileSpec;
import com.perforce.p4java.option.server.GetFileContentsOptions;
import com.perforce.p4java.server.IOptionsServer;
import com.perforce.p4java.server.ServerFactory;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.PersistentDescriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.*;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;


import com.perforce.p4java.core.file.IFileSpec;

@ExportedBean
public class P4Print extends SCM {

    private final String p4FileUrl;
    private final String localFile;

    @Exported
    public String getP4FileUrl() {
        return p4FileUrl;
    }

    @Exported
    public String getLocalFile() {
        return localFile;
    }

    @Override
    public PollingResult compareRemoteRevisionWith(Job<?,?> project, Launcher launcher,
                                                   FilePath workspace, TaskListener listener,
                                                   SCMRevisionState _baseline) throws IOException, InterruptedException
    {
        return new PollingResult(PollingResult.Change.SIGNIFICANT);
    }

    @Override
    public void checkout(Run<?,?> build, Launcher launcher, FilePath workspace, TaskListener listener,
                         File changelogFile, SCMRevisionState baseline) throws IOException, InterruptedException
    {

        IOptionsServer server = null;
        try {
            String p4server = ((DescriptorImpl) getDescriptor()).getP4server();
            String p4login = ((DescriptorImpl) getDescriptor()).getP4login();
            String p4password = ((DescriptorImpl) getDescriptor()).getP4password();

            server = ServerFactory.getOptionsServer(p4server, null);
            server.connect();

            server.setUserName(p4login);
            server.login(p4password);

            List<IFileSpec> fileSpecs = List.of(new FileSpec(p4FileUrl));
            GetFileContentsOptions printOptions = new GetFileContentsOptions();
            printOptions.setNoHeaderLine(true);

            InputStream printResult = server.getFileContents(fileSpecs, printOptions);

            String ws = workspace.getRemote();

            createFolderRecursively(ws);

            String toFile = Paths.get(ws, localFile).toString();

            File folder = new File(toFile);
            createFolderRecursively(folder.getParentFile().getPath());

            saveToFile(toFile, printResult, listener);

            listener.getLogger().println("P4 Print - File saved to: " + toFile);
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

    private static void saveToFile(String fileName, InputStream inputStream, TaskListener listener) throws IOException {
        FileOutputStream outputStream = new FileOutputStream(fileName);
        byte[] buffer = new byte[8192];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            outputStream.write(buffer, 0, bytesRead);
        }
        outputStream.close();
    }

    @Override
    public SCMRevisionState calcRevisionsFromBuild(Run<?,?> build, FilePath workspace, Launcher launcher,
                                                   TaskListener listener) throws IOException, InterruptedException
    {
        return null;
    }

    @DataBoundConstructor
    public P4Print(String p4FileUrl, String localFile) {
        this.p4FileUrl = p4FileUrl;
        this.localFile = localFile;
    }

    @java.lang.Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeLog();
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor implements PersistentDescriptor {

        String p4server;
        String p4login;
        String p4password;

        public String getP4server() {
            return p4server;
        }

        public String getP4login() {
            return p4login;
        }

        public String getP4password() {
            return p4password;
        }

        public void setP4server(String p4server) {
            this.p4server = p4server;
            save();
        }

        public void setP4login(String p4login) {
            this.p4login = p4login;
            save();
        }

        public void setP4password(String p4password) {
            this.p4password = p4password;
            save();
        }

        public DescriptorImpl() {
            super(P4Print.class, null);
            load();
        }

        @Override
        public String getDisplayName() {
            return "P4Print";
        }

        public String getTfExecutable() {
            return "p4p";
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws hudson.model.Descriptor.FormException {
            req.bindJSON(this, json.getJSONObject("p4print"));
            return true;
        }

        @Override
        public boolean isApplicable(final Job project) {
            return true;
        }
    }
}
