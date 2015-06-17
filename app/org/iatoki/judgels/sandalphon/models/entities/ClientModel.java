package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.commons.models.JidPrefix;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_client")
@JidPrefix("SACL")
public final class ClientModel extends AbstractJudgelsModel {

    public String name;

    public String secret;

    public ClientModel() {

    }

    public ClientModel(long id, String jid, String name) {
        this.id = id;
        this.jid = jid;
        this.name = name;
    }
}
