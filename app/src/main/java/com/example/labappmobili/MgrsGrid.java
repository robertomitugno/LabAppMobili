package com.example.labappmobili;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

public class MgrsGrid {

    private GoogleMap googleMap;
    private List<Polyline> gridLines;

    public MgrsGrid(GoogleMap googleMap) {
        this.googleMap = googleMap;
        this.gridLines = new ArrayList<>();
    }

    public void addGridToMap() {
        // Rimuovi la griglia esistente se presente
        removeGridFromMap();

        // Aggiungi le linee verticali
        for (double lng = -180; lng < 180; lng += 1.0) {
            LatLng start = new LatLng(-90, lng);
            LatLng end = new LatLng(90, lng);
            Polyline polyline = googleMap.addPolyline(new PolylineOptions()
                    .add(start, end)
                    .color(0x88000000) // Colore della linea
                    .width(2)); // Larghezza della linea
            gridLines.add(polyline);
        }

        // Aggiungi le linee orizzontali
        for (double lat = -90; lat < 90; lat += 1.0) {
            LatLng start = new LatLng(lat, -180);
            LatLng end = new LatLng(lat, 180);
            Polyline polyline = googleMap.addPolyline(new PolylineOptions()
                    .add(start, end)
                    .color(0x88000000) // Colore della linea
                    .width(2)); // Larghezza della linea
            gridLines.add(polyline);
        }
    }

    public void removeGridFromMap() {
        for (Polyline polyline : gridLines) {
            polyline.remove();
        }
        gridLines.clear();
    }
}
