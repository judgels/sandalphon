package org.iatoki.judgels.sandalphon.forms;

import play.data.validation.Constraints;

public final class ItemStatementConfForm {

    @Constraints.Required
    public String meta;

    @Constraints.Required
    public String statement;
}
