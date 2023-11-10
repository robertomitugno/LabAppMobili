package com.example.labappmobili;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.TileOverlay;
import com.google.android.gms.maps.model.TileOverlayOptions;

public class GridManager {
    private static GridManager instance;
    private TileOverlay gridOverlay;

    private GridManager() {
        // Costruttore privato per impedire l'istanziazione diretta
    }

    public static GridManager getInstance() {
        if (instance == null) {
            instance = new GridManager();
        }
        return instance;
    }

    public void setGrid(GoogleMap myMap, GridTileProvider gridTileProvider) {
        removeGrid();

        gridOverlay = myMap.addTileOverlay(new TileOverlayOptions().tileProvider(gridTileProvider));
    }

    public void removeGrid() {
        // Rimuovi la visualizzazione della grid qui
        if (gridOverlay != null) {
            gridOverlay.remove();
        }
    }


}