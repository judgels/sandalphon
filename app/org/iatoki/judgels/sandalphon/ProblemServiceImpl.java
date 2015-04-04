package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import org.iatoki.judgels.commons.FileInfo;
import org.iatoki.judgels.commons.FileSystemProvider;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JidService;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

public final class ProblemServiceImpl implements ProblemService {
    private final ProblemDao problemDao;
    private final FileSystemProvider fileSystemProvider;

    public ProblemServiceImpl(ProblemDao problemDao, FileSystemProvider fileSystemProvider) {
        this.problemDao = problemDao;
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public Problem createProblem(ProblemType type, String name, String additionalNote, String initialLanguageCode) {
        ProblemModel problemModel = new ProblemModel();
        problemModel.name = name;
        problemModel.additionalNote = additionalNote;

        problemDao.persist(problemModel, type.ordinal(), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatements(problemModel.jid, initialLanguageCode);

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
    public void updateProblem(long problemId, String name, String additionalNote) {
        ProblemModel problemModel = problemDao.findById(problemId);
        problemModel.name = name;
        problemModel.additionalNote = additionalNote;

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<Problem> pageProblems(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) {
        long totalRows = problemDao.countByFilters(filterString, ImmutableMap.of());
        List<ProblemModel> problemModels = problemDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), pageIndex * pageSize, pageSize);

        List<Problem> problems = Lists.transform(problemModels, m -> createProblemFromModel(m));
        return new Page<>(problems, totalRows, pageIndex, pageSize);
    }

    @Override
    public Map<String, StatementLanguageStatus> getAvailableLanguages(String problemJid) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(problemJid));
        return new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>(){}.getType());
    }

    @Override
    public void addLanguage(String problemJid, String languageCode) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>(){}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        String defaultLanguageStatement = fileSystemProvider.readFromFile(getStatementFilePath(problemJid, getDefaultLanguage(problemJid)));
        fileSystemProvider.writeToFile(getStatementFilePath(problemJid, languageCode), defaultLanguageStatement);
        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void enableLanguage(String problemJid, String languageCode) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>(){}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.ENABLED);

        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void disableLanguage(String problemJid, String languageCode) {
        String langs = fileSystemProvider.readFromFile(getStatementAvailableLanguagesFilePath(problemJid));
        Map<String, StatementLanguageStatus> availableLanguages = new Gson().fromJson(langs, new TypeToken<Map<String, StatementLanguageStatus>>(){}.getType());

        availableLanguages.put(languageCode, StatementLanguageStatus.DISABLED);

        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(problemJid), new Gson().toJson(availableLanguages));
    }

    @Override
    public void makeDefaultLanguage(String problemJid, String languageCode) {
        fileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(problemJid), languageCode);
    }

    @Override
    public String getDefaultLanguage(String problemJid) {
        return fileSystemProvider.readFromFile(getStatementDefaultLanguageFilePath(problemJid));
    }

    @Override
    public String getStatement(String problemJid, String languageCode) {
        return fileSystemProvider.readFromFile(getStatementFilePath(problemJid, languageCode));
    }

    @Override
    public void updateStatement(long problemId, String languageCode, String statement) {
        ProblemModel problemModel = problemDao.findById(problemId);
        fileSystemProvider.writeToFile(getStatementFilePath(problemModel.jid, languageCode), statement);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFile(long id, File mediaFile, String filename) {
        ProblemModel problemModel = problemDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(problemModel.jid);
        fileSystemProvider.uploadFile(mediaDirPath, mediaFile, filename);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFileZipped(long id, File mediaFileZipped) {
        ProblemModel problemModel = problemDao.findById(id);
        List<String> mediaDirPath = getStatementMediaDirPath(problemModel.jid);
        fileSystemProvider.uploadZippedFiles(mediaDirPath, mediaFileZipped);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public List<FileInfo> getStatementMediaFiles(String problemJid) {
        List<String> mediaDirPath = getStatementMediaDirPath(problemJid);
        return fileSystemProvider.listFilesInDirectory(mediaDirPath);
    }

    @Override
    public String getStatementMediaFileURL(String problemJid, String filename) {
        List<String> mediaFilePath = appendPath(getStatementMediaDirPath(problemJid), filename);
        return fileSystemProvider.getURL(mediaFilePath);
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
        List<String> statementsDirPath = getStatementsDirPath(problemJid);
        fileSystemProvider.createDirectory(statementsDirPath);

        List<String> mediaDirPath = getStatementMediaDirPath(problemJid);
        fileSystemProvider.createDirectory(mediaDirPath);

        fileSystemProvider.createFile(getStatementFilePath(problemJid, initialLanguageCode));
        fileSystemProvider.writeToFile(getStatementDefaultLanguageFilePath(problemJid), initialLanguageCode);

        Map<String, StatementLanguageStatus> initialLanguage = ImmutableMap.of(initialLanguageCode, StatementLanguageStatus.ENABLED);
        fileSystemProvider.writeToFile(getStatementAvailableLanguagesFilePath(problemJid), new Gson().toJson(initialLanguage));
    }

    private ArrayList<String> getRootDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    private ArrayList<String> getStatementsDirPath(String problemJid) {
        return appendPath(getRootDirPath(problemJid), "statements");
    }

    private ArrayList<String> getStatementFilePath(String problemJid, String languageCode) {
        return appendPath(getStatementsDirPath(problemJid), languageCode + ".html");
    }

    private ArrayList<String> getStatementDefaultLanguageFilePath(String problemJid) {
        return appendPath(getStatementsDirPath(problemJid), "defaultLanguage.txt");
    }

    private ArrayList<String> getStatementAvailableLanguagesFilePath(String problemJid) {
        return appendPath(getStatementsDirPath(problemJid), "availableLanguages.txt");
    }

    private ArrayList<String> getStatementMediaDirPath(String problemJid) {
        return appendPath(getStatementsDirPath(problemJid), "media");
    }

    private ArrayList<String> appendPath(ArrayList<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
