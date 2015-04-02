package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
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

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao problemDao;
    private final FileSystemProvider fileSystemProvider;

    public ProblemServiceImpl(ProblemDao problemDao, FileSystemProvider fileSystemProvider) {
        this.problemDao = problemDao;
        this.fileSystemProvider = fileSystemProvider;
    }

    @Override
    public Problem createProblem(ProblemType type, String name, String additionalNote) {
        ProblemModel problemModel = new ProblemModel();
        problemModel.name = name;
        problemModel.additionalNote = additionalNote;

        problemDao.persist(problemModel, type.ordinal(), IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatements(problemModel.jid);

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
    public String getStatement(String problemJid) {
        List<String> statementFilePath = appendPath(getStatementsDirPath(problemJid), "statement.html");
        return fileSystemProvider.readFromFile(statementFilePath);
    }

    @Override
    public void updateStatement(long problemId, String statement) {
        ProblemModel problemModel = problemDao.findById(problemId);
        List<String> statementFilePath = appendPath(getStatementsDirPath(problemModel.jid), "statement.html");
        fileSystemProvider.writeToFile(statementFilePath, statement);

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

    private void initStatements(String problemJid) {
        List<String> statementsDirPath = getStatementsDirPath(problemJid);
        fileSystemProvider.createDirectory(statementsDirPath);

        List<String> mediaDirPath = getStatementMediaDirPath(problemJid);
        fileSystemProvider.createDirectory(mediaDirPath);

        ArrayList<String> statementFilePath = getStatementsDirPath(problemJid);
        statementFilePath.add("statement.html");
        fileSystemProvider.writeToFile(statementFilePath,
                "<h3>Deskripsi</h3>\n" +
                        "\n" +
                        "<p>Blabla.</p>\n" +
                        "\n" +
                        "<h3>Format Masukan</h3>\n" +
                        "\n" +
                        "<p>Blabla.</p>\n" +
                        "\n" +
                        "<h3>Format Keluaran</h3>\n" +
                        "\n" +
                        "<p>Blabla.</p>\n" +
                        "\n" +
                        "<h3>Contoh Masukan</h3>\n" +
                        "\n" +
                        "<pre>\n" +
                        "Blabla.</pre>\n" +
                        "\n" +
                        "<h3>Contoh Keluaran</h3>\n" +
                        "\n" +
                        "<pre>\n" +
                        "Blabla.</pre>\n" +
                        "\n" +
                        "<h3>Batasan/Subsoal</h3>\n" +
                        "\n" +
                        "<ul>\n" +
                        "\t<li>Blabla</li>\n" +
                        "</ul>\n"
        );
    }

    private ArrayList<String> getRootDirPath(String problemJid) {
        return Lists.newArrayList(SandalphonProperties.getInstance().getBaseProblemsDirKey(), problemJid);
    }

    private ArrayList<String> getStatementsDirPath(String problemJid) {
        return appendPath(getRootDirPath(problemJid), "statements");
    }

    private ArrayList<String> getStatementMediaDirPath(String problemJid) {
        return appendPath(getStatementsDirPath(problemJid), "media");
    }

    private ArrayList<String> appendPath(ArrayList<String> parentPath, String child) {
        parentPath.add(child);
        return parentPath;
    }
}
