# FSpatialMap — WMS-Kartenviewer mit GIS-Overlay & ColorAdmin-Kopplung

`com.dan.fspatialmap` (+ `com.dan.fdal.sdo`) · Java 8 · Laboratory Dark

Ein WMS-Kartenviewer im F-Style, der Oracle-SDO_GEOMETRY-Daten, die
`ColorAdmin`-Faerbung und Heatmap-Paletten direkt auf einer echten Basiskarte
(GDI-SH-Geoserver) zusammenbringt. In sechs Ausbaustufen gewachsen — jede
einzeln nutzbar, alle zusammen ein vollstaendiges GIS-Werkzeug.

---

## Installation

`FSpatialMap.jar` neben `FStyle.jar` auf den Classpath (bzw. beide in
NetBeans als Bibliotheken einbinden). Keine weiteren Abhaengigkeiten ausser
dem JDBC-Treiber, falls `com.dan.fdal.sdo` genutzt wird.

```java
FSpatialMapViewer viewer = new FSpatialMapViewer();
viewer.setView(10.1228, 54.3233, 11.0); // Kiel, Zoom 11
panel.add(viewer);
```

Ohne weitere Konfiguration zeigt der Viewer sofort die freie DTK25-Karte des
Landesamts fuer Vermessung und Geoinformation Schleswig-Holstein
(`service.gdi-sh.de`, CC BY 4.0, kostenfrei zur privaten Nutzung).

---

## Schnelluebersicht nach Ausbaustufe

| Phase | Kernklassen | Zweck |
|---|---|---|
| 1 | `FSpatialMapViewer`, `FBasemapType`, `FTileCache`, `FWmsTileFetcher` | WMS-Kachel-Basiskarte, Pan/Zoom/Klick |
| 2 | `com.dan.fdal.sdo.*`, `setVectorModel(FGeoChartModel)` | Oracle SDO_GEOMETRY \u2192 Vektor-Overlay |
| 3 | `setColorAdminModel(FLayerStylerModel)`, `FColorAdminLayerStyle` | Live-Faerbung aus dem ColorAdmin-Editor |
| 4 | `setLayerPalette(FGeoLayer, PaletteDTO)`, `FHeatmapApplier` | Heatmap-Rendering + Persistenz nach `CA_GEOM_COLOR` |
| 5 | `FLayerPanel` | Sichtbarkeit + Transparenz je Layer (alle gleichzeitig) |
| 6 | `FMapTool`, `FSpatialMapToolbar` | Werkzeuge: Verschieben / Abfragen / Messen |
| 7 | `FSpatialMapDemo.java` (nicht im JAR) | Alles zusammen in einem `DFrame` |

---

## Phase 1 — Basiskarte

```java
FSpatialMapViewer viewer = new FSpatialMapViewer();
viewer.getModel().setBasemap(FBasemapType.DTK25_GRAU); // oder DTK25_FARBE
viewer.setView(10.1228, 54.3233, 12.0);
```

- Pan per Drag, Zoom per Mausrad/Buttons (animiert, cursor-zentriert)
- Zweistufiger Kachel-Cache: In-Memory-LRU + Disk (`~/.fstyle/tiles/`)
- `FTileFetcher` austauschbar (Stub fuer Tests ohne Netzwerk):
  ```java
  viewer.setFetcher((basemap, z, x, y) -> meineKachel);
  ```

## Phase 2 — Oracle SDO_GEOMETRY

```java
Map<String,String> layers = new LinkedHashMap<>();
layers.put("Gemeinden", "SELECT gemeinde_id AS ID, gemeinde_name AS NAME, geom AS GEOM FROM sh_gemeinden");

OracleSdoFeatureProvider provider = new OracleSdoFeatureProvider(
        () -> DriverManager.getConnection(url, user, pwd), layers);

DefaultFGeoChartModel model = new DefaultFGeoChartModel();
FGeoLayer layer = model.addLayer("Gemeinden", FLayerGeometryType.POLYGON, Color.CYAN);
for (FGeoFeature f : provider.findFeatures("Gemeinden")) layer.add(f);
viewer.setVectorModel(model);
```

**Grenze:** einfache, nicht-zusammengesetzte 2D-Geometrien (Point/Line/Polygon,
kein Loch, keine Compound-Kurven) — `SdoParseException` mit Klartext-Grund
bei allem daruber hinaus.

## Phase 3 — ColorAdmin-Live-Kopplung

```java
DefaultFLayerStylerModel stylerModel = new DefaultFLayerStylerModel(
        provider, colorAdminDAO, paletteAdminDAO);
viewer.setColorAdminModel(stylerModel);
```

Zeigt den im `FLayerStyler`-Panel *ausgewaehlten* Layer live gefaerbt — jede
Farbzuweisung im Editor erscheint sofort auf der Karte, kein manueller
Refresh. `featureRowClicked`-Event fuer die Rueckrichtung (Kartenklick \u2192
Editor-Auswahl).

## Phase 4 — Heatmap

```java
FHeatmapApplier.autoNormalize(layer);          // Wertespanne aus den Daten
viewer.setLayerPalette(layer, meinePalette);   // PaletteDTO aus ColorAdmin

// optional dauerhaft speichern:
FHeatmapApplier.persist(paletteAdminDAO, "Gemeinden", layer, meinePalette,
        LocalDate.now(), null);
```

## Phase 5 — Layer-Panel

```java
FLayerPanel layerPanel = new FLayerPanel(viewer);
sidebar.add(layerPanel);
```

Eine `FAccordion`-Sektion je Layer, Checkbox (Sichtbarkeit) + Slider
(Transparenz 0\u2013100 %) — alle Layer gleichzeitig, nicht nur der eine
ColorAdmin-ausgewaehlte.

## Phase 6 — Werkzeuge

```java
FSpatialMapToolbar toolbar = new FSpatialMapToolbar(viewer);
```

- **Verschieben** (Default): normales Pan/Klick-Verhalten
- **Abfragen**: Klick zeigt Attribut-Popup des getroffenen Features
- **Messen**: Klicks setzen Punkte, kumulative Distanz wird eingeblendet,
  Rechtsklick setzt zurueck

---

## Bekannte Entscheidungen & Grenzen

- **Vektor-Rendering ist eigenstaendig**, nicht ueber `FGeoChart`: dessen
  Hintergrund bleibt bei `setOpaque(false)` undurchsichtig und der
  `scale`-Parameter verhaelt sich nicht linear zu Pixel/Grad. `FGeoFeature`/
  `FGeoLayer` werden nur als Datenmodell wiederverwendet.
- **Kein interaktiver Heatmap-Apply-Dialog** — die Rechen-/Persistenzlogik
  (`FHeatmapApplier`) steht, der Dialog waere reine UI-Schale und bewusst
  nicht gebaut.
- **WMS wird client-seitig gekachelt** (kein WMTS/TMS beim GDI-SH-Dienst) —
  ein Kachel-Cache ist entsprechend wichtig und vorhanden.

## Tests

`SmokeTest.java` / `DemoSmokeTest.java` (nicht im JAR) laufen headless ohne
echten Netzwerkzugriff (Stub-`FTileFetcher`) und decken alle sechs Phasen ab:
Kachel-Ladepfad, Klick-/Feature-/Row-Events, Heatmap-Farbwerte, Panel-
Interaktion (Checkbox/Slider), Toolbar-Werkzeugwechsel, Messdistanz.
