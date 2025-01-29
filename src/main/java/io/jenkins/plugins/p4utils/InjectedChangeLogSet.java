package io.jenkins.plugins.p4utils;

import hudson.model.Run;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;

import java.util.Collection;
import java.util.Iterator;

public class InjectedChangeLogSet extends ChangeLogSet<InjectedChangeLogEntry> {
    private final Collection<InjectedChangeLogEntry> entries;

    protected InjectedChangeLogSet(Run build, RepositoryBrowser browser, Collection<InjectedChangeLogEntry> entries) {
        super(build, browser);
        this.entries = entries;
    }

    @Override
    public boolean isEmptySet() {
        return entries.isEmpty();
    }

    @Override
    public Iterator<InjectedChangeLogEntry> iterator() {
        return entries.iterator();
    }

    public Collection<InjectedChangeLogEntry> getEntries() {
        return entries;
    }

}
