package io.jenkins.plugins.p4print;

import com.perforce.p4java.core.IChangelist;
import com.perforce.p4java.core.file.IFileSpec;
import com.perforce.p4java.impl.generic.core.file.FilePath;
import com.perforce.p4java.option.server.ChangelistOptions;
import com.perforce.p4java.server.IOptionsServer;
import hudson.Extension;
import hudson.model.TaskListener;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.jenkinsci.plugins.workflow.steps.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.StaplerRequest;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;
import java.util.*;

public class P4FilesMatcherStep extends Step {
    private final String changeList;
    private final boolean shelve;
    private final List<String> patterns;
    private final String matchType;
    private final String matchScope;


    @DataBoundConstructor
    public P4FilesMatcherStep(String changeList, boolean shelve, List<String> patterns, String matchType, String matchScope) {
        this.changeList = changeList;
        this.shelve = shelve;
        this.patterns = new ArrayList<>(patterns);
        this.matchType = matchType;
        this.matchScope = matchScope;
    }

    @Override
    public StepExecution start(StepContext context) {
        return new P4FilesMatcherStep.Execution(context, changeList, shelve, patterns, matchType, matchScope);
    }

    public String getChangeList() {
        return changeList;
    }

    public boolean isShelve() {
        return shelve;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public String getMatchType() {
        return matchType;
    }

    public String getMatchScope() {
        return matchScope;
    }

    private static class Execution extends SynchronousStepExecution<Boolean> {
        private final String changeList;
        private final boolean shelve;
        private final List<String> patterns;
        private final String matchType;
        private final String matchScope;

        protected Execution(@Nonnull StepContext context, String changeList, boolean shelve, List<String> patterns, String matchType, String matchScope) {
            super(context);
            this.changeList = changeList;
            this.shelve = shelve;
            this.patterns = patterns;
            this.matchType = matchType;
            this.matchScope = matchScope;
        }

        @Override
        protected Boolean run() {
            IOptionsServer server = null;
            TaskListener listener = null;
            try {
                listener = getContext().get(TaskListener.class);
            }
            catch (Exception e) {}

            try {
                P4PrintGlobalConfiguration globalConfig = P4PrintGlobalConfiguration.get();
                String p4server = globalConfig.getP4server();
                String p4login = globalConfig.getP4login();
                String p4password = globalConfig.getP4password();


                server = P4Utils.connectToPerforce(p4server, p4login, p4password);
                List<IFileSpec> files;
                if(shelve) {
                    files = server.getShelvedFiles(Integer.parseInt(changeList));
                }
                else {
                    files = server.getChangelistFiles(Integer.parseInt(changeList));
                }

                switch (matchScope)
                {
                    case "any":
                        return files.stream().anyMatch(x -> isMaching(x));
                    case "all":
                        return files.stream().allMatch(x -> isMaching(x));
                    case "none":
                        return files.stream().noneMatch(x -> isMaching(x));
                }
            }
            catch (Exception e)
            {
                listener.getLogger().println("P4 Files Matcher Error: " + e.getMessage());
            }
            finally {
                if (server != null) {
                    try {
                        server.disconnect();
                    } catch (Exception e) {
                        listener.getLogger().println("P4 Files Matcher Error: " + e.getMessage());
                    }
                }
            }
            return false;
        }
        public boolean isMaching(IFileSpec file){

            switch (matchType){
                case "endsWith":
                    return patterns.stream().anyMatch(x -> file.getDepotPathString().endsWith(x));
                case "startsWith":
                    return patterns.stream().anyMatch(x -> file.getDepotPathString().startsWith(x));
                case "contains":
                    return patterns.stream().anyMatch(x -> file.getDepotPathString().contains(x));
                case "regex":
                    return patterns.stream().anyMatch(x -> Pattern.matches(x, file.getDepotPathString()));
            }
            return false;
        }
    }


    @Extension
    public static class DescriptorImpl extends StepDescriptor {


        public DescriptorImpl() {
            load();
        }

        @Override
        public String getFunctionName() {
            return "p4FilesMatcher";
        }

        @Override
        public String getDisplayName() {
            return "P4 Files Matcher";
        }

        @Override
        public Set<? extends Class<?>> getRequiredContext() {
            return Collections.emptySet();
        }

        @Override
        public boolean configure(final StaplerRequest req, final JSONObject json)
                throws hudson.model.Descriptor.FormException {
            req.bindJSON(this, json.getJSONObject("p4FilesMatcher"));
            return true;
        }

        @Override
        public Step newInstance(StaplerRequest req, JSONObject formData)
                throws FormException {

            String changeList = formData.getString("changeList");
            boolean shelve = formData.getBoolean("shelve");
            String machType = formData.getString("matchType");
            String matchScope = formData.getString("matchScope");

            List<String> patterns = new ArrayList<>();
            Object t = formData.get("patterns");
            if (t instanceof JSONObject) {
                JSONObject o = (JSONObject) t;
                patterns.add(o.getString("pattern"));
            } else if (t instanceof JSONArray) {
                JSONArray a = (JSONArray) t;
                for (int i = 0; i < a.size(); i++) {
                    JSONObject o = a.getJSONObject(i);
                    patterns.add(o.getString("pattern"));
                }
            }
            P4FilesMatcherStep step = new P4FilesMatcherStep(changeList, shelve, patterns, machType, matchScope);

            return step;
        }
    }
}
