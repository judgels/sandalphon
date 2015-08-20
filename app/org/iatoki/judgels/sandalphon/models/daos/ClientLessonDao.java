package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.Dao;
import org.iatoki.judgels.sandalphon.models.entities.ClientLessonModel;

import java.util.List;

public interface ClientLessonDao extends Dao<Long, ClientLessonModel> {

    boolean existsByClientJidAndLessonJid(String clientJid, String lessonJid);

    ClientLessonModel findByClientJidAndLessonJid(String clientJid, String lessonJid);

    List<ClientLessonModel> getByLessonJid(String lessonJid);
}
