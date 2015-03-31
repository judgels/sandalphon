package org.iatoki.judgels.sandalphon.commons.programming;

import org.iatoki.judgels.sandalphon.commons.Problem;
import org.iatoki.judgels.sandalphon.commons.ProblemType;

import java.util.Date;

public final class ProgrammingProblem extends Problem {

    private final String gradingEngine;
    private final String additionalNote;
    private final LanguageRestriction languageRestriction;

    public ProgrammingProblem(String jid, String gradingEngine, String additionalNote, LanguageRestriction languageRestriction) {
        super(jid, ProblemType.PROGRAMMING);
        this.gradingEngine = gradingEngine;
        this.additionalNote = additionalNote;
        this.languageRestriction = languageRestriction;
    }

    public ProgrammingProblem(long id, String jid, String name, String authorJid, Date lastUpdate, String gradingEngine, String additionalNote, LanguageRestriction languageRestriction) {
        super(id, jid, name, authorJid, lastUpdate, ProblemType.PROGRAMMING);
        this.gradingEngine = gradingEngine;
        this.additionalNote = additionalNote;
        this.languageRestriction = languageRestriction;
    }

    public String getGradingEngine() {
        return gradingEngine;
    }

    public String getAdditionalNote() {
        return additionalNote;
    }

    public LanguageRestriction getLanguageRestriction() {
        return languageRestriction;
    }
}
