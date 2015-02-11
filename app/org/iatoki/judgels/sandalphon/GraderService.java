package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;

public interface GraderService {

    boolean existsByJid(String graderJid);

    Grader findGraderById(long graderId);

    Grader findGraderByJid(String graderJid);

    Page<Grader> pageGraders(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    void createGrader(String name);

    void updateGrader(long graderId, String name);

    boolean verifyGrader(String graderJid, String clientSecret);
}
