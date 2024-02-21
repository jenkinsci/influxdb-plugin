package jenkinsci.plugins.influxdb.generators;

import com.influxdb.client.write.Point;
import hudson.model.*;
import hudson.scm.ChangeLogSet;
import hudson.scm.EditType;
import jenkins.model.Jenkins;
import jenkinsci.plugins.influxdb.renderer.ProjectNameRenderer;
import org.apache.commons.lang3.StringUtils;
import org.jenkinsci.plugins.workflow.job.WorkflowRun;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.*;


public class ChangeLogGeneratorTest {

    private Run<?, ?> build;
    private ProjectNameRenderer measurementRenderer;
    private TaskListener listener;
    private long currTime;

    private Job job;


    private AbstractBuild mockBuild(String projectFullName, int buildNumber) {
        AbstractBuild build = Mockito.mock(AbstractBuild.class);
        Mockito.when(build.getParent()).thenReturn(job);
        Mockito.when(job.getName()).thenReturn(projectFullName);
        Mockito.when(build.getNumber()).thenReturn(buildNumber);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + projectFullName);
        return build;
    }

    private WorkflowRun mockWorkflow(String projectFullName, int buildNumber) {
        WorkflowRun build = Mockito.mock(WorkflowRun.class);
        Mockito.doReturn(job).when(build).getParent();
//        Mockito.when(build.getParent()).thenReturn((WorkflowJob) job);
        Mockito.when(job.getName()).thenReturn(projectFullName);
        Mockito.when(build.getNumber()).thenReturn(buildNumber);
        Mockito.when(job.getRelativeNameFrom(Mockito.nullable(Jenkins.class))).thenReturn("folder/" + projectFullName);
        return build;
    }

    private ChangeLogSet.Entry createEntry(String name, String message, Collection<ChangeLogSet.AffectedFile> affectedFiles) {
        ChangeLogSet.Entry entry = Mockito.mock(ChangeLogSet.Entry.class);
        User user = Mockito.mock(User.class);
        Mockito.when(user.getFullName()).thenReturn(name);
        Mockito.when(entry.getAuthor()).thenReturn(user);
        Mockito.when(entry.getMsg()).thenReturn(message);
        Mockito.doReturn(affectedFiles).when(entry).getAffectedFiles();

        return entry;
    }

    private ChangeLogSet.AffectedFile createFile(String path) {
        ChangeLogSet.AffectedFile affectedFile = Mockito.mock(ChangeLogSet.AffectedFile.class);
        Mockito.when(affectedFile.getPath()).thenReturn(path);
        Mockito.when(affectedFile.getEditType()).thenReturn(EditType.EDIT);

        return affectedFile;
    }

    @Before
    public void before() {
        build = Mockito.mock(Run.class);
        measurementRenderer = new ProjectNameRenderer("", null);
        listener = Mockito.mock(TaskListener.class);
        job = Mockito.mock(Job.class);

        Mockito.doReturn(job).when(build).getParent();
        Mockito.when(build.getNumber()).thenReturn(1);
        Mockito.when(job.getName()).thenReturn("changelog_test");

        currTime = System.currentTimeMillis();
    }

    @Test
    public void testHasReportWithoutCommitsShouldReturnFalse() {
        ChangeLogPointGenerator generator = new ChangeLogPointGenerator(build, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY);
        assertFalse(generator.hasReport());
    }

    @Test
    public void testAbstractBuildShouldGenerateData() {
        String name = "John Doe";
        String message = "Awesome commit message";
        String path = "path/to/file.txt";

        AbstractBuild abstractBuild = mockBuild("abstract_build", 1);

        ChangeLogSet.AffectedFile affectedFile = createFile(path);
        Collection<ChangeLogSet.AffectedFile> affectedFiles = Collections.singletonList(affectedFile);

        ChangeLogSet.Entry entry = createEntry(name, message, affectedFiles);

        ChangeLogSet changeLogSet = Mockito.mock(ChangeLogSet.class);
        List<ChangeLogSet.Entry> entries = new ArrayList<>();
        entries.add(entry);
        Mockito.when(abstractBuild.getChangeSet()).thenReturn(changeLogSet);
        Mockito.when(changeLogSet.iterator()).thenReturn(entries.iterator());

        ChangeLogPointGenerator generator = new ChangeLogPointGenerator(abstractBuild, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY);
        assertTrue(generator.hasReport());

        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        assertTrue(lineProtocol.contains("culprits=\"" + name + "\""));
        assertTrue(lineProtocol.contains("commit_messages=\"" + message + "\""));
        assertTrue(lineProtocol.contains("affected_paths=\"" + path + "\""));
        assertTrue(lineProtocol.contains("commit_count=1"));
    }

    @Test
    public void testWorkflowBuildShouldGenerateData() {
        WorkflowRun workflowRun = mockWorkflow("workflow_run", 1);

        // Generate mock data
        String name1 = "John Doe";
        String name2 = "Jane Doe";
        String message1 = "Awesome commit message";
        String message2 = "Another message";
        String path1 = "path/to/file.txt";
        String path2 = "another/path/to/file.txt";
        ChangeLogSet.AffectedFile file1 = createFile(path1);
        ChangeLogSet.AffectedFile file2 = createFile(path2);
        Collection<ChangeLogSet.AffectedFile> affectedFiles = new ArrayList<>();
        affectedFiles.add(file1);
        affectedFiles.add(file2);
        ChangeLogSet.Entry entry1 = createEntry(name1, message1, affectedFiles);
        ChangeLogSet.Entry entry2 = createEntry(name2, message2, affectedFiles);
        List<ChangeLogSet.Entry> entries = new ArrayList<>();
        entries.add(entry1);
        entries.add(entry2);
        ChangeLogSet changeLogSet = Mockito.mock(ChangeLogSet.class);
        List<ChangeLogSet<? extends ChangeLogSet.Entry>> changeLogSets = new ArrayList<>();
        changeLogSets.add(changeLogSet);
        Mockito.when(workflowRun.getChangeSets()).thenReturn(changeLogSets);
        Mockito.when(changeLogSet.iterator()).thenReturn(entries.iterator());

        // Generate point
        ChangeLogPointGenerator generator = new ChangeLogPointGenerator(workflowRun, listener, measurementRenderer, currTime, StringUtils.EMPTY, StringUtils.EMPTY);
        assertTrue(generator.hasReport());
        Point[] points = generator.generate();
        String lineProtocol = points[0].toLineProtocol();

        String paths = path1 + ", " + path2;
        assertTrue(lineProtocol.contains("culprits=\"" + name1 + ", " + name2 + "\""));
        assertTrue(lineProtocol.contains("commit_messages=\"" + message1 + ", " + message2 + "\""));
        assertTrue(lineProtocol.contains("affected_paths=\"" + paths + ", " + paths + "\""));
        assertTrue(lineProtocol.contains("commit_count=2"));
    }
}
