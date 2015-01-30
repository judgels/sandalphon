package org.iatoki.judgels.sandalphon;

import org.apache.commons.lang3.StringUtils;

public final class UserRoleUpdateForm {

    public UserRoleUpdateForm(UserRole userRole) {
        this.alias = userRole.getAlias();
        this.roles = StringUtils.join(userRole.getRoles(), ",");
    }

    public String alias;

    public String roles;

}
