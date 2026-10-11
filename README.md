# excel-bkm-geo

Java-Konsolenprogramm: liest eine Excel-Datei, wandelt Betriebskilometer (BKM) per GeoService `convertLocations` in Koordinaten um und schreibt `Lat:` / `Lon:` in eine neue Excel-Datei.

## Voraussetzungen

- JDK 8 oder neuer
- Maven 3.9+

## Konfiguration

Werte in `src/main/resources/application.properties` (oder als Kommandozeilenargumente):

- `excel.input` / `excel.output`
- Keycloak Client Credentials (`keycloak.token-url`, `keycloak.client-id`, `keycloak.client-secret`)
- GeoService-Basis-URL (`geo.base-url`)

Keine Secrets ins Repository legen.

## Start

```powershell
mvn -q exec:java
```

Oder:

```powershell
mvn -q exec:java "-Dexec.args=C:\pfad\datei.xlsx"
```
