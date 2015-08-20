package org.iatoki.judgels.sandalphon.services;

import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.sandalphon.Grader;
import org.iatoki.judgels.sandalphon.GraderNotFoundException;

public interface GraderService {

    boolean graderExistsByJid(String graderJid);

    Grader findGraderById(long graderId) throws GraderNotFoundException;

    Grader findGraderByJid(String graderJid);

    Page<Grader> getPageOfGraders(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    void createGrader(String name);

    void updateGrader(long graderId, String name);

    boolean verifyGrader(String graderJid, String clientSecret);
}
