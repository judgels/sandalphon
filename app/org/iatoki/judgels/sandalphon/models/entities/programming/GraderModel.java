package org.iatoki.judgels.sandalphon.models.entities.programming;

import org.iatoki.judgels.play.models.JidPrefix;
import org.iatoki.judgels.play.models.entities.AbstractJudgelsModel;

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
