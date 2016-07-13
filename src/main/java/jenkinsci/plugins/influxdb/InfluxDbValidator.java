package jenkinsci.plugins.influxdb;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 *
 * @author jrajala-eficode
 * @author joachimrodrigues
 */
public class InfluxDbValidator {

    public boolean validatePortFormat(String port) {
        int portNbr = 0;
        try {
            portNbr = Integer.parseInt(port);
        } catch (Exception e) {
            return false;
        }
        return portNbr >= 1 && portNbr <= 65535;
    }

    /**
     * @param ip
     * @return whether host is present
     */
    public boolean isHostPresent(String host) {
        return host!= null && host.length() != 0;
    }

    /**
     * @param port
     * @return whether port present 
     */
    public boolean isPortPresent(String port) {
        return port!=null && port.length() != 0;
    }

    /**
     * @param description
     * @return isDescriptionPresent
     */
    public boolean isDescriptionPresent(String description) {
        return description!=null && description.length() != 0;
    }

    /**
     * @param description
     * @return isDescriptionTooLong
     */
    public boolean isDescriptionTooLong(String description) {
        return description.length() > 100;
    }
    
    /**
     * 
     * @param baseQueueName
     * @return isDatabaseNamePresent
     */
    public boolean isDatabaseNamePresent(String baseQueueName) {
        return StringUtils.isNotBlank(baseQueueName);
    }


}
