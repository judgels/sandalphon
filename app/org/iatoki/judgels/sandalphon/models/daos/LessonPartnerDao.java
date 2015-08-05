package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.Dao;
import org.iatoki.judgels.sandalphon.models.entities.LessonPartnerModel;

import java.util.List;

public interface LessonPartnerDao extends Dao<Long, LessonPartnerModel> {

    boolean existsByLessonJidAndPartnerJid(String lessonJid, String partnerJid);

    LessonPartnerModel findByLessonJidAndPartnerJid(String lessonJid, String partnerJid);

    List<String> findLessonJidsByPartnerJid(String partnerJid);
}
