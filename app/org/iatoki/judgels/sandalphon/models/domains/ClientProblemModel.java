package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractModel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_client_problem")
public final class ClientProblemModel extends AbstractModel {

    @Id
    @GeneratedValue
    public long id;

    public String clientJid;

    public String problemJid;

    public String secret;
}
