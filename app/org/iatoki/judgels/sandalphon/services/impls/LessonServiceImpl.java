package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.GitCommit;
import org.iatoki.judgels.commons.GitProvider;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonPartner;
import org.iatoki.judgels.sandalphon.LessonPartnerConfig;
import org.iatoki.judgels.sandalphon.LessonPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.models.daos.LessonDao;
import org.iatoki.judgels.sandalphon.models.daos.LessonPartnerDao;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel_;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel_;
import org.iatoki.judgels.sandalphon.services.LessonService;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class LessonServiceImpl implements LessonService {
    private final LessonDao lessonDao;
    private final LessonPartnerDao lessonPartnerDao;
    private final FileSystemProvider fileSystemProvider;
    private final GitProvider gitProvider;

    public LessonServiceImpl(LessonDao lessonDao, LessonPartnerDao lessonPartnerDao, FileSystemProvider fileSystemProvider, GitProvider gitProvider) {
        this.lessonDao = lessonDao;
        this.lessonPartnerDao = lessonPartnerDao;
        this.fileSystemProvider = fileSystemProvider;
        this.gitProvider = gitProvider;
    }

    @Override
    public Lesson createLesson(String name, String additionalNote, String initialLanguageCode) throws IOException {
        LessonModel lessonModel = new LessonModel();
        lessonModel.name = name;
        lessonModel.additionalNote = additionalNote;

        lessonDao.persist(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatements(lessonModel.jid, initialLanguageCode);
        fileSystemProvider.createDirectory(LessonServiceUtils.getClonesDirPath(lessonModel.jid));

        return createLessonFromModel(lessonModel);
    }

    @Override
    public boolean lessonExistsByJid(String lessonJid) {
        return lessonDao.existsByJid(lessonJid);
    }

    @Override
    public final Lesson findLessonById(long lessonId) throws LessonNotFoundException {
        LessonModel lessonModel = lessonDao.findById(lessonId);
        if (lessonModel != null) {
            return createLessonFromModel(lessonModel);
        } else {
            throw new LessonNotFoundException("Lesson not found.");
        }
    }

    @Override
    public final Lesson findLessonByJid(String lessonJid) {
        LessonModel lessonModel = lessonDao.findByJid(lessonJid);
        return createLessonFromModel(lessonModel);
    }

    @Override
    public boolean isLessonPartnerByUserJid(String lessonJid, String userJid) {
        return lessonPartnerDao.existsByLessonJidAndPartnerJid(lessonJid, userJid);
    }

    @Override
    public void createLessonPartner(long lessonId, String userJid, LessonPartnerConfig config) {
        LessonModel lessonModel = lessonDao.findById(lessonId);

        LessonPartnerModel lessonPartnerModel = new LessonPartnerModel();
        lessonPartnerModel.lessonJid = lessonModel.jid;
        lessonPartnerModel.userJid = userJid;
        lessonPartnerModel.config = new Gson().toJson(config);

        lessonPartnerDao.persist(lessonPartnerModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateLessonPartner(long lessonPartnerId, LessonPartnerConfig config) {
        LessonPartnerModel lessonPartnerModel = lessonPartnerDao.findById(lessonPartnerId);
        lessonPartnerModel.config = new Gson().toJson(config);

        lessonPartnerDao.edit(lessonPartnerModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<LessonPartner> pageLessonPartners(String lessonJid, long pageIndex, long pageSize, String orderBy, String orderDir) {
        long totalRows = lessonPartnerDao.countByFilters("", ImmutableMap.of(LessonPartnerModel_.lessonJid, lessonJid), ImmutableMap.of());
        List<LessonPartnerModel> lessonPartnerModels = lessonPartnerDao.findSortedByFilters(orderBy, orderDir, "", ImmutableMap.of(LessonPartnerModel_.lessonJid, lessonJid), ImmutableMap.of(), pageIndex, pageIndex * pageSize);
        List<LessonPartner> lessonPartners = Lists.transform(lessonPartnerModels, m -> createLessonPartnerFromModel(m));

        return new Page<>(lessonPartners, totalRows, pageIndex, pageSize);
    }

    @Override
    public LessonPartner findLessonPartnerByLessonPartnerId(long lessonPartnerId) throws LessonPartnerNotFoundException {
        LessonPartnerModel lessonPartnerModel = lessonPartnerDao.findById(lessonPartnerId);
        if (lessonPartnerModel != null) {
            return createLessonPartnerFromModel(lessonPartnerModel);
        } else {
            throw new LessonPartnerNotFoundException("Lesson partner not found.");
        }
    }

    @Override
    public LessonPartner findLessonPartnerByLessonJidAndPartnerJid(String lessonJid, String partnerJid) {
        LessonPartnerModel lessonPartnerModel = lessonPartnerDao.findByLessonJidAndPartnerJid(lessonJid, partnerJid);

        return createLessonPartnerFromModel(lessonPartnerModel);
    }

    @Override
    public void updateLesson(long lessonId, String name, String additionalNote) {
        LessonModel lessonModel = lessonDao.findById(lessonId);
        lessonModel.name = name;
        lessonModel.additionalNote = additionalNote;

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<Lesson> pageLessons(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString, String userJid, boolean isAdmin) {
        if (isAdmin) {
            long totalRows = lessonDao.countByFilters(filterString);
            List<LessonModel> lessonModels = lessonDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

            List<Lesson> lessons = Lists.transform(lessonModels, m -> createLessonFromModel(m));
            return new Page<>(lessons, totalRows, pageIndex, pageSize);
        } else {
            List<String> lessonJidsWhereIsAuthor = lessonDao.findLessonJidsByAuthorJid(userJid);
            List<String> lessonJidsWhereIsPartner = lessonPartnerDao.findLessonJidsByPartnerJid(userJid);

            ImmutableSet.Builder<String> allowedLessonJidsBuilder = ImmutableSet.builder();
            allowedLessonJidsBuilder.addAll(lessonJidsWhereIsAuthor);
            allowedLessonJidsBuilder.addAll(lessonJidsWhereIsPartner);

            Set<String> allowedLessonJids = allowedLessonJidsBuilder.build();

            long totalRows = lessonDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of(LessonModel_.jid, allowedLessonJids));
            List<LessonModel> lessonModels = lessonDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(LessonModel_.jid, allowedLessonJids), pageIndex * pageSize, pageSize);

            List<Lesson> lessons = Lists.transform(lessonModels, m -> createLessonFromModel(m));
            return new Page<>(lessons, totalRows, pageIndex, pageSize);
        }

    }

    @Override
    public Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String lessonJid) throws IOException {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        return new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {}.getType());
    }

    @Override
    public void addLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        String defaultLanguageStatement = fileSystemProvider.readFromFile(getStatementFilePath(userJid, lessonJid, getDefaultLanguage(userJid, lessonJid)));
        fileSystemProvider.writeToFile(getStatementFilePath(userJid, lessonJid, languageCode), defaultLanguageStatement);
        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void enableLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void disableLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.DISABLED);

        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void makeDefaultLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        fileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(userJid, lessonJid), languageCode);
    }

    @Override
    public String getDefaultLanguage(String userJid, String lessonJid) throws IOException {
        return fileSystemProvider.readFromFile(getStatementDefaultLanguageFilePath(userJid, lessonJid));
    }

    @Override
    public String getStatement(String userJid, String lessonJid, String languageCode) throws IOException {
        return fileSystemProvider.readFromFile(getStatementFilePath(userJid, lessonJid, languageCode));
    }

    @Override
    public void updateStatement(String userJid, long lessonId, String languageCode, String statement) throws IOException {
        LessonModel lessonModel = lessonDao.findById(lessonId);
        fileSystemProvider.writeToFile(getStatementFilePath(userJid, lessonModel.jid, languageCode), statement);

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFile(String userJid, long id, File mediaFile, String filename) throws IOException {
        LessonModel lessonModel = lessonDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, lessonModel.jid);
        fileSystemProvider.uploadFile(mediaDirPath, mediaFile, filename);

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFileZipped(String userJid, long id, File mediaFileZipped) throws IOException {
        LessonModel lessonModel = lessonDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, lessonModel.jid);
        fileSystemProvider.uploadZippedFiles(mediaDirPath, mediaFileZipped, false);

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public List<FileInfo> getStatementMediaFiles(String userJid, String lessonJid) {
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, lessonJid);
        return fileSystemProvider.listFilesInDirectory(mediaDirPath);
    }

    @Override
    public String getStatementMediaFileURL(String userJid, String lessonJid, String filename) {
        List<String> mediaFilePath = LessonServiceUtils.appendPath(getStatementMediaDirPath(userJid, lessonJid), filename);
        return fileSystemProvider.getURL(mediaFilePath);
    }

    @Override
    public List<GitCommit> getVersions(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getRootDirPath(fileSystemProvider, userJid, lessonJid);
        return gitProvider.getLog(root);
    }

    @Override
    public void initRepository(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getRootDirPath(fileSystemProvider, null, lessonJid);

        gitProvider.init(root);
        gitProvider.addAll(root);
        gitProvider.commit(root, userJid, "no@email.com", "Initial commit", "");
    }

    @Override
    public boolean userCloneExists(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        return fileSystemProvider.directoryExists(root);
    }

    @Override
    public void createUserCloneIfNotExists(String userJid, String lessonJid) {
        List<String> origin = LessonServiceUtils.getOriginDirPath(lessonJid);
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        if (!fileSystemProvider.directoryExists(root)) {
            gitProvider.clone(origin, root);
        }
    }

    @Override
    public boolean commitThenMergeUserClone(String userJid, String lessonJid, String title, String description) {
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        gitProvider.addAll(root);
        gitProvider.commit(root, userJid, "no@email.com", title, description);
        boolean success = gitProvider.rebase(root);

        if (!success) {
            gitProvider.resetToParent(root);
        }

        return success;
    }

    @Override
    public boolean updateUserClone(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        gitProvider.addAll(root);
        gitProvider.commit(root, userJid, "no@email.com", "dummy", "dummy");
        boolean success = gitProvider.rebase(root);

        gitProvider.resetToParent(root);

        return success;
    }

    @Override
    public boolean pushUserClone(String userJid, String lessonJid) {
        List<String> origin = LessonServiceUtils.getOriginDirPath(lessonJid);
        List<String> root = LessonServiceUtils.getRootDirPath(fileSystemProvider, userJid, lessonJid);

        if (gitProvider.push(root)) {
            gitProvider.resetHard(origin);
            return true;
        }
        return false;
    }

    @Override
    public boolean fetchUserClone(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getRootDirPath(fileSystemProvider, userJid, lessonJid);

        return gitProvider.fetch(root);
    }

    @Override
    public void discardUserClone(String userJid, String lessonJid) throws IOException {
        List<String> root = LessonServiceUtils.getRootDirPath(fileSystemProvider, userJid, lessonJid);

        fileSystemProvider.removeFile(root);
    }

    @Override
    public void restore(String lessonJid, String hash) {
        List<String> root = LessonServiceUtils.getOriginDirPath(lessonJid);

        gitProvider.restore(root, hash);
    }

    private Lesson createLessonFromModel(LessonModel lessonModel) {
        return new Lesson(lessonModel.id, lessonModel.jid, lessonModel.name, lessonModel.userCreate, lessonModel.additionalNote, new Date(lessonModel.timeUpdate));
    }

    private void initStatements(String lessonJid, String initialLanguageCode) throws IOException {
        List<String> statementsDirPath = getStatementsDirPath(null, lessonJid);
        fileSystemProvider.createDirectory(statementsDirPath);

        List<String> mediaDirPath = getStatementMediaDirPath(null, lessonJid);
        fileSystemProvider.createDirectory(mediaDirPath);
        fileSystemProvider.createFile(LessonServiceUtils.appendPath(mediaDirPath, ".gitkeep"));

        fileSystemProvider.createFile(getStatementFilePath(null, lessonJid, initialLanguageCode));
        fileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(null, lessonJid), initialLanguageCode);

        Map<String, StatementLanguageStatus> initialLanguage = ImmutableMap.of(initialLanguageCode, StatementLanguageStatus.ENABLED);
        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(null, lessonJid), new Gson().toJson(initialLanguage));
    }

    private List<String> getStatementsDirPath(String userJid, String lessonJid) {
        return LessonServiceUtils.appendPath(LessonServiceUtils.getRootDirPath(fileSystemProvider, userJid, lessonJid), "statements");
    }

    private List<String> getStatementFilePath(String userJid, String lessonJid, String languageCode) {
        return LessonServiceUtils.appendPath(getStatementsDirPath(userJid, lessonJid), languageCode + ".html");
    }

    private List<String> getStatementDefaultLanguageFilePath(String userJid, String lessonJid) {
        return LessonServiceUtils.appendPath(getStatementsDirPath(userJid, lessonJid), "defaultLanguage.txt");
    }

    private List<String> getStatementAvailableLanguagesFilePath(String userJid, String lessonJid) {
        return LessonServiceUtils.appendPath(getStatementsDirPath(userJid, lessonJid), "availableLanguages.txt");
    }

    private List<String> getStatementMediaDirPath(String userJid, String lessonJid) {
        return LessonServiceUtils.appendPath(getStatementsDirPath(userJid, lessonJid), "resources");
    }

    private LessonPartner createLessonPartnerFromModel(LessonPartnerModel lessonPartnerModel) {
        return new LessonPartner(lessonPartnerModel.id, lessonPartnerModel.lessonJid, lessonPartnerModel.userJid, lessonPartnerModel.config);
    }
}
