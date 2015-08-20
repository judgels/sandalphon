package org.iatoki.judgels.sandalphon.services;

import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.UserNotFoundException;

import java.util.List;

public interface UserService extends BaseUserService {

    User findUserById(long userId) throws UserNotFoundException;

    User findUserByJid(String userJid);

    void createUser(String userJid, List<String> roles);

    void updateUser(long userId, List<String> roles);

    void deleteUser(long userId);

    Page<User> getPageOfUsers(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    void upsertUserFromJophielUserJid(String userJid);

    void upsertUserFromJophielUserJid(String userJid, List<String> roles);
}
