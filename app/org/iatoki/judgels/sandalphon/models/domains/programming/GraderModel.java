package org.iatoki.judgels.sandalphon.models.domains.programming;

import org.iatoki.judgels.commons.models.JidPrefix;
import org.iatoki.judgels.commons.models.domains.AbstractJudgelsModel;

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_grader")
@JidPrefix("SAGR")
public final class GraderModel extends AbstractJudgelsModel {
    public String name;
    public String secret;

    public GraderModel() {

    }

    public GraderModel(long id, String jid, String name) {
        this.id = id;
        this.jid = jid;
        this.name = name;
    }
}
