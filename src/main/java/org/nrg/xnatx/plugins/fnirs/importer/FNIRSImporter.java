package org.nrg.xnatx.plugins.fnirs.importer;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.framework.constants.PrearchiveCode;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xft.utils.fileExtraction.Format;
import org.nrg.xnat.helpers.ZipEntryFileWriterWrapper;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.restlet.actions.importer.ImporterHandler;
import org.nrg.xnat.restlet.actions.importer.ImporterHandlerA;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnat.services.messaging.prearchive.PrearchiveOperationRequest;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;
import org.nrg.xnatx.plugins.fnirs.preferences.FnirsPreferences;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.nrg.xnat.archive.Operation.Rebuild;

/**
 * An import handler for fNIRS sessions that supports ZIP uploads of a fNIRS session.
 * Each directory in the folder is assumed to be a single session.
 */
@ImporterHandler(handler = "fNIRS")
@Slf4j
public class FNIRSImporter extends ImporterHandlerA {
    private static final String UNKNOWN_SESSION_LABEL = "fNIRS_zip_upload";
    private static final String MACOSX                = "__MACOSX";
    private static final String ADD                   = "add";
    private static final String NIRS                  = "/nirs/";
    private static final String PARAMS                = "params";

    private final InputStream         inputStream;
    private final UserI               user;
    private final Map<String, Object> params;
    private final String              projectId;
    private final ArcProject          arcProject;
    private final String              filename;
    private final Pattern             fnirsSubjectId;
    private final Pattern             fnirsSessionId;

    private final Date             uploadDate = new Date();
    private final Set<String>      uris       = new LinkedHashSet<>();
    private final Set<SessionData> sessions   = new LinkedHashSet<>();

    public FNIRSImporter(final Object listenerControl,
                         final UserI user,
                         final FileWriterWrapperI fileWriter,
                         final Map<String, Object> params) throws ClientException, IOException {
        super(listenerControl, user);

        // Project ID is required
        if (!params.containsKey(URIManager.PROJECT_ID)) {
            throw new ClientException("PROJECT_ID is a required parameter for fNIRS session uploads.");
        }

        this.projectId = (String) params.get(URIManager.PROJECT_ID);

        if (StringUtils.isBlank(projectId)) {
            throw new ClientException("PROJECT_ID was specified but is blank. This a required parameter for fNIRS session uploads.");
        }

        XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectId, user, false);

        if (project == null) {
            throw new ClientException("PROJECT_ID was specified as " + projectId + " but no project with that ID exists.");
        }

        this.arcProject = project.getArcSpecification();
        if (arcProject == null) {
            throw new ClientException("Tried to get the arc project from project " + projectId + ", but got null in return.");
        }

        // Only accepting ZIP format
        if (Format.getFormat(this.filename = fileWriter.getName()) != Format.ZIP) {
            throw new ClientException("Only ZIP format is supported for fNIRS session uploads.");
        }

        this.inputStream = fileWriter.getInputStream();
        this.user        = user;
        this.params      = params;

        final FnirsPreferences preferences              = XDAT.getContextService().getBean(FnirsPreferences.class);
        final boolean          useFnirsTemplatesAsRegex = preferences.getUseFnirsTemplatesAsRegex();

        this.fnirsSubjectId = getPattern(preferences.getFnirsSubjectId(), useFnirsTemplatesAsRegex);
        this.fnirsSessionId = getPattern(preferences.getFnirsSessionId(), useFnirsTemplatesAsRegex);
    }

    @Override
    public List<String> call() throws ServerException {
        try (final ZipInputStream zis = new ZipInputStream(inputStream)) {
            log.info("Zip file {} received by fNIRS importer for import into project {}.", filename, projectId);

            //read the zip file data
            ZipEntry zipEntry = zis.getNextEntry();
            while (null != zipEntry) {
                if (zipEntry.isDirectory() && fnirsSubjectId.matcher(zipEntry.getName()).find()) {
                    if (fnirsSessionId.matcher(zipEntry.getName()).find() && !zipEntry.getName().contains(MACOSX)) {
                        String[]     paths     = splitPath(zipEntry.getName());
                        String       session   = paths[paths.length - 1];
                        String       subject   = paths[paths.length - 2];
                        final String timestamp = PrearcUtils.makeTimestamp();
                        //after entering a session while the session is the same and the subject is the same create the corresponding folders needed in prearchive and copy contents
                        while (zipEntry != null && zipEntry.getName().contains(session) && zipEntry.getName().contains(subject)) {
                            if (zipEntry.isDirectory() && zipEntry.getName().contains(NIRS) && !zipEntry.getName().contains(MACOSX)) {
                                //It is a nirs folder so get subject, session, and scan name details from path
                                log.info("nirs directory create scan and push files for dir {}", zipEntry.getName());
                                paths = splitPath(zipEntry.getName());
                                String scanName = paths[paths.length - 2];
                                session = paths[paths.length - 3];
                                subject = paths[paths.length - 4];
                                //Create subject session and prearchive folder to transfer data too
                                XnatSubjectdata lookForSubject = XnatSubjectdata.GetSubjectByProjectIdentifier(projectId, subject, user, false);
                                String          subjectID      = null;
                                if (lookForSubject != null) {
                                    subjectID = createSubject(projectId, subject);
                                }
                                Path prearchiveFolderPath = createPreArchiveFolder(projectId, subject, session, scanName, timestamp, Boolean.FALSE);
                                createSession(projectId, subject, session, scanName, timestamp, prearchiveFolderPath);

                                //go next into the folder and get the scan files and write the data to the prearchive equivalent file
                                zipEntry = zis.getNextEntry();
                                //while you don't hit the next folder keep adding files to list of scan files
                                while (zipEntry != null && !zipEntry.isDirectory()) {
                                    //if you see a params folder create the subject and transfer file to subject level file
                                    log.info("pushing scan data to subject {} session {} scan {}", subject, session, scanName);
                                    writeEntryToPrearchive(zis, zipEntry, prearchiveFolderPath);
                                    zipEntry = zis.getNextEntry();
                                }
                            }
                            if (zipEntry != null && zipEntry.isDirectory() && zipEntry.getName().contains(ADD)) {
                                log.info("Additional data directory create scan and push files {}", zipEntry.getName());
                                paths = splitPath(zipEntry.getName());
                                String scanName = paths[paths.length - 2];
                                session = paths[paths.length - 3];
                                subject = paths[paths.length - 4];
                                //Create subject session and prearchive folder to transfer data too
                                XnatSubjectdata lookForSubject = XnatSubjectdata.GetSubjectByProjectIdentifier(projectId, subject, user, false);
                                String          subjectID      = null;
                                if (lookForSubject != null) {
                                    subjectID = createSubject(projectId, subject);
                                }
                                Path prearchiveFolderPath = createPreArchiveFolder(projectId, subject, session, scanName, timestamp, Boolean.TRUE);
                                createSession(projectId, subject, session, scanName, timestamp, prearchiveFolderPath);

                                //go next into the folder and get the scan files and add it to the list
                                zipEntry = zis.getNextEntry();
                                //while you don't hit the next folder keep adding files to list of scan files
                                while (zipEntry != null && !zipEntry.isDirectory()) {
                                    if (zipEntry.getName().contains(PARAMS)) {
                                        log.debug("Params file is ignored here: {}", zipEntry.getName());
                                    } else {
                                        log.info("Push additional data to subject {} session {} scan {}", subject, session, scanName);
                                        writeEntryToPrearchive(zis, zipEntry, prearchiveFolderPath);
                                    }
                                    zipEntry = zis.getNextEntry();
                                }
                            }
                            zipEntry = zis.getNextEntry();
                        }
                    }
                }
                zipEntry = zis.getNextEntry();
            }
            zis.closeEntry();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Send a build request after all sessions have been imported
        sessions.forEach(this::sendSessionBuildRequest);

        return new ArrayList<>(uris);
    }

    private static void writeEntryToPrearchive(final ZipInputStream zis, final ZipEntry zipEntry, final Path prearchiveFolderPath) throws IOException {
        // Create file in the prearchive and write file details to the file in prearchive
        final String filename               = new File(zipEntry.getName()).getName();
        Path         finalPathForPrearchive = prearchiveFolderPath.resolve(filename);

        if (Files.notExists(finalPathForPrearchive)) {
            log.debug("Creating prearchive folder {} for file {}", prearchiveFolderPath, filename);
            Files.createDirectories(prearchiveFolderPath);
        }
        Files.createFile(finalPathForPrearchive);
        new ZipEntryFileWriterWrapper(zipEntry, zis).write(finalPathForPrearchive.toFile());
    }


    private String createSubject(String projectId, String subjectLabel) {
        final String subjectId;
        try {
            subjectId = XnatSubjectdata.CreateNewID();
        } catch (Exception e) {
            log.error("Unable to create new subject ID for subject {} in project {}", subjectLabel, projectId, e);
            return "ERROR";
        }

        log.info("Creating subject in project {} with ID {}", projectId, subjectId);
        final XnatSubjectdata subject = new XnatSubjectdata(user);
        subject.setId(subjectId);
        subject.setProject(projectId);
        subject.setLabel(subjectLabel);

        final PersistentWorkflowI workflow;
        final EventMetaI          eventMeta;

        try {
            workflow = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatSubjectdata.SCHEMA_ELEMENT_NAME, subjectLabel, projectId, EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "Auto-created for project", "Created to support archiving image session", "Creating new subject " + subjectLabel));
            assert workflow != null;
            workflow.setStepDescription("Creating");
            eventMeta = workflow.buildEvent();
        } catch (Exception e) {
            log.error("Unable to create workflow entry for creating subject {} for project {}", subjectLabel, projectId, e);
            return "ERROR";
        }
        try {
            SaveItemHelper.authorizedSave(subject, user, false, false, eventMeta);
            workflow.setStepDescription(PersistentWorkflowUtils.COMPLETE);
            WorkflowUtils.complete(workflow, eventMeta);
            log.info("Successfully created subject {} with ID {} in project {}", subjectLabel, subject.getId(), projectId);
        } catch (Exception e) {
            workflow.setStepDescription(PersistentWorkflowUtils.FAILED);
            try {
                WorkflowUtils.fail(workflow, eventMeta);
            } catch (Exception ex) {
                log.error("Unable to fail workflow for project {}", projectId, ex);
            }
            log.error("Unable to create subject {} for project {}", subjectLabel, projectId, e);
        }
        return subjectId;
    }

    private Path createPreArchiveFolder(String projectId, String subject, String sessionLabel, String scanLabel, String timestamp, Boolean addFolder) throws IOException {
        // Create prearchive timestamp
        log.info("Creating prearchive folder for project {} subject {} session {} scan {} timestamp {} {}", projectId, subject, sessionLabel, scanLabel, timestamp, addFolder ? "additional folder" : "");
        Path   prearchiveTimestampPath  = Paths.get(ArcSpecManager.GetInstance().getGlobalPrearchivePath(), projectId, timestamp);
        String sessionFolderName        = subject.trim() + "_" + sessionLabel.trim();
        Path   sessionFolder            = Paths.get(prearchiveTimestampPath.toString(), sessionFolderName);
        Path   prearchiveScanFolderPath = Paths.get(sessionFolder.toString(), "SCANS", scanLabel);
        // mkdir if it doesnt exist
        if (addFolder) {
            prearchiveScanFolderPath = Paths.get(prearchiveScanFolderPath.toString(), "Additional Data");
        }
        if (Files.notExists(prearchiveScanFolderPath)) {
            Files.createDirectories(prearchiveScanFolderPath);
        }
        return prearchiveScanFolderPath;
    }

    private void createSession(String projectId, String subjectLabel, String sessionLabel, String scanLabel, String timestamp, Path prearchiveTimestampPath) throws ServerException {
        SessionData session           = new SessionData();
        String      sessionFolderName = subjectLabel.trim() + "_" + sessionLabel.trim();
        session.setFolderName(sessionFolderName);
        session.setName(sessionFolderName);
        session.setProject(projectId);
        session.setUploadDate(uploadDate);
        session.setTimestamp(timestamp);
        session.setStatus(PrearcUtils.PrearcStatus.RECEIVING);
        session.setLastBuiltDate(Calendar.getInstance().getTime());
        session.setSubject(subjectLabel);
        session.setSource(params.get(URIManager.SOURCE));
        session.setPreventAnon(Boolean.valueOf((String) params.get(URIManager.PREVENT_ANON)));
        session.setPreventAutoCommit(Boolean.valueOf((String) params.get(URIManager.PREVENT_AUTO_COMMIT)));
        session.setAutoArchive(shouldAutoArchive(projectId));

        Optional<SessionData> matchingSession = sessions.stream().filter(s -> s.getProject().equals(session.getProject()) &&
                                                                              s.getFolderName().equals(session.getFolderName()) &&
                                                                              s.getName().equals(session.getName()) &&
                                                                              s.getSubject().equals(session.getSubject()) &&
                                                                              (!s.getName().equalsIgnoreCase(UNKNOWN_SESSION_LABEL) ||
                                                                               !session.getName().equalsIgnoreCase(UNKNOWN_SESSION_LABEL))).findAny();

        Path sessionFolder = Paths.get(prearchiveTimestampPath.toString(), sessionFolderName);

        if (matchingSession.isPresent()) {
            log.info("Session exists for project {} subject {} session {} scan {}, proceeding", projectId, subjectLabel, sessionLabel, scanLabel);
        } else {
            session.setUrl(sessionFolder.toString());
            try {
                PrearcDatabase.addSession(session);
                log.info("Adding session to prearchive database for project {} subject {} session {} scan {}, proceeding", projectId, subjectLabel, sessionLabel, scanLabel);
            } catch (Exception e) {
                throw new ServerException("Unable to add fNIRS session for project " + projectId + " subject " + subjectLabel + " session " + sessionLabel + " scan " + scanLabel, e);
            }
        }
        sessions.add(session);
        uris.add(sessionFolder.toString());
    }

    private PrearchiveCode shouldAutoArchive(final String projectId) {
        if (params.containsKey("dest")) {
            final String dest = (String) params.get("dest");
            switch (dest) {
                case "/prearchive":
                    return PrearchiveCode.Manual;
                case "/archive":
                    return PrearchiveCode.AutoArchive;
                default:
                    log.warn("Unknown destination {} provided in parameters. Defaulting to prearchive settings for project {}.", dest, projectId);
            }
        }
        final PrearchiveCode code = PrearchiveCode.code(arcProject.getPrearchiveCode());
        log.debug("Got prearchive code {} for project {}", code, projectId);
        return code;
    }

    private void sendSessionBuildRequest(SessionData sessionData) {
        try {
            final File sessionDir = PrearcUtils.getPrearcSessionDir(user, sessionData.getProject(), sessionData.getTimestamp(), sessionData.getFolderName(), false);
            XDAT.sendJmsRequest(new PrearchiveOperationRequest(user, Rebuild, sessionData, sessionDir));
        } catch (Exception e) {
            log.info("Unable to request session build. Sitewide prearchive settings will be used instead.");
        }
    }

    private static String[] splitPath(String pathString) {
        return StreamSupport.stream(Paths.get(pathString).spliterator(), false)
                            .map(Path::toString)
                            .toArray(String[]::new);
    }

    private static Pattern getPattern(final String pattern, final boolean useFnirsTemplatesAsRegex) {
        return useFnirsTemplatesAsRegex
               ? Pattern.compile(pattern)
               : Pattern.compile("^.*" + pattern + ".*$");
    }
}
