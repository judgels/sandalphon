package org.iatoki.judgels.sandalphon.models.schema;

import org.hibernate.annotations.GenericGenerator;
import org.iatoki.judgels.commons.models.schema.Model;
import org.iatoki.judgels.commons.models.ModelField;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "m_problem")
public class Problem extends Model {

    @Id
    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    @Column(name = "problem_id")
    private String problemId;

    @ModelField(formName = "Judul Soal")
    @Column(name = "title")
    private String title;

    public String getProblemId() {
        return problemId;
    }

    public void setProblemId(String problemId) {
        this.problemId = problemId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }
}
