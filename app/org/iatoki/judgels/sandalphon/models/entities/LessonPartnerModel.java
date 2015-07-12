package org.iatoki.judgels.sandalphon.models.entities;

import org.iatoki.judgels.play.models.domains.AbstractModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "sandalphon_lesson_partner")
public final class LessonPartnerModel extends AbstractModel {

    @Id
    @GeneratedValue
    public long id;

    public String lessonJid;

    public String userJid;

    @Column(columnDefinition = "TEXT")
    public String config;
}
