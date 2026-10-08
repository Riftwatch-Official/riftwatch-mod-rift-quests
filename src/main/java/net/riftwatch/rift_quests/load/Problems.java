package net.riftwatch.rift_quests.load;

import java.util.ArrayList;
import java.util.List;

public final class Problems {
    private final List<Problem> list = new ArrayList<>();
    private int errors;

    public void error(String file, String field, String message) {
        list.add(new Problem(Problem.Severity.ERROR, file, field, message));
        errors++;
    }

    public void warning(String file, String field, String message) {
        list.add(new Problem(Problem.Severity.WARNING, file, field, message));
    }

    public void addAll(List<Problem> problems) {
        for (Problem problem : problems) {
            list.add(problem);
            if (problem.severity() == Problem.Severity.ERROR) {
                errors++;
            }
        }
    }

    public int errorCount() {
        return errors;
    }

    public List<Problem> list() {
        return List.copyOf(list);
    }
}
