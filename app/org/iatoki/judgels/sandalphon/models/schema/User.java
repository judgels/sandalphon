package org.iatoki.judgels.sandalphon.models.schema;

import org.iatoki.judgels.commons.models.schema.AbstractUser;

import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class User extends AbstractUser {
    private String username;
}
