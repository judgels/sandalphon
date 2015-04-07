package org.iatoki.judgels.sandalphon;

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
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemPartnerDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel_;
import org.iatoki.judgels.sandalphon.models.domains.ProblemPartnerModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ProblemServiceImpl implements ProblemService {
    private final ProblemDao problemDao;
    private final ProblemPartnerDao problemPartnerDao;
    private final FileSystemProvider fileSystemProvider;
    private final GitProvider gitProvider;

    public ProblemServiceImpl(ProblemDao problemDao, ProblemPartnerDao problemPartnerDao, FileSystemProvider fileSystemProvider, GitProvider gitProvider) {
        this.problemDao = problemDao;
        this.problemPartnerDao = problemPartnerDao;
        this.fileSystemProvider = fileSystemProvider;
        this.gitProvider = gitProvider;
    }

    @Override
    public Problem createProblem(ProblemType type, String name, String additionalNote, String initialLanguageCode) {
        ProblemModel problemModel = new ProblemModel();
        problemModel.name = name;
        problemModel.additionalNote = additionalNote;

        problemDao.persist(problemModel, type.ordinal(), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatements(problemModel.jid, initialLanguageCode);
        fileSystemProvider.createDirectory(getClonesDirPath(problemModel.jid));

        return createProblemFromModel(problemModel);
    }

    @Override
    public boolean problemExistsByJid(String problemJid) {
        return problemDao.existsByJid(problemJid);
    }

    @Override
    public final Problem findProblemById(long problemId) {
        ProblemModel problemModel = problemDao.findById(problemId);
        return createProblemFromModel(problemModel);
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
        Map<String, String> filterColumns = ImmutableMap.of("problemJid", problemJid);
        long totalRows = problemPartnerDao.countByFilters("", filterColumns);
        List<ProblemPartnerModel> problemPartnerModels = problemPartnerDao.findSortedByFilters(orderBy, orderDir, "", filterColumns, pageIndex, pageIndex * pageSize);
        List<ProblemPartner> problemPartners = Lists.transform(problemPartnerModels, m -> createProblemPartnerFromModel(m));

        return new Page<>(problemPartners, totalRows, pageIndex, pageSize);
    }

    @Override
    public ProblemPartner findProblemPartnerByProblemPartnerId(long problemPartnerId) {
        ProblemPartnerModel problemPartnerModel = problemPartnerDao.findById(problemPartnerId);

        return createProblemPartnerFromModel(problemPartnerModel);
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
            List<ProblemModel> problemModels = problemDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), pageIndex * pageSize, pageSize);

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
    public Map<String, StatementLanguageStatus> getAvailableLanguages(String userJid, String problemJid) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        return new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());
    }

    @Override
    public void addLanguage(String userJid, String problemJid, String languageCode) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        String defaultLanguageStatement = fileSystemProvider.readFromFile(getStatementFilePath(userJid, problemJid, getDefaultLanguage(userJid, problemJid)));
        fileSystemProvider.writeToFile(getStatementFilePath(userJid, problemJid, languageCode), defaultLanguageStatement);
        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void enableLanguage(String userJid, String problemJid, String languageCode) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void disableLanguage(String userJid, String problemJid, String languageCode) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(userJid, problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>() {
        }.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.DISABLED);

        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(userJid, problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void makeDefaultLanguage(String userJid, String problemJid, String languageCode) {
        fileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(userJid, problemJid), languageCode);
    }

    @Override
    public String getDefaultLanguage(String userJid, String problemJid) {
        return fileSystemProvider.readFromFile(getStatementDefaultLanguageFilePath(userJid, problemJid));
    }

    @Override
    public String getStatement(String userJid, String problemJid, String languageCode) {
        return fileSystemProvider.readFromFile(getStatementFilePath(userJid, problemJid, languageCode));
    }

    @Override
    public void updateStatement(String userJid, long problemId, String languageCode, String statement) {
        ProblemModel problemModel = problemDao.findById(problemId);
        fileSystemProvider.writeToFile(getStatementFilePath(userJid, problemModel.jid, languageCode), statement);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFile(String userJid, long id, File mediaFile, String filename) {
        ProblemModel problemModel = problemDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, problemModel.jid);
        fileSystemProvider.uploadFile(mediaDirPath, mediaFile, filename);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFileZipped(String userJid, long id, File mediaFileZipped) {
        ProblemModel problemModel = problemDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, problemModel.jid);
        fileSystemProvider.uploadZippedFiles(mediaDirPath, mediaFileZipped);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public List<FileInfo> getStatementMediaFiles(String userJid, String problemJid) {
        List<String> mediaDirPath = getStatementMediaDirPath(userJid, problemJid);
        return fileSystemProvider.listFilesInDirectory(mediaDirPath);
    }

    @Override
    public String getStatementMediaFileURL(String userJid, String problemJid, String filename) {
        List<String> mediaFilePath = appendPath(getStatementMediaDirPath(userJid, problemJid), filename);
        return fileSystemProvider.getURL(mediaFilePath);
    }

    @Override
    public List<GitCommit> getVersions(String userJid, String problemJid) {
        List<String> root = getRootDirPath(userJid, problemJid);
        return gitProvider.getLog(root);
    }

    @Override
    public void initRepository(String userJid, String problemJid) {
        List<String> root = getRootDirPath(null, problemJid);

        gitProvider.init(root);
        gitProvider.addAll(root);
        gitProvider.commit(root, userJid, "no@email.com", "Initial commit", "");
    }

    @Override
    public boolean userCloneExists(String userJid, String problemJid) {
        List<String> root = getCloneDirPath(userJid, problemJid);

        return fileSystemProvider.fileExists(root);
    }

    @Override
    public void createUserCloneIfNotExists(String userJid, String problemJid) {
        List<String> origin = getOriginDirPath(problemJid);
        List<String> root = getCloneDirPath(userJid, problemJid);

        if (!fileSystemProvider.fileExists(root)) {
            gitProvider.clone(origin, root);
        }
    }

    @Override
    public boolean commitThenMergeUserClone(String userJid, String problemJid, String title, String description) {
        List<String> root = getCloneDirPath(userJid, problemJid);

        gitProvider.addAll(root);
        gitProvider.commit(root, userJid, "no@email.com", title, description);
        boolean success = gitProvider.merge(root);

        if (!success) {
            gitProvider.resetSoftToParent(root);
        }

        return success;
    }

    @Override
    public boolean updateUserClone(String userJid, String problemJid) {
        List<String> root = getCloneDirPath(userJid, problemJid);

        gitProvider.addAll(root);
        gitProvider.commit(root, userJid, "no@email.com", "dummy", "dummy");
        boolean success = gitProvider.merge(root);

        gitProvider.resetSoftToParent(root);
        return success;
    }

    @Override
    public boolean pushUserClone(String userJid, String problemJid) {
        List<String> origin = getOriginDirPath(problemJid);
        List<String> root = getRootDirPath(userJid, problemJid);

        if (gitProvider.push(root)) {
            gitProvider.resetHard(origin);
            return true;
        }
        return false;
    }

    @Override
    public boolean fetchUserClone(String userJid, String problemJid) {
        List<String> root = getRootDirPath(userJid, problemJid);

        return gitProvider.fetch(root);
    }

    @Override
    public void discardUserClone(String userJid, String problemJid) {
        List<String> root = getRootDirPath(userJid, problemJid);

        fileSystemProvider.removeFile(root);
    }

    @Override
    public void restore(String problemJid, String hash) {
        List<String> root = getOriginDirPath(problemJid);

        gitProvider.restore(root, hash);
    }

    private ProblemType getProblemType(ProblemModel problemModel) {
        String prefix = JidService.getInstance().parsePrefix(problemModel.jid);
        if (prefix.equals("PROG")) {
            return ProblemType.PROGRAMMING;
        } else {
            throw new IllegalStateException("Unknown problem type: " + prefix);
        }
    }

    private Problem createProblemFromModel(ProblemModel problemModel) {
        return new Problem(problemModel.id, problemModel.jid, problemModel.name, problemModel.userCreate, problemModel.additionalNote, new Date(problemModel.timeUpdate), getProblemType(problemModel));
    }

    private void initStatements(String problemJid, String initialLanguageCode) {
        List<String> statementsDirPath = getStatementsDirPath(null, problemJid);
        fileSystemProvider.createDirectory(statementsDirPath);

        List<String> mediaDirPath = getStatementMediaDirPath(null, problemJid);
        fileSystemProvider.createDirectory(mediaDirPath);

        fileSystemProvider.createFile(getStatementFilePath(null, problemJid, initialLanguageCode));
        fileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(null, problemJid), initialLanguageCode);

        Map<String, StatementLanguageStatus> initialLanguage = ImmutableMap.of(initialLanguageCode, StatementLanguageStatus.ENABLED);
        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(null, problemJid), new Gson().toJson(initialLanguage));
    }

    private ArrayList<String> getOriginDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    private ArrayList<String> getClonesDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemClonesDirKey(), problemJid);
    }

    private ArrayList<String> getCloneDirPath(String userJid, String problemJid) {
        return appendPath(getClonesDirPath(problemJid), userJid);
    }

    private ArrayList<String> getRootDirPath(String userJid, String problemJid) {
        ArrayList<String> origin =  getOriginDirPath(problemJid);
        ArrayList<String> root = getCloneDirPath(userJid, problemJid);

        if (userJid == null || !fileSystemProvider.fileExists(root)) {
            return origin;
        } else {
            return root;
        }
    }

    private ArrayList<String> getStatementsDirPath(String userJid, String problemJid) {
        return appendPath(getRootDirPath(userJid, problemJid), "statements");
    }

    private ArrayList<String> getStatementFilePath(String userJid, String problemJid, String languageCode) {
        return appendPath(getStatementsDirPath(userJid, problemJid), languageCode + ".html");
    }

    private ArrayList<String> getStatementDefaultLanguageFilePath(String userJid, String problemJid) {
        return appendPath(getStatementsDirPath(userJid, problemJid), "defaultLanguage.txt");
    }

    private ArrayList<String> getStatementAvailableLanguagesFilePath(String userJid, String problemJid) {
        return appendPath(getStatementsDirPath(userJid, problemJid), "availableLanguages.txt");
    }

    private ArrayList<String> getStatementMediaDirPath(String userJid, String problemJid) {
        return appendPath(getStatementsDirPath(userJid, problemJid), "resources");
    }

    private ArrayList<String> appendPath(ArrayList<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }

    private ProblemPartner createProblemPartnerFromModel(ProblemPartnerModel problemPartnerModel) {
        return new ProblemPartner(problemPartnerModel.id, problemPartnerModel.problemJid, problemPartnerModel.userJid, problemPartnerModel.baseConfig, problemPartnerModel.childConfig);
    }
}
