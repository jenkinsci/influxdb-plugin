/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package jenkinsci.plugins.influxdb;

/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 */
public class Server {

    String host;

    String port;

    String description;

    String user;

    String password;

    String databaseName;

    public String getHost() {
        return host;
    }

    public String getPort() {
        return port;
    }

    public String getDescription() {
        return description;
    }

    public String getUser() {
        return user;
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public String getPassword() {
        return password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public void setDescription(String description) {
        this.description = description;
    }


    public void setPassword(String password) {
        this.password = password;
    }


    public void setUser(String user) {
        this.user = user;
    }

    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }
}
