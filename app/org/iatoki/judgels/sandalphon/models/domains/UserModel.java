package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.jophiel.commons.models.domains.AbstractUserModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_user")
public final class UserModel extends AbstractUserModel {

    public String roles;
}
