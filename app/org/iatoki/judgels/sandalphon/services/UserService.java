package org.iatoki.judgels.sandalphon.services;

import org.iatoki.judgels.api.jophiel.JophielUser;
import org.iatoki.judgels.play.Page;
import org.iatoki.judgels.jophiel.services.BaseUserService;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.UserNotFoundException;

import java.util.List;

public interface UserService extends BaseUserService {

    User findUserById(long userId) throws UserNotFoundException;

    User findUserByJid(String userJid);

    void createUser(String userJid, List<String> roles, String createUserJid, String createUserIpAddress);

    void updateUser(String userJid, List<String> roles, String updateUserJid, String updateUserIpAddress);

    void deleteUser(String userJid);

    Page<User> getPageOfUsers(long pageIndex, long pageSize, String orderBy, String orderDir, String filterString);

    void upsertUserFromJophielUser(JophielUser jophielUser, String upsertUserJid, String upsertUserIpAddress);

    void upsertUserFromJophielUser(JophielUser jophielUser, List<String> roles, String upsertUserJid, String upsertUserIpAddress);
}
