package org.iatoki.judgels.sandalphon.services.impls;

import org.iatoki.judgels.jophiel.UserTokens;
import org.iatoki.judgels.sandalphon.User;
import org.iatoki.judgels.sandalphon.models.entities.UserModel;

import java.util.Arrays;

final class UserServiceUtils {

    private UserServiceUtils() {
        // prevent instantiation
    }

    static UserTokens createUserTokensFromUserModel(UserModel userModel) {
        return new UserTokens(userModel.userJid, userModel.accessToken, userModel.refreshToken, userModel.idToken, userModel.expirationTime);
    }

    static User createUserFromUserModel(UserModel userModel) {
        return new User(userModel.id, userModel.userJid, Arrays.asList(userModel.roles.split(",")));
    }
}
