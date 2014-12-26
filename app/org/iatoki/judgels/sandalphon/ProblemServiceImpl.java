package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.interfaces.ProblemDao;
import org.iatoki.judgels.sandalphon.models.domains.ProblemModel;

import java.util.List;
import java.util.stream.Collectors;

public final class ProblemServiceImpl implements ProblemService {

    private final ProblemDao dao;

    public ProblemServiceImpl(ProblemDao dao) {
        this.dao = dao;
    }

    @Override
    public final Problem findProblemById(long id) {
        ProblemModel model = dao.findById(id);
        return new ProblemImpl(model);
    }

    @Override
    public final void updateProblem(long id, String name, String note) {
        ProblemModel model = dao.findById(id);
        model.name = name;
        model.note = note;
        dao.edit(model, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public
    Page<Problem> pageProblem(long page, long pageSize, String sortBy, String order, String filterString) {
        long totalPage = dao.countByFilter(filterString);
        List<ProblemModel> problemRecords = dao.findByFilterAndSort(filterString, sortBy, order, page * pageSize, pageSize);

        List<Problem> problems = problemRecords
                .stream()
                .map(problemRecord -> new ProblemImpl(problemRecord))
                .collect(Collectors.toList());

        return new Page<>(problems, totalPage, page, pageSize);
    }

    private static class ProblemImpl implements Problem {

        private final ProblemModel problemModel;

        ProblemImpl(ProblemModel problemModel) {
            this.problemModel = problemModel;
        }

        @Override
        public long getId() {
            return problemModel.id;
        }

        @Override
        public String getJid() {
            return problemModel.jid;
        }

        @Override
        public String getName() {
            return problemModel.name;
        }

        @Override
        public String getNote() {
            return problemModel.note;
        }

        @Override
        public ProblemType getType() {
            if (problemModel.type.equals("PROGRAMMING")) {
                return ProblemType.PROGRAMMING;
            } else {
                return ProblemType.MULTIPLE_CHOICE;
            }
        }
    }
}