package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.entities.LessonModel;

import java.util.List;

public interface LessonDao extends JudgelsDao<LessonModel> {
    List<String> findLessonJidsByAuthorJid(String authorJid);
}
