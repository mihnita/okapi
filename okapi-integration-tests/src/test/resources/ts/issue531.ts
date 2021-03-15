<?xml version="1.0" encoding="utf-8"?>
<!DOCTYPE TS>
<TS version="2.0" language="de">
<context>
    <name>LRelease</name>
    <message numerus="yes">
        <source>Dropped %n message(s) which had no ID.</source>
        <translation>
            <numerusform>Es wurde ein Eintrag ohne Bezeichner gelöscht.</numerusform>
            <numerusform>Es wurde %n Einträge ohne Bezeichner gelöscht.</numerusform>
        </translation>
    </message>
    <message numerus="yes">
        <source>Excess context/disambiguation dropped from %n message(s).</source>
        <translation>
            <numerusform>Es wurde überflüssiger Kontext beziehungsweise überflüssige Infomation zur Unterscheidung bei einem Eintrag entfernt.</numerusform>
            <numerusform>Es wurde überflüssiger Kontext beziehungsweise überflüssige Infomation zur Unterscheidung bei %n Einträgen entfernt.</numerusform>
        </translation>
    </message>
    <message numerus="yes">
        <source>    Generated %n translation(s) (%1 finished and %2 unfinished)</source>
        <translation>
            <numerusform>    Eine Übersetzung wurde erzeugt (%1 abgeschlossen und %2 nicht abgeschlossen)</numerusform>
            <numerusform>    %n Übersetzungen wurden erzeugt (%1 abgeschlossen und %2 nicht abgeschlossen)</numerusform>
        </translation>
    </message>
    <message numerus="yes">
        <source>    Ignored %n untranslated source text(s)</source>
        <translation>
            <numerusform>    Ein nicht übersetzter Text wurde ignoriert</numerusform>
            <numerusform>    %n nicht übersetzte Texte wurden ignoriert</numerusform>
        </translation>
    </message>
    <message numerus="yes">
        <source>    Generated %n translation(s) (%1 finished and %2 unfinished)
</source>
        <translation type="obsolete">
            <numerusform>    Eine Übersetzung wurde erzeugt (%1 abgeschlossen und %2 nicht abgeschlossen)
</numerusform>
            <numerusform>   %n Übersetzungen wurden erzeugt (%1 abgeschlossene und %2 nicht abgeschlossene)
</numerusform>
        </translation>
    </message>
    <message numerus="yes">
        <source>    Ignored %n untranslated source text(s)
</source>
        <translation type="obsolete">
            <numerusform>    Ein nicht übersetzter Text wurde ignoriert
</numerusform>
            <numerusform>    %n nicht übersetzte Texte wurden ignoriert
</numerusform>
        </translation>
    </message>
    <message>
        <source>Usage:
    lrelease [options] project-file
    lrelease [options] ts-files [-qm qm-file]

lrelease is part of Qt&apos;s Linguist tool chain. It can be used as a
stand-alone tool to convert XML-based translations files in the TS
format into the &apos;compiled&apos; QM format used by QTranslator objects.

Options:
    -help  Display this information and exit
    -idbased
           Use IDs instead of source strings for message keying
    -compress
           Compress the QM files
    -nounfinished
           Do not include unfinished translations
    -removeidentical
           If the translated text is the same as
           the source text, do not include the message
    -markuntranslated &lt;prefix&gt;
           If a message has no real translation, use the source text
           prefixed with the given string instead
    -silent
           Do not explain what is being done
    -version
           Display the version of lrelease and exit
</source>
        <translation type="unfinished"></translation>
    </message>
    <message>
        <source>lrelease error: %1</source>
        <translation>Fehler in lrelease: %1</translation>
    </message>
    <message>
        <source>Updating &apos;%1&apos;...
</source>
        <translation>Bringe &apos;%1&apos; auf aktuellen Stand...
</translation>
    </message>
    <message>
        <source>Removing translations equal to source text in &apos;%1&apos;...
</source>
        <translation>Entferne Übersetzungen, die dem unübersetzten Text entsprechen, in &apos;%1&apos;...
</translation>
    </message>
    <message>
        <source>lrelease error: cannot create &apos;%1&apos;: %2
</source>
        <translation>Fehler in lrelease: &apos;%1&apos; kann nicht erzeugt werden: %2
</translation>
    </message>
    <message>
        <source>lrelease error: cannot save &apos;%1&apos;: %2</source>
        <translation>Fehler in lrelease: &apos;%1&apos; kann nicht gespeichert werden: %2
</translation>
    </message>
    <message>
        <source>lrelease version %1
</source>
        <translation>lrelease Version %1
</translation>
    </message>
    <message>
        <source>lrelease error: cannot read project file &apos;%1&apos;.
</source>
        <translation>Fehler in lrelease: Die Projektdatei &apos;%1&apos; kann nicht gelesen werden.
</translation>
    </message>
    <message>
        <source>lrelease error: cannot process project file &apos;%1&apos;.
</source>
        <translation>Fehler in lrelease: Die Projektdatei &apos;%1&apos; kann verarbeitet werden.
</translation>
    </message>
    <message>
        <source>lrelease warning: Met no &apos;TRANSLATIONS&apos; entry in project file &apos;%1&apos;
</source>
        <translation>Warnung in lrelease : Die Projektdatei &apos;%1&apos; enthält keinen &apos;TRANSLATIONS&apos;-Eintrag
</translation>
    </message>
</context>
</TS>