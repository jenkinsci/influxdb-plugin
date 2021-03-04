package jenkinsci.plugins.influxdb.models;

import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernameListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

public class Target extends AbstractDescribableImpl<Target> implements java.io.Serializable {

    private String description;
    private String url;
    private String credentialsId;
    private String database;
    private String retentionPolicy;
    private boolean jobScheduledTimeAsPointsTimestamp;
    private boolean exposeExceptions;
    private boolean usingJenkinsProxy;
    private boolean globalListener;
    private String globalListenerFilter;

    public Target() {
        //nop
    }

    @DataBoundConstructor
    public Target(String description, String url, String credentialsId, String database,
            String retentionPolicy, boolean jobScheduledTimeAsPointsTimestamp, boolean exposeExceptions,
            boolean usingJenkinsProxy, boolean globalListener, String globalListenerFilter) {
        this.description = description;
        this.url = url;
        this.credentialsId = credentialsId;
        this.database = database;
        this.retentionPolicy = retentionPolicy;
        this.jobScheduledTimeAsPointsTimestamp = jobScheduledTimeAsPointsTimestamp;
        this.exposeExceptions = exposeExceptions;
        this.usingJenkinsProxy = usingJenkinsProxy;
        this.globalListener = globalListener;
        this.globalListenerFilter = globalListenerFilter;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getCredentialsId() {
        return credentialsId;
    }

    public void setCredentialsId(String credentialsId) {
        this.credentialsId = credentialsId;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getRetentionPolicy() {
        return retentionPolicy;
    }

    public void setRetentionPolicy(String retentionPolicy) {
        this.retentionPolicy = retentionPolicy;
    }

    public boolean isJobScheduledTimeAsPointsTimestamp() {
        return jobScheduledTimeAsPointsTimestamp;
    }

    public void setJobScheduledTimeAsPointsTimestamp(boolean jobScheduledTimeAsPointsTimestamp) {
        this.jobScheduledTimeAsPointsTimestamp = jobScheduledTimeAsPointsTimestamp;
    }

    public boolean isExposeExceptions() {
        return exposeExceptions;
    }

    public void setExposeExceptions(boolean exposeExceptions) {
        this.exposeExceptions = exposeExceptions;
    }

    public boolean isUsingJenkinsProxy() {
        return usingJenkinsProxy;
    }

    public void setUsingJenkinsProxy(boolean usingJenkinsProxy) {
        this.usingJenkinsProxy = usingJenkinsProxy;
    }

    public boolean isGlobalListener() {
        return globalListener;
    }

    public void setGlobalListener(boolean globalListener) {
        this.globalListener = globalListener;
    }

    public String getGlobalListenerFilter() {
        return globalListenerFilter;
    }

    public void setGlobalListenerFilter(String globalListenerFilter) {
        this.globalListenerFilter = globalListenerFilter;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("description", description)
                .append("url", url)
                .append("credentialsId", credentialsId)
                .append("database", database)
                .toString();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Target> {

        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String url,
                                                     @QueryParameter String credentialsId) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardUsernameListBoxModel()
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM,
                            Jenkins.get(),
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.fromUri(url).build())
                    .includeCurrentValue(credentialsId);
        }

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
