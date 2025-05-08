package jenkinsci.plugins.influxdb.models;

import com.cloudbees.plugins.credentials.CredentialsMatchers;
import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.common.StandardCredentials;
import com.cloudbees.plugins.credentials.common.StandardListBoxModel;
import com.cloudbees.plugins.credentials.common.StandardUsernamePasswordCredentials;
import com.cloudbees.plugins.credentials.domains.URIRequirementBuilder;
import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import hudson.model.Item;
import hudson.security.ACL;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import jenkins.model.Jenkins;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.jenkinsci.plugins.plaincredentials.StringCredentials;
import org.kohsuke.stapler.AncestorInPath;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;
import org.kohsuke.stapler.verb.POST;

import java.util.List;

public class Target extends AbstractDescribableImpl<Target> implements java.io.Serializable {

    private String description;
    private String url;
    private String credentialsId;
    private String database;
    private String organization;
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
    public Target(String description, String url, String credentialsId, String organization, String database,
                  String retentionPolicy, boolean jobScheduledTimeAsPointsTimestamp, boolean exposeExceptions,
                  boolean usingJenkinsProxy, boolean globalListener, String globalListenerFilter) {
        this.description = description;
        this.url = url;
        this.credentialsId = credentialsId;
        this.organization = organization;
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

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
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
                .append("organization", organization)
                .toString();
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<Target> {

        public static StandardUsernamePasswordCredentials getUsernamePasswordCredentials(
                String credentialsId,
                String url,
                @AncestorInPath Item context
        ) {
            List<StandardUsernamePasswordCredentials> lookupCredentials = CredentialsProvider.lookupCredentialsInItem(
                    StandardUsernamePasswordCredentials.class,
                    context,
                    ACL.SYSTEM2,
                    URIRequirementBuilder.fromUri(url).build());

            return CredentialsMatchers.firstOrNull(
                    lookupCredentials,
                    CredentialsMatchers.withId(null == credentialsId ? "" : credentialsId)
            );
        }

        public static StringCredentials getTokenCredentials(String url, String credentialsId, @AncestorInPath Item context) {
            List<StringCredentials> lookupTokenCredentials = CredentialsProvider.lookupCredentialsInItem(
                    StringCredentials.class,
                    context,
                    ACL.SYSTEM2,
                    URIRequirementBuilder.fromUri(url).build());
            return CredentialsMatchers.firstOrNull(
                    lookupTokenCredentials,
                    CredentialsMatchers.withId(null == credentialsId ? "" : credentialsId)
            );
        }

        public ListBoxModel doFillCredentialsIdItems(@QueryParameter String url,
                                                     @QueryParameter String credentialsId) {
            if (!Jenkins.get().hasPermission(Jenkins.ADMINISTER)) {
                return new StandardListBoxModel().includeCurrentValue(credentialsId);
            }
            return new StandardListBoxModel()
                    .includeEmptyValue()
                    .includeAs(ACL.SYSTEM,
                            Jenkins.get(),
                            StandardUsernamePasswordCredentials.class,
                            URIRequirementBuilder.fromUri(url).build())
                    .includeAs(ACL.SYSTEM,
                            Jenkins.get(),
                            StandardCredentials.class,
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

        @POST
        public FormValidation doVerifyConnection(@QueryParameter String url, @QueryParameter String credentialsId,
                                                 @QueryParameter String organization, @QueryParameter String database,
                                                 @QueryParameter String retentionPolicy, @AncestorInPath Item context) {

            InfluxDBClientWrapper client = null;
            try {
                doCheckUrl(url);
                doCheckDatabase(database);
                Jenkins.get().checkPermission(Jenkins.ADMINISTER);
                StandardUsernamePasswordCredentials basicAuthCredentials = getUsernamePasswordCredentials(credentialsId, url, context);
                StringCredentials tokenCredentials = getTokenCredentials(url, credentialsId, context);
                client = new InfluxDBClientWrapper(
                        url,
                        organization,
                        database,
                        retentionPolicy,
                        basicAuthCredentials,
                        tokenCredentials
                );
                String apiVersion = client.getAPIVersion();
                return FormValidation.ok("Connection success (API version " + apiVersion + ")");
            } catch (Exception e) {
                return FormValidation.error(e, "Connection Failed");
            } finally {
                if (client != null) {
                    client.close();
                }
            }
        }
    }
}
