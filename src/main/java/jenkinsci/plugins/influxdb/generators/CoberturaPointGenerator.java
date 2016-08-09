package jenkinsci.plugins.influxdb.generators;

import org.influxdb.dto.Point;

import hudson.FilePath;
import jenkins.MasterToSlaveFileCallable;
import hudson.remoting.VirtualChannel;
import hudson.model.Run;
import net.sourceforge.cobertura.coveragedata.ClassData;
import net.sourceforge.cobertura.coveragedata.CoverageDataFileHandler;
import net.sourceforge.cobertura.coveragedata.PackageData;
import net.sourceforge.cobertura.coveragedata.ProjectData;

import java.io.IOException;
import java.io.File;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.List;

public class CoberturaPointGenerator extends AbstractPointGenerator {

    public static final String COBERTURA_PACKAGE_COVERAGE_RATE = "cobertura_package_coverage_rate";
    public static final String COBERTURA_CLASS_COVERAGE_RATE = "cobertura_class_coverage_rate";
    public static final String COBERTURA_LINE_COVERAGE_RATE = "cobertura_line_coverage_rate";
    public static final String COBERTURA_BRANCH_COVERAGE_RATE = "cobertura_branch_coverage_rate";
    public static final String COBERTURA_NUMBER_OF_PACKAGES = "cobertura_number_of_packages";
    public static final String COBERTURA_NUMBER_OF_SOURCEFILES = "cobertura_number_of_sourcefiles";
    public static final String COBERTURA_NUMBER_OF_CLASSES = "cobertura_number_of_classes";

    private final Run<?, ?> build;
    private final FilePath coberturaFile;

    public CoberturaPointGenerator(Run<?, ?> build, FilePath workspace, String coberturaReportLocation) {
        this.build = build;
        coberturaFile = new FilePath(workspace, coberturaReportLocation);
    }

    public boolean hasReport() {
        try {
            return (coberturaFile != null && coberturaFile.exists());
        } catch (IOException|InterruptedException e) {
            // NOP
        }
        return false;
    }

    private static final class CoberturaFileCallable extends MasterToSlaveFileCallable<List<Number>> {
        private static final long serialVersionUID = 1;
        private ProjectData coberturaProjectData;
        @Override
        public List<Number> invoke(File f, VirtualChannel channel) {
            coberturaProjectData = CoverageDataFileHandler.loadCoverageData(f);
            List<Number> ls = new ArrayList<Number>();
            ls.add(coberturaProjectData.getPackages().size());
            ls.add(coberturaProjectData.getNumberOfSourceFiles());
            ls.add(coberturaProjectData.getNumberOfClasses());
            ls.add(coberturaProjectData.getBranchCoverageRate()*100d);
            ls.add(coberturaProjectData.getLineCoverageRate()*100d);
            ls.add(getPackageCoverage()*100d);
            ls.add(getClassCoverage()*100d);
            return ls;
        }
        private double getPackageCoverage() {
            double totalPacakges = coberturaProjectData.getPackages().size();
            double packagesCovered = 0;
            for(Object nextPackage : coberturaProjectData.getPackages()) {
                PackageData packageData = (PackageData) nextPackage;
                if(packageData.getLineCoverageRate() > 0)
                    packagesCovered++;
            }
            return packagesCovered / totalPacakges;
        }

        private double getClassCoverage() {
            double totalClasses = coberturaProjectData.getNumberOfClasses();
            double classesCovered = 0;
            for(Object nextClass : coberturaProjectData.getClasses()) {
                ClassData classData = (ClassData) nextClass;
                if(classData.getLineCoverageRate() > 0)
                    classesCovered++;
            }
            return classesCovered / totalClasses;
        }
    }

    public Point[] generate() {
        try {
            List ls = coberturaFile.act(new CoberturaFileCallable()); 
            Point point = Point.measurement("cobertura_data")
                .field(BUILD_NUMBER, build.getNumber())
                .field(PROJECT_NAME, build.getParent().getName())
                .field(COBERTURA_NUMBER_OF_PACKAGES, ls.get(0))
                .field(COBERTURA_NUMBER_OF_SOURCEFILES, ls.get(1))
                .field(COBERTURA_NUMBER_OF_CLASSES, ls.get(2))
                .field(COBERTURA_BRANCH_COVERAGE_RATE, ls.get(3))
                .field(COBERTURA_LINE_COVERAGE_RATE, ls.get(4))
                .field(COBERTURA_PACKAGE_COVERAGE_RATE, ls.get(5))
                .field(COBERTURA_CLASS_COVERAGE_RATE, ls.get(6))
                .build();
            return new Point[] {point};
        } catch (IOException|InterruptedException e) {
            // NOP
        }
        return null;
    }

}
