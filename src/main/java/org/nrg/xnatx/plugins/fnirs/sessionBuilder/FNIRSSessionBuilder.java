package org.nrg.xnatx.plugins.fnirs.sessionBuilder;

import lombok.extern.slf4j.Slf4j;
import org.nrg.session.SessionBuilder;
import org.nrg.xdat.bean.FnirsFnirssessiondataBean;
import org.nrg.xdat.bean.XnatImagesessiondataBean;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class FNIRSSessionBuilder extends SessionBuilder {

    private final File sessionDir;

    public FNIRSSessionBuilder(final File sessionDir, final Writer fileWriter) {
        super(sessionDir, sessionDir.getPath(), fileWriter);
        this.sessionDir = sessionDir;
    }

    @Override
    public String getSessionInfo() {
        return "(undetermined)";
    }

    @Override
    public XnatImagesessiondataBean call() throws Exception {
        // Get proj/subj/sess/... parameters
        Map<String, String> parameters = getParameters();
        String project = parameters.getOrDefault(PrearcUtils.PARAM_PROJECT, null);
        String subject = parameters.getOrDefault(PrearcUtils.PARAM_SUBJECT_ID, "");
        String label = parameters.getOrDefault(PrearcUtils.PARAM_LABEL, null);

        log.debug("Building BLI session for Project: {} Subject: {} Session: {}", project, subject, label);

        // Initialize the session and populate
        FnirsFnirssessiondataBean fnirsSession = new FnirsFnirssessiondataBean();
        fnirsSession.setPrearchivepath(sessionDir.getPath());
        fnirsSession.setProject(project);
        fnirsSession.setSubjectId(subject);
        fnirsSession.setLabel(label);

        // Build scans
        try (final Stream<Path> files = Files.list(sessionDir.toPath().resolve("SCANS"))) {
            files.filter(Files::isDirectory)
                 .forEach(folder -> {
                     try {
                         final FNIRSScanBuilder builder = new FNIRSScanBuilder(folder);
                         fnirsSession.addScans_scan(builder.call());
                     } catch (IOException e) {
                         throw new RuntimeException("An error occurred trying to build a scan from the folder " + folder, e);
                     }
                 });
        }
        return fnirsSession;
    }
}
