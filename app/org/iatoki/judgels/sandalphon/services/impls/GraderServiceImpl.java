package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.iatoki.judgels.commons.IdentityUtils;
import org.iatoki.judgels.commons.JudgelsUtils;
import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.sandalphon.models.daos.programming.GraderDao;
import org.iatoki.judgels.sandalphon.models.entities.programming.GraderModel;
import org.iatoki.judgels.sandalphon.programming.Grader;
import org.iatoki.judgels.sandalphon.programming.GraderNotFoundException;
import org.iatoki.judgels.sandalphon.services.GraderService;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.List;

@Singleton
@Named("graderService")
public final class GraderServiceImpl implements GraderService {

    private final GraderDao graderDao;

    @Inject
    public GraderServiceImpl(GraderDao graderDao) {
        this.graderDao = graderDao;
    }

    @Override
    public boolean existsByJid(String graderJid) {
        return graderDao.existsByJid(graderJid);
    }

    @Override
    public Grader findGraderById(long graderId) throws GraderNotFoundException {
        GraderModel graderModel = graderDao.findById(graderId);
        if (graderModel != null) {
            return createGraderFromModel(graderModel);
        } else {
            throw new GraderNotFoundException("Grader not found.");
        }
    }

    @Override
    public Grader findGraderByJid(String graderJid) {
        GraderModel graderModel = graderDao.findByJid(graderJid);
        return createGraderFromModel(graderModel);
    }

    @Override
    public void createGrader(String name) {
        GraderModel graderModel = new GraderModel();
        graderModel.name = name;
        graderModel.secret = JudgelsUtils.generateNewSecret();

        graderDao.persist(graderModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public void updateGrader(long clientId, String name) {
        GraderModel clientModel = graderDao.findById(clientId);
        clientModel.name = name;

        graderDao.edit(clientModel, IdentityUtils.getUserJid(), IdentityUtils.getIpAddress());
    }

    @Override
    public Page<Grader> pageGraders(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) {
        long totalPages = graderDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of());
        List<GraderModel> graderModels = graderDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

        List<Grader> graders = Lists.transform(graderModels, m -> createGraderFromModel(m));

        return new Page<>(graders, totalPages, pageIndex, pageSize);
    }

    @Override
    public boolean verifyGrader(String graderJid, String graderSecret) {
        GraderModel graderModel = graderDao.findByJid(graderJid);

        return graderModel != null && graderModel.secret.equals(graderSecret);
    }

    private Grader createGraderFromModel(GraderModel graderModel) {
        return new Grader(graderModel.id, graderModel.jid, graderModel.name, graderModel.secret);
    }
}
