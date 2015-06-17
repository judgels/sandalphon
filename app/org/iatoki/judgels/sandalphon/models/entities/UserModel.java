package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.jophiel.models.entities.AbstractUserModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_user")
public final class UserModel extends AbstractUserModel {

    public String roles;
}
