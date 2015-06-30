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
import org.iatoki.judgels.commons.JidService;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemPartner;
import org.iatoki.judgels.sandalphon.ProblemPartnerChildConfig;
import org.iatoki.judgels.sandalphon.ProblemPartnerConfig;
import org.iatoki.judgels.sandalphon.ProblemPartnerNotFoundException;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.StatementLanguageStatus;
import org.iatoki.judgels.sandalphon.models.daos.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.ProblemPartnerDao;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel;
import org.iatoki.judgels.sandalphon.models.entities.ProblemModel_;
import org.iatoki.judgels.sandalphon.models.entities.ProblemPartnerModel;
import org.iatoki.judgels.sandalphon.models.entities.ProblemPartnerModel_;
import org.iatoki.judgels.sandalphon.services.ProblemService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Singleton
@Named("problemService")
public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao problemDao;
    private final ProblemPartnerDao problemPartnerDao;
    private final FileSystemProvider problemFileSystemProvider;
    private final GitProvider problemGitProvider;

    @Inject
    public ProblemServiceImpl(ProblemDao problemDao, ProblemPartnerDao problemPartnerDao, FileSystemProvider problemFileSystemProvider, GitProvider problemGitProvider) {
        this.problemDao = problemDao;
        this.problemPartnerDao = problemPartnerDao;
        this.problemFileSystemProvider = problemFileSystemProvider;
        this.problemGitProvider = problemGitProvider;
    }

    @Override
    public Problem createProblem(ProblemType type, String name, String additionalNote, String initialLanguageCode) throws IOException {
        ProblemModel problemModel = new ProblemModel();
        problemModel.name = name;
        problemModel.additionalNote = additionalNote;

        problemDao.persist(problemModel, type.ordinal(), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatements(problemModel.jid, initialLanguageCode);
        problemFileSystemProvider.createDirectory(ProblemServiceUtils.getClonesDirPath(problemModel.jid));

        return createProblemFromModel(problemModel);
    }

    @Override
    public boolean problemExistsByJid(String problemJid) {
        return problemDao.existsByJid(problemJid);
    }

    @Override
    public final Problem findProblemById(long problemId) throws ProblemNotFoundException {
        ProblemModel problemModel = problemDao.findById(problemId);
        if (problemModel != null) {
            return createProblemFromModel(problemModel);
        } else {
            throw new ProblemNotFoundException("Problem not found.");
        }
    }

    @Override
    public final Problem findProblemByJid(String problemJid) {
        ProblemModel problemModel = problemDao.findByJid(problemJid);
        return createProblemFromModel(problemModel);
    }

    @Override
    public boolean isProblemPartnerByUserJid(String problemJid, String userJid) {
        return problemPartnerDao.existsByProblemJidAndPartnerJid(problemJid, userJid);
    }

    @Override
    public void createProblemPartner(long problemId, String userJid, ProblemPartnerConfig baseConfig, ProblemPartnerChildConfig childConfig) {
        ProblemModel problemModel = problemDao.findById(problemId);

        ProblemPartnerModel problemPartnerModel = new ProblemPartnerModel();
        problemPartnerModel.problemJid = problemModel.jid;
        problemPartnerModel.userJid = userJid;
        problemPartnerModel.baseConfig = new Gson().toJson(baseConfig);
        problemPartnerModel.childConfig = new Gson().toJson(childConfig);

        problemPartnerDao.persist(problemPartnerModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateProblemPartner(long problemPartnerId, ProblemPartnerConfig baseConfig, ProblemPartnerChildConfig childConfig) {
        ProblemPartnerModel problemPartnerModel = problemPartnerDao.findById(problemPartnerId);
        problemPartnerModel.baseConfig = new Gson().toJson(baseConfig);
        problemPartnerModel.childConfig = new Gson().toJson(childConfig);

        problemPartnerDao.edit(problemPartnerModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<ProblemPartner> pageProblemPartners(String problemJid, long pageIndex, long pageSize, String orderBy, String orderDir) {
        long totalRows = problemPartnerDao.countByFilters("", ImmutableMap.of(ProblemPartnerModel_.problemJid, problemJid), ImmutableMap.of());
        List<ProblemPartnerModel> problemPartnerModels = problemPartnerDao.findSortedByFilters(orderBy, orderDir, "", ImmutableMap.of(ProblemPartnerModel_.problemJid, problemJid), ImmutableMap.of(), pageIndex, pageIndex * pageSize);
        List<ProblemPartner> problemPartners = Lists.transform(problemPartnerModels, m -> createProblemPartnerFromModel(m));

        return new Page<>(problemPartners, totalRows, pageIndex, pageSize);
    }

    @Override
    public ProblemPartner findProblemPartnerByProblemPartnerId(long problemPartnerId) throws ProblemPartnerNotFoundException {
        ProblemPartnerModel problemPartnerModel = problemPartnerDao.findById(problemPartnerId);
        if (problemPartnerModel != null) {
            return createProblemPartnerFromModel(problemPartnerModel);
        } else {
            throw new ProblemPartnerNotFoundException("Problem partner not found.");
        }
    }

    @Override
    public ProblemPartner findProblemPartnerByProblemJidAndPartnerJid(String problemJid, String partnerJid) {
        ProblemPartnerModel problemPartnerModel = problemPartnerDao.findByProblemJidAndPartnerJid(problemJid, partnerJid);

        return createProblemPartnerFromModel(problemPartnerModel);
    }

    @Override
    public void updateProblem(long problemId, String name, String additionalNote) {
        ProblemModel problemModel = problemDao.findById(problemId);
        problemModel.name = name;
        problemModel.additionalNote = additionalNote;

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString, String userJid, boolean isAdmin) {
        if (isAdmin) {
            long totalRows = problemDao.countByFilters(filterString);
            List<ProblemModel> problemModels = problemDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

            List<Problem> problems = Lists.transform(problemModels, m -> createProblemFromModel(m));
            return new Page<>(problems, totalRows, pageIndex, pageSize);
        } else {
            List<String> problemJidsWhereIsAuthor = problemDao.findProblemJidsByAuthorJid(userJid);
            List<String> problemJidsWhereIsPartner = problemPartnerDao.findProblemJidsByPartnerJid(userJid);

            ImmutableSet.Builder<String> allowedProblemJidsBuilder = ImmutableSet.builder();
            allowedProblemJidsBuilder.addAll(problemJidsWhereIsAuthor);
            allowedProblemJidsBuilder.addAll(problemJidsWhereIsPartner);

            Set<String> allowedProblemJids = allowedProblemJidsBuilder.build();

            long totalRows = problemDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of(ProblemModel_.jid, allowedProblemJids));
            List<ProblemModel> problemModels = problemDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(ProblemModel_.jid, allowedProblemJids), pageIndex * pageSize, pageSize);

            List<Problem> problems = Lists.transform(problemModels, m -> createProblemFromModel(m));
            return new Page<>(problems, totalRows, pageIndex, pageSize);
        }

    }

    @Override
    public Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String problemJid) throws IOException {
        String langs = problemFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        return new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());
    }

    @Override
    public void addLanguage(String userJid, String problemJid, String languageCode) throws IOException {
        String langs = problemFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        String defaultLanguageStatement = problemFileSystemProvider.readFromFile(getStatementFilePath(userJid, problemJid, getDefaultLanguage(userJid, problemJid)));
        problemFileSystemProvider.writeToFile(getStatementFilePath(userJid, problemJid, languageCode), defaultLanguageStatement);
        problemFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void enableLanguage(String userJid, String problemJid, String languageCode) throws IOException {
        String langs = problemFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        problemFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void disableLanguage(String userJid, String problemJid, String languageCode) throws IOException {
        String langs = problemFileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.DISABLED);

        problemFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void makeDefaultLanguage(String userJid, String problemJid, String languageCode) throws IOException {
        problemFileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(userJid, problemJid), languageCode);
    }

    @Override
    public String getDefaultLanguage(String userJid, String problemJid) throws IOException {
        return problemFileSystemProvider.readFromFile(getStatementDefaultLanguageFilePath(userJid, problemJid));
    }

    @Override
    public String getStatement(String userJid, String problemJid, String languageCode) throws IOException {
        return problemFileSystemProvider.readFromFile(getStatementFilePath(userJid, problemJid, languageCode));
    }

    @Override
    public void updateStatement(String userJid, long problemId, String languageCode, String statement) throws IOException {
        ProblemModel problemModel = problemDao.findById(problemId);
        problemFileSystemProvider.writeToFile(getStatementFilePath(userJid, problemModel.jid, languageCode), statement);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFile(String userJid, long id, File mediaFile, String filename) throws IOException {
        ProblemModel problemModel = problemDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, problemModel.jid);
        problemFileSystemProvider.uploadFile(mediaDirPath, mediaFile, filename);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFileZipped(String userJid, long id, File mediaFileZipped) throws IOException {
        ProblemModel problemModel = problemDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, problemModel.jid);
        problemFileSystemProvider.uploadZippedFiles(mediaDirPath, mediaFileZipped, false);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public List<FileInfo> getStatementMediaFiles(String userJid, String problemJid) {
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, problemJid);
        return problemFileSystemProvider.listFilesInDirectory(mediaDirPath);
    }

    @Override
    public String getStatementMediaFileURL(String userJid, String problemJid, String filename) {
        List<String> mediaFilePath = ProblemServiceUtils.appendPath(getStatementMediaDirPath(userJid, problemJid), filename);
        return problemFileSystemProvider.getURL(mediaFilePath);
    }

    @Override
    public List<GitCommit> getVersions(String userJid, String problemJid) {
        List<String> root = ProblemServiceUtils.getRootDirPath(problemFileSystemProvider, userJid, problemJid);
        return problemGitProvider.getLog(root);
    }

    @Override
    public void initRepository(String userJid, String problemJid) {
        List<String> root = ProblemServiceUtils.getRootDirPath(problemFileSystemProvider, null, problemJid);

        problemGitProvider.init(root);
        problemGitProvider.addAll(root);
        problemGitProvider.commit(root, userJid, "no@email.com", "Initial commit", "");
    }

    @Override
    public boolean userCloneExists(String userJid, String problemJid) {
        List<String> root = ProblemServiceUtils.getCloneDirPath(userJid, problemJid);

        return problemFileSystemProvider.directoryExists(root);
    }

    @Override
    public void createUserCloneIfNotExists(String userJid, String problemJid) {
        List<String> origin = ProblemServiceUtils.getOriginDirPath(problemJid);
        List<String> root = ProblemServiceUtils.getCloneDirPath(userJid, problemJid);

        if (!problemFileSystemProvider.directoryExists(root)) {
            problemGitProvider.clone(origin, root);
        }
    }

    @Override
    public boolean commitThenMergeUserClone(String userJid, String problemJid, String title, String description) {
        List<String> root = ProblemServiceUtils.getCloneDirPath(userJid, problemJid);

        problemGitProvider.addAll(root);
        problemGitProvider.commit(root, userJid, "no@email.com", title, description);
        boolean success = problemGitProvider.rebase(root);

        if (!success) {
            problemGitProvider.resetToParent(root);
        }

        return success;
    }

    @Override
    public boolean updateUserClone(String userJid, String problemJid) {
        List<String> root = ProblemServiceUtils.getCloneDirPath(userJid, problemJid);

        problemGitProvider.addAll(root);
        problemGitProvider.commit(root, userJid, "no@email.com", "dummy", "dummy");
        boolean success = problemGitProvider.rebase(root);

        problemGitProvider.resetToParent(root);

        return success;
    }

    @Override
    public boolean pushUserClone(String userJid, String problemJid) {
        List<String> origin = ProblemServiceUtils.getOriginDirPath(problemJid);
        List<String> root = ProblemServiceUtils.getRootDirPath(problemFileSystemProvider, userJid, problemJid);

        if (problemGitProvider.push(root)) {
            problemGitProvider.resetHard(origin);
            return true;
        }
        return false;
    }

    @Override
    public boolean fetchUserClone(String userJid, String problemJid) {
        List<String> root = ProblemServiceUtils.getRootDirPath(problemFileSystemProvider, userJid, problemJid);

        return problemGitProvider.fetch(root);
    }

    @Override
    public void discardUserClone(String userJid, String problemJid) throws IOException {
        List<String> root = ProblemServiceUtils.getRootDirPath(problemFileSystemProvider, userJid, problemJid);

        problemFileSystemProvider.removeFile(root);
    }

    @Override
    public void restore(String problemJid, String hash) {
        List<String> root = ProblemServiceUtils.getOriginDirPath(problemJid);

        problemGitProvider.restore(root, hash);
    }

    private ProblemType getProblemType(ProblemModel problemModel) {
        String prefix = JidService.getInstance().parsePrefix(problemModel.jid);

        if (prefix.equals("PROG")) {
            return ProblemType.PROGRAMMING;
        } else if (prefix.equals("BUND")) {
            return ProblemType.BUNDLE;
        } else {
            throw new IllegalStateException("Unknown problem type: " + prefix);
        }
    }

    private Problem createProblemFromModel(ProblemModel problemModel) {
        return new Problem(problemModel.id, problemModel.jid, problemModel.name, problemModel.userCreate, problemModel.additionalNote, new Date(problemModel.timeUpdate), getProblemType(problemModel));
    }

    private void initStatements(String problemJid, String initialLanguageCode) throws IOException {
        List<String> statementsDirPath = getStatementsDirPath(null, problemJid);
        problemFileSystemProvider.createDirectory(statementsDirPath);

        List<String> mediaDirPath = getStatementMediaDirPath(null, problemJid);
        problemFileSystemProvider.createDirectory(mediaDirPath);
        problemFileSystemProvider.createFile(ProblemServiceUtils.appendPath(mediaDirPath, ".gitkeep"));

        problemFileSystemProvider.createFile(getStatementFilePath(null, problemJid, initialLanguageCode));
        problemFileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(null, problemJid), initialLanguageCode);

        Map<String, StatementLanguageStatus> initialLanguage = ImmutableMap.of(initialLanguageCode, StatementLanguageStatus.ENABLED);
        problemFileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(null, problemJid), new Gson().toJson(initialLanguage));
    }

    private List<String> getStatementsDirPath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(ProblemServiceUtils.getRootDirPath(problemFileSystemProvider, userJid, problemJid), "statements");
    }

    private List<String> getStatementFilePath(String userJid, String problemJid, String languageCode) {
        return ProblemServiceUtils.appendPath(getStatementsDirPath(userJid, problemJid), languageCode + ".html");
    }

    private List<String> getStatementDefaultLanguageFilePath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getStatementsDirPath(userJid, problemJid), "defaultLanguage.txt");
    }

    private List<String> getStatementAvailableLanguagesFilePath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getStatementsDirPath(userJid, problemJid), "availableLanguages.txt");
    }

    private List<String> getStatementMediaDirPath(String userJid, String problemJid) {
        return ProblemServiceUtils.appendPath(getStatementsDirPath(userJid, problemJid), "resources");
    }

    private ProblemPartner createProblemPartnerFromModel(ProblemPartnerModel problemPartnerModel) {
        return new ProblemPartner(problemPartnerModel.id, problemPartnerModel.problemJid, problemPartnerModel.userJid, problemPartnerModel.baseConfig, problemPartnerModel.childConfig);
    }
}
