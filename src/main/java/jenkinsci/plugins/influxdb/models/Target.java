package jenkinsci.plugins.influxdb.models;

import org.kohsuke.stapler.DataBoundConstructor;

public class Target implements java.io.Serializable {

    private String description;
    private String url;
    private String username;
    private String password;
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
    public Target(String description, String url, String username, String password, String database,
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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
        return "[url=" + this.url + ", description=" + this.description + ", username=" + this.username
                + ", password=*****, database=" + this.database + "]";
    }
}
