# IBAN Quiz

109 Single-Choice-Fragen zu IBAN, Java, Spring Boot, Docker & mehr.  
Fragen und Antwortoptionen werden bei jedem Laden zufällig gemischt.

## Lokal starten

```bash
cd quiz
python3 -m http.server 8888
```

Dann öffnen: [http://localhost:8888](http://localhost:8888)

## Bedienung

- **Antwort wählen** → Klick auf eine Option
- **Prüfen** → Button „Antwort prüfen" oder `Enter`
- **Navigation** → Buttons oder `←` / `→` Pfeiltasten

## Dateien

| Datei            | Inhalt                                    |
| ---------------- | ----------------------------------------- |
| `questions.json` | 109 Fragen als strukturiertes JSON        |
| `app.js`         | Quiz-Logik (Shuffle, Render, Check)       |
| `index.html`     | HTML-Shell                                |
| `style.css`      | Styling                                   |
| `context.txt`    | Build-Kontext & Entscheidungen für Agents |
