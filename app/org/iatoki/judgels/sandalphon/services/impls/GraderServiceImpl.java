package org.iatoki.judgels.sandalphon.services.impls;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.iatoki.judgels.play.JudgelsAppClient;
import org.iatoki.judgels.play.JudgelsPlayUtils;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.sandalphon.Grader;
import org.iatoki.judgels.sandalphon.GraderNotFoundException;
import org.iatoki.judgels.sandalphon.models.daos.GraderDao;
import org.iatoki.judgels.sandalphon.models.entities.GraderModel;
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
    public boolean graderExistsByJid(String graderJid) {
        return graderDao.existsByJid(graderJid);
    }

    @Override
    public Grader findGraderById(long graderId) throws GraderNotFoundException {
        GraderModel graderModel = graderDao.findById(graderId);
        if (graderModel == null) {
            throw new GraderNotFoundException("Grader not found.");
        }

        return GraderServiceUtils.createGraderFromModel(graderModel);
    }

    @Override
    public Grader findGraderByJid(String graderJid) {
        GraderModel graderModel = graderDao.findByJid(graderJid);

        return GraderServiceUtils.createGraderFromModel(graderModel);
    }

    @Override
    public void createGrader(String name, String userJid, String userIpAddress) {
        GraderModel graderModel = new GraderModel();
        graderModel.name = name;
        graderModel.secret = JudgelsPlayUtils.generateNewSecret();

        graderDao.persist(graderModel, userJid, userIpAddress);
    }

    @Override
    public void updateGrader(String graderJid, String name, String userJid, String userIpAddress) {
        GraderModel graderModel = graderDao.findByJid(graderJid);
        graderModel.name = name;

        graderDao.edit(graderModel, userJid, userIpAddress);
    }

    @Override
    public Page<Grader> getPageOfGraders(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString) {
        long totalPages = graderDao.countByFilters(filterString, ImmutableMap.of(), ImmutableMap.of());
        List<GraderModel> graderModels = graderDao.findSortedByFilters(orderBy, orderDir, filterString, ImmutableMap.of(), ImmutableMap.of(), pageIndex * pageSize, pageSize);

        List<Grader> graders = Lists.transform(graderModels, m -> GraderServiceUtils.createGraderFromModel(m));

        return new Page<>(graders, totalPages, pageIndex, pageSize);
    }

    @Override
    public boolean clientExistsByJid(String clientJid) {
        return graderExistsByJid(clientJid);
    }

    @Override
    public JudgelsAppClient findClientByJid(String clientJid) {
        return findGraderByJid(clientJid);
    }
}
