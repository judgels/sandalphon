package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.JudgelsDao;
import org.iatoki.judgels.sandalphon.models.domains.LessonModel;

import java.util.List;

public interface LessonDao extends JudgelsDao<LessonModel> {
    List<String> findLessonJidsByAuthorJid(String authorJid);
}
