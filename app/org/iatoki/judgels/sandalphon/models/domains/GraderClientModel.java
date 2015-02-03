package org.iatoki.judgels.sandalphon.models.domains;

import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_client_grader")
public final class GraderClientModel extends AbstractJudgelsModel {
    public String name;
    public String secret;

    public GraderClientModel() {

    }

    public GraderClientModel(long id, String jid, String name) {
        this.id = id;
        this.jid = jid;
        this.name = name;
    }
}
