package org.iatoki.judgels.sandalphon.services;

import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.play.services.JudgelsAppClientService;
import org.iatoki.judgels.sandalphon.Grader;
import org.iatoki.judgels.sandalphon.GraderNotFoundException;

public interface GraderService extends JudgelsAppClientService {

    boolean graderExistsByJid(String graderJid);

    Grader findGraderById(long graderId) throws GraderNotFoundException;

    Grader findGraderByJid(String graderJid);

    Page<Grader> getPageOfGraders(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    Grader createGrader(String name, String userJid, String userIpAddress);

    void updateGrader(String graderJid, String name, String userJid, String userIpAddress);
}
