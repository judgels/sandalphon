package org.iatoki.judgels.sandalphon.commons;

import java.util.Date;

public final class Problem extends AbstractProblem {
    public Problem(long id, String jid, String name, String authorJid, Date lastUpdate, ProblemType type) {
        super(id, jid, name, authorJid, lastUpdate, type);
    }
}
