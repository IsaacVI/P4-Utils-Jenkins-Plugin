package io.jenkins.plugins.p4utils;

import com.perforce.p4java.server.IOptionsServer;
import hudson.Extension;
import hudson.FilePath;
import hudson.Launcher;
import hudson.model.Job;
import hudson.model.PersistentDescriptor;
import hudson.model.Run;
import hudson.model.TaskListener;
import hudson.scm.*;
import io.jenkins.plugins.p4utils.ChangeLog;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import org.kohsuke.stapler.export.Exported;
import org.kohsuke.stapler.export.ExportedBean;

import java.io.*;
import java.nio.file.Paths;

@ExportedBean
public class P4Print extends SCM {

    private final String p4FilePath;
    private final String localFile;

    @Exported
    public String getP4FilePath() {
        return p4FilePath;
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
            P4PrintGlobalConfiguration globalConfig = P4PrintGlobalConfiguration.get();
            String p4server = globalConfig.getP4server();
            String p4login = globalConfig.getP4login();
            String p4password = globalConfig.getP4password();

            server = P4Utils.connectToPerforce(p4server, p4login, p4password);

            String ws = workspace.getRemote();

            P4Utils.createFolderRecursively(ws);

            String toFile = Paths.get(ws, localFile).toString();

            File folder = new File(toFile);
            P4Utils.createFolderRecursively(folder.getParentFile().getPath());

            InputStream printResult = P4Utils.getP4PrintInputStream(server, p4FilePath);

            P4Utils.saveToFile(toFile, printResult, listener);

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



    @Override
    public SCMRevisionState calcRevisionsFromBuild(Run<?,?> build, FilePath workspace, Launcher launcher,
                                                   TaskListener listener) throws IOException, InterruptedException
    {
        return null;
    }

    @DataBoundConstructor
    public P4Print(String p4FilePath, String localFile) {
        this.p4FilePath = p4FilePath;
        this.localFile = localFile;
    }

    @java.lang.Override
    public ChangeLogParser createChangeLogParser() {
        return new ChangeLog();
    }

    @Extension
    public static class DescriptorImpl extends SCMDescriptor implements PersistentDescriptor {

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
