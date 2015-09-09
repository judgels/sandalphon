package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.iatoki.judgels.FileInfo;
import org.iatoki.judgels.FileSystemProvider;
import org.iatoki.judgels.GitCommit;
import org.iatoki.judgels.GitProvider;
import org.iatoki.judgels.play.IdentityUtils;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.sandalphon.Lesson;
import org.iatoki.judgels.sandalphon.LessonNotFoundException;
import org.iatoki.judgels.sandalphon.LessonPartner;
import org.iatoki.judgels.sandalphon.LessonPartnerConfig;
import org.iatoki.judgels.sandalphon.LessonPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.LessonStatement;
import org.iatoki.judgels.sandalphon.ProblemStatement;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.config.LessonFileSystemProvider;
import org.iatoki.judgels.sandalphon.config.LessonGitProvider;
import org.iatoki.judgels.sandalphon.models.daos.LessonDao;
import org.iatoki.judgels.sandalphon.models.daos.LessonPartnerDao;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel_;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel_;
import org.iatoki.judgels.sandalphon.services.LessonService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@Named("lessonService")
public final class LessonServiceImpl implements LessonService {

    private final LessonDao lessonDao;
    private final FileSystemProvider lessonFileSystemProvider;
    private final GitProvider lessonGitProvider;
    private final LessonPartnerDao lessonPartnerDao;

    @Inject
    public LessonServiceImpl(LessonDao lessonDao, @LessonFileSystemProvider FileSystemProvider lessonFileSystemProvider, @LessonGitProvider GitProvider lessonGitProvider, LessonPartnerDao lessonPartnerDao) {
        this.lessonDao = lessonDao;
        this.lessonFileSystemProvider = lessonFileSystemProvider;
        this.lessonGitProvider = lessonGitProvider;
        this.lessonPartnerDao = lessonPartnerDao;
    }

    @Override
    public Lesson createLesson(String slug, String additionalNote, String initialLanguageCode) throws IOException {
        LessonModel lessonModel = new LessonModel();
        lessonModel.slug = slug;
        lessonModel.additionalNote = additionalNote;

        lessonDao.persist(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatements(lessonModel.jid, initialLanguageCode);
        lessonFileSystemProvider.createDirectory(LessonServiceUtils.getClonesDirPath(lessonModel.jid));

        return LessonServiceUtils.createLessonFromModel(lessonModel);
    }

    @Override
    public boolean lessonExistsByJid(String lessonJid) {
        return lessonDao.existsByJid(lessonJid);
    }

    @Override
    public boolean lessonExistsBySlug(String slug) {
        return lessonDao.existsBySlug(slug);
    }

    @Override
    public Lesson findLessonById(long lessonId) throws LessonNotFoundException {
        LessonModel lessonModel = lessonDao.findById(lessonId);
        if (lessonModel == null) {
            throw new LessonNotFoundException("Lesson not found.");
        }

        return LessonServiceUtils.createLessonFromModel(lessonModel);
    }

    @Override
    public Lesson findLessonByJid(String lessonJid) {
        LessonModel lessonModel = lessonDao.findByJid(lessonJid);

        return LessonServiceUtils.createLessonFromModel(lessonModel);
    }

    @Override
    public boolean isUserPartnerForLesson(String lessonJid, String userJid) {
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
    public Page<LessonPartner> getPageOfLessonPartners(String lessonJid, long pageIndex, long pageSize, String orderBy, String orderDir) {
        long totalRows = lessonPartnerDao.countByFilters("", ImmutableMap.of(LessonPartnerModel_.lessonJid, lessonJid), ImmutableMap.of());
        List<LessonPartnerModel> lessonPartnerModels = lessonPartnerDao.findSortedByFilters(orderBy, orderDir, "", ImmutableMap.of(LessonPartnerModel_.lessonJid, lessonJid), ImmutableMap.of(), pageIndex, pageIndex * pageSize);
        List<LessonPartner> lessonPartners = Lists.transform(lessonPartnerModels, m -> LessonServiceUtils.createLessonPartnerFromModel(m));

        return new Page<>(lessonPartners, totalRows, pageIndex, pageSize);
    }

    @Override
    public LessonPartner findLessonPartnerById(long lessonPartnerId) throws LessonPartnerNotFoundException {
        LessonPartnerModel lessonPartnerModel = lessonPartnerDao.findById(lessonPartnerId);
        if (lessonPartnerModel == null) {
            throw new LessonPartnerNotFoundException("Lesson partner not found.");
        }

        return LessonServiceUtils.createLessonPartnerFromModel(lessonPartnerModel);
    }

    @Override
    public LessonPartner findLessonPartnerByLessonJidAndPartnerJid(String lessonJid, String partnerJid) {
        LessonPartnerModel lessonPartnerModel = lessonPartnerDao.findByLessonJidAndPartnerJid(lessonJid, partnerJid);

        return LessonServiceUtils.createLessonPartnerFromModel(lessonPartnerModel);
    }

    @Override
    public void updateLesson(long lessonId, String slug, String additionalNote) {
        LessonModel lessonModel = lessonDao.findById(lessonId);
        lessonModel.slug = slug;
        lessonModel.additionalNote = additionalNote;

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<Lesson> getPageOfLessons(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString, String userJid, boolean isAdmin) {
        if (isAdmin) {
            long totalRows = lessonDao.countByFilters(filterString);
            List<LessonModel> lessonModels = lessonDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

            List<Lesson> lessons = Lists.transform(lessonModels, m -> LessonServiceUtils.createLessonFromModel(m));
            return new Page<>(lessons, totalRows, pageIndex, pageSize);
        } else {
            List<String> lessonJidsWhereIsAuthor = lessonDao.getJidsByAuthorJid(userJid);
            List<String> lessonJidsWhereIsPartner = lessonPartnerDao.getLessonJidsByPartnerJid(userJid);

            ImmutableSet.Builder<String> allowedLessonJidsBuilder = ImmutableSet.builder();
            allowedLessonJidsBuilder.addAll(lessonJidsWhereIsAuthor);
            allowedLessonJidsBuilder.addAll(lessonJidsWhereIsPartner);

            Set<String> allowedLessonJids = allowedLessonJidsBuilder.build();

            long totalRows = lessonDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of(LessonModel_.jid, allowedLessonJids));
            List<LessonModel> lessonModels = lessonDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(LessonModel_.jid, allowedLessonJids), pageIndex * pageSize, pageSize);

            List<Lesson> lessons = Lists.transform(lessonModels, m -> LessonServiceUtils.createLessonFromModel(m));
            return new Page<>(lessons, totalRows, pageIndex, pageSize);
        }

    }

    @Override
    public Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String lessonJid) throws IOException {
        String langs = lessonFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        return new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() { }.getType());
    }

    @Override
    public void addLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        String langs = lessonFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() { }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        ProblemStatement defaultLanguageStatement = getStatement(userJid, lessonJid, getDefaultLanguage(userJid, lessonJid));
        lessonFileSystemProvider.writeToFile(getStatementTitleFilePath(userJid, lessonJid, languageCode), defaultLanguageStatement.getTitle());
        lessonFileSystemProvider.writeToFile(getStatementTextFilePath(userJid, lessonJid, languageCode), defaultLanguageStatement.getText());
        lessonFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void enableLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        String langs = lessonFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() { }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        lessonFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void disableLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        String langs = lessonFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() { }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.DISABLED);

        lessonFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, lessonJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void makeDefaultLanguage(String userJid, String lessonJid, String languageCode) throws IOException {
        lessonFileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(userJid, lessonJid), languageCode);
    }

    @Override
    public String getDefaultLanguage(String userJid, String lessonJid) throws IOException {
        return lessonFileSystemProvider.readFromFile(getStatementDefaultLanguageFilePath(userJid, lessonJid));
    }

    @Override
    public ProblemStatement getStatement(String userJid, String lessonJid, String languageCode) throws IOException {
        String title = lessonFileSystemProvider.readFromFile(getStatementTitleFilePath(userJid, lessonJid, languageCode));
        String text = lessonFileSystemProvider.readFromFile(getStatementTextFilePath(userJid, lessonJid, languageCode));

        return new ProblemStatement(title, text);
    }

    @Override
    public Map<String, String> getTitlesByLanguage(String userJid, String lessonJid) throws IOException {
        Map<String, StatementLanguageStatus> availableLanguages = getAvailableLanguages(userJid, lessonJid);

        ImmutableMap.Builder<String, String> titlesByLanguageBuilder = ImmutableMap.builder();

        for (Map.Entry<String, StatementLanguageStatus> entry : availableLanguages.entrySet()) {
            if (entry.getValue() == StatementLanguageStatus.ENABLED) {
                String title = lessonFileSystemProvider.readFromFile(getStatementTitleFilePath(userJid, lessonJid, entry.getKey()));
                titlesByLanguageBuilder.put(entry.getKey(), title);
            }
        }

        return titlesByLanguageBuilder.build();
    }

    @Override
    public void updateStatement(String userJid, long lessonId, String languageCode, LessonStatement statement) throws IOException {
        LessonModel lessonModel = lessonDao.findById(lessonId);
        lessonFileSystemProvider.writeToFile(getStatementTitleFilePath(userJid, lessonModel.jid, languageCode), statement.getTitle());
        lessonFileSystemProvider.writeToFile(getStatementTextFilePath(userJid, lessonModel.jid, languageCode), statement.getText());

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFile(String userJid, long id, File mediaFile, String filename) throws IOException {
        LessonModel lessonModel = lessonDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, lessonModel.jid);
        lessonFileSystemProvider.uploadFile(mediaDirPath, mediaFile, filename);

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFileZipped(String userJid, long id, File mediaFileZipped) throws IOException {
        LessonModel lessonModel = lessonDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, lessonModel.jid);
        lessonFileSystemProvider.uploadZippedFiles(mediaDirPath, mediaFileZipped, false);

        lessonDao.edit(lessonModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public List<FileInfo> getStatementMediaFiles(String userJid, String lessonJid) {
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, lessonJid);
        return lessonFileSystemProvider.listFilesInDirectory(mediaDirPath);
    }

    @Override
    public String getStatementMediaFileURL(String userJid, String lessonJid, String filename) {
        List<String> mediaFilePath = LessonServiceUtils.appendPath(getStatementMediaDirPath(userJid, lessonJid), filename);
        return lessonFileSystemProvider.getURL(mediaFilePath);
    }

    @Override
    public List<GitCommit> getVersions(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getRootDirPath(lessonFileSystemProvider, userJid, lessonJid);
        return lessonGitProvider.getLog(root);
    }

    @Override
    public void initRepository(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getRootDirPath(lessonFileSystemProvider, null, lessonJid);

        lessonGitProvider.init(root);
        lessonGitProvider.addAll(root);
        lessonGitProvider.commit(root, userJid, "no@email.com", "Initial commit", "");
    }

    @Override
    public boolean userCloneExists(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        return lessonFileSystemProvider.directoryExists(root);
    }

    @Override
    public void createUserCloneIfNotExists(String userJid, String lessonJid) {
        List<String> origin = LessonServiceUtils.getOriginDirPath(lessonJid);
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        if (!lessonFileSystemProvider.directoryExists(root)) {
            lessonGitProvider.clone(origin, root);
        }
    }

    @Override
    public boolean commitThenMergeUserClone(String userJid, String lessonJid, String title, String description) {
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        lessonGitProvider.addAll(root);
        lessonGitProvider.commit(root, userJid, "no@email.com", title, description);
        boolean success = lessonGitProvider.rebase(root);

        if (!success) {
            lessonGitProvider.resetToParent(root);
        }

        return success;
    }

    @Override
    public boolean updateUserClone(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getCloneDirPath(userJid, lessonJid);

        lessonGitProvider.addAll(root);
        lessonGitProvider.commit(root, userJid, "no@email.com", "dummy", "dummy");
        boolean success = lessonGitProvider.rebase(root);

        lessonGitProvider.resetToParent(root);

        return success;
    }

    @Override
    public boolean pushUserClone(String userJid, String lessonJid) {
        List<String> origin = LessonServiceUtils.getOriginDirPath(lessonJid);
        List<String> root = LessonServiceUtils.getRootDirPath(lessonFileSystemProvider, userJid, lessonJid);

        if (lessonGitProvider.push(root)) {
            lessonGitProvider.resetHard(origin);
            return true;
        }
        return false;
    }

    @Override
    public boolean fetchUserClone(String userJid, String lessonJid) {
        List<String> root = LessonServiceUtils.getRootDirPath(lessonFileSystemProvider, userJid, lessonJid);

        return lessonGitProvider.fetch(root);
    }

    @Override
    public void discardUserClone(String userJid, String lessonJid) throws IOException {
        List<String> root = LessonServiceUtils.getRootDirPath(lessonFileSystemProvider, userJid, lessonJid);

        lessonFileSystemProvider.removeFile(root);
    }

    @Override
    public void restore(String lessonJid, String hash) {
        List<String> root = LessonServiceUtils.getOriginDirPath(lessonJid);

        lessonGitProvider.restore(root, hash);
    }

    private void initStatements(String lessonJid, String initialLanguageCode) throws IOException {
        List<String> statementsDirPath = getStatementsDirPath(null, lessonJid);
        lessonFileSystemProvider.createDirectory(statementsDirPath);

        List<String> statementDirPath = getStatementDirPath(null, lessonJid, initialLanguageCode);
        lessonFileSystemProvider.createDirectory(statementDirPath);

        List<String> mediaDirPath = getStatementMediaDirPath(null, lessonJid);
        lessonFileSystemProvider.createDirectory(mediaDirPath);
        lessonFileSystemProvider.createFile(LessonServiceUtils.appendPath(mediaDirPath, ".gitkeep"));

        lessonFileSystemProvider.createFile(getStatementTitleFilePath(null, lessonJid, initialLanguageCode));
        lessonFileSystemProvider.createFile(getStatementTextFilePath(null, lessonJid, initialLanguageCode));
        lessonFileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(null, lessonJid), initialLanguageCode);

        Map<String, StatementLanguageStatus> initialLanguage = ImmutableMap.of(initialLanguageCode, StatementLanguageStatus.ENABLED);
        lessonFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(null, lessonJid), new Gson().toJson(initialLanguage));
    }

    private List<String> getStatementsDirPath(String userJid, String lessonJid) {
        return LessonServiceUtils.appendPath(LessonServiceUtils.getRootDirPath(lessonFileSystemProvider, userJid, lessonJid), "statements");
    }

    private List<String> getStatementDirPath(String userJid, String lessonJid, String languageCode) {
        return LessonServiceUtils.appendPath(getStatementsDirPath(userJid, lessonJid), languageCode);
    }

    private List<String> getStatementTitleFilePath(String userJid, String lessonJid, String languageCode) {
        return LessonServiceUtils.appendPath(getStatementDirPath(userJid, lessonJid, languageCode), "title.txt");
    }

    private List<String> getStatementTextFilePath(String userJid, String lessonJid, String languageCode) {
        return LessonServiceUtils.appendPath(getStatementDirPath(userJid, lessonJid, languageCode), "text.html");
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
}
