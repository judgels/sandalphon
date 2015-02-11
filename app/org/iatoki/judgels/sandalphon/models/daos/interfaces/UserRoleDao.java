package org.iatoki.judgels.sandalphon.models.daos.interfaces;

import org.iatoki.judgels.commons.models.daos.interfaces.Dao;
import org.iatoki.judgels.sandalphon.models.domains.UserRoleModel;

import java.util.List;

public interface UserRoleDao extends Dao<Long, UserRoleModel> {

    boolean existsByUserJid(String userJid);

    UserRoleModel findByUserJid(String userJid);

    List<String> findUserJidByFilter(String filterString);
}
