package org.iatoki.judgels.sandalphon.programming;

import org.iatoki.judgels.sandalphon.Problem;
import org.iatoki.judgels.sandalphon.ProblemType;
import org.iatoki.judgels.sandalphon.commons.programming.LanguageRestriction;

import java.util.Date;

public final class ProgrammingProblem extends Problem {

    private final String gradingEngine;
    private final LanguageRestriction languageRestriction;

    public ProgrammingProblem(String jid, String gradingEngine, LanguageRestriction languageRestriction) {
        super(jid, ProblemType.PROGRAMMING);
        this.gradingEngine = gradingEngine;
        this.languageRestriction = languageRestriction;
    }

    public ProgrammingProblem(long id, String jid, String name, String authorJid, String additionalNote, Date lastUpdate, String gradingEngine, LanguageRestriction languageRestriction) {
        super(id, jid, name, authorJid, additionalNote, lastUpdate, ProblemType.PROGRAMMING);
        this.gradingEngine = gradingEngine;
        this.languageRestriction = languageRestriction;
    }

    public String getGradingEngine() {
        return gradingEngine;
    }

    public LanguageRestriction getLanguageRestriction() {
        return languageRestriction;
    }
}
