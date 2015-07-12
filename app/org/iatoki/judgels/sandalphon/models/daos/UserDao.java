package org.iatoki.judgels.sandalphon.models.daos;

import org.iatoki.judgels.play.models.daos.Dao;
import org.iatoki.judgels.sandalphon.models.entities.UserModel;

import java.util.List;

public interface UserDao extends Dao<Long, UserModel> {

    boolean existsByUserJid(String userJid);

    UserModel findByUserJid(String userJid);

    List<String> findUserJidByFilter(String filterString);
}
