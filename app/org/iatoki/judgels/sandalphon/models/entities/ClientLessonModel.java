package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.play.models.entities.AbstractModel;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_client_lesson")
public final class ClientLessonModel extends AbstractModel {

    @Id
    @GeneratedValue
    public long id;

    public String clientJid;

    public String lessonJid;

    public String secret;
}
