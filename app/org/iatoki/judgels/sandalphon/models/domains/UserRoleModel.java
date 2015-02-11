package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractModel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_user_role")
public final class UserRoleModel extends AbstractModel {

    @Id
    @GeneratedValue
    public long id;

    public String userJid;

    public String username;

    public String roles;
}
