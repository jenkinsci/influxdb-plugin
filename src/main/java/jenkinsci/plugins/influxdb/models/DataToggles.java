package jenkinsci.plugins.influxdb.models;

import hudson.Extension;
import hudson.model.AbstractDescribableImpl;
import hudson.model.Descriptor;
import jenkinsci.plugins.influxdb.generators.*;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.DataBoundSetter;

public class DataToggles extends AbstractDescribableImpl<DataToggles> implements java.io.Serializable {

    /** @see CoberturaPointGenerator */
    private boolean cobertura = true;

    /** @see RobotFrameworkPointGenerator */
    private boolean robotFramework = true;

    /** @see JacocoPointGenerator */
    private boolean jacoco = true;

    /** @see PerformancePointGenerator */
    private boolean performance = true;

    /** @see SonarQubePointGenerator */
    private boolean sonarqube = true;

    /** @see ChangeLogPointGenerator */
    private boolean changelog = true;

    /** @see PerfPublisherPointGenerator */
    private boolean perfPublisher = true;

    @DataBoundConstructor
    public DataToggles() {}

    public boolean isCobertura() {
        return cobertura;
    }

    @DataBoundSetter
    public void setCobertura(boolean cobertura) {
        this.cobertura = cobertura;
    }

    public boolean isRobotFramework() {
        return robotFramework;
    }

    @DataBoundSetter
    public void setRobotFramework(boolean robotFramework) {
        this.robotFramework = robotFramework;
    }

    public boolean isJacoco() {
        return jacoco;
    }

    @DataBoundSetter
    public void setJacoco(boolean jacoco) {
        this.jacoco = jacoco;
    }

    public boolean isPerformance() {
        return performance;
    }

    @DataBoundSetter
    public void setPerformance(boolean performance) {
        this.performance = performance;
    }

    public boolean isSonarqube() {
        return sonarqube;
    }

    @DataBoundSetter
    public void setSonarqube(boolean sonarqube) {
        this.sonarqube = sonarqube;
    }

    public boolean isChangelog() {
        return changelog;
    }

    @DataBoundSetter
    public void setChangelog(boolean changelog) {
        this.changelog = changelog;
    }

    public boolean isPerfPublisher() {
        return perfPublisher;
    }

    @DataBoundSetter
    public void setPerfPublisher(boolean perfPublisher) {
        this.perfPublisher = perfPublisher;
    }

    @Extension
    public static class DescriptorImpl extends Descriptor<DataToggles> {}
}
