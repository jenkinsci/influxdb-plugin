package jenkinsci.plugins.influxdb.models;
 
public class BuildData {
 
    private String jobName;
    private long jobDurationSeconds;
 
    public BuildData() {
        //nop
    }
 
    public String getJobName() {
        return jobName;
    }
 
    public void setJobName(String jobName) {
        this.jobName = jobName;
    }
 
    public long getJobDurationSeconds() {
        return jobDurationSeconds;
    }
 
    public void setJobDurationSeconds(long jobDurationSeconds) {
        this.jobDurationSeconds = jobDurationSeconds;
    }
 
    @Override
    public String toString() {
        return "BuildData [jobName=" + this.jobName + ", jobDurationSeconds=" + this.jobDurationSeconds + "]";
    }
}
