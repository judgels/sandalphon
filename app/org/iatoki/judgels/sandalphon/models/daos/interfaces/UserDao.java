package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.Dao;
import org.iatoki.judgels.sandalphon.models.domains.UserModel;

import java.util.List;

public interface UserDao extends Dao<Long, UserModel> {

    boolean existsByUserJid(String userJid);

    UserModel findByUserJid(String userJid);

    List<String> findUserJidByFilter(String filterString);
}
