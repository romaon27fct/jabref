package org.jabref.gui.exporter;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.jabref.gui.ClipBoardManager;
import org.jabref.gui.DialogService;
import org.jabref.gui.Globals;
import org.jabref.gui.JabRefFrame;
import org.jabref.gui.LibraryTab;
import org.jabref.gui.util.CurrentThreadTaskExecutor;
import org.jabref.gui.util.TaskExecutor;
import org.jabref.logic.exporter.Exporter;
import org.jabref.logic.exporter.ExporterFactory;
import org.jabref.logic.exporter.SavePreferences;
import org.jabref.logic.exporter.TemplateExporter;
import org.jabref.logic.l10n.Localization;
import org.jabref.logic.layout.LayoutFormatterPreferences;
import org.jabref.logic.util.StandardFileType;
import org.jabref.logic.xmp.XmpPreferences;
import org.jabref.model.database.BibDatabaseContext;
import org.jabref.model.entry.BibEntry;
import org.jabref.model.entry.field.StandardField;
import org.jabref.model.entry.types.StandardEntryType;
import org.jabref.model.metadata.MetaData;
import org.jabref.preferences.JabRefPreferences;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyCollection;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ExportToClipboardActionTest {

    private ExportToClipboardAction exportToClipboardAction;
    private LibraryTab libraryTab = mock(LibraryTab.class);
    private JabRefFrame jabRefFrame = mock(JabRefFrame.class);
    private DialogService dialogService = spy(DialogService.class);
    private ExporterFactory exporterFactory;
    private ClipBoardManager clipBoardManager = mock(ClipBoardManager.class);
    private TaskExecutor taskExecutor;
    private List<BibEntry> selectedEntries;
    private BibDatabaseContext databaseContext = mock(BibDatabaseContext.class);

    @BeforeEach
    public void setUp() {
        if(Globals.prefs == null){
            Globals.prefs = JabRefPreferences.getInstance();
        }
        taskExecutor = new CurrentThreadTaskExecutor();

        List<TemplateExporter> customFormats = new ArrayList<>();
        LayoutFormatterPreferences layoutPreferences = mock(LayoutFormatterPreferences.class, Answers.RETURNS_DEEP_STUBS);
        SavePreferences savePreferences = mock(SavePreferences.class);
        XmpPreferences xmpPreferences = mock(XmpPreferences.class);
        exporterFactory = ExporterFactory.create(customFormats, layoutPreferences, savePreferences, xmpPreferences);
        exportToClipboardAction = new ExportToClipboardAction(libraryTab, dialogService, exporterFactory, clipBoardManager, taskExecutor);

        BibEntry entry = new BibEntry(StandardEntryType.Misc)
                .withField(StandardField.AUTHOR, "Souti Chattopadhyay and Nicholas Nelson and Audrey Au and Natalia Morales and Christopher Sanchez and Rahul Pandita and Anita Sarma")
                .withField(StandardField.TITLE, "A tale from the trenches")
                .withField(StandardField.YEAR, "2020")
                .withField(StandardField.DOI, "10.1145/3377811.3380330")
                .withField(StandardField.SUBTITLE, "cognitive biases and software development");

        selectedEntries = new ArrayList<>();
        selectedEntries.add(entry);
    }

    @Test
    public void testExecuteIfNoSelectedEntries() {
        when(libraryTab.getSelectedEntries()).thenReturn(Collections.EMPTY_LIST);

        exportToClipboardAction.execute();
        verify(dialogService, times(1)).notify(Localization.lang("This operation requires one or more entries to be selected."));
    }

    @Test
    public void testExecuteOnSuccess() {
        Exporter selectedExporter = new Exporter("html", "HTML", StandardFileType.HTML) {
            @Override
            public void export(BibDatabaseContext databaseContext, Path file, Charset encoding, List<BibEntry> entries) throws Exception {
            }
        };

        when(libraryTab.getSelectedEntries()).thenReturn(selectedEntries);
        when(libraryTab.getBibDatabaseContext()).thenReturn(databaseContext);
        when(databaseContext.getFileDirectories(Globals.prefs.getFilePreferences())).thenReturn(new ArrayList<Path>(Arrays.asList(Path.of("path"))));
        when(databaseContext.getMetaData()).thenReturn(new MetaData());
        when(dialogService.showChoiceDialogAndWait(
                eq(Localization.lang("Export")), eq(Localization.lang("Select export format")),
                eq(Localization.lang("Export")), any(Exporter.class), anyCollection())).thenReturn(Optional.of(selectedExporter));

        exportToClipboardAction.execute();
        verify(dialogService, times(1)).showChoiceDialogAndWait(
               eq(Localization.lang("Export")), eq(Localization.lang("Select export format")),
                eq(Localization.lang("Export")), any(Exporter.class), anyCollection());
        verify(dialogService, times(1)).notify(Localization.lang("Entries exported to clipboard") + ": " + selectedEntries.size());
    }
}
