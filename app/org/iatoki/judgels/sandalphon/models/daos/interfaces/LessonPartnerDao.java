package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.Dao;
import org.iatoki.judgels.sandalphon.models.domains.LessonPartnerModel;

import java.util.List;

public interface LessonPartnerDao extends Dao<Long, LessonPartnerModel> {

    boolean existsByLessonJidAndPartnerJid(String lessonJid, String partnerJid);

    LessonPartnerModel findByLessonJidAndPartnerJid(String lessonJid, String partnerJid);

    List<String> findLessonJidsByPartnerJid(String partnerJid);
}
