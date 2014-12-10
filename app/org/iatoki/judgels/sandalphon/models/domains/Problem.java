package org.iatoki.judgels.sandalphon.models.domains;

import org.hibernate.annotations.GenericGenerator;
import org.iatoki.judgels.commons.helpers.crud.CrudAction;
import org.iatoki.judgels.commons.helpers.crud.CrudVisible;
import org.iatoki.judgels.commons.helpers.crud.FormFieldCustomType;
import org.iatoki.judgels.commons.helpers.crud.FormFieldType;
import org.iatoki.judgels.commons.models.domains.Model;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "m_problem")
public class Problem extends Model {

    @Id
    @GeneratedValue
    @CrudVisible(CrudAction.LIST)
    public long id;

    @GeneratedValue(generator = "system-uuid")
    @GenericGenerator(name = "system-uuid", strategy = "uuid")
    public String problemId;

    @CrudVisible
    public String title;

    @CrudVisible
    public boolean test;

    @CrudVisible
    @FormFieldCustomType(FormFieldType.PASSWORD)
    public String password;

    public Problem() {
        // nothing
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
