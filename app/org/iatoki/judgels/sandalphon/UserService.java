package org.iatoki.judgels.sandalphon;

import org.iatoki.judgels.commons.Page;
import org.iatoki.judgels.jophiel.commons.BaseUserService;

import java.util.List;

public interface UserService extends BaseUserService {

    User findUserById(long userId);

    User findUserByUserJid(String userJid);

    void createUser(String userJid, List<String> roles);

    void updateUser(long userRoleId, List<String> roles);

    void deleteUser(long userRoleId);

    Page<User> pageUsers(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    void upsertUserFromJophielUserJid(String userJid);

}