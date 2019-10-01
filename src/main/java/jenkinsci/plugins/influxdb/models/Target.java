package jenkinsci.plugins.influxdb.models;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.util.FormValidation;
import hudson.util.Secret;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.DataBoundSetter;

public class Target extends AbstractDescribableImpl<Target> implements java.io.Serializable {

    private String description;
    private String url;
    private String username;
    private Secret password;
    private String database;
    private String retentionPolicy;
    private boolean jobScheduledTimeAsPointsTimestamp;
    private boolean exposeExceptions;
    private boolean usingJenkinsProxy;
    private boolean globalListener;
    private String globalListenerFilter;
    private DataToggles data;

    public Target() {
        //nop
    }

    @DataBoundConstructor
    public Target(String description, String url, String username, Secret password, String database,
            String retentionPolicy, boolean jobScheduledTimeAsPointsTimestamp, boolean exposeExceptions,
            boolean usingJenkinsProxy, boolean globalListener, String globalListenerFilter) {
        this.description = description;
        this.url = url;
        this.username = username;
        this.password = password;
        this.database = database;
        this.retentionPolicy = retentionPolicy;
        this.jobScheduledTimeAsPointsTimestamp = jobScheduledTimeAsPointsTimestamp;
        this.exposeExceptions = exposeExceptions;
        this.usingJenkinsProxy = usingJenkinsProxy;
        this.globalListener = globalListener;
        this.globalListenerFilter = globalListenerFilter;
        this.data = new DataToggles();
    }

    public String getDescription() {
        return description;
    }

    @DataBoundSetter
    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    @DataBoundSetter
    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    @DataBoundSetter
    public void setUsername(String username) {
        this.username = username;
    }

    public Secret getPassword() {
        return password;
    }

    @DataBoundSetter
    public void setPassword(Secret password) {
        this.password = password;
    }

    public String getDatabase() {
        return database;
    }

    @DataBoundSetter
    public void setDatabase(String database) {
        this.database = database;
    }

    public String getRetentionPolicy() {
        return retentionPolicy;
    }

    @DataBoundSetter
    public void setRetentionPolicy(String retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public boolean isJobScheduledTimeAsPointsTimestamp() {
        return jobScheduledTimeAsPointsTimestamp;
    }

    @DataBoundSetter
    public void setJobScheduledTimeAsPointsTimestamp(boolean jobScheduledTimeAsPointsTimestamp) {
        this.jobScheduledTimeAsPointsTimestamp = jobScheduledTimeAsPointsTimestamp;
    }

    public boolean isExposeExceptions() {
        return exposeExceptions;
    }

    @DataBoundSetter
    public void setExposeExceptions(boolean exposeExceptions) {
        this.exposeExceptions = exposeExceptions;
    }

    public boolean isUsingJenkinsProxy() {
        return usingJenkinsProxy;
    }

    @DataBoundSetter
    public void setUsingJenkinsProxy(boolean usingJenkinsProxy) {
        this.usingJenkinsProxy = usingJenkinsProxy;
    }

    public boolean isGlobalListener() {
        return globalListener;
    }

    @DataBoundSetter
    public void setGlobalListener(boolean globalListener) {
        this.globalListener = globalListener;
    }

    public String getGlobalListenerFilter() {
        return globalListenerFilter;
    }

    @DataBoundSetter
    public void setGlobalListenerFilter(String globalListenerFilter) {
        this.globalListenerFilter = globalListenerFilter;
    }

    public DataToggles getData() {
        return data;
    }

    @DataBoundSetter
    public void setData(DataToggles data) {
        this.data = data;
    }

    private Object readResolve() {
        // handle old config file without a "data" element
        if (data == null) {
            data = new DataToggles();
        }
        return this;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("description", description)
                .append("url", url)
                .append("username", username)
                .append("database", database)
                .toString();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Target> {

        public FormValidation doCheckDescription(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckUrl(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }

        public FormValidation doCheckDatabase(@QueryParameter String value) {
            return FormValidation.validateRequired(value);
        }
    }
}
