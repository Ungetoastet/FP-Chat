# FP-Chat
Fortgeschrittenes Programmierpraktikum WS23/24: Chatprogramm

2 Idioten vollgepumpt mit Koffein, die in einer Nacht nen Chatroom programmieren.
Einer kann was, die andere malt.

# Features
- Multithreaded server
- JavaScript/HTML Web Client
- Login / Registration for users

# ToDo
## Known issues
- Clients werden nach dem Trennen immer noch angezeigt, sind also noch registriert
- Falls das ServerFrontend geschlossen wird, soll der Server runtergefahren werden

## Nice to have
- Zeitstempel für Nachrichten
- Laden des Chatverlaufs beim Betreten eines Raums

## Meilenstein 1
### Server
- ✅ starten und beenden des ServerSockets
- ✅ Realisierung eines Netzwerk-Listener, der es neuen Clients ermöglicht, sich anzumelden
- ✅ Realisierung eines Nachrichten-Listener, welcher die Voraussetzung für den Nachrichtenaustausch zwischen Clients schafft
- ✅ die Möglichkeit, Namen und dazugehörige Passwörter zu speichern und zu verwalten – solange der Server läuft
- ✅ Realisierung eines Anmeldevorgangs, bei dem sich ein Client mit Namen und Passwort anmeldet. Sollte der Name noch nicht registriert sein, werden diese Daten auf dem Server gespeichert
- ✅ nach einem Anmeldevorgang bekommt der sich gerade angemeldete Client eine aktuelle Liste der Namen von bereits angemeldeten Clients gesendet.
- ✅ nach einem Anmeldevorgang erhalten alle bisher angemeldeten Clients weiter eine Meldung, dass sich ein neuer Client angemeldet hat

### Client
- ✅ Möglichkeit zum Aufbau und Trennen einer Verbindung zum Server
- ✅ Weitergabe einer Eingabemaske mit Name und Passwort um sich beim Server anzumelden
- ✅ Möglichkeit zum Senden von Nachrichten an alle angemeldeten Clients
- ✅ Nachrichten, welche vom Server gesendet werden zu empfangen und zu visualisieren


## Meilenstein 2
### Server
- ✅ Implementierung als grafische Benutzerschnittstelle (siehe Anhang 1)
- Realisierung einer visuellen Anzeige, einschließlich dynamischer Aktualisierung von folgenden Elementen: (siehe Anhang 1, Anhang 2 und Anhang 3)
- Anzeige der Namen aller vorhandenen Räume
- Anzeige der Namen aller angemeldeten Benutzer sowie der Angabe des Namens von dem Raum, in dem sich der Benutzer aktuell befindet
- ✅ Gestaltung einer Kommunikationsoberfläche zur Visualisierung aller Aktivitäten nach dem Starten des Servers bis zum Beenden im Bezug auf Verbindung, Kommunikation und Verwaltung 
- außerdem sollen diese Ereignisse in einer Datei protokolliert werden (siehe Anhang 1)
- Weiterentwicklung des Einraum-Systems zu einem Mehrraum-System
- ✅ Optimierung der Benutzerkontenverwaltung durch eine dauerhafte Speicherung der Daten auf der Festplatte und der Möglichkeit, diese Datensätze anzuzeigen
- Bereitstellung von Methoden zur Verwaltung von Räumen, wobei folgende Funktion- alitäten bereitgestellt werden müssen: (siehe Anhang 4)
- Erstellen von Räumen
- Editieren von Räumen
- Löschen von Räumen
- Bereitstellung von Methoden zur Verwaltung von Benutzern, wobei folgende Funktionalitäten bereitgestellt werden müssen:
- Verwarnen von Benutzern (Client eine Nachricht schreiben)
- ✅ Temporäres Ausschließen von Benutzern (Verbindung zum Client trennen)
- ✅ Permanentes Ausschließen von Benutzern (Verbindung zum Client trennen und erneutes Anmelden des Clients verhindern)

### Client
- ✅ Implementierung als grafische Benutzerschnittstelle
- Realisierung einer visuellen Anzeige einschließlich dynamischer Aktualisierung von folgenden Elementen: (siehe Anhang 5)
- Anzeige der Namen aller vorhandenen Räume
- Anzeige der Namen aller Benutzer aus demjenigen Raum, in welchem sich der Client befindet
- ✅ Anordnung einer Kommunikationsoberfläche, wodurch die Möglichkeit geschaffen wird, mit anderen Clients zu kommunizieren (siehe Anhang 6)
- Möglichkeit zum Versenden und Empfangen von PDF- und Bilddateien, die auf der empfangenden Client-Seite durch Einbindung geeigneter Bibliotheksfunktionen gesondert dargestellt werden sollen.


## Meilenstein 3
### Server
- Weiterentwicklung des Chatroom-Systems durch die Einführung einer Kommunikation über private Räume zwischen 2 Clients (siehe Anhang 1)
- Bereitstellung von Mechanismen für Clients zur Verwaltung von privaten Räumen, wobei folgende Funktionalitäten vom Server bereitgestellt werden müssen:
- Eröffnen eines privaten Raumes
- Senden von Nachrichten von einem Client zum anderen Client
- Schließen eines privaten Raumes

### Client
- Bereitstellen eines Dialogfensters sowie folgender Wahlmöglichkeiten zum Umgang mit privaten Räumen: (siehe Anhang 1)
- Eröffnen eines privaten Raumes
- Senden von Nachrichten von einem Client zum anderen Client
- Schließen eines privaten Raumes


## Andere Ideen
- Passwort Transfer mit MD5
- Ende zu Ende Verschlüsselung in privaten Räumen
...
