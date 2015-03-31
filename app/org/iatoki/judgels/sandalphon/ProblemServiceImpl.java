package org.iatoki.judgels.sandalphon;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JidService;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.commons.Problem;
import org.iatoki.judgels.sandalphon.commons.ProblemType;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.List;

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao problemDao;

    public ProblemServiceImpl(ProblemDao problemDao) {
        this.problemDao = problemDao;
    }

    @Override
    public Problem createProblem(String name, String childJid) {
        ProblemModel problemModel = new ProblemModel();
        problemModel.name = name;

        problemDao.persist(problemModel, childJid, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());

        initStatement(problemModel.jid);
        initMedia(problemModel.jid);

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
    public void updateProblem(long problemId, String name) {
        ProblemModel problemModel = problemDao.findById(problemId);
        problemModel.name = name;

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
    public void updateStatement(long problemId, String statement) {
        ProblemModel problemModel = problemDao.findById(problemId);
        File statementDir = SandalphonProperties.getInstance().getProblemStatementDir(problemModel.jid);

        try {
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"), statement);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public String getStatement(String problemJid) {
        File statementDir = SandalphonProperties.getInstance().getProblemStatementDir(problemJid);
        String statement;
        try {
            statement = FileUtils.readFileToString(new File(statementDir, "statement.html"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return statement;
    }

    @Override
    public void uploadStatementMediaFile(long id, File mediaFile, String filename) {
        ProblemModel problemModel = problemDao.findById(id);
        File problemMediaDir = SandalphonProperties.getInstance().getProblemMediaDir(problemModel.jid);
        try {
            FileUtils.copyFile(mediaFile, new File(problemMediaDir, filename));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void uploadStatementMediaFileZipped(long id, File mediaFileZipped) {
        ProblemModel problemModel = problemDao.findById(id);
        File problemMediaDir = SandalphonProperties.getInstance().getProblemMediaDir(problemModel.jid);

        SandalphonUtils.uploadZippedFiles(problemMediaDir, mediaFileZipped);

        problemDao.edit(problemModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public List<File> getStatementMediaFiles(String problemJid) {
        File mediaDir = SandalphonProperties.getInstance().getProblemMediaDir(problemJid);

        return SandalphonUtils.getSortedFilesInDir(mediaDir);
    }

    @Override
    public File getStatementMediaFile(String problemJid, String filename) {
        File mediaDir = SandalphonProperties.getInstance().getProblemMediaDir(problemJid);

        return new File(mediaDir, filename);
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
        return new Problem(problemModel.id, problemModel.jid, problemModel.name, problemModel.userCreate, new Date(problemModel.timeUpdate), getProblemType(problemModel));
    }

    private void initStatement(String problemJid) {
        File statementDir = SandalphonProperties.getInstance().getProblemStatementDir(problemJid);

        try {
            FileUtils.forceMkdir(statementDir);
            FileUtils.writeStringToFile(new File(statementDir, "statement.html"),

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
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void initMedia(String problemJid) {
        File mediaDir = SandalphonProperties.getInstance().getProblemMediaDir(problemJid);

        try {
            FileUtils.forceMkdir(mediaDir);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
