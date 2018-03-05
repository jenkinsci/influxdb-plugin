package jenkinsci.plugins.influxdb.models;
 
public class Target {
 
    String description;
    String url;
    String username;
    String password;
    String database;
    String retentionPolicy;
    boolean exposeExceptions;
    boolean globalListener;
    String globalListenerFilter;

    public Target(){
        //nop
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

    public boolean isExposeExceptions() {
        return exposeExceptions;
    }

    public void setExposeExceptions(boolean exposeExceptions) {
        this.exposeExceptions = exposeExceptions;
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
        return "[" +
                "description='" + description + '\'' +
                ", url='" + url + '\'' +
                ", database='" + database + '\'' +
                ", retentionPolicy='" + retentionPolicy + '\'' +
                ", exposeExceptions=" + exposeExceptions +
                ", globalListener=" + globalListener +
                ", globalListenerFilter='" + globalListenerFilter + '\'' +
                ']';
    }
}
