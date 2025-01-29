package io.jenkins.plugins.p4utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import hudson.model.Run;
import hudson.scm.ChangeLogParser;
import hudson.scm.ChangeLogSet;
import hudson.scm.RepositoryBrowser;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class InjectedChangeLogParser extends ChangeLogParser {

    @Override
    public ChangeLogSet<? extends ChangeLogSet.Entry> parse(Run build, RepositoryBrowser<?> browser, File changelogFile) throws IOException, SAXException {
        List<InjectedChangeLogEntry> entries = new ArrayList<>();


        ObjectMapper objectMapper = new ObjectMapper();
        var changes = objectMapper.readValue(changelogFile,new TypeReference<List<InjectedChange>>() {});


        for (var c : changes) {
            entries.add(new InjectedChangeLogEntry(c.getAuthor(), c.getDescription(), String.valueOf(c.getId()), c.getTimestamp(), c.getFiles()));
        }

        ChangeLogSet<InjectedChangeLogEntry> list = new InjectedChangeLogSet(build, browser, entries);
        return list;
    }
}
