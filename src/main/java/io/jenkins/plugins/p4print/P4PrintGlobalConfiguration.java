package io.jenkins.plugins.p4print;

import hudson.Extension;
import jenkins.model.GlobalConfiguration;
import net.sf.json.JSONObject;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;
import org.kohsuke.stapler.StaplerRequest;


@Extension
public class P4PrintGlobalConfiguration extends GlobalConfiguration {


    @DataBoundConstructor
    public P4PrintGlobalConfiguration() {
        load();
    }

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

    @DataBoundSetter
    public void setP4server(String p4server) {
        this.p4server = p4server;
        save();
    }
    @DataBoundSetter
    public void setP4login(String p4login) {
        this.p4login = p4login;
        save();
    }
    @DataBoundSetter
    public void setP4password(String p4password) {
        this.p4password = p4password;
        save();
    }


    @Override
    public String getDisplayName() {
        return "P4Print Configuration";
    }

    public static P4PrintGlobalConfiguration get() {
        return GlobalConfiguration.all().get(P4PrintGlobalConfiguration.class);
    }

    @Override
    public boolean configure(StaplerRequest req, JSONObject json) {
        req.bindJSON(this, json);
        this.save();
        return true;
    }
}
