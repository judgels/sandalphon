package org.iatoki.judgels.sandalphon.models.domains;

import org.hibernate.annotations.GenericGenerator;
import org.iatoki.judgels.commons.models.domains.Model;
import org.iatoki.judgels.commons.views.FormField;
import org.iatoki.judgels.commons.views.FormFieldType;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "m_problem")
public class Problem extends Model {

    @Id
    @GeneratedValue
    public long id;

    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    public String problemId;

    @FormField(formName = "Judul Soal")
    public String title;

    @FormField(formName = "Coba")
    public boolean test;

    @FormField(formName = "Sandi", forceType = FormFieldType.PASSWORD)
    public String password;

    public Problem() {

    }

    public Problem(long id, String title, String password) {
        this.id = id;
        this.title = title;
        this.password = password;
    }

    public Problem(long id, String title, boolean test, String password) {
        this.id = id;
        this.title = title;
        this.test = test;
        this.password = password;
    }

}
