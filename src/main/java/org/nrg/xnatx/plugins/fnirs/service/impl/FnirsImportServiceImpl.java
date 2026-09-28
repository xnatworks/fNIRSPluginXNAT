package org.nrg.xnatx.plugins.fnirs.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.nrg.action.ClientException;
import org.nrg.action.ServerException;
import org.nrg.framework.constants.PrearchiveCode;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.ArcProject;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.om.XnatSubjectdata;
import org.nrg.xdat.services.cache.UserDataCache;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.SaveItemHelper;
import org.nrg.xnat.helpers.ZipEntryFileWriterWrapper;
import org.nrg.xnat.helpers.prearchive.PrearcDatabase;
import org.nrg.xnat.helpers.prearchive.PrearcUtils;
import org.nrg.xnat.helpers.prearchive.SessionData;
import org.nrg.xnat.services.messaging.prearchive.PrearchiveOperationRequest;
import org.nrg.xnat.turbine.utils.ArcSpecManager;
import org.nrg.xnat.utils.WorkflowUtils;
import org.nrg.xnatx.plugins.fnirs.preferences.FnirsPreferences;
import org.nrg.xnatx.plugins.fnirs.service.FnirsImportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.nrg.xnat.archive.Operation.Rebuild;

@Slf4j
@Service
public class FnirsImportServiceImpl implements FnirsImportService {

    private final UserDataCache userDataCache;
    final FnirsPreferences preferences;

    private static final String UNKNOWN_SESSION_LABEL = "fNIRS_zip_upload";
    private static final String MACOSX = "__MACOSX";
    private static final String NIRS = "/nirs/";
    private static final String ADD = "add";
    private static final String PARAMS = "params";


    @Autowired
    public FnirsImportServiceImpl(UserDataCache userDataCache, FnirsPreferences preferences) {
        this.userDataCache = userDataCache;
        this.preferences = preferences;
    }

    @Override
    public void importFnirsDataFromZip(String projectId, String cachePath, UserI user) throws Exception {
        Set<SessionData> sessions = new LinkedHashSet<>();
        String relativeCachePath = cachePath.startsWith("/") ? cachePath.substring(1) : cachePath;
        File file = userDataCache.getUserDataCacheFile(user, Paths.get(relativeCachePath));

        try (final ZipInputStream zis = new ZipInputStream(new FileInputStream(file))) {
            log.info("Zip file {} received by fNIRS importer for import into project {}.", cachePath, projectId);

            boolean useFnirsTemplatesAsRegex = preferences.getUseFnirsTemplatesAsRegex();
            Pattern fnirsSubjectId = getPattern(preferences.getFnirsSubjectId(), useFnirsTemplatesAsRegex);
            Pattern fnirsSessionId = getPattern(preferences.getFnirsSessionId(), useFnirsTemplatesAsRegex);

            //read the zip file data
            ZipEntry zipEntry = zis.getNextEntry();
            while (null != zipEntry) {
                if (zipEntry.isDirectory() && fnirsSubjectId.matcher(zipEntry.getName()).find()) {
                    if (fnirsSessionId.matcher(zipEntry.getName()).find() && !zipEntry.getName().contains(MACOSX)) {
                        String[] paths = splitPath(zipEntry.getName());
                        String session = paths[paths.length - 1];
                        String subject = paths[paths.length - 2];
                        final String timestamp = PrearcUtils.makeTimestamp();
                        //after entering a session while the session is the same and the subject is the same create the corresponding folders needed in prearchive and copy contents
                        while (zipEntry != null && zipEntry.getName().contains(session) && zipEntry.getName().contains(subject)) {
                            if (zipEntry.isDirectory() && zipEntry.getName().contains(NIRS) && !zipEntry.getName().contains(MACOSX)) {
                                //It is a nirs folder so get subject, session, and scan name details from path
                                log.info("fnirs directory create scan and push files for dir {}", zipEntry.getName());
                                paths = splitPath(zipEntry.getName());
                                String scanName = paths[paths.length - 2];
                                session = paths[paths.length - 3];
                                subject = paths[paths.length - 4];
                                //Create subject session and prearchive folder to transfer data too
                                XnatSubjectdata lookForSubject = XnatSubjectdata.GetSubjectByProjectIdentifier(projectId, subject, user, false);
                                if (lookForSubject != null) {
                                    createSubject(projectId, subject, user);
                                }
                                Path prearchiveFolderPath = createPreArchiveFolder(projectId, subject, session, scanName, timestamp, Boolean.FALSE);
                                createSession(projectId, subject, session, scanName, timestamp, prearchiveFolderPath, user, sessions);

                                //go next into the folder and get the scan files and write the data to the prearchive equivalent file
                                zipEntry = zis.getNextEntry();
                                //while you don't hit the next folder keep adding files to list of scan files
                                while (zipEntry != null && !zipEntry.isDirectory()) {
                                    //if you see a params folder create the subject and transfer file to subject level file
                                    log.info("pushing scan data to subject {} session {} scan {}", subject, session, scanName);
                                    writeZipFileToPrearchive(zis, zipEntry, prearchiveFolderPath);
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
                                if (lookForSubject != null) {
                                    createSubject(projectId, subject, user);
                                }
                                Path prearchiveFolderPath = createPreArchiveFolder(projectId, subject, session, scanName, timestamp, Boolean.TRUE);
                                createSession(projectId, subject, session, scanName, timestamp, prearchiveFolderPath, user, sessions);

                                //go next into the folder and get the scan files and add it to the list
                                zipEntry = zis.getNextEntry();
                                //while you don't hit the next folder keep adding files to list of scan files
                                while (zipEntry != null && !zipEntry.isDirectory()) {
                                    if (zipEntry.getName().contains(PARAMS)) {
                                        log.debug("Params file is ignored here: {}", zipEntry.getName());
                                    } else {
                                        log.info("Push additional data to subject {} session {} scan {}", subject, session, scanName);
                                        writeZipFileToPrearchive(zis, zipEntry, prearchiveFolderPath);
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
        } catch (IOException | ServerException | ClientException e) {
            throw new RuntimeException(e);
        }

        // Send a build request after all sessions have been imported
        sessions.forEach(session -> sendSessionBuildRequest(session, user));
    }

    @Override
    public void importFnirsDataFromFolder(String projectId, String cacheBasePath, UserI user) throws Exception{
        Set<SessionData> sessions = new LinkedHashSet<>();
        String relativeCachePath = cacheBasePath.startsWith("/") ? cacheBasePath.substring(1) : cacheBasePath;
        File folder = userDataCache.getUserDataCacheFile(user, Paths.get(relativeCachePath), UserDataCache.Options.Folder);

        boolean useFnirsTemplatesAsRegex = preferences.getUseFnirsTemplatesAsRegex();
        Pattern fnirsSubjectId = getPattern(preferences.getFnirsSubjectId(), useFnirsTemplatesAsRegex);
        Pattern fnirsSessionId = getPattern(preferences.getFnirsSessionId(), useFnirsTemplatesAsRegex);

        try {
            log.info("Folder {} received by fNIRS importer for import into project {}.", cacheBasePath, projectId);
            List<Path> subjectSessionDirs;
            try (Stream<Path> walk = Files.walk(folder.toPath(), 2)) {
                subjectSessionDirs = walk.filter(Files::isDirectory)
                    .filter(path -> fnirsSubjectId.matcher(path.toString()).find()).toList();

                for (Path sessionDir : subjectSessionDirs) {
                    if (!fnirsSessionId.matcher(sessionDir.toString()).find() || sessionDir.toString().contains(MACOSX)) {
                        continue;
                    }
                    final String timestamp = PrearcUtils.makeTimestamp();

                    List<Path> scanDirs;
                    try (Stream<Path> scanWalk = Files.walk(sessionDir)) {
                        scanDirs = scanWalk.filter(Files::isDirectory).toList();
                    }

                    for (Path scanDir : scanDirs) {
                        String scanDirName = scanDir.toString();
                        String[] paths = splitPath(scanDirName);
                        String scan = paths[paths.length - 2];
                        String session = paths[paths.length - 3];
                        String subject = paths[paths.length - 4];

                        if (paths[paths.length - 1].equalsIgnoreCase("nirs") && !scanDirName.contains(MACOSX)) {
                            log.info("Found scan directory: {}", scanDirName);

                            //located inside if statement(s) so we don't create any subjects with no data in them
                            XnatSubjectdata lookForSubject = XnatSubjectdata.GetSubjectByProjectIdentifier(projectId, subject, user, false);
                            if (lookForSubject != null) {
                                createSubject(projectId, subject, user);
                            }

                            Path prearchiveFolderPath = createPreArchiveFolder(projectId, subject, session, scan, timestamp, Boolean.FALSE);
                            createSession(projectId, subject, session, scan, timestamp, prearchiveFolderPath, user, sessions);

                            try (Stream<Path> files = Files.list(scanDir)) {
                                for (Path file : files.filter(Files::isRegularFile).toList()) {
                                    log.info("Creating scan data: subject {} session {} scan {}", subject, session, scan);
                                    copyFileToPrearchive(file, prearchiveFolderPath);
                                }
                            }
                        }

                        if (scanDirName.contains(ADD)) {
                            log.info("Importing additional data directory {}", scanDirName);

                            //located inside if statement(s) so we don't create any subjects with no data in them
                            XnatSubjectdata lookForSubject = XnatSubjectdata.GetSubjectByProjectIdentifier(projectId, subject, user, false);
                            if (lookForSubject != null) {
                                createSubject(projectId, subject, user);
                            }

                            Path prearchiveFolderPath = createPreArchiveFolder(projectId, subject, session, scan, timestamp, Boolean.TRUE);
                            createSession(projectId, subject, session, scan, timestamp, prearchiveFolderPath, user, sessions);

                            try (Stream<Path> files = Files.list(scanDir)) {
                                for (Path file : files.filter(Files::isRegularFile).toList()) {
                                    if (!file.getFileName().toString().contains(PARAMS)) {
                                        log.info("Adding additional data to subject {} session {} scan {}", subject, session, scan);
                                        copyFileToPrearchive(file, prearchiveFolderPath);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (IOException | ServerException | ClientException e) {
            throw new RuntimeException(e);
        }

        // Send a build request after all sessions have been imported
        sessions.forEach(session -> sendSessionBuildRequest(session, user));
    }

    private static void writeZipFileToPrearchive(final ZipInputStream zis,
                                               final ZipEntry zipEntry,
                                               final Path prearchiveFolderPath) throws IOException {
        writeFileToPrearchive(zis, new File(zipEntry.getName()).getName(), prearchiveFolderPath);
    }

    private static void copyFileToPrearchive(final Path sourceFile,
                                             final Path prearchiveFolderPath) throws IOException {
        try (InputStream inputStream = Files.newInputStream(sourceFile)) {
            writeFileToPrearchive(inputStream, sourceFile.getFileName().toString(), prearchiveFolderPath);
        }
    }

    private static void writeFileToPrearchive(final InputStream inputStream,
                                               final String filename,
                                               final Path prearchiveFolderPath) throws IOException {
        Path finalPathForPrearchive = prearchiveFolderPath.resolve(filename);

        if (Files.notExists(prearchiveFolderPath)) {
            log.debug("Creating prearchive folder {} for file {}", prearchiveFolderPath, filename);
            Files.createDirectories(prearchiveFolderPath);
        }
        Files.createFile(finalPathForPrearchive);
        Files.copy(inputStream, finalPathForPrearchive, StandardCopyOption.REPLACE_EXISTING);
    }

    private void createSubject(String projectId, String subjectLabel, UserI user) {
        final String subjectId;
        try {
            subjectId = XnatSubjectdata.CreateNewID();
        } catch (Exception e) {
            log.error("Unable to create new subject ID for subject {} in project {}", subjectLabel, projectId, e);
            return;
        }

        log.info("Creating subject in project {} with ID {}", projectId, subjectId);
        final XnatSubjectdata subject = new XnatSubjectdata(user);
        subject.setId(subjectId);
        subject.setProject(projectId);
        subject.setLabel(subjectLabel);

        final PersistentWorkflowI workflow;
        final EventMetaI eventMeta;

        try {
            EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA,
                                                     EventUtils.TYPE.PROCESS,
                                                     "Auto-created for project",
                                                     "Created to support archiving image session",
                                                     "Creating new subject " + subjectLabel);
            workflow = PersistentWorkflowUtils.buildOpenWorkflow(user,
                                                                 XnatSubjectdata.SCHEMA_ELEMENT_NAME,
                                                                 subjectLabel,
                                                                 projectId,
                                                                 eventDetails);
            assert workflow != null;
            workflow.setStepDescription("Creating");
            eventMeta = workflow.buildEvent();
        } catch (Exception e) {
            log.error("Unable to create workflow entry for creating subject {} for project {}", subjectLabel, projectId, e);
            return;
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
    }

    private Path createPreArchiveFolder(String projectId,
                                        String subject,
                                        String sessionLabel,
                                        String scanLabel,
                                        String timestamp,
                                        Boolean addFolder) throws IOException {
        // Create prearchive timestamp
        log.info("Creating prearchive folder for project {} subject {} session {} scan {} timestamp {} {}",
                 projectId, subject, sessionLabel, scanLabel, timestamp, addFolder ? "additional folder" : "");

        Path prearchiveTimestampPath = Paths.get(ArcSpecManager.GetInstance().getGlobalPrearchivePath(), projectId, timestamp);
        String sessionFolderName = subject.trim() + "_" + sessionLabel.trim();
        Path sessionFolder = Paths.get(prearchiveTimestampPath.toString(), sessionFolderName);
        Path prearchiveScanFolderPath = Paths.get(sessionFolder.toString(), "SCANS", scanLabel);

        // mkdir if it doesn't exist
        if (addFolder) {
            prearchiveScanFolderPath = Paths.get(prearchiveScanFolderPath.toString(), "Additional Data");
        }
        if (Files.notExists(prearchiveScanFolderPath)) {
            Files.createDirectories(prearchiveScanFolderPath);
        }
        return prearchiveScanFolderPath;
    }

    private void createSession(String projectId,
                               String subjectLabel,
                               String sessionLabel,
                               String scanLabel,
                               String timestamp,
                               Path prearchiveTimestampPath,
                               UserI user,
                               Set<SessionData> sessions) throws Exception {
        SessionData session = new SessionData();
        String sessionFolderName = subjectLabel.trim() + "_" + sessionLabel.trim();
        session.setFolderName(sessionFolderName);
        session.setName(sessionFolderName);
        session.setProject(projectId);
        session.setUploadDate(new Date());
        session.setTimestamp(timestamp);
        session.setStatus(PrearcUtils.PrearcStatus.RECEIVING);
        session.setLastBuiltDate(Calendar.getInstance().getTime());
        session.setSubject(subjectLabel);
        session.setAutoArchive(shouldAutoArchive(projectId, user));

        SessionData existingSession = PrearcDatabase.getSessionIfExists(session.getFolderName(), session.getTimestamp(), projectId);
        Path sessionFolder = Paths.get(prearchiveTimestampPath.toString(), sessionFolderName);

        if (existingSession != null) {
            log.info("Session exists for project {} subject {} session {} scan {}, proceeding",
                     projectId, subjectLabel, sessionLabel, scanLabel);

        } else {
            session.setUrl(sessionFolder.toString());
            try {
                PrearcDatabase.addSession(session);
                sessions.add(session);
                log.info("Adding session to prearchive database for project {} subject {} session {} scan {}, " +
                                 "proceeding", projectId, subjectLabel, sessionLabel, scanLabel);
            } catch (Exception e) {
                throw new ServerException("Unable to add fNIRS session for project " + projectId + " subject " +
                                                  subjectLabel + " session " + sessionLabel + " scan " + scanLabel, e);
            }
        }
    }

    private PrearchiveCode shouldAutoArchive(final String projectId, UserI user) throws ClientException {
        XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectId, user, false);

        ArcProject arcProject = project.getArcSpecification();
        if (arcProject == null) {
            throw new ClientException("Could not find project ARC.");
        }
        final PrearchiveCode code = PrearchiveCode.code(arcProject.getPrearchiveCode());
        log.debug("Got prearchive code {} for project {}", code, projectId);
        return code;
    }

    private void sendSessionBuildRequest(SessionData sessionData, UserI user) {
        try {
            final File sessionDir = PrearcUtils.getPrearcSessionDir(user,
                                                                    sessionData.getProject(),
                                                                    sessionData.getTimestamp(),
                                                                    sessionData.getFolderName(),
                                                                    false);
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
